"use client";

import { Fragment, useEffect, useState } from "react";
import { Check } from "./icons";
import { useInView, useInterval, usePrefersReducedMotion } from "./hooks";

/* ---------------- Modular stores: toggles + dependency diff ---------------- */

const STARTERS = ["utxo", "block", "transaction", "assets", "metadata", "script", "staking", "epoch", "governance", "adapot", "account", "submit"];
const INITIAL_ON = ["utxo", "block", "transaction", "assets", "metadata"];

export function StoreToggles() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [on, setOn] = useState(() => new Set(INITIAL_ON));
  const [changes, setChanges] = useState([
    { id: 1, op: "+", name: "utxo" },
    { id: 2, op: "+", name: "metadata" },
    { id: 3, op: "+", name: "assets" },
  ]);

  const flip = (name) => {
    const op = on.has(name) ? "-" : "+";
    const next = new Set(on);
    op === "+" ? next.add(name) : next.delete(name);
    setOn(next);
    setChanges((c) => [...c.slice(-2), { id: Date.now() + Math.random(), op, name }]);
  };

  useInterval(() => flip(STARTERS[Math.floor(Math.random() * STARTERS.length)]), 1700, inView && !reduced);

  return (
    <div ref={ref}>
      <div className="ys-toggles">
        {STARTERS.map((s) => (
          <button
            type="button"
            key={s}
            className={`ys-toggle${on.has(s) ? " on" : ""}`}
            onClick={() => flip(s)}
            aria-pressed={on.has(s)}
            style={{ font: "inherit", fontFamily: "var(--font-mono)", fontSize: "0.74rem", cursor: "pointer", textAlign: "left" }}
          >
            {s}
            <span className="sw" />
          </button>
        ))}
      </div>
      <div className="ys-dep" aria-label="Gradle dependencies for the selected stores">
        <span className="n">dependencies</span> {"{"}
        {"\n"}
        {changes.map((c) => (
          <Fragment key={c.id}>
            <span className="flash">
              <span className={c.op === "+" ? "s" : "k"}>{c.op}</span>
              {"   "}implementation <span className="s">&apos;com.bloxbean.cardano:yaci-store-{c.name}-spring-boot-starter:&lt;version&gt;&apos;</span>
            </span>
            {"\n"}
          </Fragment>
        ))}
        {"}"}
      </div>
    </div>
  );
}

/* ---------------- Blockfrost-compatible API terminal ---------------- */

const BF_CMD = "curl localhost:8080/api/v1/blockfrost/blocks/latest";
const BF_FIELDS = [
  ["time", 5],
  ["height", 4],
  ["hash", 12],
  ["slot", 6],
  ["epoch", 3],
  ["epoch_slot", 4],
  ["tx_count", 2],
];

export function BlockfrostTerm() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [typed, setTyped] = useState(reduced ? BF_CMD.length : 0);
  const [shown, setShown] = useState(reduced ? BF_FIELDS.length + 2 : 0);

  useEffect(() => {
    if (reduced) {
      setTyped(BF_CMD.length);
      setShown(BF_FIELDS.length + 2);
    }
  }, [reduced]);

  useInterval(
    () => {
      if (typed < BF_CMD.length) setTyped((t) => Math.min(BF_CMD.length, t + 2));
      else if (shown < BF_FIELDS.length + 2) setShown((s) => s + 1);
      else if (shown < BF_FIELDS.length + 40) setShown((s) => s + 1); // hold
      else {
        setTyped(0);
        setShown(0);
      }
    },
    typed < BF_CMD.length ? 45 : 110,
    inView && !reduced
  );

  const done = typed >= BF_CMD.length;
  const rows = Math.min(shown, BF_FIELDS.length + 2);

  return (
    <div className="ys-term" ref={ref}>
      <div className="ys-term-bar">
        <div className="ys-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </div>
        GET /blocks/latest
      </div>
      <div className="ys-term-body" style={{ minHeight: 232 }}>
        <span className="p">$ </span>
        {BF_CMD.slice(0, typed)}
        {!done && <span className="ys-caret" />}
        {"\n"}
        {rows > 0 && <div className="ys-json-line">{"{"}</div>}
        {BF_FIELDS.slice(0, Math.max(0, rows - 1)).map(([k, w], i) => (
          <div key={k} className="ys-json-line">
            {"  "}
            <span className="k">&quot;{k}&quot;</span>: <span className="ys-skel" style={{ width: `${w * 0.6}em` }} />
            {i < BF_FIELDS.length - 1 ? "," : ""}
          </div>
        ))}
        {rows === BF_FIELDS.length + 2 && <div className="ys-json-line">{"}"}</div>}
      </div>
    </div>
  );
}

