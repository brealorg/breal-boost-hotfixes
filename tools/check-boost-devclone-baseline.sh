#!/usr/bin/env bash
# Classify whether a Boost devclone is suitable for functional feed/login/media validation.
#
# Result semantics:
# - DEVCLONE_BASELINE_FUNCTIONAL_CANDIDATE:
#     install/runtime baseline is clean and content/API success has been explicitly proven.
#     This is still not release validation by itself; it only permits feed/media testing.
# - DEVCLONE_BASELINE_SMOKE_ONLY:
#     devclone is useful for install/settings/crash smoke, but not feed/login/media validation.
# - DEVCLONE_BASELINE_FAIL:
#     hard install/package/runtime problem.

FAIL=0
SMOKE_ONLY=0
mark_fail() { echo "FAIL: $*"; FAIL=1; }
mark_smoke_only() { echo "SMOKE_ONLY: $*"; SMOKE_ONLY=1; }

DEV_PKG="${1:-com.rubenmayayo.reddit.devfull}"
NORMAL_PKG="com.rubenmayayo.reddit"
DURATION="${DURATION:-90}"
CONTENT_PROOF="${MORPHE_DEVCLONE_CONTENT_PROOF:-}"

OUT="local-artifacts/devclone-baseline/$(date +%Y%m%d-%H%M%S)-${DEV_PKG}"
mkdir -p "$OUT"

echo "===== devclone baseline gate ====="
echo "DEV_PKG=$DEV_PKG"
echo "NORMAL_PKG=$NORMAL_PKG"
echo "DURATION=${DURATION}s"
echo "CONTENT_PROOF=${CONTENT_PROOF:-none}"
echo "OUT=$OUT"

echo
echo "===== installed package gate ====="
DEV_BASE="$(adb shell pm path "$DEV_PKG" 2>/dev/null | sed 's/^package://' | tr -d '\r' | head -1)"
NORMAL_BASE="$(adb shell pm path "$NORMAL_PKG" 2>/dev/null | sed 's/^package://' | tr -d '\r' | head -1)"

echo "DEV_BASE=$DEV_BASE"
echo "NORMAL_BASE=$NORMAL_BASE"

test -n "$DEV_BASE" || mark_fail "dev package is not installed"
test "$DEV_PKG" != "$NORMAL_PKG" || mark_fail "dev package equals normal package"

adb shell dumpsys package "$DEV_PKG" \
  | grep -E 'versionCode=|versionName=|targetSdk=|firstInstallTime=|lastUpdateTime=|signatures=' \
  | tee "$OUT/dev-package.txt" || true

if [ -n "$DEV_BASE" ]; then
  adb pull "$DEV_BASE" "$OUT/dev-base.apk" >/dev/null 2>&1 \
    && sha256sum "$OUT/dev-base.apk" | tee "$OUT/dev-base.sha256" \
    || mark_fail "could not pull dev base.apk"
fi

echo
echo "===== static package isolation gate ====="
if [ -f "$OUT/dev-base.apk" ]; then
  aapt dump badging "$OUT/dev-base.apk" \
    | grep -E "package:|targetSdkVersion|application-label:'" \
    | head -30 \
    | tee "$OUT/badging.txt" || true

  aapt dump badging "$OUT/dev-base.apk" | grep -q "package: name='$DEV_PKG'" \
    || mark_fail "APK package is not $DEV_PKG"

  aapt dump badging "$OUT/dev-base.apk" | grep -q "targetSdkVersion:'35'" \
    || mark_smoke_only "targetSdk is not 35 or not visible in badging"

  if strings "$OUT/dev-base.apk" | grep -F "$NORMAL_PKG.provider" >/dev/null 2>&1; then
    mark_fail "normal provider authority string still present"
  else
    echo "OK: no obvious normal provider authority string"
  fi

  if strings "$OUT/dev-base.apk" | grep -F "$NORMAL_PKG.easyphotopicker.fileprovider" >/dev/null 2>&1; then
    mark_fail "normal easyphotopicker authority string still present"
  else
    echo "OK: no obvious normal easyphotopicker authority string"
  fi

  if strings "$OUT/dev-base.apk" | grep -F "$NORMAL_PKG.firebaseinitprovider" >/dev/null 2>&1; then
    mark_fail "normal firebase provider authority string still present"
  else
    echo "OK: no obvious normal firebase provider authority string"
  fi
fi

