import React from 'react'
import { DocsThemeConfig } from 'nextra-theme-docs'

const config: DocsThemeConfig = {
  logo: <span><b>Yaci Store</b></span>,
  project: {
    link: 'https://github.com/bloxbean/yaci-store',
  },
  chat: {
    link: 'https://discord.gg/JtQ54MSw6p',
  },
  docsRepositoryBase: 'https://github.com/bloxbean/yaci-store/tree/main/docs',
  footer: {
    text: '© 2024 BloxBean project',
  },
   useNextSeoProps() {
        return {
            titleTemplate: '%s – Yaci Store'
        }
   },
    head: (
        <>
            <meta property="description" content="Yaci Store - A modular Java library for developers who are keen on building their own custom indexer for Cardano"/>
            <meta property="og:title" content="Yaci Store - A modular Java library for developers who are keen on building their own custom indexer for Cardano"/>
            <meta property="og:description" content="Yaci Store - A modular Java library for developers who are keen on building their own custom indexer for Cardano"/>
        </>
    )
}

export default config
