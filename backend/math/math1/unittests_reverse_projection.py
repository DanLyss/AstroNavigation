import unittest
import numpy as np
import matplotlib.pyplot as plt
from AstroNavigation.backend.math.Star_Star_Cluster import Star, Star_Cluster
from AstroNavigation.backend.math.test_cases_Astrometry import TEST_CASES


#NOTE: DATE OF PHOTOS - 9/11/2001, location - Bremen, Bremen time 22:00


class TestProjectionOnDatasets(unittest.TestCase):
    DEBUG = False # Set to False to disable plots

    def setUp(self):
        self.test_cases = TEST_CASES

    def test_all_cases(self):
        for idx, case in enumerate(self.test_cases):
            with self.subTest(f"Test Case #{idx + 1}"):
                stars = []
                for i in range(len(case["pixel_coords"])):
                    x, y = case["pixel_coords"][i]
                    if type(case["RA_Dec"][i][0]) == float:
                        ra_deg = case["RA_Dec"][i][0]
                        dec_deg = case["RA_Dec"][i][1]
                    else:
                        ra_h, ra_m, ra_s = case["RA_Dec"][i][0]
                        dec_d, dec_m, dec_s = case["RA_Dec"][i][1]

                        ra_deg = (ra_h * 3600 + ra_m * 60 + ra_s) / 86400 * 360
                        dec_deg = dec_d + dec_m / 60 + dec_s / 3600

                    stars.append(Star(x, y, np.deg2rad(ra_deg), np.deg2rad(dec_deg)))

                pos_angle_rad = np.deg2rad(case["positional_angle_deg"])
                rotation_angle = np.deg2rad(case["rotation_angle_deg"])
                cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle)

                true_vals = np.array(case["AzAlt_deg"])
                computed_vals = np.array([
                    np.rad2deg([star.Az, star.Alt]) for star in cluster.stars
                ])

                az_offset = true_vals[0, 0] - computed_vals[0, 0]
                computed_vals[:, 0] += az_offset

                error = np.linalg.norm(computed_vals - true_vals, axis=1)
                mean_error = np.mean(error)

                if self.DEBUG:
                    self._plot_debug(true_vals, computed_vals, idx + 1)

                print(f"[Test Case {idx+1}] Average angular error: {mean_error:.2f}°")
                self.assertLess(
                    mean_error,
                    0.6,
                    f"Average angular error too high ({mean_error:.2f}°)"
                )

    def test_robustness_to_pixel_noise_on_one_star(self):
        noise_factor = 0.5

        for idx, case in enumerate(self.test_cases): #test only on 10 stars datasets
            if len(case["pixel_coords"]) != 10:
                continue
            for star_idx in range(len(case["pixel_coords"])):
                with self.subTest(f"Test Case #{idx + 1}, Star #{star_idx + 1} perturbed"):
                    stars = []

                    for i in range(len(case["pixel_coords"])):
                        x, y = case["pixel_coords"][i]

                        if i == star_idx:
                            x *= 1 + noise_factor * np.random.choice([-1, 1])
                            y *= 1 + noise_factor * np.random.choice([-1, 1])

                        if type(case["RA_Dec"][i][0]) == float:
                            ra_deg = case["RA_Dec"][i][0]
                            dec_deg = case["RA_Dec"][i][1]
                        else:
                            ra_h, ra_m, ra_s = case["RA_Dec"][i][0]
                            dec_d, dec_m, dec_s = case["RA_Dec"][i][1]

                            ra_deg = (ra_h * 3600 + ra_m * 60 + ra_s) / 86400 * 360
                            dec_deg = dec_d + dec_m / 60 + dec_s / 3600
                            
                        stars.append(Star(x, y, np.deg2rad(ra_deg), np.deg2rad(dec_deg)))

                    pos_angle_rad = np.deg2rad(case["positional_angle_deg"])
                    rotation_angle = np.deg2rad(case["rotation_angle_deg"])
                    cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle)

                    true_vals = np.array(case["AzAlt_deg"])
                    computed_vals = np.array([
                        np.rad2deg([star.Az, star.Alt]) for star in cluster.stars
                    ])

                    az_offset = true_vals[0, 0] - computed_vals[0, 0]
                    computed_vals[:, 0] += az_offset

                    error = np.linalg.norm(computed_vals - true_vals, axis=1)
                    mean_error = np.mean(error)

                    print(
                        f"[Test Case {idx + 1}, Star {star_idx + 1}] Mean angular error with noise: {mean_error:.2f}°")

                    self.assertLess(
                        mean_error,
                        1,
                        f"Robustness test failed (mean error: {mean_error:.2f}°)"
                    )

                    if self.DEBUG:
                        self._plot_debug(true_vals, computed_vals, f"{idx + 1} (Star {star_idx + 1} noisy)")

    def test_noise_on_all_pixels(self):
        noise_factor = 0.05

        for idx, case in enumerate(self.test_cases):
            with self.subTest(f"Test Case #{idx + 1} with noise on all pixels"):
                stars = []

                for i in range(len(case["pixel_coords"])):
                    x, y = case["pixel_coords"][i]

                    x *= 1 + noise_factor * np.random.uniform(-1, 1)
                    y *= 1 + noise_factor * np.random.uniform(-1, 1)

                    if type(case["RA_Dec"][i][0]) == float:
                        ra_deg = case["RA_Dec"][i][0]
                        dec_deg = case["RA_Dec"][i][1]
                    else:
                        ra_h, ra_m, ra_s = case["RA_Dec"][i][0]
                        dec_d, dec_m, dec_s = case["RA_Dec"][i][1]

                        ra_deg = (ra_h * 3600 + ra_m * 60 + ra_s) / 86400 * 360
                        dec_deg = dec_d + dec_m / 60 + dec_s / 3600

                    stars.append(Star(x, y, np.deg2rad(ra_deg), np.deg2rad(dec_deg)))

                pos_angle_rad = np.deg2rad(case["positional_angle_deg"])
                rotation_angle = np.deg2rad(case["rotation_angle_deg"])
                cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle)

                true_vals = np.array(case["AzAlt_deg"])
                computed_vals = np.array([
                    np.rad2deg([star.Az, star.Alt]) for star in cluster.stars
                ])

                az_offset = true_vals[0, 0] - computed_vals[0, 0]
                computed_vals[:, 0] += az_offset

                error = np.linalg.norm(computed_vals - true_vals, axis=1)
                mean_error = np.mean(error)

                print(f"[Test Case {idx + 1}] Mean angular error with 5% noise: {mean_error:.2f}°")

                self.assertLess(
                    mean_error,
                    1,
                    f"Mean error too high with noise ({mean_error:.2f}°)"
                )

                if self.DEBUG:
                    self._plot_debug(true_vals, computed_vals, f"{idx + 1} (5% noise)")

    def _plot_debug(self, true_vals, computed_vals, test_num):
        plt.figure(figsize=(8, 6))
        plt.scatter(computed_vals[:, 0], computed_vals[:, 1], color='blue', label='Predicted Az/Alt')
        plt.scatter(true_vals[:, 0], true_vals[:, 1], color='red', label='Ground Truth Az/Alt')
        plt.xlabel("Azimuth (degrees)")
        plt.ylabel("Altitude (degrees)")
        plt.title(f"Test Case #{test_num}: Az/Alt Prediction vs Ground Truth")
        plt.legend()
        plt.grid(True)
        plt.tight_layout()
        plt.show()


if __name__ == "__main__":
    unittest.main()
