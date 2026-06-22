#!/usr/bin/env bash

set -u

usage() {
  cat <<'EOF'
Usage:
  scripts/archive-apk-candidate.sh \
    --apk /path/to/app.apk \
    --name boost-normal-smoke-baseline \
    [--apply-log /path/to/apply.log] \
    [--result-json /path/to/result.json] \
    [--notes "short note"]

Creates:
  local-artifacts/apk-archive/<timestamp>-<name>/
EOF
}

APK=""
NAME=""
APPLY_LOG=""
RESULT_JSON=""
NOTES=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --apk) APK="${2:-}"; shift 2 ;;
    --name) NAME="${2:-}"; shift 2 ;;
    --apply-log) APPLY_LOG="${2:-}"; shift 2 ;;
    --result-json) RESULT_JSON="${2:-}"; shift 2 ;;
    --notes) NOTES="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "BAD: unknown arg: $1"
      usage
      exit 1
      ;;
  esac
done

if [ -z "$APK" ] || [ -z "$NAME" ]; then
  usage
  exit 1
fi

if [ ! -f "$APK" ]; then
  echo "BAD: APK not found: $APK"
  exit 1
fi

SAFE_NAME="$(printf '%s' "$NAME" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9._-]+/-/g; s/^-+|-+$//g')"
STAMP="$(date +%Y%m%d-%H%M%S)"
ROOT="local-artifacts/apk-archive/${STAMP}-${SAFE_NAME}"
mkdir -p "$ROOT/apk"

APK_BASENAME="$(basename "$APK")"
cp -a "$APK" "$ROOT/apk/$APK_BASENAME"

if [ -n "$APPLY_LOG" ] && [ -f "$APPLY_LOG" ]; then
  cp -a "$APPLY_LOG" "$ROOT/apply.log"
fi

if [ -n "$RESULT_JSON" ] && [ -f "$RESULT_JSON" ]; then
  cp -a "$RESULT_JSON" "$ROOT/result.json"
fi

{
  echo "$NOTES"
} > "$ROOT/notes.txt"

SHA="$(sha256sum "$ROOT/apk/$APK_BASENAME" | awk '{print $1}')"
sha256sum "$ROOT/apk/$APK_BASENAME" > "$ROOT/sha256sums.txt"

PKG="$(aapt dump badging "$ROOT/apk/$APK_BASENAME" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n1)"
VERSION_CODE="$(aapt dump badging "$ROOT/apk/$APK_BASENAME" 2>/dev/null | sed -n "s/^package:.* versionCode='\([^']*\)'.*/\1/p" | head -n1)"
VERSION_NAME="$(aapt dump badging "$ROOT/apk/$APK_BASENAME" 2>/dev/null | sed -n "s/^package:.* versionName='\([^']*\)'.*/\1/p" | head -n1)"
TARGET_SDK="$(aapt dump badging "$ROOT/apk/$APK_BASENAME" 2>/dev/null | sed -n "s/^targetSdkVersion:'\([^']*\)'.*/\1/p" | head -n1)"
LABEL="$(aapt dump badging "$ROOT/apk/$APK_BASENAME" 2>/dev/null | sed -n "s/^application-label:'\([^']*\)'.*/\1/p" | head -n1)"
SIGNER_SHA256="$(apksigner verify --print-certs "$ROOT/apk/$APK_BASENAME" 2>/dev/null | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -n1)"

PATCH_MARKERS="$(mktemp)"
unzip -q "$ROOT/apk/$APK_BASENAME" -d "$ROOT/unzip-scan" 2>/dev/null || true

scan_marker() {
  local key="$1"
  local marker="$2"
  if find "$ROOT/unzip-scan" -maxdepth 1 -type f -name 'classes*.dex' -print0 2>/dev/null \
    | xargs -0 strings 2>/dev/null \
    | grep -Fq "$marker"; then
    printf '    "%s": true,\n' "$key" >> "$PATCH_MARKERS"
  else
    printf '    "%s": false,\n' "$key" >> "$PATCH_MARKERS"
  fi
}

scan_marker "modify_login_webview" "ModifyWebViewPatch"
scan_marker "redreader_user_agent" "org.quantumbadger.redreader/1.25.1"
scan_marker "redreader_redirect_uri" "redreader://rr_oauth_redir"
scan_marker "inline_giphy" "InlineGiphyCommentPreview"
scan_marker "boost_static_image_viewer" "openStaticImageViaBoost"
scan_marker "okhttp_request_hook" "OkHttpRequestHook"
scan_marker "firebase_crashlytics" "FirebaseCrashlytics"
scan_marker "crashlytics_registrar" "com.google.firebase.crashlytics.CrashlyticsRegistrar"

rm -rf "$ROOT/unzip-scan"

python3 <<PY > "$ROOT/manifest.json"
import json
from pathlib import Path

markers_text = Path("$PATCH_MARKERS").read_text().strip()
markers = {}
for line in markers_text.splitlines():
    line = line.strip().rstrip(",")
    if not line:
        continue
    key, value = line.split(":", 1)
    markers[key.strip().strip('"')] = value.strip().lower() == "true"

data = {
    "created_at": "$STAMP",
    "name": "$SAFE_NAME",
    "apk_file": "apk/$APK_BASENAME",
    "sha256": "$SHA",
    "package": "$PKG",
    "label": "$LABEL",
    "version_code": "$VERSION_CODE",
    "version_name": "$VERSION_NAME",
    "target_sdk": "$TARGET_SDK",
    "signer_sha256": "$SIGNER_SHA256",
    "has_apply_log": bool("$APPLY_LOG"),
    "has_result_json": bool("$RESULT_JSON"),
    "notes": "$NOTES",
    "marker_scan": markers,
}
print(json.dumps(data, indent=2, ensure_ascii=False))
PY

rm -f "$PATCH_MARKERS"

echo "===== archived APK candidate ====="
echo "DIR=$ROOT"
echo "APK=$ROOT/apk/$APK_BASENAME"
echo "MANIFEST=$ROOT/manifest.json"
echo
cat "$ROOT/manifest.json"
echo
echo "Terminal is still alive."
