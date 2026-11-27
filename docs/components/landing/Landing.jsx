"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { JetBrains_Mono, Inter, Space_Grotesk } from "next/font/google";
import YaciStoreLogo from "../../utils/icons/YaciStore.svg";
import LiveConsole from "./LiveConsole";
import Pipeline from "./Pipeline";
import { AdminDash, AnalyticsArt, BlockfrostTerm, LedgerRings, McpChat, PluginTabs, StoreToggles } from "./FeatureArt";
import {
  Archive,
  ArrowRight,
  BarChart,
  Book,
  Box,
  Braces,
  Check,
  Code,
  Coins,
  Copy,
  Cpu,
  Database,
  Discord,
  External,
  Filter,
  Gauge,
  GitHub,
  Globe,
  Layers,
  Moon,
  Sparkles,
  Sun,
  XLogo,
} from "./icons";
import "./landing.css";

const display = Space_Grotesk({ subsets: ["latin"], weight: ["500", "600", "700"], variable: "--font-ys-display", display: "swap" });
const body = Inter({ subsets: ["latin"], variable: "--font-ys-body", display: "swap" });
const mono = JetBrains_Mono({ subsets: ["latin"], weight: ["400", "500", "600"], variable: "--font-ys-mono", display: "swap" });

const GITHUB_URL = "https://github.com/bloxbean/yaci-store";
const DISCORD_URL = "https://discord.gg/JtQ54MSw6p";
const X_URL = "https://x.com/bloxbean";

const DOCS = {
  intro: "/docs/v2/introduction/overview",
  gettingStarted: "/docs/v2/getting-started/overview",
  v3: "/docs/v3/introduction/overview",
  starters: "/docs/v2/introduction/spring-boot-starters",
  blockfrost: "/docs/v3/blockfrost/overview",
  analytics: "/docs/v3/analytics/overview",
  queryApi: "/docs/v3/analytics/query-api",
  mcp: "/docs/v3/analytics/mcp-server",
  plugins: "/docs/v2/plugins/write-first-plugin",
  stores: "/docs/v2/stores/overview",
  adminUi: "/docs/v2/admin-ui/overview",
  showcase: "/docs/v2/showcase/projects-using-yaci-store",
};

/* ------------------------------------------------------------------ */

function useTheme() {
  const [theme, setTheme] = useState("dark");
  useEffect(() => {
    // Re-read on every mount: the visitor may have switched theme in the docs since the page loaded.
    let saved = null;
    try {
      saved = localStorage.getItem("theme");
    } catch {}
    const current =
      saved === "light" || saved === "dark"
        ? saved
        : window.matchMedia("(prefers-color-scheme: light)").matches
          ? "light"
          : "dark";
    document.documentElement.setAttribute("data-ys-theme", current);
    setTheme(current);
  }, []);
  const toggle = useCallback(() => {
    setTheme((prev) => {
      const next = prev === "dark" ? "light" : "dark";
      document.documentElement.setAttribute("data-ys-theme", next);
      try {
        // Same key next-themes uses, so the docs pages open in the same theme.
        localStorage.setItem("theme", next);
      } catch {}
      return next;
    });
  }, []);
  return [theme, toggle];
}

function useGithubStars() {
  const [stars, setStars] = useState(null);
  useEffect(() => {
    let cancelled = false;
    try {
      const cached = sessionStorage.getItem("ys-stars");
      if (cached) {
        setStars(Number(cached));
        return;
      }
    } catch {}
    fetch("https://api.github.com/repos/bloxbean/yaci-store")
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => {
        if (cancelled || !d || typeof d.stargazers_count !== "number") return;
        setStars(d.stargazers_count);
        try {
          sessionStorage.setItem("ys-stars", String(d.stargazers_count));
        } catch {}
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);
  return stars;
}

function useReveal() {
  useEffect(() => {
    const els = document.querySelectorAll(".ys-reveal");
    if (typeof IntersectionObserver === "undefined") {
      els.forEach((el) => el.classList.add("in"));
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add("in");
            io.unobserve(e.target);
          }
        });
      },
      { rootMargin: "0px 0px -8% 0px", threshold: 0.08 }
    );
    els.forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, []);
}

function useScrolled(offset = 12) {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > offset);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [offset]);
  return scrolled;
}

const formatStars = (n) => (n >= 1000 ? `${(n / 1000).toFixed(1).replace(/\.0$/, "")}k` : String(n));

