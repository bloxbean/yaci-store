#!/usr/bin/env python3
"""Fault-injecting TCP proxy. With --mux it understands Ouroboros node-to-node mux framing, so it can fault
individual mini-protocols and forge chain-sync / block-fetch messages.

usage: fault-proxy.py --listen PORT --target HOST:PORT --ctl PORT [--mux]

Control (one line per TCP connection on the ctl port, JSON reply), see faultctl.py:
  pass                 clear every fault, release everything held
  status               counters
  rst | fin            reset / gracefully close every open connection
  refuse on|off        stop listening (connection refused for new connections)
  freeze on|off        partition: stop reading both directions on all connections, new ones accepted but silent
  new-silent on|off    new connections are accepted and never answered; existing ones keep working
  new-rst on|off       new connections are reset straight after accept
  latency MS | throttle KBPS
 mux only:
  stall PROTO[,PROTO]  hold node->client frames of these protocols (2 chain-sync, 3 block-fetch); keep-alive still flows
  stall-after N        after N more blocks, stall block-fetch
  nofetch on|off       answer every RequestRange with MsgNoBlocks, without asking the node
  intersect-nf N       rewrite the next N IntersectFound into IntersectNotFound
  drop-block N         drop the next N MsgBlock (block-fetch)
  dup-block N          deliver the next N MsgBlock twice
  swap-block           swap the order of the next two MsgBlock
  corrupt-block N      replace the next N MsgBlock bodies with undecodable bytes
  skip-forward N       hide the next N chain-sync RollForward from the client (it never hears of those blocks)
  dup-forward          deliver the last RollForward a second time, on the next RequestNext
  rollback K           on the next RequestNext: forged RollBackward K blocks, then replay those K RollForwards
  rollback-noreplay K  same but without the replay: the client continues from the node's position
"""
import argparse
import asyncio
import collections
import hashlib
import json
import socket
import struct
import time

MAX_SEG = 12288
CHAINSYNC, BLOCKFETCH = 2, 3
RESP = 0x8000
REQUEST_NEXT = b"\x81\x00"
NO_BLOCKS = b"\x81\x03"


class Incomplete(Exception):
    pass


def _head(b, i):
    if i >= len(b):
        raise Incomplete
    ib = b[i]
    major, ai = ib >> 5, ib & 31
    i += 1
    if ai < 24:
        return major, ai, i
    if ai == 31:
        return major, None, i
    n = {24: 1, 25: 2, 26: 4, 27: 8}.get(ai)
    if n is None:
        raise ValueError(f"bad cbor {ib:#x}")
    if i + n > len(b):
        raise Incomplete
    return major, int.from_bytes(b[i:i + n], "big"), i + n


def cbor_end(b, i=0):
    """Index just past the CBOR item at i. Raises Incomplete when b does not hold all of it."""
    major, val, i = _head(b, i)
    if major in (0, 1):
        return i
    if major in (2, 3):
        if val is None:
            while True:
                if i >= len(b):
                    raise Incomplete
                if b[i] == 0xff:
                    return i + 1
                i = cbor_end(b, i)
        if i + val > len(b):
            raise Incomplete
        return i + val
    if major in (4, 5):
        if val is None:
            while True:
                if i >= len(b):
                    raise Incomplete
                if b[i] == 0xff:
                    return i + 1
                i = cbor_end(b, i)
                if major == 5:
                    i = cbor_end(b, i)
        for _ in range(val * (2 if major == 5 else 1)):
            i = cbor_end(b, i)
        return i
    if major == 6:
        return cbor_end(b, i)
    return i  # simple values: _head already consumed the argument


def cbor_uint(n):
    if n < 24:
        return bytes([n])
    for ai, size in ((24, 1), (25, 2), (26, 4), (27, 8)):
        if n < 1 << (8 * size):
            return bytes([ai]) + n.to_bytes(size, "big")
    raise ValueError(n)


def point_bytes(slot, hsh):
    return b"\x82" + cbor_uint(slot) + b"\x58\x20" + hsh


