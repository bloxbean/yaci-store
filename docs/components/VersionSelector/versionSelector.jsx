"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { usePathname } from "next/navigation";
import { getSafeVersionedPath, VERSIONS } from "../../utils/versions";

const versions = VERSIONS.map(v => ({
  id: v.value,
  label: v.label,
  badge: v.isLatest ? "Latest" : "Legacy",
  value: v.value
}));

export function VersionSelector() {
  const [isOpen, setIsOpen] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  const getCurrentVersion = () => {
    if (pathname.includes("/docs/v0.1.x")) {
      return versions.find((v) => v.id === "v0.1.x");
    }
    if (pathname.includes("/docs/v2.0.0")) {
      return versions.find((v) => v.id === "v2.0.0");
    }
    return versions.find(v => v.badge === "Latest") || versions[0]; // Default to latest
  };

  const handleVersionChange = (version) => {
    setIsOpen(false);
    const newPath = getSafeVersionedPath(pathname, version.value);
    router.push(newPath);
  };

  const currentVersion = getCurrentVersion();

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
        
        .dark .version-button {
          color: #d1d5db;
        }
        
        .dark .version-button:hover {
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
          z-index: 20;
          padding: 4px 0;
        }
        
        .dark .dropdown-menu {
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
        }
        
        .dropdown-item:hover {
          background: #f9fafb;
        }
        
        .dropdown-item.current {
          background: #eff6ff;
          color: #2563eb;
        }
        
        .dark .dropdown-item {
          color: #d1d5db;
        }
        
        .dark .dropdown-item:hover {
          background: #374151;
        }
        
        .dark .dropdown-item.current {
          background: #1e3a8a;
          color: #60a5fa;
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
        
        .dark .badge-latest {
          background: #14532d;
          color: #86efac;
        }
        
        .dark .badge-legacy {
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
          className="version-button"
          onClick={() => setIsOpen(!isOpen)}
        >
          <span>{currentVersion?.label}</span>
          <svg
            className={`version-icon ${isOpen ? "open" : ""}`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>

        {isOpen && (
          <>
            <div className="dropdown-backdrop" onClick={() => setIsOpen(false)} />
            
            <div className="dropdown-menu">
              {versions.map((version) => (
                <button
                  key={version.id}
                  className={`dropdown-item ${
                    currentVersion?.id === version.id ? "current" : ""
                  }`}
                  onClick={() => handleVersionChange(version)}
                >
                  <div className="version-info">
                    <span className="version-label">{version.label}</span>
                    <span
                      className={`version-badge ${
                        version.id === "v2.0.0" ? "badge-latest" : "badge-legacy"
                      }`}
                    >
                      {version.badge}
                    </span>
                  </div>
                  {currentVersion?.id === version.id && (
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