/** Moves the radial spotlight on a card to follow the pointer. */
function spotlight(e) {
  const card = e.target.closest?.(".ys-card");
  if (!card) return;
  const r = card.getBoundingClientRect();
  card.style.setProperty("--mx", `${e.clientX - r.left}px`);
  card.style.setProperty("--my", `${e.clientY - r.top}px`);
}

/* ------------------------------------------------------------------ */

function Nav({ theme, onToggleTheme, stars }) {
  const scrolled = useScrolled();
  return (
    <header className={`ys-nav${scrolled ? " scrolled" : ""}`}>
      <div className="ys-container ys-nav-inner">
        <Link href="/" className="ys-brand" aria-label="Yaci Store home">
          <Image src={YaciStoreLogo} alt="" width={26} priority />
          Yaci Store
        </Link>
        <nav className="ys-nav-links" aria-label="Primary">
          <a href="#features">Features</a>
          <a href="#how-it-works">How it works</a>
          <a href="#get-started">Get started</a>
          <a href="#showcase">Showcase</a>
          <Link href={DOCS.intro}>Docs</Link>
        </nav>
        <div className="ys-nav-actions">
          <a className="ys-icon-btn" href={GITHUB_URL} target="_blank" rel="noopener noreferrer" aria-label="Yaci Store on GitHub">
            <GitHub />
            {stars !== null && <span className="count hide-sm">★ {formatStars(stars)}</span>}
          </a>
          <a className="ys-icon-btn hide-sm" href={DISCORD_URL} target="_blank" rel="noopener noreferrer" aria-label="Join the Discord">
            <Discord />
          </a>
          <button
            type="button"
            className="ys-icon-btn"
            onClick={onToggleTheme}
            aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
          >
            {theme === "dark" ? <Sun /> : <Moon />}
          </button>
          <Link href={DOCS.gettingStarted} className="ys-btn ys-btn-primary ys-btn-sm">
            Get started
          </Link>
        </div>
      </div>
    </header>
  );
}

function Hero() {
  return (
    <section className="ys-hero">
      <div className="ys-container ys-hero-grid">
        <div className="ys-hero-copy">
          <Link href={DOCS.v3} className="ys-announce ys-reveal">
            <span className="tag">v3 Beta</span>
            <span className="text">Blockfrost-compatible APIs, Analytics Store &amp; MCP</span>
            <ArrowRight width={15} height={15} />
          </Link>
          <h1 className="ys-hero-title ys-reveal" style={{ "--delay": "0.05s" }}>
            Cardano data,
            <br />
            indexed <span className="ys-gradient-text">your way.</span>
          </h1>
          <p className="ys-hero-lead ys-reveal" style={{ "--delay": "0.12s" }}>
            Yaci Store is a modular, open-source Java indexer for Cardano. Run the ready-made app with Docker, or embed
            only the stores you need as Spring Boot starters. Serve the data over REST, Blockfrost-compatible APIs, SQL,
            Parquet or MCP.
          </p>
          <div className="ys-hero-ctas ys-reveal" style={{ "--delay": "0.18s" }}>
            <Link href={DOCS.gettingStarted} className="ys-btn ys-btn-primary">
              Get started <ArrowRight />
            </Link>
            <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer" className="ys-btn ys-btn-ghost">
              <GitHub /> View on GitHub
            </a>
          </div>
          <div className="ys-hero-meta ys-reveal" style={{ "--delay": "0.24s" }}>
            <span>
              <Globe /> Mainnet · Preprod · Preview · Devnet
            </span>
            <span>
              <Database /> PostgreSQL (recommended) · MySQL · H2
            </span>
            <span>
              <Check /> MIT licensed
            </span>
          </div>
        </div>
        <div className="ys-reveal" style={{ "--delay": "0.15s" }}>
          <LiveConsole />
        </div>
      </div>
    </section>
  );
}

function SectionHead({ eyebrow, title, children }) {
  return (
    <div className="ys-section-head ys-reveal">
      <div className="ys-eyebrow">{eyebrow}</div>
      <h2 className="ys-h2">{title}</h2>
      {children && <p className="ys-sub">{children}</p>}
    </div>
  );
}

