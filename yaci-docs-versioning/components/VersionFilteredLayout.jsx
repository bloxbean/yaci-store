"use client";

import { usePathname } from "next/navigation";
import { Footer, Layout } from "nextra-theme-docs";
import { NavbarWithThemeLogo } from "./NavbarWithThemeLogo/NavbarWithThemeLogo";
import { DOCUMENTATION_GITHUB_REPO_URL, WEBPAGE_URL } from "../utils/constants";

const currentYear = new Date().getFullYear();

const footer = <Footer>
<a href={WEBPAGE_URL} target="_blank" rel="noopener noreferrer">
  {`© ${currentYear} Bloxbean project`}
</a>
</Footer>;

export default function VersionFilteredLayout({ children, allPageMap }) {
  const pathname = usePathname();
  
  // Determine current version from URL
  const currentVersion = pathname.includes('/docs/v1.0.0') ? 'v1.0.0' : 'v2.0.0';
  
  // Filter the page map to show only the current version's content
  const filteredPageMap = allPageMap.filter(item => {
    // Check if the item's route starts with the current version path
    return item.route && item.route.startsWith(`/docs/${currentVersion}/`);
  });
  
  return (
    <Layout
      navbar={<NavbarWithThemeLogo />}
      pageMap={filteredPageMap}
      docsRepositoryBase={DOCUMENTATION_GITHUB_REPO_URL + "/tree/main"}
      footer={footer}
    >
      {children}
    </Layout>
  );
}