#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import re
from pathlib import Path


ROOT = Path.cwd()


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def parse_gradle_version(path: Path) -> str:
    text = read(path)
    match = re.search(r"(?m)^\s*version\s*=\s*([^\s#]+)\s*$", text)
    if not match:
        raise SystemExit("Could not find version = ... in gradle.properties")
    return match.group(1)


def mpp_name_for(version: str) -> str:
    return f"patches-{version}.mpp"


def read_readme_sha(readme: str) -> str | None:
    match = re.search(r"(?ms)^SHA256:\s*`?([0-9a-f]{64})`?", readme)
    return match.group(1) if match else None


def set_readme_heading_value(text: str, heading: str, value: str) -> str:
    lines = text.splitlines()

    for i, line in enumerate(lines):
        if line.strip() == heading:
            j = i + 1
            while j < len(lines) and lines[j].strip() == "":
                j += 1

            replacement = f"`{value}`"

            if j < len(lines):
                lines[j] = replacement
            else:
                lines.append("")
                lines.append(replacement)

            return "\n".join(lines) + "\n"

    raise SystemExit(f"Could not find README heading: {heading}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Update or verify README SHA256 against a built Morphe .mpp artifact."
    )
    parser.add_argument(
        "--version",
        help="Bundle version. Defaults to gradle.properties.",
    )
    parser.add_argument(
        "--mpp",
        help="Path to built .mpp. Defaults to patches/build/libs/patches-<version>.mpp.",
    )
    parser.add_argument(
        "--readme",
        default="README.md",
        help="README path. Defaults to README.md.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Only verify README SHA; do not modify README.md.",
    )
    args = parser.parse_args()

    version = args.version or parse_gradle_version(ROOT / "gradle.properties")
    mpp_path = Path(args.mpp) if args.mpp else ROOT / "patches" / "build" / "libs" / mpp_name_for(version)
    readme_path = Path(args.readme)

    if not mpp_path.exists():
        raise SystemExit(f"MPP does not exist: {mpp_path}")

    if not readme_path.exists():
        raise SystemExit(f"README does not exist: {readme_path}")

    readme = read(readme_path)
    actual_sha = sha256_file(mpp_path)
    current_sha = read_readme_sha(readme)

    expected_asset = mpp_name_for(version)
    if version not in readme:
        raise SystemExit(f"README does not contain version {version}")

    if expected_asset not in readme:
        raise SystemExit(f"README does not contain asset name {expected_asset}")

    print("version:", version)
    print("mpp:", mpp_path)
    print("mpp_sha256:", actual_sha)
    print("readme:", readme_path)
    print("readme_sha256:", current_sha or "(missing)")

    if args.check:
        if current_sha != actual_sha:
            raise SystemExit(
                f"README SHA mismatch. README={current_sha}, actual={actual_sha}"
            )
        print("README SHA OK")
        return 0

    updated = set_readme_heading_value(readme, "SHA256:", actual_sha)

    if updated == readme:
        print("README SHA already current.")
        return 0

    write(readme_path, updated)
    print("README SHA updated.")
    print("old_sha256:", current_sha or "(missing)")
    print("new_sha256:", actual_sha)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
