module.exports = {

"[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
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
    "title": "🛠️ Chain Indexer Troubleshooting",
    "filePath": "content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx",
    "timestamp": 1758217573548
};
function useTOC(props) {
    return [
        {
            value: "🔗 Yaci Store as a Chain Indexer (Spring Boot App)",
            id: "-yaci-store-as-a-chain-indexer-spring-boot-app",
            depth: 2
        },
        {
            value: "1. 🔄 Sync Not Starting Automatically",
            id: "1--sync-not-starting-automatically",
            depth: 3
        },
        {
            value: "2. 💾 High Disk Usage",
            id: "2--high-disk-usage",
            depth: 3
        },
        {
            value: "3. 🧩 Only Some Stores Are Working",
            id: "3--only-some-stores-are-working",
            depth: 3
        },
        {
            value: "4. 🧠 Ledger State Mismatches",
            id: "4--ledger-state-mismatches",
            depth: 3
        },
        {
            value: "5. 🐘 PostgreSQL Performance Issues",
            id: "5--postgresql-performance-issues",
            depth: 3
        },
        {
            value: "6. 🚦 Memory and Resource Issues",
            id: "6--memory-and-resource-issues",
            depth: 3
        },
        {
            value: "7. 🔄 Sync Recovery and Rollback Issues",
            id: "7--sync-recovery-and-rollback-issues",
            depth: 3
        },
        {
            value: "8. 🌐 Network and Connectivity Issues",
            id: "8--network-and-connectivity-issues",
            depth: 3
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
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h1, {
                children: "🛠️ Chain Indexer Troubleshooting"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "This guide covers common issues when running Yaci Store as a standalone chain indexer (Spring Boot application) and their solutions."
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 130
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 303
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 371
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 454
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 439
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Application starts successfully but shows no block syncing activity"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 556
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Logs show “Sync auto-start is disabled” message"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 666
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Block height remains at 0 or doesn’t increase"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 756
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 534
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Root Causes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 882
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 867
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Auto-sync is disabled in configuration"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 987
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Node connectivity issues"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 1068
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Incorrect network configuration"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 1135
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Node not fully synced"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 1209
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 965
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 1311
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 1296
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Enable Auto-Sync"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 68,
                                        columnNumber: 1451
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 68,
                                columnNumber: 1436
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#D73A49",
                                                    "--shiki-dark": "#F97583"
                                                },
                                                children: "store.sync-auto-start"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 68,
                                                columnNumber: 1677
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#24292E",
                                                    "--shiki-dark": "#E1E4E8"
                                                },
                                                children: "=true"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 71,
                                                columnNumber: 50
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 68,
                                        columnNumber: 1659
                                    }, this)
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 68,
                                    columnNumber: 1641
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 68,
                                columnNumber: 1539
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 68,
                        columnNumber: 1414
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Verify Node Connection"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 74,
                                        columnNumber: 156
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 74,
                                columnNumber: 141
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# For Ogmios"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 74,
                                                columnNumber: 388
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 74,
                                            columnNumber: 370
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "ogmios.url"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 77,
                                                    columnNumber: 84
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=ws://localhost:1337"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 80,
                                                    columnNumber: 39
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 77,
                                            columnNumber: 66
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 83,
                                            columnNumber: 74
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# For Node-to-Client (Unix socket)"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 83,
                                                columnNumber: 140
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 83,
                                            columnNumber: 122
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "n2c.socket-path"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 86,
                                                    columnNumber: 106
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=/path/to/cardano-node/socket"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 89,
                                                    columnNumber: 44
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 86,
                                            columnNumber: 88
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "n2c.host"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 92,
                                                    columnNumber: 101
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=localhost"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 95,
                                                    columnNumber: 37
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 92,
                                            columnNumber: 83
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "n2c.port"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 98,
                                                    columnNumber: 82
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=3001"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 101,
                                                    columnNumber: 37
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 98,
                                            columnNumber: 64
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 74,
                                    columnNumber: 352
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 74,
                                columnNumber: 250
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 74,
                        columnNumber: 119
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Check Node Status"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 104,
                                        columnNumber: 156
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 104,
                                columnNumber: 141
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                                children: [
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Ensure Cardano node is fully synced"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 104,
                                        columnNumber: 267
                                    }, this),
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Verify node accepts connections on configured port"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 104,
                                        columnNumber: 345
                                    }, this),
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: [
                                            "Test connectivity: ",
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                                children: "telnet localhost 3001"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 104,
                                                columnNumber: 477
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 104,
                                        columnNumber: 438
                                    }, this),
                                    "\n"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 104,
                                columnNumber: 245
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 104,
                        columnNumber: 119
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Network Configuration"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 104,
                                        columnNumber: 645
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 104,
                                columnNumber: 630
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Mainnet (default)"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 104,
                                                columnNumber: 876
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 104,
                                            columnNumber: 858
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.cardano.network"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 107,
                                                    columnNumber: 91
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=mainnet"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 110,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 107,
                                            columnNumber: 73
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 113,
                                            columnNumber: 62
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Preprod testnet"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 113,
                                                columnNumber: 128
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 113,
                                            columnNumber: 110
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.cardano.network"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 116,
                                                    columnNumber: 89
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=preprod"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 119,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 116,
                                            columnNumber: 71
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 122,
                                            columnNumber: 62
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Preview testnet  "
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 122,
                                                columnNumber: 128
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 122,
                                            columnNumber: 110
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.cardano.network"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 125,
                                                    columnNumber: 91
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=preview"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 128,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 125,
                                            columnNumber: 73
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 104,
                                    columnNumber: 840
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 104,
                                columnNumber: 738
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 104,
                        columnNumber: 608
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 68,
                columnNumber: 1392
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Verification"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 160
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 145
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Check logs for “Sync started” message"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 266
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Monitor block height progression in database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 346
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "Use ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "/api/v1/blocks/latest"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 131,
                                columnNumber: 457
                            }, this),
                            " endpoint to verify syncing"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 433
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 244
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 596
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 620
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 703
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 688
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "PostgreSQL database grows beyond expected size"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 805
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Disk space warnings or out-of-space errors"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 894
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Performance degradation due to large tables"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 979
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 783
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Root Causes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1103
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 1088
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Pruning disabled or misconfigured"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1208
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Large transaction history accumulation"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1284
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "UTXO set growth without cleanup"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1365
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Missing vacuum/analyze maintenance"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1439
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 1186
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1554
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 1539
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Enable UTXO Pruning"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 131,
                                        columnNumber: 1694
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 131,
                                columnNumber: 1679
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.utxo.pruning-enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 131,
                                                    columnNumber: 1923
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 134,
                                                    columnNumber: 55
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 131,
                                            columnNumber: 1905
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.utxo.pruning.interval"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 137,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=600"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 140,
                                                    columnNumber: 56
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 137,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Keep last 2160 blocks (~1 day)"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 143,
                                                columnNumber: 76
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 143,
                                            columnNumber: 58
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.utxo.pruning.blocks-behind"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 146,
                                                    columnNumber: 104
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=2160"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 149,
                                                    columnNumber: 61
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 146,
                                            columnNumber: 86
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 131,
                                    columnNumber: 1887
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 131,
                                columnNumber: 1785
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 131,
                        columnNumber: 1657
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Enable Transaction Pruning"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 152,
                                        columnNumber: 156
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 152,
                                columnNumber: 141
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.transaction.pruning-enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 152,
                                                    columnNumber: 392
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 155,
                                                    columnNumber: 62
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 152,
                                            columnNumber: 374
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.transaction.pruning.interval"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 158,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=3600"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 161,
                                                    columnNumber: 63
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 158,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Keep last 43200 blocks (~30 days)"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 164,
                                                columnNumber: 77
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 164,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.transaction.pruning.blocks-behind"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 167,
                                                    columnNumber: 107
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=43200"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 170,
                                                    columnNumber: 68
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 167,
                                            columnNumber: 89
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 152,
                                    columnNumber: 356
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 152,
                                columnNumber: 254
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 152,
                        columnNumber: 119
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Database Maintenance"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 173,
                                        columnNumber: 157
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 173,
                                columnNumber: 142
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "sql",
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
                                                children: "-- Run periodically to reclaim space"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 173,
                                                columnNumber: 380
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 173,
                                            columnNumber: 362
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#24292E",
                                                    "--shiki-dark": "#E1E4E8"
                                                },
                                                children: "VACUUM ANALYZE;"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 176,
                                                columnNumber: 108
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 176,
                                            columnNumber: 90
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 179,
                                            columnNumber: 69
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "-- For aggressive cleanup"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 179,
                                                columnNumber: 135
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 179,
                                            columnNumber: 117
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#24292E",
                                                    "--shiki-dark": "#E1E4E8"
                                                },
                                                children: "VACUUM FULL;"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 182,
                                                columnNumber: 97
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 182,
                                            columnNumber: 79
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 173,
                                    columnNumber: 344
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 173,
                                columnNumber: 249
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 173,
                        columnNumber: 120
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Storage Optimization"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 185,
                                        columnNumber: 163
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 185,
                                columnNumber: 148
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Disable unnecessary stores"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 185,
                                                columnNumber: 393
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 185,
                                            columnNumber: 375
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.blocks.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 188,
                                                    columnNumber: 100
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 191,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 188,
                                            columnNumber: 82
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.transactions.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 194,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 197,
                                                    columnNumber: 55
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 194,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.utxos.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 200,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 203,
                                                    columnNumber: 48
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 200,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.rewards.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 206,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=false  "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 209,
                                                    columnNumber: 50
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6A737D",
                                                        "--shiki-dark": "#6A737D"
                                                    },
                                                    children: "# If not needed"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 212,
                                                    columnNumber: 37
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 206,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.governance.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 215,
                                                    columnNumber: 87
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=false  "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 218,
                                                    columnNumber: 53
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6A737D",
                                                        "--shiki-dark": "#6A737D"
                                                    },
                                                    children: "# If not needed"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 221,
                                                    columnNumber: 37
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 215,
                                            columnNumber: 69
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 185,
                                    columnNumber: 357
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 185,
                                columnNumber: 255
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 126
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 131,
                columnNumber: 1635
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Monitoring"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 167
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 152
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "Check database size: ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "SELECT pg_size_pretty(pg_database_size('yaci_store'));"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 224,
                                columnNumber: 312
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 271
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "Monitor table sizes: ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "SELECT schemaname,tablename,pg_size_pretty(pg_total_relation_size(tablename)) FROM pg_tables;"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 224,
                                columnNumber: 471
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 430
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 249
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 651
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 675
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 758
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 743
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Expected data (rewards, governance, assets) is missing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 860
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Only basic block/transaction data appears"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 957
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "API endpoints return empty results for specific data types"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1041
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 838
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Root Causes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1180
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 1165
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Specific stores disabled in configuration"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1285
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Store initialization failures"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1369
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Database schema issues"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1441
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Version compatibility problems"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1506
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 1263
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1617
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 1602
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Enable Required Stores"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 224,
                                        columnNumber: 1757
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 224,
                                columnNumber: 1742
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Core stores"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 224,
                                                columnNumber: 1989
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 224,
                                            columnNumber: 1971
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.blocks.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 227,
                                                    columnNumber: 85
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 230,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 227,
                                            columnNumber: 67
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.transactions.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 233,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 236,
                                                    columnNumber: 55
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 233,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.utxos.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 239,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 242,
                                                    columnNumber: 48
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 239,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 245,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Extended stores"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 245,
                                                columnNumber: 125
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 245,
                                            columnNumber: 107
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.rewards.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 248,
                                                    columnNumber: 89
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 251,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 248,
                                            columnNumber: 71
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.governance.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 254,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 257,
                                                    columnNumber: 53
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 254,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.assets.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 260,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 263,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 260,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.metadata.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 266,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 269,
                                                    columnNumber: 51
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 266,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.script.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 272,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 275,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 272,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.multisig.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 278,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 281,
                                                    columnNumber: 51
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 278,
                                            columnNumber: 59
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 224,
                                    columnNumber: 1953
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 224,
                                columnNumber: 1851
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 224,
                        columnNumber: 1720
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Check Initialization Logs"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 284,
                                        columnNumber: 156
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 284,
                                columnNumber: 141
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
                                                children: "# Look for store initialization errors"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 284,
                                                columnNumber: 385
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 284,
                                            columnNumber: 367
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: "tail"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 287,
                                                    columnNumber: 110
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -f"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 290,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " logs/yaci-store.log"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 293,
                                                    columnNumber: 32
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " |"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 296,
                                                    columnNumber: 49
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: " grep"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 299,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -i"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 302,
                                                    columnNumber: 34
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " \"store\""
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 305,
                                                    columnNumber: 32
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 287,
                                            columnNumber: 92
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 284,
                                    columnNumber: 349
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 284,
                                columnNumber: 253
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 284,
                        columnNumber: 119
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Verify Database Schema"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 308,
                                        columnNumber: 161
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 308,
                                columnNumber: 146
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "sql",
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
                                                children: "-- Check if store tables exist"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 308,
                                                columnNumber: 386
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 308,
                                            columnNumber: 368
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "SELECT"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 311,
                                                    columnNumber: 102
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " table_name "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 314,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "FROM"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 317,
                                                    columnNumber: 41
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " information_schema"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 320,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "."
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 323,
                                                    columnNumber: 48
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: "tables"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 326,
                                                    columnNumber: 30
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 329,
                                                    columnNumber: 35
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 311,
                                            columnNumber: 84
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "WHERE"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 332,
                                                    columnNumber: 73
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " table_schema "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 335,
                                                    columnNumber: 34
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 338,
                                                    columnNumber: 43
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " 'public'"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 341,
                                                    columnNumber: 30
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " AND"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 344,
                                                    columnNumber: 38
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " table_name "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 347,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "LIKE"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 350,
                                                    columnNumber: 41
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " '%reward%'"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 353,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ";"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 356,
                                                    columnNumber: 40
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 332,
                                            columnNumber: 55
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 308,
                                    columnNumber: 350
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 308,
                                columnNumber: 255
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 308,
                        columnNumber: 124
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Reset Specific Store"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 359,
                                        columnNumber: 152
                                    }, this),
                                    " (if needed):"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 359,
                                columnNumber: 137
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Force reset of governance store"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 359,
                                                columnNumber: 394
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 359,
                                            columnNumber: 376
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.governance.reset-on-start"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 362,
                                                    columnNumber: 105
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 365,
                                                    columnNumber: 60
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 362,
                                            columnNumber: 87
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 359,
                                    columnNumber: 358
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 359,
                                columnNumber: 256
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 359,
                        columnNumber: 115
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 224,
                columnNumber: 1698
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Verification"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 157
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 142
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "Check API endpoints: ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "/api/v1/rewards"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 304
                            }, this),
                            ", ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "/api/v1/governance"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 366
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 263
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Verify table creation in database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 448
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Monitor store-specific logs during startup"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 524
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 241
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 632
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 656
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 739
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 724
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Rewards data differs from DB-Sync or official sources"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 841
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "DRep information inconsistencies"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 937
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Governance proposal status discrepancies"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1012
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Treasury/reserves balance mismatches"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1095
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 819
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Root Causes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1212
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 1197
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Known issues with specific epochs or network events"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1317
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Incomplete sync or rollback handling"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1411
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Version-specific bugs"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1490
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Network-specific ledger state complexities"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1554
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 1295
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1677
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 1662
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Check Known Issues"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 368,
                                        columnNumber: 1817
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 1802
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                                children: [
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: [
                                            "Review ",
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                                href: "/knownIssues/ledgerStateMismatch/overview",
                                                children: "Known Issues → Ledger State Mismatch"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 368,
                                                columnNumber: 1956
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 368,
                                        columnNumber: 1929
                                    }, this),
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Verify if your issue matches documented problems"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 368,
                                        columnNumber: 2099
                                    }, this),
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Check for available patches or workarounds"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 368,
                                        columnNumber: 2190
                                    }, this),
                                    "\n"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 1907
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 1780
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Force Resync"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 368,
                                        columnNumber: 2358
                                    }, this),
                                    " (if needed):"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 2343
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Reset from specific slot"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 368,
                                                columnNumber: 2592
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 368,
                                            columnNumber: 2574
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.reset-on-start"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 371,
                                                    columnNumber: 98
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 374,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 371,
                                            columnNumber: 80
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.sync.start-slot"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 377,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=108000000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 380,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 377,
                                            columnNumber: 59
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 368,
                                    columnNumber: 2556
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 2454
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 368,
                        columnNumber: 2321
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Enable Ledger State Validation"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 383,
                                        columnNumber: 161
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 383,
                                columnNumber: 146
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Compare against node's ledger state (performance impact)"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 383,
                                                columnNumber: 401
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 383,
                                            columnNumber: 383
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.ledger-state.validation-enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 386,
                                                    columnNumber: 130
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 389,
                                                    columnNumber: 66
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 386,
                                            columnNumber: 112
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 383,
                                    columnNumber: 365
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 383,
                                columnNumber: 263
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 383,
                        columnNumber: 124
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Report New Issues"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 392,
                                        columnNumber: 156
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 392,
                                columnNumber: 141
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                                children: [
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Document epoch, slot, tx hash, and affected addresses"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 392,
                                        columnNumber: 267
                                    }, this),
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Compare with DB-Sync or Koios API"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 392,
                                        columnNumber: 363
                                    }, this),
                                    "\n",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                        children: "Create detailed GitHub issue with reproduction steps"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 392,
                                        columnNumber: 439
                                    }, this),
                                    "\n"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 392,
                                columnNumber: 245
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 119
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 368,
                columnNumber: 1758
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 603
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[5].id,
                children: toc[5].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 627
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 710
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 695
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Slow query response times"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 812
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Connection timeouts"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 880
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "High CPU/memory usage on database server"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 942
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Sync process slowing down significantly"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1025
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 790
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Root Causes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1145
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 1130
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Insufficient PostgreSQL configuration"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1250
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Missing or suboptimal indexes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1330
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Hardware resource constraints"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1402
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Large dataset without proper partitioning"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1474
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 1228
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1596
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 1581
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Docker/Container Optimization"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 392,
                                        columnNumber: 1736
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 392,
                                columnNumber: 1721
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
                                                children: "# docker-compose.yml"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 392,
                                                columnNumber: 1969
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 392,
                                            columnNumber: 1951
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#22863A",
                                                        "--shiki-dark": "#85E89D"
                                                    },
                                                    children: "services"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 395,
                                                    columnNumber: 92
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ":"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 398,
                                                    columnNumber: 37
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 395,
                                            columnNumber: 74
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#22863A",
                                                        "--shiki-dark": "#85E89D"
                                                    },
                                                    children: "  postgres"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 401,
                                                    columnNumber: 73
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ":"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 404,
                                                    columnNumber: 39
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 401,
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
                                                    children: "    image"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 407,
                                                    columnNumber: 73
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ": "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 410,
                                                    columnNumber: 38
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: "postgres:15"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 413,
                                                    columnNumber: 31
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 407,
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
                                                    children: "    shm_size"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 416,
                                                    columnNumber: 83
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ": "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 419,
                                                    columnNumber: 41
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: "2g"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 422,
                                                    columnNumber: 31
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 416,
                                            columnNumber: 65
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#22863A",
                                                        "--shiki-dark": "#85E89D"
                                                    },
                                                    children: "    environment"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 425,
                                                    columnNumber: 74
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ":"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 428,
                                                    columnNumber: 44
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 425,
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
                                                    children: "      POSTGRES_SHARED_PRELOAD_LIBRARIES"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 431,
                                                    columnNumber: 73
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ": "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 434,
                                                    columnNumber: 68
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: "pg_stat_statements"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 437,
                                                    columnNumber: 31
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 431,
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
                                                    children: "    command"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 440,
                                                    columnNumber: 90
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ": "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 443,
                                                    columnNumber: 40
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "|"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 446,
                                                    columnNumber: 31
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 440,
                                            columnNumber: 72
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: "      postgres -c shared_buffers=1GB"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 449,
                                                columnNumber: 73
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 449,
                                            columnNumber: 55
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: "               -c effective_cache_size=3GB"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 452,
                                                columnNumber: 108
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 452,
                                            columnNumber: 90
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: "               -c work_mem=64MB"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 455,
                                                columnNumber: 114
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 455,
                                            columnNumber: 96
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: "               -c maintenance_work_mem=512MB"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 458,
                                                columnNumber: 103
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 458,
                                            columnNumber: 85
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: "               -c max_connections=200"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 461,
                                                columnNumber: 116
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 461,
                                            columnNumber: 98
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 392,
                                    columnNumber: 1933
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 392,
                                columnNumber: 1837
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 392,
                        columnNumber: 1699
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Native PostgreSQL Configuration"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 464,
                                        columnNumber: 188
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 464,
                                columnNumber: 173
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "sql",
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
                                                children: "-- postgresql.conf optimizations"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 464,
                                                columnNumber: 422
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 464,
                                            columnNumber: 404
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "shared_buffers "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 467,
                                                    columnNumber: 104
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 470,
                                                    columnNumber: 44
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " 1GB"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 473,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 467,
                                            columnNumber: 86
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "effective_cache_size "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 476,
                                                    columnNumber: 76
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 479,
                                                    columnNumber: 50
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " 3GB"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 482,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 476,
                                            columnNumber: 58
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "work_mem "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 485,
                                                    columnNumber: 76
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 488,
                                                    columnNumber: 38
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " 64MB"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 491,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 485,
                                            columnNumber: 58
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "maintenance_work_mem "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 494,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 497,
                                                    columnNumber: 50
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " 512MB"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 500,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 494,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "max_connections "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 503,
                                                    columnNumber: 78
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 506,
                                                    columnNumber: 45
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " 200"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 509,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 503,
                                            columnNumber: 60
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "random_page_cost "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 512,
                                                    columnNumber: 76
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 515,
                                                    columnNumber: 46
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " 1"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 518,
                                                    columnNumber: 30
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "."
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 521,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: "1"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 524,
                                                    columnNumber: 30
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "  # "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 527,
                                                    columnNumber: 30
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "For"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 530,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " SSD"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 533,
                                                    columnNumber: 32
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 512,
                                            columnNumber: 58
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "checkpoint_segments "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 536,
                                                    columnNumber: 76
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 539,
                                                    columnNumber: 49
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " 32"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 542,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 536,
                                            columnNumber: 58
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "checkpoint_completion_target "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 545,
                                                    columnNumber: 75
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "="
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 548,
                                                    columnNumber: 58
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " 0"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 551,
                                                    columnNumber: 30
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "."
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 554,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: "7"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 557,
                                                    columnNumber: 30
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 545,
                                            columnNumber: 57
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 464,
                                    columnNumber: 386
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 464,
                                columnNumber: 291
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 464,
                        columnNumber: 151
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Index Optimization"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 560,
                                        columnNumber: 152
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 560,
                                columnNumber: 137
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "sql",
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
                                                children: "-- Common performance indexes"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 560,
                                                columnNumber: 373
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 560,
                                            columnNumber: 355
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "CREATE"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 563,
                                                    columnNumber: 101
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " INDEX"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 566,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: " CONCURRENTLY"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 569,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " idx_block_number "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 572,
                                                    columnNumber: 42
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "ON"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 575,
                                                    columnNumber: 47
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " block"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 578,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "(block_number);"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 581,
                                                    columnNumber: 35
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 563,
                                            columnNumber: 83
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "CREATE"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 584,
                                                    columnNumber: 87
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " INDEX"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 587,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: " CONCURRENTLY"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 590,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " idx_tx_hash "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 593,
                                                    columnNumber: 42
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "ON"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 596,
                                                    columnNumber: 42
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " transaction"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 599,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "(tx_hash);"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 602,
                                                    columnNumber: 41
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 584,
                                            columnNumber: 69
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "CREATE"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 605,
                                                    columnNumber: 82
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " INDEX"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 608,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: " CONCURRENTLY"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 611,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " idx_utxo_address "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 614,
                                                    columnNumber: 42
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "ON"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 617,
                                                    columnNumber: 47
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " address_utxo("
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 620,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "address"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 623,
                                                    columnNumber: 43
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ");"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 626,
                                                    columnNumber: 36
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 605,
                                            columnNumber: 64
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "CREATE"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 629,
                                                    columnNumber: 74
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: " INDEX"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 632,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: " CONCURRENTLY"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 635,
                                                    columnNumber: 35
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " idx_epoch_rewards "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 638,
                                                    columnNumber: 42
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "ON"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 641,
                                                    columnNumber: 48
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: " reward(epoch, "
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 644,
                                                    columnNumber: 31
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "address"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 647,
                                                    columnNumber: 44
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: ");"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 650,
                                                    columnNumber: 36
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 629,
                                            columnNumber: 56
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 560,
                                    columnNumber: 337
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 560,
                                columnNumber: 242
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 560,
                        columnNumber: 115
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Connection Pool Tuning"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 653,
                                        columnNumber: 153
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 653,
                                columnNumber: 138
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Application connection pool"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 653,
                                                columnNumber: 385
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 653,
                                            columnNumber: 367
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "spring.datasource.hikari.maximum-pool-size"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 656,
                                                    columnNumber: 101
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=20"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 659,
                                                    columnNumber: 71
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 656,
                                            columnNumber: 83
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "spring.datasource.hikari.minimum-idle"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 662,
                                                    columnNumber: 75
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=5"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 665,
                                                    columnNumber: 66
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 662,
                                            columnNumber: 57
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "spring.datasource.hikari.connection-timeout"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 668,
                                                    columnNumber: 74
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=30000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 671,
                                                    columnNumber: 72
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 668,
                                            columnNumber: 56
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "spring.datasource.hikari.idle-timeout"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 674,
                                                    columnNumber: 78
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=600000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 677,
                                                    columnNumber: 66
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 674,
                                            columnNumber: 60
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "spring.datasource.hikari.max-lifetime"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 680,
                                                    columnNumber: 79
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=1800000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 683,
                                                    columnNumber: 66
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 680,
                                            columnNumber: 61
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 653,
                                    columnNumber: 349
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 653,
                                columnNumber: 247
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 653,
                        columnNumber: 116
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 392,
                columnNumber: 1677
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 145
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[6].id,
                children: toc[6].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 169
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 252
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 237
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Out of memory errors"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 354
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Application crashes during sync"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 417
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Extremely slow processing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 491
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "High JVM heap usage"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 559
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 332
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Root Causes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 659
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 644
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Insufficient JVM heap size"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 764
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Memory leaks in event processing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 833
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Large batch sizes"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 908
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Multiple stores with high memory usage"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 968
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 742
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 1087
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 1072
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "JVM Memory Configuration"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 686,
                                        columnNumber: 1227
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 686,
                                columnNumber: 1212
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
                                                children: "# Increase heap size"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 686,
                                                columnNumber: 1455
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 686,
                                            columnNumber: 1437
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
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 689,
                                                    columnNumber: 92
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -Xmx8g"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 692,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -Xms2g"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 695,
                                                    columnNumber: 36
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -jar"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 698,
                                                    columnNumber: 36
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " yaci-store.jar"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 701,
                                                    columnNumber: 34
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 689,
                                            columnNumber: 74
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 704,
                                            columnNumber: 69
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Enable G1GC for better memory management"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 704,
                                                columnNumber: 135
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 704,
                                            columnNumber: 117
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
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 707,
                                                    columnNumber: 114
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -XX:+UseG1GC"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 710,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -XX:MaxGCPauseMillis=200"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 713,
                                                    columnNumber: 42
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -jar"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 716,
                                                    columnNumber: 54
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " yaci-store.jar"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 719,
                                                    columnNumber: 34
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 707,
                                            columnNumber: 96
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 686,
                                    columnNumber: 1419
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 686,
                                columnNumber: 1323
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 686,
                        columnNumber: 1190
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Batch Processing Optimization"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 722,
                                        columnNumber: 166
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 722,
                                columnNumber: 151
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.batch-size"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 722,
                                                    columnNumber: 405
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=100"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 725,
                                                    columnNumber: 45
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 722,
                                            columnNumber: 387
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.parallel-processing"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 728,
                                                    columnNumber: 76
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 731,
                                                    columnNumber: 54
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 728,
                                            columnNumber: 58
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.worker-thread-count"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 734,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=4"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 737,
                                                    columnNumber: 54
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 734,
                                            columnNumber: 59
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 722,
                                    columnNumber: 369
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 722,
                                columnNumber: 267
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 722,
                        columnNumber: 129
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Selective Store Loading"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 740,
                                        columnNumber: 153
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 740,
                                columnNumber: 138
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Only enable essential stores"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 740,
                                                columnNumber: 386
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 740,
                                            columnNumber: 368
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.blocks.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 743,
                                                    columnNumber: 102
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 746,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 743,
                                            columnNumber: 84
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.transactions.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 749,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 752,
                                                    columnNumber: 55
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 749,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.utxos.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 755,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 758,
                                                    columnNumber: 48
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 755,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Disable memory-intensive stores if not needed"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 761,
                                                columnNumber: 77
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 761,
                                            columnNumber: 59
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.assets.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 764,
                                                    columnNumber: 119
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=false"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 767,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 764,
                                            columnNumber: 101
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.metadata.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 770,
                                                    columnNumber: 78
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=false"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 773,
                                                    columnNumber: 51
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 770,
                                            columnNumber: 60
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 740,
                                    columnNumber: 350
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 740,
                                columnNumber: 248
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 740,
                        columnNumber: 116
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 686,
                columnNumber: 1168
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 776,
                columnNumber: 143
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[7].id,
                children: toc[7].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 776,
                columnNumber: 167
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 250
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 776,
                columnNumber: 235
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Sync stops at specific block"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 352
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Data inconsistency after network rollback"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 423
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "“Fork detected” errors"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 507
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Duplicate block processing errors"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 572
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 776,
                columnNumber: 330
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 686
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 776,
                columnNumber: 671
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Enable Rollback Handling"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 776,
                                        columnNumber: 826
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 776,
                                columnNumber: 811
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.rollback.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 776,
                                                    columnNumber: 1060
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 779,
                                                    columnNumber: 51
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 776,
                                            columnNumber: 1042
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.rollback.max-depth"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 782,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=2160"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 785,
                                                    columnNumber: 53
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 782,
                                            columnNumber: 59
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 776,
                                    columnNumber: 1024
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 776,
                                columnNumber: 922
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 776,
                        columnNumber: 789
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Force Resync from Point"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 788,
                                        columnNumber: 156
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 788,
                                columnNumber: 141
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.reset-on-start"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 788,
                                                    columnNumber: 389
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 791,
                                                    columnNumber: 49
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 788,
                                            columnNumber: 371
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.sync.start-slot"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 794,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=107500000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 797,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 794,
                                            columnNumber: 59
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 788,
                                    columnNumber: 353
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 788,
                                columnNumber: 251
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 788,
                        columnNumber: 119
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Monitor Rollback Events"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 800,
                                        columnNumber: 161
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 800,
                                columnNumber: 146
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "bash",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6F42C1",
                                                    "--shiki-dark": "#B392F0"
                                                },
                                                children: "tail"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 800,
                                                columnNumber: 388
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#005CC5",
                                                    "--shiki-dark": "#79B8FF"
                                                },
                                                children: " -f"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 803,
                                                columnNumber: 33
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: " logs/yaci-store.log"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 806,
                                                columnNumber: 32
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#D73A49",
                                                    "--shiki-dark": "#F97583"
                                                },
                                                children: " |"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 809,
                                                columnNumber: 49
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6F42C1",
                                                    "--shiki-dark": "#B392F0"
                                                },
                                                children: " grep"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 812,
                                                columnNumber: 31
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#005CC5",
                                                    "--shiki-dark": "#79B8FF"
                                                },
                                                children: " -i"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 815,
                                                columnNumber: 34
                                            }, this),
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#032F62",
                                                    "--shiki-dark": "#9ECBFF"
                                                },
                                                children: " \"rollback\""
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 818,
                                                columnNumber: 32
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 800,
                                        columnNumber: 370
                                    }, this)
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 800,
                                    columnNumber: 352
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 800,
                                columnNumber: 256
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 800,
                        columnNumber: 124
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 776,
                columnNumber: 767
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 821,
                columnNumber: 150
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[8].id,
                children: toc[8].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 821,
                columnNumber: 174
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Symptoms"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 257
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 821,
                columnNumber: 242
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Intermittent connection drops"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 359
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "“Connection refused” errors"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 431
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Sync stops unexpectedly"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 501
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "High latency in block processing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 567
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 821,
                columnNumber: 337
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Solutions"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 680
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 821,
                columnNumber: 665
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Connection Retry Configuration"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 821,
                                        columnNumber: 820
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 821,
                                columnNumber: 805
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
                                "data-word-wrap": "",
                                "data-pagefind-ignore": "all",
                                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                    children: [
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.network.retry-attempts"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 821,
                                                    columnNumber: 1060
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=5"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 824,
                                                    columnNumber: 57
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 821,
                                            columnNumber: 1042
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.network.retry-delay"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 827,
                                                    columnNumber: 74
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=10000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 830,
                                                    columnNumber: 54
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 827,
                                            columnNumber: 56
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.network.connection-timeout"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 833,
                                                    columnNumber: 78
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=30000"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 836,
                                                    columnNumber: 61
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 833,
                                            columnNumber: 60
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 821,
                                    columnNumber: 1024
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 821,
                                columnNumber: 922
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 821,
                        columnNumber: 783
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Use Multiple Node Endpoints"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 839,
                                        columnNumber: 157
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 839,
                                columnNumber: 142
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                                tabIndex: "0",
                                "data-language": "properties",
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
                                                children: "# Primary node"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 839,
                                                columnNumber: 394
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 839,
                                            columnNumber: 376
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "ogmios.url"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 842,
                                                    columnNumber: 86
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=ws://node1.example.com:1337"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 845,
                                                    columnNumber: 39
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 842,
                                            columnNumber: 68
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Fallback configuration in application"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 848,
                                                columnNumber: 100
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 848,
                                            columnNumber: 82
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.network.fallback-nodes"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 851,
                                                    columnNumber: 111
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=ws://node2.example.com:1337,ws://node3.example.com:1337"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 854,
                                                    columnNumber: 57
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 851,
                                            columnNumber: 93
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 839,
                                    columnNumber: 358
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 839,
                                columnNumber: 256
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 839,
                        columnNumber: 120
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                        children: "Network Monitoring"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 857,
                                        columnNumber: 207
                                    }, this),
                                    ":"
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 857,
                                columnNumber: 192
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
                                                children: "# Test node connectivity"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 857,
                                                columnNumber: 429
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 857,
                                            columnNumber: 411
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
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 860,
                                                    columnNumber: 96
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -I"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 863,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " http://node.example.com:1337"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 866,
                                                    columnNumber: 32
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 860,
                                            columnNumber: 78
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: " "
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 869,
                                            columnNumber: 83
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                style: {
                                                    "--shiki-light": "#6A737D",
                                                    "--shiki-dark": "#6A737D"
                                                },
                                                children: "# Monitor connection stability"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                lineNumber: 869,
                                                columnNumber: 149
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 869,
                                            columnNumber: 131
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#6F42C1",
                                                        "--shiki-dark": "#B392F0"
                                                    },
                                                    children: "ping"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 872,
                                                    columnNumber: 102
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " -c"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 875,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#005CC5",
                                                        "--shiki-dark": "#79B8FF"
                                                    },
                                                    children: " 100"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 878,
                                                    columnNumber: 32
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#032F62",
                                                        "--shiki-dark": "#9ECBFF"
                                                    },
                                                    children: " node.example.com"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                                    lineNumber: 881,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                            lineNumber: 872,
                                            columnNumber: 84
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                    lineNumber: 857,
                                    columnNumber: 393
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 857,
                                columnNumber: 297
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 857,
                        columnNumber: 170
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 821,
                columnNumber: 761
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 884,
                columnNumber: 154
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "💬 ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Still stuck?"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 884,
                                columnNumber: 230
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 884,
                        columnNumber: 208
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                        children: [
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                children: [
                                    "Ask questions on ",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                        href: "https://discord.gg/6AnKrFx4ne",
                                        children: "Bloxbean Developers Discord"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 884,
                                        columnNumber: 368
                                    }, this),
                                    "& ",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                        href: "https://github.com/bloxbean/yaci-store/discussions",
                                        children: "GitHub Discussions"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 884,
                                        columnNumber: 473
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 884,
                                columnNumber: 331
                            }, this),
                            "\n",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                                children: [
                                    "Report bugs via the ",
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                        href: "https://github.com/bloxbean/yaci-store/issues",
                                        children: "Issue Tracker"
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                        lineNumber: 884,
                                        columnNumber: 647
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                                lineNumber: 884,
                                columnNumber: 607
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                        lineNumber: 884,
                        columnNumber: 309
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/troubleshooting/indexerTroubleshooting.mdx.tsx",
                lineNumber: 884,
                columnNumber: 178
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),

};

//# sourceMappingURL=content_v2_0_0_troubleshooting_indexerTroubleshooting_mdx_tsx_3da77633._.js.map