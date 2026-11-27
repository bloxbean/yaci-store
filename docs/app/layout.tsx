import Script from 'next/script'
import { Head } from 'nextra/components'
import 'nextra-theme-docs/style.css'
import './globals.css'

export const metadata = {
  title: {
    template: '%s – Yaci Store',
    default: 'Yaci Store'
  },
  description: 'Yaci Store - A modular Java library for developers who are keen on building their own custom indexer for Cardano',
  icons: {
    icon: '/images/YaciStore.svg',
  },
  openGraph: {
    title: 'Yaci Store - A modular Java library for developers who are keen on building their own custom indexer for Cardano',
    description: 'Yaci Store - A modular Java library for developers who are keen on building their own custom indexer for Cardano'
  }
}

// Landing page theme, applied before first paint: the saved theme (next-themes' "theme" key, shared
// with the docs) or the OS preference. data-ys-js lets the landing page's scroll-reveal styles apply.
const landingThemeScript = `(function(){try{var d=document.documentElement;d.setAttribute('data-ys-js','');var t=localStorage.getItem('theme');if(t!=='light'&&t!=='dark'){t=window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark'}d.setAttribute('data-ys-theme',t)}catch(e){}})();`

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <Head />
      <body>
        <Script id="ys-theme" strategy="beforeInteractive">
          {landingThemeScript}
        </Script>
        {children}
      </body>
    </html>
  )
}
