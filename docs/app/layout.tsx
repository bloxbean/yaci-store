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

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <Head />
      <body>{children}</body>
    </html>
  )
}
