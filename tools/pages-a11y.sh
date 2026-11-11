#!/usr/bin/env bash
# Accessibility check of the built static demo site (axe-core in a real browser, light + dark, desktop + phone).
# Serves the site like GitHub Pages does (under a base path, 404.html as fallback for deep links) and runs
# tools/pages-a11y.mjs against every route.
# Usage: tools/pages-a11y.sh <site-dir> [base-path]     e.g. tools/pages-a11y.sh frontend/dist/demo /opsdesk/
# The site must have been built with the same --base-href. Needs Node and network access for the first npm install.
set -euo pipefail

site=${1:?usage: pages-a11y.sh <site-dir> [base-path]}
base=${2:-/}
[[ $base == /* && $base == */ ]] || { echo "base-path must start and end with /" >&2; exit 2; }
[[ -f $site/index.html ]] || { echo "no index.html in $site (build the demo first)" >&2; exit 2; }

here=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d)
server_pid=
cleanup() { [[ -n $server_pid ]] && kill "$server_pid" 2>/dev/null || true; rm -rf "$work"; }
trap cleanup EXIT

# Same layout as GitHub Pages: the app below the base path, 404.html at the root of the served folder.
mkdir -p "$work/site${base}"
cp -r "$site"/. "$work/site${base}"
cp "$site/index.html" "$work/site/404.html"

# Pinned tools, installed into the temp dir (nothing is added to the repo). Use a preinstalled Chrome when there is one.
if command -v google-chrome >/dev/null 2>&1; then
  export PUPPETEER_SKIP_DOWNLOAD=true CHROME_PATH=${CHROME_PATH:-$(command -v google-chrome)}
fi
(cd "$work" && npm install --silent --no-audit --no-fund --no-save puppeteer@25.9.0 axe-core@4.14.0 http-server@14.1.1 >/dev/null)
cp "$here/pages-a11y.mjs" "$work/"

port=4300
"$work/node_modules/.bin/http-server" "$work/site" -p "$port" -s -c-1 &
server_pid=$!
for _ in $(seq 1 30); do curl -fs "http://127.0.0.1:$port${base}index.html" >/dev/null && break; sleep 0.5; done

(cd "$work" && node pages-a11y.mjs "http://127.0.0.1:$port" "$base")
