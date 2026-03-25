'use client';
import { useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import YaciStore from '../utils/icons/YaciStore.svg';
import '../styles/landing.css';
import {
  Fish, PolyCloud, IcebergTip, ScrollHintArrow, WaveDivider,
  IcebergBackground, OceanFloor,
  GitHubIcon, DiscordIcon, DocsIcon, XIcon, ArrowUpIcon,
  RocketIcon, BookOpenIcon, StarIcon, ChevronDownIcon,
  BoltIcon, WrenchIcon,
  VoteIcon, BarChartIcon, CoinsIcon, AwardIcon,
  DatabaseIcon, FlaskIcon,
  CoffeeIcon, CoffeeSmIcon,
  RefreshIcon, BallotIcon, BuildingIcon, TagIcon,
  DropletIcon, TrendingUpIcon, ShieldIcon, ServerIcon,
} from '../components/svgs';

export default function LandingPage() {
  const [mounted, setMounted] = useState(false);
  const [scrollY, setScrollY] = useState(0);
  const [showBackTop, setShowBackTop] = useState(false);

  useEffect(() => {
    setMounted(true);

    const observer = new IntersectionObserver(
      (entries) =>
        entries.forEach((e) => {
          if (e.isIntersecting) e.target.classList.add('in-view');
        }),
      { threshold: 0.1 },
    );
    const timer = setTimeout(() => {
      document
        .querySelectorAll('.reveal')
        .forEach((el) => observer.observe(el));
    }, 150);

    const onScroll = () => {
      const y = window.scrollY;
      setScrollY(y);
      setShowBackTop(y > 320);
    };
    window.addEventListener('scroll', onScroll, { passive: true });

    return () => {
      clearTimeout(timer);
      observer.disconnect();
      window.removeEventListener('scroll', onScroll);
    };
  }, []);

  const clouds = useMemo(() => {
    const rand = (min, max) => min + Math.random() * (max - min);
    const lerp = (a, b, t) => a + (b - a) * t;

    return Array.from({ length: 12 }, () => {
      const depth = Math.random();
      const baseWidth = lerp(90, 175, depth);
      const width = Math.round(baseWidth + rand(-12, 12));
      const baseDuration = lerp(110, 60, depth);
      const duration = rand(baseDuration * 0.9, baseDuration * 1.1);
      const opacity = lerp(0.34, 0.72, depth);
      const top = `${rand(4, 28).toFixed(1)}%`;

      return {
        top,
        width,
        opacity: +opacity.toFixed(2),
        duration: +duration.toFixed(1),
        delay: -rand(0, duration).toFixed(1),
        parallax: +rand(-0.012, 0.012).toFixed(3),
        floatDuration: +rand(14, 24).toFixed(1),
        floatX1: `${rand(-10, 10).toFixed(1)}px`,
        floatY1: `${rand(-6, -1.5).toFixed(1)}px`,
        floatX2: `${rand(-8, 8).toFixed(1)}px`,
        floatY2: `${rand(1.5, 5).toFixed(1)}px`,
        scaleDuration: +rand(18, 30).toFixed(1),
        scaleFrom: rand(0.9, 0.97).toFixed(2),
        scaleTo: rand(0.99, 1.05).toFixed(2),
        blur: `${lerp(1.2, 0, depth).toFixed(2)}px`,
        opacityFrom: Math.max(0.18, opacity * 0.88).toFixed(2),
        opacityTo: Math.min(1, opacity * 1.04).toFixed(2),
      };
    }).sort((a, b) => a.width - b.width);
  }, []);

  if (!mounted) return null;

  /* ── Shared class constants ── */
  const btnBase =
    'px-[1.7rem] py-[0.78rem] rounded-lg font-semibold text-[0.93rem] no-underline border-2 inline-flex items-center gap-[0.4rem] transition-all duration-[220ms] cursor-pointer whitespace-nowrap';
  const btnGlass = `${btnBase} bg-white/[0.12] text-sky-100 border-white/40 hover:bg-white/[0.22] hover:-translate-y-0.5`;
  const btnGhost = `${btnBase} bg-white/[0.07] text-sky-200 border-white/20 hover:bg-white/[0.14] hover:-translate-y-0.5`;

  const depthLabel =
    'inline-flex items-center gap-[0.45rem] bg-white/[0.06] border border-white/[0.12] rounded-[20px] px-[0.85rem] py-[0.25rem] text-[0.72rem] font-semibold text-white/40 uppercase tracking-[0.09em] mb-4';
  const cardEyebrow =
    'text-[0.72rem] font-bold uppercase tracking-[0.12em] text-sky-300 mb-2';
  const cardTitle =
    'text-[1.35rem] font-extrabold text-slate-50 mb-3 leading-[1.25]';
  const cardBody = 'text-[0.9rem] text-slate-400 leading-[1.7] mb-[1.4rem]';
  const pill =
    'bg-white/[0.07] border border-white/[0.13] rounded-[5px] px-[0.7rem] py-[0.28rem] text-[0.76rem] text-sky-200 font-medium transition-all duration-[180ms] hover:bg-sky-300/[0.14] hover:text-sky-100';
  const cardInfoRow =
    'flex items-start gap-3 bg-white/[0.05] border border-white/[0.09] rounded-[10px] px-4 py-[0.9rem] mb-3 text-[0.84rem]';
  const infoTitle = 'text-[0.84rem] font-bold text-sky-100 mb-[0.2rem]';
  const infoDesc = 'text-[0.8rem] text-slate-400 leading-[1.55]';

  const featCard =
    'feat-card reveal bg-[rgba(8,28,55,0.72)] backdrop-blur-[20px] border border-white/[0.13] rounded-[22px] p-8 max-[600px]:p-6 max-w-[460px] max-[860px]:max-w-[540px] w-full text-slate-50 shadow-[0_8px_40px_rgba(0,0,0,0.45)] hover:shadow-[0_12px_48px_rgba(0,0,0,0.6)]';

  const depthRow =
    'min-h-[72vh] max-[600px]:min-h-[60vh] flex items-center py-20 max-[600px]:py-14';
  const depthLeft = `${depthRow} justify-start max-[860px]:justify-center`;
  const depthRight = `${depthRow} justify-end max-[860px]:justify-center`;
  const depthCenter = `${depthRow} justify-center pb-16`;

  const projLink =
    'text-sky-300 no-underline text-[0.76rem] font-semibold px-[0.6rem] py-[0.18rem] rounded-[5px] border border-sky-300/20 bg-sky-300/[0.05] transition-all duration-[180ms] hover:bg-sky-300/[0.14] hover:border-sky-300/45';
  const projLinkFeatured =
    'text-green-400 no-underline text-[0.76rem] font-semibold px-[0.6rem] py-[0.18rem] rounded-[5px] border border-green-400/[0.22] bg-green-400/[0.05] transition-all duration-[180ms] hover:bg-green-400/[0.14] hover:border-green-400/45';
  const commLink =
    'flex items-center gap-[0.55rem] px-6 py-3 bg-white/[0.05] border border-white/[0.1] rounded-[10px] text-slate-200 no-underline font-medium text-[0.875rem] transition-all duration-[220ms] hover:bg-white/[0.1] hover:border-white/[0.22] hover:-translate-y-[3px] hover:shadow-[0_8px_20px_rgba(0,0,0,0.35)]';

  const projects = [
    {
      icon: <RefreshIcon />,
      name: 'Cardano Rosetta Java',
      desc: 'Official Rosetta API for Cardano by Cardano Foundation — seamless blockchain integration for exchanges and institutions.',
      href: 'https://github.com/cardano-foundation/cardano-rosetta-java',
      featured: true,
    },
    {
      icon: <BallotIcon />,
      name: 'CF Cardano Ballot',
      desc: 'Hybrid on- and off-chain voting system by Cardano Foundation, powering governance and community decisions.',
      href: 'https://github.com/cardano-foundation/cf-cardano-ballot',
    },
    {
      icon: <BuildingIcon />,
      name: 'CF Reeve Platform',
      desc: 'Bridges traditional accounting systems with blockchain technology by Cardano Foundation.',
      href: 'https://github.com/cardano-foundation/cf-reeve-platform',
    },
    {
      icon: <TagIcon />,
      name: 'CF AdaHandle Resolver',
      desc: 'Fast and reliable NFT handle resolution service by Cardano Foundation.',
      href: 'https://github.com/cardano-foundation/cf-adahandle-resolver',
    },
    {
      icon: <DropletIcon />,
      name: 'FluidTokens Aquarium Node',
      desc: 'Indexes FluidTokens Tank UTxOs and processes scheduled DeFi transactions automatically.',
      href: 'https://github.com/FluidTokens/ft-aquarium-node',
    },
    {
      icon: <TrendingUpIcon />,
      name: 'SundaeSwap Analytics',
      desc: 'DEX analytics crawler by Easy1 Staking that indexes scoop transaction data for trading insights.',
      href: 'https://github.com/easy1staking-com/sundaeswap-scooper-analytics',
    },
    {
      icon: <ShieldIcon />,
      name: 'AdaMatic',
      desc: 'Professional Cardano staking platform by Easy1 Staking with enterprise-grade analytics.',
      href: 'https://adamatic.xyz/',
    },
    {
      icon: <ServerIcon />,
      name: 'CBI Backend Service',
      desc: 'Community-driven blockchain backend by Cardano Fans (CRFA) for ecosystem tooling and applications.',
      href: 'https://github.com/Cardano-Fans/cbi-backend',
    },
  ];

  return (
    <>
      {/* ─── FIXED HEADER ─── */}
      <header className="absolute top-0 left-0 right-0 z-[1000] bg-white/20 border-b border-white/25 shadow-[0_1px_12px_rgba(0,0,0,0.08)] py-[0.7rem]">
        <div className="max-w-[1200px] mx-auto px-8 max-[600px]:px-4 flex items-center justify-between gap-4">
          <Link
            href="/"
            className="flex items-center gap-[0.55rem] no-underline"
          >
            <Image src={YaciStore} alt="Yaci Store Logo" width={32} />
            <span className="text-[1.15rem] font-bold text-gray-800">
              Yaci Store
            </span>
          </Link>
          <nav className="flex items-center gap-[0.3rem] max-[600px]:gap-[0.15rem]">
            <Link
              href="/docs/getting-started/overview"
              className="no-underline text-[0.875rem] max-[600px]:text-[0.78rem] font-medium px-[0.8rem] py-[0.38rem] max-[600px]:px-[0.55rem] max-[600px]:py-[0.3rem] rounded-[6px] transition-all duration-[180ms] whitespace-nowrap bg-sky-700 text-white hover:bg-sky-800"
            >
              Get Started
            </Link>
            <Link
              href="/docs/introduction/overview"
              className="text-gray-700 no-underline text-[0.875rem] max-[600px]:text-[0.78rem] font-medium px-[0.8rem] py-[0.38rem] max-[600px]:px-[0.55rem] max-[600px]:py-[0.3rem] rounded-[6px] transition-all duration-[180ms] whitespace-nowrap hover:text-sky-700 hover:bg-sky-50"
            >
              Docs
            </Link>
            <a
              href="https://github.com/bloxbean/yaci-store"
              target="_blank"
              rel="noopener noreferrer"
              className="text-gray-700 no-underline text-[0.875rem] max-[600px]:text-[0.78rem] font-medium px-[0.8rem] py-[0.38rem] max-[600px]:px-[0.55rem] max-[600px]:py-[0.3rem] rounded-[6px] transition-all duration-[180ms] whitespace-nowrap hover:text-sky-700 hover:bg-sky-50"
            >
              GitHub
            </a>
            <a
              href="https://discord.gg/JtQ54MSw6p"
              target="_blank"
              rel="noopener noreferrer"
              className="text-gray-700 no-underline text-[0.875rem] max-[600px]:text-[0.78rem] font-medium px-[0.8rem] py-[0.38rem] max-[600px]:px-[0.55rem] max-[600px]:py-[0.3rem] rounded-[6px] transition-all duration-[180ms] whitespace-nowrap hover:text-sky-700 hover:bg-sky-50"
            >
              Discord
            </a>
          </nav>
        </div>
      </header>

      <div className="relative overflow-hidden">
        {/* ══════════════════════════════
            SKY SECTION
        ══════════════════════════════ */}
        <section className="min-h-screen pt-[5.5rem] max-[600px]:pt-24 px-8 relative overflow-hidden flex flex-col items-center justify-start [background:linear-gradient(180deg,#dbeafe_0%,#bae6fd_24%,#7dd3fc_46%,#7dd3fc_66%,rgb(14,165,233)_66%,rgb(14,165,233)_100%)]">
          {/* Clouds layer */}
          <div
            className="absolute inset-0 pointer-events-none z-0 overflow-hidden"
            style={{ top: '48px' }}
            aria-hidden="true"
          >
            {clouds.map((cloud, i) => (
              <div
                key={i}
                className="cloud-pass"
                style={{
                  top: cloud.top,
                  '--cloud-duration': `${cloud.duration}s`,
                  '--cloud-delay': `${cloud.delay}s`,
                  '--cloud-width': `${cloud.width}px`,
                }}
              >
                <div
                  className="cloud-parallax"
                  style={{
                    transform: `translateX(${scrollY * cloud.parallax}px)`,
                  }}
                >
                  <div
                    className="cloud-float"
                    style={{
                      '--float-duration': `${cloud.floatDuration}s`,
                      '--float-x1': cloud.floatX1,
                      '--float-y1': cloud.floatY1,
                      '--float-x2': cloud.floatX2,
                      '--float-y2': cloud.floatY2,
                    }}
                  >
                    <div
                      className="cloud-scale"
                      style={{
                        '--scale-duration': `${cloud.scaleDuration}s`,
                        '--scale-from': cloud.scaleFrom,
                        '--scale-to': cloud.scaleTo,
                        '--opacity-from': cloud.opacityFrom,
                        '--opacity-to': cloud.opacityTo,
                        filter: `blur(${cloud.blur})`,
                      }}
                    >
                      <PolyCloud width={cloud.width} opacity={cloud.opacity} />
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Hero content */}
          <div className="relative z-[2] mt-12 max-[1536px]:mt-5 max-[1100px]:mt-4 text-center max-w-[820px] w-full">
            <div className="flex items-center justify-center gap-[1.1rem] mb-7 max-[1536px]:mb-4 max-[1100px]:mb-3 max-[600px]:flex-col max-[600px]:gap-2">
              <div className="relative">
                <Image
                  src={YaciStore}
                  alt="Yaci Store Logo"
                  height={92}
                  className="relative z-[2] max-[1536px]:h-[66px] max-[1100px]:h-[56px] w-auto"
                />
              </div>
              <span className="text-[clamp(1.8rem,3.5vw,2.6rem)] max-[1536px]:text-[2rem] max-[1100px]:text-[1.8rem] max-[600px]:text-[1.6rem] font-extrabold text-[#0c2340] tracking-[-0.5px]">
                Yaci Store
              </span>
            </div>

            <h1 className="text-[clamp(1.2rem,2.2vw,1.7rem)] max-[1536px]:text-[1.35rem] max-[1100px]:text-[1.2rem] font-semibold text-[#0c2340] leading-[1.35] mb-4 max-[1536px]:mb-3 max-[1100px]:mb-2">
              Cardano data infrastructure that goes deeper.
            </h1>
            <p className="text-[clamp(0.875rem,1.4vw,1rem)] max-[1536px]:text-[0.875rem] max-[1100px]:text-[0.85rem] text-[#1e3a5f] leading-[1.65] max-w-[520px] mx-auto mb-9 max-[1536px]:mb-5 max-[1100px]:mb-4">
              Launch fast with a Blockfrost-compatible API, then go deeper with
              a self-hosted, modular indexer built for scale.
            </p>

            <div className="flex gap-[0.875rem] justify-center flex-wrap mb-8 max-[1536px]:mb-4 max-[1100px]:mb-3">
              <Link
                href="/docs/getting-started/overview"
                className={`${btnBase} bg-sky-700 text-white border-sky-700 hover:bg-sky-800 hover:-translate-y-0.5 hover:shadow-[0_8px_18px_rgba(3,105,161,0.35)]`}
              >
                <RocketIcon /> Get Started
              </Link>
              <Link
                href="/docs/api-reference/api-overview"
                className={`${btnBase} bg-transparent text-sky-800 border-sky-800 hover:bg-sky-800 hover:text-white hover:-translate-y-0.5`}
              >
                <BookOpenIcon /> Browse the API
              </Link>
              <a
                href="https://github.com/bloxbean/yaci-store"
                target="_blank"
                rel="noopener noreferrer"
                className={`${btnBase} bg-transparent text-sky-800 border-sky-800 hover:bg-sky-800 hover:text-white hover:-translate-y-0.5`}
              >
                <StarIcon /> GitHub
              </a>
            </div>
          </div>

          {/* Iceberg tip */}
          <div
            className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[clamp(380px,56vw,740px)] z-[1] pointer-events-none"
            aria-hidden="true"
          >
            <IcebergTip />
          </div>

          {/* Water shimmer line */}
          <div className="absolute bottom-0 left-0 right-0 h-10 bg-gradient-to-b from-transparent to-sky-500/45 z-0" />

          {/* Scroll hint */}
          <div className="scroll-hint absolute bottom-6 left-1/2 -translate-x-1/2 text-sky-800 flex flex-col items-center gap-[0.3rem] text-[0.72rem] font-bold tracking-[0.1em] uppercase pointer-events-none z-[3]">
            <span>Dive deeper</span>
            <ScrollHintArrow />
          </div>
        </section>

        {/* Wave: sky → ocean */}
        <div className="block w-full overflow-hidden leading-none bg-transparent relative z-[2] -mt-1">
          <WaveDivider />
        </div>

        {/* ══════════════════════════════
            OCEAN SECTION
        ══════════════════════════════ */}
        <div className="relative [background:linear-gradient(180deg,#0369a1_0%,#075985_12%,#1e3a5f_28%,#0c2340_48%,#071527_70%,#040e1d_85%,#020912_100%)] overflow-hidden">
          {/* Large background iceberg */}
          <div
            className="absolute top-0 left-1/2 -translate-x-1/2 w-[clamp(380px,56vw,740px)] h-full pointer-events-none z-0"
            aria-hidden="true"
          >
            <IcebergBackground />
          </div>

          {/* Floating bubbles */}
          <div aria-hidden="true">
            {[
              { l: '7%', b: '5%', s: 12, d: '24s', dl: '0s' },
              { l: '18%', b: '12%', s: 8, d: '18s', dl: '4s' },
              { l: '30%', b: '3%', s: 18, d: '32s', dl: '9s' },
              { l: '48%', b: '8%', s: 10, d: '21s', dl: '2s' },
              { l: '63%', b: '15%', s: 15, d: '27s', dl: '7s' },
              { l: '78%', b: '4%', s: 9, d: '22s', dl: '13s' },
              { l: '89%', b: '10%', s: 13, d: '19s', dl: '5s' },
            ].map((b, i) => (
              <div
                key={i}
                className="absolute rounded-full bg-white/[0.05] border border-white/[0.1] pointer-events-none z-[1]"
                style={{
                  left: b.l,
                  bottom: b.b,
                  width: b.s,
                  height: b.s,
                  animationName: 'rise',
                  animationDuration: b.d,
                  animationTimingFunction: 'linear',
                  animationIterationCount: 'infinite',
                  animationDelay: b.dl,
                }}
              />
            ))}
          </div>

          {/* Swimming fish */}
          <div
            className="absolute inset-0 pointer-events-none z-[1] overflow-hidden"
            aria-hidden="true"
          >
            <div
              className="fish-swim"
              style={{
                top: '5%',
                animationDuration: '22s',
                animationDelay: '-11s',
              }}
            >
              <Fish width={52} opacity={0.2} flip />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '8%',
                animationDuration: '18s',
                animationDelay: '-5s',
              }}
            >
              <Fish width={38} opacity={0.16} flip />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '22%',
                animationDuration: '28s',
                animationDelay: '-20s',
              }}
            >
              <Fish width={58} opacity={0.18} flip />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '25%',
                animationDuration: '20s',
                animationDelay: '-8s',
              }}
            >
              <Fish width={42} opacity={0.14} flip />
            </div>
            <div
              className="fish-swim-r"
              style={{
                top: '18%',
                animationDuration: '30s',
                animationDelay: '-14s',
              }}
            >
              <Fish width={48} opacity={0.15} />
            </div>
            <div
              className="fish-swim-r"
              style={{
                top: '35%',
                animationDuration: '24s',
                animationDelay: '-9s',
              }}
            >
              <Fish width={36} opacity={0.13} />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '45%',
                animationDuration: '26s',
                animationDelay: '-3s',
              }}
            >
              <Fish width={60} opacity={0.14} flip />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '52%',
                animationDuration: '32s',
                animationDelay: '-25s',
              }}
            >
              <Fish width={44} opacity={0.12} flip />
            </div>
            <div
              className="fish-swim-r"
              style={{
                top: '60%',
                animationDuration: '28s',
                animationDelay: '-7s',
              }}
            >
              <Fish width={50} opacity={0.12} />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '72%',
                animationDuration: '36s',
                animationDelay: '-18s',
              }}
            >
              <Fish width={56} opacity={0.1} flip />
            </div>
            <div
              className="fish-swim-r"
              style={{
                top: '80%',
                animationDuration: '30s',
                animationDelay: '-22s',
              }}
            >
              <Fish width={40} opacity={0.09} />
            </div>
            <div
              className="fish-swim"
              style={{
                top: '88%',
                animationDuration: '42s',
                animationDelay: '-30s',
              }}
            >
              <Fish width={64} opacity={0.08} flip />
            </div>
          </div>

          {/* ── Feature cards ── */}
          <div className="relative z-[2] max-w-[1200px] mx-auto px-8 max-[600px]:px-4">
            {/* DEPTH 1: Scoped Indexer ~30 m */}
            <div className={depthLeft}>
              <div className={featCard}>
                <div className={depthLabel}><ChevronDownIcon /> ~30 m depth</div>
                <div className={cardEyebrow}>Scoped Indexer</div>
                <h2 className={cardTitle}>
                  Index only what you need. Not a byte more.
                </h2>
                <p className={cardBody}>
                  Enable only the data pipelines your application actually uses
                  — keeping your database lean and your queries fast.
                </p>
                <div className="flex flex-wrap gap-[0.45rem] mb-5">
                  {[
                    'UTxO Store',
                    'Block Store',
                    'Transaction Store',
                    'Asset Store',
                    'Governance Store',
                    'Staking Store',
                    'Script Store',
                    'Epoch Store',
                    'Metadata Store',
                    'Reward Store',
                  ].map((s) => (
                    <span key={s} className={pill}>
                      {s}
                    </span>
                  ))}
                </div>
                <div className={cardInfoRow}>
                  <span className="text-sky-300 shrink-0"><DatabaseIcon /></span>
                  <div>
                    <div className={infoTitle}>
                      SQL Access &amp; Custom Backends
                    </div>
                    <div className={infoDesc}>
                      Query via raw SQL or plug in any storage implementation.
                    </div>
                    <div className="flex flex-wrap gap-[0.4rem] mt-2">
                      {['PostgreSQL', 'H2', 'MySQL', 'Custom'].map((db) => (
                        <span
                          key={db}
                          className="bg-sky-400/[0.12] border border-sky-400/25 rounded px-2 py-[0.15rem] text-[0.7rem] font-bold text-sky-300"
                        >
                          {db}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
                <Link href="/docs/introduction/modules" className={btnGlass}>
                  Explore Stores →
                </Link>
              </div>
            </div>

            {/* DEPTH 2: Blockfrost-Compatible API ~80 m */}
            <div className={depthRight}>
              <div className={featCard} style={{ transitionDelay: '0.1s' }}>
                <div className={depthLabel}><ChevronDownIcon /> ~80 m depth</div>
                <div className={cardEyebrow}>Blockfrost-Compatible API</div>
                <h2 className={cardTitle}>Drop in, don&apos;t rewrite.</h2>
                <p className={cardBody}>
                  Swap your Blockfrost endpoint for Yaci Store and keep your
                  existing code. Own your infrastructure, own your data.
                </p>
                <div className="flex items-start gap-3 bg-white/[0.05] border border-white/[0.09] rounded-[10px] px-4 py-[0.9rem] mb-3 font-mono text-[0.78rem]">
                  <div className="w-full">
                    <div className="text-green-300 mb-[0.3rem]">
                      GET /api/v1/addresses/{'{address}'}/utxos
                    </div>
                    <div className="text-slate-400">{'{'}</div>
                    <div className="text-slate-400 pl-4">
                      &quot;utxo_count&quot;: 42,
                    </div>
                    <div className="text-slate-400 pl-4">
                      {'"amount": [{"unit": "lovelace", "quantity": "..."}]'}
                    </div>
                    <div className="text-slate-400">{'}'}</div>
                  </div>
                </div>
                <div className="flex flex-wrap gap-[0.45rem] mb-5">
                  {[
                    '50+ endpoints',
                    'Addresses',
                    'Transactions',
                    'Blocks',
                    'Assets',
                    'Epochs',
                    'Pools',
                  ].map((s) => (
                    <span key={s} className={pill}>
                      {s}
                    </span>
                  ))}
                </div>
                <Link
                  href="/docs/api-reference/api-overview"
                  className={btnGlass}
                >
                  API Reference →
                </Link>
              </div>
            </div>

            {/* DEPTH 3: Plugin System ~250 m */}
            <div className={depthLeft}>
              <div className={featCard}>
                <div className={depthLabel}><ChevronDownIcon /> ~250 m depth</div>
                <div className={cardEyebrow}>Plugin System</div>
                <h2 className={cardTitle}>Your logic. Your language.</h2>
                <p className={cardBody}>
                  Hook into the live data stream and filter, transform, or
                  enrich blockchain events using plugins in the language you
                  already know.
                </p>
                <div className="flex gap-[0.7rem] flex-wrap mb-5">
                  <div className="flex items-center gap-2 px-4 py-2 rounded-[9px] font-bold text-[0.85rem] border border-blue-300/[0.28] bg-blue-300/[0.1] text-blue-300 transition-all hover:-translate-y-0.5">
                    Python
                  </div>
                  <div className="flex items-center gap-2 px-4 py-2 rounded-[9px] font-bold text-[0.85rem] border border-amber-200/[0.28] bg-amber-200/[0.1] text-amber-200 transition-all hover:-translate-y-0.5">
                    <BoltIcon /> JavaScript
                  </div>
                  <div className="flex items-center gap-2 px-4 py-2 rounded-[9px] font-bold text-[0.85rem] border border-green-300/[0.28] bg-green-300/[0.1] text-green-300 transition-all hover:-translate-y-0.5">
                    <WrenchIcon /> MVEL
                  </div>
                </div>
                <p className="text-[0.82rem] text-slate-400 mb-5 leading-[1.6]">
                  No recompilation. Scripts are hot-loaded at runtime, evaluated
                  per block event, and can write directly to your storage
                  backend.
                </p>
                <Link
                  href="/docs/v2/plugins/plugin-api-guide"
                  className={btnGlass}
                >
                  Plugin Docs →
                </Link>
              </div>
            </div>

            {/* DEPTH 4: Aggregated Data ~1 000 m */}
            <div className={depthRight}>
              <div className={featCard} style={{ transitionDelay: '0.1s' }}>
                <div className={depthLabel}><ChevronDownIcon /> ~1,000 m depth</div>
                <div className={cardEyebrow}>Aggregated Data</div>
                <h2 className={cardTitle}>
                  Deep computations. Surface-ready results.
                </h2>
                <p className={cardBody}>
                  Node-level calculations — no external services, no oracles.
                  Just your Cardano node and Yaci Store.
                </p>
                <div className="grid grid-cols-2 max-[600px]:grid-cols-1 gap-[0.65rem] mb-5">
                  {[
                    {
                      icon: <VoteIcon />,
                      title: 'Governance',
                      desc: 'DRep distribution, voting power & proposals',
                    },
                    {
                      icon: <BarChartIcon />,
                      title: 'Stake Distribution',
                      desc: 'Per-epoch pool delegation snapshots',
                    },
                    {
                      icon: <CoinsIcon />,
                      title: 'Account Values',
                      desc: 'ADA & multi-asset balances from UTxOs',
                    },
                    {
                      icon: <AwardIcon />,
                      title: 'Rewards',
                      desc: 'Epoch rewards with node-level precision',
                    },
                  ].map((cell) => (
                    <div
                      key={cell.title}
                      className="bg-white/[0.05] border border-white/[0.09] rounded-[10px] px-[0.9rem] py-[0.85rem]"
                    >
                      <span className="text-sky-300 mb-[0.35rem] block">
                        {cell.icon}
                      </span>
                      <div className="text-[0.82rem] font-bold text-slate-50 mb-[0.2rem]">
                        {cell.title}
                      </div>
                      <div className="text-[0.74rem] text-slate-400 leading-[1.5]">
                        {cell.desc}
                      </div>
                    </div>
                  ))}
                </div>
                <Link href="/docs/introduction/overview" className={btnGlass}>
                  Learn More →
                </Link>
              </div>
            </div>

            {/* DEPTH 5: Networks ~3 000 m */}
            <div className={depthLeft}>
              <div className={featCard}>
                <div className={depthLabel}><ChevronDownIcon /> ~3,000 m depth</div>
                <div className={cardEyebrow}>Network Support</div>
                <h2 className={cardTitle}>
                  Every Cardano network, out of the box.
                </h2>
                <p className={cardBody}>
                  One config change to switch between Mainnet, Preprod, or
                  Preview. Or connect to a fully custom private devnet.
                </p>
                <div className="flex gap-[0.7rem] flex-wrap mb-5">
                  {[
                    {
                      label: 'Mainnet',
                      dot: 'bg-green-500 shadow-[0_0_6px_#22c55e]',
                    },
                    {
                      label: 'Preprod',
                      dot: 'bg-amber-400 shadow-[0_0_6px_#fbbf24]',
                    },
                    {
                      label: 'Preview',
                      dot: 'bg-blue-400 shadow-[0_0_6px_#60a5fa]',
                    },
                    {
                      label: 'Custom Devnet',
                      dot: 'bg-violet-400 shadow-[0_0_6px_#a78bfa]',
                    },
                  ].map((net) => (
                    <div
                      key={net.label}
                      className="flex items-center gap-2 px-[1.1rem] py-2 rounded-[30px] text-[0.85rem] font-semibold border border-white/[0.16] bg-white/[0.06] text-slate-50"
                    >
                      <span
                        className={`w-2 h-2 rounded-full shrink-0 ${net.dot}`}
                      />
                      {net.label}
                    </div>
                  ))}
                </div>
                <div className={cardInfoRow}>
                  <span className="text-violet-300 shrink-0"><FlaskIcon /></span>
                  <div>
                    <div className={infoTitle}>Want your own devnet?</div>
                    <div className={infoDesc}>
                      Yaci DevKit spins up a private Cardano network in seconds
                      — fully compatible with Yaci Store.
                    </div>
                    <div className="mt-[0.65rem]">
                      <a
                        href="https://devkit.yaci.xyz"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-[0.8rem] px-4 py-[0.4rem] rounded-lg font-semibold no-underline border-2 inline-flex items-center gap-[0.4rem] transition-all duration-[220ms] cursor-pointer whitespace-nowrap bg-violet-400/[0.14] text-violet-300 border-violet-400/[0.38] hover:bg-violet-400/[0.26] hover:-translate-y-0.5"
                      >
                        Discover Yaci DevKit →
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* DEPTH 6: Java Library — Ocean floor */}
            <div className={depthCenter}>
              <div
                className="feat-card reveal bg-[rgba(8,28,55,0.72)] backdrop-blur-[20px] border border-white/[0.13] rounded-[22px] p-8 max-[600px]:p-6 max-w-[520px] w-full text-center text-slate-50 shadow-[0_8px_40px_rgba(0,0,0,0.45)] hover:shadow-[0_12px_48px_rgba(0,0,0,0.6)]"
                style={{ transitionDelay: '0.05s' }}
              >
                <div className="inline-flex items-center justify-center gap-[0.45rem] bg-white/[0.06] border border-white/[0.12] rounded-[20px] px-[0.85rem] py-[0.25rem] text-[0.72rem] font-semibold text-white/40 uppercase tracking-[0.09em] mb-4">
                  <ChevronDownIcon /> Ocean floor
                </div>
                <div className={cardEyebrow}>Java Library</div>
                <h2 className={cardTitle}>
                  Yaci Store is also a Java library.
                </h2>
                <p className={cardBody}>
                  Embed the full indexer inside your Spring Boot application.
                  Select only the stores you need, plug in your own storage, and
                  keep your footprint minimal.
                </p>
                <div className="flex gap-[0.9rem] flex-wrap mb-6 justify-center">
                  {[
                    { d: '5s', dl: '0s' },
                    { d: '6.5s', dl: '1s' },
                    { d: '4.5s', dl: '0.5s' },
                    { d: '5.8s', dl: '1.8s' },
                    { d: '7s', dl: '2.5s' },
                  ].map((b, i) => (
                    <div
                      key={i}
                      className="ice-block w-[72px] h-[72px] bg-sky-200/[0.06] border border-sky-200/20 rounded-[11px] flex items-center justify-center text-sky-200/60 shadow-[0_0_20px_rgba(186,230,253,0.04)]"
                      style={{ animationDuration: b.d, animationDelay: b.dl }}
                    >
                      <CoffeeIcon />
                    </div>
                  ))}
                </div>
                <div className="mb-5">
                  {[
                    'yaci-store-utxo-spring-boot-starter',
                    'yaci-store-blocks-spring-boot-starter',
                    'yaci-store-assets-spring-boot-starter',
                    'yaci-store-governance-spring-boot-starter',
                    'yaci-store-staking-spring-boot-starter',
                  ].map((s) => (
                    <div
                      key={s}
                      className="flex items-center gap-[0.6rem] px-3 py-[0.55rem] mb-[0.4rem] bg-white/[0.04] border border-white/[0.07] rounded-[7px] text-[0.75rem] text-slate-400 font-mono"
                    >
                      <CoffeeSmIcon />
                      {s}
                    </div>
                  ))}
                </div>
                <div className="flex gap-[0.65rem] justify-center flex-wrap">
                  <Link
                    href="/docs/introduction/spring-boot-starters"
                    className={btnGlass}
                  >
                    Spring Boot Starters →
                  </Link>
                  <Link href="/docs/usage/as-library" className={btnGhost}>
                    Library Guide →
                  </Link>
                </div>
              </div>
            </div>
          </div>
          {/* end ocean-content */}

          {/* Ocean floor SVG */}
          <div
            className="relative z-[2] w-full leading-none mt-8"
            aria-hidden="true"
          >
            <OceanFloor />
          </div>
        </div>
        {/* end .ocean */}

        {/* ══════════════════════════════
            TRUSTED BY
        ══════════════════════════════ */}
        <section className="bg-[#060f1e] px-8 py-[5.5rem] text-slate-50">
          <div className="max-w-[1100px] mx-auto">
            <div className="text-[0.75rem] font-bold uppercase tracking-[0.12em] text-sky-300 mb-2 text-center">
              Trusted By
            </div>
            <h2 className="text-[clamp(1.6rem,3vw,2.4rem)] font-extrabold text-slate-50 mb-3 text-center leading-[1.2]">
              Built for the Cardano ecosystem
            </h2>
            <p className="text-base text-slate-400 text-center max-w-[500px] mx-auto mb-12 leading-[1.7]">
              From the Cardano Foundation to independent DeFi teams, Yaci Store
              powers production infrastructure across the ecosystem.
            </p>

            <div className="grid grid-cols-[repeat(auto-fill,minmax(310px,1fr))] max-[600px]:grid-cols-1 gap-[1.1rem] mb-14">
              {projects.map((p, i) => (
                <div
                  key={p.name}
                  className={`proj-card reveal border rounded-[14px] p-6 flex gap-4 items-start ${
                    p.featured
                      ? 'border-green-500/25 bg-green-500/[0.04] hover:border-green-500/44'
                      : 'border-white/[0.08] bg-white/[0.04] hover:border-sky-300/[0.22] hover:bg-white/[0.07]'
                  }`}
                  style={{ transitionDelay: `${(i % 4) * 0.08}s` }}
                >
                  <div className={`shrink-0 mt-0.5 ${p.featured ? 'text-green-400' : 'text-sky-400'}`}>
                    {p.icon}
                  </div>
                  <div>
                    <div
                      className={`text-[0.97rem] font-bold mb-[0.4rem] ${p.featured ? 'text-green-400' : 'text-slate-50'}`}
                    >
                      {p.name}
                    </div>
                    <p className="text-[0.825rem] text-slate-400 leading-[1.6] mb-[0.875rem]">
                      {p.desc}
                    </p>
                    <div className="flex gap-2 flex-wrap">
                      <a
                        href={p.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={p.featured ? projLinkFeatured : projLink}
                      >
                        {p.href.includes('github') ? 'GitHub' : 'Visit Site'}
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ══════════════════════════════
            COMMUNITY
        ══════════════════════════════ */}
        <section className="[background:linear-gradient(135deg,#050d1a_0%,#0c1a2e_100%)] px-8 py-[4.5rem] text-center text-slate-50 border-t border-white/[0.05]">
          <h2 className="text-[1.85rem] font-extrabold mb-[0.875rem]">
            Join the Community
          </h2>
          <p className="text-[0.975rem] text-slate-400 mb-9 max-w-[450px] mx-auto leading-[1.7]">
            Connect with developers, get support, and help shape the future of
            Cardano infrastructure tooling.
          </p>
          <div className="flex justify-center gap-[0.875rem] flex-wrap">
            <a
              href="https://github.com/bloxbean/yaci-store"
              target="_blank"
              rel="noopener noreferrer"
              className={commLink}
            >
              <GitHubIcon />
              GitHub
            </a>
            <a
              href="https://discord.gg/JtQ54MSw6p"
              target="_blank"
              rel="noopener noreferrer"
              className={commLink}
            >
              <DiscordIcon />
              Discord
            </a>
            <a href="/docs" className={commLink}>
              <DocsIcon />
              Documentation
            </a>
            <a
              href="https://x.com/bloxbean"
              target="_blank"
              rel="noopener noreferrer"
              className={commLink}
            >
              <XIcon />X / Twitter
            </a>
          </div>
        </section>
      </div>
      {/* end .scene */}

      {/* ── Back to top ── */}
      <button
        className={`back-top fixed bottom-8 right-8 z-[1001] w-[46px] h-[46px] rounded-full bg-sky-700/[0.82] backdrop-blur-[14px] border border-white/[0.22] text-white cursor-pointer flex items-center justify-center shadow-[0_4px_20px_rgba(0,0,0,0.35)] hover:bg-sky-800/95${showBackTop ? ' visible' : ''}`}
        onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        aria-label="Back to top"
      >
        <ArrowUpIcon />
      </button>
    </>
  );
}
