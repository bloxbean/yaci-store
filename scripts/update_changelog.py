import requests

REPO = "bloxbean/yaci-store"
MDX_FILE = "docs/changelogs.mdx"

releases = requests.get(f"https://api.github.com/repos/{REPO}/releases").json()

with open(MDX_FILE, "w") as f:
    f.write("# 📝 Changelog\n\n")
    for release in releases:
        f.write(f"## 📦 {release['name']} — {release['published_at'][:10]}\n\n")
        f.write(f"**Tag**: `{release['tag_name']}`\n\n")
        if release.get("body"):
            f.write(f"{release['body']}\n\n")
        else:
            f.write("_No changelog provided._\n\n")
        f.write("---\n\n")
