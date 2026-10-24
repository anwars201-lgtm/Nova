#!/data/user/0/com.androidharness.app/files/linux/bin/bash
# Build APK Nova di device ini.
# Panggil dari root workspace: bash scripts/build.sh [assembleDebug|assembleRelease]
#
# Kenapa env ini diset di sini: shell aplikasi tidak mengekspor JAVA_HOME,
# dan Gradle/Toolchain default mengarah ke direktori Termux yang tidak ada.
set -e
set -o pipefail


PREFIX="${PREFIX:-/data/user/0/com.androidharness.app/files/linux}"
PREFIX="${PREFIX%/usr}"
JAVA_HOME="${JAVA_HOME:-$PREFIX/lib/jvm/java-17-openjdk}"
export JAVA_HOME

SCRATCH="${HARNESS_SCRATCH:-/data/user/0/com.androidharness.app/files/.harness-scratch}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$SCRATCH/gradle-home}"
export TMPDIR="${TMPDIR:-$SCRATCH/tmp}"
mkdir -p "$TMPDIR"

if [ "$#" -eq 0 ]; then
    set -- assembleDebug
fi

echo "== gradle $*"
gradle --no-daemon --console=plain "$@" 2>&1 | tail -30

out_dir="app/build/outputs/apk"
apk=$(find "$out_dir" -name '*.apk' -newer STATUS.md 2>/dev/null | head -1)
[ -z "$apk" ] && apk=$(find "$out_dir" -name '*.apk' | head -1)

echo
echo "== hasil"
ls -l "$apk"
echo "sha256: $(sha256sum "$apk" | cut -d' ' -f1)"
echo "path  : $apk"
