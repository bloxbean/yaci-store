import YaciStoreLogo from "../../utils/icons/YaciStore.svg";

const BLUE = "#3b8bff";
const CYAN = "#22d3ee";
const MINT = "#34d399";
const AMBER = "#fbbf24";
const VIOLET = "#a78bfa";
const PINK = "#f472b6";

const NETWORKS = [
  ["Mainnet", 40, 226],
  ["Preprod", 138, 226],
  ["Preview", 40, 258],
  ["Devnet", 138, 258],
];

const STORES = [
  "UTxO", "Block", "Transaction",
  "Assets", "Metadata", "Script",
  "Staking", "Epoch", "Governance",
  "AdaPot", "Account", "Submit",
];

const ACCESS = [
  { label: "REST APIs", sub: "Swagger / OpenAPI", y: 70, c: BLUE, from: "db" },
  { label: "Blockfrost APIs", sub: "v3 · compatible paths", y: 124, c: CYAN, from: "db" },
  { label: "Direct SQL", sub: "your own queries", y: 178, c: MINT, from: "db" },
  { label: "Query API", sub: "sandboxed DuckDB", y: 288, c: AMBER, from: "an" },
  { label: "MCP tools", sub: "for AI agents", y: 342, c: VIOLET, from: "an" },
];

const DB_OUT = { x: 900, y: 150 };
const AN_OUT = { x: 900, y: 320 };

const curve = (x1, y1, x2, y2) => {
  const mx = (x1 + x2) / 2;
  return `M${x1} ${y1} C${mx} ${y1} ${mx} ${y2} ${x2} ${y2}`;
};

function Wire({ d, color, dur = 2.4, dots = 1, delay = 0 }) {
  return (
    <g>
      <path className="wire" d={d} />
      <path className="wire-flow" d={d} stroke={color} />
      {Array.from({ length: dots }).map((_, i) => (
        <circle key={i} r="3.5" fill={color} style={{ filter: `drop-shadow(0 0 4px ${color})` }}>
          <animateMotion dur={`${dur}s`} repeatCount="indefinite" path={d} begin={`${delay + (i * dur) / dots}s`} />
        </circle>
      ))}
    </g>
  );
}

function Chip({ x, y, w, h = 26, label, c, i, pulse }) {
  return (
    <g className={`chip${pulse ? " pulse-chip" : ""}`} style={{ "--c": c, "--i": i }}>
      <rect x={x} y={y} width={w} height={h} rx="7" />
      <text x={x + w / 2} y={y + h / 2 + 3.6} textAnchor="middle">
        {label}
      </text>
    </g>
  );
}