/* ---------------- Analytics: SQL + bar chart ---------------- */

const BARS = [0.42, 0.55, 0.5, 0.63, 0.58, 0.72, 0.66, 0.8, 0.74, 0.86, 0.78, 0.92, 0.88, 1];

export function AnalyticsArt() {
  return (
    <div className="ys-term">
      <div className="ys-term-bar">
        <div className="ys-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </div>
        POST /api/v1/analytics/query/sql
      </div>
      <div className="ys-term-body">
        <span className="m">SELECT</span> epoch, <span className="y">count</span>(*) <span className="m">AS</span> blocks{"\n"}
        <span className="m">FROM</span> block <span className="m">WHERE</span> epoch &gt;= <span className="n">500</span>{"\n"}
        <span className="m">GROUP BY</span> epoch <span className="m">ORDER BY</span> epoch
      </div>
      <div className="ys-chart" aria-hidden="true">
        {BARS.map((h, i) => (
          <i key={i} style={{ height: `${h * 100}%`, "--i": i }} />
        ))}
      </div>
    </div>
  );
}

/* ---------------- MCP: agent conversation ---------------- */

const TOOLS = ["analytics-list-tables", "analytics-describe-table", "analytics-execute-sql"];

export function McpChat() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  // step: 0 = question only, 1..3 = tool i-1 running, 4 = all done + typing, 5 = answer, then hold
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (reduced) setStep(5);
  }, [reduced]);

  useInterval(() => setStep((s) => (s >= 9 ? 0 : s + 1)), 1100, inView && !reduced);

  const s = Math.min(step, 5);
  const toolState = (i) => (s > i + 1 ? "done" : s === i + 1 ? "running" : "");

  return (
    <div className="ys-chat" ref={ref}>
      <div className="ys-msg user">How many blocks were produced per epoch since epoch 500?</div>
      {TOOLS.map((t, i) => (
        <div key={t} className={`ys-tool ${toolState(i)}`}>
          <span className="st">
            <Check />
          </span>
          {t}
        </div>
      ))}
      <div className="ys-msg agent" style={{ minHeight: 44 }}>
        {s < 4 ? (
          <span style={{ color: "var(--faint)" }}>Waiting for tools…</span>
        ) : s === 4 ? (
          <span className="ys-typing" aria-label="typing">
            <i />
            <i />
            <i />
          </span>
        ) : (
          <span className="ys-fade-swap">Done. I queried the exported <code>block</code> table and grouped it by epoch. The results are ready.</span>
        )}
      </div>
    </div>
  );
}

/* ---------------- Plugins: language tabs ---------------- */

const PLUGINS = [
  {
    lang: "MVEL",
    code: [
      "filters:",
      "  metadata.save:",
      '    - name: "NFT Metadata Filter"',
      "      lang: mvel",
      '      expression: label == "721"',
    ],
  },
  {
    lang: "SpEL",
    code: [
      "filters:",
      "  utxo.unspent.save:",
      '    - name: "High Value UTXO Filter"',
      "      lang: spel",
      "      expression: lovelaceAmount > 1000000000",
    ],
  },
  {
    lang: "JavaScript",
    code: [
      "post-actions:",
      "  transaction.save:",
      '    - name: "Webhook Notification"',
      "      lang: js",
      "      inline-script: |",
      "        http.postJson(webhookUrl, { count: items.length });",
    ],
  },
  {
    lang: "Python",
    code: [
      "scripts:",
      "  - id: utxo_utilities",
      "    lang: python",
      "    file: /app/plugins/scripts/utxo.py",
      "    enable-pool: true",
    ],
  },
];

