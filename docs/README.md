# Yaci Store Documentation

This is the official documentation site for Yaci Store, built with [Nextra 4](https://nextra.site/) and [Next.js 15](https://nextjs.org/).

## Tech Stack

- **Nextra 4.0.14** - Documentation framework
- **Next.js 15.1.6** - React framework with App Router
- **React 19** - UI library
- **nextra-theme-docs** - Documentation theme

## Getting Started

### Prerequisites

- Node.js 18+
- npm or pnpm

### Installation

```bash
npm install
```

### Development

Start the development server:

```bash
npm run dev
```

The site will be available at `http://localhost:3000`

### Build

Build for production:

```bash
npm run build
```

Start production server:

```bash
npm start
```

## Project Structure

```
docs/
├── app/                          # Next.js App Router directory
│   ├── docs/                     # Documentation content
│   │   ├── v1/                   # Version 1 documentation
│   │   │   ├── _meta.js          # V1 navigation configuration
│   │   │   ├── yaci-store/       # Yaci Store section
│   │   │   ├── getting-started/  # Getting Started section
│   │   │   ├── usage/            # Usage section
│   │   │   ├── stores/           # Stores section
│   │   │   ├── api-reference/    # API Reference section
│   │   │   ├── advanced-configuration/
│   │   │   ├── tutorials/        # Tutorials section
│   │   │   ├── upgrade-guide/    # Upgrade Guide
│   │   │   └── showcase/         # Showcase section
│   │   │
│   │   ├── v2/                   # Version 2 documentation
│   │   │   ├── _meta.js          # V2 navigation configuration
│   │   │   ├── yaci-store/       # Yaci Store section
│   │   │   ├── getting-started/  # Getting Started section
│   │   │   ├── usage/            # Usage section
│   │   │   ├── stores/           # Stores section
│   │   │   ├── api-reference/    # API Reference section
│   │   │   ├── plugins/          # Plugin Framework (V2 only)
│   │   │   ├── advanced-configuration/
│   │   │   ├── tutorials/        # Tutorials section
│   │   │   ├── ledger-state-mismatches/ # Known Issues
│   │   │   └── showcase/         # Showcase section
│   │   │
│   │   ├── _meta.js              # Root docs navigation
│   │   ├── layout.tsx            # Docs layout with version filtering
│   │   └── not-found.tsx         # Custom 404 handler
│   │
│   ├── page.jsx                  # Landing page
│   ├── layout.tsx                # Root layout
│   └── globals.css               # Global styles
│
├── components/                   # React components
│   ├── VersionFilteredLayout.tsx # Version filtering logic
│   └── ModernVersionSelector.tsx # Version dropdown selector
│
├── public/                       # Static assets
└── theme.config.tsx             # Nextra theme configuration
```

## Documentation Versioning

The documentation supports two versions: **v1** (legacy) and **v2** (current).

### Version Structure

- Each version has its own directory under `app/docs/`
- Versions are filtered using the `VersionFilteredLayout` component
- The version selector allows users to switch between versions
- When switching versions, the system tries to preserve the current path
- If a page doesn't exist in the target version, users are redirected to the overview page

### V2-Only Sections

Some sections only exist in V2:
- Plugin Framework (`/plugins`)
- Known Issues (`/ledger-state-mismatches`)

When switching to V1 from these sections, users are automatically redirected to the V1 overview page.

## Adding/Editing Content

### Creating a New Page

1. Navigate to the appropriate section directory (e.g., `app/docs/v2/tutorials/`)
2. Create a new `.mdx` file (e.g., `my-tutorial.mdx`)
3. Add content using MDX format:

```mdx
# My Tutorial

This is a tutorial about...

## Step 1

First, do this...
```

### Using Components

Nextra provides several built-in components:

#### Callout Component

```mdx
import { Callout } from 'nextra/components'

<Callout type="warning">
⚠️ **Important Notice**

This is an important warning message.
</Callout>
```

Available types:
- `warning` - Yellow warning box
- `info` - Blue info box
- `error` - Red error box
- `default` - Gray default box

#### Other Components

```mdx
import { Tabs, Tab } from 'nextra/components'
import { Steps } from 'nextra/components'
import { FileTree } from 'nextra/components'
```

See [Nextra Components Documentation](https://nextra.site/docs/guide/built-in-components) for more details.

### Navigation Configuration

Each directory can have a `_meta.js` file to configure navigation:

```javascript
export default {
  "overview": "Overview",
  "installation": "Installation",
  "configuration": "Configuration",
  "tutorials": "Tutorials & Use Cases"
}
```

- Keys match the folder/file names (without extension)
- Values are the display names in the sidebar
- Order in the object determines the sidebar order

### Hiding Pages from Navigation

To hide a page from the sidebar but keep it accessible:

```mdx
---
navigation: false
---

# Hidden Page

This page won't appear in the sidebar.
```

### Page Metadata

Add frontmatter at the top of MDX files:

```mdx
---
title: My Page Title
description: Page description for SEO
---

# Content starts here
```

## Version Selector

The version selector is implemented in `components/ModernVersionSelector.tsx`. It:

1. Detects the current version from the URL
2. Allows switching between v1 and v2
3. Preserves the current path when switching (if the page exists)
4. Redirects to overview for V2-only sections when switching to V1

## Custom 404 Handling

The `app/docs/not-found.tsx` file handles 404 errors:

- Detects which version the user is in
- Redirects to the appropriate version's overview page
- Shows a "Redirecting..." message during the redirect

## Styling

- Global styles are in `app/globals.css`
- The landing page has custom styles in `app/page.jsx`
- Nextra theme configuration is in `theme.config.tsx`

## Best Practices

### Writing Documentation

1. **Use clear, concise headings** - Use `##` for main sections, `###` for subsections
2. **Add code examples** - Include working code snippets with syntax highlighting
3. **Use callouts for important information** - Warnings, notes, tips
4. **Include tables for structured data** - Use Markdown tables for comparisons
5. **Link to related pages** - Help users navigate related content

### Navigation Structure

1. **Keep it simple** - Don't nest more than 3 levels deep
2. **Use consistent naming** - Follow existing naming conventions
3. **Order matters** - Put most important/common pages first
4. **Group related content** - Keep related topics together

### File Naming

1. **Use kebab-case** - `my-page-name.mdx`
2. **Be descriptive** - Name should indicate content
3. **Avoid special characters** - Stick to letters, numbers, and hyphens
4. **Use `page.mdx` for index pages** - Main page of a section

### Version Management

1. **Keep versions in sync** - Update both versions when content applies to both
2. **Mark legacy content** - Use callouts to indicate deprecated features
3. **Update upgrade guide** - Document breaking changes between versions
4. **Test version switching** - Ensure links work across versions

## Troubleshooting

### Page not appearing in sidebar

- Check that the file name matches the key in `_meta.js`
- Ensure `navigation: false` is not set in frontmatter
- Verify the file is in the correct directory

### Version switching not working

- Check that the path exists in both versions
- Verify `ModernVersionSelector.tsx` has correct version detection
- Check for V2-only sections in the redirect logic

### Build errors

- Ensure all MDX files have valid syntax
- Check that imported components exist
- Verify all internal links point to existing pages

### Styles not applying

- Check `globals.css` for conflicts
- Verify Tailwind classes are being used correctly
- Clear `.next` cache and rebuild

## Deployment

The site can be deployed to any Next.js hosting platform:

- Vercel (recommended)
- Netlify
- AWS Amplify
- Self-hosted with Node.js

Build command: `npm run build`
Start command: `npm start`

## Contributing

When contributing to the documentation:

1. Create a new branch for your changes
2. Test locally with `npm run dev`
3. Build to check for errors: `npm run build`
4. Create a pull request with a clear description
5. Ensure all links work and navigation is correct

## Resources

- [Nextra Documentation](https://nextra.site/)
- [Next.js Documentation](https://nextjs.org/docs)
- [MDX Documentation](https://mdxjs.com/)
- [React Documentation](https://react.dev/)
