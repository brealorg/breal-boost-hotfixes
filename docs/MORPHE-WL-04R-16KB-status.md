# MORPHE WL-04 / WL-04R / WL-04E status

Status date: 2026-06-30T10:40:25+02:00

## Corrected classification

WL-04R is **not** closed as true 16 KB runtime validated.

Correct status:

- Static 16 KB hardening: **validated**
- DEV runtime on current Pixel 6 / Android 17 target: **validated**
- Current runtime page size on tested physical target: **4096 bytes**
- True 16 KB runtime validation: **pending**
- Normal package touched during WL-04R/WL-04E validation: **no**
- Manager lane used during WL-04R/WL-04E validation: **no**

## Why this correction exists

The earlier closeout wording could be read as overclaiming 16 KB runtime validation. That is not accepted.

A result may only be labelled true 16 KB runtime validated when a target reports:

```
getconf PAGESIZE=16384
```

and the relevant DEV candidate is installed, launched, exercised, and logcat-checked on that same target.

## What has been validated

The WL-04R remediation removes the previously problematic RenderScript support path from the DEV remediation candidate:

- `he.c / BlurTransformation.b(Bitmap)` no longer uses RenderScript.
- Google ads overlay RenderScript blur path is neutralized.
- `librsjni_androidx.so` and `libRSSupport.so` are removed from APK ABIs.
- Remaining arm64 native libraries in the validated candidate were checked for 16 KB-compatible LOAD alignment.
- The final DEV APK passes `zipalign -c -P 16 -v 4`.

Latest final DEV APK observed by this correction gate:

```
local-artifacts/boost-dev-overwrite-candidates/20260630-103249-wl04r-16kb-appside-dev-committed-final/dev/boost-devclone.apk
5be5a05a2f13dc8872a1a1cd54669724487dc3542282407b15bca55b11adbfd2  local-artifacts/boost-dev-overwrite-candidates/20260630-103249-wl04r-16kb-appside-dev-committed-final/dev/boost-devclone.apk
```

## Current WL-04E runtime-target status

Current online adb targets observed by this correction gate:

```

### 192.168.1.248:35071
model=Pixel 6
ro.product.cpu.abilist=arm64-v8a,armeabi-v7a,armeabi
ro.build.version.release=17
ro.build.version.sdk=37
getconf PAGESIZE=4096
```

If no target reports `getconf PAGESIZE=16384`, WL-04E remains open.

## Required next step for true 16 KB runtime validation

WL-04E must continue until one of these is available:

1. a physical device with `getconf PAGESIZE=16384`, or
2. an Android emulator/AVD configured for 16 KB page size.

Only then may the DEV candidate be installed and runtime-tested for true 16 KB validation.
