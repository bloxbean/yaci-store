# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Nextra 4 documentation site for Yaci Store, a Cardano blockchain data indexing and storage solution. The project features:

- **Landing Page**: Clean, professional landing page at `/` with project overview and links to documentation
- **Versioned Documentation**: Documentation organized by versions (v0.1.0 - v0.1.4, v2.0.0) accessible via `/docs/v{version}/`
- **Nextra 4**: Uses the latest Nextra framework with App Router support for documentation generation

## Architecture

### Routing Structure

- `/` - Landing page (client component with styled-jsx)
- `/docs` - Redirects to latest version (`/docs/v2.0.0`)
- `/docs/v2.0.0/*` - Version 2.0.0 documentation
- `/docs/v0.1.x/*` - Version 0.1.0 - 0.1.4 documentation

### Key Directories

- `app/` - Next.js App Router pages
  - `page.jsx` - Landing page (client component)
  - `docs/[[...slug]]/page.jsx` - Catch-all route for documentation
- `content/docs/` - MDX documentation content
  - `v2.0.0/` - Latest version documentation
  - `v0.1.x/` - Previous version documentation (v0.1.0 - v0.1.4)
- `components/` - React components
  - `NavbarWithThemeLogo/` - Custom navbar with version selector
  - `VersionSelector/` - Version switching component
- `utils/` - Utility functions and constants

### Content Structure

Documentation content is organized in `content/docs/{version}/` with:

- `index.mdx` - Version homepage
- `_meta.ts` - Navigation configuration
- Folders for different sections (gettingStarted, apis, usage, etc.)

## Development Commands

```bash
# Start development server
npm run dev

# Build for production
npm run build

# Start production server
npm run start

# Format code
npm run prettier
```

## Common Tasks

### Adding New Documentation

1. Create MDX files in `content/docs/{version}/`
2. Update `_meta.ts` files to include new pages in navigation
3. Follow existing folder structure and naming conventions

### Creating New Version

1. Copy existing version folder in `content/docs/`
2. Update version references in content
3. Update `utils/versions.js` with new version info
4. Update redirects in `next.config.mjs` if needed

### Modifying Landing Page

- Edit `app/page.jsx` (client component)
- Styles are included via styled-jsx
- Keep it professional and focused on driving users to documentation

## Important Notes

### Nextra 4 Specifics

- Uses `generateStaticParamsFor('content/docs')` for route generation
- Content must be in `content/docs/` to match `/docs/*` routes
- Theme configuration in `theme.config.tsx`

### Version Management

- Version selector reads from `utils/versions.js`
- Path manipulation handled by `utils/versions.js` functions
- Current version extracted from URL path

### Routing

- Landing page is separate from documentation
- All docs routes handled by catch-all `[[...slug]]` pattern
- Redirects configured in `next.config.mjs` for backward compatibility

## Testing

Always test these key scenarios:

1. Landing page loads and renders correctly
2. `/docs` redirects to latest version
3. Version selector switches between versions correctly
4. All documentation pages render with proper navigation
5. Build process completes without errors

## Troubleshooting

### Common Issues

- **pathSegments.join error**: Usually means content structure doesn't match route structure
- **Layout issues**: Check if components are client/server components correctly
- **Version switching**: Verify `utils/versions.js` functions and URL patterns
- **Build failures**: Check for missing meta files or content structure issues
