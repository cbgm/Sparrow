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

    def request(self, token: str, routing_id: str = ROUTING_ID) -> tuple[bytes, bytes]:
        reply_key = os.urandom(32)
        plaintext = json.dumps({"token": token, "routingId": routing_id, "replyKey": b64(reply_key)}).encode()
        sealed = seal(plaintext, phone_verifier.decode(self.keys["requestKey"], 32), phone_verifier.REQUEST_CONTEXT)
        return sealed, reply_key

    def attest(self, token: str) -> dict:
        sealed, reply_key = self.request(token)
        return json.loads(ChaCha20Poly1305(reply_key).decrypt(bytes(12), self.verifier.attest(sealed), None))

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

    def test_the_daily_maximum_refuses_further_verifications_until_the_next_day(self):
        now = [NOW_MS]
        self.verifier.clock = lambda: now[0]
        self.verifier.limit = phone_verifier.DailyLimit(None, 2, clock=lambda: now[0])
        self.attest(self.google.token())
        self.attest(self.google.token())

        with self.assertRaises(phone_verifier.PhoneVerificationLimitReached):
            self.attest(self.google.token())
        now[0] += 24 * 60 * 60 * 1000
        self.attest(self.google.token())
        self.assertEqual(1, self.verifier.limit.used_today())

    def test_failed_verifications_do_not_count(self):
        self.verifier.limit = phone_verifier.DailyLimit(None, 1, clock=lambda: NOW_MS)
        with self.assertRaises(ValueError):
            self.attest(self.google.token(firebase={"sign_in_provider": "password"}))

        self.attest(self.google.token())
        self.assertEqual(1, self.verifier.limit.used_today())

    def test_the_count_of_the_day_survives_a_restart_and_holds_nothing_else(self):
        path = phone_verifier.Path(self.id().replace(".", "_") + ".json")
        try:
            phone_verifier.DailyLimit(path, 5, clock=lambda: NOW_MS).take()
            restarted = phone_verifier.DailyLimit(path, 5, clock=lambda: NOW_MS)

            self.assertEqual(1, restarted.used_today())
            self.assertEqual({"day", "count"}, set(json.loads(path.read_text(encoding="utf-8"))))
            self.assertEqual(0, phone_verifier.DailyLimit(path, 5, clock=lambda: NOW_MS + 86_400_000).used_today())
        finally:
            path.unlink(missing_ok=True)

    def test_without_a_maximum_there_is_no_limit(self):
        limit = phone_verifier.DailyLimit(None, 0, clock=lambda: NOW_MS)
        for _ in range(50):
            limit.take()
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
