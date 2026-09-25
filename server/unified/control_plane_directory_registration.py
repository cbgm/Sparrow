#!/usr/bin/env python3
"""Opt-in registration of this server's EXISTING Control Plane root identity.

This PUBLIC-BUNDLE client never creates a signing key and has no directory admin
credential. The independent directory automatically publishes a verified registration.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, build_opener, HTTPRedirectHandler

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

from control_plane_directory_client import (
    DirectoryError, _decode_json, _normalized_directory_url, _plane_url,
    verify_envelope,
)

PROOF_DOMAIN = b"sparrow-control-plane-directory-registration-v1\n"
CHALLENGE_ID = re.compile(r"[A-Za-z0-9_-]{32}\Z")
MAX_RESPONSE = 512 * 1024


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        raise DirectoryError("Directory redirected a registration request")


def _request(opener, url: str, data: dict | None = None) -> dict:
    payload = None if data is None else _canonical(data)
    request = Request(url, data=payload,
                      headers={"Accept": "application/json", **({"Content-Type": "application/json"} if payload else {})},
                      method="GET" if payload is None else "POST")
    try:
        with opener.open(request, timeout=9) as response:
            if response.status not in ((200,) if payload is None else (201, 202)):
                raise DirectoryError("Unexpected Directory Server HTTP status")
            body = response.read(MAX_RESPONSE + 1)
            if len(body) > MAX_RESPONSE:
                raise DirectoryError("Directory Server response too large")
            parsed = _decode_json(body)
            if not isinstance(parsed, dict):
                raise DirectoryError("Directory Server response must be an object")
            return parsed
    except (HTTPError, URLError, TimeoutError) as exc:
        raise DirectoryError("Directory Server request failed: " + str(exc)[:150]) from exc


def _canonical(value: dict) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def _encode(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def load_existing_identity(path: Path) -> tuple[Ed25519PrivateKey, str, str]:
    """Read NodeIdentityStore's Java properties; do not create or regenerate it."""
    if not path.is_file():
        raise DirectoryError("Existing registry-root.identity not found; registration deferred")
    if path.stat().st_size > 16_384:
        raise DirectoryError("Unexpectedly large Control Plane identity file")
    fields = {}
    for line in path.read_text(encoding="iso-8859-1").splitlines():
        item = line.strip()
        if not item or item.startswith(('#', '!')):
            continue
        key, sep, value = item.partition('=')
        if sep and key.strip() in ('publicKey', 'privateKey'):
            # java.util.Properties.store() escapes '=' in property VALUES as
            # '\='. Standard Base64 often ends in '=' padding, so a raw
            # split-on-'=' parser rejects an otherwise valid Java Ed25519 key.
            # Decode only Java's escaped equals here; Base64 validation below
            # rejects every other backslash/escape or malformed value.
            fields[key.strip()] = value.strip().replace('\\=', '=')
    if set(fields) != {'publicKey', 'privateKey'}:
        raise DirectoryError("Control Plane identity file missing publicKey/privateKey")
    try:
        encoded_pub = base64.b64decode(fields['publicKey'], validate=True)
        encoded_priv = base64.b64decode(fields['privateKey'], validate=True)
        private = serialization.load_der_private_key(encoded_priv, password=None)
        if not isinstance(private, Ed25519PrivateKey):
            raise ValueError("identity is not Ed25519")
        public = private.public_key().public_bytes(serialization.Encoding.DER,
                                                  serialization.PublicFormat.SubjectPublicKeyInfo)
        if encoded_pub != public:
            raise ValueError("Control Plane public/private identity mismatch")
    except (ValueError, TypeError) as exc:
        raise DirectoryError("Existing Control Plane signing identity is invalid") from exc
    return private, _encode(public), hashlib.sha256(public).hexdigest()


