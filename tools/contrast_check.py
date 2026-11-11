#!/usr/bin/env python3
"""WCAG contrast check of the design tokens in frontend/src/styles/_tokens.scss (light and dark).

Text pairs need 4.5:1, UI parts (borders, focus ring) 3:1. Exit code 1 lists every failing pair.
Usage: python3 tools/contrast_check.py
"""
import re
import sys
from pathlib import Path

TOKENS = Path(__file__).resolve().parent.parent / "frontend/src/styles/_tokens.scss"
TEXT = ["text", "muted", "link", "accent", "danger", "warning", "info"]
UI = ["border", "focus"]


def luminance(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    channels = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    lin = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def ratio(a: str, b: str) -> float:
    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def schemes(scss: str) -> dict[str, dict[str, str]]:
    # First ":root { ... }" is light, the one inside "@media (prefers-color-scheme: dark)" is dark.
    dark_start = scss.index("prefers-color-scheme: dark")
    def colors(block: str) -> dict[str, str]:
        return dict(re.findall(r"--([a-z-]+):\s*(#[0-9a-fA-F]{6});", block))
    return {"light": colors(scss[:dark_start]), "dark": colors(scss[dark_start:])}


def main() -> int:
    failures = []
    for scheme, t in schemes(TOKENS.read_text()).items():
        pairs = [(f, b, 4.5) for f in TEXT for b in ("bg", "surface")]
        pairs += [(f, b, 3.0) for f in UI for b in ("bg", "surface")]
        pairs += [("on-accent", "accent", 4.5)]
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
