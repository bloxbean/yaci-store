import { useId } from 'react';

/* ─── Fish ─── */
interface FishProps {
  width?: number;
  opacity?: number;
  flip?: boolean;
}

export function Fish({ width = 64, opacity = 0.22, flip = false }: FishProps) {
  return (
    <svg
      width={width}
      height={width * 0.45}
      viewBox="0 0 80 36"
      fill="none"
      style={{ transform: flip ? 'scaleX(-1)' : 'none', display: 'block' }}
    >
      <polygon
        points="16,18 40,4 70,11 80,18 70,25 40,32"
        fill={`rgba(255,255,255,${opacity})`}
        stroke={`rgba(255,255,255,${opacity + 0.18})`}
        strokeWidth="1.2"
        strokeLinejoin="round"
      />
      <polygon
        points="16,18 1,7 3,18 1,29"
        fill={`rgba(255,255,255,${opacity})`}
        stroke={`rgba(255,255,255,${opacity + 0.18})`}
        strokeWidth="1.2"
        strokeLinejoin="round"
      />
      <circle
        cx="60"
        cy="15"
        r="2.2"
        fill={`rgba(255,255,255,${opacity + 0.35})`}
      />
    </svg>
  );
}

/* ─── PolyCloud ─── */
interface PolyCloudProps {
  width?: number;
  o?: number;
}

export function PolyCloud({ width = 220, o = 0.9 }: PolyCloudProps) {
  const clipId = useId();
  const a = (v: number) => Math.max(0, Math.min(1, v));

  const cloudPath = `
    M18 86
    C18 73 28 64 41 63
    C44 46 60 35 78 37
    C86 23 104 16 121 22
    C132 15 149 14 162 21
    C176 24 188 34 193 47
    C208 48 218 59 218 72
    C218 86 205 96 188 97
    C182 104 171 108 158 108
    H49
    C29 108 18 98 18 86
    Z
  `;

  return (
    <svg width={width} height={width * 0.52} viewBox="0 0 220 114" fill="none">
      <defs>
        <clipPath id={clipId}>
          <path d={cloudPath} />
        </clipPath>
      </defs>

      <path d={cloudPath} fill={`rgba(255,255,255,${a(o)})`} />

      <path
        d="M43 63 C57 49 75 42 94 44 C107 33 126 29 145 33 C159 34 173 40 184 49"
        fill="none"
        stroke={`rgba(255,255,255,${a(o * 0.26)})`}
        strokeWidth="5"
        strokeLinecap="round"
      />

      <g clipPath={`url(#${clipId})`}>
        <polygon
          points="24,86 48,54 70,64 53,82 30,92"
          fill={`rgba(255,255,255,${a(o - 0.1)})`}
        />
        <polygon
          points="48,54 84,36 101,58 70,64"
          fill={`rgba(255,255,255,${a(o + 0.03)})`}
        />
        <polygon
          points="84,36 121,22 134,52 101,58"
          fill={`rgba(255,255,255,${a(o - 0.04)})`}
        />
        <polygon
          points="121,22 160,22 178,48 134,52"
          fill={`rgba(255,255,255,${a(o + 0.05)})`}
        />
        <polygon
          points="160,22 193,47 178,48"
          fill={`rgba(255,255,255,${a(o - 0.09)})`}
        />
        <polygon
          points="53,82 101,58 96,88 48,101"
          fill={`rgba(255,255,255,${a(o - 0.02)})`}
        />
        <polygon
          points="101,58 134,52 156,76 96,88"
          fill={`rgba(255,255,255,${a(o + 0.02)})`}
        />
        <polygon
          points="134,52 178,48 199,73 156,76"
          fill={`rgba(255,255,255,${a(o - 0.06)})`}
        />
        <polygon
          points="48,101 96,88 148,100 92,112 36,110"
          fill={`rgba(225,240,255,${a(o * 0.55)})`}
        />
        <polygon
          points="96,88 156,76 190,96 148,100"
          fill={`rgba(215,235,255,${a(o * 0.6)})`}
        />
        <path
          d="M0 84 C40 91 77 93 112 91 C148 89 183 92 220 88 V114 H0 Z"
          fill={`rgba(190,220,255,${a(o * 0.24)})`}
        />
      </g>

      <path
        d={cloudPath}
        fill="none"
        stroke={`rgba(255,255,255,${a(o * 0.18)})`}
        strokeWidth="1.1"
      />
    </svg>
  );
}

