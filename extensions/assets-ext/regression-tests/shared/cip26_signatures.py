"""
CIP-26 attestation signatures, verified the way a client verifies them.

Each property of a registry entry is signed over

    blake2b-256( blake2b-256(cbor(subject)) | blake2b-256(cbor(property name))
               | blake2b-256(cbor(value)) | blake2b-256(cbor(sequence number)) )

with the subject as the registry file writes it: the same asset written in another case is another CBOR text, so a
signature made over one does not verify against the other. A value is a CBOR text, decimals an unsigned integer, and the
logo the image itself, a CBOR byte string of the base64 value decoded (as the registry's signing tool does it).
"""

import base64
import hashlib

from nacl.exceptions import BadSignatureError
from nacl.signing import VerifyKey

SIGNED_PROPERTIES = ("name", "description", "ticker", "url", "decimals", "logo")


def _blake2b256(data: bytes) -> bytes:
    return hashlib.blake2b(data, digest_size=32).digest()


def _cbor_head(major: int, length: int) -> bytes:
    if length < 24:
        return bytes([major << 5 | length])
    for additional, size in ((24, 1), (25, 2), (26, 4), (27, 8)):
        if length < 1 << (8 * size):
            return bytes([major << 5 | additional]) + length.to_bytes(size, "big")
    raise ValueError(f"too long for CBOR: {length}")


def _cbor(value) -> bytes:
    if isinstance(value, bytes):
        return _cbor_head(2, len(value)) + value
    if isinstance(value, bool):
        raise TypeError("a CIP-26 value is a text, an unsigned integer or bytes, not a boolean")
    if isinstance(value, int):
        return _cbor_head(0, value)
    encoded = value.encode("utf-8")
    return _cbor_head(3, len(encoded)) + encoded


def message(subject: str, prop: str, value, sequence_number: int) -> bytes:
    """The message a CIP-26 attestation signature is made over."""
    if prop == "decimals":
        value = int(value)
    elif prop == "logo":
        value = base64.b64decode(value)
    return _blake2b256(b"".join(_blake2b256(_cbor(part)) for part in (subject, prop, value, int(sequence_number))))


def verify_entry(entry: dict, subject: str | None = None) -> tuple[int, list[str]]:
    """
    Verify every signature of a CIP-26 entry as an API serves it (V1, or standards.cip26 in V2): each property is
    {value, sequenceNumber, signatures: [{signature, publicKey}]}. The subject is the entry's own unless one is given.

    :return: how many signatures were checked, and a description of each one that does not verify
    """
    subject = entry["subject"] if subject is None else subject
    checked, failed = 0, []
    for prop in SIGNED_PROPERTIES:
        item = entry.get(prop)
        if not item or not item.get("signatures"):
            continue
        signed = message(subject, prop, item["value"], item.get("sequenceNumber") or 0)
        for signature in item["signatures"]:
            checked += 1
            try:
                VerifyKey(bytes.fromhex(signature["publicKey"])).verify(signed, bytes.fromhex(signature["signature"]))
            except (BadSignatureError, ValueError):
                failed.append(f"{prop} (key {signature['publicKey'][:16]}...)")
    return checked, failed
