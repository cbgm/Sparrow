"""Phone verification for Sparrow: a separate service next to the Control Plane Directory, run by its operator only.

It checks the app's Firebase phone sign-in with the operator's Firebase Admin service account file; Control Planes
need no Firebase credentials. The app sends its token sealed for this service's request key, through two relays: no relay
reads it, and this service never sees the app's address. It answers with an attestation "this phone alias belongs to
this device", signed with the attestation key and sealed with the reply key from the request, so the last relay cannot
read it either. The Directory signing key certifies both keys once (`directory_service.py certify-phone-keys`); apps
and Control Planes trust the certificate through the Directory key they already pin. This service never holds the
Directory signing key. Nothing is stored: no token, no number, no alias.

Before the app has Firebase send the SMS, it reserves a sign-up slot here (`/v1/phone-attestations/slots`, sealed and
through the relays like the attestation): a small proof of work buys one random slot token, valid for an hour, while the
day's maximum is not reached; otherwise the app shows "try again tomorrow" and no SMS is sent. The attestation uses the
slot up. Only the day, its count and the hashes of the open slots are kept.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import stat
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.asymmetric.x25519 import X25519PrivateKey, X25519PublicKey
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305

from directory_service import canonical, decode, encode, utc_ms

ATTESTATION_PROTOCOL = "sparrow-phone-attestation-v1"
ATTESTATION_DOMAIN = b"sparrow-phone-attestation-v1\n"
REQUEST_CONTEXT = "sparrow-phone-attestation-request-v1"
ATTESTATION_LIFETIME_MS = 60 * 60_000
MAX_BODY = 8_192
PHONE_NUMBER = re.compile(r"\+[1-9][0-9]{1,14}")
ROUTING_ID = re.compile(r"scrouting1_[A-Za-z0-9_-]{1,116}")
SLOT_CONTEXT = "sparrow-phone-slot-request-v1"
SLOT_LIFETIME_MS = 60 * 60_000
SLOT = re.compile(r"[A-Za-z0-9_-]{43}")
WORK_CONTEXT = "sparrow-mailbox-work-v1"
# What the app's solver pays (MailboxCreationWorkSolver.REQUIRED_BITS); more needs an app update first.
WORK_BITS = 18
WORK_WINDOW_MS = 10 * 60_000
STAMP = re.compile(r"[A-Za-z0-9_-]{16,64}")
DAY_MS = 24 * 60 * 60_000
ATTESTATION_KEY_FILE = "attestation.pem"
REQUEST_KEY_FILE = "request.pem"
DAILY_COUNT_FILE = "daily-count.json"


class PhoneVerificationError(ValueError):
    pass


class PhoneVerificationLimitReached(Exception):
    pass


class DailyLimit:
    """At most [maximum] sign-ups per UTC day; 0 means no limit. A sign-up reserves a slot before its SMS is sent
    ([reserve]) and uses it up with its attestation ([take]); a slot not used within [SLOT_LIFETIME_MS] is free again.
    A day counts its attestations plus the open slots. Only the day, its count and the hashes of the open slots are
    kept, in the verifier's own volume, so a restart neither starts the day again nor loses a slot. Nothing about the
    users is kept."""

    def __init__(self, filename: Path | None, maximum: int, clock=utc_ms):
        if maximum < 0:
            raise ValueError("The daily maximum must not be negative")
        self.filename = filename
        self.maximum = maximum
        self.clock = clock
        self.lock = threading.Lock()
        self.day, self.count, self.slots = self.load()

    def today(self) -> str:
        return time.strftime("%Y-%m-%d", time.gmtime(self.clock() // 1000))

    def next_day_ms(self) -> int:
        """When the next UTC day starts: the earliest time a full day has room again."""
        return (self.clock() // DAY_MS + 1) * DAY_MS

    def reserve(self) -> tuple[str, int]:
        """A new slot and when it expires, or raises when today's maximum is reached."""
        with self.lock:
            self.roll()
            if self.maximum and self.count + len(self.slots) >= self.maximum:
                raise PhoneVerificationLimitReached()
            slot = encode(secrets.token_bytes(32))
            expires_at = self.clock() + SLOT_LIFETIME_MS
            self.slots[slot_hash(slot)] = expires_at
            self.save()
            return slot, expires_at

    def take(self, slot: object) -> None:
        """Uses up [slot] for one attestation, or raises when it is unknown or expired."""
        with self.lock:
            self.roll()
            if not isinstance(slot, str) or not SLOT.fullmatch(slot) or self.slots.pop(slot_hash(slot), None) is None:
                raise PhoneVerificationLimitReached()
            self.count += 1
            self.save()

    def used_today(self) -> int:
        with self.lock:
            return self.count if self.day == self.today() else 0

    def open_slots(self) -> int:
        with self.lock:
            self.roll()
            return len(self.slots)

    def roll(self) -> None:
        """A new day starts at 0; slots expire on their own, whatever the day."""
        if self.day != self.today():
            self.day, self.count = self.today(), 0
        now = self.clock()
        for key in [key for key, expires_at in self.slots.items() if expires_at <= now]:
            del self.slots[key]

    def load(self) -> tuple[str, int, dict[str, int]]:
        try:
            saved = json.loads(self.filename.read_text(encoding="utf-8")) if self.filename else {}
        except (OSError, ValueError):
            saved = {}
        if not isinstance(saved, dict):
            saved = {}
        stored = saved.get("slots")
        slots = {key: value for key, value in stored.items() if isinstance(key, str) and type(value) is int} \
            if isinstance(stored, dict) else {}
        count = saved.get("count")
        if saved.get("day") == self.today() and type(count) is int and count >= 0:
            return saved["day"], count, slots
        return self.today(), 0, slots

    def save(self) -> None:
        if self.filename is None:
            return
        pending = self.filename.with_name(self.filename.name + ".pending")
        pending.write_text(json.dumps({"day": self.day, "count": self.count, "slots": self.slots}), encoding="utf-8")
        os.replace(pending, self.filename)


