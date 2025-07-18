import { getPageMap } from "nextra/page-map";
import VersionFilteredLayout from "../../components/VersionFilteredLayout";

function transformPageMap(pageMap) {
  const result = [];
  
  // Process both versions and flatten them
  ['v1.0.0', 'v2.0.0'].forEach(version => {
    const versionEntry = pageMap.find(item => item.name === version);
    
    if (versionEntry && versionEntry.children) {
      // Transform routes recursively
      function transformRoutes(item, basePath) {
        const newRoute = `${basePath}/${item.name}`;
        
        return {
          ...item,
          route: newRoute,
          children: item.children ? item.children.map(child => transformRoutes(child, newRoute)) : undefined
        };
      }
      
      // Get children, filter out index, and transform their routes
      const transformedChildren = versionEntry.children
        .filter(child => child.name !== 'index')
        .map(child => transformRoutes(child, `/docs/${version}`));
      
      result.push(...transformedChildren);
    }
  });
  
  return result;
}

export default async function DocsLayout({ children }) {
  const originalPageMap = await getPageMap();
  const transformedPageMap = transformPageMap(originalPageMap);
  
  return (
    <VersionFilteredLayout allPageMap={transformedPageMap}>
      {children}
    </VersionFilteredLayout>
  );
}