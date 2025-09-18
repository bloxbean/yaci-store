// utils/versions.js
export const VERSIONS = [
  {
    label: "v2.0.0 Beta1 - Latest",
    value: "v2.0.0",
    path: "/docs/v2.0.0",
    isLatest: true,
  },
  {
    label: "v0.1.0 - v0.1.4",
    value: "v0.1.x",
    path: "/docs/v0.1.x",
    isLatest: false,
  },
];

export const DEFAULT_VERSION = "v2.0.0";

export const getVersionFromPath = (pathname) => {
  const versionMatch = pathname.match(/^\/docs\/v(\d+\.\d+\.\d+|0\.1\.x)/);
  return versionMatch ? `v${versionMatch[1]}` : DEFAULT_VERSION;
};

export const getVersionConfig = (version) => {
  return VERSIONS.find((v) => v.value === version) || VERSIONS[0];
};

export const getVersionedPath = (currentPath, newVersion) => {
  // Clean the path by removing all docs and version segments first
  let cleanPath = currentPath.replace(/^(\/docs\/v(\d+\.\d+\.\d+|0\.1\.x))+/, "");

  // If the clean path is empty or just "/", use the version root
  if (!cleanPath || cleanPath === "/") {
    return `/docs/${newVersion}`;
  }

  // Ensure clean path starts with "/"
  if (!cleanPath.startsWith("/")) {
    cleanPath = "/" + cleanPath;
  }

  return `/docs/${newVersion}${cleanPath}`;
};

// Define pages that exist in v2.0.0 but not in v0.1.x
const V2_ONLY_PAGES = [
  '/plugin-framework/context-variables',
  '/plugin-framework/getting-started', 
  '/plugin-framework/plugin-api-guide',
  '/plugin-framework/plugin-configuration',
  '/knownIssues/ledgerStateMismatch/2-beta-1',
  '/knownIssues/ledgerStateMismatch/2-beta-3',
  '/knownIssues/ledgerStateMismatch/overview',
  '/gettingStarted/quickStart',
  '/advancedConfigurations/monitoring'
];

// Fallback pages for different sections when switching to v0.1.x
const V01X_FALLBACK_MAP = {
  '/plugin-framework': '/yacistore/overview',
  '/knownIssues/ledgerStateMismatch': '/knownIssues/ledgerStateMismatch',
  '/gettingStarted/quickStart': '/gettingStarted/requirements',
  '/advancedConfigurations/monitoring': '/yacistore/overview',
  // Default fallback
  'default': '/yacistore/overview'
};

export const getSafeVersionedPath = (currentPath, newVersion) => {
  const cleanPath = currentPath.replace(/^(\/docs\/v(\d+\.\d+\.\d+|0\.1\.x))+/, "");
  
  // If switching to v0.1.x, check if the current page exists in that version
  if (newVersion === 'v0.1.x') {
    // Check if current path is a v2.0.0 only page
    const isV2OnlyPage = V2_ONLY_PAGES.some(v2Page => cleanPath.startsWith(v2Page));
    
    if (isV2OnlyPage) {
      // Find appropriate fallback
      for (const [section, fallback] of Object.entries(V01X_FALLBACK_MAP)) {
        if (section !== 'default' && cleanPath.startsWith(section)) {
          return `/docs/${newVersion}${fallback}`;
        }
      }
      // Use default fallback
      return `/docs/${newVersion}${V01X_FALLBACK_MAP.default}`;
    }
  }
  
  // Use regular versioned path logic for all other cases
  return getVersionedPath(currentPath, newVersion);
};