def parse_forward(p):
    """RollForward = [2, [era, tag24(bytes header)], tip]. Returns (slot, hash, tip bytes) for Shelley and later."""
    i = 2  # 0x83 0x02
    hdr_end = cbor_end(p, i)
    tip = bytes(p[hdr_end:])
    major, n, j = _head(p, i)
    if major != 4 or n != 2:
        return None, None, tip
    j = cbor_end(p, j)  # era
    major, tag, j = _head(p, j)
    if major != 6 or tag != 24:
        return None, None, tip
    major, ln, j = _head(p, j)
    inner = bytes(p[j:j + ln])
    _, _, k = _head(inner, 0)      # header = [body, sig]
    _, _, k = _head(inner, k)      # body = [blockNo, slot, ...]
    k = cbor_end(inner, k)         # blockNo
    _, slot, _ = _head(inner, k)
    return slot, hashlib.blake2b(inner, digest_size=32).digest(), tip


def frame(mode_proto, payload):
    ts = int(time.monotonic() * 1e6) & 0xffffffff
    return struct.pack(">IHH", ts, mode_proto, len(payload)) + payload


def frames(mode_proto, payload):
    return b"".join(frame(mode_proto, payload[i:i + MAX_SEG]) for i in range(0, len(payload), MAX_SEG))


class State:
    def __init__(self, a):
        self.a = a
        self.conns = set()
        self.server = None
        self.c = collections.Counter()
        self.gate = asyncio.Event()
        self.gate.set()
        self.silent = []
        self.forwards = collections.deque(maxlen=6000)  # (slot, hash, payload)
        self.tip = None
        self.last_slot = None
        self.clear()

    def clear(self):
        self.new_silent = self.new_rst = self.nofetch = False
        self.latency = self.throttle = 0
        self.stalled = set()
        self.stall_after = self.intersect_nf = self.drop_block = self.dup_block = 0
        self.corrupt_block = self.skip_forward = 0
        self.swap = 0
        self.swap_held = None
        self.inject = collections.deque()


