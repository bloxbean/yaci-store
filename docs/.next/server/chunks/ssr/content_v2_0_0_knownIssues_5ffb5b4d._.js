module.exports = {

"[project]/content/v2.0.0/knownIssues/_meta.ts [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__)
});
const meta = {
    ledgerStateMismatch: {
        title: "Ledger State Mismatch",
        theme: {
            collapsed: true
        }
    }
};
const __TURBOPACK__default__export__ = meta;
}}),
"[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/_meta.ts [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__)
});
const meta = {
    overview: "Overview",
    "2-beta-3": "2 Beta 3 Issues",
    "2-beta-1": "2 Beta 1 Issues"
};
const __TURBOPACK__default__export__ = meta;
}}),
"[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
/*@jsxRuntime automatic*/ /*@jsxImportSource react*/ __turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__),
    "metadata": (()=>metadata),
    "toc": (()=>toc)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-jsx-dev-runtime.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/mdx-components.js [app-rsc] (ecmascript)");
;
;
const metadata = {
    "title": "🐛 Yaci Store 2 Beta 1 Issues",
    "filePath": "content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx",
    "timestamp": 1758217573362
};
function useTOC(props) {
    return [
        {
            value: "🔍 Known Issues",
            id: "-known-issues",
            depth: 2
        },
        {
            value: "1. Initial Sync Performance Degradation",
            id: "1-initial-sync-performance-degradation",
            depth: 3
        },
        {
            value: "2. Plugin System Initialization Failures",
            id: "2-plugin-system-initialization-failures",
            depth: 3
        },
        {
            value: "3. REST API Rate Limiting Issues",
            id: "3-rest-api-rate-limiting-issues",
            depth: 3
        },
        {
            value: "4. Transaction Output Indexing Gaps",
            id: "4-transaction-output-indexing-gaps",
            depth: 3
        },
        {
            value: "5. Memory Usage Spikes During Epoch Boundaries",
            id: "5-memory-usage-spikes-during-epoch-boundaries",
            depth: 3
        },
        {
            value: "🔧 Historical Workarounds",
            id: "-historical-workarounds",
            depth: 2
        },
        {
            value: "Database Connection Configuration",
            id: "database-connection-configuration",
            depth: 3
        },
        {
            value: "JVM Memory Settings",
            id: "jvm-memory-settings",
            depth: 3
        },
        {
            value: "Plugin Loading Delays",
            id: "plugin-loading-delays",
            depth: 3
        },
        {
            value: "📊 Performance Improvements Since 2 Beta 1",
            id: "-performance-improvements-since-2-beta-1",
            depth: 2
        },
        {
            value: "🚀 Migration from 2 Beta 1",
            id: "-migration-from-2-beta-1",
            depth: 2
        },
        {
            value: "Pre-Migration Checklist",
            id: "pre-migration-checklist",
            depth: 3
        },
        {
            value: "Migration Steps",
            id: "migration-steps",
            depth: 3
        },
        {
            value: "Post-Migration Validation",
            id: "post-migration-validation",
            depth: 3
        },
        {
            value: "📈 Lessons Learned",
            id: "-lessons-learned",
            depth: 2
        },
        {
            value: "For Future Beta Testing",
            id: "for-future-beta-testing",
            depth: 3
        },
        {
            value: "Community Feedback Integration",
            id: "community-feedback-integration",
            depth: 3
        },
        {
            value: "🏆 Community Recognition",
            id: "-community-recognition",
            depth: 2
        },
        {
            value: "📚 Additional Resources",
            id: "-additional-resources",
            depth: 2
        }
    ];
}
const toc = useTOC({});
function _createMdxContent(props) {
    const _components = {
        a: "a",
        code: "code",
        em: "em",
        h1: "h1",
        h2: "h2",
        h3: "h3",
        hr: "hr",
        input: "input",
        li: "li",
        ol: "ol",
        p: "p",
        pre: "pre",
        span: "span",
        strong: "strong",
        table: "table",
        tbody: "tbody",
        td: "td",
        th: "th",
        thead: "thead",
        tr: "tr",
        ul: "ul",
        ...(0, __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["useMDXComponents"])(),
        ...props.components
    };
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["Fragment"], {
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: " "
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h1, {
                children: "🐛 Yaci Store 2 Beta 1 Issues"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Last updated:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 141
                    }, this),
                    " 2025-01-21"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 126
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "This document outlines known issues specific to Yaci Store version 2 Beta 1 release. Most of these issues have been addressed in subsequent beta releases."
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 236
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 431
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 455
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 523
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 606
                    }, this),
                    " Initial blockchain synchronization was significantly slower than expected, particularly for mainnet."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 591
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 119,
                    columnNumber: 799
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 784
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Sync speeds below 100 blocks/minute"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 897
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "High CPU usage during initial sync"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 975
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Database lock contention"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1052
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 875
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Resolution:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1157
                    }, this),
                    " ✅ ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Fixed in 2 Beta 2"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1220
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1142
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Improved batch processing algorithms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1326
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Optimized database queries"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1405
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Reduced lock contention"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1474
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1304
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1563
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1587
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1670
                    }, this),
                    " Custom plugins would occasionally fail to initialize properly on startup, causing missing functionality."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1655
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 119,
                    columnNumber: 1867
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1852
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Plugin activation errors in logs"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 1965
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Missing custom event handlers"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2040
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Incomplete data processing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2112
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 1943
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Resolution:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2219
                    }, this),
                    " ✅ ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Fixed in 2 Beta 2"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2282
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2204
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Improved plugin loading sequence"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2388
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Better error handling and reporting"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2463
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Added plugin health checks"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2541
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2366
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2633
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2657
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 2740
                    }, this),
                    " Built-in rate limiting was too aggressive, causing legitimate requests to be throttled."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2725
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 119,
                    columnNumber: 2920
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2905
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "HTTP 429 responses for normal usage"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 3018
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "API clients timing out"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 3096
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Inconsistent request handling"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 119,
                        columnNumber: 3161
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 2996
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Workaround (Historical):"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 119,
                    columnNumber: 3271
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 3256
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "yaml",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# application.yml - No longer needed in newer versions"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 119,
                                columnNumber: 3494
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 119,
                            columnNumber: 3476
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "yaci"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 122,
                                    columnNumber: 126
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 125,
                                    columnNumber: 33
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 122,
                            columnNumber: 108
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "  store"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 128,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 131,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 128,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "    api"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 134,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 137,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 134,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      rate-limit"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 140,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 143,
                                    columnNumber: 45
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 140,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "        enabled"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 146,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 149,
                                    columnNumber: 44
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "false"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 152,
                                    columnNumber: 31
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#6A737D",
                                        "--shiki-dark": "#6A737D"
                                    },
                                    children: "  # Temporary workaround"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 155,
                                    columnNumber: 34
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 146,
                            columnNumber: 55
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 119,
                    columnNumber: 3458
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 119,
                columnNumber: 3362
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Resolution:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 130
                    }, this),
                    " ✅ ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Fixed in 2 Beta 2"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 193
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 115
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Adjusted default rate limits"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 299
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Improved rate limiting algorithms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 370
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Added configurable thresholds"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 446
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 277
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 541
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 565
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 648
                    }, this),
                    " Some transaction outputs were not being indexed correctly, leading to incomplete UTXO data."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 633
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 158,
                    columnNumber: 832
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 817
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Missing UTXOs in API responses"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 930
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Inconsistent balance calculations"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1003
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Address history gaps"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1079
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 908
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Resolution:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1180
                    }, this),
                    " ✅ ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Fixed in 2 Beta 2"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1243
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1165
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Corrected UTXO indexing logic"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1349
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Added validation checks"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1421
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Implemented gap detection and recovery"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1487
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1327
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1591
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[5].id,
                children: toc[5].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1615
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1698
                    }, this),
                    " Significant memory usage spikes occurred during epoch transitions, sometimes causing OOM errors."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1683
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 158,
                    columnNumber: 1887
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1872
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Memory usage doubling at epoch boundaries"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 1985
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "OutOfMemoryError exceptions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2069
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Performance degradation around epoch changes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2139
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 1963
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Resolution:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2264
                    }, this),
                    " ✅ ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Fixed in 2 Beta 3"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2327
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2249
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Optimized epoch boundary processing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2433
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Implemented streaming for large datasets"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2511
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Added memory pressure monitoring"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 158,
                        columnNumber: 2594
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2411
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2692
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[6].id,
                children: toc[6].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2716
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "These workarounds were necessary for 2 Beta 1 but are no longer required:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2784
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[7].id,
                children: toc[7].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2898
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "yaml",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# No longer needed - fixed in 2 Beta 2"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 158,
                                columnNumber: 3098
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 158,
                            columnNumber: 3080
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "spring"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 161,
                                    columnNumber: 110
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 164,
                                    columnNumber: 35
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 161,
                            columnNumber: 92
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "  datasource"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 167,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 170,
                                    columnNumber: 41
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 167,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "    hikari"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 173,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 176,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 173,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      connection-timeout"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 179,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 182,
                                    columnNumber: 53
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "60000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 185,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 179,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      validation-timeout"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 188,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 191,
                                    columnNumber: 53
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "5000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 194,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 188,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 158,
                    columnNumber: 3062
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 158,
                columnNumber: 2966
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[8].id,
                children: toc[8].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 197,
                columnNumber: 95
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "bash",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# Aggressive settings that were required for 2 Beta 1"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 197,
                                columnNumber: 295
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 197,
                            columnNumber: 277
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#6F42C1",
                                        "--shiki-dark": "#B392F0"
                                    },
                                    children: "java"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 200,
                                    columnNumber: 125
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -Xmx16g"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 203,
                                    columnNumber: 33
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -XX:MaxDirectMemorySize=4g"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 206,
                                    columnNumber: 37
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -XX:+UseG1GC"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 209,
                                    columnNumber: 56
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -jar"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 212,
                                    columnNumber: 42
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#032F62",
                                        "--shiki-dark": "#9ECBFF"
                                    },
                                    children: " yaci-store-2-beta-1.jar"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 215,
                                    columnNumber: 34
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 200,
                            columnNumber: 107
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 197,
                    columnNumber: 259
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 197,
                columnNumber: 163
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[9].id,
                children: toc[9].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 218,
                columnNumber: 115
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "yaml",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# No longer needed - plugin system improved"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 218,
                                columnNumber: 315
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 218,
                            columnNumber: 297
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "yaci"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 221,
                                    columnNumber: 115
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 224,
                                    columnNumber: 33
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 221,
                            columnNumber: 97
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "  store"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 227,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 230,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 227,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "    plugins"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 233,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 236,
                                    columnNumber: 40
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 233,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      initialization-delay"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 239,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 242,
                                    columnNumber: 55
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "30000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 245,
                                    columnNumber: 31
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#6A737D",
                                        "--shiki-dark": "#6A737D"
                                    },
                                    children: "  # 30 second delay"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 248,
                                    columnNumber: 34
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 239,
                            columnNumber: 55
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 218,
                    columnNumber: 279
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 218,
                columnNumber: 183
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 110
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[10].id,
                children: toc[10].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 134
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.table, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.thead, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Metric"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 251,
                                    columnNumber: 258
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "2 Beta 1"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 251,
                                    columnNumber: 301
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "2 Beta 3"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 251,
                                    columnNumber: 346
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Improvement"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                    lineNumber: 251,
                                    columnNumber: 391
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 251,
                            columnNumber: 242
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 223
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tbody, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Initial Sync Speed"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 511
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~80 blocks/min"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 566
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~300 blocks/min"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 617
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: "275% faster"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                            lineNumber: 251,
                                            columnNumber: 685
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 669
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 495
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Memory Usage (Steady State)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 791
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~8GB"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 855
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~4GB"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 896
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: "50% reduction"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                            lineNumber: 251,
                                            columnNumber: 953
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 937
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 775
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "API Response Time (avg)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1061
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~200ms"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1121
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~50ms"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1164
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: "75% faster"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                            lineNumber: 251,
                                            columnNumber: 1222
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1206
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 1045
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Plugin Load Success Rate"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1327
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~85%"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1388
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "~99%"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1429
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: "16% improvement"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                            lineNumber: 251,
                                            columnNumber: 1486
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1470
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 1311
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Database Query Performance"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1596
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Baseline"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1659
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "3x faster"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1704
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: "200% improvement"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                            lineNumber: 251,
                                            columnNumber: 1766
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                        lineNumber: 251,
                                        columnNumber: 1750
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 1580
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 476
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 204
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 1907
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[11].id,
                children: toc[11].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 1931
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "If you’re still using 2 Beta 1, we ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "strongly recommend"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 2055
                    }, this),
                    " upgrading to the latest beta version:"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 2001
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[12].id,
                children: toc[12].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 2182
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                className: "contains-task-list",
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2348
                            }, this),
                            " ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Backup your database"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2399
                            }, this),
                            " completely"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 2305
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2545
                            }, this),
                            " ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Document custom configurations"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2596
                            }, this),
                            " and plugins"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 2502
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2753
                            }, this),
                            " ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Test in staging environment"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2804
                            }, this),
                            " first"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 2710
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 2952
                            }, this),
                            " ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Plan for downtime"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3003
                            }, this),
                            " (typically 2-4 hours for full resync)"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 2909
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 2252
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[13].id,
                children: toc[13].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 3153
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Stop Yaci Store"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3261
                            }, this),
                            " service"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 3245
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Backup database"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3372
                            }, this),
                            " and configuration files"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 3356
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Update application"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3499
                            }, this),
                            " to latest beta"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 3483
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Review configuration changes"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3620
                            }, this),
                            " (many workarounds no longer needed)"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 3604
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Start service"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3772
                            }, this),
                            " and monitor logs"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 3756
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Verify data integrity"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 3890
                            }, this),
                            " with spot checks"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 3874
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 3223
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[14].id,
                children: toc[14].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 4023
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                className: "contains-task-list",
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 4189
                            }, this),
                            " ",
                            "Check API response times"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 4146
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 4334
                            }, this),
                            " ",
                            "Verify plugin functionality"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 4291
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 4482
                            }, this),
                            " ",
                            "Confirm UTXO data completeness"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 4439
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 4633
                            }, this),
                            " ",
                            "Test epoch boundary transitions"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 4590
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 4785
                            }, this),
                            " ",
                            "Monitor memory usage patterns"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 4742
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 4093
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 4915
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[15].id,
                children: toc[15].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 4939
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[16].id,
                children: toc[16].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 5009
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Load testing"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 5117
                            }, this),
                            " is crucial for blockchain indexers"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 5101
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Memory profiling"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 5252
                            }, this),
                            " should be continuous during development"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 5236
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Plugin architecture"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 5396
                            }, this),
                            " needs robust error handling"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 5380
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Database optimization"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 5531
                            }, this),
                            " cannot be an afterthought"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 5515
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 5079
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[17].id,
                children: toc[17].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 5673
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Performance metrics"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 5781
                            }, this),
                            " from community deployments were invaluable"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 5765
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Real-world usage patterns"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 5931
                            }, this),
                            " exposed edge cases"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 5915
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Plugin developer feedback"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 6063
                            }, this),
                            " improved the extensibility framework"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 6047
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 5743
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 6220
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[18].id,
                children: toc[18].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 6244
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Special thanks to early adopters who provided feedback on 2 Beta 1 issues:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 6314
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "DeFi Protocol Teams"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 6467
                            }, this),
                            " - For identifying UTXO indexing gaps"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 6451
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Exchange Integrators"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 6611
                            }, this),
                            " - For uncovering API rate limiting issues"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 6595
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Academic Researchers"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 6761
                            }, this),
                            " - For memory usage pattern analysis"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 6745
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Plugin Developers"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 6905
                            }, this),
                            " - For initialization sequence improvements"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 6889
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 6429
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 7060
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[19].id,
                children: toc[19].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 7084
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                            href: "/docs/v2.0.0/gettingStarted/migration",
                            children: "Migration Guide from 2 Beta 1"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 251,
                            columnNumber: 7192
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 7176
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                            href: "/docs/v2.0.0/advancedConfigurations/performance",
                            children: "Performance Tuning Guide"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 251,
                            columnNumber: 7340
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 7324
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                            href: "/docs/v2.0.0/tutorialsAndUseCases/beta-testing",
                            children: "Beta Testing Best Practices"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 251,
                            columnNumber: 7493
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 7477
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                            href: "https://github.com/bloxbean/yaci-store/releases/tag/v2.0.0-beta.1",
                            children: "GitHub Release Notes"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                            lineNumber: 251,
                            columnNumber: 7648
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                        lineNumber: 251,
                        columnNumber: 7632
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 7154
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 7822
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.em, {
                    children: "This document serves as a historical record of 2 Beta 1 issues and demonstrates the significant improvements made in subsequent releases."
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                    lineNumber: 251,
                    columnNumber: 7861
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-1.mdx.tsx",
                lineNumber: 251,
                columnNumber: 7846
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),
"[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
/*@jsxRuntime automatic*/ /*@jsxImportSource react*/ __turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__),
    "metadata": (()=>metadata),
    "toc": (()=>toc)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-jsx-dev-runtime.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/mdx-components.js [app-rsc] (ecmascript)");
