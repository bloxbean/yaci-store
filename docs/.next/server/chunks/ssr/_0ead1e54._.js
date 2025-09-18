module.exports = {

"[project]/components/VersionFilteredLayout.jsx (client reference/proxy) <module evaluation>": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$server$2d$dom$2d$turbopack$2d$server$2d$edge$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-server-dom-turbopack-server-edge.js [app-rsc] (ecmascript)");
;
const __TURBOPACK__default__export__ = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$server$2d$dom$2d$turbopack$2d$server$2d$edge$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["registerClientReference"])(function() {
    throw new Error("Attempted to call the default export of [project]/components/VersionFilteredLayout.jsx <module evaluation> from the server, but it's on the client. It's not possible to invoke a client function from the server, it can only be rendered as a Component or passed to props of a Client Component.");
}, "[project]/components/VersionFilteredLayout.jsx <module evaluation>", "default");
}}),
"[project]/components/VersionFilteredLayout.jsx (client reference/proxy)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>__TURBOPACK__default__export__)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$server$2d$dom$2d$turbopack$2d$server$2d$edge$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-server-dom-turbopack-server-edge.js [app-rsc] (ecmascript)");
;
const __TURBOPACK__default__export__ = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$server$2d$dom$2d$turbopack$2d$server$2d$edge$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["registerClientReference"])(function() {
    throw new Error("Attempted to call the default export of [project]/components/VersionFilteredLayout.jsx from the server, but it's on the client. It's not possible to invoke a client function from the server, it can only be rendered as a Component or passed to props of a Client Component.");
}, "[project]/components/VersionFilteredLayout.jsx", "default");
}}),
"[project]/components/VersionFilteredLayout.jsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
var __TURBOPACK__imported__module__$5b$project$5d2f$components$2f$VersionFilteredLayout$2e$jsx__$28$client__reference$2f$proxy$29$__$3c$module__evaluation$3e$__ = __turbopack_context__.i("[project]/components/VersionFilteredLayout.jsx (client reference/proxy) <module evaluation>");
var __TURBOPACK__imported__module__$5b$project$5d2f$components$2f$VersionFilteredLayout$2e$jsx__$28$client__reference$2f$proxy$29$__ = __turbopack_context__.i("[project]/components/VersionFilteredLayout.jsx (client reference/proxy)");
;
__turbopack_context__.n(__TURBOPACK__imported__module__$5b$project$5d2f$components$2f$VersionFilteredLayout$2e$jsx__$28$client__reference$2f$proxy$29$__);
}}),
"[project]/app/docs/layout.jsx [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "default": (()=>DocsLayout)
});
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/server/route-modules/app-page/vendored/rsc/react-jsx-dev-runtime.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2f$dist$2f$server$2f$page$2d$map$2f$get$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/nextra/dist/server/page-map/get.js [app-rsc] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$components$2f$VersionFilteredLayout$2e$jsx__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/components/VersionFilteredLayout.jsx [app-rsc] (ecmascript)");
;
;
;
function transformPageMap(pageMap) {
    const result = [];
    // Process both versions and flatten them
    [
        'v0.1.x',
        'v2.0.0'
    ].forEach((version)=>{
        const versionEntry = pageMap.find((item)=>item.name === version);
        if (versionEntry && versionEntry.children) {
            // Transform routes recursively
            function transformRoutes(item, basePath) {
                const newRoute = `${basePath}/${item.name}`;
                return {
                    ...item,
                    route: newRoute,
                    children: item.children ? item.children.map((child)=>transformRoutes(child, newRoute)) : undefined
                };
            }
            // Get children, filter out index, and transform their routes
            const transformedChildren = versionEntry.children.filter((child)=>child.name !== 'index').map((child)=>transformRoutes(child, `/docs/${version}`));
            result.push(...transformedChildren);
        }
    });
    return result;
}
async function DocsLayout({ children }) {
    const originalPageMap = await (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$nextra$2f$dist$2f$server$2f$page$2d$map$2f$get$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["getPageMap"])();
    const transformedPageMap = transformPageMap(originalPageMap);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$server$2f$route$2d$modules$2f$app$2d$page$2f$vendored$2f$rsc$2f$react$2d$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$components$2f$VersionFilteredLayout$2e$jsx__$5b$app$2d$rsc$5d$__$28$ecmascript$29$__["default"], {
        allPageMap: transformedPageMap,
        children: children
    }, void 0, false, {
        fileName: "[project]/app/docs/layout.jsx",
        lineNumber: 40,
        columnNumber: 5
    }, this);
}
}}),
"[project]/node_modules/nextra/dist/server/page-map/get.js [app-rsc] (ecmascript)": ((__turbopack_context__) => {
"use strict";

var { g: global, __dirname } = __turbopack_context__;
{
__turbopack_context__.s({
    "getPageMap": (()=>getPageMap),
    "getRouteToFilepath": (()=>getRouteToFilepath)
});
function importPageMap(lang = "") {
    return ({
        "": ()=>__turbopack_context__.r("[project]/node_modules/nextra/dist/server/page-map/placeholder.js?lang= [app-rsc] (ecmascript, async loader)")(__turbopack_context__.i)
    })[lang]();
}
const defaultLocale = process.env.NEXTRA_DEFAULT_LOCALE;
async function getPageMap(route = "/") {
    const segments = route.split("/");
    const lang = segments.splice(0, defaultLocale ? 2 : 1).at(-1);
    let { pageMap } = await importPageMap(lang);
    let segment;
    while(segment = segments.shift()){
        const folder = pageMap.find((item)=>"name" in item && item.name === segment);
        if (!folder) {
            throw new Error(`Can't find pageMap for "${segment}" in route "${route}"`);
        }
        pageMap = folder.children;
    }
    return pageMap;
}
async function getRouteToFilepath(lang) {
    const { RouteToFilepath } = await importPageMap(lang);
    return RouteToFilepath;
}
;
}}),

};

//# sourceMappingURL=_0ead1e54._.js.map