export default function Pipeline() {
  const src = { x: 240, y: 240 };
  const coreIn = { x: 330, y: 240 };

  return (
    <div className="ys-pipeline">
      <div className="ys-pipeline-scroll">
        <svg viewBox="0 0 1120 440" role="img" aria-labelledby="ys-pipeline-title ys-pipeline-desc">
          <title id="ys-pipeline-title">How Yaci Store works</title>
          <desc id="ys-pipeline-desc">
            Yaci Store syncs blocks from a Cardano network, publishes events to modular stores and plugins, writes
            to PostgreSQL, MySQL or H2, exports to Parquet or DuckLake for analytics, and serves the data over REST,
            Blockfrost-compatible APIs, SQL, a DuckDB query API and MCP tools.
          </desc>
          <defs>
            <linearGradient id="ys-core-stroke" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stopColor={BLUE} />
              <stop offset="0.5" stopColor={CYAN} />
              <stop offset="1" stopColor={MINT} />
            </linearGradient>
            <radialGradient id="ys-core-glow" cx="0.5" cy="0.35" r="0.7">
              <stop offset="0" stopColor={BLUE} stopOpacity="0.22" />
              <stop offset="1" stopColor={BLUE} stopOpacity="0" />
            </radialGradient>
          </defs>

          {/* Column labels */}
          <text className="col-label" x="20" y="28">01 · SOURCE</text>
          <text className="col-label" x="330" y="28">02 · INDEX</text>
          <text className="col-label" x="720" y="28">03 · STORE</text>
          <text className="col-label" x="950" y="28">04 · SERVE</text>

          {/* Wires first so nodes sit on top */}
          <Wire d={`M${src.x} ${src.y} L${coreIn.x} ${coreIn.y}`} color={BLUE} dur={1.6} dots={3} />
          <Wire d={curve(650, 180, 720, 150)} color={BLUE} dur={2} dots={2} />
          <Wire d="M810 205 L810 265" color={AMBER} dur={1.8} />
          {ACCESS.map((a, i) => {
            const from = a.from === "db" ? DB_OUT : AN_OUT;
            return <Wire key={a.label} d={curve(from.x, from.y, 950, a.y + 20)} color={a.c} dur={2.2} delay={i * 0.3} />;
          })}

          {/* Source */}
          <g>
            <rect className="node-bg" x="20" y="150" width="220" height="146" rx="16" />
            <circle cx="206" cy="182" r="15" fill={`${BLUE}22`} stroke={`${BLUE}66`} />
            <text x="206" y="187.5" textAnchor="middle" fill={BLUE} fontSize="15" fontWeight="700">
              ₳
            </text>
            <text className="node-title" x="40" y="185">Cardano network</text>
            <text className="node-sub" x="40" y="205">node-to-node · node-to-client</text>
            {NETWORKS.map(([label, x, y], i) => (
              <Chip key={label} x={x} y={y} w={90} label={label} c={MINT} i={i} />
            ))}
          </g>

          {/* Core */}
          <g>
            <rect x="330" y="44" width="320" height="376" rx="22" fill="url(#ys-core-glow)" />
            <rect className="node-bg" x="330" y="44" width="320" height="376" rx="22" stroke="url(#ys-core-stroke)" strokeWidth="1.6" />
            <circle className="core-ring" cx="370" cy="84" r="24" stroke={CYAN} />
            <image href={YaciStoreLogo.src ?? YaciStoreLogo} x="358" y="66" width="24" height="36" />
            <text className="node-title" x="404" y="80" style={{ fontSize: 18 }}>Yaci Store</text>
            <text className="node-sub" x="404" y="99">chain sync → events → stores</text>

            {/* Event bus */}
            <rect x="352" y="120" width="276" height="30" rx="8" fill={`${BLUE}14`} stroke={`${BLUE}40`} />
            <text className="node-sub" x="366" y="139.5" style={{ fill: BLUE, fontWeight: 600 }}>EVENT BUS</text>
            {[0, 1, 2].map((i) => (
              <circle key={i} r="3" fill={CYAN}>
                <animateMotion dur="2.4s" repeatCount="indefinite" path="M440 135 L616 135" begin={`${i * 0.8}s`} />
                <animate attributeName="opacity" values="0;1;1;0" dur="2.4s" repeatCount="indefinite" begin={`${i * 0.8}s`} />
              </circle>
            ))}

            {STORES.map((label, i) => {
              const col = i % 3;
              const row = Math.floor(i / 3);
              return (
                <Chip
                  key={label}
                  x={352 + col * 95}
                  y={166 + row * 38}
                  w={86}
                  h={28}
                  label={label}
                  c={row === 3 ? AMBER : i % 2 ? CYAN : BLUE}
                  i={i}
                  pulse
                />
              );
            })}

            <g className="chip" style={{ "--c": PINK }}>
              <rect x="352" y="326" width="276" height="34" rx="9" />
              <text x="490" y="347" textAnchor="middle">Plugins · MVEL · SpEL · JS · Python</text>
            </g>
            <text className="node-sub" x="490" y="396" textAnchor="middle">
              Spring Boot starters · pick only what you need
            </text>
          </g>

          {/* Storage */}
          <g>
            <rect className="node-bg" x="720" y="95" width="180" height="110" rx="16" />
            <g transform="translate(740 118)" stroke={BLUE} fill="none" strokeWidth="1.6">
              <ellipse cx="10" cy="4" rx="10" ry="3.6" />
              <path d="M0 4v14c0 2 4.5 3.6 10 3.6s10-1.6 10-3.6V4" />
              <path d="M0 11c0 2 4.5 3.6 10 3.6s10-1.6 10-3.6" />
            </g>
            <text className="node-title" x="770" y="136">Database</text>
            <text className="node-sub" x="740" y="170">
              PostgreSQL <tspan style={{ fill: MINT }}>(recommended)</tspan>
            </text>
            <text className="node-sub" x="740" y="188">MySQL · H2</text>

            <rect className="node-bg" x="720" y="265" width="180" height="110" rx="16" stroke={`${AMBER}88`} />
            <g transform="translate(740 290)" stroke={AMBER} fill="none" strokeWidth="1.6">
              <rect x="0" y="0" width="20" height="20" rx="3" />
              <path d="M0 7h20M0 13h20M7 0v20" />
            </g>
            <text className="node-title" x="770" y="306">Analytics</text>
            <text className="node-sub" x="740" y="340">Parquet · DuckLake</text>
            <text className="node-sub" x="740" y="358" style={{ fill: "var(--faint)" }}>exported at chain tip</text>
            <text className="node-sub" x="822" y="240" style={{ fill: AMBER }}>export</text>
          </g>

          {/* Access */}
          {ACCESS.map((a) => (
            <g key={a.label}>
              <rect className="node-bg" x="950" y={a.y} width="160" height="40" rx="11" />
              <rect x="950" y={a.y + 10} width="3" height="20" rx="1.5" fill={a.c} />
              <text x="966" y={a.y + 18} style={{ fontSize: 12.5, fontWeight: 700, fill: "var(--text)" }}>
                {a.label}
              </text>
              <text className="node-sub" x="966" y={a.y + 32} style={{ fontSize: 9.5 }}>
                {a.sub}
              </text>
            </g>
          ))}
        </svg>
      </div>
      <div className="ys-legend">
        <span style={{ "--c": BLUE }}><i />Core indexing</span>
        <span style={{ "--c": PINK }}><i />Plugins</span>
        <span style={{ "--c": AMBER }}><i />Analytics (v3)</span>
        <span style={{ "--c": VIOLET }}><i />AI / MCP (v3)</span>
      </div>
    </div>
  );
}