function YamlLine({ line }) {
  const m = line.match(/^(\s*-?\s*)([\w.-]+)(:)(.*)$/);
  if (!m) return <span className="c">{line}</span>;
  const [, indent, key, colon, rest] = m;
  const val = rest.trim();
  let valEl = rest;
  if (/^".*"$/.test(val)) valEl = <> <span className="s">{val}</span></>;
  else if (/^(mvel|spel|js|python)$/.test(val)) valEl = <> <span className="y">{val}</span></>;
  else if (val === "true") valEl = <> <span className="n">{val}</span></>;
  return (
    <>
      {indent}
      <span className="k">{key}</span>
      {colon}
      {valEl}
    </>
  );
}

export function PluginTabs() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [active, setActive] = useState(0);
  const [paused, setPaused] = useState(false);

  useInterval(() => setActive((a) => (a + 1) % PLUGINS.length), 3600, inView && !paused && !reduced);

  return (
    <div className="ys-term" ref={ref} onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="ys-tabs-mini" role="tablist" aria-label="Plugin languages">
        {PLUGINS.map((p, i) => (
          <button
            key={p.lang}
            type="button"
            role="tab"
            aria-selected={i === active}
            className={i === active ? "active" : ""}
            onClick={() => {
              setActive(i);
              setPaused(true);
            }}
          >
            {p.lang}
          </button>
        ))}
      </div>
      <div className="ys-term-body ys-fade-swap" key={active} style={{ minHeight: 132 }}>
        {PLUGINS[active].code.map((line, i) => (
          <div key={i}>
            {PLUGINS[active].lang === "JavaScript" && i === 5 ? <span className="s">{line}</span> : <YamlLine line={line} />}
          </div>
        ))}
      </div>
    </div>
  );
}

/* ---------------- Ledger state rings ---------------- */

const RINGS = [
  { r: 50, p: 0.78, c: "#34d399", label: "Staking rewards" },
  { r: 38, p: 0.62, c: "#fbbf24", label: "AdaPot: treasury & reserves" },
  { r: 26, p: 0.46, c: "#a78bfa", label: "DRep distribution & proposal status" },
];

export function LedgerRings() {
  return (
    <div className="ys-rings">
      <svg viewBox="0 0 120 120" aria-hidden="true">
        {RINGS.map((ring, i) => {
          const len = 2 * Math.PI * ring.r;
          return (
            <g key={ring.label}>
              <circle className="track" cx="60" cy="60" r={ring.r} strokeWidth="8" />
              <circle
                className="arc"
                cx="60"
                cy="60"
                r={ring.r}
                strokeWidth="8"
                stroke={ring.c}
                strokeDasharray={len}
                strokeDashoffset={len * (1 - ring.p)}
                style={{ "--len": len, "--p": ring.p, "--d": `${i * 0.35}s` }}
              />
            </g>
          );
        })}
      </svg>
      <ul>
        {RINGS.map((ring) => (
          <li key={ring.label}>
            <i style={{ "--c": ring.c }} />
            {ring.label}
          </li>
        ))}
      </ul>
    </div>
  );
}

/* ---------------- Admin UI mini dashboard ---------------- */

export function AdminDash() {
  return (
    <div className="ys-dash" aria-hidden="true">
      <div className="head">
        <span>Sync status</span>
        <b>● synced</b>
      </div>
      <div className="bar">
        <i />
      </div>
      <svg viewBox="0 0 200 46" preserveAspectRatio="none">
        <defs>
          <linearGradient id="ys-spark-grad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#22d3ee" stopOpacity="0.45" />
            <stop offset="1" stopColor="#22d3ee" stopOpacity="0" />
          </linearGradient>
        </defs>
        <path className="spark-fill" d="M0 38 L20 30 L40 34 L60 20 L80 26 L100 14 L120 22 L140 10 L160 18 L180 8 L200 12 L200 46 L0 46 Z" />
        <path className="spark" d="M0 38 L20 30 L40 34 L60 20 L80 26 L100 14 L120 22 L140 10 L160 18 L180 8 L200 12" />
      </svg>
      <div className="kpis">
        <div className="kpi">
          Health<b>UP</b>
        </div>
        <div className="kpi">
          Metrics<b>Prometheus</b>
        </div>
        <div className="kpi">
          Checks<b>Koios</b>
        </div>
      </div>
    </div>
  );
}
