#!/usr/bin/env bash
# Keeps a GitHub pre-release, tag `snapshot`, pointing at the newest development snapshot, so users find it on the
# repository's Releases page: the latest Maven snapshot version with a Gradle snippet, and the latest snapshot
# distribution files with their checksums. Everything is read back from the public BloxBean repository; files stay
# there and are only linked. Run by bloxbean-snapshot.yml and bloxbean-snapshot-dist.yml after they publish.
# Standard: bloxbean/release-ops docs/12-bloxbean-maven-repository.md and templates/scripts/; keep it identical.
#
# Environment: GH_TOKEN (contents: write), GITHUB_REPOSITORY, GITHUB_SHA, GITHUB_SERVER_URL,
# MAVEN_METADATA_URL (the public artifact-level maven-metadata.xml of the coordinate users depend on),
# MAVEN_COORDINATE ("group:artifact"), MAVEN_REPOSITORY_URL, and optional DIST_LATEST_URL (latest.json).
#
# The release is created once - that is the only time watchers are notified - and edited afterwards. It is a
# pre-release that never becomes Latest, and the `snapshot` tag moves to the commit that published last. Only v* tags
# trigger the release workflows.
set -euo pipefail

TAG=snapshot
notes="$RUNNER_TEMP/snapshot-notes.md"

metadata=$(curl -fsSL --retry 5 --retry-all-errors "${MAVEN_METADATA_URL:?}")
maven_version=$(sed -n 's:.*<latest>\(.*\)</latest>.*:\1:p' <<< "$metadata" | head -1)
maven_updated=$(sed -n 's:.*<lastUpdated>\([0-9]\{8\}\).*:\1:p' <<< "$metadata" | head -1)
dist=""
if [[ -n "${DIST_LATEST_URL:-}" ]]; then
  dist=$(curl -fsSL --retry 5 --retry-delay 5 "$DIST_LATEST_URL" 2> /dev/null || true)
fi

{
  echo "**Development snapshot - not a release.** Built from \`$GITHUB_REF_NAME\` and updated automatically by"
  echo "the snapshot workflows. Official releases are the \`v*\` releases."
  echo
  echo "### Maven"
  echo
  echo "Latest snapshot: **\`$maven_version\`**," \
    "published ${maven_updated:0:4}-${maven_updated:4:2}-${maven_updated:6:2}."
  echo "Each commit publishes its own version, so pin the exact one."
  echo
  echo '```groovy'
  echo 'repositories {'
  echo '    mavenCentral()'
  echo '    maven {'
  echo "        url = uri('$MAVEN_REPOSITORY_URL')"
  echo '        mavenContent { snapshotsOnly() }'
  echo '    }'
  echo '}'
  echo 'dependencies {'
  echo "    implementation '$MAVEN_COORDINATE:$maven_version'"
  echo '}'
  echo '```'
  if [[ -n "$dist" ]]; then
    echo
    echo "### Distributions"
    echo
    jq -r --arg server "$GITHUB_SERVER_URL/$GITHUB_REPOSITORY" '"Latest snapshot build **`\(.version)`**"
      + " from [`\(.commit[0:7])`](\($server)/commit/\(.commit)), published \(.published[0:10])."
      + " Snapshot builds are kept for 30 days."' <<< "$dist"
    echo
    echo '| File | Size | SHA-256 |'
    echo '|---|---|---|'
    jq -r '.files[] | "| [\(.name)](\(.url)) | \(.size / 1e6 | round) MB | `\(.sha256)` |"' <<< "$dist"
    echo
    echo "Newest build as JSON: $DIST_LATEST_URL"
  fi
} > "$notes"

if gh api "repos/$GITHUB_REPOSITORY/git/ref/tags/$TAG" > /dev/null 2>&1; then
  gh api -X PATCH "repos/$GITHUB_REPOSITORY/git/refs/tags/$TAG" -f sha="$GITHUB_SHA" -F force=true > /dev/null
else
  gh api -X POST "repos/$GITHUB_REPOSITORY/git/refs" -f ref="refs/tags/$TAG" -f sha="$GITHUB_SHA" > /dev/null
fi
if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" > /dev/null 2>&1; then
  gh release edit "$TAG" --repo "$GITHUB_REPOSITORY" --notes-file "$notes" --prerelease --latest=false
else
  gh release create "$TAG" --repo "$GITHUB_REPOSITORY" --title "Development snapshot" --notes-file "$notes" \
    --prerelease --latest=false --verify-tag
fi
echo "Updated $GITHUB_SERVER_URL/$GITHUB_REPOSITORY/releases/tag/$TAG"
