"""python3 -m unittest test_phone_verifier (from this folder)."""
import base64
import hashlib
import json
import os
import unittest

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.asymmetric.x25519 import X25519PrivateKey, X25519PublicKey
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305

import directory_service
import phone_verifier

PHONE = "+491701234567"
ROUTING_ID = "scrouting1_device"
NOW_MS = 1_800_000_000_000


def b64(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


class CertificateFetchError(Exception):
    """Named like the SDK's error for unreachable Google token keys."""


class FakeFirebase:
    """Stands in for the Firebase Admin SDK: a token is its claims as JSON; the SDK's own checks are its job."""

    def __init__(self):
        self.refuse = None

    def verify(self, token: str) -> dict:
        if self.refuse:
            raise self.refuse
        return json.loads(token)

    def token(self, **changes) -> str:
        claims = {"sub": "user", "phone_number": PHONE, "firebase": {"sign_in_provider": "phone"}}
        claims.update(changes)
        return json.dumps(claims)


def seal(message: bytes, recipient: bytes, context: str) -> bytes:
    ephemeral = X25519PrivateKey.generate()
    ephemeral_public = ephemeral.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)
    shared = ephemeral.exchange(X25519PublicKey.from_public_bytes(recipient))
    key = hashlib.sha256(context.encode() + shared + ephemeral_public + recipient).digest()
    return ephemeral_public + ChaCha20Poly1305(key).encrypt(bytes(12), message, None)


def solve(created_at: int, stamp: str | None = None) -> dict:
    """The app's proof of work (MailboxCreationWorkSolver), at the verifier's 18 bits."""
    stamp = stamp or b64(os.urandom(16))
    nonce = 0
    while phone_verifier.leading_zero_bits(hashlib.sha256(
            f"sparrow-mailbox-work-v1|{stamp}|{created_at}|{nonce}".encode()).digest()) < phone_verifier.WORK_BITS:
        nonce += 1
    return {"stamp": stamp, "createdAtEpochMilliseconds": created_at, "nonce": nonce}


