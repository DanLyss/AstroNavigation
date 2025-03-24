import unittest
import numpy as np
from AstroNavigation.backend.math.math1.converter_flat_hor import Star, Star_Cluster
from latt_long_calc import mean_lattitude, norm, mean_longitude
from AstroNavigation.backend.math.testdata import TEST_CASES


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
                true_vals = np.array([np.deg2rad(case["AzAlt_deg"][0][0]), np.deg2rad(53.16542)])

                computed_vals1 = np.array(mean_lattitude(cluster)[:-1])
                computed_vals2 = np.array([norm(computed_vals1[0]-np.pi, 0, 2 * np.pi), computed_vals1[1]])

                error = min(np.mean(np.abs(true_vals - computed_vals1)), np.mean(np.abs(true_vals - computed_vals2)))
                self.assertLess(error,
                                0.01,
                                f"Latt_Az test failed (mean error: {np.rad2deg(error):.2f}°)")

                print(f"[Latt Test Case {idx + 1}] Lattitude error: {np.rad2deg(min(np.abs(true_vals[1] - computed_vals1[1]), np.abs(true_vals[1] - computed_vals2[1]))):.5f}°")

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
                A1, phi = np.array(mean_lattitude(cluster)[:-1])[0], np.array(mean_lattitude(cluster)[:-1])[1]
                # Вычисляем долготу
                computed_long = mean_longitude(phi, A1, cluster, cur_time)
                # Считаем ошибку в градусах
                error = abs(np.rad2deg(computed_long - true_long_rad))
                # Приводим к [0, 360]
                if error > 180:
                    error = 360 - error

                self.assertLess(
                    error,
                2,  # допускаемая погрешность в градусах
                    f"Longitude estimation error too high ({error:.2f}°)"
                )

                print(f"[Longitude Test {idx + 1}] Longitude error: {error:.3f}°")

if __name__ == "__main__":
    unittest.main()
