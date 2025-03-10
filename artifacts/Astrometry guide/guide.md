# 🚀 Setting Up and Running **Astrometry.net** in Ubuntu (WSL/Linux)

This guide will take you through the **entire process** of installing Astrometry.net, configuring index files, and running `solve-field` to analyze an astronomical image.

---

## 📌 **1. Install Astrometry.net**
First, ensure your system is up-to-date and install **Astrometry.net**:

```bash
sudo apt update
sudo apt install astrometry.net
```

Verify the installation:
```bash
solve-field --help
```
✅ If you see the help output, the installation was successful.

---

## 📌 **2. Verify Index Files**
Astrometry.net requires **index files (`index-*.fits`)** to process images. We assume that the index files are **already downloaded** in a known directory.

Check if they exist:
```bash
ls -lh X/index-*.fits
```
If they are missing, download the appropriate index files from [Astrometry.net](http://data.astrometry.net/4100/).

---

## 📌 **3. Move Index Files to `/etc/astrometry/indexes/`**
We want to store the index files in **`/etc/astrometry/indexes/`** so that Astrometry.net can access them system-wide.

### **Step 1: Create the directory**
```bash
sudo mkdir -p /etc/astrometry/indexes
```

### **Step 2: Copy the index files**
```bash
sudo cp X/index-*.fits /etc/astrometry/indexes/
```

### **Step 3: Verify that the files are in the correct location**
```bash
ls -lh /etc/astrometry/indexes/
```
✅ You should see files like `index-4107.fits`, `index-4108.fits`, etc.

---

## 📌 **4. Configure Astrometry.net to Use Index Files**
We need to tell Astrometry.net where to find the index files by updating the configuration file.

### **Step 1: Add the index path to the configuration file**
```bash
echo "add_path /etc/astrometry/indexes" | sudo tee -a /etc/astrometry.cfg
```

### **Step 2: Verify the configuration**
```bash
cat /etc/astrometry.cfg
```
✅ You should see:
```
add_path /etc/astrometry/indexes
```

---

## 📌 **5. Running the Python Script**
To automate the execution of `solve-field`, use the following Python script:

```python
import os
import subprocess
import time
from astropy.io import fits

# 📌 Configure Paths
wsl_mode = "wsl" if os.name == "nt" else ""  # Use WSL on Windows
astrometry_path = "/usr/bin/solve-field"
image_path = r"put_here_path_to_your_image"  # Input image

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
```

Save this script as `astrometry_runner.py` and run it:
```bash
python3 astrometry_runner.py
```
If everything was done right, the script will print first 10 stars, identified on the picture in the format of their picture coorinates (x, y) and spherical coordinates (RA, dec)


## 📌 6. **Viewing -ngc.png File for Object Annotations**

After solve-field runs successfully, it generates a -ngc.png file that contains object annotations and recognized stars. You can open it and check how the image was processed

## 🎯 **Summary**
✅ Installed **Astrometry.net**  
✅ Moved index files to `/etc/astrometry/indexes/`  
✅ Configured **`/etc/astrometry.cfg`** to recognize the index files  
✅ Successfully **solved an astronomical image**  

🚀 **Now you can process your own astrophotography images!**.

**P.S**
If something went wrong during the installation you can still check the images as well as their annotated versions in this folder
```