def slot_hash(slot: str) -> str:
    return hashlib.sha256(slot.encode("utf-8")).hexdigest()


class CreationWork:
    """The price of a slot: the same proof of work as for a mailbox (CreationWorkCheck on the server,
    MailboxCreationWorkSolver in the app). `SHA-256("sparrow-mailbox-work-v1|stamp|createdAt|nonce")` must start with
    [bits] zero bits. A stamp counts once and only within [WORK_WINDOW_MS] of its time; used stamps are kept only that
    long, and nothing about who sent them."""

    def __init__(self, bits: int = WORK_BITS, clock=utc_ms):
        self.bits = bits
        self.clock = clock
        self.lock = threading.Lock()
        self.used: dict[str, int] = {}

    def check(self, stamp: object, created_at: object, nonce: object) -> None:
        now = self.clock()
        if not isinstance(stamp, str) or not STAMP.fullmatch(stamp) or type(created_at) is not int or \
                type(nonce) is not int or nonce < 0 or abs(now - created_at) > WORK_WINDOW_MS:
            raise PhoneVerificationError("Invalid proof of work")
        digest = hashlib.sha256(f"{WORK_CONTEXT}|{stamp}|{created_at}|{nonce}".encode("utf-8")).digest()
        if leading_zero_bits(digest) < self.bits:
            raise PhoneVerificationError("Invalid proof of work")
        with self.lock:
            for used, expires_at in list(self.used.items()):
                if expires_at < now:
                    del self.used[used]
            if stamp in self.used:
                raise PhoneVerificationError("Proof of work used twice")
            self.used[stamp] = created_at + WORK_WINDOW_MS


def leading_zero_bits(digest: bytes) -> int:
    bits = 0
    for byte in digest:
        if byte:
            return bits + 8 - byte.bit_length()
        bits += 8
    return bits


def phone_alias(phone_number: str) -> str:
    """Same as ClientRoutingIds.fromPhoneNumber on the server and in the app."""
    if not PHONE_NUMBER.fullmatch(phone_number):
        raise PhoneVerificationError("Verified phone must use E.164")
    return "scphone1_" + encode(hashlib.sha256(phone_number.encode("utf-8")).digest())


