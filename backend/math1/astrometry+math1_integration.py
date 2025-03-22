import os
import subprocess
import time
from astropy.io import fits
from converter_flat_hor import Star, Star_Cluster
import matplotlib.pyplot as plt
import numpy as np

POS_ANGLE = 10 # Alt of center of picture
ROT_ANGLE = 20 # Angle of rotation around positional axis
threshold = 0.99 # Relative level of uncertainty of star detection from which we take star from Astrometry analysis

def extract_am_output(AM_output) -> Star_Cluster:
    stars = []
    for star_data in AM_output:
        if star_data["match_weight"] > threshold:
            stars.append(Star(star_data["field_x"],
                              -star_data["field_y"], #I work in coordinate system which is not inverted, while on pictures y is always inverted
                              np.deg2rad(star_data["field_ra"]),
                              np.deg2rad(star_data["field_dec"])))
    return Star_Cluster(stars[:10], positional_angle=np.deg2rad(POS_ANGLE), rotation_angle=np.deg2rad(ROT_ANGLE))


ARCSECPERPIX = 220 #arc seconds per pixel scale (take from phone paramters)
delta = 10 #level of uncertainty in the arcsecpix value
output_file = "cluster_output.txt" #here computed data about star cluster is written


wsl_mode = "wsl" if os.name == "nt" else ""
astrometry_path = "/usr/bin/solve-field"
image_path =  r"/mnt/c/Users/Dan/Downloads/im333.jpg" #here one shall put an image to be processed location

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
    #run_solve_field() #uncomment this line to run astrometry analysis. Then comment it back when working with same image not to perform useless computations
    time.sleep(2)  # Wait for processing
    cluster = extract_am_output(analyze_corr())
    #write cluster to the file
    cluster.write_to_file(output_file)
    print(f"Results saved to {output_file}")


if __name__ == "__main__":
    main()