"use client";

import Link from "next/link";
import { useState } from "react";

const ExpandableNavSection = ({ title, children, isExpanded = false }) => {
  const [expanded, setExpanded] = useState(isExpanded);

  return (
    <>
      <style jsx>{`
        .nav-section-toggle {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          padding: 0.5rem 0.75rem;
          margin: 0.125rem 0;
          border-radius: 0.375rem;
          cursor: pointer;
          transition: all 0.15s ease;
          font-size: 0.875rem;
          font-weight: 500;
          color: #374151;
          background: transparent;
        }

        .nav-section-toggle:hover {
          background: #f3f4f6;
          color: #111827;
        }

        .dark .nav-section-toggle {
          color: #d1d5db;
        }

        .dark .nav-section-toggle:hover {
          background: #374151;
          color: #f9fafb;
        }

        .nav-section-arrow {
          width: 1rem;
          height: 1rem;
          transition: transform 0.2s ease;
          transform: ${expanded ? "rotate(90deg)" : "rotate(0deg)"};
          color: #9ca3af;
        }

        .nav-section-children {
          padding-left: 1.5rem;
          margin: 0.25rem 0;
        }
      `}</style>

      <div
        onClick={() => setExpanded(!expanded)}
        className="nav-section-toggle"
      >
        <svg
          className="nav-section-arrow"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="m8.25 4.5 7.5 7.5-7.5 7.5"
          />
        </svg>
        {title}
      </div>
      {expanded && <div className="nav-section-children">{children}</div>}
    </>
  );
};

const NavLink = ({ href, children, isActive = false }) => {
  return (
    <>
      <style jsx>{`
        .nav-link {
          display: block;
          padding: 0.5rem 0.75rem;
          margin: 0.125rem 0;
          border-radius: 0.375rem;
          color: ${isActive ? "#2563eb" : "#6b7280"};
          font-size: 0.875rem;
          font-weight: ${isActive ? "500" : "400"};
          text-decoration: none;
          transition: all 0.15s ease;
          background: ${isActive ? "#eff6ff" : "transparent"};
        }

        .nav-link:hover {
          background: ${isActive ? "#eff6ff" : "#f3f4f6"};
          color: ${isActive ? "#2563eb" : "#111827"};
        }

        .dark .nav-link {
          color: ${isActive ? "#60a5fa" : "#d1d5db"};
          background: ${isActive ? "#1e3a8a" : "transparent"};
        }

        .dark .nav-link:hover {
          background: ${isActive ? "#1e3a8a" : "#374151"};
          color: ${isActive ? "#60a5fa" : "#f9fafb"};
        }
      `}</style>
      <Link href={href} className="nav-link">
        {children}
      </Link>
    </>
  );
};

