"use client";

import Link from "next/link";
import { VersionSelector } from "../VersionSelector/versionSelector";
import { DocsNavigation } from "../DocsNavigation/DocsNavigation";

export function NextraLayout({ children, currentPath = "" }) {
  return (
    <>
      <style jsx global>{`
        /* Nextra 4 inspired layout */
        .nextra-layout {
          min-height: 100vh;
          background: #ffffff;
          font-family:
            -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
            "Helvetica Neue", sans-serif;
        }

        .dark .nextra-layout {
          background: #111827;
          color: #ffffff;
        }

        /* Top navigation bar */
        .nextra-navbar {
          position: sticky;
          top: 0;
          z-index: 50;
          backdrop-filter: blur(12px);
          background: rgba(255, 255, 255, 0.8);
          border-bottom: 1px solid #e5e7eb;
          padding: 0;
        }

        .dark .nextra-navbar {
          background: rgba(17, 24, 39, 0.8);
          border-bottom-color: #374151;
        }

        .navbar-content {
          max-width: 90rem;
          margin: 0 auto;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0.75rem 1.5rem;
        }

        .logo-section {
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }

        .logo-text {
          font-size: 1.25rem;
          font-weight: 600;
          color: #111827;
          text-decoration: none;
        }

        .dark .logo-text {
          color: #ffffff;
        }

        /* Main content layout */
        .nextra-content {
          max-width: 90rem;
          margin: 0 auto;
          display: grid;
          grid-template-columns: 16rem 1fr;
          gap: 0;
        }

        /* Sidebar */
        .nextra-sidebar {
          position: sticky;
          top: 4rem;
          height: calc(100vh - 4rem);
          overflow-y: auto;
          background: #ffffff;
          border-right: 1px solid #e5e7eb;
          padding: 1.5rem 1rem 1.5rem 1.5rem;
          scrollbar-width: thin;
          scrollbar-color: #d1d5db transparent;
        }

        .nextra-sidebar::-webkit-scrollbar {
          width: 6px;
        }

        .nextra-sidebar::-webkit-scrollbar-track {
          background: transparent;
        }

        .nextra-sidebar::-webkit-scrollbar-thumb {
          background: #d1d5db;
          border-radius: 3px;
        }

        .dark .nextra-sidebar {
          background: #111827;
          border-right-color: #374151;
        }

        .dark .nextra-sidebar::-webkit-scrollbar-thumb {
          background: #4b5563;
        }

        /* Main article */
        .nextra-main {
          min-width: 0;
          padding: 2rem 3rem;
          background: #ffffff;
        }

        .dark .nextra-main {
          background: #111827;
        }

        /* Content styling */
        .nextra-article {
          max-width: none;
          line-height: 1.7;
        }

        .article-title {
          font-size: 2.25rem;
          font-weight: 700;
          color: #111827;
          margin-bottom: 0.5rem;
          line-height: 1.2;
        }

        .dark .article-title {
          color: #ffffff;
        }

        .article-subtitle {
          font-size: 1.125rem;
          color: #6b7280;
          margin-bottom: 2rem;
          line-height: 1.6;
        }

        .dark .article-subtitle {
          color: #9ca3af;
        }

        .version-badge {
          display: inline-flex;
          align-items: center;
          background: #2563eb;
          color: #ffffff;
          padding: 0.25rem 0.75rem;
          border-radius: 9999px;
          font-size: 0.75rem;
          font-weight: 500;
          margin-bottom: 1.5rem;
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }

        /* Responsive design */
        @media (max-width: 1024px) {
          .nextra-content {
            grid-template-columns: 14rem 1fr;
          }

          .nextra-main {
            padding: 1.5rem 2rem;
          }
        }

        @media (max-width: 768px) {
          .nextra-content {
            grid-template-columns: 1fr;
          }

          .nextra-sidebar {
            display: none;
          }

          .nextra-main {
            padding: 1rem 1.5rem;
          }

          .navbar-content {
            padding: 0.75rem 1rem;
          }
        }
      `}</style>

      <div className="nextra-layout">
        <nav className="nextra-navbar">
          <div className="navbar-content">
            <div className="logo-section">
              <Link href="/" className="logo-text">
                Yaci Store
              </Link>
            </div>
            <VersionSelector />
          </div>
        </nav>

        <div className="nextra-content">
          <aside className="nextra-sidebar">
            <DocsNavigation currentPath={currentPath} />
          </aside>

          <main className="nextra-main">
            <article className="nextra-article">{children}</article>
          </main>
        </div>
      </div>
    </>
  );
}