def register(directory_url: str, plane_url: str, public_key: str, identity_file: Path,
             proof_root: Path, *, opener=None, now=None) -> str:
    directory_url = _normalized_directory_url(directory_url)
    plane_url = _plane_url(plane_url)
    private, encoded_pub, plane_id = load_existing_identity(identity_file)
    opener = opener or build_opener(NoRedirect())
    clock = int(time.time() * 1000) if now is None else now
    # Authenticate the endpoint using the independently provisioned trust pin
    # BEFORE sending any registration request. A changed URL cannot supply a key.
    snapshot = _request(opener, directory_url + '/v1/control-planes')
    contents = verify_envelope(snapshot, public_key, clock)
    if plane_id in contents['revokedControlPlaneIds']:
        raise DirectoryError("This Control Plane identity was revoked; operator recovery required")
    for entry in contents['controlPlanes']:
        if entry['controlPlaneId'] == plane_id:
            if entry['baseUrl'] != plane_url or entry['publicKey'] != encoded_pub:
                raise DirectoryError("Existing directory identity has a different endpoint; operator review required")
            return 'already-approved'
        if entry['baseUrl'] == plane_url:
            raise DirectoryError("Endpoint is already registered to another Control Plane identity")

    challenge = _request(opener, directory_url + '/v1/registrations/challenge', {
        'controlPlaneId': plane_id, 'baseUrl': plane_url, 'publicKey': encoded_pub,
    })
    required = {'challengeId', 'controlPlaneId', 'baseUrl', 'publicKey', 'expiresAtEpochMilliseconds'}
    challenge_id = challenge.get('challengeId')
    expires = challenge.get('expiresAtEpochMilliseconds')
    if (set(challenge) != required or not isinstance(challenge_id, str) or
            not CHALLENGE_ID.fullmatch(challenge_id) or
            challenge['controlPlaneId'] != plane_id or challenge['baseUrl'] != plane_url or
            challenge['publicKey'] != encoded_pub or type(expires) is not int or
            not clock < expires <= clock + 10 * 60_000 + 30_000):
        raise DirectoryError('Invalid, mismatched or expired registration challenge')
    signature = _encode(private.sign(PROOF_DOMAIN + _canonical(challenge)))
    proof = {**challenge, 'signature': signature}
    # Caddy serves ONLY this dedicated public proof path, never the identity
    # file. The identity stays in control-plane/secrets, outside this mount.
    proof_file = proof_root / '.well-known' / 'sparrow-directory-registration' / challenge_id
    proof_file.parent.mkdir(parents=True, exist_ok=True)
    if proof_file.is_symlink() or proof_file.exists():
        raise DirectoryError('Registration proof path already occupied')
    pending = None
    try:
        fd, name = tempfile.mkstemp(prefix='.pending-', dir=str(proof_file.parent))
        pending = Path(name)
        with os.fdopen(fd, 'wb') as stream:
            stream.write(_canonical(proof))
            stream.flush()
            os.fsync(stream.fileno())
        os.chmod(pending, 0o644)  # Caddy container reads the public proof only.
        os.replace(pending, proof_file)
        pending = None
        reply = _request(opener, directory_url + '/v1/registrations', {
            'challengeId': challenge_id, 'signature': signature,
        })
        if reply.get('registrationId') != challenge_id or reply.get('status') != 'approved':
            raise DirectoryError('Directory Server did not confirm approved registration')
        return 'registered (' + challenge_id + ')'
    finally:
        if pending is not None:
            pending.unlink(missing_ok=True)
        proof_file.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description='Register an existing public Sparrow Control Plane')
    # Explicit command-line parameters remain available for operator diagnostics.
    # Containerized registration receives ONLY these three non-secret config
    # values, plus a single read-only mount of the existing CP root identity.
    parser.add_argument('--directory-url', default=os.getenv('CONTROL_PLANE_DIRECTORY_URL', ''))
    parser.add_argument('--plane-url', default=os.getenv('SPARROW_REGISTRATION_PLANE_URL', ''))
    parser.add_argument('--public-key', default=os.getenv('CONTROL_PLANE_DIRECTORY_PUBLIC_KEY', ''))
    parser.add_argument('--identity-file', type=Path,
                        default=Path('/run/control-plane/registry-root.identity'))
    parser.add_argument('--proof-root', type=Path, default=Path('/proofs'))
    args = parser.parse_args()
    try:
        print(register(args.directory_url, args.plane_url, args.public_key,
                       args.identity_file, args.proof_root))
        return 0
    except (OSError, ValueError, TypeError, KeyError) as exc:
        print('Directory registration deferred: ' + str(exc)[:230], file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
