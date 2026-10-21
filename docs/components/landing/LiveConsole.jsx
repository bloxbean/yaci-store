"use client";

import { useEffect, useRef, useState } from "react";
import { formatDuration, formatNumber, mainnetTip, useInView, useMainnetTip, usePrefersReducedMotion } from "./hooks";

const STORES = [
  { id: "block", label: "Block", c: "#3b8bff" },
  { id: "transaction", label: "Transaction", c: "#22d3ee" },
  { id: "utxo", label: "UTxO", c: "#2dd4bf" },
  { id: "assets", label: "Assets", c: "#34d399" },
  { id: "metadata", label: "Metadata", c: "#a78bfa" },
  { id: "script", label: "Script", c: "#f472b6" },
  { id: "staking", label: "Staking", c: "#fbbf24" },
  { id: "governance", label: "Governance", c: "#fb923c" },
  { id: "plugins", label: "Plugins", c: "#e879f9" },
];

// Real yaci-store event types and the stores that consume them.
const EVENTS = [
  { name: "TransactionEvent", stores: ["transaction", "utxo", "plugins"], p: 0.92, c: "#22d3ee" },
  { name: "MintBurnEvent", stores: ["assets"], p: 0.35, c: "#34d399" },
  { name: "AuxDataEvent", stores: ["metadata"], p: 0.35, c: "#a78bfa" },
  { name: "ScriptEvent", stores: ["script"], p: 0.3, c: "#f472b6" },
  { name: "CertificateEvent", stores: ["staking"], p: 0.28, c: "#fbbf24" },
  { name: "GovernanceEvent", stores: ["governance"], p: 0.12, c: "#fb923c" },
];

const MAX_LINES = 7;
const BLOCK_EVERY_MS = 2800;

export default function LiveConsole() {
  const tip = useMainnetTip();
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView();
  const [lines, setLines] = useState([]);
  const [hits, setHits] = useState({});
  const lineId = useRef(0);
  const timers = useRef([]);

  useEffect(() => {
    if (!inView || reduced) return;

    const later = (fn, ms) => timers.current.push(setTimeout(fn, ms));

    const emit = (evt, slot) => {
      const id = ++lineId.current;
      setLines((prev) => [...prev.slice(-(MAX_LINES - 1)), { id, slot, ...evt }]);
      setHits((prev) => {
        const next = { ...prev };
        evt.stores.forEach((s) => (next[s] = id));
        return next;
      });
      later(() => {
        setHits((prev) => {
          const next = { ...prev };
          evt.stores.forEach((s) => {
            if (next[s] === id) delete next[s];
          });
          return next;
        });
      }, 1100);
    };

    const block = () => {
      const { slot } = mainnetTip();
      emit({ name: "BlockHeaderEvent", stores: ["block"], c: "#7fb2ff" }, slot);
      const picked = EVENTS.filter((e) => Math.random() < e.p);
      picked.forEach((evt, i) => later(() => emit(evt, slot), 320 * (i + 1)));
    };

    block();
    const id = setInterval(block, BLOCK_EVERY_MS);
    return () => {
      clearInterval(id);
      timers.current.forEach(clearTimeout);
      timers.current = [];
    };
  }, [inView, reduced]);

  return (
    <div ref={ref}>
      <div className="ys-console-wrap">
        <div
          className="ys-console"
          role="img"
          aria-label="Illustration of Yaci Store indexing Cardano mainnet events into stores"
        >
          <div className="ys-console-bar">
            <div className="ys-dots" aria-hidden="true">
              <i />
              <i />
              <i />
            </div>
            <span className="ys-console-title">yaci-store · mainnet</span>
            <span className="ys-live">
              <i />
              LIVE
            </span>
          </div>

          <div className="ys-tip">
            <div>
              <span className="label">Epoch</span>
              <span className="value">{tip ? tip.epoch : "—"}</span>
            </div>
            <div>
              <span className="label">Slot</span>
              <span className="value slot">{tip ? formatNumber(tip.slot) : "—"}</span>
            </div>
            <div className="ys-progress">
              <div className="row">
                <span>
                  Epoch progress <b>{tip ? `${(tip.progress * 100).toFixed(1)}%` : ""}</b>
                </span>
                <span>{tip ? `${formatDuration(tip.remaining)} left` : ""}</span>
              </div>
              <div className="track">
                <div className="fill" style={{ width: tip ? `${tip.progress * 100}%` : "0%" }} />
              </div>
            </div>
          </div>

          <div className="ys-stores">
            {STORES.map((s) => (
              <div key={s.id} className={`ys-store${hits[s.id] ? " hit" : ""}`} style={{ "--c": s.c }}>
                <i />
                {s.label}
              </div>
            ))}
          </div>

          <div className="ys-log" aria-hidden="true">
            <div className="ys-log-inner">
              {lines.map((l) => (
                <div key={l.id} className="ys-log-line">
                  <span className="slot">{formatNumber(l.slot)}</span>
                  <span className="evt" style={{ "--c": l.c }}>
                    <b>{l.name}</b> <span>→ {l.stores.join(" · ")}</span>
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
      <p className="ys-console-note">
        Epoch and slot are computed live from mainnet genesis. The event stream is illustrative.
      </p>
    </div>
  );
}
