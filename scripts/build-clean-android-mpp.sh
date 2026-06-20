#!/usr/bin/env bash
set -euo pipefail

VERSION="$(grep -E '^version\s*=' gradle.properties | sed -E 's/^version\s*=\s*//')"
MPP_NAME="patches-${VERSION}.mpp"
BASE_MPP="patches/build/libs/${MPP_NAME}"
PROGRAM_JAR="/tmp/morphe-patches-program-${VERSION}.jar"
DEX_OUT="/tmp/morphe-clean-d8-${VERSION}"
D8_LOG="/tmp/morphe-clean-d8-${VERSION}.log"

echo "===== build JVM/base mpp ====="
./gradlew clean :patches:jar

echo "===== locate SDK/android.jar/d8 ====="
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
ANDROID_JAR="$(find "$SDK/platforms" -name android.jar | sort -V | tail -1)"
D8_BIN="$(find "$SDK/build-tools" -type f -name d8 | sort -V | tail -1)"

test -f "$ANDROID_JAR"
test -x "$D8_BIN"

echo "ANDROID_JAR=$ANDROID_JAR"
echo "D8_BIN=$D8_BIN"

echo "===== collect :patches classpath ====="
cat > /tmp/print-patches-classpath.gradle <<'GRADLE'
allprojects {
    if (path == ':patches') {
        tasks.register("printPatchesCompileClasspath") {
            doLast {
                configurations.getByName("compileClasspath").files.each {
                    println(it.absolutePath)
                }
            }
        }
        tasks.register("printPatchesRuntimeClasspath") {
            doLast {
                configurations.getByName("runtimeClasspath").files.each {
                    println(it.absolutePath)
                }
            }
        }
    }
}
GRADLE

./gradlew -q :patches:printPatchesCompileClasspath -I /tmp/print-patches-classpath.gradle > /tmp/patches-compile-classpath.txt
./gradlew -q :patches:printPatchesRuntimeClasspath -I /tmp/print-patches-classpath.gradle > /tmp/patches-runtime-classpath.txt

cat /tmp/patches-compile-classpath.txt /tmp/patches-runtime-classpath.txt \
  | sort -u \
  | grep -v '^$' \
  > /tmp/patches-d8-classpath.txt

echo "classpath entries: $(wc -l < /tmp/patches-d8-classpath.txt)"

echo "===== run D8 cleanly ====="
rm -rf "$DEX_OUT"
mkdir -p "$DEX_OUT"
cp "$BASE_MPP" "$PROGRAM_JAR"

CP_ARGS=()
while IFS= read -r f; do
    [ -e "$f" ] && CP_ARGS+=(--classpath "$f")
done < /tmp/patches-d8-classpath.txt

"$D8_BIN" \
  --release \
  --min-api 23 \
  --lib "$ANDROID_JAR" \
  "${CP_ARGS[@]}" \
  --output "$DEX_OUT" \
  "$PROGRAM_JAR" \
  2>&1 | tee "$D8_LOG"

echo "===== D8 warning/error check ====="
if grep -iE 'warning|error|exception|failed' "$D8_LOG"; then
    echo "STOP: D8 was not clean."
    exit 1
fi

test -f "$DEX_OUT/classes.dex"

echo "===== inject clean dex into mpp ====="
zip -q -d "$BASE_MPP" 'classes*.dex' 2>/dev/null || true
zip -q -X -j "$BASE_MPP" "$DEX_OUT"/classes*.dex

echo "===== final mpp check ====="
unzip -l "$BASE_MPP" | grep -Ei 'classes.*\.dex|META-INF/MANIFEST.MF'

echo "===== final size/hash ====="
ls -lh "$BASE_MPP"
sha256sum "$BASE_MPP"

echo "CLEAN_D8_OK=$BASE_MPP"