echo
echo "===== patch marker gate ====="
if [ -f "$OUT/dev-base.apk" ]; then
  DEX_LIST="$(zipinfo -1 "$OUT/dev-base.apk" 'classes*.dex' 2>/dev/null | sort)"
  echo "$DEX_LIST"

  check_dex_marker() {
    marker="$1"
    found=0
    while IFS= read -r dex; do
      [ -n "$dex" ] || continue
      if unzip -p "$OUT/dev-base.apk" "$dex" | strings | grep -F "$marker" >/dev/null 2>&1; then
        echo "OK: marker present in $dex: $marker"
        found=1
      fi
    done <<EOF
$DEX_LIST
EOF

    if [ "$found" -eq 0 ]; then
      mark_smoke_only "marker not found in installed dev APK DEX: $marker"
    fi
  }

  check_dex_marker "BoostMorphePreferenceFragment"
  check_dex_marker "BoostMediaPreferences"
  check_dex_marker "morphe_boost_inline_previews"
  check_dex_marker "morphe_boost_left_align_previews"
  check_dex_marker "morphe_boost_hide_source_after_preview"
fi

echo
echo "===== runtime/content probe ====="
echo "ACTION:"
echo "  1. The app will be launched."
echo "  2. Try opening/refreshing feed or one subreddit."
echo "  3. If login is needed, complete it only if you are comfortable doing so."
echo "  4. Do not press Ctrl+C; this may look idle until timeout."
echo

adb logcat -c
adb shell am start -n "$DEV_PKG/com.rubenmayayo.reddit.ui.submissions.subreddit.MainActivity" || mark_fail "launch failed"

LOGCAT="$OUT/devclone-baseline-logcat.txt"
HITS="$OUT/devclone-baseline-hits.txt"

timeout "$DURATION"s adb logcat -v threadtime \
  | grep -Ei "InlineGiphy|BoostMorphePreferenceFragment|BoostMediaPreferences|morphe_boost_|MediaImageActivity|MediaVideoActivity|net\.dean\.jraw|RedditJRAW|Request returned non-successful status code|403 Blocked|Forbidden|Unauthorized|HTTP/[0-9.]+ 40[13]|ActivityNotFoundException|INSTALL_FAILED_CONFLICTING_PROVIDER|AndroidRuntime|FATAL EXCEPTION|NoClassDefFoundError|ClassNotFoundException|NoSuchMethodError|InflateException|$DEV_PKG" \
  | tee "$LOGCAT"

echo
echo "===== runtime hit summary ====="
grep -Ein "net\.dean\.jraw|RedditJRAW|Request returned non-successful status code|403 Blocked|Forbidden|Unauthorized|HTTP/[0-9.]+ 40[13]|ActivityNotFoundException|AndroidRuntime|FATAL EXCEPTION|NoClassDefFoundError|ClassNotFoundException|NoSuchMethodError|InflateException" "$LOGCAT" \
  | tee "$HITS" || true

echo
echo "===== classification ====="
if grep -Ei 'FATAL EXCEPTION|AndroidRuntime|NoClassDefFoundError|ClassNotFoundException|NoSuchMethodError|InflateException|ActivityNotFoundException' "$LOGCAT" >/dev/null 2>&1; then
  mark_fail "hard runtime blocker captured"
fi

if grep -Ei '403 Blocked|Request returned non-successful status code: 403|Forbidden|Unauthorized|HTTP/[0-9.]+ 40[13]' "$LOGCAT" >/dev/null 2>&1; then
  mark_smoke_only "content/API blocker captured; devclone is not valid for feed/media validation"
fi

if [ "$FAIL" -eq 0 ] && [ "$SMOKE_ONLY" -eq 0 ]; then
  if [ -n "$CONTENT_PROOF" ]; then
    echo "OK: explicit content/API proof supplied: $CONTENT_PROOF"
  else
    mark_smoke_only "no positive content/API success proof supplied; defaulting devclone to smoke-only"
  fi
fi

if [ "$FAIL" -ne 0 ]; then
  echo "RESULT=DEVCLONE_BASELINE_FAIL"
elif [ "$SMOKE_ONLY" -ne 0 ]; then
  echo "RESULT=DEVCLONE_BASELINE_SMOKE_ONLY"
else
  echo "RESULT=DEVCLONE_BASELINE_FUNCTIONAL_CANDIDATE"
fi

echo "LOGCAT=$LOGCAT"
echo "HITS=$HITS"
echo "OUT=$OUT"
echo "Terminal still alive."

if [ "$FAIL" -ne 0 ]; then
  exit 1
fi
exit 0
