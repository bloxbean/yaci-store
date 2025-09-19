const config = {
  logo: <span>Yaci Store</span>,
  project: {
    link: 'https://github.com/bloxbean/yaci-store',
  },
  docsRepositoryBase: 'https://github.com/bloxbean/yaci-store/tree/main/docs',
  footer: {
    text: '© 2025 BloxBean project',
  },
  useNextSeoProps() {
    return {
      titleTemplate: '%s – Yaci Store'
    }
  }
}

export default config
