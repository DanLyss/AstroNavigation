import os
import subprocess
import time
from astropy.io import fits

# 📌 Configure Paths
wsl_mode = "wsl" if os.name == "nt" else ""  # Use WSL on Windows
astrometry_path = "/usr/bin/solve-field"
image_path = r"/mnt/c/Users/Dan/Downloads/photo_2023-08-17_00-00-37.jpg"  # Input image

# 🔹 Running `solve-field` on the image
def run_solve_field():
    print("🔍 Running Astrometry.net on the image...")
    command = f"""{wsl_mode} solve-field \
    --overwrite {image_path}"""

    subprocess.run(command, shell=True)
    print("✅ Image processing completed!")

# 🔹 Analyzing `.corr` file (detected stars)
def analyze_corr():
    corr_file = image_path.replace(".jpg", ".corr")
    if not os.path.exists(corr_file):
        print("❌ Error: `.corr` file not found!")
        return

    with fits.open(corr_file) as hdul:
        data = hdul[1].data
        print(f"🔎 Found {len(data)} matched stars.")

        print("\n📍 First 10 matched stars:")
        for row in data[:10]:
            print(f"X coord: {row['field_x']:.4f}, Y coord: {row['field_y']:.4f}, RA: {row['field_ra']:.4f}, DEC: {row['field_dec']:.4f}")

# 🔹 Main Execution
def main():
    run_solve_field()
    time.sleep(2)  # Wait for processing
    analyze_corr()

# 🚀 Run the script
if __name__ == "__main__":
    main()