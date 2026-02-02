import nextra from 'nextra'

const withNextra = nextra({
  latex: true,
  search: {
    codeblocks: false
  }
})

export default withNextra({
    images: {
        unoptimized: true,
    },
    // output: 'export'  // Temporarily disabled for development
    async redirects() {
        return [
            // Redirect old tutorial URLs to new versioned paths
            {
                source: '/tutorials/:path*',
                destination: '/docs/v2/tutorials/:path*',
                permanent: true,
            },
            // Redirect /docs to overview page
            {
                source: '/docs',
                destination: '/docs/v2/yaci-store/overview',
                permanent: false,
            },
        ];
    },
})