function HowItWorks() {
  return (
    <section id="how-it-works" className="ys-section">
      <div className="ys-container">
        <SectionHead eyebrow="How it works" title={<>From chain sync to query, <span className="ys-gradient-text">one pipeline</span></>}>
          Yaci Store follows the chain from any Cardano node, turns every block into typed events, and fans them out to
          the stores you enable. Plugins filter what gets stored. The results land in your database and, optionally, in
          analytics-ready Parquet.
        </SectionHead>
        <div className="ys-pipeline-card ys-reveal">
          <Pipeline />
        </div>
      </div>
    </section>
  );
}

function FeatureCard({ span, accent, icon, badge, title, text, link, children, delay }) {
  return (
    <div className={`ys-card span-${span} ys-reveal`} style={{ "--accent": accent, "--delay": delay }}>
      <div className="ys-card-body">
        <div className="ys-card-top">
          <span className="ys-card-icon">{icon}</span>
          {badge && <span className="ys-badge">{badge}</span>}
        </div>
        <h3 className="ys-card-title">{title}</h3>
        <p className="ys-card-text">{text}</p>
        {link && (
          <Link href={link.href} className="ys-card-link">
            {link.label} <ArrowRight />
          </Link>
        )}
      </div>
      <div className="ys-card-art">{children}</div>
    </div>
  );
}

function Features() {
  return (
    <section id="features" className="ys-section">
      <div className="ys-container">
        <SectionHead eyebrow="Features" title={<>Everything you need to build on <span className="ys-gradient-text">Cardano data</span></>}>
          Start with the stores you need and add more later. Every module is a standalone library and a Spring Boot
          starter, so the indexer grows with your application.
        </SectionHead>

        <div className="ys-bento" onPointerMove={spotlight}>
          <FeatureCard
            span={4}
            accent="var(--blue)"
            icon={<Layers />}
            title="Modular by design"
            text="UTxO, blocks, transactions, assets, metadata, scripts, staking, governance and more. Turn each store on or off, or pull it in as its own Spring Boot starter."
            link={{ href: DOCS.starters, label: "Spring Boot starters" }}
          >
            <StoreToggles />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--cyan)"
            icon={<Braces />}
            badge="v3"
            title="Blockfrost-compatible APIs"
            text="Paths and response shapes follow the Blockfrost API, so existing clients can point at your own node."
            link={{ href: DOCS.blockfrost, label: "Blockfrost APIs" }}
            delay="0.06s"
          >
            <BlockfrostTerm />
          </FeatureCard>

          <FeatureCard
            span={3}
            accent="var(--amber)"
            icon={<BarChart />}
            badge="v3"
            title="Analytics Store"
            text="Export indexed data to Parquet or DuckLake catalogs with ACID transactions and time travel, then query it with a sandboxed DuckDB engine."
            link={{ href: DOCS.analytics, label: "Analytics Store" }}
          >
            <AnalyticsArt />
            <div className="ys-pills">
              <span className="ys-pill">Parquet</span>
              <span className="ys-pill">DuckLake</span>
              <span className="ys-pill">DuckDB</span>
              <span className="ys-pill">custom exporters</span>
            </div>
          </FeatureCard>

          <FeatureCard
            span={3}
            accent="var(--violet)"
            icon={<Sparkles />}
            badge="v3"
            title="Ready for AI agents"
            text="Built-in MCP tools let an agent discover tables, read schemas and run read-only SQL over your analytics data."
            link={{ href: DOCS.mcp, label: "Set up the MCP server" }}
            delay="0.06s"
          >
            <McpChat />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--pink)"
            icon={<Filter />}
            title="Plugins in your language"
            text="Filter, transform or react to data with MVEL, SpEL, JavaScript or Python. No Java code or rebuild needed."
            link={{ href: DOCS.plugins, label: "Write a plugin" }}
          >
            <PluginTabs />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--mint)"
            icon={<Coins />}
            title="Ledger state, computed"
            text="The ledger-state profile calculates rewards, AdaPot and governance state itself, with no external data source."
            link={{ href: DOCS.stores, label: "Stores" }}
            delay="0.06s"
          >
            <LedgerRings />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--cyan)"
            icon={<Gauge />}
            title="Operate with confidence"
            text="Admin UI for sync status and health, Prometheus and Grafana monitoring, rollback handling, pruning and auto-recovery."
            link={{ href: DOCS.adminUi, label: "Admin UI" }}
            delay="0.12s"
          >
            <AdminDash />
          </FeatureCard>
        </div>
      </div>
    </section>
  );
}

/* ------------------------------------------------------------------ */

