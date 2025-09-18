import nextra from 'nextra'

const withNextra = nextra({
  latex: true,
  search: {
    codeblocks: false
  }
})

export default withNextra({
  reactStrictMode: true,
  async redirects() {
    return [
      {
        source: '/docs',
        destination: '/docs/v2.0.0/yacistore/overview',
        permanent: false,
      },
    ];
  },
})
