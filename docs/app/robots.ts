import type { MetadataRoute } from "next";
import { DOCUMENTATION_URL } from "../utils/constants";

export const dynamic = 'force-static';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
    },
    sitemap: DOCUMENTATION_URL + "/sitemap.xml",
  };
}