class PhoneVerifierTest(unittest.TestCase):
    def setUp(self):
        self.google = FakeFirebase()
        self.directory = Ed25519PrivateKey.generate()
        attestation, request = Ed25519PrivateKey.generate(), X25519PrivateKey.generate()
        self.keys = phone_verifier.public_keys(attestation, request)
        certificate = directory_service.phone_keys_certificate(
            self.directory, self.keys["attestationKey"], self.keys["requestKey"])
        self.verifier = phone_verifier.PhoneVerifier(
            attestation, request, certificate, phone_verifier.FirebaseTokens(verify=self.google.verify),
            clock=lambda: NOW_MS)

    def request(self, token: str, routing_id: str = ROUTING_ID, slot: str | None = None) -> tuple[bytes, bytes]:
        reply_key = os.urandom(32)
        slot = slot or self.slot()["slot"]
        plaintext = json.dumps({"token": token, "routingId": routing_id, "replyKey": b64(reply_key),
                                "slot": slot}).encode()
        sealed = seal(plaintext, phone_verifier.decode(self.keys["requestKey"], 32), phone_verifier.REQUEST_CONTEXT)
        return sealed, reply_key

    def attest(self, token: str, slot: str | None = None) -> dict:
        sealed, reply_key = self.request(token, slot=slot)
        return json.loads(ChaCha20Poly1305(reply_key).decrypt(bytes(12), self.verifier.attest(sealed), None))

    def slot(self, work: dict | None = None) -> dict:
        reply_key = os.urandom(32)
        plaintext = json.dumps({**(work or solve(self.verifier.clock())), "replyKey": b64(reply_key)}).encode()
        sealed = seal(plaintext, phone_verifier.decode(self.keys["requestKey"], 32), phone_verifier.SLOT_CONTEXT)
        return json.loads(ChaCha20Poly1305(reply_key).decrypt(bytes(12), self.verifier.reserve_slot(sealed), None))

    def test_a_verified_phone_gets_an_attestation_the_directory_key_vouches_for(self):
        attestation = self.attest(self.google.token())

        keys = attestation["keys"]
        keys_payload = phone_verifier.decode(keys["payload"])
        self.directory.public_key().verify(phone_verifier.decode(keys["signature"], 64),
                                           directory_service.PHONE_KEYS_DOMAIN + keys_payload)
        attestation_key = directory_service.key_from_string(json.loads(keys_payload)["attestationKey"])
        payload = phone_verifier.decode(attestation["payload"])
        attestation_key.verify(phone_verifier.decode(attestation["signature"], 64),
                               phone_verifier.ATTESTATION_DOMAIN + payload)
        self.assertEqual({
            "protocol": "sparrow-phone-attestation-v1",
            "alias": "scphone1_tCZ6IjTiBy-k0ujYn75yfFD8QUcx1uyswu09APBQONE",
            "routingId": ROUTING_ID,
            "issuedAtEpochMilliseconds": NOW_MS,
            "expiresAtEpochMilliseconds": NOW_MS + phone_verifier.ATTESTATION_LIFETIME_MS
        }, json.loads(payload))

    def test_only_a_phone_sign_in_with_a_valid_number_is_accepted(self):
        for changes in ({"firebase": {"sign_in_provider": "password"}}, {"phone_number": "0170"},
                        {"phone_number": None}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.attest(self.google.token(**changes))

    def test_a_token_the_sdk_refuses_is_refused_and_unreachable_google_keys_are_not_a_refusal(self):
        self.google.refuse = ValueError("revoked")
        with self.assertRaises(phone_verifier.PhoneVerificationError):
            self.attest(self.google.token())
        self.google.refuse = CertificateFetchError()
        with self.assertRaises(OSError):
            self.attest(self.google.token())

    def test_a_changed_request_is_refused(self):
        token = self.google.token()
        sealed, _ = self.request(token)
        with self.assertRaises(ValueError):
            self.verifier.attest(sealed[:-1] + bytes([sealed[-1] ^ 1]))
        with self.assertRaises(ValueError):
            self.verifier.attest(self.request(token, routing_id="scphone1_not-a-device")[0])

    def test_a_slot_says_when_it_expires_and_how_often_the_code_may_be_sent_again(self):
        slot = self.slot()

        self.assertEqual(43, len(slot["slot"]))
        self.assertEqual(NOW_MS + phone_verifier.SLOT_LIFETIME_MS, slot["expiresAtEpochMilliseconds"])
        self.assertEqual(1, slot["resends"])

    def test_a_full_day_gives_no_slot_until_the_next_day(self):
        now = [NOW_MS]
        self.verifier.clock = self.verifier.work.clock = lambda: now[0]
        self.verifier.limit = phone_verifier.DailyLimit(None, 2, clock=lambda: now[0])
        self.attest(self.google.token())
        self.slot()

        with self.assertRaises(phone_verifier.PhoneVerificationLimitReached):
            self.slot()
        self.assertEqual((NOW_MS // 86_400_000 + 1) * 86_400_000, self.verifier.limit.next_day_ms())
        now[0] += 24 * 60 * 60 * 1000
        self.slot()
        self.assertEqual(0, self.verifier.limit.used_today())

    def test_an_unused_slot_is_free_again_after_its_lifetime(self):
        now = [NOW_MS]
        self.verifier.clock = self.verifier.work.clock = lambda: now[0]
        self.verifier.limit = phone_verifier.DailyLimit(None, 1, clock=lambda: now[0])
        expired = self.slot()["slot"]
        with self.assertRaises(phone_verifier.PhoneVerificationLimitReached):
            self.slot()

        now[0] += phone_verifier.SLOT_LIFETIME_MS
        with self.assertRaises(phone_verifier.PhoneVerificationLimitReached):
            self.attest(self.google.token(), slot=expired)
        self.attest(self.google.token())
        self.assertEqual(1, self.verifier.limit.used_today())

    def test_an_attestation_needs_a_slot_and_uses_it_up(self):
        for slot in ("x" * 43, b64(os.urandom(32))):
            with self.subTest(slot=slot), self.assertRaises(phone_verifier.PhoneVerificationLimitReached):
                self.attest(self.google.token(), slot=slot)
        slot = self.slot()["slot"]
        self.attest(self.google.token(), slot=slot)

        with self.assertRaises(phone_verifier.PhoneVerificationLimitReached):
            self.attest(self.google.token(), slot=slot)

    def test_a_failed_verification_keeps_its_slot(self):
        self.verifier.limit = phone_verifier.DailyLimit(None, 1, clock=lambda: NOW_MS)
        slot = self.slot()["slot"]
        with self.assertRaises(ValueError):
            self.attest(self.google.token(firebase={"sign_in_provider": "password"}), slot=slot)

        self.attest(self.google.token(), slot=slot)
        self.assertEqual(1, self.verifier.limit.used_today())

    def test_a_slot_costs_a_fresh_proof_of_work_that_counts_once(self):
        work = solve(NOW_MS)
        self.slot(work)
        with self.assertRaises(phone_verifier.PhoneVerificationError):
            self.slot(work)
        for bad in ({**solve(NOW_MS), "nonce": -1}, solve(NOW_MS - phone_verifier.WORK_WINDOW_MS - 1),
                    {**solve(NOW_MS), "stamp": "short"}):
            with self.subTest(work=bad), self.assertRaises(phone_verifier.PhoneVerificationError):
                self.slot(bad)
        cheap = {"stamp": b64(os.urandom(16)), "createdAtEpochMilliseconds": NOW_MS, "nonce": 0}
        while phone_verifier.leading_zero_bits(hashlib.sha256(
                f"sparrow-mailbox-work-v1|{cheap['stamp']}|{NOW_MS}|{cheap['nonce']}".encode()).digest()) >= 18:
            cheap["nonce"] += 1
        with self.assertRaises(phone_verifier.PhoneVerificationError):
            self.slot(cheap)

    def test_the_count_and_open_slots_survive_a_restart_and_hold_nothing_else(self):
        path = phone_verifier.Path(self.id().replace(".", "_") + ".json")
        try:
            limit = phone_verifier.DailyLimit(path, 5, clock=lambda: NOW_MS)
            used, kept = limit.reserve()[0], limit.reserve()[0]
            limit.take(used)
            restarted = phone_verifier.DailyLimit(path, 5, clock=lambda: NOW_MS)

            self.assertEqual(1, restarted.used_today())
            self.assertEqual(1, restarted.open_slots())
            saved = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual({"day", "count", "slots"}, set(saved))
            self.assertNotIn(kept, json.dumps(saved))
            restarted.take(kept)
            self.assertEqual(0, phone_verifier.DailyLimit(path, 5, clock=lambda: NOW_MS + 86_400_000).used_today())
        finally:
            path.unlink(missing_ok=True)

    def test_without_a_maximum_there_is_no_limit(self):
        limit = phone_verifier.DailyLimit(None, 0, clock=lambda: NOW_MS)
        for _ in range(50):
            limit.take(limit.reserve()[0])
        self.assertEqual(50, limit.used_today())

    def test_the_certificate_must_be_for_the_own_keys(self):
        other = phone_verifier.public_keys(Ed25519PrivateKey.generate(), X25519PrivateKey.generate())
        certificate = directory_service.phone_keys_certificate(
            self.directory, other["attestationKey"], other["requestKey"])
        path = phone_verifier.Path(self.id().replace(".", "_") + ".json")
        path.write_text(json.dumps(certificate), encoding="utf-8")
        try:
            with self.assertRaises(ValueError):
                phone_verifier.read_certificate(path, self.keys)
            self.assertEqual(certificate, phone_verifier.read_certificate(path, other))
        finally:
            path.unlink()


if __name__ == "__main__":
    unittest.main()
