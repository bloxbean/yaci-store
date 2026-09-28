import { readdirSync } from "node:fs";
import type { MetadataRoute } from "next";
import { DOCUMENTATION_URL } from "../utils/constants";

export const dynamic = 'force-static';

// Docs version the site serves. v1 is kept online but left out of the sitemap.
const DOCS_ROOT = "app/docs/v2";

export default function sitemap(): MetadataRoute.Sitemap {
  const pages = readdirSync(DOCS_ROOT, { recursive: true, encoding: "utf8" })
    .filter((file) => file.endsWith("page.mdx"))
    .map((file) => `/docs/v2/${file.replace(/\/?page\.mdx$/, "")}`);

  return ["/", ...pages].map((route) => ({
    url: new URL(route, DOCUMENTATION_URL).toString(),
    lastModified: new Date(),
    changeFrequency: "monthly",
    priority: route === "/" ? 1 : 0.5,
  }));
}