/* ─── IcebergTip ─── */
export function IcebergTip() {
  return (
    <svg viewBox="0 0 600 300" fill="none" xmlns="http://www.w3.org/2000/svg">
      <polygon points="298,29 318,83 300,136 277,95" fill="#bff4ff" />
      <polygon points="277,95 300,136 252,202 238,144" fill="#d7f7ff" />
      <polygon points="300,136 342,115 386,199 318,173" fill="#66baf7" />
      <polygon points="298,29 346,110 318,83" fill="#9ce3ff" />
      <polygon points="318,83 346,110 342,115 300,136" fill="#3f88d8" />
      <polygon points="252,202 300,136 318,173 278,237" fill="#4a95e8" />
      <polygon points="318,173 386,199 362,254 278,237" fill="#7cc9ff" />
      <polygon points="238,144 252,202 203,237" fill="#eefcff" />
      <polygon points="203,237 278,237 228,300 154,283" fill="#f4fdff" />
      <polygon points="278,237 362,254 300,289 228,300" fill="#ffffff" />
      <polygon points="362,254 459,298 300,289" fill="#daf6ff" />
      <polygon points="386,199 450,271 459,298 362,254" fill="#a8e8ff" />
      <polygon points="203,237 154,283 125,277 169,240" fill="#9adcf8" />
      <polygon points="459,298 521,300 484,289" fill="#83d6f3" />
    </svg>
  );
}

/* ─── ScrollHintArrow ─── */
export function ScrollHintArrow() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <line x1="12" y1="5" x2="12" y2="19" />
      <polyline points="19 12 12 19 5 12" />
    </svg>
  );
}

/* ─── WaveDivider ─── */
export function WaveDivider() {
  return (
    <svg
      viewBox="0 0 1440 68"
      preserveAspectRatio="none"
      style={{ display: 'block', width: '100%', height: '68px' }}
    >
      <path d="M0,0 L1440,0 L1440,68 L0,68 Z" fill="#0ea5e9" />
      <path
        d="M0,34 C180,68 360,0 540,34 C720,68 900,4 1080,34 C1260,64 1440,14 1440,34 L1440,68 L0,68 Z"
        fill="#0369a1"
      />
    </svg>
  );
}