class Conn:
    def __init__(self, st, cr, cw, ur, uw):
        self.st, self.cr, self.cw, self.ur, self.uw = st, cr, cw, ur, uw
        self.fetch_buf = bytearray()
        self.held = []
        self.closed = False
        self.s2c_lock = asyncio.Lock()   # frames must reach the client in order, also while held ones are released

    async def run(self):
        self.st.conns.add(self)
        self.st.c["conns_total"] += 1
        try:
            a = asyncio.create_task(self.c2s())
            b = asyncio.create_task(self.s2c())
            await asyncio.wait({a, b}, return_when=asyncio.FIRST_COMPLETED)
            a.cancel(); b.cancel()
        finally:
            self.st.conns.discard(self)
            self.close(False)

    def close(self, rst):
        if self.closed:
            return
        self.closed = True
        for w in (self.cw, self.uw):
            try:
                if rst:
                    w.get_extra_info("socket").setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack("ii", 1, 0))
                    w.transport.abort()
                else:
                    w.close()
            except Exception:
                pass

    async def out(self, w, data):
        st = self.st
        if st.latency:
            await asyncio.sleep(st.latency / 1000)
        if st.throttle:
            await asyncio.sleep(len(data) / (st.throttle * 1024))
        w.write(data)
        await w.drain()

    async def read_frames(self, r):
        hdr = await r.readexactly(8)
        ts, mp, ln = struct.unpack(">IHH", hdr)
        return mp, await r.readexactly(ln), hdr

    async def c2s(self):
        st = self.st
        try:
            if not st.a.mux:
                while data := await self.cr.read(65536):
                    await st.gate.wait()
                    await self.out(self.uw, data)
                return
            while True:
                mp, p, hdr = await self.read_frames(self.cr)
                await st.gate.wait()
                proto = mp & 0x7fff
                if proto == CHAINSYNC and bytes(p) == REQUEST_NEXT and st.inject:
                    msg = st.inject.popleft()
                    st.c["injected_chainsync"] += 1
                    await self.out(self.cw, frame(RESP | CHAINSYNC, msg))
                    continue
                if proto == BLOCKFETCH and st.nofetch and p[:2] == b"\x83\x00":
                    st.c["nofetch_answered"] += 1
                    await self.out(self.cw, frame(RESP | BLOCKFETCH, NO_BLOCKS))
                    continue
                await self.out(self.uw, hdr + p)
        except (asyncio.IncompleteReadError, ConnectionError, OSError):
            pass

    async def s2c(self):
        st = self.st
        try:
            if not st.a.mux:
                while data := await self.ur.read(65536):
                    await st.gate.wait()
                    await self.out(self.cw, data)
                return
            while True:
                mp, p, hdr = await self.read_frames(self.ur)
                await st.gate.wait()
                proto = mp & 0x7fff
                async with self.s2c_lock:
                    if proto in st.stalled:
                        self.held.append((mp, p, hdr))
                        st.c["frames_held"] += 1
                        continue
                    await self.release_locked()
                    await self.on_frame(mp, p, hdr)
        except (asyncio.IncompleteReadError, ConnectionError, OSError):
            pass

    async def release(self):
        async with self.s2c_lock:
            await self.release_locked()

    async def release_locked(self):
        # Frames of a protocol still stalled stay held; the others go out in the order they arrived
        st = self.st
        keep = []
        for f in self.held:
            if f[0] & 0x7fff in st.stalled:
                keep.append(f)
            else:
                await self.on_frame(*f)
        self.held = keep

    async def on_frame(self, mp, p, hdr):
        proto = mp & 0x7fff
        if proto == CHAINSYNC:
            await self.on_chainsync(mp, p, hdr)
        elif proto == BLOCKFETCH:
            await self.on_blockfetch(p)
        else:
            await self.out(self.cw, hdr + p)

    async def on_chainsync(self, mp, p, hdr):
        st = self.st
        pb = bytes(p)
        tag = pb[:2]
        if tag == b"\x83\x02":
            try:
                slot, hsh, tip = parse_forward(pb)
            except Exception:
                st.c["forward_parse_errors"] += 1
                slot, hsh, tip = None, None, None
            st.c["forwards"] += 1
            if tip:
                st.tip = tip
            if st.skip_forward > 0:
                st.skip_forward -= 1
                st.c["forwards_skipped"] += 1
                self.uw.write(frame(CHAINSYNC, REQUEST_NEXT))
                await self.uw.drain()
                return
            if slot is not None:
                st.forwards.append((slot, hsh, pb))
                st.last_slot = slot
        elif tag == b"\x83\x03":
            st.c["natural_rollbacks"] += 1
            try:
                _, n, i = _head(pb, 2)
                if n == 2:
                    _, rb_slot, _ = _head(pb, i)
                    while st.forwards and st.forwards[-1][0] > rb_slot:
                        st.forwards.pop()
            except Exception:
                pass
        elif tag == b"\x83\x05" and st.intersect_nf > 0:
            st.intersect_nf -= 1
            st.c["intersect_rewritten"] += 1
            tip = pb[cbor_end(pb, 2):]
            await self.out(self.cw, frame(RESP | CHAINSYNC, b"\x82\x06" + tip))
            return
        await self.out(self.cw, hdr + p)

    async def on_blockfetch(self, p):
        st = self.st
        self.fetch_buf += p
        while self.fetch_buf:
            try:
                end = cbor_end(self.fetch_buf)
            except Incomplete:
                return
            msg = bytes(self.fetch_buf[:end])
            del self.fetch_buf[:end]
            if msg[:2] == b"\x82\x04":
                st.c["blocks"] += 1
                if st.stall_after > 0:
                    st.stall_after -= 1
                    if st.stall_after == 0:
                        st.stalled.add(BLOCKFETCH)
                        st.c["stall_after_fired"] += 1
                if st.drop_block > 0:
                    st.drop_block -= 1
                    st.c["blocks_dropped"] += 1
                    continue
                if st.corrupt_block > 0:
                    st.corrupt_block -= 1
                    st.c["blocks_corrupted"] += 1
                    msg = b"\x82\x04\xd8\x18\x58\x40" + b"\xff" * 64
                if st.dup_block > 0:
                    st.dup_block -= 1
                    st.c["blocks_duplicated"] += 1
                    await self.out(self.cw, frames(RESP | BLOCKFETCH, msg))
                if st.swap == 2:
                    st.swap = 1
                    st.swap_held = msg
                    st.c["blocks_swapped"] += 1
                    continue
                if st.swap == 1:
                    st.swap = 0
                    await self.out(self.cw, frames(RESP | BLOCKFETCH, msg))
                    msg = st.swap_held
                    st.swap_held = None
            await self.out(self.cw, frames(RESP | BLOCKFETCH, msg))


