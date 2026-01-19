'use client'

import { usePathname } from 'next/navigation'
import { useState, useEffect, useRef } from 'react'

const versions = [
  { label: '2.x.x', value: 'v2', path: '/docs/v2' },
  { label: '1.x.x', value: 'v1', path: '/docs/v1' }
]

export function VersionSelector() {
  const pathname = usePathname()
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  // Determine current version from pathname
  const currentVersion = pathname?.includes('/docs/v1') ? 'v1' : pathname?.includes('/docs/v2') ? 'v2' : null
  const currentVersionLabel = currentVersion ? versions.find(v => v.value === currentVersion)?.label : null

  // Don't show version selector on landing page
  if (!currentVersion) return null

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleVersionChange = (versionPath: string) => {
    const currentPath = pathname || '/'
    let pathWithoutVersion = currentPath

    if (currentPath.includes('/docs/v1/')) {
      pathWithoutVersion = currentPath.replace('/docs/v1', '')
    } else if (currentPath.includes('/docs/v2/')) {
      pathWithoutVersion = currentPath.replace('/docs/v2', '')
    } else if (currentPath === '/docs/v1' || currentPath === '/docs/v2') {
      pathWithoutVersion = ''
    }

    const newPath = pathWithoutVersion ? versionPath + pathWithoutVersion : versionPath
    window.location.href = newPath
  }

  return (
    <div className="relative inline-block" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px',
          fontSize: '12px',
          padding: '3px 8px',
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
        }}
        className="font-semibold border border-gray-400 dark:border-gray-500 rounded-md bg-white dark:bg-gray-800 hover:border-gray-500 dark:hover:border-gray-400 hover:shadow-md transition-all text-gray-800 dark:text-gray-100"
        aria-label="Select version"
      >
        <span>v{currentVersionLabel}</span>
        <svg
          className={`transition-transform ${isOpen ? 'rotate-180' : ''}`}
          style={{ width: '10px', height: '10px', flexShrink: 0 }}
          fill="currentColor"
          viewBox="0 0 20 20"
        >
          <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
        </svg>
      </button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-[9998]"
            onClick={() => setIsOpen(false)}
          />
          <div
            className="absolute left-0 top-full mt-2 min-w-[70px] bg-white dark:bg-gray-800 border border-gray-400 dark:border-gray-500 rounded-md overflow-hidden z-[9999]"
            style={{ boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)' }}
            onClick={(e) => e.stopPropagation()}
          >
            {versions
              .filter((version) => version.value !== currentVersion)
              .map((version) => (
                <button
                  key={version.value}
                  onClick={(e) => {
                    e.stopPropagation()
                    handleVersionChange(version.path)
                    setIsOpen(false)
                  }}
                  style={{ fontSize: '12px', padding: '6px 10px' }}
                  className="w-full text-left transition-colors font-medium text-gray-800 dark:text-gray-100 hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-700 dark:hover:text-blue-300"
                >
                  v{version.label}
                </button>
              ))}
          </div>
        </>
      )}
    </div>
  )
}
