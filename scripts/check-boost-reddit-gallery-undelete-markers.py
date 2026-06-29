#!/usr/bin/env python3
import argparse
import hashlib
import sys
import tempfile
import zipfile
from pathlib import Path

SOURCE_REQUIRED_MARKERS = [
    "morphe_boost_reddit_gallery_undelete_submission_json",
    '"media_metadata"',
    "restoreRedditGalleryMetadata",
    "hasUsableGalleryMetadata",
    'getNested(data, "posts", "models", "t3_" + id, "media")',
    'editableNode.set("gallery_data", gallery)',
    'editableNode.set("media_metadata", mediaMetadata)',
    'editableNode.set("is_gallery", BooleanNode.TRUE)',
]

MPP_REQUIRED_MARKERS = [
    "morphe_boost_reddit_gallery_undelete_submission_json",
    "media_metadata",
    "restoreRedditGalleryMetadata",
    "hasUsableGalleryMetadata",
]

REQUIRED_MPP_ENTRIES = [
    "classes.dex",
    "extensions/boostforreddit.mpe",
]

SOURCE_FILES = [
    Path("extensions/boostforreddit/src/main/java/app/morphe/extension/boostforreddit/http/arcticshift/ArcticShift.java"),
    Path("extensions/boostforreddit/src/main/java/app/morphe/extension/boostforreddit/http/reddit/RedditSubmissionUndeleteInterceptor.java"),
]

def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def read_source() -> str:
    chunks = []
    missing = []
    for path in SOURCE_FILES:
        if not path.is_file():
            missing.append(str(path))
            continue
        chunks.append(path.read_text(errors="replace"))
    if missing:
        raise FileNotFoundError("Missing source files: " + ", ".join(missing))
    return "\n".join(chunks)

def archive_text_contains(zip_path: Path, marker: str) -> bool:
    marker_bytes = marker.encode("utf-8", errors="ignore")
    with zipfile.ZipFile(zip_path) as zf:
        for name in zf.namelist():
            with zf.open(name) as f:
                if marker_bytes in f.read():
                    return True
    return False

def main() -> int:
    parser = argparse.ArgumentParser(description="Check Boost Reddit gallery undelete source and MPP markers.")
    parser.add_argument("--version", required=True)
    parser.add_argument("--mpp", default=None)
    args = parser.parse_args()

    mpp = Path(args.mpp or f"patches/build/libs/patches-{args.version}.mpp")

    print("===== Boost Reddit gallery undelete marker gate =====")
    print(f"version: {args.version}")
    print(f"mpp: {mpp}")

    failures = []

    try:
        source_text = read_source()
    except Exception as exc:
        print(f"ERROR source read failed: {exc}")
        return 1

    print()
    print("===== source-required markers =====")
    for marker in SOURCE_REQUIRED_MARKERS:
        if marker in source_text:
            print(f"OK      {marker}")
        else:
            print(f"MISSING {marker}")
            failures.append(f"source missing marker: {marker}")

    print()
    print("===== local MPP =====")
    if not mpp.is_file():
        print(f"MISSING {mpp}")
        failures.append(f"missing MPP: {mpp}")
    else:
        print(f"mpp_sha256: {sha256(mpp)}")
        print(f"mpp_size: {mpp.stat().st_size}")

        with zipfile.ZipFile(mpp) as zf:
            names = set(zf.namelist())

        print()
        print("===== MPP required entries =====")
        for entry in REQUIRED_MPP_ENTRIES:
            if entry in names:
                print(f"OK      {entry}")
            else:
                print(f"MISSING {entry}")
                failures.append(f"MPP missing entry: {entry}")

        print()
        print("===== MPP required markers =====")
        for marker in MPP_REQUIRED_MARKERS:
            if archive_text_contains(mpp, marker):
                print(f"OK      {marker}")
            else:
                print(f"MISSING {marker}")
                failures.append(f"MPP missing marker: {marker}")

    print()
    if failures:
        print("REDDIT GALLERY UNDELETE MARKER GATE FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("REDDIT GALLERY UNDELETE MARKER GATE OK")
    print("BOOST_REDDIT_GALLERY_UNDELETE_SOURCE_MARKERS=OK")
    print("BOOST_REDDIT_GALLERY_UNDELETE_MPP_MARKERS=OK")
    return 0

if __name__ == "__main__":
    sys.exit(main())