async def on_client(st, cr, cw):
    if st.new_rst:
        st.c["new_rst"] += 1
        s = cw.get_extra_info("socket")
        s.setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack("ii", 1, 0))
        cw.transport.abort()
        return
    if st.new_silent or not st.gate.is_set():
        st.c["new_silent"] += 1
        st.silent.append(cw)
        try:
            while await cr.read(65536):
                pass
        except (ConnectionError, OSError):
            pass
        return
    h, p = st.a.target.rsplit(":", 1)
    try:
        ur, uw = await asyncio.open_connection(h, int(p))
    except OSError:
        st.c["upstream_connect_failed"] += 1
        cw.close()
        return
    await Conn(st, cr, cw, ur, uw).run()


async def set_listen(st, on):
    if on and st.server is None:
        st.server = await asyncio.start_server(lambda r, w: on_client(st, r, w), "127.0.0.1", st.a.listen,
                                               reuse_address=True)
    elif not on and st.server is not None:
        st.server.close()
        st.server = None


async def control(st, r, w):
    def b(x):
        return x == "on"
    try:
        parts = (await r.readline()).decode().split()
        cmd, args = parts[0], parts[1:]
        res = {"ok": True}
        if cmd == "pass":
            st.clear()
            st.gate.set()
            await set_listen(st, True)
            for c in list(st.conns):
                await c.release()
            for sw in st.silent:
                sw.close()
            st.silent.clear()
        elif cmd == "status":
            res = dict(st.c, conns=len(st.conns), listening=st.server is not None, frozen=not st.gate.is_set(),
                       stalled=sorted(st.stalled), inject=len(st.inject), ring=len(st.forwards), last_slot=st.last_slot)
        elif cmd in ("rst", "fin"):
            for c in list(st.conns):
                c.close(cmd == "rst")
            res["closed"] = True
        elif cmd == "refuse":
            await set_listen(st, not b(args[0]))
        elif cmd == "freeze":
            st.gate.clear() if b(args[0]) else st.gate.set()
        elif cmd == "new-silent":
            st.new_silent = b(args[0])
        elif cmd == "new-rst":
            st.new_rst = b(args[0])
        elif cmd == "latency":
            st.latency = int(args[0])
        elif cmd == "throttle":
            st.throttle = int(args[0])
        elif cmd == "stall":
            st.stalled = {int(x) for x in args[0].split(",")}
        elif cmd == "stall-after":
            st.stall_after = int(args[0])
        elif cmd == "nofetch":
            st.nofetch = b(args[0])
        elif cmd == "intersect-nf":
            st.intersect_nf = int(args[0])
        elif cmd == "drop-block":
            st.drop_block = int(args[0])
        elif cmd == "dup-block":
            st.dup_block = int(args[0])
        elif cmd == "swap-block":
            st.swap = 2
        elif cmd == "corrupt-block":
            st.corrupt_block = int(args[0])
        elif cmd == "skip-forward":
            st.skip_forward = int(args[0])
        elif cmd == "dup-forward":
            if not st.forwards:
                raise ValueError("no forwards seen yet")
            st.inject.append(st.forwards[-1][2])
        elif cmd in ("rollback", "rollback-noreplay"):
            k = int(args[0])
            if len(st.forwards) <= k or st.tip is None:
                raise ValueError(f"only {len(st.forwards)} forwards in the ring")
            items = list(st.forwards)[-(k + 1):]
            base = items[0]
            st.inject.append(b"\x83\x03" + point_bytes(base[0], base[1]) + st.tip)
            if cmd == "rollback":
                st.inject.extend(x[2] for x in items[1:])
            res.update(point_slot=base[0], point_hash=base[1].hex())
        else:
            raise ValueError(f"unknown command {cmd}")
    except Exception as e:
        res = {"ok": False, "error": repr(e)}
    w.write((json.dumps(res) + "\n").encode())
    await w.drain()
    w.close()


async def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--listen", type=int, required=True)
    ap.add_argument("--target", required=True)
    ap.add_argument("--ctl", type=int, required=True)
    ap.add_argument("--mux", action="store_true")
    a = ap.parse_args()
    st = State(a)
    await set_listen(st, True)
    ctl = await asyncio.start_server(lambda r, w: control(st, r, w), "127.0.0.1", a.ctl)
    async with ctl:
        await ctl.serve_forever()


asyncio.run(main())
