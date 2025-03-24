import unittest
import numpy as np
from AstroNavigation.backend.math.Star_Star_Cluster import Star, Star_Cluster
from AstroNavigation.backend.math.math2.latt_long_calc import longitude
from latt_long_calc import mean_lattitude, norm, mean_longitude
from AstroNavigation.backend.math.test_cases_Astrometry import TEST_CASES


class TestLatitudeAzimuthEstimation(unittest.TestCase):
    def setUp(self):
        self.test_cases = TEST_CASES

    def test_lat_az(self):
        for idx, case in enumerate(self.test_cases):
            with self.subTest(f"Test Case #{idx + 1}"):
                stars = []
                for i in range(len(case["pixel_coords"])):
                    x, y = case["pixel_coords"][i]
                    ra_h, ra_m, ra_s = case["RA_Dec"][i][0]
                    dec_d, dec_m, dec_s = case["RA_Dec"][i][1]

                    ra_deg = (ra_h * 3600 + ra_m * 60 + ra_s) / 86400 * 360
                    dec_deg = dec_d + dec_m / 60 + dec_s / 3600

                    stars.append(Star(x, y, np.deg2rad(ra_deg), np.deg2rad(dec_deg)))

                pos_angle_rad = np.deg2rad(case["positional_angle_deg"])
                rotation_angle = np.deg2rad(case["rotation_angle_deg"])
                cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle)
                error = np.deg2rad(np.abs(np.deg2rad(53.0577)-cluster.phi))
                self.assertLess(error,
                                0.01,
                                f"Latt_Az test failed (mean error: {np.rad2deg(error)}°)")

                print(f"[Latt Test Case {idx + 1}] Lattitude error: {error * 57.3:.7f}°")

    def test_longitude(self):
        # Дата и время съёмки (фиксированное)
        cur_time = "2001-09-11T22:00:00+02:00"

        true_long_deg = 8.8
        true_long_rad = np.deg2rad(true_long_deg)

        for idx, case in enumerate(self.test_cases):
            with self.subTest(f"Longitude Test Case #{idx + 1}"):
                stars = []
                for i in range(len(case["pixel_coords"])):
                    x, y = case["pixel_coords"][i]
                    ra_h, ra_m, ra_s = case["RA_Dec"][i][0]
                    dec_d, dec_m, dec_s = case["RA_Dec"][i][1]

                    ra_deg = (ra_h * 3600 + ra_m * 60 + ra_s) / 86400 * 360
                    dec_deg = dec_d + dec_m / 60 + dec_s / 3600

                    stars.append(Star(x, y, np.deg2rad(ra_deg), np.deg2rad(dec_deg)))

                pos_angle_rad = np.deg2rad(case["positional_angle_deg"])
                rotation_angle = np.deg2rad(case["rotation_angle_deg"])
                cluster = Star_Cluster(stars, pos_angle_rad, rotation_angle)
                longitude = cluster.long
                error = np.abs(8.80 - np.rad2deg(longitude))

                self.assertLess(
                    error,
                1,
                    f"Longitude estimation error too high ({error:.2f}°)"
                )

                print(f"[Longitude Test {idx + 1}] Longitude error: {error:.3f}°")

if __name__ == "__main__":
    unittest.main()
