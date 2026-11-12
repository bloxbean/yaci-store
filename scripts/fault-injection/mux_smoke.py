#!/usr/bin/env python3
"""Offline self-test of the CBOR and forward parsing in fault-proxy.py."""
import hashlib, importlib.util, sys, types
src = open(sys.argv[1]).read().replace("asyncio.run(main())", "")
m = types.ModuleType("fp"); sys.modules["fp"] = m; exec(compile(src, "fp", "exec"), m.__dict__)
inner = b"\x82" + b"\x83\x1a\x00\x45\xbd\x10\x1a\x05\xf5\xe1\x00\x58\x20" + b"\xab"*32 + b"\x41\x00"
hdr = b"\x82\x06\xd8\x18\x58" + bytes([len(inner)]) + inner
tip = b"\x82" + m.point_bytes(99999999, b"\xcd"*32) + b"\x1a\x00\x45\xbd\x11"
fwd = b"\x83\x02" + hdr + tip
slot, h, t = m.parse_forward(fwd)
assert slot == 100000000, slot
assert h == hashlib.blake2b(inner, digest_size=32).digest()
assert t == tip
assert m.cbor_end(fwd) == len(fwd)
try:
    m.cbor_end(fwd[:-3]); raise SystemExit("expected Incomplete")
except m.Incomplete:
    pass
blk = b"\x82\x04\xd8\x18\x5a\x00\x00\x00\x05hello"
assert m.cbor_end(blk + b"\x81\x05") == len(blk)
assert m.cbor_end(b"\x9f\x01\x02\xff") == 4
print("parser self-test ok")
