const config = {
  logo: <span>Yaci Store Documentation</span>,
  project: {
    link: 'https://github.com/bloxbean/yaci-store',
  },
  docsRepositoryBase: 'https://github.com/bloxbean/yaci-store/tree/main/docs',
  footer: {
    text: 'Yaci Store Documentation',
  },
  useNextSeoProps() {
    return {
      titleTemplate: '%s – Yaci Store'
    }
  }
}

export default config