/* ─── IcebergBackground ─── */
export function IcebergBackground() {
  return (
    <svg
      viewBox="0 0 600 3200"
      fill="none"
      preserveAspectRatio="xMidYMin meet"
      xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%' }}
    >
      <defs>
        <linearGradient
          id="ibg"
          x1="0"
          y1="0"
          x2="0"
          y2="1"
          gradientUnits="objectBoundingBox"
        >
          <stop offset="0%" stopColor="#2f6ea8" stopOpacity="0.88" />
          <stop offset="10%" stopColor="#275a89" stopOpacity="0.89" />
          <stop offset="24%" stopColor="#214d76" stopOpacity="0.91" />
          <stop offset="44%" stopColor="#1f4d79" stopOpacity="0.93" />
          <stop offset="66%" stopColor="#173654" stopOpacity="0.95" />
          <stop offset="84%" stopColor="#10243a" stopOpacity="0.97" />
          <stop offset="100%" stopColor="#08111d" stopOpacity="0.99" />
        </linearGradient>
        <linearGradient
          id="ihl"
          x1="0"
          y1="0"
          x2="1"
          y2="0"
          gradientUnits="objectBoundingBox"
        >
          <stop offset="0%" stopColor="#ffffff" stopOpacity="0.15" />
          <stop offset="38%" stopColor="#ffffff" stopOpacity="0.05" />
          <stop offset="100%" stopColor="#ffffff" stopOpacity="0" />
        </linearGradient>
        <clipPath id="iceClip">
          <path d="M 78,0 L 524,0 L 458,220 L 410,480 L 468,820 L 432,1180 L 392,1540 L 418,1880 L 392,2220 L 430,2520 L 408,2820 L 388,3050 L 390,3200 L 210,3200 L 212,3050 L 192,2820 L 170,2520 L 208,2220 L 182,1880 L 158,1540 L 172,1180 L 146,820 L 118,420 L 136,220 Z" />
        </clipPath>
      </defs>
      <path
        d="M 78,0 L 524,0 L 458,220 L 410,480 L 468,820 L 432,1180 L 392,1540 L 418,1880 L 392,2220 L 430,2520 L 408,2820 L 388,3050 L 390,3200 L 210,3200 L 212,3050 L 192,2820 L 170,2520 L 208,2220 L 182,1880 L 158,1540 L 172,1180 L 146,820 L 118,420 L 136,220 Z"
        fill="url(#ibg)"
      />
      <g
        clipPath="url(#iceClip)"
        stroke="#dff7ff"
        strokeOpacity="0.10"
        strokeWidth="1"
      >
        <polygon
          points="78,0 230,0 190,230 136,220"
          fill="#2c618f"
          fillOpacity="0.46"
        />
        <polygon
          points="230,0 360,0 315,290 190,230"
          fill="#204d79"
          fillOpacity="0.38"
        />
        <polygon
          points="360,0 524,0 458,220 315,290"
          fill="#1d3f63"
          fillOpacity="0.52"
        />
        <polygon
          points="136,220 190,230 250,560 118,420"
          fill="#183653"
          fillOpacity="0.48"
        />
        <polygon
          points="190,230 315,290 338,560 250,560"
          fill="#285f96"
          fillOpacity="0.34"
        />
        <polygon
          points="315,290 458,220 410,480 338,560"
          fill="#214d76"
          fillOpacity="0.50"
        />
        <polygon
          points="118,420 250,560 232,900 146,820"
          fill="#244c74"
          fillOpacity="0.48"
        />
        <polygon
          points="250,560 338,560 334,930 232,900"
          fill="#2f6ea8"
          fillOpacity="0.32"
        />
        <polygon
          points="338,560 410,480 468,820 334,930"
          fill="#2a679d"
          fillOpacity="0.40"
        />
        <polygon
          points="146,820 232,900 248,1280 172,1180"
          fill="#1b3c5b"
          fillOpacity="0.55"
        />
        <polygon
          points="232,900 334,930 320,1360 248,1280"
          fill="#173654"
          fillOpacity="0.58"
        />
        <polygon
          points="334,930 468,820 432,1180 320,1360"
          fill="#214b74"
          fillOpacity="0.46"
        />
        <polygon
          points="172,1180 248,1280 266,1760 158,1540"
          fill="#1a3450"
          fillOpacity="0.60"
        />
        <polygon
          points="248,1280 320,1360 310,1820 266,1760"
          fill="#1f4d79"
          fillOpacity="0.50"
        />
        <polygon
          points="320,1360 432,1180 392,1540 310,1820"
          fill="#275a89"
          fillOpacity="0.44"
        />
        <polygon
          points="158,1540 266,1760 244,2230 182,1880"
          fill="#162f49"
          fillOpacity="0.66"
        />
        <polygon
          points="266,1760 310,1820 314,2250 244,2230"
          fill="#1b3d60"
          fillOpacity="0.58"
        />
        <polygon
          points="310,1820 392,1540 418,1880 314,2250"
          fill="#224f7d"
          fillOpacity="0.44"
        />
        <polygon
          points="182,1880 244,2230 224,2570 170,2520"
          fill="#17324d"
          fillOpacity="0.72"
        />
        <polygon
          points="244,2230 314,2250 306,2590 224,2570"
          fill="#214e7a"
          fillOpacity="0.50"
        />
        <polygon
          points="314,2250 418,1880 392,2220 306,2590"
          fill="#2a679d"
          fillOpacity="0.38"
        />
        <polygon
          points="170,2520 224,2570 246,2920 192,2820"
          fill="#12293f"
          fillOpacity="0.78"
        />
        <polygon
          points="224,2570 306,2590 342,2930 246,2920"
          fill="#1d4368"
          fillOpacity="0.56"
        />
        <polygon
          points="306,2590 430,2520 408,2820 342,2930"
          fill="#255b8e"
          fillOpacity="0.40"
        />
        <polygon
          points="192,2820 246,2920 210,3200 212,3050"
          fill="#0f2031"
          fillOpacity="0.82"
        />
        <polygon
          points="246,2920 342,2930 300,3200 210,3200"
          fill="#173756"
          fillOpacity="0.72"
        />
        <polygon
          points="342,2930 408,2820 390,3200 300,3200"
          fill="#1f4d79"
          fillOpacity="0.54"
        />
        <polygon
          points="190,230 250,560 315,290"
          fill="#356fa2"
          fillOpacity="0.16"
        />
        <polygon
          points="250,560 334,930 338,560"
          fill="#8fd8ff"
          fillOpacity="0.08"
        />
        <polygon
          points="232,900 320,1360 334,930"
          fill="#88d6ff"
          fillOpacity="0.07"
        />
        <polygon
          points="248,1280 310,1820 320,1360"
          fill="#7dcfff"
          fillOpacity="0.06"
        />
        <polygon
          points="266,1760 314,2250 310,1820"
          fill="#6fc7fb"
          fillOpacity="0.06"
        />
        <polygon
          points="244,2230 306,2590 314,2250"
          fill="#66baf7"
          fillOpacity="0.06"
        />
        <polygon
          points="246,2920 300,3200 342,2930"
          fill="#9adcf8"
          fillOpacity="0.05"
        />
      </g>
      <path
        d="M 78,0 L 136,220 L 118,420 L 146,820 L 172,1180 L 158,1540 L 182,1880 L 208,2220 L 170,2520 L 192,2820 L 210,3200 L 300,3200 L 300,0 Z"
        fill="url(#ihl)"
      />
      <g stroke="#ffffff" strokeOpacity="0.065" strokeWidth="0.8" fill="none">
        <line x1="300" y1="0" x2="300" y2="3200" strokeDasharray="8 18" />
        <line x1="315" y1="290" x2="172" y2="1180" />
        <line x1="315" y1="290" x2="432" y2="1180" />
        <line x1="334" y1="930" x2="158" y2="1540" />
        <line x1="334" y1="930" x2="392" y2="1540" />
        <line x1="320" y1="1360" x2="182" y2="1880" />
        <line x1="320" y1="1360" x2="418" y2="1880" />
        <line x1="314" y1="2250" x2="210" y2="3200" />
        <line x1="314" y1="2250" x2="390" y2="3200" />
      </g>
      <path
        d="M 78,0 L 524,0 L 458,220 L 410,480 L 468,820 L 432,1180 L 392,1540 L 418,1880 L 392,2220 L 430,2520 L 408,2820 L 388,3050 L 390,3200 L 210,3200 L 212,3050 L 192,2820 L 170,2520 L 208,2220 L 182,1880 L 158,1540 L 172,1180 L 146,820 L 118,420 L 136,220 Z"
        stroke="#dff7ff"
        strokeOpacity="0.16"
        strokeWidth="2"
      />
    </svg>
  );
}