const RUN_OPTIONS = [
  {
    id: "docker",
    icon: <Box />,
    title: "Docker",
    tag: "Recommended",
    desc: "The full stack: Yaci Store, PostgreSQL, admin CLI, Prometheus and Grafana.",
    file: "terminal",
    code: [
      ["c", "# Download the Docker distribution from GitHub Releases, then"],
      ["c", "# point it at a network in config/application.properties:"],
      ["k", "store.cardano.host=preprod-node.world.dev.cardano.org"],
      ["k", "store.cardano.port=30000"],
      ["k", "store.cardano.protocol-magic=1"],
      ["", ""],
      ["p", "./yaci-store.sh start"],
      ["p", "./yaci-store.sh logs:yaci-store"],
    ],
    points: ["PostgreSQL included", "Ledger-state profile", "Grafana dashboards"],
    href: "/docs/v2/getting-started/installation/docker",
  },
  {
    id: "zip",
    icon: <Archive />,
    title: "ZIP",
    desc: "Prebuilt JARs and config files. Bring your own Java 21+ and PostgreSQL.",
    file: "terminal",
    code: [
      ["c", "# Java 21+ (for example, via SDKMAN)"],
      ["p", "sdk install java 24.0.1-tem"],
      ["", ""],
      ["c", "# Unzip the release, edit config/application.properties, then:"],
      ["p", "./bin/start.sh"],
      ["", ""],
      ["c", "# With ledger-state calculation"],
      ["p", "./bin/start.sh ledger-state"],
    ],
    points: ["Full control", "Properties-based config", "Ledger-state support"],
    href: "/docs/v2/getting-started/installation/zip",
  },
  {
    id: "native",
    icon: <Cpu />,
    title: "Native",
    tag: "Preview",
    desc: "GraalVM native binaries per OS and architecture. No JVM to install.",
    file: "terminal",
    code: [
      ["c", "# From a rel-native-<version> release, download"],
      ["c", "# yaci-store-<version>-<os>-<arch>-all.zip, unzip, then:"],
      ["p", "./yaci-store"],
      ["", ""],
      ["c", "# With ledger-state calculation"],
      ["p", "SPRING_PROFILES_ACTIVE=ledger-state ./yaci-store"],
    ],
    points: ["Single binary", "Linux, macOS & Windows builds", "Same config as ZIP"],
    href: "/docs/v2/getting-started/installation/native",
  },
  {
    id: "library",
    icon: <Code />,
    title: "Java library",
    desc: "Embed Yaci Store in your Spring Boot app and index only what you need.",
    file: "build.gradle",
    code: [
      ["n", "dependencies {"],
      ["s", "    implementation 'com.bloxbean.cardano:yaci-store-spring-boot-starter:<version>'"],
      ["s", "    implementation 'com.bloxbean.cardano:yaci-store-utxo-spring-boot-starter:<version>'"],
      ["s", "    implementation 'com.bloxbean.cardano:yaci-store-metadata-spring-boot-starter:<version>'"],
      ["n", "}"],
      ["", ""],
      ["c", "// Or override the storage layer to keep only the data you care about."],
    ],
    points: ["14+ store starters", "Custom storage", "Event-driven hooks"],
    href: "/docs/v2/usage/as-library",
  },
];

