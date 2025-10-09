import type { MetadataRoute } from "next";
import { DOCUMENTATION_URL } from "../utils/constants";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    {
      url: DOCUMENTATION_URL,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 1,
    },
    {
      url: `${DOCUMENTATION_URL}/yacistore`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.8,
    },
    {
      url: `${DOCUMENTATION_URL}/yacistore/overview`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.8,
    },
    {
      url: `${DOCUMENTATION_URL}/yacistore/design`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },

    {
      url: `${DOCUMENTATION_URL}/yacistore/yacistoremodules/coremodules`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/yaciStore/yacistoremodules/stories`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/yaciStore/yacistoremodules/aggregates`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/yaciStore/yacistoresprintbootstarters`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/gettingstarted`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/gettingstarted/requirements`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/gettingstarted/compatibilitymatrix`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/gettingstarted/installation`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/gettingstarted/installation/buildandrun`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/gettingstarted/installation/docker`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/usage`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/usage/asalibrary`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/usage/outoftheboxindexer`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.8,
    },
    {
      url: `${DOCUMENTATION_URL}/usage/ledgerState`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/apis`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/apis/apisoverview`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.8,
    },
    {
      url: `${DOCUMENTATION_URL}/advancedconfigurations`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/advancedconfigurations/granularindexing`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/advancedconfigurations/pruning`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/advancedconfigurations/autosyncoff`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/advancedconfigurations/storespecificapplication`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/tutorialsandusecases`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/tutorialsandusecases/tutorial1`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
    {
      url: `${DOCUMENTATION_URL}/knownissues`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/knownissues/ledgerstatemismatch`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/faq`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/troubleshooting`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/troubleshooting/librarytroubleshooting`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/troubleshooting/indexertroubleshooting`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.4,
    },
    {
      url: `${DOCUMENTATION_URL}/changelogs`,
      lastModified: new Date(),
      changeFrequency: "yearly",
      priority: 0.6,
    },
  ];
}
