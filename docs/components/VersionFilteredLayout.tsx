"use client";

import { usePathname } from "next/navigation";
import { Footer, Layout } from "nextra-theme-docs";
import { NavbarWithVersion } from "./NavbarWithVersion";

const currentYear = new Date().getFullYear();

export default function VersionFilteredLayout({ children, pageMap }: { children: React.ReactNode, pageMap: any[] }) {
  const pathname = usePathname();

  // Determine current version from URL
  const currentVersion = pathname?.includes('/docs/v1') ? 'v1' : 'v2';

  // Find the docs entry in the pageMap
  const docsEntry = pageMap.find((item: any) => item.name === 'docs');

  // Find the version entry within docs
  const versionEntry = docsEntry?.children?.find((item: any) => item.name === currentVersion);

  // Filter out pages with navigation: false and use the version's children as the filtered pageMap
  const filterPageMap = (items: any[]): any[] => {
    return items
      .filter((item: any) => {
        // Exclude items with frontMatter.navigation === false
        if (item.frontMatter?.navigation === false) {
          return false;
        }
        // Exclude items named 'page' (both .mdx and .tsx files)
        if (item.name === 'page') {
          return false;
        }
        return true;
      })
      .map((item: any) => {
        // Recursively filter children
        if (item.children) {
          return { ...item, children: filterPageMap(item.children) };
        }
        return item;
      });
  };

  const filteredPageMap = filterPageMap(versionEntry?.children || []);

  const footer = (
    <Footer>
      © {currentYear} BloxBean project
    </Footer>
  );

  return (
    <Layout
      navbar={<NavbarWithVersion />}
      pageMap={filteredPageMap}
      docsRepositoryBase="https://github.com/bloxbean/yaci-store/tree/main/docs"
      footer={footer}
      sidebar={{
        toggleButton: true,
        defaultMenuCollapseLevel: 1
      }}
    >
      {children}
    </Layout>
  );
}
