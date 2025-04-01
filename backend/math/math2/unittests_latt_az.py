import unittest
import numpy as np
from AstroNavigation.backend.math.Star_Star_Cluster import Star, Star_Cluster
from AstroNavigation.backend.math.math2.latt_long_calc import longitude
from latt_long_calc import mean_lattitude, norm, mean_longitude
from AstroNavigation.backend.math.test_cases_Stellarium import TEST_CASES


class TestLatitudeEstimation(unittest.TestCase):
    def setUp(self):
        self.test_cases = TEST_CASES

    def test_lat_az(self):
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
                cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle, case["datetime"])
                error = np.deg2rad(np.abs(np.deg2rad(case["latitude"])-cluster.phi))
                self.assertLess(error,
                                0.01,
                                f"Latt_Az test failed (mean error: {np.rad2deg(error)}°)")

                print(f"[Latt Test Case {idx + 1}] Lattitude error: {error * 180 / np.pi:.7f}°")
                error_Az = np.deg2rad(np.abs(norm(case["AzAlt_deg"][0][0], 0, 360)-norm(np.rad2deg(cluster.stars[0].Az), 0, 360)))
                self.assertLess(
                    error_Az,
                    1,
                    f"Azimuth test failed (mean error: {np.rad2deg(error_Az)}°)"
                )
                print(f"[Azimuth Test Case {idx + 1}] Azimuth error: {error_Az:.7f}°")

    def test_longitude(self):

        for idx, case in enumerate(self.test_cases):
            with self.subTest(f"Longitude Test Case #{idx + 1}"):
                true_long_deg = case["longitude"]
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
                cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle, case["datetime"])
                longitude = cluster.long
                error = np.abs(true_long_deg - np.rad2deg(longitude))

                self.assertLess(
                    error,
                1,
                    f"Longitude estimation error too high ({error:.2f}°)"
                )

                print(f"[Longitude Test {idx + 1}] Longitude error: {error:.3f}°")

if __name__ == "__main__":
    unittest.main()