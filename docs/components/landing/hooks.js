"use client";

import { useEffect, useRef, useState } from "react";

// Mainnet Shelley genesis: slot 4,492,800 started epoch 208 at 2020-07-29T21:44:51Z,
// and every slot since then is one second, 432,000 slots per epoch.
const SHELLEY_START_SLOT = 4492800;
const SHELLEY_START_TIME = 1596059091;
const SHELLEY_START_EPOCH = 208;
const EPOCH_LENGTH = 432000;

export function mainnetTip(nowMs = Date.now()) {
  const slot = SHELLEY_START_SLOT + Math.floor(nowMs / 1000) - SHELLEY_START_TIME;
  const sinceShelley = slot - SHELLEY_START_SLOT;
  const slotInEpoch = sinceShelley % EPOCH_LENGTH;
  return {
    slot,
    epoch: SHELLEY_START_EPOCH + Math.floor(sinceShelley / EPOCH_LENGTH),
    slotInEpoch,
    progress: slotInEpoch / EPOCH_LENGTH,
    remaining: EPOCH_LENGTH - slotInEpoch,
  };
}

/** Current mainnet tip, recomputed every second. Null until mounted so SSR output stays stable. */
export function useMainnetTip() {
  const [tip, setTip] = useState(null);
  useEffect(() => {
    setTip(mainnetTip());
    const id = setInterval(() => setTip(mainnetTip()), 1000);
    return () => clearInterval(id);
  }, []);
  return tip;
}

/** True while the element is on screen; used to pause timers for off-screen illustrations. */
export function useInView(options = { rootMargin: "80px" }) {
  const ref = useRef(null);
  const [inView, setInView] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el || typeof IntersectionObserver === "undefined") {
      setInView(true);
      return;
    }
    const io = new IntersectionObserver(([entry]) => setInView(entry.isIntersecting), options);
    io.observe(el);
    return () => io.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return [ref, inView];
}

/** Runs `fn` every `ms` while `active` is true. */
export function useInterval(fn, ms, active = true) {
  const saved = useRef(fn);
  saved.current = fn;
  useEffect(() => {
    if (!active) return;
    const id = setInterval(() => saved.current(), ms);
    return () => clearInterval(id);
  }, [ms, active]);
}

export function usePrefersReducedMotion() {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    setReduced(mq.matches);
    const onChange = () => setReduced(mq.matches);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);
  return reduced;
}

export const formatNumber = (n) => n.toLocaleString("en-US");

export function formatDuration(seconds) {
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}