# ---------------------------------------------------------------------- sealing (same as server X25519Sealing)
def open_sealed(sealed: bytes, key: X25519PrivateKey, context: str) -> bytes:
    if len(sealed) <= 32 + 16:
        raise PhoneVerificationError("Sealed request is too short")
    ephemeral = sealed[:32]
    recipient = key.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)
    shared = key.exchange(X25519PublicKey.from_public_bytes(ephemeral))
    sealing_key = hashlib.sha256(context.encode("utf-8") + shared + ephemeral + recipient).digest()
    try:
        return ChaCha20Poly1305(sealing_key).decrypt(bytes(12), sealed[32:], None)
    except InvalidTag as exc:
        raise PhoneVerificationError("Sealed request does not open") from exc


def seal_reply(message: bytes, reply_key: bytes) -> bytes:
    """Same as ReplySealing on the server and in the app: one fresh key per reply, zero nonce."""
    return ChaCha20Poly1305(reply_key).encrypt(bytes(12), message, None)


# ---------------------------------------------------------------------- Firebase ID tokens
class FirebaseTokens:
    """Firebase phone sign-in tokens, checked by the Firebase Admin SDK with the operator's service account file:
    Google's signature, the project of the file, expiry, and that the sign-in was not revoked or the user disabled."""

    def __init__(self, credentials_file: Path | None = None, verify=None):
        if verify is None:
            import firebase_admin
            from firebase_admin import auth, credentials
            app = firebase_admin.initialize_app(credentials.Certificate(str(credentials_file)),
                                                name="sparrow-phone-verification")
            verify = lambda token: auth.verify_id_token(token, app=app, check_revoked=True)  # noqa: E731
        self.verify = verify

    def phone_number(self, token: str) -> str:
        if not isinstance(token, str) or not token or len(token) > 4_096:
            raise PhoneVerificationError("Malformed token")
        try:
            claims = self.verify(token)
        except Exception as exc:  # noqa: BLE001 - every SDK refusal means "not verified"
            if type(exc).__name__ == "CertificateFetchError":
                raise OSError("Google's token keys are not reachable") from exc
            raise PhoneVerificationError("Token is not a valid phone sign-in") from exc
        firebase = claims.get("firebase")
        if not isinstance(firebase, dict) or firebase.get("sign_in_provider") != "phone":
            raise PhoneVerificationError("Phone sign-in required")
        phone_number = claims.get("phone_number")
        if not isinstance(phone_number, str):
            raise PhoneVerificationError("Token has no phone number")
        return phone_number


# ---------------------------------------------------------------------- attestations
class PhoneVerifier:
    def __init__(self, attestation_key: Ed25519PrivateKey, request_key: X25519PrivateKey, certificate: dict,
                 tokens: FirebaseTokens, clock=utc_ms, limit: DailyLimit | None = None, resends: int = 1,
                 work: CreationWork | None = None):
        if resends < 0:
            raise ValueError("Resends must not be negative")
        self.limit = limit or DailyLimit(None, 0, clock)
        self.resends = resends
        self.work = work or CreationWork(clock=clock)
        self.attestation_key = attestation_key
        self.request_key = request_key
        self.certificate = certificate
        self.tokens = tokens
        self.clock = clock

    def reserve_slot(self, sealed_request: bytes) -> bytes:
        """Checks the request's proof of work and returns a new slot, sealed with the request's reply key. Raises
        [PhoneVerificationLimitReached] when today is full, before anything is sent."""
        try:
            request = json.loads(open_sealed(sealed_request, self.request_key, SLOT_CONTEXT))
        except json.JSONDecodeError as exc:
            raise PhoneVerificationError("Malformed request") from exc
        if not isinstance(request, dict) or set(request) != {"stamp", "createdAtEpochMilliseconds", "nonce", "replyKey"}:
            raise PhoneVerificationError("Malformed request")
        reply_key = decode(request["replyKey"], 32)
        self.work.check(request["stamp"], request["createdAtEpochMilliseconds"], request["nonce"])
        slot, expires_at = self.limit.reserve()
        return seal_reply(canonical({"slot": slot, "expiresAtEpochMilliseconds": expires_at, "resends": self.resends}),
                          reply_key)

    def attest(self, sealed_request: bytes) -> bytes:
        """Opens the request, verifies its token and returns the attestation sealed with the request's reply key."""
        try:
            request = json.loads(open_sealed(sealed_request, self.request_key, REQUEST_CONTEXT))
        except json.JSONDecodeError as exc:
            raise PhoneVerificationError("Malformed request") from exc
        if not isinstance(request, dict) or set(request) != {"token", "routingId", "replyKey", "slot"}:
            raise PhoneVerificationError("Malformed request")
        routing_id = request["routingId"]
        if not isinstance(routing_id, str) or not ROUTING_ID.fullmatch(routing_id):
            raise PhoneVerificationError("Invalid device routing ID")
        reply_key = decode(request["replyKey"], 32)
        now = self.clock()
        alias = phone_alias(self.tokens.phone_number(request["token"]))
        # The token is checked first, so a slot is only used up by a real phone sign-in.
        self.limit.take(request["slot"])
        payload = canonical({
            "protocol": ATTESTATION_PROTOCOL,
            "alias": alias,
            "routingId": routing_id,
            "issuedAtEpochMilliseconds": now,
            "expiresAtEpochMilliseconds": now + ATTESTATION_LIFETIME_MS
        })
        attestation = {
            "payload": encode(payload),
            "signature": encode(self.attestation_key.sign(ATTESTATION_DOMAIN + payload)),
            "keys": self.certificate
        }
        return seal_reply(canonical(attestation), reply_key)


