# Boost validation policy

## Source of truth

Functional validation for Boost for Reddit uses the real normal Boost app patched through Morphe or Morphe Manager.

The normal user path is the only valid source of truth for login, inline media, media routing, downloads, notifications, and visible app behavior.

## APK-lab scope

APK-lab may be used only for narrow technical smoke checks:

- the APK builds
- the APK installs when explicitly intended
- the app launches
- target SDK and manifest permissions are correct
- obvious fatal crashes are absent
- Crashlytics startup noise is absent or reduced
- release artifacts contain the expected files and marker strings

A repackaged, cloned, rewritten, or otherwise artificial package is not equivalent to the normal user path. Failures in such builds are diagnostic only.

## Stop rule

If the normal Morphe-patched Boost app works and there is no matching user report, do not treat lab-only behavior as a release blocker.

Do not spend release time debugging behavior that exists only in artificial local builds.

## Release validation

Before release, verify:

- MPP is built with `./gradlew :patches:buildAndroid`
- MPP contains `classes.dex`
- MPP contains `extensions/boostforreddit.mpe`
- Manager-facing description is short and current-release only
- README SHA matches the MPP
- no APK/build artifacts are staged
- required marker strings are present
- old release values are gone
- remote release asset SHA and contents are verified after publish

For Boost target SDK changes, also verify:

- installed target SDK
- requested permissions
- `POST_NOTIFICATIONS`
- notification channel behavior
- actual completed-download notification visibility
- relevant logcat

For Crashlytics changes, also verify:

- no `CrashlyticsRegistrar` marker
- no FirebaseCrashlytics startup network noise
- no firebase-settings Crashlytics spam
- app still launches normally
