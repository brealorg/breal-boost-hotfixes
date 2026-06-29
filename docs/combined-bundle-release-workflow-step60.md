# Combined Bundle Release Workflow - Step 60

This document describes the local combined-bundle workflow after the held Boost runtime media candidate and release-tooling cleanup.

## Current branch model

- Published baseline: `origin/main` / `origin/dev`
- Current public Manager metadata: `1.4.31`
- Combined work branch: `work/boost-combined-bundle-dev1`
- Held local candidate metadata currently on branch: `1.4.32`
- Held tag name reserved but not created: `morphe-patches-32`

The combined branch starts from the held Boost runtime media tap-action candidate and adds release-tooling cleanup. It is not an end-user release branch yet.

## Current local candidate status

The current local `1.4.32` candidate is intentionally held.

Classification:

    PUBLISH_STATUS=HELD_NOT_PUBLISHED
    END_USER_RELEASE=NO
    NEXT_PUBLIC_RELEASE=COMBINED_BUNDLE_AFTER_MORE_FIXES

Do not publish the current held candidate as a standalone release.

Explicitly do not do these until the final combined bundle is intentionally released:

- Do not create `morphe-patches-32`.
- Do not create a GitHub release for `morphe-patches-32`.
- Do not push this branch to `main` or `dev` as-is.
- Do not expose `1.4.32` through the raw `patches-bundle.json` source.
- Do not treat the existing local MPP as final after more fixes are added.

## Local final candidate flow

After adding a new fix to the combined branch, rebuild and re-gate locally before any publish decision.

Use:

    make release-local-final VERSION=<version> TAG=<tag>
    make boost-runtime-media-marker-gate VERSION=<version>
    make release-hold-gate VERSION=<version> TAG=<tag>

`release-local-final` performs the local build, updates README SHA through `scripts/update-readme-sha.py`, and runs `release-gate`.

The runtime media marker gate protects the held Boost media tap-action feature by checking the built MPP for both extension-scoped and MPP-wide markers.

The hold gate verifies that a local candidate has not accidentally become end-user visible.

## Held-candidate gate

Use this while the candidate is intentionally local only:

    make release-hold-gate VERSION=<version> TAG=<tag>

The hold gate verifies:

- local MPP exists
- local MPP contains `classes.dex`
- local MPP contains `extensions/boostforreddit.mpe`
- local release tag does not exist
- remote release tag does not exist
- GitHub release does not exist
- raw Manager metadata does not expose the held version/tag/asset

## Publish-time flow

Only after the combined bundle is selected as the next public release:

1. Rebuild final local artifact.
2. Update metadata and README for the final version/tag.
3. Run local release gates.
4. Commit final metadata and release notes.
5. Create tag.
6. Push branch/tag intentionally.
7. Create GitHub release asset.
8. Verify remote raw metadata and downloaded MPP.

Remote verification remains:

    make verify-remote VERSION=<version> TAG=<tag>

## Required final checks before publish

Before publishing a combined bundle, verify:

- `git status --porcelain` is clean
- version is consistent in `gradle.properties`
- version is consistent in `patches-bundle.json`
- README current release/version/tag/asset/SHA are correct
- final MPP SHA equals README SHA
- final MPP contains `classes.dex`
- final MPP contains `extensions/boostforreddit.mpe`
- Boost runtime media marker gate passes
- release-gate passes
- no stale old release values remain in metadata
- Manager-facing description is short and current-release-only
- no held-candidate warning is violated
- runtime validation has been completed for behavior changes

## Current tooling added on combined branch

The combined branch currently includes these release-tooling additions:

- `scripts/update-readme-sha.py`
- `scripts/release-hold-gate.py`
- `scripts/check-boost-runtime-media-markers.py`
- `make update-readme-sha`
- `make release-local-final`
- `make release-hold-gate`
- `make boost-runtime-media-marker-gate`

## Lemmy boundary

Boost for Lemmy remains research/documentation-only in the current combined-bundle path.

Do not add Boost-for-Lemmy behavior patches to this Boost-for-Reddit source lane unless a dedicated Lemmy patch lane is created first.