class PhoneVerifierHandler(BaseHTTPRequestHandler):
    verifier: PhoneVerifier

    def json_response(self, status: int, result: dict) -> None:
        content = canonical(result)
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store" if self.command == "POST" else "public, max-age=300")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(content)))
        self.end_headers()
        self.wfile.write(content)

    def do_GET(self) -> None:
        if self.path == "/health":
            # Only reachable inside Docker: Caddy forwards the phone-attestation paths only.
            self.json_response(200, {"status": "ok", "verificationsToday": self.verifier.limit.used_today(),
                                     "openSlots": self.verifier.limit.open_slots(),
                                     "maximumPerDay": self.verifier.limit.maximum,
                                     "resends": self.verifier.resends})
        elif self.path == "/v1/phone-attestations/keys":
            self.json_response(200, self.verifier.certificate)
        else:
            self.json_response(404, {"error": "Not found"})

    def do_POST(self) -> None:
        if self.path not in ("/v1/phone-attestations", "/v1/phone-attestations/slots"):
            self.json_response(404, {"error": "Not found"})
            return
        try:
            size = int(self.headers.get("Content-Length", "-1"))
            if size < 2 or size > MAX_BODY:
                raise PhoneVerificationError("Invalid request size")
            body = json.loads(self.rfile.read(size))
            if not isinstance(body, dict) or set(body) != {"sealedRequest"}:
                raise PhoneVerificationError("Malformed request")
            if self.path.endswith("/slots"):
                sealed = self.verifier.reserve_slot(sealed_request(body["sealedRequest"]))
                self.json_response(200, {"sealedSlot": encode(sealed)})
            else:
                sealed = self.verifier.attest(sealed_request(body["sealedRequest"]))
                self.json_response(200, {"sealedAttestation": encode(sealed)})
        except PhoneVerificationLimitReached:
            self.json_response(429, {"error": "PHONE_VERIFICATION_LIMIT",
                                     "retryAtEpochMilliseconds": self.verifier.limit.next_day_ms()})
        except (ValueError, TypeError, KeyError):
            # Never the token, number or alias.
            self.json_response(403, {"error": "PHONE_VERIFICATION_FAILED"})
        except OSError:
            self.json_response(503, {"error": "PHONE_VERIFICATION_UNAVAILABLE"})

    def log_message(self, fmt: str, *args) -> None:
        # Requests come through relays and carry phone tokens: nothing is logged.
        return


def sealed_request(value: str) -> bytes:
    """Base64url without padding, like every other value here; longer than [decode] allows, for the token."""
    if not isinstance(value, str) or not 0 < len(value) <= MAX_BODY:
        raise PhoneVerificationError("Malformed request")
    raw = base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)
    if encode(raw) != value:
        raise PhoneVerificationError("Malformed request")
    return raw