/* ─── OceanFloor ─── */
export function OceanFloor() {
  return (
    <svg
      viewBox="0 0 1440 160"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      preserveAspectRatio="none"
    >
      <path
        d="M0,100 Q180,80 360,100 Q540,120 720,100 Q900,80 1080,105 Q1260,125 1440,100 L1440,160 L0,160 Z"
        fill="#060f1e"
      />
      <polygon points="120,100 150,55 180,100" fill="#0a1628" opacity="0.9" />
      <polygon points="145,100 162,72 178,100" fill="#0c1e38" opacity="0.8" />
      <polygon points="340,105 380,42 420,105" fill="#0a1628" opacity="0.9" />
      <polygon points="365,105 390,65 415,105" fill="#0d2040" opacity="0.75" />
      <polygon points="700,95 730,40 760,95" fill="#0a1628" opacity="0.9" />
      <polygon points="718,95 740,62 762,95" fill="#0c1e38" opacity="0.8" />
      <polygon points="980,100 1020,38 1060,100" fill="#0a1628" opacity="0.9" />
      <polygon
        points="1005,100 1030,60 1055,100"
        fill="#0d2040"
        opacity="0.78"
      />
      <polygon points="1280,95 1310,50 1340,95" fill="#0a1628" opacity="0.9" />
      <line
        x1="60"
        y1="100"
        x2="60"
        y2="68"
        stroke="#0d3a5c"
        strokeWidth="4"
        strokeLinecap="round"
      />
      <line
        x1="60"
        y1="80"
        x2="48"
        y2="68"
        stroke="#0d3a5c"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <line
        x1="60"
        y1="76"
        x2="72"
        y2="65"
        stroke="#0d3a5c"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <circle cx="60" cy="67" r="5" fill="#0d3a5c" />
      <circle cx="48" cy="67" r="4" fill="#0d3a5c" />
      <circle cx="72" cy="64" r="4" fill="#0d3a5c" />
      <line
        x1="240"
        y1="100"
        x2="240"
        y2="62"
        stroke="#0f4068"
        strokeWidth="4"
        strokeLinecap="round"
      />
      <line
        x1="240"
        y1="76"
        x2="225"
        y2="64"
        stroke="#0f4068"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <line
        x1="240"
        y1="72"
        x2="255"
        y2="60"
        stroke="#0f4068"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <line
        x1="240"
        y1="68"
        x2="232"
        y2="55"
        stroke="#0f4068"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      <circle cx="240" cy="61" r="5" fill="#0f4068" />
      <circle cx="225" cy="63" r="4" fill="#0f4068" />
      <circle cx="255" cy="59" r="4" fill="#0f4068" />
      <circle cx="232" cy="54" r="3.5" fill="#0f4068" />
      <line
        x1="850"
        y1="100"
        x2="850"
        y2="65"
        stroke="#0d3a5c"
        strokeWidth="4"
        strokeLinecap="round"
      />
      <line
        x1="850"
        y1="78"
        x2="837"
        y2="66"
        stroke="#0d3a5c"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <line
        x1="850"
        y1="74"
        x2="863"
        y2="62"
        stroke="#0d3a5c"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <circle cx="850" cy="64" r="5" fill="#0d3a5c" />
      <circle cx="837" cy="65" r="4" fill="#0d3a5c" />
      <circle cx="863" cy="61" r="4" fill="#0d3a5c" />
      <line
        x1="1180"
        y1="100"
        x2="1180"
        y2="60"
        stroke="#0f4068"
        strokeWidth="4"
        strokeLinecap="round"
      />
      <line
        x1="1180"
        y1="74"
        x2="1165"
        y2="62"
        stroke="#0f4068"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <line
        x1="1180"
        y1="70"
        x2="1194"
        y2="58"
        stroke="#0f4068"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <circle cx="1180" cy="59" r="5" fill="#0f4068" />
      <circle cx="1165" cy="61" r="4" fill="#0f4068" />
      <circle cx="1194" cy="57" r="4" fill="#0f4068" />
      {[80, 200, 460, 580, 820, 1100, 1380].map((x, i) => (
        <ellipse
          key={i}
          cx={x}
          cy={104 + (i % 3) * 3}
          rx={8 + (i % 4) * 3}
          ry={5 + (i % 2) * 2}
          fill="#0a1628"
          opacity="0.7"
        />
      ))}
    </svg>
  );
}

