module.exports = {

"[project]/content/v2.0.0/usage/_meta.ts [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__)
});
const meta = {
    asALibrary: "As a Library",
    outOfTheBoxIndexer: "Out of the box Chain Indexer",
    ledgerState: "Ledger State"
};
const __TURBOPACK__default__export__ = meta;
}}),
"[project]/content/v2.0.0/usage/asALibrary.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
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
    "title": "📚 Use Yaci Store as a Library",
    "filePath": "content/v2.0.0/usage/asALibrary.mdx",
    "timestamp": 1758217571004
};
function useTOC(props) {
    const _components = {
        code: "code",
        ...(0, __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["useMDXComponents"])()
    };
    return [
        {
            value: "🧪 Sample Applications",
            id: "-sample-applications",
            depth: 2
        },
        {
            value: "✅ Prerequisites",
            id: "-prerequisites",
            depth: 2
        },
        {
            value: "⚙️ Create a Spring Boot Application",
            id: "️-create-a-spring-boot-application",
            depth: 2
        },
        {
            value: "📦 Add Dependencies",
            id: "-add-dependencies",
            depth: 2
        },
        {
            value: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["Fragment"], {
                children: [
                    "Gradle (",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "build.gradle"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 31,
                        columnNumber: 26
                    }, this),
                    ")"
                ]
            }, void 0, true),
            id: "gradle-buildgradle",
            depth: 3
        },
        {
            value: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["Fragment"], {
                children: [
                    "Maven (",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "pom.xml"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 35,
                        columnNumber: 25
                    }, this),
                    ")"
                ]
            }, void 0, true),
            id: "maven-pomxml",
            depth: 3
        },
        {
            value: "🧩 Add Required Store Starters",
            id: "-add-required-store-starters",
            depth: 2
        },
        {
            value: "Gradle",
            id: "gradle",
            depth: 3
        },
        {
            value: "Maven",
            id: "maven",
            depth: 3
        },
        {
            value: "🛠️ Flyway Configuration",
            id: "️-flyway-configuration",
            depth: 2
        },
        {
            value: "🧾 Yaci Store Configuration",
            id: "-yaci-store-configuration",
            depth: 2
        },
        {
            value: "🔌 Network Configuration",
            id: "-network-configuration",
            depth: 3
        },
        {
            value: "🗄️ Database Configuration",
            id: "️-database-configuration",
            depth: 3
        },
        {
            value: "⚙️ Optional Configuration",
            id: "️-optional-configuration",
            depth: 2
        },
        {
            value: "🌍 Genesis Files",
            id: "-genesis-files",
            depth: 3
        },
        {
            value: "🔗 N2C (Node-to-Client) Configuration",
            id: "-n2c-node-to-client-configuration",
            depth: 3
        },
        {
            value: "🚀 Application Startup",
            id: "-application-startup",
            depth: 2
        },
        {
            value: "⚡ Parallel Processing (Optional)",
            id: "-parallel-processing-optional",
            depth: 2
        },
        {
            value: "🧩 Customization – Override Default Behavior",
            id: "-customization--override-default-behavior",
            depth: 2
        },
        {
            value: "🗃️ Custom Storage",
            id: "️-custom-storage",
            depth: 3
        },
        {
            value: "⚙️ Custom Processor",
            id: "️-custom-processor",
            depth: 3
        },
        {
            value: "🔁 Rollback Handling",
            id: "-rollback-handling",
            depth: 3
        },
        {
            value: "🧹 Remove Optional Indexes During Initial Sync",
            id: "-remove-optional-indexes-during-initial-sync",
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
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h1, {
                children: "📚 Use Yaci Store as a Library"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Yaci Store can be used as a ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "library in your own application"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 174
                    }, this),
                    ", giving you full control over what data you want to index. This is made possible by its ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "modular architecture"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 343
                    }, this),
                    " and ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Spring dependency injection"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 417
                    }, this),
                    "."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 127
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "For example, you can:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 516
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Store only UTXOs for specific addresses"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 600
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Persist metadata with specific labels"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 682
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Use your own storage backend"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 762
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 578
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 856
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 880
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "A metadata indexer project using Yaci Store as a library"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 970
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "A scoped indexer for Ada Handler"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 1069
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 948
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1167
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1191
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                            children: "Java 21"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 128,
                            columnNumber: 1297
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 1281
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Spring Boot"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 128,
                                columnNumber: 1388
                            }, this),
                            " ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "3.2.x"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 128,
                                columnNumber: 1449
                            }, this),
                            " (tested with ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "3.2.0"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 128,
                                columnNumber: 1513
                            }, this),
                            ", ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "3.2.1"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 128,
                                columnNumber: 1565
                            }, this),
                            ", ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "3.2.2"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 128,
                                columnNumber: 1617
                            }, this),
                            ")"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 1372
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1259
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1714
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1738
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Use ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                        href: "https://start.spring.io/",
                        children: "Spring Initializr"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 1829
                    }, this),
                    " to scaffold your Spring Boot app."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1806
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1973
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 1997
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 2065
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "groovy",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "implementation "
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 128,
                                columnNumber: 2267
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "'com.bloxbean.cardano:yaci-store-spring-boot-starter:<version>'"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 131,
                                columnNumber: 44
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 128,
                        columnNumber: 2249
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 128,
                    columnNumber: 2231
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 128,
                columnNumber: 2133
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[5].id,
                children: toc[5].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 134,
                columnNumber: 154
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "xml",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "<"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 134,
                                    columnNumber: 353
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "dependency"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 137,
                                    columnNumber: 30
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 140,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 134,
                            columnNumber: 335
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "  <"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 143,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "groupId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 146,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">com.bloxbean.cardano</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 149,
                                    columnNumber: 36
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "groupId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 152,
                                    columnNumber: 52
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 155,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 143,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "  <"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 158,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "artifactId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 161,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">yaci-store-spring-boot-starter</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 164,
                                    columnNumber: 39
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "artifactId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 167,
                                    columnNumber: 62
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 170,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 158,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "  <"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 173,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "version"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 176,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">{version}</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 179,
                                    columnNumber: 36
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "version"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 182,
                                    columnNumber: 41
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 185,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 173,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 188,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "dependency"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 191,
                                    columnNumber: 31
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 194,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 188,
                            columnNumber: 55
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 134,
                    columnNumber: 317
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 134,
                columnNumber: 222
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 197,
                columnNumber: 92
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[6].id,
                children: toc[6].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 197,
                columnNumber: 116
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "For example, to include the metadata store:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 197,
                columnNumber: 184
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[7].id,
                children: toc[7].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 197,
                columnNumber: 268
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "groovy",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "implementation "
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 197,
                                columnNumber: 470
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "'com.bloxbean.cardano:yaci-store-metadata-spring-boot-starter:<version>'"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 200,
                                columnNumber: 44
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 197,
                        columnNumber: 452
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 197,
                    columnNumber: 434
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 197,
                columnNumber: 336
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[8].id,
                children: toc[8].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 203,
                columnNumber: 163
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "xml",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "<"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 203,
                                    columnNumber: 362
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "dependency"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 206,
                                    columnNumber: 30
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 209,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 203,
                            columnNumber: 344
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "  <"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 212,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "groupId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 215,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">com.bloxbean.cardano</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 218,
                                    columnNumber: 36
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "groupId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 221,
                                    columnNumber: 52
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 224,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 212,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "  <"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 227,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "artifactId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 230,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">yaci-store-metadata-spring-boot-starter</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 233,
                                    columnNumber: 39
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "artifactId"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 236,
                                    columnNumber: 71
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 239,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 227,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "  <"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 242,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "version"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 245,
                                    columnNumber: 32
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">{version}</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 248,
                                    columnNumber: 36
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "version"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 251,
                                    columnNumber: 41
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 254,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 242,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "</"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 257,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "dependency"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 260,
                                    columnNumber: 31
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ">"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 263,
                                    columnNumber: 39
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 257,
                            columnNumber: 55
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 203,
                    columnNumber: 326
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 203,
                columnNumber: 231
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: "📘 You can include multiple store starters. Full list of supported starters is available in the documentation."
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 266,
                        columnNumber: 122
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 266,
                columnNumber: 92
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 266,
                columnNumber: 304
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[9].id,
                children: toc[9].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 266,
                columnNumber: 328
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Add the following to your ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application.yml"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 266,
                        columnNumber: 441
                    }, this),
                    ":"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 266,
                columnNumber: 396
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
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "spring"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 266,
                                    columnNumber: 656
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 269,
                                    columnNumber: 35
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 266,
                            columnNumber: 638
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "  flyway"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 272,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ":"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 275,
                                    columnNumber: 37
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 272,
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
                                    children: "    locations"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 278,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 281,
                                    columnNumber: 42
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#032F62",
                                        "--shiki-dark": "#9ECBFF"
                                    },
                                    children: "classpath:db/store/{vendor}"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 284,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 278,
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
                                    children: "    out-of-order"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 287,
                                    columnNumber: 99
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 290,
                                    columnNumber: 45
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#005CC5",
                                        "--shiki-dark": "#79B8FF"
                                    },
                                    children: "true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 293,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 287,
                            columnNumber: 81
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: " "
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 296,
                            columnNumber: 58
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#22863A",
                                        "--shiki-dark": "#85E89D"
                                    },
                                    children: "apiPrefix"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 296,
                                    columnNumber: 124
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: ": "
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 299,
                                    columnNumber: 38
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#032F62",
                                        "--shiki-dark": "#9ECBFF"
                                    },
                                    children: "/api/v1"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 302,
                                    columnNumber: 31
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 296,
                            columnNumber: 106
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 266,
                    columnNumber: 620
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 266,
                columnNumber: 524
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "📝 For Yaci Store ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "0.0.11"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 305,
                                columnNumber: 165
                            }, this),
                            " or earlier, use ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "classpath:db/migration/{vendor}"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 305,
                                columnNumber: 233
                            }, this),
                            " instead."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 305,
                        columnNumber: 128
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 98
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "You can also define your own Flyway locations for custom migrations."
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 371
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 480
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[10].id,
                children: toc[10].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 504
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Download the ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application.properties"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 305,
                        columnNumber: 606
                    }, this),
                    " file from the release page and place it in a folder named ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "config"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 305,
                        columnNumber: 732
                    }, this),
                    " at the root of your project."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 574
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Alternatively, add the configs directly into ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application.yml"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 305,
                        columnNumber: 898
                    }, this),
                    "."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 834
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[11].id,
                children: toc[11].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 981
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
                                    children: "store.cardano.host"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 305,
                                    columnNumber: 1189
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=preprod-node.world.dev.cardano.org"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 308,
                                    columnNumber: 47
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 305,
                            columnNumber: 1171
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.port"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 311,
                                    columnNumber: 107
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=30000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 314,
                                    columnNumber: 47
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 311,
                            columnNumber: 89
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.protocol-magic"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 317,
                                    columnNumber: 78
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=1"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 320,
                                    columnNumber: 57
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 317,
                            columnNumber: 60
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 305,
                    columnNumber: 1153
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 305,
                columnNumber: 1051
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[12].id,
                children: toc[12].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 323,
                columnNumber: 93
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
                                    children: "spring.datasource.url"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 323,
                                    columnNumber: 301
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "="
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 326,
                                    columnNumber: 50
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 323,
                            columnNumber: 283
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "spring.datasource.username"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 329,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=user"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 332,
                                    columnNumber: 55
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 329,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "spring.datasource.password"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 335,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "="
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 338,
                                    columnNumber: 55
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 335,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 323,
                    columnNumber: 265
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 323,
                columnNumber: 163
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 341,
                columnNumber: 92
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[13].id,
                children: toc[13].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 341,
                columnNumber: 116
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[14].id,
                children: toc[14].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 341,
                columnNumber: 186
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Genesis files are required for certain modules (e.g., UTXO, epoch, aggregate):"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 341,
                columnNumber: 256
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
                                    children: "store.cardano.byron-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 341,
                                    columnNumber: 513
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/to/byron-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 344,
                                    columnNumber: 61
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 341,
                            columnNumber: 495
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.shelley-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 347,
                                    columnNumber: 100
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/to/shelley-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 350,
                                    columnNumber: 63
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 347,
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
                                    children: "store.cardano.alonzo-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 353,
                                    columnNumber: 102
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/to/alonzo-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 356,
                                    columnNumber: 62
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 353,
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
                                    children: "store.cardano.conway-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 359,
                                    columnNumber: 101
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/to/conway-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 362,
                                    columnNumber: 62
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 359,
                            columnNumber: 83
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 341,
                    columnNumber: 477
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 341,
                columnNumber: 375
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "For ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "mainnet"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 365,
                        columnNumber: 143
                    }, this),
                    ", ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "preprod"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 365,
                        columnNumber: 201
                    }, this),
                    ", or ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "preview"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 365,
                        columnNumber: 262
                    }, this),
                    ", these are included by default."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 365,
                columnNumber: 120
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[15].id,
                children: toc[15].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 365,
                columnNumber: 372
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Required for transaction submission and fetching protocol parameters:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 365,
                columnNumber: 442
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
                                children: "#store.cardano.n2c-node-socket-path=/path/to/node.socket"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 365,
                                columnNumber: 690
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 365,
                            columnNumber: 672
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: " "
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 368,
                            columnNumber: 110
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# If using a relay (e.g., socat)"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 368,
                                columnNumber: 176
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 368,
                            columnNumber: 158
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.n2c-host=<relay_host>"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 371,
                                columnNumber: 104
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 371,
                            columnNumber: 86
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.n2c-port=<relay_port>"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 374,
                                columnNumber: 108
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 374,
                            columnNumber: 90
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 365,
                    columnNumber: 654
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 365,
                columnNumber: 552
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 127
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[16].id,
                children: toc[16].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 151
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "When you start your app:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 221
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Yaci Store creates required tables"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 377,
                        columnNumber: 308
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Syncs blockchain data"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 377,
                        columnNumber: 385
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Publishes events for processing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 377,
                        columnNumber: 449
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 286
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 546
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[17].id,
                children: toc[17].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 570
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Enable parallel sync for faster performance:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 640
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
                                    children: "store.executor.enable-parallel-processing"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 377,
                                    columnNumber: 863
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 380,
                                    columnNumber: 70
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 377,
                            columnNumber: 845
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.executor.blocks-batch-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 383,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=100"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 386,
                                    columnNumber: 61
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 383,
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
                                    children: "store.executor.blocks-partition-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 389,
                                    columnNumber: 76
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=10"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 392,
                                    columnNumber: 65
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 389,
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
                                    children: "store.executor.use-virtual-thread-for-batch-processing"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 395,
                                    columnNumber: 75
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 398,
                                    columnNumber: 83
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 395,
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
                                    children: "store.executor.use-virtual-thread-for-event-processing"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 401,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                    lineNumber: 404,
                                    columnNumber: 83
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                            lineNumber: 401,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                    lineNumber: 377,
                    columnNumber: 827
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 377,
                columnNumber: 725
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 96
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[18].id,
                children: toc[18].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 120
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[19].id,
                children: toc[19].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 190
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "You can override the default storage implementation to:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 260
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Use a different database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 378
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Filter by metadata labels"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 445
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Store scoped data only"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 513
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 356
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: "🔍 See the sample projects for implementation references."
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 631
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 601
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[20].id,
                children: toc[20].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 760
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "You can listen to core or derived events to perform custom logic. This is useful for:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 830
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Filtering"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 978
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Analytics"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 1030
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: "Auditing"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 1082
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 956
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[21].id,
                children: toc[21].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 1156
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Default Storage"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 407,
                                columnNumber: 1264
                            }, this),
                            ": Rollbacks are handled automatically."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 1248
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Custom Storage"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 407,
                                columnNumber: 1405
                            }, this),
                            ": You must handle ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "RollbackEvent"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 407,
                                columnNumber: 1486
                            }, this),
                            " manually and ensure ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "idempotency"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 407,
                                columnNumber: 1565
                            }, this),
                            "."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 1389
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 1226
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 1672
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[22].id,
                children: toc[22].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 1696
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "To speed up the initial sync:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 1766
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ol, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "Remove optional indexes listed in ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "drop-index.sql"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 407,
                                columnNumber: 1912
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 1858
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            "Re-add them after full sync using ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "create-index.sql"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                                lineNumber: 407,
                                columnNumber: 2044
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 1990
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 1836
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: "💡 Each store has its own index files in the repository."
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                        lineNumber: 407,
                        columnNumber: 2177
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/asALibrary.mdx.tsx",
                lineNumber: 407,
                columnNumber: 2147
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),
"[project]/content/v2.0.0/usage/ledgerState.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
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
    "title": "Ledgerstate",
    "filePath": "content/v2.0.0/usage/ledgerState.mdx",
    "timestamp": 1758217573362
};
function useTOC(props) {
    return [
        {
            value: "🧮 Ledger State - Aggregation App",
            id: "-ledger-state---aggregation-app",
            depth: 2
        },
        {
            value: "🧭 Modes of Operation",
            id: "-modes-of-operation",
            depth: 2
        },
        {
            value: "⚙️ Mode 1 - Companion Mode",
            id: "️-mode-1---companion-mode",
            depth: 3
        },
        {
            value: "⚙️ Mode 2 - Standalone Mode",
            id: "️-mode-2---standalone-mode",
            depth: 3
        },
        {
            value: "🧪 Mode 3 - Standalone with RocksDB (Experimental)",
            id: "-mode-3---standalone-with-rocksdb-experimental",
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
        em: "em",
        h2: "h2",
        h3: "h3",
        hr: "hr",
        li: "li",
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
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Using ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "yaci-store-aggregation-app"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 147
                    }, this),
                    ", you can track account balance related data. As account balance calculation is a resource-intensive task, this application should be run separately."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 122
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Like the main application, place ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application.properties"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 441
                    }, this),
                    " in a folder named ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "config"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 527
                    }, this),
                    " in the same directory as the jar file. The application will automatically detect the configuration file."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 389
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "👉 Swagger UI: ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                        href: "http://localhost:8081/swagger-ui.html",
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                            children: "http://localhost:8081/swagger-ui.html"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 51,
                            columnNumber: 799
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 739
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 705
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 915
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 939
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 1007
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "🔹 ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Reads UTxOs from the main database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 1097
                    }, this),
                    " and ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "writes account balance"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 1185
                    }, this),
                    " to the ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "same database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 1264
                    }, this),
                    "."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 1075
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "In this mode, the aggregation app starts from the genesis block but only writes account balance data. It reads UTxOs from the database (already synced by ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "yaci-store-all"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 1522
                    }, this),
                    " or ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "yaci-store-utxo-indexer"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 1585
                    }, this),
                    ")."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 1349
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Note:"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 51,
                                columnNumber: 1722
                            }, this),
                            " Ensure this app is started ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.em, {
                                children: "after"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 51,
                                columnNumber: 1804
                            }, this),
                            " the main app has synced for some time."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 1707
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 1677
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Run:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 51,
                    columnNumber: 1957
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 1942
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 51,
                                columnNumber: 2160
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 54,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-aggregation-app-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 57,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 60,
                                columnNumber: 57
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 63,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 66,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 69,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 72,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 51,
                        columnNumber: 2142
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 51,
                    columnNumber: 2124
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 51,
                columnNumber: 2028
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Clean balance data and restart from genesis:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 75,
                    columnNumber: 110
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 75,
                columnNumber: 95
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 75,
                                columnNumber: 353
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dstore.aggr.clean-db-before-start=true"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 78,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 81,
                                columnNumber: 69
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-aggregation-app-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 84,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 87,
                                columnNumber: 57
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 90,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 93,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 96,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 99,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 75,
                        columnNumber: 335
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 75,
                    columnNumber: 317
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 75,
                columnNumber: 221
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 95
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 119
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "🔹 ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Writes UTxOs and account balances to a separate database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 209
                    }, this),
                    "."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 187
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Does not depend on ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "yaci-store-all"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 375
                    }, this),
                    " or ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "utxo-indexer"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 438
                    }, this),
                    " to run."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 337
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "➡️ Requires additional ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application-aggr.properties"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 567
                    }, this),
                    " (download from releases)."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 525
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Place both files in ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "config/"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 726
                    }, this),
                    " folder:"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 687
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                            children: "application.properties"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 102,
                            columnNumber: 846
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 830
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                            children: "application-aggr.properties"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 102,
                            columnNumber: 948
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 932
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 808
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Edit:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 1062
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "application.properties"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 102,
                                columnNumber: 1146
                            }, this),
                            ": database connection for balance data"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 1130
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "application-aggr.properties"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 102,
                                columnNumber: 1290
                            }, this),
                            ":",
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
                                                    children: "store.account.enabled"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 102,
                                                    columnNumber: 1507
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 105,
                                                    columnNumber: 50
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                            lineNumber: 102,
                                            columnNumber: 1489
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "store.extensions.utxo-storage-type"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 108,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=default"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 111,
                                                    columnNumber: 63
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                            lineNumber: 108,
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
                                                    children: "spring.flyway.locations"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 114,
                                                    columnNumber: 80
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=classpath:db/store/{vendor}"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 117,
                                                    columnNumber: 52
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                            lineNumber: 114,
                                            columnNumber: 62
                                        }, this),
                                        "\n",
                                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                            children: [
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#D73A49",
                                                        "--shiki-dark": "#F97583"
                                                    },
                                                    children: "spring.flyway.out-of-order"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 120,
                                                    columnNumber: 100
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=true"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 123,
                                                    columnNumber: 55
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                            lineNumber: 120,
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
                                                    children: "logging.file.name"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 126,
                                                    columnNumber: 77
                                                }, this),
                                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                                    style: {
                                                        "--shiki-light": "#24292E",
                                                        "--shiki-dark": "#E1E4E8"
                                                    },
                                                    children: "=./logs/yaci-store-aggr.log"
                                                }, void 0, false, {
                                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                                    lineNumber: 129,
                                                    columnNumber: 46
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                            lineNumber: 126,
                                            columnNumber: 59
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 102,
                                    columnNumber: 1471
                                }, this)
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 102,
                                columnNumber: 1369
                            }, this),
                            "\n"
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 102,
                        columnNumber: 1274
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 102,
                columnNumber: 1108
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Run with aggr profile:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 132,
                    columnNumber: 179
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 132,
                columnNumber: 164
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 132,
                                columnNumber: 400
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dspring.profiles.active=aggr"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 135,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 138,
                                columnNumber: 59
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-aggregation-app-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 141,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 144,
                                columnNumber: 57
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 147,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 150,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 153,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 156,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 132,
                        columnNumber: 382
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 132,
                    columnNumber: 364
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 132,
                columnNumber: 268
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Clean and restart:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 159,
                    columnNumber: 110
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 159,
                columnNumber: 95
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 159,
                                columnNumber: 327
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dstore.aggr.clean-db-before-start=true"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 162,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dspring.profiles.active=aggr"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 165,
                                columnNumber: 69
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 168,
                                columnNumber: 59
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-aggregation-app-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 171,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 174,
                                columnNumber: 57
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 177,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 180,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 183,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 186,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 159,
                        columnNumber: 309
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 159,
                    columnNumber: 291
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 159,
                columnNumber: 195
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 189,
                columnNumber: 95
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 189,
                columnNumber: 119
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "🔹 ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "Stores UTxOs in embedded RocksDB"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 189,
                        columnNumber: 209
                    }, this),
                    ", account balances in a ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "separate database"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 189,
                        columnNumber: 314
                    }, this),
                    "."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 189,
                columnNumber: 187
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "➡️ Still requires ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application-aggr.properties"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 189,
                        columnNumber: 440
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 189,
                columnNumber: 403
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Place both files in ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "config/"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 189,
                        columnNumber: 569
                    }, this),
                    " folder. Update properties:"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 189,
                columnNumber: 530
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
                                    children: "store.account.enabled"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 189,
                                    columnNumber: 808
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 192,
                                    columnNumber: 50
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 189,
                            columnNumber: 790
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "spring.flyway.locations"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 195,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=classpath:db/store/{vendor}"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 198,
                                    columnNumber: 52
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 195,
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
                                    children: "spring.flyway.out-of-order"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 201,
                                    columnNumber: 100
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 204,
                                    columnNumber: 55
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 201,
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
                                    children: "logging.file.name"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 207,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=./logs/yaci-store-aggr.log"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 210,
                                    columnNumber: 46
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 207,
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
                                    children: "store.rocksdb.base-dir"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 213,
                                    columnNumber: 99
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=./rocksdb"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 216,
                                    columnNumber: 51
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 213,
                            columnNumber: 81
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.extensions.utxo-storage-type"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 219,
                                    columnNumber: 82
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=rocksdb"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 222,
                                    columnNumber: 63
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 219,
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
                                    children: "store.extensions.rocksdb-utxo-storage.write-batch-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 225,
                                    columnNumber: 80
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=3000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 228,
                                    columnNumber: 83
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 225,
                            columnNumber: 62
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.extensions.rocksdb-utxo-storage.parallel-write"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 231,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                    lineNumber: 234,
                                    columnNumber: 81
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                            lineNumber: 231,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 189,
                    columnNumber: 772
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 189,
                columnNumber: 670
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Run with aggr profile:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 237,
                    columnNumber: 111
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 237,
                columnNumber: 96
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 237,
                                columnNumber: 332
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dspring.profiles.active=aggr"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 240,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 243,
                                columnNumber: 59
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-aggregation-app-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 246,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 249,
                                columnNumber: 57
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 252,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 255,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 258,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 261,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 237,
                        columnNumber: 314
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 237,
                    columnNumber: 296
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 237,
                columnNumber: 200
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                    children: "Clean and restart:"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 264,
                    columnNumber: 110
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 264,
                columnNumber: 95
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 264,
                                columnNumber: 327
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dstore.aggr.clean-db-before-start=true"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 267,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -Dspring.profiles.active=aggr"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 270,
                                columnNumber: 69
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 273,
                                columnNumber: 59
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-aggregation-app-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 276,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 279,
                                columnNumber: 57
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 282,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 285,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 288,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                                lineNumber: 291,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                        lineNumber: 264,
                        columnNumber: 309
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                    lineNumber: 264,
                    columnNumber: 291
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/ledgerState.mdx.tsx",
                lineNumber: 264,
                columnNumber: 195
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),
"[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
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
    "title": "🚀 Chain Indexer",
    "filePath": "content/v2.0.0/usage/outOfTheBoxIndexer.mdx",
    "timestamp": 1758217571004
};
function useTOC(props) {
    return [
        {
            value: "🚀 Available Out of the Box Applications",
            id: "-available-out-of-the-box-applications",
            depth: 3
        },
        {
            value: "⚙️ Configuration",
            id: "️-configuration",
            depth: 2
        },
        {
            value: "🛠️ Setup",
            id: "️-setup",
            depth: 3
        },
        {
            value: "🔌 Mandatory Configuration",
            id: "-mandatory-configuration",
            depth: 3
        },
        {
            value: "Network",
            id: "network",
            depth: 4
        },
        {
            value: "Database",
            id: "database",
            depth: 4
        },
        {
            value: "⚡ Parallel Processing (Optional)",
            id: "-parallel-processing-optional",
            depth: 3
        },
        {
            value: "🧪 Genesis File Configuration (for custom networks)",
            id: "-genesis-file-configuration-for-custom-networks",
            depth: 3
        },
        {
            value: "🔗 N2C Configuration (Optional)",
            id: "-n2c-configuration-optional",
            depth: 3
        },
        {
            value: "🛡️ Enable / Disable Specific Stores",
            id: "️-enable--disable-specific-stores",
            depth: 3
        },
        {
            value: "🌟 Start from a Specific Point",
            id: "-start-from-a-specific-point",
            depth: 3
        },
        {
            value: "🔮 Run as JAR",
            id: "-run-as-jar",
            depth: 2
        },
        {
            value: "📜 Swagger UI",
            id: "-swagger-ui",
            depth: 2
        },
        {
            value: "📃 DB Indexes (Post-Sync)",
            id: "-db-indexes-post-sync",
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
        em: "em",
        h1: "h1",
        h2: "h2",
        h3: "h3",
        h4: "h4",
        hr: "hr",
        li: "li",
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
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 12
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h1, {
                children: "🚀 Chain Indexer"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 54
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Yaci Store comes with a few applications that you can use directly. These applications are designed to be used as-is:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 113
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[0].id,
                children: toc[0].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 271
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.table, {
                children: [
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.thead, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Application"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 95,
                                    columnNumber: 393
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Description"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 95,
                                    columnNumber: 441
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.th, {
                                    children: "Use Case"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 95,
                                    columnNumber: 489
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 95,
                            columnNumber: 377
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 95,
                        columnNumber: 358
                    }, this),
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tbody, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                                children: "yaci-store-all"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 642
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                            lineNumber: 95,
                                            columnNumber: 622
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 606
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Bundles all available modules/stores into a single application."
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 735
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: [
                                            "Index ",
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                                children: "all available data"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 861
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 835
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 95,
                                columnNumber: 590
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                                children: "yaci-store-utxo-indexer"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 1010
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                            lineNumber: 95,
                                            columnNumber: 990
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 974
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Contains UTXO store, protocol parameters store, and submit module."
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 1112
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: [
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                                children: "UTXO indexing"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 1231
                                            }, this),
                                            " and ",
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                                children: "transaction submission"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 1298
                                            }, this)
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 1215
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 95,
                                columnNumber: 958
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.tr, {
                                children: [
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                                children: "yaci-store-aggregation-app"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 1451
                                            }, this)
                                        }, void 0, false, {
                                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                            lineNumber: 95,
                                            columnNumber: 1431
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 1415
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: "Handles account balance aggregation; runs as a separate app. See the Aggregation App Getting Started section."
                                    }, void 0, false, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 1556
                                    }, this),
                                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.td, {
                                        children: [
                                            "Calculate ",
                                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                                children: "address balances"
                                            }, void 0, false, {
                                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                                lineNumber: 95,
                                                columnNumber: 1732
                                            }, this),
                                            " separately"
                                        ]
                                    }, void 0, true, {
                                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                        lineNumber: 95,
                                        columnNumber: 1702
                                    }, this)
                                ]
                            }, void 0, true, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 95,
                                columnNumber: 1399
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 95,
                        columnNumber: 571
                    }, this)
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 339
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: [
                            "🧹 The JAR files for these applications are available in the ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                                href: "https://github.com/bloxbean/yaci-store/releases",
                                children: "release section"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 95,
                                columnNumber: 1998
                            }, this),
                            "."
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 95,
                        columnNumber: 1918
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 1888
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2161
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[1].id,
                children: toc[1].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2185
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[2].id,
                children: toc[2].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2253
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "Download the ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "application.properties"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 95,
                        columnNumber: 2353
                    }, this),
                    " file from the release section and place it inside a ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "config"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 95,
                        columnNumber: 2473
                    }, this),
                    " folder in the same directory as your JAR file. The app will pick it up automatically."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2321
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Update the configuration file with your own values. Below are key properties:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2632
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2750
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[3].id,
                children: toc[3].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2774
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h4, {
                id: toc[4].id,
                children: toc[4].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2842
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
                                    children: "store.cardano.host"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 95,
                                    columnNumber: 3048
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=preprod-node.world.dev.cardano.org"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 98,
                                    columnNumber: 47
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 95,
                            columnNumber: 3030
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.port"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 101,
                                    columnNumber: 107
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=30000"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 104,
                                    columnNumber: 47
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 101,
                            columnNumber: 89
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.protocol-magic"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 107,
                                    columnNumber: 78
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=1"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 110,
                                    columnNumber: 57
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 107,
                            columnNumber: 60
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 95,
                    columnNumber: 3012
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 95,
                columnNumber: 2910
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h4, {
                id: toc[5].id,
                children: toc[5].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 113,
                columnNumber: 93
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
                                    children: "spring.datasource.url"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 113,
                                    columnNumber: 299
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "="
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 116,
                                    columnNumber: 50
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 113,
                            columnNumber: 281
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "spring.datasource.username"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 119,
                                    columnNumber: 73
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=user"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 122,
                                    columnNumber: 55
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 119,
                            columnNumber: 55
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "spring.datasource.password"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 125,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "="
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 128,
                                    columnNumber: 55
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 125,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 113,
                    columnNumber: 263
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 113,
                columnNumber: 161
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Additional DB configs:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 131,
                columnNumber: 92
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
                                    children: "spring.datasource.hikari.maximum-pool-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 131,
                                    columnNumber: 293
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=50"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 134,
                                    columnNumber: 71
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 131,
                            columnNumber: 275
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
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 137,
                                    columnNumber: 75
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=5"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 140,
                                    columnNumber: 66
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 137,
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
                                    children: "spring.jpa.properties.hibernate.jdbc.batch_size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 143,
                                    columnNumber: 74
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=100"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 146,
                                    columnNumber: 76
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 143,
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
                                    children: "spring.jpa.properties.hibernate.order_inserts"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 149,
                                    columnNumber: 76
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 152,
                                    columnNumber: 74
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 149,
                            columnNumber: 58
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 131,
                    columnNumber: 257
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 131,
                columnNumber: 155
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 155,
                columnNumber: 96
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[6].id,
                children: toc[6].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 155,
                columnNumber: 120
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
                                    children: "store.executor.enable-parallel-processing"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 155,
                                    columnNumber: 326
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 158,
                                    columnNumber: 70
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 155,
                            columnNumber: 308
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.executor.blocks-batch-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 161,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=200"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 164,
                                    columnNumber: 61
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 161,
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
                                    children: "store.executor.blocks-partition-size"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 167,
                                    columnNumber: 76
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=10"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 170,
                                    columnNumber: 65
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 167,
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
                                    children: "store.executor.use-virtual-thread-for-batch-processing"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 173,
                                    columnNumber: 75
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 176,
                                    columnNumber: 83
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 173,
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
                                    children: "store.executor.use-virtual-thread-for-event-processing"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 179,
                                    columnNumber: 77
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=true"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 182,
                                    columnNumber: 83
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 179,
                            columnNumber: 59
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 155,
                    columnNumber: 290
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 155,
                columnNumber: 188
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.blockquote, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                        children: "⚠️ If parallel processing is enabled, ensure your DB pool settings are scaled accordingly."
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 126
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 185,
                columnNumber: 96
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 185,
                columnNumber: 288
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[7].id,
                children: toc[7].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 185,
                columnNumber: 312
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "These are ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "only required for custom networks"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 409
                    }, this),
                    ". For public networks like ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "mainnet"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 518
                    }, this),
                    ", ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "preprod"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 572
                    }, this),
                    ", ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "preview"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 626
                    }, this),
                    ", or ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "sanchonet"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 185,
                        columnNumber: 683
                    }, this),
                    ", these are already embedded."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 185,
                columnNumber: 380
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
                                    children: "store.cardano.byron-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 185,
                                    columnNumber: 926
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/byron-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 188,
                                    columnNumber: 61
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 185,
                            columnNumber: 908
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.shelley-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 191,
                                    columnNumber: 97
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/shelley-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 194,
                                    columnNumber: 63
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 191,
                            columnNumber: 79
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.alonzo-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 197,
                                    columnNumber: 99
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/alonzo-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 200,
                                    columnNumber: 62
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 197,
                            columnNumber: 81
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.conway-genesis-file"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 203,
                                    columnNumber: 98
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=/path/conway-genesis.json"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 206,
                                    columnNumber: 62
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 203,
                            columnNumber: 80
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 185,
                    columnNumber: 890
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 185,
                columnNumber: 788
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 209,
                columnNumber: 117
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[8].id,
                children: toc[8].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 209,
                columnNumber: 141
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Used for transaction submission and protocol parameter fetching:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 209,
                columnNumber: 209
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
                                children: "#store.cardano.n2c-node-socket-path=/path/to/node.socket"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 209,
                                columnNumber: 452
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 209,
                            columnNumber: 434
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: " "
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 212,
                            columnNumber: 110
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# Optional: using relay"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 212,
                                columnNumber: 176
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 212,
                            columnNumber: 158
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.n2c-host=<relay_host>"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 215,
                                columnNumber: 95
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 215,
                            columnNumber: 77
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.n2c-port=<relay_port>"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 218,
                                columnNumber: 108
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 218,
                            columnNumber: 90
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 209,
                    columnNumber: 416
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 209,
                columnNumber: 314
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 221,
                columnNumber: 127
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[9].id,
                children: toc[9].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 221,
                columnNumber: 151
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "You can selectively enable/disable stores in ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                        children: "yaci-store-all"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 221,
                        columnNumber: 283
                    }, this),
                    ". Example:"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 221,
                columnNumber: 219
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
                                    children: "store.utxo.enabled"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 221,
                                    columnNumber: 512
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=false"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 224,
                                    columnNumber: 47
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 221,
                            columnNumber: 494
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "store.<store_name>."
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 227,
                                    columnNumber: 78
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "enabled"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 230,
                                    columnNumber: 48
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=false"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 233,
                                    columnNumber: 36
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 227,
                            columnNumber: 60
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 221,
                    columnNumber: 476
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 221,
                columnNumber: 374
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 236,
                columnNumber: 97
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h3, {
                id: toc[10].id,
                children: toc[10].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 236,
                columnNumber: 121
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
                                    children: "store.cardano.sync-start-slot"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 236,
                                    columnNumber: 329
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=2738868"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 239,
                                    columnNumber: 58
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 236,
                            columnNumber: 311
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#D73A49",
                                        "--shiki-dark": "#F97583"
                                    },
                                    children: "store.cardano.sync-start-blockhash"
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 242,
                                    columnNumber: 80
                                }, this),
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                    style: {
                                        "--shiki-light": "#24292E",
                                        "--shiki-dark": "#E1E4E8"
                                    },
                                    children: "=a5c72a0e..."
                                }, void 0, false, {
                                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                    lineNumber: 245,
                                    columnNumber: 63
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 242,
                            columnNumber: 62
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# For Byron start"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 248,
                                columnNumber: 84
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 248,
                            columnNumber: 66
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.sync-start-byron-block-number=2737340"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 251,
                                columnNumber: 89
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 251,
                            columnNumber: 71
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "# Optional stop point"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 254,
                                columnNumber: 124
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 254,
                            columnNumber: 106
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.sync-stop-slot=76667163"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 257,
                                columnNumber: 93
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 257,
                            columnNumber: 75
                        }, this),
                        "\n",
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#6A737D",
                                    "--shiki-dark": "#6A737D"
                                },
                                children: "#store.cardano.sync-stop-blockhash=3e9a93a..."
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 260,
                                columnNumber: 110
                            }, this)
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 260,
                            columnNumber: 92
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 236,
                    columnNumber: 293
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 236,
                columnNumber: 191
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 263,
                columnNumber: 136
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[11].id,
                children: toc[11].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 263,
                columnNumber: 160
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "To run Yaci Store apps:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 263,
                columnNumber: 230
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 263,
                                columnNumber: 426
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 266,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-all-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 269,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 272,
                                columnNumber: 45
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 275,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 278,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 281,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 284,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 263,
                        columnNumber: 408
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 263,
                    columnNumber: 390
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 263,
                columnNumber: 294
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
                                children: "java"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 287,
                                columnNumber: 227
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#005CC5",
                                    "--shiki-dark": "#79B8FF"
                                },
                                children: " -jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 290,
                                columnNumber: 33
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: " yaci-store-utxo-indexer-"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 293,
                                columnNumber: 34
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: "<"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 296,
                                columnNumber: 54
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: "versio"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 299,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#24292E",
                                    "--shiki-dark": "#E1E4E8"
                                },
                                children: "n"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 302,
                                columnNumber: 35
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#D73A49",
                                    "--shiki-dark": "#F97583"
                                },
                                children: ">"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 305,
                                columnNumber: 30
                            }, this),
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                                style: {
                                    "--shiki-light": "#032F62",
                                    "--shiki-dark": "#9ECBFF"
                                },
                                children: ".jar"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 308,
                                columnNumber: 30
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 287,
                        columnNumber: 209
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 287,
                    columnNumber: 191
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 287,
                columnNumber: 95
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 95
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[12].id,
                children: toc[12].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 119
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: "Access Swagger UI at:"
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 189
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.pre, {
                tabIndex: "0",
                "data-language": "plaintext",
                "data-word-wrap": "",
                "data-pagefind-ignore": "all",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.span, {
                            children: "http://localhost:8080/swagger-ui/index.html"
                        }, void 0, false, {
                            fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                            lineNumber: 311,
                            columnNumber: 388
                        }, this)
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 311,
                        columnNumber: 370
                    }, this)
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 311,
                    columnNumber: 352
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 251
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 534
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.h2, {
                id: toc[13].id,
                children: toc[13].value
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 558
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "To speed up syncing, optional indexes are ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                        children: "dropped during initial sync"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 311,
                        columnNumber: 689
                    }, this),
                    ". Re-apply them after sync completion:"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 628
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.ul, {
                children: [
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "Yaci Store All"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 311,
                                columnNumber: 863
                            }, this),
                            " → ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "create-index.sql"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 311,
                                columnNumber: 929
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 311,
                        columnNumber: 847
                    }, this),
                    "\n",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.li, {
                        children: [
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.strong, {
                                children: "UTxO Indexer"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 311,
                                columnNumber: 1025
                            }, this),
                            " → ",
                            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.code, {
                                children: "create-index.sql"
                            }, void 0, false, {
                                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                                lineNumber: 311,
                                columnNumber: 1089
                            }, this)
                        ]
                    }, void 0, true, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 311,
                        columnNumber: 1009
                    }, this),
                    "\n"
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 825
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: [
                    "You’ll find these scripts in the ",
                    /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.a, {
                        href: "https://github.com/bloxbean/yaci-store",
                        children: "GitHub repository"
                    }, void 0, false, {
                        fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                        lineNumber: 311,
                        columnNumber: 1244
                    }, this),
                    "."
                ]
            }, void 0, true, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 1192
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.hr, {}, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 1369
            }, this),
            "\n",
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.p, {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(_components.em, {
                    children: "Last updated on: June 23, 2025"
                }, void 0, false, {
                    fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                    lineNumber: 311,
                    columnNumber: 1408
                }, this)
            }, void 0, false, {
                fileName: "[project]/content/v2.0.0/usage/outOfTheBoxIndexer.mdx.tsx",
                lineNumber: 311,
                columnNumber: 1393
            }, this)
        ]
    }, void 0, true);
}
const __TURBOPACK__default__export__ = _createMdxContent;
}}),

};

//# sourceMappingURL=content_v2_0_0_usage_4200c28a._.js.map