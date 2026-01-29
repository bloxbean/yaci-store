import { getPageMap } from "nextra/page-map";
import VersionFilteredLayout from "../../components/VersionFilteredLayout";

export default async function DocsLayout({ children }: { children: React.ReactNode }) {
  const pageMap = await getPageMap();

  return (
    <VersionFilteredLayout pageMap={pageMap}>
      {children}
    </VersionFilteredLayout>
  );
}