/* ─── Community icons ─── */
export function GitHubIcon() {
  return (
    <svg
      className="w-[1.1rem] h-[1.1rem] shrink-0"
      viewBox="0 0 24 24"
      fill="currentColor"
    >
      <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
    </svg>
  );
}

export function DiscordIcon() {
  return (
    <svg
      className="w-[1.1rem] h-[1.1rem] shrink-0"
      viewBox="0 0 24 24"
      fill="currentColor"
    >
      <path d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z" />
    </svg>
  );
}

export function DocsIcon() {
  return (
    <svg
      className="w-[1.1rem] h-[1.1rem] shrink-0"
      viewBox="0 0 24 24"
      fill="currentColor"
    >
      <path d="M19 2H6c-1.206 0-3 .799-3 3v14c0 2.201 1.794 3 3 3h15v-2H6.012C5.55 19.988 5 19.806 5 19s.55-.988 1.012-1H21V4c0-1.103-.897-2-2-2zm0 14H5V5c0-.806.55-.988 1-1h13v12z" />
      <path d="M9 6h2v2H9zm0 4h2v2H9zm0 4h2v2H9zm4-8h2v2h-2zm0 4h2v2h-2z" />
    </svg>
  );
}

export function XIcon() {
  return (
    <svg
      className="w-[1.1rem] h-[1.1rem] shrink-0"
      viewBox="0 0 24 24"
      fill="currentColor"
    >
      <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
    </svg>
  );
}

