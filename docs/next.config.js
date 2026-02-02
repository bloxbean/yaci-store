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
            // ===== TUTORIALS =====
            {
                source: '/tutorials/:path*',
                destination: '/docs/v2/tutorials/:path*',
                permanent: true,
            },

            // ===== PLUGINS =====
            {
                source: '/plugins/plugin-getting-started',
                destination: '/docs/v2/plugins/write-first-plugin',
                permanent: true,
            },
            {
                source: '/plugins/plugin_api-guide',
                destination: '/docs/v2/plugins/plugin-api-guide',
                permanent: true,
            },
            {
                source: '/plugins/context-variables',
                destination: '/docs/v2/plugins/context-variables-overview',
                permanent: true,
            },
            {
                source: '/plugins/variables/:path*',
                destination: '/docs/v2/plugins/variables-reference-guide/:path*',
                permanent: true,
            },
            {
                source: '/plugins/plugin-configuration-reference',
                destination: '/docs/v2/plugins/plugin-configuration',
                permanent: true,
            },

            // ===== ROOT LEVEL PAGES =====
            {
                source: '/design',
                destination: '/docs/v2/introduction/design',
                permanent: true,
            },
            {
                source: '/docker',
                destination: '/docs/v2/getting-started/installation/docker',
                permanent: true,
            },
            {
                source: '/build_run',
                destination: '/docs/v2/getting-started/installation/zip',
                permanent: true,
            },
            {
                source: '/showcase',
                destination: '/docs/v2/showcase/projects-using-yaci-store',
                permanent: true,
            },
            {
                source: '/other-configurations',
                destination: '/docs/v2/advanced-configuration/pruning',
                permanent: true,
            },

            // ===== GETTING STARTED =====
            {
                source: '/getting-started/getting-started-2.x.x',
                destination: '/docs/v2/getting-started/overview',
                permanent: true,
            },
            {
                source: '/getting-started/getting-started-2.0.0-beta1',
                destination: '/docs/v2/getting-started/previous-versions/beta1-overview',
                permanent: true,
            },

            // ===== USAGE =====
            {
                source: '/usage/getting-started-as-library',
                destination: '/docs/v2/usage/as-library',
                permanent: true,
            },
            {
                source: '/usage/getting-started-out-of-box',
                destination: '/docs/v2/usage/out-of-box-indexers',
                permanent: true,
            },
            {
                source: '/usage/aggregation-app-getting-started',
                destination: '/docs/v2/usage/as-library',
                permanent: true,
            },

            // ===== STORES =====
            {
                source: '/stores/overview',
                destination: '/docs/v2/stores/overview',
                permanent: true,
            },
            {
                source: '/stores/configuration',
                destination: '/docs/v2/stores/configuration',
                permanent: true,
            },

            // ===== LEDGER STATE MISMATCHES =====
            {
                source: '/ledger-state-mismatches',
                destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta1/overview',
                permanent: true,
            },
            {
                source: '/ledger-state-mismatches/2-0-0-beta1/2-0-0-beta1',
                destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta1/overview',
                permanent: true,
            },
            {
                source: '/ledger-state-mismatches/2-0-0-beta3/2-0-0-beta3',
                destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta3/overview',
                permanent: true,
            },
            {
                source: '/ledger-state-mismatches/2-0-0-beta3/mainnet/:path*',
                destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta3/mainnet/:path*',
                permanent: true,
            },
            {
                source: '/ledger-state-mismatches/2-0-0-beta5/2-0-0-beta5',
                destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta5/overview',
                permanent: true,
            },
            {
                source: '/ledger-state-mismatches/2-0-0-beta5/mainnet/:path*',
                destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta5/mainnet/:path*',
                permanent: true,
            },

            // ===== DOCS INDEX =====
            {
                source: '/docs',
                destination: '/docs/v2/introduction/overview',
                permanent: false,
            },
        ];
    },
})
