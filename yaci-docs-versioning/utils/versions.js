// utils/versions.js
export const VERSIONS = [
  {
    label: "v2.0.0",
    value: "v2.0.0",
    path: "/docs/v2.0.0",
    isLatest: true,
  },
  {
    label: "v1.0.0",
    value: "v1.0.0",
    path: "/docs/v1.0.0",
    isLatest: false,
  },
];

export const DEFAULT_VERSION = "v2.0.0";

export const getVersionFromPath = (pathname) => {
  const versionMatch = pathname.match(/^\/docs\/v(\d+\.\d+\.\d+)/);
  return versionMatch ? `v${versionMatch[1]}` : DEFAULT_VERSION;
};

export const getVersionConfig = (version) => {
  return VERSIONS.find((v) => v.value === version) || VERSIONS[0];
};

export const getVersionedPath = (currentPath, newVersion) => {
  // Clean the path by removing all docs and version segments first
  let cleanPath = currentPath.replace(/^(\/docs\/v\d+\.\d+\.\d+)+/, "");

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
