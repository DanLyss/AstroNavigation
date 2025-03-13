import os
import subprocess
import time
from astropy.io import fits

from converter_flat_hor import Star_Cluster
from astrometry_to_cluster import extract_am_output

ARCSECPERPIX = 220
delta = 10 #deviation
output_file = "cluster_output.txt"


wsl_mode = "wsl" if os.name == "nt" else ""
astrometry_path = "/usr/bin/solve-field"
image_path =  r"/mnt/c/Users/Dan/Downloads/photo_2024-07-13_22-07-41.jpg"

# 🔹 Running `solve-field` on the image
def run_solve_field():
    command = f"""{wsl_mode} solve-field \
    --scale-low {ARCSECPERPIX-delta} --scale-high {ARCSECPERPIX+delta} --scale-units arcsecperpix --overwrite {image_path}"""
    subprocess.run(command, shell=True)

# 🔹 Analyzing `.corr` file (detected stars)
def analyze_corr():
    corr_file = image_path.replace(".jpg", ".corr")
    if not os.path.exists(corr_file):
        print("Error: `.corr` file not found!")
        return
    to_load_in_cluster = []
    with fits.open(corr_file) as hdul:
        data = hdul[1].data
        for row in data:
            to_load_in_cluster.append({"field_x": row['field_x'], "field_y" : row['field_y'],
                                       "field_ra" : row['field_ra'], "field_dec": row['field_dec'],
                                       "match_weight": row['match_weight']})
    return to_load_in_cluster

# 🔹 Main Execution
def main():
    #run_solve_field()
    time.sleep(2)  # Wait for processing
    cluster = extract_am_output(analyze_corr())
    #write cluster to the file
    cluster.write_to_file(output_file)
    print(f"Results saved to {output_file}")


if __name__ == "__main__":
    main()