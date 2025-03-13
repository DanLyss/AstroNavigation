from converter_flat_hor import Star, Star_Cluster
import numpy as np

POS_ANGLE = 60
X_LENGTH = 1260
Y_LENGTH = 840
threshold = 0.9

def extract_am_output(AM_output) -> Star_Cluster:
    stars = []
    for star_data in AM_output:
        if star_data["match_weight"] > threshold:
            stars.append(Star(star_data["field_x"] - X_LENGTH / 2,
                              star_data["field_y"] - Y_LENGTH / 2,
                              np.deg2rad(star_data["field_ra"]),
                              np.deg2rad(star_data["field_dec"])))
    return Star_Cluster(stars, pix_length_x=X_LENGTH, pix_length_y=Y_LENGTH,
                        positional_angle=np.deg2rad(POS_ANGLE))
