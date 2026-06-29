# MORPHE-WL-02 — Boost media matrix runtime gate

- Install performed: NO
- Patch Lemmy: NO
- Patch Reddit: NO

## Purpose

Create a repeatable media runtime gate for Boost for Reddit before any release decision.

## Required media cells

- WL02-01 — direct `i.redd.it/*.gif`
- WL02-02 — static image preview
- WL02-03 — Giphy preview
- WL02-04 — Redgifs
- WL02-05 — `v.redd.it`
- WL02-06 — AVIF/static modern image
- WL02-07 — image-heavy post/gallery
- WL02-08 — download/share

## Required runtime setting states

- `image_viewer`
- `video_viewer` where applicable
- `browser`
- `disabled` if supported

## Required logcat blockers

- `FATAL EXCEPTION`
- `ActivityNotFoundException`
- `GifIOException`
- `Data is not in GIF format`
- `ExoPlaybackException`
- `GlideException`
- `Request returned non-successful status code: 403`
- `403 Blocked`
- `android:com.rubenmayayo.reddit:v1.12.12`

## Static/source markers

- `InlineGiphyCommentPreview`
- `morphe_boost_direct_reddit_gif_tap_action`
- `image_viewer`
- `video_viewer`
- `browser`
- `openStaticImageViaBoost`
- `open direct i.redd.it gif via Boost image viewer`
- `MediaImageActivity`
- `MediaVideoActivity`

## Existing source plan reference

This WL-02 seed is linked to the existing combined runtime test plan:

- `docs/combined-bundle-runtime-test-plan-step70.md`