/* ─── ArrowUpIcon ─── */
export function ArrowUpIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <line x1="12" y1="19" x2="12" y2="5" />
      <polyline points="5 12 12 5 19 12" />
    </svg>
  );
}

/* ─── Shared stroke props helper ─── */
const S = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: '1.75',
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  viewBox: '0 0 24 24',
};

/* ─── Button icons ─── */
export function RocketIcon() {
  return (
    <svg className="w-[1rem] h-[1rem] shrink-0" {...S}>
      <path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z" />
      <path d="m12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z" />
      <path d="M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0" />
      <path d="M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5" />
    </svg>
  );
}

export function BookOpenIcon() {
  return (
    <svg className="w-[1rem] h-[1rem] shrink-0" {...S}>
      <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
      <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
    </svg>
  );
}

export function StarIcon() {
  return (
    <svg className="w-[1rem] h-[1rem] shrink-0" {...S}>
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  );
}

/* ─── Depth label chevron ─── */
export function ChevronDownIcon() {
  return (
    <svg className="w-[0.85rem] h-[0.85rem] shrink-0 inline-block" {...S} strokeWidth="2.5">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

/* ─── Plugin language icons ─── */
export function PythonIcon() {
  return (
    <svg className="w-[0.9rem] h-[0.9rem] shrink-0" {...S} strokeWidth="1.5">
      <path d="M12 2C8.5 2 6 3.5 6 5.5V9h6v1H4.5C3 10 2 11.5 2 13.5s1 3.5 2.5 3.5H6v-2.5c0-2 2.5-3 6-3s6 1 6 3V17h1.5c1.5 0 2.5-1.5 2.5-3.5S21 10 19.5 10H18V9h-6V5.5c0-2-2.5-3.5-0 0" />
      <circle cx="9" cy="6" r="1" fill="currentColor" stroke="none" />
      <circle cx="15" cy="17" r="1" fill="currentColor" stroke="none" />
      <path d="M6 9h6M12 15h6" />
    </svg>
  );
}

export function BoltIcon() {
  return (
    <svg className="w-[0.9rem] h-[0.9rem] shrink-0" {...S}>
      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
    </svg>
  );
}

export function WrenchIcon() {
  return (
    <svg className="w-[0.9rem] h-[0.9rem] shrink-0" {...S}>
      <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
    </svg>
  );
}

/* ─── Aggregated data cell icons ─── */
export function VoteIcon() {
  return (
    <svg className="w-5 h-5 shrink-0" {...S}>
      <path d="M9 11l3 3L22 4" />
      <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
    </svg>
  );
}

export function BarChartIcon() {
  return (
    <svg className="w-5 h-5 shrink-0" {...S}>
      <line x1="18" y1="20" x2="18" y2="10" />
      <line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" />
    </svg>
  );
}

export function CoinsIcon() {
  return (
    <svg className="w-5 h-5 shrink-0" {...S}>
      <circle cx="8" cy="8" r="6" />
      <path d="M18.09 10.37A6 6 0 1 1 10.34 18" />
      <path d="M7 6h1v4" />
      <line x1="16.71" y1="13.88" x2="13.14" y2="17.42" />
    </svg>
  );
}

export function AwardIcon() {
  return (
    <svg className="w-5 h-5 shrink-0" {...S}>
      <circle cx="12" cy="8" r="6" />
      <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11" />
    </svg>
  );
}

/* ─── Info card icons ─── */
export function DatabaseIcon() {
  return (
    <svg className="w-5 h-5 shrink-0" {...S}>
      <ellipse cx="12" cy="5" rx="9" ry="3" />
      <path d="M3 5v14c0 1.66 4.03 3 9 3s9-1.34 9-3V5" />
      <path d="M3 12c0 1.66 4.03 3 9 3s9-1.34 9-3" />
    </svg>
  );
}

export function FlaskIcon() {
  return (
    <svg className="w-5 h-5 shrink-0" {...S}>
      <path d="M9 3h6v6l3.5 6.5c.83 1.55-.17 3.5-2 3.5H7.5c-1.83 0-2.83-1.95-2-3.5L9 9V3z" />
      <line x1="9" y1="3" x2="15" y2="3" />
    </svg>
  );
}

/* ─── Java / Coffee icon (for floating blocks & starter list) ─── */
export function CoffeeIcon() {
  return (
    <svg className="w-7 h-7" {...S}>
      <path d="M17 8h1a4 4 0 0 1 0 8h-1" />
      <path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z" />
      <line x1="6" y1="2" x2="6" y2="4" />
      <line x1="10" y1="2" x2="10" y2="4" />
      <line x1="14" y1="2" x2="14" y2="4" />
    </svg>
  );
}

export function CoffeeSmIcon() {
  return (
    <svg className="w-[0.95rem] h-[0.95rem] shrink-0" {...S} strokeWidth="1.5">
      <path d="M17 8h1a4 4 0 0 1 0 8h-1" />
      <path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z" />
    </svg>
  );
}

/* ─── Project card icons ─── */
export function RefreshIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
      <path d="M21 3v5h-5" />
      <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
      <path d="M8 16H3v5" />
    </svg>
  );
}

