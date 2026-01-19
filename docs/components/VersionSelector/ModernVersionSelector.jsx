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

export function ModernVersionSelector() {
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
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors duration-200"
      >
        <span>{currentVersion?.label}</span>
        <svg
          className={`w-4 h-4 transition-transform duration-200 ${
            isOpen ? "rotate-180" : ""
          }`}
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
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />
          
          {/* Dropdown */}
          <div className="absolute top-full left-0 mt-2 w-48 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-20 py-1">
            {versions.map((version) => (
              <button
                key={version.id}
                onClick={() => handleVersionChange(version)}
                className={`w-full flex items-center justify-between px-4 py-2 text-sm text-left hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors duration-150 ${
                  currentVersion?.id === version.id
                    ? "bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300"
                    : "text-gray-700 dark:text-gray-300"
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className="font-medium">{version.label}</span>
                  <span
                    className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                      version.id === "v2.0.0"
                        ? "bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300"
                        : "bg-amber-100 dark:bg-amber-800 text-amber-800 dark:text-amber-100"
                    }`}
                  >
                    {version.badge}
                  </span>
                </div>
                {currentVersion?.id === version.id && (
                  <div className="w-2 h-2 bg-blue-500 rounded-full" />
                )}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}