import os
import subprocess
import time
from astropy.io import fits
from numpy.core.defchararray import center

from AstroNavigation.backend.math.Star_Star_Cluster import Star, Star_Cluster
import numpy as np
import matplotlib.pyplot as plt


def process_astrometry_image(
        image_path: str,
        astrometry_path: str = "/usr/bin/solve-field",
        output_file: str = "cluster_output.txt",
        arcsec_per_pix: float = 220,
        delta: float = 10,
        pos_angle_deg: float = 10,
        rot_angle_deg: float = 90,
        match_weight_threshold: float = 0.995,
        max_star_count: int = 10,
        run_astrometry: bool = True,
        seed: int = 0
) -> Star_Cluster | None:
    np.random.seed(seed)

    wsl_mode = "wsl" if os.name == "nt" else ""

    def run_solve_field():
        command = f"""{wsl_mode} {astrometry_path} \
--scale-low {arcsec_per_pix - delta} --scale-high {arcsec_per_pix + delta} \
--scale-units arcsecperpix --overwrite "{image_path}" """
        print(f"Running astrometry command:\n{command}")
        subprocess.run(command, shell=True)

    def analyze_corr(corr_path):
        if not os.path.exists(corr_path):
            print("Error: `.corr` file not found!")
            return []

        with fits.open(corr_path) as hdul:
            data = hdul[1].data
            stars_data = []
            for row in data:
                stars_data.append({
                    "field_x": row['field_x'],
                    "field_y": row['field_y'],
                    "field_ra": row['field_ra'],
                    "field_dec": row['field_dec'],
                    "match_weight": row['match_weight']
                })
            return stars_data

    def extract_cluster_from_am_output(AM_output):
        stars = []
        x = []
        y = []

        for star_data in AM_output:
            if star_data["match_weight"] > match_weight_threshold:
                x.append(star_data["field_x"])
                y.append(-star_data["field_y"])
        offset = min(zip(x, y), key=lambda p: (p[0] - np.mean(x)) ** 2 + (p[1] - np.mean(y)) ** 2)
        base_star = None
        for star_data in AM_output:
            if star_data["match_weight"] > match_weight_threshold:
                stars.append(Star(
                    star_data["field_x"] - offset[0],
                    -star_data["field_y"] + offset[1],  # Flip Y
                    np.deg2rad(star_data["field_ra"]),
                    np.deg2rad(star_data["field_dec"])
                ))
                if star_data["field_x"] - offset[0] == 0 and -star_data["field_y"] + offset[1] == 0:
                    base_star = stars[-1]

        if len(stars) < max_star_count or base_star is None:
            raise ValueError(f"Not enough high-confidence stars found (only {len(stars)})")

        selected_stars = np.random.choice(stars, max_star_count, replace=False)
        if not base_star in selected_stars:
            selected_stars.pop()
            selected_stars.append(base_star)
        return Star_Cluster(selected_stars,
                            positional_angle=np.deg2rad(pos_angle_deg),
                            rotation_angle=np.deg2rad(rot_angle_deg),
                            timeGMT="2025-04-25T23:46:00+02:00")

    # --- Main execution ---
    if run_astrometry:
        run_solve_field()
        time.sleep(2)  # Let solve-field finish

    # corr_path = image_path.replace(".jpg", ".corr")
    corr_path = image_path
    AM_output = analyze_corr(corr_path)
    if not AM_output:
        print("No star data found, exiting.")
        return None

    cluster = extract_cluster_from_am_output(AM_output)
    cluster.write_to_file(output_file)
    print(f"Cluster written to: {output_file}")
    return cluster


if __name__ == "__main__":
    cluster = process_astrometry_image(
        image_path="input.jpg",
        output_file="cluster_output.txt",
        arcsec_per_pix=500,
        delta=499,
        pos_angle_deg=81.96182813671038,
        rot_angle_deg=29.98584423820313,
        match_weight_threshold=0.995,
        max_star_count=15,
        run_astrometry=False,
        seed=0
    )

    if cluster is not None:
        print("Cluster loaded with", len(cluster.stars), "stars")
        # print("latitude = ", np.rad2deg(cluster.phi), "actual latitude = ", 53.165437)
        # print("longitude = ", np.rad2deg(cluster.long), "actual longitude = ", 8.6555426)
