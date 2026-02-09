#!/usr/bin/env node
/**
 * Post-build script to generate static redirect HTML files
 * Run after `next build` to create redirect pages in the `out/` folder
 */

import { simpleRedirects } from '../redirects.config.js';
import { existsSync, mkdirSync, writeFileSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const outDir = join(__dirname, '..', 'out');

/**
 * Generate HTML content for a redirect page
 */
function generateRedirectHtml(destination) {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="refresh" content="0;url=${destination}">
  <link rel="canonical" href="${destination}">
  <title>Redirecting...</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      margin: 0;
      background: #f5f5f5;
    }
    .container {
      text-align: center;
      padding: 2rem;
    }
    a {
      color: #0070f3;
      text-decoration: none;
    }
    a:hover {
      text-decoration: underline;
    }
  </style>
</head>
<body>
  <div class="container">
    <p>Redirecting to <a href="${destination}">${destination}</a>...</p>
  </div>
  <script>window.location.href = "${destination}";</script>
</body>
</html>`;
}

/**
 * Create a redirect HTML file at the specified path
 */
function createRedirectFile(source, destination) {
  // Remove leading slash and add index.html
  const relativePath = source.replace(/^\//, '');
  const filePath = join(outDir, relativePath, 'index.html');
  const dirPath = dirname(filePath);

  // Create directory if it doesn't exist
  if (!existsSync(dirPath)) {
    mkdirSync(dirPath, { recursive: true });
  }

  // Skip if the file already exists (don't overwrite actual content)
  if (existsSync(filePath)) {
    console.log(`  Skipping ${source} (file already exists)`);
    return false;
  }

  // Write the redirect HTML
  const html = generateRedirectHtml(destination);
  writeFileSync(filePath, html, 'utf-8');
  return true;
}

/**
 * Main function
 */
function main() {
  console.log('Generating redirect HTML files...\n');

  if (!existsSync(outDir)) {
    console.error(`Error: Output directory not found: ${outDir}`);
    console.error('Make sure to run "next build" before this script.');
    process.exit(1);
  }

  let created = 0;
  let skipped = 0;

  for (const { source, destination } of simpleRedirects) {
    const wasCreated = createRedirectFile(source, destination);
    if (wasCreated) {
      console.log(`  Created: ${source} -> ${destination}`);
      created++;
    } else {
      skipped++;
    }
  }

  console.log(`\nDone! Created ${created} redirect files, skipped ${skipped}.`);
}

main();