def create_server(host: str, port: int, verifier: PhoneVerifier) -> ThreadingHTTPServer:
    class Handler(PhoneVerifierHandler):
        pass
    Handler.verifier = verifier
    server = ThreadingHTTPServer((host, port), Handler)
    server.daemon_threads = True
    return server


# ---------------------------------------------------------------------- keys
def private_key(filename: Path):
    if filename.stat().st_mode & (stat.S_IRWXG | stat.S_IRWXO):
        raise ValueError(f"{filename.name} must not be accessible to group or other")
    return serialization.load_pem_private_key(filename.read_bytes(), password=None)


def write_new_key(filename: Path, key) -> None:
    fd = os.open(filename, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(fd, "wb") as stream:
        stream.write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                       serialization.NoEncryption()))


def read_keys(state: Path) -> tuple[Ed25519PrivateKey, X25519PrivateKey]:
    attestation, request = private_key(state / ATTESTATION_KEY_FILE), private_key(state / REQUEST_KEY_FILE)
    if not isinstance(attestation, Ed25519PrivateKey) or not isinstance(request, X25519PrivateKey):
        raise ValueError("Phone verification keys have the wrong type")
    return attestation, request


def public_keys(attestation: Ed25519PrivateKey, request: X25519PrivateKey) -> dict:
    """The form `directory_service.py certify-phone-keys` takes them in."""
    return {
        "attestationKey": encode(attestation.public_key().public_bytes(
            serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)),
        "requestKey": encode(request.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw))
    }


def read_certificate(filename: Path, keys: dict) -> dict:
    certificate = json.loads(filename.read_text(encoding="utf-8"))
    if not isinstance(certificate, dict) or set(certificate) != {"payload", "signature"}:
        raise ValueError("Malformed phone key certificate")
    payload = json.loads(decode(certificate["payload"]))
    if {name: payload.get(name) for name in keys} != keys:
        raise ValueError("The phone key certificate is for other keys; certify these keys again")
    return certificate


def main() -> None:
    parser = argparse.ArgumentParser(description="Sparrow phone verification (operator only)")
    parser.add_argument("command", choices=("init-keys", "show-public-keys", "serve"))
    parser.add_argument("--state", type=Path, required=True, help="folder with the two private keys")
    parser.add_argument("--certificate", type=Path, help="phone key certificate from the Directory")
    parser.add_argument("--firebase-admin-file", type=Path,
                        default=Path(os.getenv("GOOGLE_APPLICATION_CREDENTIALS") or "/run/firebase-admin.json"),
                        help="the Firebase Admin service account file of the app's Firebase project")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=9082)
    parser.add_argument("--per-day", type=int, default=int(os.getenv("PHONE_VERIFICATIONS_PER_DAY") or "0"),
                        help="maximum verifications per UTC day (0 = no limit)")
    parser.add_argument("--resends", type=int, default=int(os.getenv("PHONE_VERIFICATION_RESENDS") or "1"),
                        help="how often the app may send the SMS code again for one sign-up")
    args = parser.parse_args()
    if args.command == "init-keys":
        args.state.mkdir(parents=True, exist_ok=True)
        if not (args.state / ATTESTATION_KEY_FILE).exists():
            write_new_key(args.state / ATTESTATION_KEY_FILE, Ed25519PrivateKey.generate())
        if not (args.state / REQUEST_KEY_FILE).exists():
            write_new_key(args.state / REQUEST_KEY_FILE, X25519PrivateKey.generate())
    attestation, request = read_keys(args.state)
    keys = public_keys(attestation, request)
    if args.command != "serve":
        print(json.dumps(keys))
        return
    if args.certificate is None:
        parser.error("--certificate is required for serve")
    verifier = PhoneVerifier(attestation, request, read_certificate(args.certificate, keys),
                             FirebaseTokens(args.firebase_admin_file),
                             limit=DailyLimit(args.state / DAILY_COUNT_FILE, args.per_day), resends=args.resends)
    server = create_server(args.host, args.port, verifier)
    print("phone verification listener:", server.server_address,
          "per day:", args.per_day or "no limit", "resends:", args.resends)
    try:
        server.serve_forever()
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
