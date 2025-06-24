module.exports = {

"[project]/.next-internal/server/app/[[...mdxPath]]/page/actions.js [app-rsc] (server actions loader, ecmascript)": (function(__turbopack_context__) {

var { g: global, __dirname, m: module, e: exports } = __turbopack_context__;
{
}}),
"[project]/app/layout.jsx [app-rsc] (ecmascript, Next.js server component)": ((__turbopack_context__) => {

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.n(__turbopack_context__.i("[project]/app/layout.jsx [app-rsc] (ecmascript)"));
}}),
"[project]/app/[[...mdxPath]]/page.jsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>Page),
    "generateMetadata": (()=>generateMetadata),
    "generateStaticParams": (()=>generateStaticParams)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-jsx-dev-runtime.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2f$dist$2f$client$2f$pages$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/nextra/dist/client/pages.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/mdx-components.js [app-rsc] (ecmascript)");
;
;
;
const generateStaticParams = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2f$dist$2f$client$2f$pages$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["generateStaticParamsFor"])('mdxPath');
async function generateMetadata(props) {
    const params = await props.params;
    const { metadata } = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2f$dist$2f$client$2f$pages$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["importPage"])(params.mdxPath);
    return metadata;
}
const Wrapper = (0, __TURBOPACK__imported__module__$5b$project$5d2f$mdx$2d$components$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["useMDXComponents"])().wrapper;
async function Page(props) {
    const params = await props.params;
    const result = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2f$dist$2f$client$2f$pages$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["importPage"])(params.mdxPath);
    const { default: MDXContent, toc, metadata } = result;
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(Wrapper, {
        toc: toc,
        metadata: metadata,
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(MDXContent, {
            ...props,
            params: params
        }, void 0, false, {
            fileName: "[project]/app/[[...mdxPath]]/page.jsx",
            lineNumber: 20,
            columnNumber: 7
        }, this)
    }, void 0, false, {
        fileName: "[project]/app/[[...mdxPath]]/page.jsx",
        lineNumber: 19,
        columnNumber: 5
    }, this);
}
}}),
"[project]/app/[[...mdxPath]]/page.jsx [app-rsc] (ecmascript, Next.js server component)": ((__turbopack_context__) => {

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.n(__turbopack_context__.i("[project]/app/[[...mdxPath]]/page.jsx [app-rsc] (ecmascript)"));
}}),
"[project]/mdx-components.js [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "useMDXComponents": (()=>useMDXComponents)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2d$theme$2d$docs$2f$dist$2f$mdx$2d$components$2f$index$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/nextra-theme-docs/dist/mdx-components/index.js [app-rsc] (ecmascript)"); // nextra-theme-blog or your custom theme
;
// Get the default MDX components
const themeComponents = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2d$theme$2d$docs$2f$dist$2f$mdx$2d$components$2f$index$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["useMDXComponents"])();
function useMDXComponents(components) {
    return {
        ...themeComponents,
        ...components
    };
}
}}),

};

//# sourceMappingURL=_2facebde._.js.map