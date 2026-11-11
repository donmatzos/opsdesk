#!/usr/bin/env python3
"""Checks the project guide sources in docs/guide.

- every relative link and image resolves to an existing file or directory (relative to docs/guide);
- every glossary entry (## Term in glossary.md) has exactly one line `Source: [label](https://...)`.
Exit code 1 lists every problem. Usage: python3 tools/guide_check.py
"""
import re
import sys
from pathlib import Path

GUIDE = Path(__file__).resolve().parent.parent / "docs" / "guide"
LINK = re.compile(r"!?\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
SOURCE = re.compile(r"^Source: \[[^\]]+\]\(https://[^)\s]+\)\s*$")


def strip_code(text: str) -> str:
    """Links inside code blocks and inline code are examples, not links."""
    text = re.sub(r"```.*?```", "", text, flags=re.S)
    return re.sub(r"`[^`]*`", "", text)


def check_links(md_file: Path) -> list[str]:
    problems = []
    for target in LINK.findall(strip_code(md_file.read_text(encoding="utf-8"))):
        if re.match(r"^(https?:|mailto:|#)", target):
            continue
        path = target.split("#")[0]
        if path and not (md_file.parent / path).resolve().exists():
            problems.append(f"{md_file.name}: link target does not exist: {target}")
    return problems


def check_glossary(md_file: Path) -> list[str]:
    problems = []
    entries = re.split(r"^## ", md_file.read_text(encoding="utf-8"), flags=re.M)[1:]
    for entry in entries:
        term = entry.splitlines()[0].strip()
        sources = [line for line in entry.splitlines() if line.startswith("Source:")]
        if len(sources) != 1 or not SOURCE.match(sources[0]):
            problems.append(f"glossary.md: '{term}' needs exactly one line 'Source: [label](https://...)'")
    return problems


def main() -> int:
    problems = []
    for md_file in sorted(GUIDE.glob("*.md")):
        problems += check_links(md_file)
    glossary = GUIDE / "glossary.md"
    if glossary.exists():
        problems += check_glossary(glossary)
    if problems:
        print("\n".join(problems))
        return 1
    print(f"guide ok: {len(list(GUIDE.glob('*.md')))} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
