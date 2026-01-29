"use client";

import { useState, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";

const versions = [
  {
    value: "v2",
    label: "v2.x.x",
    isLatest: true,
  },
  {
    value: "v1",
    label: "v0.1.x",
    isLatest: false,
  },
];

export function ModernVersionSelector() {
  const [isOpen, setIsOpen] = useState(false);
  const [mounted, setMounted] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (isOpen && !(event.target as Element).closest(".version-selector")) {
        setIsOpen(false);
      }
    };

    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [isOpen]);

  if (!mounted) {
    return null;
  }

  const getCurrentVersion = () => {
    if (pathname?.includes("/docs/v1")) {
      return "v1";
    }
    if (pathname?.includes("/docs/v2")) {
      return "v2";
    }
    return null;
  };

  const currentVersion = getCurrentVersion();
  const currentVersionConfig = versions.find((v) => v.value === currentVersion) || versions[0];

  // Don't show on landing page
  if (!currentVersion) return null;

  const handleVersionChange = (version: typeof versions[0]) => {
    const currentPath = pathname || '/';
    let pathWithoutVersion = currentPath;

    // Extract the path after /docs/v1/ or /docs/v2/
    if (currentPath.includes('/docs/v1/')) {
      pathWithoutVersion = currentPath.replace('/docs/v1', '');
    } else if (currentPath.includes('/docs/v2/')) {
      pathWithoutVersion = currentPath.replace('/docs/v2', '');
    } else if (currentPath === '/docs/v1' || currentPath === '/docs/v2') {
      pathWithoutVersion = '';
    }

    // Define sections that don't exist in V1
    const v2OnlySections = ['/plugins', '/ledger-state-mismatches'];

    // If switching to V1 and current path is in a V2-only section, redirect to overview
    if (version.value === 'v1' && v2OnlySections.some(section => pathWithoutVersion.startsWith(section))) {
      router.push('/docs/v1/yaci-store/overview');
      setIsOpen(false);
      return;
    }

    // Try to preserve the path in the new version
    const newPath = pathWithoutVersion ? `/docs/${version.value}${pathWithoutVersion}` : `/docs/${version.value}/yaci-store/overview`;
    router.push(newPath);
    setIsOpen(false);
  };

  return (
    <>
      <style jsx>{`
        .version-selector {
          position: relative;
          display: inline-block;
        }

        .version-button {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 12px;
          background: transparent;
          border: none;
          border-radius: 8px;
          color: #6b7280;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .version-button:hover {
          background: #f3f4f6;
          color: #111827;
        }

        :global(.dark) .version-button {
          color: #d1d5db;
        }

        :global(.dark) .version-button:hover {
          background: #374151;
          color: #f9fafb;
        }

        .version-icon {
          width: 16px;
          height: 16px;
          transition: transform 0.2s ease;
        }

        .version-icon.open {
          transform: rotate(180deg);
        }

        .dropdown-backdrop {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          z-index: 10;
        }

        .dropdown-menu {
          position: absolute;
          top: 100%;
          left: 0;
          margin-top: 8px;
          width: 192px;
          background: white;
          border: 1px solid #e5e7eb;
          border-radius: 8px;
          box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
          z-index: 1000;
          padding: 4px 0;
        }

        :global(.dark) .dropdown-menu {
          background: #1f2937;
          border-color: #374151;
        }

        .dropdown-item {
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 16px;
          background: transparent;
          border: none;
          text-align: left;
          font-size: 14px;
          cursor: pointer;
          transition: background-color 0.15s ease;
          color: #374151;
        }

        .dropdown-item:hover {
          background: #f9fafb;
          color: #111827;
        }

        .dropdown-item.current {
          background: #eff6ff;
          color: #2563eb;
        }

        :global(.dark) .dropdown-item {
          color: #f3f4f6;
        }

        :global(.dark) .dropdown-item:hover {
          background: #374151;
          color: #ffffff;
        }

        :global(.dark) .dropdown-item.current {
          background: #1e3a8a;
          color: #93c5fd;
        }

        .version-info {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .version-label {
          font-weight: 500;
        }

        .version-badge {
          padding: 2px 8px;
          font-size: 12px;
          font-weight: 500;
          border-radius: 9999px;
        }

        .badge-latest {
          background: #dcfce7;
          color: #166534;
        }

        .badge-legacy {
          background: #fbbf24;
          color: #92400e;
        }

        :global(.dark) .badge-latest {
          background: #16a34a;
          color: #ffffff;
        }

        :global(.dark) .badge-legacy {
          background: #d97706;
          color: #fef3c7;
        }

        .current-indicator {
          width: 8px;
          height: 8px;
          background: #3b82f6;
          border-radius: 50%;
        }
      `}</style>

      <div className="version-selector">
        <button
          type="button"
          className="version-button"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setIsOpen(!isOpen);
          }}
        >
          <span>{currentVersionConfig.label}</span>
        </button>

        {isOpen && (
          <>
            <div className="dropdown-backdrop" onClick={() => setIsOpen(false)} />

            <div className="dropdown-menu">
              {versions.map((version) => (
                <button
                  type="button"
                  key={version.value}
                  className={`dropdown-item ${
                    version.value === currentVersion ? "current" : ""
                  }`}
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    handleVersionChange(version);
                  }}
                >
                  <div className="version-info">
                    <span className="version-label">{version.label}</span>
                    <span className={`version-badge ${version.isLatest ? 'badge-latest' : 'badge-legacy'}`}>
                      {version.isLatest ? 'Latest' : 'Legacy'}
                    </span>
                  </div>
                  {version.value === currentVersion && (
                    <div className="current-indicator" />
                  )}
                </button>
              ))}
            </div>
          </>
        )}
      </div>
    </>
  );
}
