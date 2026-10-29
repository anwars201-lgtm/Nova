#!/data/user/0/com.androidharness.app/files/linux/bin/bash
# Build APK Nova di device ini.
# Panggil dari root workspace: bash scripts/build.sh [task Gradle ...]
#
# Kenapa env ini diset di sini: shell aplikasi tidak mengekspor JAVA_HOME,
# dan Gradle/Toolchain default mengarah ke direktori Termux yang tidak ada.
#
# Sesuai Pasal 11 RULES.md:
#   - seluruh keluaran Gradle ditulis ke build.log (bukan dipipe ke tail),
#   - baris terakhir log adalah "EXIT=<kode>" yang menjadi vonis build,
#   - skrip mencetak 25 baris terakhir agar bisa dibaca tanpa membuka file,
#   - sha256 dan waktu ubah APK dicetak supaya artefak lama tidak lolos.
set -e

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

LOG="${BUILD_LOG:-build.log}"
# Log lama dihapus supaya baris EXIT= dari build sebelumnya tidak terbaca sebagai vonis.
rm -f "$LOG"

echo "== gradle $*"
"$JAVA_HOME/bin/java" -version >/dev/null 2>&1 || true

set +e
gradle --no-daemon --console=plain "$@" > "$LOG" 2>&1
code=$?
echo "EXIT=$code" >> "$LOG"
set -e

echo "--- 25 baris terakhir $LOG"
tail -25 "$LOG"

echo
echo "== vonis: EXIT=$code"

if [ "$code" -ne 0 ]; then
    echo "== build GAGAL, lihat baris error di atas atau di $LOG"
    exit "$code"
fi

out_dir="app/build/outputs/apk"
apk=$(find "$out_dir" -name '*.apk' 2>/dev/null | head -1)

if [ -z "$apk" ]; then
    echo "== tidak ada APK di $out_dir"
    exit 1
fi

echo
echo "== hasil"
ls -l "$apk"
echo "sha256: $(sha256sum "$apk" | cut -d' ' -f1)"
echo "path  : $apk"
echo "ubah  : $(date -r "$apk" '+%Y-%m-%d %H:%M:%S')"
