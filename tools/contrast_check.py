#!/usr/bin/env python3
"""WCAG contrast check of the design tokens in frontend/src/styles/_tokens.scss (light and dark).

Text pairs need 4.5:1, borders of controls and the focus ring 3:1. Exit code 1 lists every failing pair.
Usage: python3 tools/contrast_check.py
"""
import re
import sys
from pathlib import Path

TOKENS = Path(__file__).resolve().parent.parent / "frontend/src/styles/_tokens.scss"
SURFACES = ["bg", "surface", "surface-2"]
TEXT_ON_SURFACES = ["text", "muted", "link", "accent", "ok", "warning", "danger", "info"]
SOFT_PAIRS = [("ok", "ok-soft"), ("warning", "warning-soft"), ("danger", "danger-soft"), ("info", "info-soft"),
              ("accent-text", "accent-soft"), ("on-accent", "accent")]


def luminance(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    channels = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    lin = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def ratio(a: str, b: str) -> float:
    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def colors(block: str) -> dict[str, str]:
    return dict(re.findall(r"--([a-z0-9-]+):\s*(#[0-9a-fA-F]{6});", block))


def schemes(scss: str) -> dict[str, dict[str, str]]:
    blocks = {name: re.search(r"@mixin " + name + r"-tokens \{(.*?)\n\}", scss, re.S) for name in ("light", "dark")}
    if not all(blocks.values()):
        raise SystemExit("_tokens.scss must define @mixin light-tokens and @mixin dark-tokens")
    return {name: colors(m.group(1)) for name, m in blocks.items()}


def main() -> int:
    failures = []
    for scheme, t in schemes(TOKENS.read_text()).items():
        pairs = [(fg, bg, 4.5) for fg in TEXT_ON_SURFACES for bg in SURFACES]
        pairs += [(fg, bg, 4.5) for fg, bg in SOFT_PAIRS]
        pairs += [("strong", bg, 3.0) for bg in SURFACES] + [("focus", bg, 3.0) for bg in ("bg", "surface")]
        for fg, bg, minimum in pairs:
            r = ratio(t[fg], t[bg])
            if r < minimum:
                failures.append(f"{scheme}: {fg} {t[fg]} on {bg} {t[bg]} = {r:.2f} (needs {minimum})")
    if failures:
        print("\n".join(failures))
        return 1
    print("contrast ok: all token pairs pass in light and dark")
    return 0


if __name__ == "__main__":
    sys.exit(main())
