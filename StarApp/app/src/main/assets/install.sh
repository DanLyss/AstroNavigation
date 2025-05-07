#!/data/data/com.termux/files/usr/bin/bash
set -e  # Stop script on any error

astro_dir="$HOME/astro"

echo "📦 Creating directories..."
mkdir -p "$astro_dir/astro/bin"
mkdir -p "$astro_dir/astro/lib"
mkdir -p "$astro_dir/input"
mkdir -p "$astro_dir/output"
mkdir -p "$astro_dir/index"

cd "$astro_dir" || exit 1

echo "📂 Moving archives and scripts from shared storage..."
if ls ~/storage/shared/Android/media/com.example.cameralong/astro/*.zip 1>/dev/null 2>&1; then
    mv -f ~/storage/shared/Android/media/com.example.cameralong/astro/*.zip "$astro_dir/"
else
    echo "⚠️ .zip files not found"
fi

if ls ~/storage/shared/Android/media/com.example.cameralong/astro/*.sh 1>/dev/null 2>&1; then
    mv -f ~/storage/shared/Android/media/com.example.cameralong/astro/*.sh "$HOME/" || true
else
    echo "⚠️ .sh files not found"
fi

echo "📦 Unpacking archives..."
if [ -f astrometry.zip ]; then
    unzip -o astrometry.zip -d "$astro_dir/astro/"
else
    echo "❌ File astrometry.zip not found!"
fi

if [ -f index_files.zip ]; then
    unzip -o index_files.zip -d "$astro_dir/index/"
else
    echo "❌ File index_files.zip not found!"
fi

echo "⚙️ Creating configuration file..."
cfg="$astro_dir/astro/bin/my.cfg"
cat <<EOF > "$cfg"
index $astro_dir/index/index-4117.fits
index $astro_dir/index/index-4118.fits
index $astro_dir/index/index-4119.fits
EOF

chmod +x "$astro_dir/astro/bin/solve-field"

echo "✅ Astrometry installed"
exit 0
