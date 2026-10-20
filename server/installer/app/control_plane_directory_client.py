"""Verified, offline-first discovery client for Sparrow's independent directory.

This is a PUBLIC client. It contains no registration, signing or admin secrets.
The configured public key is an independently provisioned Ed25519 trust anchor;
changing the network URL never replaces that anchor.
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
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey

PROTOCOL = "sparrow-control-plane-directory-v1"
SNAPSHOT_DOMAIN = b"sparrow-control-plane-directory-snapshot-v1\n"
MAX_RESPONSE_BYTES = 512 * 1024
MAX_SNAPSHOT_AGE_MS = 24 * 60 * 60 * 1000
MAX_CLOCK_SKEW_MS = 5 * 60 * 1000
HEX_ID = re.compile(r"[0-9a-f]{64}\Z")


class DirectoryError(ValueError):
    pass


def _pairs_without_duplicates(items):
    result = {}
    for name, value in items:
        if name in result:
            raise DirectoryError("Duplicate JSON property")
        result[name] = value
    return result


def _decode_json(data):
    return json.loads(data, object_pairs_hook=_pairs_without_duplicates,
                      parse_constant=lambda _: (_ for _ in ()).throw(DirectoryError("Non-finite JSON number")))


def _decode_base64(value, length=None):
    if not isinstance(value, str) or not value or len(value) > 512:
        raise DirectoryError("Invalid public key or signature encoding")
    try:
        raw = base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)
    except (ValueError, base64.binascii.Error) as exc:
        raise DirectoryError("Invalid base64url") from exc
    if base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii") != value or (length is not None and len(raw) != length):
        raise DirectoryError("Noncanonical base64url or wrong value length")
    return raw


def _public_key(encoded):
    raw = _decode_base64(encoded)
    try:
        key = serialization.load_der_public_key(raw)
    except ValueError as exc:
        raise DirectoryError("Invalid DER-encoded directory public key") from exc
    if not isinstance(key, Ed25519PublicKey) or key.public_bytes(
            serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo) != raw:
        raise DirectoryError("Directory public key must be a canonical DER Ed25519 key")
    identifier = hashlib.sha256(raw).hexdigest()
    return key, identifier


def _normalized_directory_url(value):
    parsed = urlsplit(value)
    if (parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password or
            parsed.path not in ("", "/") or parsed.query or parsed.fragment or
            parsed.port not in (None, 443)):
        raise DirectoryError("Directory URL must be an HTTPS origin without credentials, path, or query")
    return "https://" + parsed.hostname.lower().rstrip(".")


def _plane_url(value):
    if not isinstance(value, str) or len(value) > 253:
        raise DirectoryError("Invalid Control Plane URL")
    parsed = urlsplit(value)
    if (parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password or
            parsed.path not in ("", "/") or parsed.query or parsed.fragment or parsed.port not in (None, 443)):
        raise DirectoryError("Directory contains a non-HTTPS Control Plane origin")
    return "https://" + parsed.hostname.lower().rstrip(".")


def verify_envelope(envelope, public_key, now_ms, *, allow_expired=False):
    """Return signed payload after crypto, time, shape and identity checks."""
    key, trusted_id = _public_key(public_key)
    if not isinstance(envelope, dict) or set(envelope) != {"payload", "keyId", "signature"}:
        raise DirectoryError("Invalid directory envelope")
    if envelope["keyId"] != trusted_id:
        raise DirectoryError("Directory signing-key ID does not match configured trust anchor")
    payload = envelope["payload"]
    if not isinstance(payload, dict) or set(payload) != {
        "protocol", "version", "generatedAtEpochMilliseconds", "validUntilEpochMilliseconds",
        "controlPlanes", "revokedControlPlaneIds"
    } or payload["protocol"] != PROTOCOL:
        raise DirectoryError("Unsupported directory payload")
    try:
        key.verify(_decode_base64(envelope["signature"], 64),
                   SNAPSHOT_DOMAIN + json.dumps(payload, sort_keys=True, separators=(",", ":"),
                                                ensure_ascii=False).encode("utf-8"))
    except InvalidSignature as exc:
        raise DirectoryError("Directory signature verification failed") from exc

    version, issued, valid = (payload["version"], payload["generatedAtEpochMilliseconds"],
                              payload["validUntilEpochMilliseconds"])
    if (type(version) is not int or version < 0 or type(issued) is not int or type(valid) is not int or
            issued < 0 or valid <= issued or valid - issued > MAX_SNAPSHOT_AGE_MS or
            issued > now_ms + MAX_CLOCK_SKEW_MS):
        raise DirectoryError("Invalid directory version or timestamps")
    if not allow_expired and valid <= now_ms:
        raise DirectoryError("Directory snapshot has expired")
    planes, revoked = payload["controlPlanes"], payload["revokedControlPlaneIds"]
    if not isinstance(planes, list) or not isinstance(revoked, list) or len(planes) > 2048 or len(revoked) > 4096:
        raise DirectoryError("Directory entries exceed limits")
    if any(not isinstance(entry, str) or not HEX_ID.fullmatch(entry) for entry in revoked) or len(set(revoked)) != len(revoked):
        raise DirectoryError("Invalid or duplicate revoked identity")
    identities, urls = set(), set()
    for entry in planes:
        if not isinstance(entry, dict) or set(entry) != {"controlPlaneId", "baseUrl", "publicKey"}:
            raise DirectoryError("Invalid directory plane entry")
        identity = entry["controlPlaneId"]
        if not isinstance(identity, str) or not HEX_ID.fullmatch(identity):
            raise DirectoryError("Invalid plane identity")
        _, entry_id = _public_key(entry["publicKey"])
        if entry_id != identity or identity in identities or identity in revoked:
            raise DirectoryError("Mismatched, duplicate or revoked plane identity")
        url = _plane_url(entry["baseUrl"])
        if entry["baseUrl"] != url or url in urls:
            raise DirectoryError("Noncanonical or duplicate Control Plane URL")
        identities.add(identity)
        urls.add(url)
    return payload


def _load_cache(path, key, now_ms):
    if not path.is_file():
        return None
    try:
        if path.stat().st_size > MAX_RESPONSE_BYTES:
            raise DirectoryError("Cached directory is too large")
        envelope = _decode_json(path.read_bytes())
        payload = verify_envelope(envelope, key, now_ms, allow_expired=True)
        return envelope, payload
    except (OSError, ValueError, TypeError, KeyError) as exc:
        raise DirectoryError("Cached directory cannot be authenticated") from exc


def _store_atomically(path, envelope):
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, pending = tempfile.mkstemp(prefix=".directory-", dir=str(path.parent))
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as stream:
            os.chmod(pending, 0o600)
            json.dump(envelope, stream, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(pending, path)
    finally:
        if os.path.exists(pending):
            os.unlink(pending)


def discover(directory_url, public_key, cache_file, *, fetch=None, now_ms=None):
    """Return (verified-plane-URLs, status); outage never destroys signed cache.

    An expired cached snapshot is usable for *discovery of previously verified*
    identities, not for enrolling/rotating trust; consumers must enforce that.
    Revoked entries in the highest cached version are never resurrected by a
    lower-version response. Invalid cache fails closed rather than falling back
    to unverified legacy address JSON.
    """
    if not public_key:
        raise DirectoryError("Missing pinned CONTROL_PLANE_DIRECTORY_PUBLIC_KEY")
    _, pin_id = _public_key(public_key)
    url = _normalized_directory_url(directory_url)
    cache_file = Path(cache_file)
    now = int(time.time() * 1000) if now_ms is None else now_ms
    # A corrupt or untrusted cache must never be consumed, but a fresh,
    # independently authenticated response may repair it without a reinstall.
    try:
        cached = _load_cache(cache_file, public_key, now)
    except DirectoryError:
        cached = None
    status = "verified-cache" if cached else "no-verified-cache"
    try:
        if fetch is None:
            request = Request(url + "/v1/control-planes", headers={"Accept": "application/json"})
            with urlopen(request, timeout=8) as response:
                if response.status != 200:
                    raise DirectoryError("Directory endpoint returned non-200")
                document = response.read(MAX_RESPONSE_BYTES + 1)
        else:
            document = fetch(url + "/v1/control-planes")
        if len(document) > MAX_RESPONSE_BYTES:
            raise DirectoryError("Directory response is too large")
        incoming = _decode_json(document)
        payload = verify_envelope(incoming, public_key, now)
        if cached:
            previous = cached[1]
            if payload["version"] < previous["version"]:
                raise DirectoryError("Directory version rollback detected")
            if (payload["version"] == previous["version"] and
                    (payload["controlPlanes"] != previous["controlPlanes"] or
                     payload["revokedControlPlaneIds"] != previous["revokedControlPlaneIds"])):
                raise DirectoryError("Directory contents changed without a version increment")
            if not set(previous["revokedControlPlaneIds"]).issubset(payload["revokedControlPlaneIds"]):
                raise DirectoryError("Previously revoked directory identity was silently reinstated")
        _store_atomically(cache_file, incoming)
        cached = incoming, payload
        status = "verified-live"
    except (OSError, ValueError, TypeError, KeyError) as exc:
        if cached is None:
            raise DirectoryError("Directory unavailable/untrusted and no verified cache exists") from exc
        status = "verified-cache (live refresh rejected/unavailable)"
    return [entry["baseUrl"] for entry in cached[1]["controlPlanes"]], status


def main():
    parser = argparse.ArgumentParser(description="Authenticated Sparrow Control Plane directory client")
    parser.add_argument("--url", required=True, help="directory HTTPS origin")
    parser.add_argument("--public-key", required=True, help="pinned Ed25519 DER public key (base64url)")
    parser.add_argument("--cache", type=Path, required=True, help="persistent verified cache JSON path")
    args = parser.parse_args()
    try:
        urls, status = discover(args.url, args.public_key, args.cache)
    except DirectoryError as exc:
        print("Verified Control Plane directory unavailable: " + str(exc), file=sys.stderr)
        return 1
    print(json.dumps({"controlPlanes": urls, "status": status}, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
