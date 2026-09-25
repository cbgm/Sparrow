"""Sparrow's independent, operator-owned Control Plane Directory (not a bundle component).

Python 3.11+, cryptography. Public ingress is read-only except for bounded opt-in
registration; administrative HTTP is loopback-only and requires a separate token.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import http.client
import ipaddress
import json
import os
from pathlib import Path
import secrets
import socket
import sqlite3
import ssl
import stat
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey

PROTOCOL = "sparrow-control-plane-directory-v1"
PROOF_DOMAIN = b"sparrow-control-plane-directory-registration-v1\n"
SNAPSHOT_DOMAIN = b"sparrow-control-plane-directory-snapshot-v1\n"
CHALLENGE_LIFETIME_MS = 10 * 60_000
SNAPSHOT_LIFETIME_MS = 60 * 60_000
MAX_PENDING = 500
MAX_PUBLIC_RESPONSE = 8_192
MAX_BODY = 4_096


def utc_ms() -> int:
    return int(time.time() * 1000)


def encode(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def decode(value: str, length: int | None = None) -> bytes:
    if not isinstance(value, str) or not value or len(value) > 512:
        raise ValueError("Invalid encoded value")
    raw = base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)
    if encode(raw) != value or (length is not None and len(raw) != length):
        raise ValueError("Invalid encoded value")
    return raw


def canonical(value: object) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def key_id(key: Ed25519PublicKey) -> str:
    return hashlib.sha256(key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)).hexdigest()


def key_from_string(value: str) -> Ed25519PublicKey:
    raw = decode(value)
    # Existing Sparrow NodeIdentity uses X.509 DER-encoded Ed25519 public keys.
    key = serialization.load_der_public_key(raw)
    if not isinstance(key, Ed25519PublicKey):
        raise ValueError("Public key must be Ed25519")
    if key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo) != raw:
        raise ValueError("Public key encoding is not canonical")
    return key


def normalized_url(value: str) -> tuple[str, str]:
    if not isinstance(value, str) or len(value) > 253:
        raise ValueError("Invalid control-plane URL")
    parsed = urlsplit(value)
    if (parsed.scheme != "https" or parsed.username or parsed.password or
            parsed.path not in ("", "/") or parsed.query or parsed.fragment):
        raise ValueError("Control-plane URL must be an HTTPS origin")
    try:
        hostname = parsed.hostname or ""
        ascii_host = hostname.encode("idna").decode("ascii").lower().rstrip(".")
        port = parsed.port
    except (UnicodeError, ValueError) as exc:
        raise ValueError("Invalid control-plane hostname or port") from exc
    if (port not in (None, 443) or not ascii_host or len(ascii_host) > 253 or
            ascii_host in ("localhost",) or ascii_host.endswith((".localhost", ".local", ".internal")) or
            "." not in ascii_host or not all(0 < len(p) <= 63 and
            p[0].isalnum() and p[-1].isalnum() and all(c.isascii() and (c.isalnum() or c == "-") for c in p)
            for p in ascii_host.split("."))):
        raise ValueError("Control-plane URL must have a public DNS hostname")
    try:
        ipaddress.ip_address(ascii_host)
    except ValueError:
        pass
    else:
        raise ValueError("IP-address control planes are not eligible")
    return "https://" + ascii_host, ascii_host


def public_ip_addresses(host: str) -> tuple[str, ...]:
    addresses = set()
    try:
        records = socket.getaddrinfo(host, 443, type=socket.SOCK_STREAM, proto=socket.IPPROTO_TCP)
    except OSError as exc:
        raise ValueError("Control-plane hostname DNS lookup failed") from exc
    for item in records:
        addr = item[4][0]
        if not ipaddress.ip_address(addr).is_global:
            raise ValueError("Control-plane DNS includes a non-public address")
        addresses.add(addr)
        if len(addresses) > 4:
            raise ValueError("Control-plane hostname resolves to too many addresses")
    if not addresses:
        raise ValueError("Control-plane hostname has no public IP address")
    return tuple(sorted(addresses))


class PinnedHTTPSConnection(http.client.HTTPSConnection):
    """Connect to the DNS-validated IP while verifying TLS for the original hostname."""

    def __init__(self, hostname: str, address: str, timeout: float):
        super().__init__(hostname, port=443, context=ssl.create_default_context(), timeout=timeout)
        self.address = address

    def connect(self) -> None:
        sock = socket.create_connection((self.address, 443), self.timeout)
        try:
            self.sock = self._context.wrap_socket(sock, server_hostname=self.host)
        except BaseException:
            sock.close()
            raise


def fetch_endpoint_proof(host: str, challenge_id: str) -> dict:
    # No proxy, no redirects, no custom port, no untrusted URL path, no local IPs.
    addresses = public_ip_addresses(host)
    last_error: Exception | None = None
    for address in addresses:
        connection = PinnedHTTPSConnection(host, address, 4.0)
        try:
            connection.request("GET", "/.well-known/sparrow-directory-registration/" + challenge_id,
                               headers={"Accept": "application/json", "Host": host})
            response = connection.getresponse()
            if response.status != 200:
                raise ValueError("HTTPS endpoint did not publish the requested proof")
            content = response.read(MAX_PUBLIC_RESPONSE + 1)
            if len(content) > MAX_PUBLIC_RESPONSE:
                raise ValueError("HTTPS endpoint proof is too large")
            result = json.loads(content)
            if not isinstance(result, dict):
                raise ValueError("HTTPS endpoint proof must be an object")
            return result
        except (OSError, ssl.SSLError, ValueError, json.JSONDecodeError) as exc:
            last_error = exc
        finally:
            connection.close()
    raise ValueError("HTTPS endpoint ownership proof could not be verified") from last_error


def proof_message(challenge: dict) -> bytes:
    return PROOF_DOMAIN + canonical({
        "challengeId": challenge["challengeId"],
        "controlPlaneId": challenge["controlPlaneId"],
        "baseUrl": challenge["baseUrl"],
        "publicKey": challenge["publicKey"],
        "expiresAtEpochMilliseconds": challenge["expiresAtEpochMilliseconds"],
    })


def verify_proof(challenge: dict, signature: str, endpoint: dict) -> None:
    if endpoint.get("challengeId") != challenge["challengeId"] or endpoint.get("controlPlaneId") != challenge["controlPlaneId"] or endpoint.get("baseUrl") != challenge["baseUrl"] or endpoint.get("publicKey") != challenge["publicKey"] or endpoint.get("signature") != signature:
        raise ValueError("Endpoint did not publish the identical registration proof")
    try:
        key_from_string(challenge["publicKey"]).verify(decode(signature, 64), proof_message(challenge))
    except (InvalidSignature, ValueError) as exc:
        raise ValueError("Registration key-possession proof failed") from exc


class DirectoryStore:
    def __init__(self, filename: Path, signing_key: Ed25519PrivateKey, clock=utc_ms, endpoint_fetcher=fetch_endpoint_proof):
        self.clock = clock
        self.signing_key = signing_key
        self.endpoint_fetcher = endpoint_fetcher
        self.lock = threading.RLock()
        filename.parent.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(str(filename), check_same_thread=False, isolation_level=None, timeout=10)
        self.db.execute("PRAGMA journal_mode=WAL")
        self.db.execute("PRAGMA busy_timeout=10000")
        self.db.executescript("""
            CREATE TABLE IF NOT EXISTS state (name TEXT PRIMARY KEY, value INTEGER NOT NULL);
            INSERT OR IGNORE INTO state VALUES ('version', 0);
            CREATE TABLE IF NOT EXISTS challenges (
                id TEXT PRIMARY KEY, plane_id TEXT NOT NULL, base_url TEXT NOT NULL,
                public_key TEXT NOT NULL, expires INTEGER NOT NULL, used INTEGER NOT NULL DEFAULT 0);
            CREATE TABLE IF NOT EXISTS registrations (
                id TEXT PRIMARY KEY, plane_id TEXT NOT NULL, base_url TEXT NOT NULL,
                public_key TEXT NOT NULL, proof TEXT NOT NULL, proof_expires INTEGER NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('pending','approved','revoked')),
                created INTEGER NOT NULL);
            CREATE UNIQUE INDEX IF NOT EXISTS unique_active_identity ON registrations(plane_id) WHERE status='approved';
            CREATE UNIQUE INDEX IF NOT EXISTS unique_active_url ON registrations(base_url) WHERE status='approved';
        """)

    def issue_challenge(self, request: dict) -> dict:
        base_url, _ = normalized_url(request["baseUrl"])
        public_key = request["publicKey"]
        plane_id = request["controlPlaneId"]
        if not isinstance(plane_id, str) or plane_id != key_id(key_from_string(public_key)):
            raise ValueError("Control-plane ID must be SHA-256 of its X.509 Ed25519 public key")
        now = self.clock()
        challenge = {"challengeId": encode(secrets.token_bytes(24)), "controlPlaneId": plane_id,
                     "baseUrl": base_url, "publicKey": public_key,
                     "expiresAtEpochMilliseconds": now + CHALLENGE_LIFETIME_MS}
        with self.lock:
            self.db.execute("DELETE FROM challenges WHERE expires < ?", (now,))
            self.db.execute("DELETE FROM registrations WHERE status='pending' AND proof_expires <= ?", (now,))
            if (self.db.execute("SELECT COUNT(*) FROM challenges WHERE used=0").fetchone()[0] >= MAX_PENDING or
                    self.db.execute("SELECT COUNT(*) FROM registrations WHERE status='pending'").fetchone()[0] >= MAX_PENDING):
                raise ValueError("Registration queue is full")
            self.db.execute("INSERT INTO challenges VALUES (?,?,?,?,?,0)",
                            (challenge["challengeId"], plane_id, base_url, public_key, challenge["expiresAtEpochMilliseconds"]))
        return challenge

    def propose(self, request: dict) -> str:
        challenge_id = request["challengeId"]
        signature = request["signature"]
        if not isinstance(challenge_id, str) or not isinstance(signature, str):
            raise ValueError("Invalid registration")
        with self.lock:
            record = self.db.execute("SELECT plane_id, base_url, public_key, expires, used FROM challenges WHERE id=?", (challenge_id,)).fetchone()
            if not record or record[4] or self.clock() >= record[3]:
                raise ValueError("Unknown, used or expired registration challenge")
            challenge = {"challengeId": challenge_id, "controlPlaneId": record[0], "baseUrl": record[1],
                         "publicKey": record[2], "expiresAtEpochMilliseconds": record[3]}
        # Network I/O must never hold the database lock: readers can continue to
        # obtain signed snapshots if an applicant's HTTPS endpoint is unavailable.
        _, host = normalized_url(challenge["baseUrl"])
        endpoint = self.endpoint_fetcher(host, challenge_id)
        verify_proof(challenge, signature, endpoint)
        with self.lock:
            self.db.execute("BEGIN IMMEDIATE")
            try:
                changed = self.db.execute("UPDATE challenges SET used=1 WHERE id=? AND used=0 AND expires>?",
                                          (challenge_id, self.clock())).rowcount
                if changed != 1:
                    raise ValueError("Registration challenge was already used or expired")
                # Self-registration is opt-in and immediately published ONLY
                # after fresh endpoint-ownership AND private-key-possession
                # proofs passed above. Never resurrect a revoked identity or
                # take over an existing identity/URL by a different key.
                prior = self.db.execute(
                    "SELECT base_url, public_key, status FROM registrations "
                    "WHERE plane_id=? AND status IN ('approved','revoked')",
                    (record[0],)).fetchone()
                if prior:
                    if prior[2] == 'revoked':
                        raise ValueError("Revoked identity cannot register again")
                    if prior[0] != record[1] or prior[1] != record[2]:
                        raise ValueError("Identity/endpoint rotation requires a separate authenticated process")
                    # Existing approved registration is idempotent: do not
                    # create duplicate rows or increment the directory version.
                else:
                    taken = self.db.execute(
                        "SELECT 1 FROM registrations WHERE base_url=? AND status='approved'",
                        (record[1],)).fetchone()
                    if taken:
                        raise ValueError("Endpoint already belongs to another Control Plane")
                    approved_count = self.db.execute(
                        "SELECT COUNT(*) FROM registrations WHERE status='approved'").fetchone()[0]
                    if approved_count >= 2048:
                        raise ValueError("Directory capacity reached")
                    self.db.execute("INSERT INTO registrations VALUES (?,?,?,?,?,?,'approved',?)",
                                    (challenge_id, record[0], record[1], record[2], signature, record[3], self.clock()))
                    self.db.execute("UPDATE state SET value=value+1 WHERE name='version'")
                self.db.execute("COMMIT")
            except BaseException:
                self.db.execute("ROLLBACK")
                raise
            return challenge_id

    def pending(self) -> list[dict]:
        with self.lock:
            return [dict(zip(("registrationId", "controlPlaneId", "baseUrl", "publicKey", "proofExpiresAt"), row))
                    for row in self.db.execute("SELECT id, plane_id, base_url, public_key, proof_expires FROM registrations WHERE status='pending' ORDER BY created, id")]

    def approve(self, registration_id: str) -> int:
        with self.lock:
            self.db.execute("BEGIN IMMEDIATE")
            try:
                record = self.db.execute("SELECT plane_id, base_url, proof_expires, status FROM registrations WHERE id=?",
                                         (registration_id,)).fetchone()
                if not record or record[3] != "pending" or self.clock() >= record[2]:
                    raise ValueError("Pending registration is missing or its proof expired")
                if self.db.execute("SELECT 1 FROM registrations WHERE (plane_id=? AND status IN ('approved','revoked')) OR (base_url=? AND status='approved')",
                                   (record[0], record[1])).fetchone():
                    raise ValueError("Identity/endpoint already approved or revoked; rotation requires explicit operator recovery")
                self.db.execute("UPDATE registrations SET status='approved' WHERE id=?", (registration_id,))
                self.db.execute("UPDATE state SET value=value+1 WHERE name='version'")
                version = self.db.execute("SELECT value FROM state WHERE name='version'").fetchone()[0]
                self.db.execute("COMMIT")
                return version
            except BaseException:
                self.db.execute("ROLLBACK")
                raise

    def revoke(self, plane_id: str) -> int:
        with self.lock:
            self.db.execute("BEGIN IMMEDIATE")
            try:
                changed = self.db.execute("UPDATE registrations SET status='revoked' WHERE plane_id=? AND status='approved'", (plane_id,)).rowcount
                if changed != 1:
                    raise ValueError("Control-plane identity is not approved")
                self.db.execute("UPDATE state SET value=value+1 WHERE name='version'")
                version = self.db.execute("SELECT value FROM state WHERE name='version'").fetchone()[0]
                self.db.execute("COMMIT")
                return version
            except BaseException:
                self.db.execute("ROLLBACK")
                raise

    def snapshot(self) -> dict:
        with self.lock:
            version = self.db.execute("SELECT value FROM state WHERE name='version'").fetchone()[0]
            planes = [dict(zip(("controlPlaneId", "baseUrl", "publicKey"), row))
                      for row in self.db.execute("SELECT plane_id, base_url, public_key FROM registrations WHERE status='approved' ORDER BY plane_id")]
            revoked = [row[0] for row in self.db.execute("SELECT DISTINCT plane_id FROM registrations WHERE status='revoked' ORDER BY plane_id")]
        now = self.clock()
        payload = {"protocol": PROTOCOL, "version": version, "generatedAtEpochMilliseconds": now,
                   "validUntilEpochMilliseconds": now + SNAPSHOT_LIFETIME_MS,
                   "controlPlanes": planes, "revokedControlPlaneIds": revoked}
        signature = self.signing_key.sign(SNAPSHOT_DOMAIN + canonical(payload))
        return {"payload": payload, "keyId": key_id(self.signing_key.public_key()), "signature": encode(signature)}


class SlidingLimiter:
    def __init__(self):
        self.lock = threading.Lock()
        self.events: dict[str, list[float]] = {}

    def allow(self, key: str) -> bool:
        now = time.monotonic()
        with self.lock:
            if len(self.events) > 10_000:
                self.events = {k: ts for k, ts in self.events.items() if ts and ts[-1] > now - 3600}
            timestamps = [ts for ts in self.events.get(key, ()) if ts > now - 3600]
            if len(timestamps) >= 20 or sum(ts > now - 60 for ts in timestamps) >= 2:
                return False
            timestamps.append(now)
            self.events[key] = timestamps
            return True


class DirectoryHandler(BaseHTTPRequestHandler):
    store: DirectoryStore
    limiter: SlidingLimiter
    admin_token: bytes | None

    def json_response(self, status: int, result: dict | list) -> None:
        content = canonical(result)
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store" if self.admin_token else "public, max-age=60")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(content)))
        self.end_headers()
        self.wfile.write(content)

    def body(self) -> dict:
        try:
            size = int(self.headers.get("Content-Length", "-1"))
        except ValueError as exc:
            raise ValueError("Invalid Content-Length") from exc
        if size < 2 or size > MAX_BODY:
            raise ValueError("Invalid request size")
        parsed = json.loads(self.rfile.read(size))
        if not isinstance(parsed, dict):
            raise ValueError("Request must be a JSON object")
        return parsed

    def authenticated(self) -> bool:
        if self.admin_token is None:
            return False
        bearer = self.headers.get("Authorization", "")
        if not hmac.compare_digest(bearer.encode("utf-8"), b"Bearer " + self.admin_token):
            self.json_response(401, {"error": "Unauthorized"})
            return False
        return True

    def do_GET(self) -> None:
        if self.path == "/health":
            self.json_response(200, {"status": "ok"})
        elif self.path == "/.well-known/sparrow-directory" and self.admin_token is None:
            # Public metadata is not a trust anchor by itself. Clients establish
            # initial trust only over a successfully verified HTTPS connection,
            # and pin the returned signing identity in persistent local storage.
            key = self.store.signing_key.public_key()
            self.json_response(200, {
                "protocol": "sparrow-directory-bootstrap-v1",
                "keyId": key_id(key),
                "publicKey": encode(key.public_bytes(
                    serialization.Encoding.DER,
                    serialization.PublicFormat.SubjectPublicKeyInfo)),
                "controlPlanesEndpoint": "/v1/control-planes"
            })
        elif self.path == "/v1/control-planes" and self.admin_token is None:
            self.json_response(200, self.store.snapshot())
        elif self.path == "/admin/v1/registrations" and self.authenticated():
            self.json_response(200, {"pending": self.store.pending()})
        else:
            self.json_response(404, {"error": "Not found"})

    def do_POST(self) -> None:
        try:
            if self.admin_token is None and self.path in ("/v1/registrations/challenge", "/v1/registrations"):
                if not self.limiter.allow(self.client_address[0]):
                    self.json_response(429, {"error": "Rate limit exceeded"})
                    return
                request = self.body()
                if self.path.endswith("/challenge"):
                    self.json_response(201, self.store.issue_challenge(request))
                else:
                    self.json_response(201, {"registrationId": self.store.propose(request), "status": "approved"})
            elif self.admin_token is not None and self.path.startswith("/admin/v1/registrations/"):
                if not self.authenticated():
                    return
                action, sep, ident = self.path.removeprefix("/admin/v1/registrations/").partition("/")
                if not action or len(action) > 128 or sep != "/" or ident not in ("approve", "revoke"):
                    self.json_response(404, {"error": "Not found"})
                    return
                if self.headers.get("Content-Length", "0") != "0":
                    raise ValueError("Administrative action has no request body")
                version = self.store.approve(action) if ident == "approve" else self.store.revoke(action)
                self.json_response(200, {"version": version})
            else:
                self.json_response(404, {"error": "Not found"})
        except (ValueError, KeyError, TypeError, json.JSONDecodeError) as exc:
            self.json_response(400, {"error": str(exc)[:200]})
        except sqlite3.IntegrityError:
            self.json_response(409, {"error": "Registration conflicts with an existing entry"})

    def log_message(self, fmt: str, *args) -> None:
        # No registration payloads or administrator bearer tokens in access logs.
        return


def create_server(host: str, port: int, store: DirectoryStore, limiter: SlidingLimiter,
                  token: bytes | None) -> ThreadingHTTPServer:
    class Handler(DirectoryHandler):
        pass
    Handler.store = store
    Handler.limiter = limiter
    Handler.admin_token = token
    server = ThreadingHTTPServer((host, port), Handler)
    server.daemon_threads = True
    return server


def read_key(filename: Path) -> Ed25519PrivateKey:
    mode = filename.stat().st_mode
    if mode & (stat.S_IRWXG | stat.S_IRWXO):
        raise ValueError("Directory signing key file must not be accessible to group or other")
    result = serialization.load_pem_private_key(filename.read_bytes(), password=None)
    if not isinstance(result, Ed25519PrivateKey):
        raise ValueError("Directory signing key must be Ed25519")
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="Private Sparrow Control Plane Directory")
    parser.add_argument("command", choices=("init-key", "serve", "show-key-id", "show-public-key"))
    parser.add_argument("--key-file", type=Path, required=True)
    parser.add_argument("--database", type=Path)
    parser.add_argument("--public-host", default="127.0.0.1")
    parser.add_argument("--public-port", type=int, default=9080)
    parser.add_argument("--admin-port", type=int, default=9081)
    args = parser.parse_args()
    if args.command == "init-key":
        args.key_file.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(args.key_file, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        with os.fdopen(fd, "wb") as stream:
            stream.write(Ed25519PrivateKey.generate().private_bytes(
                serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    key = read_key(args.key_file)
    if args.command == "show-public-key":
        print(encode(key.public_key().public_bytes(
            serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)))
        return
    print("directory signing key ID:", key_id(key.public_key()))
    if args.command != "serve":
        return
    if args.database is None:
        parser.error("--database is required for serve")
    token = os.getenv("SPARROW_DIRECTORY_ADMIN_TOKEN", "")
    if len(token) < 32:
        parser.error("SPARROW_DIRECTORY_ADMIN_TOKEN must contain at least 32 characters")
    store = DirectoryStore(args.database, key)
    limiter = SlidingLimiter()
    public = create_server(args.public_host, args.public_port, store, limiter, None)
    admin = create_server("127.0.0.1", args.admin_port, store, limiter, token.encode("utf-8"))
    print("public listener:", public.server_address, "(terminate TLS at a dedicated reverse proxy)")
    print("admin listener: loopback only; NEVER expose or forward it publicly")
    threading.Thread(target=admin.serve_forever, daemon=True).start()
    try:
        public.serve_forever()
    finally:
        public.server_close()
        admin.shutdown()
        admin.server_close()
        store.db.close()


if __name__ == "__main__":
    main()