function RunItYourWay() {
  const [active, setActive] = useState(0);
  const [copied, setCopied] = useState(false);
  const opt = RUN_OPTIONS[active];

  const copy = async () => {
    const text = opt.code
      .filter(([cls]) => cls !== "c")
      .map(([, line]) => line.replace(/^\$ /, ""))
      .join("\n")
      .replace(/\n{2,}/g, "\n")
      .trim();
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    } catch {}
  };

  return (
    <section id="get-started" className="ys-section">
      <div className="ys-container">
        <SectionHead eyebrow="Get started" title={<>Run it <span className="ys-gradient-text">your way</span></>}>
          Go from zero to indexing in minutes with Docker, or build a custom indexer with the same modules.
        </SectionHead>

        <div className="ys-run ys-reveal">
          <div className="ys-run-tabs" role="tablist" aria-label="Distribution">
            {RUN_OPTIONS.map((o, i) => (
              <button
                key={o.id}
                type="button"
                role="tab"
                id={`ys-run-tab-${o.id}`}
                aria-selected={i === active}
                aria-controls="ys-run-panel"
                className={`ys-run-tab${i === active ? " active" : ""}`}
                onClick={() => setActive(i)}
              >
                <span className="ic">{o.icon}</span>
                <span>
                  <span className="t">
                    {o.title}
                    {o.tag && <em>{o.tag}</em>}
                  </span>
                  <span className="d">{o.desc}</span>
                </span>
              </button>
            ))}
          </div>

          <div className="ys-run-panel" id="ys-run-panel" role="tabpanel" aria-labelledby={`ys-run-tab-${opt.id}`}>
            <div className="ys-term-bar">
              <div className="ys-dots" aria-hidden="true">
                <i />
                <i />
                <i />
              </div>
              {opt.file}
              <button type="button" className="ys-copy" onClick={copy}>
                {copied ? <Check /> : <Copy />}
                {copied ? "Copied" : "Copy"}
              </button>
            </div>
            <pre className="ys-run-code ys-term ys-fade-swap" key={opt.id} style={{ border: 0, borderRadius: 0 }}>
              {opt.code.map(([cls, line], i) => (
                <div key={i}>
                  {cls === "p" && <span className="p">$ </span>}
                  <span className={cls === "p" ? "" : cls}>{line || " "}</span>
                </div>
              ))}
            </pre>
            <div className="ys-run-foot">
              <ul>
                {opt.points.map((p) => (
                  <li key={p}>
                    <Check />
                    {p}
                  </li>
                ))}
              </ul>
              <Link href={opt.href} className="ys-btn ys-btn-ghost ys-btn-sm">
                Setup guide <ArrowRight />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ------------------------------------------------------------------ */

const PROJECTS = [
  {
    name: "Cardano Rosetta Java",
    by: "Cardano Foundation",
    mark: "RJ",
    colors: ["#2f6bff", "#22d3ee"],
    desc: "Official Rosetta API implementation for Cardano, built for exchange and institutional integrations.",
    href: "https://github.com/cardano-foundation/cardano-rosetta-java",
  },
  {
    name: "Cardano Ballot",
    by: "Cardano Foundation",
    mark: "CB",
    colors: ["#7c3aed", "#f472b6"],
    desc: "Hybrid on- and off-chain voting system for the Cardano ecosystem.",
    href: "https://github.com/cardano-foundation/cf-cardano-ballot",
  },
  {
    name: "Reeve Platform",
    by: "Cardano Foundation",
    mark: "RP",
    colors: ["#059669", "#34d399"],
    desc: "Integrates traditional accounting systems with blockchain technology, bridging enterprise finance and DeFi.",
    href: "https://github.com/cardano-foundation/cf-reeve-platform",
  },
  {
    name: "AdaHandle Resolver",
    by: "Cardano Foundation",
    mark: "AH",
    colors: ["#16a34a", "#a3e635"],
    desc: "Fast, reliable resolution service for ADA Handles.",
    href: "https://github.com/cardano-foundation/cf-adahandle-resolver",
  },
  {
    name: "Nexus",
    by: "A.D. Labs",
    img: "/images/nexus.svg",
    colors: ["#111827", "#374151"],
    desc: "Multi-chain data API that uses Yaci Store as its primary Cardano data provider.",
    href: "https://market.gerowallet.io",
  },
  {
    name: "UPLC Link",
    by: "Open source",
    mark: "UL",
    colors: ["#0ea5e9", "#6366f1"],
    desc: "Verify smart contract source code against on-chain scripts and browse the registry.",
    href: "https://uplc.link/",
  },
  {
    name: "Aquarium Node",
    by: "FluidTokens",
    mark: "FT",
    colors: ["#0891b2", "#38bdf8"],
    desc: "Indexes FluidTokens Tank UTxOs and processes scheduled transactions for DeFi automation.",
    href: "https://github.com/FluidTokens/ft-aquarium-node",
  },
  {
    name: "SundaeSwap Scooper Analytics",
    by: "Easy1 Staking",
    mark: "SS",
    colors: ["#db2777", "#fb923c"],
    desc: "Crawler that stores scoop transactions for DEX analytics.",
    href: "https://github.com/easy1staking-com/sundaeswap-scooper-analytics",
  },
  {
    name: "AdaMatic",
    by: "Easy1 Staking",
    mark: "AM",
    colors: ["#1d4ed8", "#60a5fa"],
    desc: "Cardano staking platform with delegation services and analytics.",
    href: "https://adamatic.xyz/",
  },
];

function Mark({ p }) {
  return (
    <span className="ys-mono-mark" style={{ "--c1": p.colors[0], "--c2": p.colors[1] }} aria-hidden="true">
      {p.img ? <img src={p.img} alt="" /> : p.mark}
    </span>
  );
}

function Showcase() {
  const loop = [...PROJECTS, ...PROJECTS];
  return (
    <section id="showcase" className="ys-section">
      <div className="ys-container">
        <SectionHead eyebrow="Showcase" title={<>Trusted across the <span className="ys-gradient-text">Cardano ecosystem</span></>}>
          Teams building exchange integrations, governance, DeFi, analytics and developer tools run on Yaci Store.
        </SectionHead>
      </div>

      <div className="ys-marquee ys-reveal" aria-hidden="true">
        <div className="ys-marquee-track">
          {loop.map((p, i) => (
            <span key={i} className="ys-marquee-item">
              <Mark p={p} />
              {p.name}
            </span>
          ))}
        </div>
      </div>

      <div className="ys-container">
        <div className="ys-projects">
          {PROJECTS.map((p, i) => (
            <a
              key={p.name}
              href={p.href}
              target="_blank"
              rel="noopener noreferrer"
              className="ys-project ys-reveal"
              style={{ "--delay": `${(i % 3) * 0.06}s` }}
            >
              <div className="top">
                <Mark p={p} />
                <div>
                  <div className="name">{p.name}</div>
                  <span className="by">{p.by}</span>
                </div>
              </div>
              <p className="ys-project-desc">{p.desc}</p>
              <span className="go">
                {p.href.includes("github.com") ? "GitHub" : "Visit site"} <External />
              </span>
            </a>
          ))}
        </div>

        <div className="ys-supported ys-reveal">
          <span className="lbl">Supported by</span>
          <img className="cf-dark" src="/images/cf-logo-text-white.png" alt="Cardano Foundation" />
          <img className="cf-light" src="/images/cf-logo-text.svg" alt="Cardano Foundation" />
          <p className="ys-supported-text">The Cardano Foundation supports this project with engineering resources.</p>
        </div>
      </div>
    </section>
  );
}

function FinalCta() {
  return (
    <section className="ys-section" style={{ paddingBottom: 0 }}>
      <div className="ys-container">
        <div className="ys-cta ys-reveal">
          <h2 className="ys-h2">
            Start indexing <span className="ys-gradient-text">in minutes</span>
          </h2>
          <p className="ys-sub">
            Open source and MIT licensed. Pick a distribution, point it at a network, and query your first block.
          </p>
          <div className="ys-cta-buttons">
            <Link href={DOCS.gettingStarted} className="ys-btn ys-btn-primary">
              Get started <ArrowRight />
            </Link>
            <Link href={DOCS.intro} className="ys-btn ys-btn-ghost">
              <Book /> Read the docs
            </Link>
          </div>
          <div className="ys-community">
            <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
              <GitHub /> GitHub
            </a>
            <a href={DISCORD_URL} target="_blank" rel="noopener noreferrer">
              <Discord /> Discord
            </a>
            <a href={X_URL} target="_blank" rel="noopener noreferrer">
              <XLogo /> @bloxbean
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="ys-footer">
      <div className="ys-container ys-footer-inner">
        <span className="ys-brand" style={{ fontSize: "0.98rem", color: "var(--text)" }}>
          <Image src={YaciStoreLogo} alt="" width={20} />
          Yaci Store
        </span>
        <span>© {new Date().getFullYear()} BloxBean · MIT License</span>
        <nav aria-label="Footer">
          <Link href={DOCS.intro}>Docs</Link>
          <Link href={DOCS.v3}>v3 Beta docs</Link>
          <Link href={DOCS.showcase}>Showcase</Link>
          <a href={`${GITHUB_URL}/releases`} target="_blank" rel="noopener noreferrer">
            Releases
          </a>
          <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
            GitHub
          </a>
        </nav>
      </div>
    </footer>
  );
}

export default function Landing() {
  const [theme, toggleTheme] = useTheme();
  const stars = useGithubStars();
  useReveal();

  return (
    <div className={`ys ${display.variable} ${body.variable} ${mono.variable}`}>
      <div className="ys-ambient" aria-hidden="true">
        <div className="grid" />
        <div className="blob b1" />
        <div className="blob b2" />
        <div className="blob b3" />
      </div>
      <Nav theme={theme} onToggleTheme={toggleTheme} stars={stars} />
      <main className="ys-main">
        <Hero />
        <HowItWorks />
        <Features />
        <RunItYourWay />
        <Showcase />
        <FinalCta />
      </main>
      <Footer />
    </div>
  );
}