;
;
const metadata = {
    "title": "🐛 Yaci Store 2 Beta 3 Issues",
    "filePath": "content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx",
    "timestamp": 1758217573362
};
function useTOC(props) {
    return [
        {
            value: "🔍 Known Issues",
            id: "-known-issues",
            depth: 2
        },
        {
            value: "1. Database Connection Pool Exhaustion",
            id: "1-database-connection-pool-exhaustion",
            depth: 3
        },
        {
            value: "2. Memory Leak in Event Processing",
            id: "2-memory-leak-in-event-processing",
            depth: 3
        },
        {
            value: "3. Governance Data Indexing Delays",
            id: "3-governance-data-indexing-delays",
            depth: 3
        },
        {
            value: "4. Native Asset Metadata Caching Issues",
            id: "4-native-asset-metadata-caching-issues",
            depth: 3
        },
        {
            value: "🛠️ General Recommendations",
            id: "️-general-recommendations",
            depth: 2
        },
        {
            value: "For Production Deployments",
            id: "for-production-deployments",
            depth: 3
        },
        {
            value: "For Development",
            id: "for-development",
            depth: 3
        },
        {
            value: "🚨 Critical Actions Required",
            id: "-critical-actions-required",
            depth: 2
        },
        {
            value: "Before Upgrading from 2 Beta 2",
            id: "before-upgrading-from-2-beta-2",
            depth: 3
        },
        {
            value: "After Upgrading to 2 Beta 3",
            id: "after-upgrading-to-2-beta-3",
            depth: 3
        },
        {
            value: "📞 Support and Reporting",
            id: "-support-and-reporting",
            depth: 2
        },
        {
            value: "Issue Template",
            id: "issue-template",
            depth: 3
        },
        {
            value: "🔄 Changelog Integration",
            id: "-changelog-integration",
            depth: 2
        }
    ];
}
const toc = useTOC({});
function _createMdxContent(props) {
    const _components = {
        a: "a",
        code: "code",
        h1: "h1",
        h2: "h2",
        h3: "h3",
        hr: "hr",
        input: "input",
        li: "li",
        ol: "ol",
        p: "p",
        pre: "pre",
        span: "span",
        strong: "strong",
        ul: "ul",
        ...(0, __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["useMDXComponents"])(),
        ...props.components
    };
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["Fragment"], {
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: " "
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h1, {
                children: "🐛 Yaci Store 2 Beta 3 Issues"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Last updated:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 88,
                        columnNumber: 141
                    }, this),
                    " 2025-01-21"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 126
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "This document outlines known issues specific to Yaci Store version 2 Beta 3 release."
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 236
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 361
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 385
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 453
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 88,
                        columnNumber: 536
                    }, this),
                    " Under heavy load, the database connection pool may become exhausted, causing new requests to timeout."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 521
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 88,
                    columnNumber: 730
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 715
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Connection timeout errors in logs"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 88,
                        columnNumber: 828
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Slow API response times"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 88,
                        columnNumber: 904
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "HTTP 500 errors during peak usage"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 88,
                        columnNumber: 970
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 806
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Workaround:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 88,
                    columnNumber: 1084
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 1069
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "yaml",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# application.yml"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 88,
                                columnNumber: 1294
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 88,
                            columnNumber: 1276
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "spring"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 91,
                                    columnNumber: 89
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 94,
                                    columnNumber: 35
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 91,
                            columnNumber: 71
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "  datasource"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 97,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 100,
                                    columnNumber: 41
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 97,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "    hikari"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 103,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 106,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 103,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      maximum-pool-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 109,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 112,
                                    columnNumber: 52
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "50"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 115,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 109,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      connection-timeout"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 74
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 121,
                                    columnNumber: 53
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "20000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 124,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 118,
                            columnNumber: 56
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "      idle-timeout"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 127,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 130,
                                    columnNumber: 47
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "300000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 133,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 127,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 88,
                    columnNumber: 1258
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 88,
                columnNumber: 1162
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Status:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 136,
                        columnNumber: 112
                    }, this),
                    " 🔄 Fix planned for 2 Beta 4"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 97
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 218
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 242
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 136,
                        columnNumber: 325
                    }, this),
                    " Long-running instances may experience memory leaks during event processing, particularly with large transaction volumes."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 310
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 136,
                    columnNumber: 538
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 523
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Gradually increasing memory usage"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 136,
                        columnNumber: 636
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "OutOfMemoryError after extended operation"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 136,
                        columnNumber: 712
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "GC pressure warnings"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 136,
                        columnNumber: 796
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 614
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Workaround:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 136,
                    columnNumber: 897
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 882
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "bash",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# Increase JVM memory and enable G1GC"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 136,
                                columnNumber: 1107
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 136,
                            columnNumber: 1089
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#6F42C1",
                                        "--shiki-dark": "#B392F0"
                                    },
                                    children: "java"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 139,
                                    columnNumber: 109
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -Xmx8g"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 142,
                                    columnNumber: 33
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -Xms4g"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 145,
                                    columnNumber: 36
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -XX:+UseG1GC"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 148,
                                    columnNumber: 36
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -XX:MaxGCPauseMillis=200"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 151,
                                    columnNumber: 42
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -jar"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 154,
                                    columnNumber: 54
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#032F62",
                                        "--shiki-dark": "#9ECBFF"
                                    },
                                    children: " yaci-store.jar"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 157,
                                    columnNumber: 34
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 139,
                            columnNumber: 91
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 136,
                    columnNumber: 1071
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 136,
                columnNumber: 975
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Status:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 121
                    }, this),
                    " 🔄 Investigation ongoing"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 106
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 224
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 248
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 331
                    }, this),
                    " Governance proposals and voting data may experience indexing delays of 1-2 epochs during network congestion."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 316
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 160,
                    columnNumber: 532
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 517
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Delayed governance action visibility"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 630
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Inconsistent voting power calculations"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 709
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Missing proposal metadata"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 790
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 608
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Workaround:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 160,
                    columnNumber: 896
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 881
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Monitor epoch boundaries closely"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 996
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Implement retry mechanisms in applications"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1071
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Cross-reference with multiple data sources"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1156
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 974
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Status:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1279
                    }, this),
                    " 🔄 Performance optimization in progress"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 1264
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 1397
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 1421
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Issue:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1504
                    }, this),
                    " Native asset metadata may not update correctly when policies are updated, leading to stale information."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 1489
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Symptoms:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 160,
                    columnNumber: 1700
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 1685
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Outdated asset metadata displayed"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1798
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Incorrect token names or descriptions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1874
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Missing policy updates"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 160,
                        columnNumber: 1954
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 1776
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Workaround:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 160,
                    columnNumber: 2057
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 2042
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "bash",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# Clear metadata cache"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 160,
                                columnNumber: 2267
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 160,
                            columnNumber: 2249
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#6F42C1",
                                        "--shiki-dark": "#B392F0"
                                    },
                                    children: "curl"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 163,
                                    columnNumber: 94
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: " -X"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 166,
                                    columnNumber: 33
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#032F62",
                                        "--shiki-dark": "#9ECBFF"
                                    },
                                    children: " DELETE"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 169,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#032F62",
                                        "--shiki-dark": "#9ECBFF"
                                    },
                                    children: " http://localhost:8080/api/v1/metadata/cache/clear"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                    lineNumber: 172,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 163,
                            columnNumber: 76
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 160,
                    columnNumber: 2231
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 160,
                columnNumber: 2135
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Status:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 156
                    }, this),
                    " 🔄 Caching strategy revision scheduled"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 141
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 273
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[5].id,
                children: toc[5].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 297
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[6].id,
                children: toc[6].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 365
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Monitor memory usage"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 471
                            }, this),
                            " closely and restart instances if memory growth is observed"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 455
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Use external monitoring"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 638
                            }, this),
                            " tools like Prometheus/Grafana"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 622
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Implement circuit breakers"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 779
                            }, this),
                            " for API calls"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 763
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Set up automated alerts"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 907
                            }, this),
                            " for connection pool exhaustion"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 891
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 433
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[7].id,
                children: toc[7].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 1056
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Use smaller datasets"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 1162
                            }, this),
                            " for testing to avoid memory issues"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 1146
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Enable debug logging"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 1305
                            }, this),
                            " for connection pool monitoring"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 1289
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Test governance scenarios"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 1444
                            }, this),
                            " thoroughly before production"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 1428
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 1124
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 1593
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[8].id,
                children: toc[8].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 1617
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[9].id,
                children: toc[9].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 1685
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                className: "contains-task-list",
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 1849
                            }, this),
                            " ",
                            "Backup your database"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 1806
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 1990
                            }, this),
                            " ",
                            "Review connection pool settings"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 1947
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 2142
                            }, this),
                            " ",
                            "Plan for potential downtime during governance data reindexing"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 2099
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 2324
                            }, this),
                            " ",
                            "Update monitoring thresholds"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 2281
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 1753
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[10].id,
                children: toc[10].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 2453
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                className: "contains-task-list",
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 2619
                            }, this),
                            " ",
                            "Monitor memory usage patterns"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 2576
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 2769
                            }, this),
                            " ",
                            "Verify governance data accuracy"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 2726
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 2921
                            }, this),
                            " ",
                            "Test native asset metadata refresh"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 2878
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        className: "task-list-item",
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.input, {
                                type: "checkbox",
                                disabled: true
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 3076
                            }, this),
                            " ",
                            "Validate API response times under load"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 3033
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 2523
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 3215
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[11].id,
                children: toc[11].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 3239
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "If you encounter these or other issues:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 3309
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Check the logs"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 3427
                            }, this),
                            " for specific error messages"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 3411
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Gather system metrics"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 3557
                            }, this),
                            " (CPU, memory, disk usage)"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 3541
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Report via GitHub Issues"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 3692
                            }, this),
                            " with complete reproduction steps"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 3676
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Include environment details"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 3837
                            }, this),
                            " (OS, Java version, database type)"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 3821
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 3389
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[12].id,
                children: toc[12].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 3993
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "plaintext",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Version:** Yaci Store 2 Beta 3"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4200
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4182
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Environment:** [Production/Development/Testing]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4316
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4298
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Database:** [PostgreSQL/H2]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4449
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4431
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Java Version:** [e.g., OpenJDK 21]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4562
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4544
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**OS:** [e.g., Ubuntu 22.04]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4682
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4664
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {}, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4794
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4776
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Issue Description:**"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4857
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4839
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "[Detailed description of the problem]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 4963
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 4945
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {}, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5084
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5066
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Steps to Reproduce:**"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5147
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5129
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "1. [Step 1]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5254
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5236
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "2. [Step 2]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5349
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5331
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "3. [Step 3]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5444
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5426
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {}, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5539
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5521
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Expected Behavior:**"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5602
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5584
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "[What should happen]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5708
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5690
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {}, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5812
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5794
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Actual Behavior:**"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5875
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5857
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "[What actually happens]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 5979
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 5961
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {}, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 6086
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 6068
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Logs/Errors:**"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 6149
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 6131
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 175,
                    columnNumber: 4164
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 4063
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "[Include relevant log excerpts]"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 6268
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "plaintext",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {}, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 6477
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 6459
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "**Additional Context:**"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 6540
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 6522
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                children: "[Any other relevant information]"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                                lineNumber: 175,
                                columnNumber: 6647
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                            lineNumber: 175,
                            columnNumber: 6629
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                    lineNumber: 175,
                    columnNumber: 6441
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 6340
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 6782
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[13].id,
                children: toc[13].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 6806
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "These issues are tracked in the ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                        href: "https://github.com/bloxbean/yaci-store/issues",
                        children: "GitHub Issues"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 6927
                    }, this),
                    " and will be addressed in upcoming releases. Check the ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                        href: "/docs/v2.0.0/changelogs",
                        children: "Changelogs"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                        lineNumber: 175,
                        columnNumber: 7087
                    }, this),
                    " for updates on fixes and improvements."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/2-beta-3.mdx.tsx",
                lineNumber: 175,
                columnNumber: 6876
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),
"[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
/*@jsxRuntime automatic*/ /*@jsxImportSource react*/ __turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__),
    "metadata": (()=>metadata),
    "toc": (()=>toc)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-jsx-dev-runtime.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/mdx-components.js [app-rsc] (ecmascript)");
