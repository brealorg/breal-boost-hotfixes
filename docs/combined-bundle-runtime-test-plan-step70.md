# Combined Bundle Runtime Test Plan - Step 70

This document defines runtime validation for the current held combined Boost-for-Reddit bundle.

## Candidate state

- Branch: `work/boost-combined-bundle-dev1`
- Local candidate version: `1.4.32`
- Reserved tag: `morphe-patches-32`
- Publish status: `HELD_NOT_PUBLISHED`
- End-user release: `NO`

This candidate must not be published until runtime validation has been completed for every behavior change.

## Behavior changes currently in scope

### 1. Runtime media tap-action settings

Feature area:

- Direct Reddit GIF tap action
- Giphy preview tap action
- Static preview tap action

Expected settings values:

- `image_viewer`
- `video_viewer`
- `browser`
- `disabled`

Runtime validation must verify that changing the setting changes tap behavior without requiring a rebuild.

Required checks:

- Direct `i.redd.it/*.gif` opens according to the selected action.
- Giphy preview opens according to the selected action.
- Static preview opens according to the selected action.
- Disabled action does not open unintended viewer/browser flow.
- Browser action opens external/browser path only when selected.
- Existing inline media loading does not regress.
- Existing fullscreen media viewer still works.
- No crash on returning from viewer/browser.

### 2. Reddit gallery undelete metadata restore

Feature area:

- Deleted or unavailable Reddit gallery submissions
- Submission/listing JSON restoration
- `gallery_data`
- `media_metadata`
- `is_gallery`

Expected behavior:

- Normal undelete behavior for deleted text/image/video posts remains unchanged.
- Gallery posts with valid recoverable gallery metadata can restore `gallery_data` and `media_metadata`.
- `is_gallery` is set only when both gallery metadata fields are usable.
- Missing or malformed Wayback gallery payload returns safely without crashing.
- Direct media undelete behavior through `RedditMediaUndeleteInterceptor` remains unchanged.

Required checks:

- Deleted gallery post with Wayback hit.
- Deleted gallery post without Wayback hit.
- Deleted non-gallery image post.
- Deleted text post.
- Deleted video post.
- Normal visible gallery post.
- Normal visible non-gallery post.

Expected failure mode:

- If gallery metadata cannot be recovered, Boost should show the normal/fallback undelete state without crash.
- No null-pointer crash from missing `posts`, `models`, `t3_<id>`, `media`, `gallery`, or `mediaMetadata`.

## Static gates required before runtime test

Run these before installing a candidate APK:

    make release-local-final VERSION=1.4.32 TAG=morphe-patches-32
    make boost-runtime-media-marker-gate VERSION=1.4.32
    make boost-reddit-gallery-undelete-marker-gate VERSION=1.4.32
    make release-gate VERSION=1.4.32 TAG=morphe-patches-32
    make release-hold-gate VERSION=1.4.32 TAG=morphe-patches-32

## Install/runtime prerequisites

Build the APK from the original Boost 1.12.12 APK, not from an already patched APK.

Required enabled patches for runtime validation:

- Spoof client
- Modify login WebView
- Fix Boost target SDK 35 compatibility
- Runtime media tap-action settings
- Automatically undelete Reddit content
- Existing release-stable Boost fixes included in the combined bundle

Validate from apply log that `Spoof client` and `Modify login WebView` are actually enabled.

## Runtime evidence to capture

For every runtime test run, capture:

- candidate APK SHA256
- installed package name
- installed version
- installed target SDK
- active patch apply log
- relevant logcat markers
- runtime setting values used
- URLs/posts tested
- observed behavior
- pass/fail classification

## Logcat markers of interest

Runtime media:

- `morphe_boost_direct_reddit_gif_tap_action`
- `morphe_boost_giphy_preview_tap_action`
- `morphe_boost_static_preview_tap_action`
- `open direct i.redd.it gif via Boost image viewer`
- `openStaticImageViaBoost`

Reddit gallery undelete:

- `morphe_boost_reddit_gallery_undelete_submission_json`
- `restored gallery metadata`
- `existing gallery metadata`

General failure markers:

- `AndroidRuntime`
- `FATAL EXCEPTION`
- `NullPointerException`
- `JsonMappingException`
- `GifIOException`
- `FirebaseCrashlytics`

## Release rule

Do not publish the combined bundle until:

- all static gates pass
- runtime media settings are validated
- Reddit gallery undelete is validated or explicitly classified as best-effort with no crash
- no new crash markers are observed
- Manager-facing metadata is updated for the final combined release
- final version/tag are intentionally selected
