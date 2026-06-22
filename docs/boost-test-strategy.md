# Boost for Reddit test strategy

This document defines the local Boost test model and prevents accidental creation of unnecessary app variants.

## Installed package roles

| Package | Role | Rule |
| --- | --- | --- |
| `com.rubenmayayo.reddit` | Normal user app | Do not touch unless explicitly approved. |
| `com.rubenmayayo.reddit.dev` | Boost DEV | This is the only development/runtime test app. All Boost development testing happens here. |
| Any other `com.rubenmayayo.reddit.*` package | Avoid | Do not create extra app variants by default. |

## Boost DEV model

Boost DEV is the active development app:

- Package: `com.rubenmayayo.reddit.dev`
- Label: `Boost DEV`
- Purpose: development and runtime testing
- Update method: install new DEV candidates over this package
- Rollback method: reinstall a known-good DEV APK from `local-artifacts/apk-archive/`

The normal app, `com.rubenmayayo.reddit`, is not part of the development lane and must remain untouched unless explicitly approved.

## Known-good rollback baseline

Current known-good Boost DEV rollback baseline:

- Package: `com.rubenmayayo.reddit.dev`
- Label: `Boost DEV`
- Version name: `1.12.12`
- Version code: `210011212`
- Target SDK: `32`
- Status: logout/login verified
- Purpose: clean Patcheddit-compatible Boost DEV auth baseline

Known marker state:

Present:

- Modify login WebView
- RedReader user-agent
- `redreader://rr_oauth_redir`
- Morphe runtime/extension markers

Not present:

- SDK35 target
- `POST_NOTIFICATIONS`
- inline Giphy patch
- static image viewer patch

The known-good rollback APK is archived locally under:

- `local-artifacts/apk-archive/*-working-dev-auth-baseline/`

`local-artifacts/` is intentionally ignored by Git.

## APK gallery

The local APK gallery exists so Boost DEV can be overwritten during development without losing the ability to return to a known working APK.

Archive candidates under:

- `local-artifacts/apk-archive/`

Each archived candidate should include:

- APK
- SHA256
- manifest JSON
- notes
- apply log when available
- result JSON when available

The APK gallery is a code rollback mechanism. It is not an app data backup.

## Current full SDK35 candidate

The current full candidate is first built APK-only.

Expected state before DEV conversion:

- Package: `com.rubenmayayo.reddit`
- Label: `Boost`
- Target SDK: `35`
- `POST_NOTIFICATIONS`: present
- Installed by default: no

Before runtime testing, this candidate must be converted to Boost DEV:

- Package rewritten to `com.rubenmayayo.reddit.dev`
- Label preserved as `Boost DEV`
- Signed with the same DEV test keystore
- Installed with `adb install -r` over existing Boost DEV

## Runtime test policy

Runtime testing happens in Boost DEV:

- `com.rubenmayayo.reddit.dev`

Do not silently create a growing set of app variants.

Runtime rules:

- Confirm `com.rubenmayayo.reddit` exists and remains untouched.
- Confirm `com.rubenmayayo.reddit.dev` exists.
- Confirm a known-good DEV rollback APK exists in `local-artifacts/apk-archive/`.
- Build the candidate from clean Boost.
- Rewrite the candidate package to `com.rubenmayayo.reddit.dev`.
- Preserve label `Boost DEV`.
- Sign with the DEV test keystore.
- Install over `com.rubenmayayo.reddit.dev`.
- Test login, media routing, downloads, notifications, Crashlytics noise, and logcat.
- Archive the tested APK and logs.
- If the candidate fails, reinstall the known-good rollback APK.

## Normal package replacement

Normal package replacement means installing over:

- `com.rubenmayayo.reddit`

This is not the default strategy.

Only use it when explicitly approved and the risk to the normal Boost install/data is accepted, or when signing/Manager flow guarantees a safe update path.

## Release validation minimum

Before any release:

- Build MPP with `./gradlew :patches:buildAndroid`.
- Verify MPP contains `classes.dex`.
- Verify MPP contains `extensions/boostforreddit.mpe`.
- Verify Manager metadata is concise and current-release only.
- Verify README SHA matches MPP.
- Verify no APK/build artifacts are staged.
- Verify required marker strings.
- Verify old release values are gone.
- Verify remote release asset SHA and contents after publish.

For Boost target SDK changes, runtime validation must include:

- installed target SDK
- requested permissions
- `POST_NOTIFICATIONS`
- appops notification state
- notification channels
- actual download notification visibility
- relevant logcat

For Crashlytics changes, runtime validation must include:

- no `CrashlyticsRegistrar` marker
- no FirebaseCrashlytics startup network noise
- no firebase-settings Crashlytics spam
- app still launches normally
- FirebaseApp/FirebaseInitProvider/Analytics may remain alive

## Current chosen strategy

Default strategy from this point:

- Keep `com.rubenmayayo.reddit` untouched.
- Use `com.rubenmayayo.reddit.dev` as the only Boost development/runtime test app.
- Build candidates APK-only first.
- Archive candidates locally.
- Convert runtime candidates to `com.rubenmayayo.reddit.dev`.
- Install over Boost DEV for testing.
- Roll back Boost DEV from APK archive when needed.
- Do not create extra app variants by default.
