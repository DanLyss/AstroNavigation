#!/data/data/com.termux/files/usr/bin/bash
set -e

astro_dir="$HOME/astro"

INPUT_JPG="/storage/emulated/0/Android/media/com.example.cameralong/astro/input.jpg"
FITS_FILE="$astro_dir/input/photo_sil.fits"
BIN_DIR="$astro_dir/astro/astro/bin"
LIB_DIR="$astro_dir/astro/astro/lib"
OUT_DIR="$astro_dir/output"
CFG="$astro_dir/astro/bin/my.cfg"

echo "📤 Начинаем конвертацию JPEG → FITS..."

if ! command -v convert >/dev/null 2>&1; then
    echo "❌ ImageMagick 'convert' не установлен. Прервано."
    exit 1
fi

convert "$INPUT_JPG" -colorspace Gray "$FITS_FILE"
echo "✅ FITS файл создан: $FITS_FILE"

echo "🚀 Запуск solve-field..."

cd "$astro_dir/astro" || exit 1

LD_LIBRARY_PATH="$LIB_DIR" "$BIN_DIR/solve-field" \
  --fits-image "$FITS_FILE" \
  --dir "$OUT_DIR" \
  --temp-dir /data/data/com.termux/files/usr/tmp \
  -O -L 0.1 -H 180.0 -u dw -z 2 -p -y -9 \
  --uniformize 0 \
  --cpulimit 60 \
  --config "$CFG" \
  -v 2>&1 | tee "$OUT_DIR/solve_output.log"

echo "✅ solve-field завершён"
exit 0
