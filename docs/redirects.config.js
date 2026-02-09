/**
 * Redirect configuration for static export
 * Used by both the post-build script and the 404 page
 */

// Simple redirects - these will be generated as static HTML files
export const simpleRedirects = [
  // ===== PLUGINS =====
  { source: '/plugins/plugin-getting-started', destination: '/docs/v2/plugins/write-first-plugin' },
  { source: '/plugins/plugin_api-guide', destination: '/docs/v2/plugins/plugin-api-guide' },
  { source: '/plugins/context-variables', destination: '/docs/v2/plugins/context-variables-overview' },
  { source: '/plugins/plugin-configuration-reference', destination: '/docs/v2/plugins/plugin-configuration' },

  // ===== ROOT LEVEL PAGES =====
  { source: '/design', destination: '/docs/v2/introduction/design' },
  { source: '/docker', destination: '/docs/v2/getting-started/installation/docker' },
  { source: '/build_run', destination: '/docs/v2/getting-started/installation/zip' },
  { source: '/showcase', destination: '/docs/v2/showcase/projects-using-yaci-store' },
  { source: '/other-configurations', destination: '/docs/v2/advanced-configuration/pruning' },

  // ===== GETTING STARTED =====
  { source: '/getting-started/getting-started-2.x.x', destination: '/docs/v2/getting-started/overview' },
  { source: '/getting-started/getting-started-2.0.0-beta1', destination: '/docs/v2/getting-started/previous-versions/beta1-overview' },

  // ===== USAGE =====
  { source: '/usage/getting-started-as-library', destination: '/docs/v2/usage/as-library' },
  { source: '/usage/getting-started-out-of-box', destination: '/docs/v2/usage/out-of-box-indexers' },
  { source: '/usage/aggregation-app-getting-started', destination: '/docs/v2/usage/as-library' },

  // ===== STORES =====
  { source: '/stores/overview', destination: '/docs/v2/stores/overview' },
  { source: '/stores/configuration', destination: '/docs/v2/stores/configuration' },

  // ===== LEDGER STATE MISMATCHES =====
  { source: '/ledger-state-mismatches', destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta1/overview' },
  { source: '/ledger-state-mismatches/2-0-0-beta1/2-0-0-beta1', destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta1/overview' },
  { source: '/ledger-state-mismatches/2-0-0-beta3/2-0-0-beta3', destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta3/overview' },
  { source: '/ledger-state-mismatches/2-0-0-beta5/2-0-0-beta5', destination: '/docs/v2/ledger-state-mismatches/2-0-0-beta5/overview' },

  // ===== DOCS INDEX =====
  { source: '/docs', destination: '/docs/v2/introduction/overview' },
];

// Wildcard redirects - handled by the 404 page with JavaScript
// Order matters: more specific patterns should come first
export const wildcardRedirects = [
  // ===== TUTORIALS =====
  { pattern: /^\/tutorials\/(.*)$/, replacement: '/docs/v2/tutorials/$1' },

  // ===== PLUGINS VARIABLES =====
  { pattern: /^\/plugins\/variables\/(.*)$/, replacement: '/docs/v2/plugins/variables-reference-guide/$1' },

  // ===== LEDGER STATE MISMATCHES =====
  { pattern: /^\/ledger-state-mismatches\/2-0-0-beta3\/mainnet\/(.*)$/, replacement: '/docs/v2/ledger-state-mismatches/2-0-0-beta3/mainnet/$1' },
  { pattern: /^\/ledger-state-mismatches\/2-0-0-beta5\/mainnet\/(.*)$/, replacement: '/docs/v2/ledger-state-mismatches/2-0-0-beta5/mainnet/$1' },

  // ===== LATEST VERSION ALIASES =====
  { pattern: /^\/docs\/getting-started\/(.*)$/, replacement: '/docs/v2/getting-started/$1' },
  { pattern: /^\/docs\/introduction\/(.*)$/, replacement: '/docs/v2/introduction/$1' },
  { pattern: /^\/docs\/usage\/(.*)$/, replacement: '/docs/v2/usage/$1' },
  { pattern: /^\/docs\/plugins\/(.*)$/, replacement: '/docs/v2/plugins/$1' },
  { pattern: /^\/docs\/tutorials\/(.*)$/, replacement: '/docs/v2/tutorials/$1' },
];
