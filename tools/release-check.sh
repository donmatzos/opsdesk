#!/usr/bin/env bash
# Checks that a release tag is consistent before anything is published: the tag (vX.Y.Z), the Maven project version and the
# Helm chart's version and appVersion must all be the same X.Y.Z, and none may be a SNAPSHOT.
# Usage: tools/release-check.sh <tag>        RELEASE_ROOT=<dir> checks another checkout (used to test this script).
set -euo pipefail

tag=${1:?usage: release-check.sh <tag, e.g. v0.1.0>}
root=${RELEASE_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}

fail() { echo "RELEASE CHECK FAIL: $*" >&2; exit 1; }

[[ ${tag} =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "tag '${tag}' is not of the form vX.Y.Z"
version=${tag#v}

pom_version=$(python3 - "${root}/pom.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
print(ET.parse(sys.argv[1]).getroot().find("m:version", ns).text)
PY
)
chart_version=$(sed -n 's/^version: *//p' "${root}/deploy/helm/opsdesk/Chart.yaml" | tr -d '"')
app_version=$(sed -n 's/^appVersion: *//p' "${root}/deploy/helm/opsdesk/Chart.yaml" | tr -d '"')

[[ ${pom_version} != *SNAPSHOT* ]] || fail "pom.xml version is ${pom_version}: a release needs a fixed version (set ${version} in a version PR first)"
[[ ${pom_version} == "${version}" ]] || fail "pom.xml version ${pom_version} does not match the tag (${version})"
[[ ${chart_version} == "${version}" ]] || fail "Chart.yaml version ${chart_version} does not match the tag (${version})"
[[ ${app_version} == "${version}" ]] || fail "Chart.yaml appVersion ${app_version} does not match the tag (${version})"

echo "release check ok: ${tag} = pom ${pom_version} = chart ${chart_version} (appVersion ${app_version})"
