# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yaci Store documentation site built with **Nextra 4.0.14** and **Next.js 15.1.6** using the App Router architecture. The site supports dual versioning (v1 legacy, v2 current) with intelligent version switching and navigation filtering.

## Development Commands

```bash
npm install              # Install dependencies
npm run dev              # Start dev server (http://localhost:3000)
npm run build            # Production build
npm start                # Start production server
```

## Architecture

### Version Management System

The documentation uses a sophisticated dual-version system:

1. **VersionFilteredLayout** (`components/VersionFilteredLayout.tsx`)
   - Detects current version from URL path (`/docs/v1/*` or `/docs/v2/*`)
   - Filters the Nextra pageMap to show only the current version's content
   - Recursively removes pages with `navigation: false` frontmatter
   - Removes items named 'page' at root level to prevent duplicate entries
   - Injects custom navbar with version selector

2. **ModernVersionSelector** (`components/ModernVersionSelector.tsx`)
   - Dropdown component for switching between v1 and v2
   - Path preservation: Attempts to maintain the same page path when switching versions
   - V2-only section handling: Sections like `/plugins` and `/ledger-state-mismatches` don't exist in v1
   - When switching from V2-only sections to v1, automatically redirects to `/docs/v1/yaci-store/overview`

3. **Version Directory Structure**
   - Each version has its own directory: `app/docs/v1/` and `app/docs/v2/`
   - Navigation defined by `_meta.js` files in each directory
   - V2-only sections: Plugin Framework, Known Issues (Ledger State Mismatches)

### Navigation System

Navigation is controlled by `_meta.js` files:

```javascript
export default {
  "overview": "Overview",           // key = folder/file name (no extension)
  "installation": "Installation",   // value = sidebar display name
  "configuration": "Configuration"  // order = sidebar order
}
```

**Important**: Keys must match folder/file names exactly (without `.mdx` extension). Order in the object determines sidebar order.

### Redirect Pages

Version root pages (`/docs/v1` and `/docs/v2`) use **TypeScript redirect components** (`.tsx` files, not `.mdx`):

```tsx
'use client'
import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function V1Index() {
  const router = useRouter()
  useEffect(() => {
    router.replace('/docs/v1/yaci-store/overview')
  }, [router])
  return null
}
```

**Critical**: Must use `.tsx` not `.mdx`. MDX files cause `useRouter is not a function` errors in the Nextra compilation process.

### Custom 404 Handling

`app/docs/not-found.tsx` detects the current version and redirects:
- `/docs/v1/*` (404) → `/docs/v1/yaci-store/overview`
- `/docs/v2/*` (404) → `/docs/v2/yaci-store/overview`
- Otherwise → `/docs/v2/yaci-store/overview`

### Sidebar Configuration

The sidebar title is hidden via `VersionFilteredLayout.tsx`:

```tsx
sidebar={{
  toggleButton: true,
  titleComponent: () => null  // Hides "Index" title
}}
```

## Content Guidelines

### Creating Documentation Pages

1. Create `.mdx` file in appropriate version directory: `app/docs/v{1,2}/section/page.mdx`
2. Add entry to `_meta.js` in the same directory
3. Use MDX with optional frontmatter:

```mdx
---
title: Page Title
description: SEO description
---

# Page Content
```

### Hiding Pages from Sidebar

```mdx
---
navigation: false
---

# This page won't appear in the sidebar
```

### Using Components

```mdx
import { Callout } from 'nextra/components'

<Callout type="warning">
⚠️ **Important Notice**

This is a warning message.
</Callout>
```

Available Callout types: `warning`, `info`, `error`, `default`

Other components: `Tabs`, `Tab`, `Steps`, `FileTree` from `nextra/components`

### File Naming

- Use kebab-case: `my-page-name.mdx`
- Use `page.mdx` for section index pages
- Avoid special characters (only letters, numbers, hyphens)

## Key Technical Details

### Next.js App Router

This project uses Next.js 15's App Router, not Pages Router:
- Use `'use client'` directive for client components
- Import from `next/navigation` (not `next/router`)
- Use `useRouter()`, `usePathname()` from `next/navigation`

### MDX Compilation

- Nextra compiles MDX files on-demand in development
- First page visit is slower; subsequent visits are cached
- Production builds pre-compile all MDX files

### Version-Specific Redirects

When adding V2-only sections, update `ModernVersionSelector.tsx`:

```typescript
const v2OnlySections = ['/plugins', '/ledger-state-mismatches'];
```

## Common Patterns

### Adding a New V2-Only Section

1. Create directory: `app/docs/v2/new-section/`
2. Add `_meta.js`: `export default { "overview": "Overview" }`
3. Create content: `app/docs/v2/new-section/overview/page.mdx`
4. Update v2 root `_meta.js`: `"new-section": "New Section"`
5. Add to `v2OnlySections` in `ModernVersionSelector.tsx`

### Synchronizing Content Between Versions

When content applies to both v1 and v2:
1. Update both `app/docs/v1/` and `app/docs/v2/`
2. Keep `_meta.js` entries consistent
3. Use Callout components to mark version-specific notes

## Repository Information

- GitHub: https://github.com/bloxbean/yaci-store
- License: MIT
- Main documentation: See README.md for user-facing instructions
