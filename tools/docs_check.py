#!/usr/bin/env python3
"""Keeps the repository docs honest (stdlib only).

1. Relative links in README.md, docs/runbooks/*.md, docs/postmortems/*.md and docs/context/*.md resolve to existing paths.
2. Every runbook the Datadog monitors link (infra/terraform/datadog/monitors.tf) exists in docs/runbooks.
3. Every tracked file outside the docs that carries the marker "MOCK:" lies under a path in the "Where" column of the README
   table "Mocks": the mock inventory cannot silently lose or gain an entry.
docs/guide is checked by tools/guide_check.py. Exit code 1 lists every problem. Usage: python3 tools/docs_check.py
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LINK = re.compile(r"!?\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
MARKER_EXCLUDED = ("docs/", "README.md", "CLAUDE.md", "AGENTS.md", "tools/docs_check.py")


def strip_code(text: str) -> str:
    """Links in code blocks and inline code are examples, not links."""
    text = re.sub(r"```.*?```", "", text, flags=re.S)
    return re.sub(r"`[^`]*`", "", text)


def check_links() -> list[str]:
    files = [ROOT / "README.md"]
    for pattern in ("docs/runbooks/*.md", "docs/postmortems/*.md", "docs/context/*.md"):
        files += sorted(ROOT.glob(pattern))
    problems = []
    for md in files:
        for target in LINK.findall(strip_code(md.read_text(encoding="utf-8"))):
            if re.match(r"^(https?:|mailto:|#)", target):
                continue
            path = target.split("#")[0]
            if path and not (md.parent / path).resolve().exists():
                problems.append(f"{md.relative_to(ROOT)}: link target does not exist: {target}")
    return problems


def check_monitor_runbooks() -> list[str]:
    monitors = (ROOT / "infra/terraform/datadog/monitors.tf").read_text(encoding="utf-8")
    names = set(re.findall(r"runbook_base_url\}/([a-z0-9-]+\.md)", monitors))
    return [f"monitors.tf links runbook {n} which does not exist in docs/runbooks" for n in sorted(names)
            if not (ROOT / "docs/runbooks" / n).exists()]


def readme_mock_paths() -> list[str]:
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    section = re.search(r"^## Mocks\n(.*?)(?=^## |\Z)", readme, flags=re.S | re.M)
    if not section:
        return []
    paths = []
    for line in section.group(1).splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 4 and not set(cells[0]) <= set("-: "):
            paths += re.findall(r"`([^`]+)`", cells[2])  # third column: Where
    return paths


def check_mock_inventory() -> list[str]:
    paths = readme_mock_paths()
    if not paths:
        return ['README.md has no table "Mocks" with a "Where" column (third column, paths in backticks)']
    tracked = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, capture_output=True, check=True).stdout.decode().split("\0")
    problems = []
    for name in filter(None, tracked):
        if name.startswith(MARKER_EXCLUDED) or not (ROOT / name).is_file():
            continue
        try:
            text = (ROOT / name).read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if "MOCK:" in text and not any(name == p or (p.endswith("/") and name.startswith(p)) for p in paths):
            problems.append(f"{name} carries 'MOCK:' but is not covered by the README mock table (column Where)")
    return problems


def main() -> int:
    problems = check_links() + check_monitor_runbooks() + check_mock_inventory()
    if problems:
        print("\n".join(problems))
        return 1
    print("docs ok: links, monitor runbooks and mock inventory")
    return 0


if __name__ == "__main__":
    sys.exit(main())
