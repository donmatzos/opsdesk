#!/usr/bin/env python3
"""Keep agent context small: every file in docs/context/ (and CLAUDE.md) must stay
within the line and byte limits, and every context file must be listed in INDEX.md."""
import sys
from pathlib import Path

MAX_LINES, MAX_BYTES = 100, 6 * 1024
root = Path(__file__).resolve().parent.parent
ctx = root / "docs" / "context"
files = [root / "CLAUDE.md", *sorted(ctx.glob("*.md"))]
index = (ctx / "INDEX.md").read_text(encoding="utf-8")
errors = []
for f in files:
    data = f.read_bytes()
    lines = data.count(b"\n")
    if lines > MAX_LINES or len(data) > MAX_BYTES:
        errors.append(f"{f.relative_to(root)}: {lines} lines, {len(data)} B (max {MAX_LINES}/{MAX_BYTES})")
    if f.parent == ctx and f.name != "INDEX.md" and f"`{f.name}`" not in index:
        errors.append(f"{f.relative_to(root)}: not listed in INDEX.md")
for e in errors:
    print(e)
print(f"{len(files)} files checked, {len(errors)} problems")
sys.exit(1 if errors else 0)