export function DocsNavigation({ currentPath = "" }) {
  return (
    <>
      <style jsx>{`
        .nextra-nav {
          font-size: 0.875rem;
          line-height: 1.25rem;
        }

        .nav-group {
          margin-bottom: 1rem;
        }

        .nav-item {
          margin: 0.125rem 0;
        }
      `}</style>

      <nav className="nextra-nav">
        <div className="nav-group">
          <ExpandableNavSection
            title="Yaci Store"
            isExpanded={currentPath.includes("yaci-store")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0"
                isActive={currentPath === "/docs/v2.0.0"}
              >
                Overview
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/yaci-store/design"
                isActive={currentPath.includes("design")}
              >
                Design
              </NavLink>
            </div>
            <ExpandableNavSection
              title="Modules"
              isExpanded={currentPath.includes("modules")}
            >
              <div className="nav-item">
                <NavLink
                  href="/docs/v2.0.0/yaci-store/modules/core-modules"
                  isActive={currentPath.includes("core-modules")}
                >
                  Core Modules
                </NavLink>
              </div>
              <div className="nav-item">
                <NavLink
                  href="/docs/v2.0.0/yaci-store/modules/stores"
                  isActive={currentPath.includes("stores")}
                >
                  Stores
                </NavLink>
              </div>
              <div className="nav-item">
                <NavLink
                  href="/docs/v2.0.0/yaci-store/modules/aggregates"
                  isActive={currentPath.includes("aggregates")}
                >
                  Aggregates
                </NavLink>
              </div>
            </ExpandableNavSection>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/yaci-store/spring-boot-starters"
                isActive={currentPath.includes("spring-boot-starters")}
              >
                Spring Boot Starters
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="Getting Started"
            isExpanded={
              currentPath.includes("getting-started") ||
              currentPath.includes("installation")
            }
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/getting-started/requirements"
                isActive={currentPath.includes("requirements")}
              >
                Requirements
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/getting-started/compatibility-matrix"
                isActive={currentPath.includes("compatibility-matrix")}
              >
                Compatibility Matrix
              </NavLink>
            </div>
            <ExpandableNavSection
              title="Installation"
              isExpanded={currentPath.includes("installation")}
            >
              <div className="nav-item">
                <NavLink
                  href="/docs/v2.0.0/getting-started/installation"
                  isActive={
                    currentPath === "/docs/v2.0.0/getting-started/installation"
                  }
                >
                  Overview
                </NavLink>
              </div>
              <div className="nav-item">
                <NavLink
                  href="/docs/v2.0.0/getting-started/installation/docker"
                  isActive={currentPath.includes("docker")}
                >
                  Docker
                </NavLink>
              </div>
              <div className="nav-item">
                <NavLink
                  href="/docs/v2.0.0/getting-started/installation/build-and-run"
                  isActive={currentPath.includes("build-and-run")}
                >
                  Build & Run
                </NavLink>
              </div>
            </ExpandableNavSection>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="Usage"
            isExpanded={currentPath.includes("usage")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/usage"
                isActive={currentPath === "/docs/v2.0.0/usage"}
              >
                Overview
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/usage/as-a-library"
                isActive={currentPath.includes("as-a-library")}
              >
                As a Library
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/usage/out-of-box-indexer"
                isActive={currentPath.includes("out-of-box-indexer")}
              >
                Out of the box Chain Indexer
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/usage/ledger-state"
                isActive={currentPath.includes("ledger-state")}
              >
                Ledger State
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="REST API"
            isExpanded={currentPath.includes("api")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/api"
                isActive={currentPath === "/docs/v2.0.0/api"}
              >
                API List
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="Advanced Configurations"
            isExpanded={currentPath.includes("advanced-configurations")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/advanced-configurations"
                isActive={
                  currentPath === "/docs/v2.0.0/advanced-configurations"
                }
              >
                Overview
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/advanced-configurations/granular-indexing"
                isActive={currentPath.includes("granular-indexing")}
              >
                Granular Indexing
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/advanced-configurations/pruning"
                isActive={currentPath.includes("pruning")}
              >
                Pruning
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/advanced-configurations/auto-sync-off"
                isActive={currentPath.includes("auto-sync-off")}
              >
                Auto Sync Off
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/advanced-configurations/store-specific-application"
                isActive={currentPath.includes("store-specific-application")}
              >
                Store-specific Application
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="Tutorials & Use Cases"
            isExpanded={currentPath.includes("tutorials")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/tutorials/tutorial1"
                isActive={currentPath.includes("tutorial1")}
              >
                Tutorial 1
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="Known Issues"
            isExpanded={currentPath.includes("known-issues")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/known-issues/ledger-state-mismatch"
                isActive={currentPath.includes("ledger-state-mismatch")}
              >
                Ledger State Mismatch
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <ExpandableNavSection
            title="Troubleshooting"
            isExpanded={currentPath.includes("troubleshooting")}
          >
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/troubleshooting"
                isActive={currentPath === "/docs/v2.0.0/troubleshooting"}
              >
                As a Library Troubleshooting
              </NavLink>
            </div>
            <div className="nav-item">
              <NavLink
                href="/docs/v2.0.0/troubleshooting/indexer-troubleshooting"
                isActive={currentPath.includes("indexer-troubleshooting")}
              >
                Out of the box Chain Indexer Troubleshooting
              </NavLink>
            </div>
          </ExpandableNavSection>
        </div>

        <div className="nav-group">
          <div className="nav-item">
            <NavLink
              href="/docs/v2.0.0/faq"
              isActive={currentPath.includes("faq")}
            >
              FAQ
            </NavLink>
          </div>
          <div className="nav-item">
            <NavLink
              href="/docs/v2.0.0/changelogs"
              isActive={currentPath.includes("changelogs")}
            >
              Changelogs
            </NavLink>
          </div>
        </div>
      </nav>
    </>
  );
}