;
;
const metadata = {
    "title": "🧾 Ledger State Mismatches",
    "filePath": "content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx",
    "timestamp": 1758217573362
};
function useTOC(props) {
    return [
        {
            value: "📊 Overview",
            id: "-overview",
            depth: 2
        },
        {
            value: "Current Status (v2.0.0-beta3)",
            id: "current-status-v200-beta3",
            depth: 3
        },
        {
            value: "Historical Issues (v2.0.0-beta1)",
            id: "historical-issues-v200-beta1",
            depth: 3
        },
        {
            value: "🌐 Mainnet Findings",
            id: "-mainnet-findings",
            depth: 2
        },
        {
            value: "💰 Treasury and Reserves (ADAPot) Mismatches",
            id: "-treasury-and-reserves-adapot-mismatches",
            depth: 3
        },
        {
            value: "🗳️ Governance Proposal Status Mismatches",
            id: "️-governance-proposal-status-mismatches",
            depth: 3
        },
        {
            value: "🧪 Preprod Network Findings",
            id: "-preprod-network-findings",
            depth: 2
        },
        {
            value: "👤 DRep active_until Mismatches",
            id: "-drep-active_until-mismatches",
            depth: 3
        },
        {
            value: "🔍 DRep Distribution Mismatches (Epochs 681-830)",
            id: "-drep-distribution-mismatches-epochs-681-830",
            depth: 3
        },
        {
            value: "🔧 Mismatch Categories Explained",
            id: "-mismatch-categories-explained",
            depth: 2
        },
        {
            value: "Amount Mismatches",
            id: "amount-mismatches",
            depth: 3
        },
        {
            value: "Entries Only in Yaci Store",
            id: "entries-only-in-yaci-store",
            depth: 3
        },
        {
            value: "Entries Only in DB Sync",
            id: "entries-only-in-db-sync",
            depth: 3
        },
        {
            value: "📈 Recommendations for Developers",
            id: "-recommendations-for-developers",
            depth: 2
        },
        {
            value: "For Application Developers",
            id: "for-application-developers",
            depth: 3
        },
        {
            value: "For Infrastructure Operators",
            id: "for-infrastructure-operators",
            depth: 3
        },
        {
            value: "For Researchers",
            id: "for-researchers",
            depth: 3
        },
        {
            value: "🚨 Reporting New Issues",
            id: "-reporting-new-issues",
            depth: 2
        },
        {
            value: "🔄 Status Tracking",
            id: "-status-tracking",
            depth: 2
        }
    ];
}
const toc = useTOC({});
function _createMdxContent(props) {
    const _components = {
        a: "a",
        blockquote: "blockquote",
        code: "code",
        h1: "h1",
        h2: "h2",
        h3: "h3",
        hr: "hr",
        li: "li",
        ol: "ol",
        p: "p",
        strong: "strong",
        table: "table",
        tbody: "tbody",
        td: "td",
        th: "th",
        thead: "thead",
        tr: "tr",
        ul: "ul",
        ...(0, __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["useMDXComponents"])(),
        ...props.components
    };
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["Fragment"], {
        children: [
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: " "
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 112,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h1, {
                children: "🧾 Ledger State Mismatches"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 112,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Last updated:"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 112,
                        columnNumber: 138
                    }, this),
                    " January 2025"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 112,
                columnNumber: 123
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "This document details known discrepancies in Ledger state calculations for Yaci Store across different networks (Mainnet and Preprod). These mismatches help developers and researchers understand potential synchronization challenges when working with Yaci Store."
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 112,
                columnNumber: 235
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                style: {
                    background: 'linear-gradient(135deg, rgba(34, 197, 94, 0.1) 0%, rgba(16, 185, 129, 0.1) 100%)',
                    padding: '1rem',
                    borderRadius: '8px',
                    border: '1px solid rgba(34, 197, 94, 0.2)',
                    marginBottom: '1.5rem'
                },
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "💡 ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Version-Specific Information"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 28
                            }, this),
                            ": Many issues documented below have been ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "resolved in v2.0.0-beta3"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 146
                            }, this),
                            ". See version-specific sections for current status."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 6
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                            children: "Quick Links:"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                            lineNumber: 118,
                            columnNumber: 301
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 286
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                        href: "/knownIssues/ledgerStateMismatch/2-beta-1",
                                        children: "v2.0.0-beta1 Issues"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 412
                                    }, this),
                                    " - Historical issues (mostly resolved)"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 396
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                        href: "/knownIssues/ledgerStateMismatch/2-beta-3",
                                        children: "v2.0.0-beta3 Issues"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 596
                                    }, this),
                                    " - Current status and remaining items"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 580
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                        href: "/changelogs#migration-guide",
                                        children: "Migration Guide"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 779
                                    }, this),
                                    " - How to upgrade between versions"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 763
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 374
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 112,
                columnNumber: 537
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 954
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 978
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "The following categories of mismatches have been identified across different versions:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 1046
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 1173
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "✅ Treasury and Reserves"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1279
                            }, this),
                            " - ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Resolved"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1354
                            }, this),
                            ": Major epoch 546 issues fixed"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 1263
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "✅ Governance Proposal Status"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1480
                            }, this),
                            " - ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Resolved"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1560
                            }, this),
                            ": Critical discrepancies addressed"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 1464
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "🔶 DRep Distribution"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1690
                            }, this),
                            " - ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Improved"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1762
                            }, this),
                            ": Most issues resolved, minor edge cases remain"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 1674
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "🔶 DRep active_until"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1905
                            }, this),
                            " - ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Improved"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 1977
                            }, this),
                            ": Historical bug fixed, monitoring ongoing"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 1889
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 1241
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 2122
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "❌ Treasury and Reserves"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 2228
                            }, this),
                            " - Major calculation mismatches (epoch 546)"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 2212
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "❌ Governance Proposal Status"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 2382
                            }, this),
                            " - Critical status discrepancies"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 2366
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "❌ DRep Distribution"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 2530
                            }, this),
                            " - Widespread distribution mismatches"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 2514
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "❌ DRep active_until"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 2674
                            }, this),
                            " - Known bug affecting multiple epochs"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 2658
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 2190
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 2826
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 2850
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 2918
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Treasury and Reserves calculations match up to epoch 545. A significant mismatch was detected for epoch 546:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 2986
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.table, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.thead, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Epoch"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 3189
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Category"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 3231
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "DB Sync Value"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 3276
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Yaci Store Value"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 3326
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Difference"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 3379
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                            lineNumber: 118,
                            columnNumber: 3173
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 3154
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tbody, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "546"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3498
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Treasury"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3538
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "1,703,528,717,038,308"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3583
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "1,703,528,716,975,798"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3641
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "62,510"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3699
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 3482
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "546"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3775
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Reserves"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3815
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "7,323,776,141,502,598"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3860
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "7,323,776,177,082,630"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3918
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "35,580,032"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 3976
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 3759
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 3463
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 3135
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "⚠️ ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Impact:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 4138
                            }, this),
                            " Treasury shows Yaci Store with 62,510 ADA less, while Reserves shows 35,580,032 ADA more than DB Sync."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 4116
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 4086
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[5].id,
                children: toc[5].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 4350
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "The following governance action shows a status discrepancy between Yaci Store and DB Sync/Koios:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 4418
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.table, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.thead, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Governance Action Type"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 4609
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Transaction Hash"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 4668
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "DB Sync/Koios Status"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 4721
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Yaci Store Status"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 4778
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Notes"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 4832
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                            lineNumber: 118,
                            columnNumber: 4593
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 4574
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tbody, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                    children: "Constitution Change"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 4946
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                        children: "8c653ee5c9800e6d31e79b5a7f7d4400c81d44717ad4db633dc18d4c07e4a4fd"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 5018
                                    }, this)
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 5002
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                    children: "Enacted (epoch 542)"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 5140
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                    children: "Expired"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 5196
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                    children: "Critical governance action status difference"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 5240
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                            lineNumber: 118,
                            columnNumber: 4930
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 4911
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 4555
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "💡 ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Analysis:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 5436
                            }, this),
                            " This represents a significant discrepancy where Yaci Store marked the constitutional change as expired, while the official DB Sync recognized it as enacted in epoch 542."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 5414
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 5384
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 5717
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[6].id,
                children: toc[6].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 5741
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[7].id,
                children: toc[7].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 5809
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Multiple DRep IDs showed different “active until” values compared to db-sync in epoch 224:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 5877
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "🐛 ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Known Bug:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 6060
                            }, this),
                            " In epoch 224, a bug caused active_until values to be one epoch earlier than db-sync for affected DReps."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 6038
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 6008
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[8].id,
                children: toc[8].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 6276
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "The following table shows comprehensive DRep distribution discrepancies across multiple epochs:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 6344
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.table, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.thead, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Epoch(s)"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 6534
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "DRep ID"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 6579
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "DRep Hash"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 6623
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Mismatch Type"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                    lineNumber: 118,
                                    columnNumber: 6669
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                            lineNumber: 118,
                            columnNumber: 6518
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 6499
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tbody, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "736"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 6791
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "drep1y285lm709qqh54a5z5t6vl2kaaxqmszpsxs36dgh3h2n7nqltj46y"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 6847
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 6831
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "8f4fefcf28017a57b41517a67d56ef4c0dc04181a11d35178dd53f4c"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 6979
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 6963
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Amount mismatch (vs DB Sync)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7093
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 6775
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "730–731"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7191
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "drep1ygsusq7j0wx67tzcz9u50rh9l2w6pmmsqglpzzqathlf0rqm87es5"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 7251
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7235
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "21c803d27b8daf2c581179478ee5fa9da0ef70023e11081d5dfe978c"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 7383
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7367
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Amount mismatch (vs DB Sync)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7497
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 7175
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "685–757"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7595
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "drep1ytax4rwzvdwamnu67j2uk9z00660lpzcvmlys6266l9kt5c262k7m"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 7655
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7639
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "fa6a8dc2635dddcf9af495cb144f7eb4ff845866fe48695ad7cb65d3"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 7787
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7771
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Amount mismatch (vs DB Sync)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7901
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 7579
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "682–684"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 7999
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "drep1ytax4rwzvdwamnu67j2uk9z00660lpzcvmlys6266l9kt5c262k7m"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 8059
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8043
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "fa6a8dc2635dddcf9af495cb144f7eb4ff845866fe48695ad7cb65d3"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 8191
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8175
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Only in Yaci Store (not in DB Sync)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8305
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 7983
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "681–683"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8410
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "drep1ytcw6qzpqqclx2yd0zy64ztvlkkhnf6yrzza8whgnq4vz5gh89626"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 8470
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8454
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "f0ed00410031f3288d7889aa896cfdad79a7441885d3bae8982ac151"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 8602
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8586
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Only in Yaci Store (not in DB Sync)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8716
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 8394
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "709–830"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8821
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "drep1ytkdk7j6g4jpnuamve0uy9kv2fyvlprdauv0ymtdpvkrt2gla0chu"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 8881
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8865
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                            children: "ecdb7a5a456419f3bb665fc216cc5248cf846def18f26d6d0b2c35a9"
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                            lineNumber: 118,
                                            columnNumber: 9013
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 8997
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Amount mismatch (vs DB Sync)"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                        lineNumber: 118,
                                        columnNumber: 9127
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 8805
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 6756
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 6480
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 9255
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[9].id,
                children: toc[9].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 9279
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[10].id,
                children: toc[10].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 9347
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Description:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 9455
                            }, this),
                            " Stake amounts differ between Yaci Store and DB Sync for the same DRep"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 9439
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Impact:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 9625
                            }, this),
                            " May affect delegation calculations and rewards distribution"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 9609
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 9417
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[11].id,
                children: toc[11].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 9787
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Description:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 9895
                            }, this),
                            " DRep entries exist in Yaci Store but not in DB Sync for specific epochs"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 9879
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Impact:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 10067
                            }, this),
                            " Could indicate synchronization timing differences or parsing discrepancies"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 10051
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 9857
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[12].id,
                children: toc[12].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 10244
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Description:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 10352
                            }, this),
                            " DRep entries exist in DB Sync but not in Yaci Store"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 10336
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Impact:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 10504
                            }, this),
                            " May result in incomplete delegation data in Yaci Store"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 10488
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 10314
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 10661
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[13].id,
                children: toc[13].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 10685
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[14].id,
                children: toc[14].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 10755
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Be aware"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 10863
                            }, this),
                            " of these known discrepancies when building governance-related features"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 10847
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Implement fallback mechanisms"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 11030
                            }, this),
                            " when critical accuracy is required"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 11014
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Cross-reference"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 11182
                            }, this),
                            " with multiple data sources for governance actions"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 11166
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 10825
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[15].id,
                children: toc[15].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 11342
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Monitor"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 11450
                            }, this),
                            " ledger state consistency regularly"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 11434
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Report new mismatches"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 11580
                            }, this),
                            " to the Yaci Store development team"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 11564
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Consider the impact"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 11724
                            }, this),
                            " on downstream applications and services"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 11708
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 11412
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[16].id,
                children: toc[16].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 11878
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Account for these variations"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 11986
                            }, this),
                            " in academic analysis"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 11970
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Document methodology"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 12123
                            }, this),
                            " when choosing between data sources"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 12107
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Contribute findings"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 12266
                            }, this),
                            " back to the community for transparency"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 12250
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 11948
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 12419
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[17].id,
                children: toc[17].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 12443
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "If you discover additional ledger state mismatches:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 12513
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Document the mismatch"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 12643
                            }, this),
                            " with epoch, transaction hash, and specific values"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 12627
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Compare against DB Sync"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 12802
                            }, this),
                            " or other authoritative sources"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 12786
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Report via GitHub Issues"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 12944
                            }, this),
                            " in the ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                href: "https://github.com/bloxbean/yaci-store",
                                children: "Yaci Store repository"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 13025
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 12928
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Include network information"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 13166
                            }, this),
                            " (Mainnet, Preprod, Preview)"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 13150
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 12605
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 13316
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[18].id,
                children: toc[18].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 13340
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "This document serves as a transparent record of known synchronization challenges. Regular updates ensure the community stays informed about data consistency across the Cardano ecosystem."
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 13410
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "📅 ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Update Schedule:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                                lineNumber: 118,
                                columnNumber: 13689
                            }, this),
                            " This document is reviewed and updated monthly or when significant new mismatches are discovered."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                        lineNumber: 118,
                        columnNumber: 13667
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/knownIssues/ledgerStateMismatch/overview.mdx.tsx",
                lineNumber: 118,
                columnNumber: 13637
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),

};

//# sourceMappingURL=content_v2_0_0_knownIssues_5ffb5b4d._.js.map