export function BallotIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="m9 12 2 2 4-4" />
      <line x1="9" y1="7" x2="15" y2="7" />
      <line x1="9" y1="17" x2="12" y2="17" />
    </svg>
  );
}

export function BuildingIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <rect x="3" y="9" width="18" height="13" rx="1" />
      <path d="M8 22V9M16 22V9" />
      <path d="M1 9l11-6 11 6" />
      <rect x="9" y="14" width="6" height="8" />
    </svg>
  );
}

export function TagIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <path d="M12 2H2v10l9.29 9.29c.94.94 2.48.94 3.42 0l6.58-6.58c.94-.94.94-2.48 0-3.42L12 2Z" />
      <path d="M7 7h.01" />
    </svg>
  );
}

export function DropletIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <path d="M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z" />
    </svg>
  );
}

export function TrendingUpIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
      <polyline points="17 6 23 6 23 12" />
    </svg>
  );
}

export function ShieldIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  );
}

export function ServerIcon() {
  return (
    <svg className="w-6 h-6" {...S}>
      <rect x="2" y="2" width="20" height="8" rx="2" />
      <rect x="2" y="14" width="20" height="8" rx="2" />
      <line x1="6" y1="6" x2="6.01" y2="6" />
      <line x1="6" y1="18" x2="6.01" y2="18" />
    </svg>
  );
}
