# Breal Boost Hotfixes

This repository is a temporary unofficial Boost for Reddit hotfix source for Morphe. It is derived from Patcheddit while selected upstream fixes are pending review.

It is not intended to replace Patcheddit.

## Current fixes

This hotfix bundle currently includes:

### 1. v.redd.it audio fix

Fixes Boost sharing/downloading Reddit videos where audio retrieval fails with:

    Failed to retrieve audio:
    JsonParseException: Unexpected character '<'

### 2. Giphy loading fix

Fixes very slow Giphy loading in Boost by bypassing Boost's old Giphy API resolver and using Boost's existing direct MP4 fallback:

    https://media.giphy.com/media/<id>/giphy.mp4

## Tested with

- Boost 1.12.12
- Morphe Manager
- Local Patcheddit .mpp bundle

## Morphe source

Recommended source:

    github.com/brealorg/breal-boost-hotfixes

Direct JSON source:

    https://raw.githubusercontent.com/brealorg/breal-boost-hotfixes/main/patches-bundle.json

Fallback method:

Download patches-1.4.0.mpp from the latest release and import it locally in Morphe.

## Important

Morphe should show this source as "Breal Boost Hotfixes".

This source is intended only for temporary Boost hotfix testing.

Patch your own clean Boost APK. Do not install random pre-patched APKs from strangers.

## Upstream PRs

These fixes have been submitted upstream and should be considered temporary until merged into Patcheddit.

## Known issue

The experimental inline Giphy comment preview patch has been removed from the public bundle because it caused runtime crashes when opening comment threads in Boost for Reddit 1.12.12.

For a stable Boost setup, avoid enabling comment-modifying patches such as inline Giphy previews, undelete/archive/Arctic Shift comment patches, or similar CommentViewHolder hooks unless testing them separately.

Current safe patch set:
- Fix audio missing in downloads
- Fix slow Giphy loading
- Spoof client, if needed
- Modify login WebView, if needed
