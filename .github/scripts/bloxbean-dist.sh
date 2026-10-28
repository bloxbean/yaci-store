#!/usr/bin/env bash
# Publishes a snapshot distribution - the files in $DIST_DIR, e.g. JVM and native zips - to the BloxBean repository
# bucket under $PREFIX/<version>/ (e.g. dist/snapshots/yano), served at $PUBLIC_URL. Used by
# bloxbean-snapshot-dist.yml. Standard: bloxbean/release-ops docs/12-bloxbean-maven-repository.md and
# templates/scripts/; keep it identical across repositories and configure it by env.
#
#   manifest   write SHA256SUMS and manifest.json for every file in $DIST_DIR
#   upload     upload them: files and SHA256SUMS, then manifest.json (marks the build published), then latest.json
#   verify     check the public URL serves latest.json and every file at its size
#   summary    append the published files to the job summary
#
# Environment: DIST_DIR, VERSION, BUCKET, PREFIX, PUBLIC_URL, RUNNER_TEMP, GITHUB_* and the AWS CLI's endpoint and
# credentials. A published version is never overwritten; only $PREFIX/ is ever written; nothing is deleted. Old
# builds expire by the bucket's lifecycle rule on dist/snapshots/, which every publication's latest.json outlives.
set -euo pipefail

: "${DIST_DIR:?}" "${VERSION:?}" "${PREFIX:?}"

manifest() {
  (cd "$DIST_DIR" && find . -maxdepth 1 -type f ! -name SHA256SUMS ! -name manifest.json -printf '%f\n' | sort \
    | xargs -d '\n' sha256sum --) > "$RUNNER_TEMP/SHA256SUMS"
  mv "$RUNNER_TEMP/SHA256SUMS" "$DIST_DIR/SHA256SUMS"
  python3 - "$DIST_DIR" "$VERSION" "$PUBLIC_URL/$PREFIX/$VERSION" \
    "$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID" > "$RUNNER_TEMP/manifest.json" <<'EOF'
import json, os, sys
from datetime import datetime, timezone
directory, version, base, run = sys.argv[1:]
files = []
for line in open(os.path.join(directory, 'SHA256SUMS')):
    digest, name = line.split(None, 1)
    name = name.strip()
    files.append({'name': name, 'url': f'{base}/{name}', 'size': os.path.getsize(os.path.join(directory, name)),
                  'sha256': digest})
print(json.dumps({'version': version, 'commit': os.environ['GITHUB_SHA'], 'branch': os.environ['GITHUB_REF_NAME'],
                  'published': datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'), 'run': run,
                  'files': files}, indent=2))
EOF
  mv "$RUNNER_TEMP/manifest.json" "$DIST_DIR/manifest.json"
  cat "$DIST_DIR/manifest.json"
}

upload() {
  local target="s3://$BUCKET/$PREFIX/$VERSION"
  if aws s3api head-object --bucket "$BUCKET" --key "$PREFIX/$VERSION/manifest.json" \
      > /dev/null 2> "$RUNNER_TEMP/head.err"; then
    echo "::error::$VERSION is already published at $PUBLIC_URL/$PREFIX/$VERSION/." >&2
    exit 1
  elif ! grep -q '(404)' "$RUNNER_TEMP/head.err"; then
    cat "$RUNNER_TEMP/head.err" >&2
    exit 1
  fi
  aws s3 cp "$DIST_DIR/" "$target/" --recursive --only-show-errors --exclude manifest.json
  aws s3 cp "$DIST_DIR/manifest.json" "$target/manifest.json" --only-show-errors --content-type application/json
  aws s3 cp "$DIST_DIR/manifest.json" "s3://$BUCKET/$PREFIX/latest.json" --only-show-errors \
    --content-type application/json --cache-control no-cache
}

verify() {
  curl -fsSL --retry 5 --retry-delay 5 --retry-all-errors "$PUBLIC_URL/$PREFIX/latest.json" \
    -o "$RUNNER_TEMP/latest.json"
  if ! cmp -s "$RUNNER_TEMP/latest.json" "$DIST_DIR/manifest.json"; then
    echo "::error::$PUBLIC_URL/$PREFIX/latest.json does not serve this build's manifest." >&2
    exit 1
  fi
  local url size served
  while read -r url size; do
    served=$(curl -fsSIL --retry 5 --retry-delay 5 --retry-all-errors "$url" \
      | tr -d '\r' | awk 'tolower($1) == "content-length:" { n = $2 } END { print n }')
    if [[ "$served" != "$size" ]]; then
      echo "::error::$url serves $served bytes, expected $size." >&2
      exit 1
    fi
  done < <(jq -r '.files[] | "\(.url) \(.size)"' "$DIST_DIR/manifest.json")
  echo "latest.json and every file are served for $VERSION."
}

summary() {
  {
    echo "## Published snapshot distribution \`$VERSION\`"
    echo
    echo "- Build: $PUBLIC_URL/$PREFIX/$VERSION/ (\`SHA256SUMS\`, \`manifest.json\`)"
    echo "- Newest build pointer: $PUBLIC_URL/$PREFIX/latest.json"
    echo
    echo '| File | Size |'
    echo '|---|---|'
    jq -r '.files[] | "| [\(.name)](\(.url)) | \(.size / 1e6 | round) MB |"' "$DIST_DIR/manifest.json"
  } >> "$GITHUB_STEP_SUMMARY"
}

command=${1:?usage: bloxbean-dist.sh manifest|upload|verify|summary}
case "$command" in
  manifest) manifest ;;
  upload) upload ;;
  verify) verify ;;
  summary) summary ;;
  *) echo "unknown command: $command" >&2; exit 2 ;;
esac
