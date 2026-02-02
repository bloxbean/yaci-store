# Yaci Store Documentation

Documentation site for [Yaci Store](https://github.com/bloxbean/yaci-store), built with [Nextra 4](https://nextra.site/) and [Next.js](https://nextjs.org/).

## Quick Start

### Prerequisites

- Node.js 18+
- npm, yarn, or pnpm

### Run the Doc Site Locally

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The site will be available at **http://localhost:3000**

### Build for Production

```bash
npm run build
npx serve@latest out
```

---

## Where Are the Documentation Pages?

All documentation content lives in the `app/docs/` directory, organized by version:

```
app/docs/
├── v1/                    # Version 1 documentation (legacy)
│   ├── _meta.js           # Sidebar navigation config
│   ├── yaci-store/        # Yaci Store overview
│   ├── getting-started/   # Installation & setup guides
│   ├── usage/             # Usage guides
│   ├── stores/            # Store modules docs
│   ├── api-reference/     # API documentation
│   └── ...
│
├── v2/                    # Version 2 documentation (current)
│   ├── _meta.js           # Sidebar navigation config
│   ├── yaci-store/        # Yaci Store overview
│   ├── getting-started/   # Installation & setup guides
│   ├── usage/             # Usage guides
│   ├── stores/            # Store modules docs
│   ├── api-reference/     # API documentation
│   ├── plugins/           # Plugin framework (v2 only)
│   └── ...
│
└── layout.tsx             # Docs layout with version filtering
```

**Key Point:** Each documentation page is a `page.mdx` file inside a folder. For example:
- `app/docs/v2/getting-started/installation/docker/page.mdx` -> `/docs/v2/getting-started/installation/docker`

---

## Contributing

### How to Add or Edit Documentation

1. **Fork and clone** the repository
2. **Navigate to the docs folder:**
   ```bash
   cd yaci-store/docs
   ```
3. **Install dependencies and start the dev server:**
   ```bash
   npm install
   npm run dev
   ```
4. **Make your changes** - Edit existing `.mdx` files or create new ones
5. **Preview** your changes at http://localhost:3000
6. **Create a Pull Request**

### Creating a New Page

1. Create a folder under the appropriate section (e.g., `app/docs/v2/tutorials/my-tutorial/`)
2. Add a `page.mdx` file inside the folder:

```mdx
# My Tutorial Title

Introduction paragraph here...

## Section 1

Content for section 1...

## Section 2

Content for section 2...
```

3. Update the `_meta.js` file in the parent directory to add navigation:

```javascript
// app/docs/v2/tutorials/_meta.js
export default {
  "existing-tutorial": "Existing Tutorial",
  "my-tutorial": "My Tutorial Title"  // Add your new page
}
```

### File Naming Conventions

- Use **kebab-case** for folder names: `my-page-name/`
- Each page must have a `page.mdx` file inside its folder
- Use descriptive names that indicate the content

### Using Components in MDX

Nextra provides built-in components you can use:

```mdx
import { Callout, Tabs, Steps, FileTree } from 'nextra/components'

<Callout type="warning">
This is a warning message.
</Callout>

<Callout type="info">
This is an info message.
</Callout>
```

### Sidebar Navigation (`_meta.js`)

Each directory can have a `_meta.js` file to control the sidebar order and display names:

```javascript
export default {
  "overview": "Overview",           // Key = folder name, Value = display name
  "installation": "Installation",
  "configuration": "Configuration"
}
```

The order in this file determines the sidebar order.

---

## Project Structure Overview

```
docs/
├── app/
│   ├── docs/              # Documentation pages (MDX files)
│   │   ├── v1/            # Version 1 docs
│   │   └── v2/            # Version 2 docs
│   ├── page.jsx           # Landing page
│   ├── layout.tsx         # Root layout
│   └── globals.css        # Global styles
│
├── components/            # React components
│   ├── VersionFilteredLayout.tsx
│   └── ModernVersionSelector.tsx
│
├── public/                # Static assets (images, etc.)
├── theme.config.tsx       # Nextra theme configuration
└── package.json           # Project dependencies
```

---

## Troubleshooting

### Page not appearing in sidebar
- Ensure the folder name matches the key in `_meta.js`
- Verify there's a `page.mdx` inside the folder
- Check for typos in the `_meta.js` file

### Build errors
- Clear the cache: `rm -rf .next` and rebuild
- Check MDX syntax is valid
- Ensure all imported components exist

### Styles not working
- Clear `.next` cache and restart dev server
- Check `globals.css` for conflicts

---

## Useful Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm start` | Start production server |

---

## Resources

- [Nextra Documentation](https://nextra.site/)
- [Next.js Documentation](https://nextjs.org/docs)
- [MDX Documentation](https://mdxjs.com/)
- [Yaci Store Main Repository](https://github.com/bloxbean/yaci-store)
