import unittest
import numpy as np
from astropy.coordinates import SkyCoord, AltAz, EarthLocation
from astropy.time import Time
import astropy.units as u
from astrometry_math1_integration import process_astrometry_image


def get_astropy_altaz(cluster, local_time_str, utc_offset, lat, lon, height=0):
    obs_time = Time(local_time_str) - utc_offset * u.hour
    location = EarthLocation(lat=lat * u.deg, lon=lon * u.deg, height=height * u.m)

    skycoords = SkyCoord(
        ra=[np.rad2deg(star.RA) for star in cluster.stars] * u.deg,
        dec=[np.rad2deg(star.dec) for star in cluster.stars] * u.deg,
        frame="icrs"
    )

    altaz_frame = AltAz(obstime=obs_time, location=location)
    altaz = skycoords.transform_to(altaz_frame)

    return np.column_stack((altaz.az.deg, altaz.alt.deg))


class TestAstrometryGroundTruth(unittest.TestCase):
    def setUp(self):
        self.photo_cases = [
            {
                "name": "Bremen Evening Sky",
                "image_path": "/mnt/c/Users/Dan/Downloads/imz.jpg",
                "local_time": "2025-03-24 00:55:04",
                "utc_offset": 3,
                "lat": 56.2783204,
                "lon": 43.9322983,
                "height": 10,
                "pos_angle": 95.60000000000001,
                "rot_angle": 3.6,
                "seed": 0,
                "run_astrometry": True,
                "arcsec_per_pix": 500,
                "delta": 500,
                "match_weight_threshold": 0.99,
                "max_star_count": 10
            },
            {
                "name": "Paris Morning Sky",
                "image_path": "/mnt/c/Users/Dan/Downloads/photo_paris.jpg",
                "local_time": "2002-06-01 05:15:00",
                "utc_offset": 2,
                "lat": 48.8566,
                "lon": 2.3522,
                "height": 35,
                "pos_angle": 25.0,
                "rot_angle": 12.0,
                "seed": 1,
                "run_astrometry": True,
                "arcsec_per_pix": 220,
                "delta": 10,
                "match_weight_threshold": 0.995,
                "max_star_count": 10
            }
        ]

    def test_photos_against_ground_truth(self):
        for case in self.photo_cases:
            with self.subTest(case=case["name"]):
                cluster = process_astrometry_image(
                    image_path=case["image_path"],
                    output_file=f"{case['name'].replace(' ', '_')}_cluster.txt",
                    arcsec_per_pix=case["arcsec_per_pix"],
                    delta=case["delta"],
                    pos_angle_deg=case["pos_angle"],
                    rot_angle_deg=case["rot_angle"],
                    match_weight_threshold=case["match_weight_threshold"],
                    max_star_count=case["max_star_count"],
                    run_astrometry=case["run_astrometry"],
                    seed=case["seed"]
                )

                self.assertIsNotNone(cluster, "Cluster was not created")

                gt_altaz = get_astropy_altaz(
                    cluster,
                    local_time_str=case["local_time"],
                    utc_offset=case["utc_offset"],
                    lat=case["lat"],
                    lon=case["lon"],
                    height=case["height"]
                )

                pred_altaz = np.array([[np.rad2deg(s.Az), np.rad2deg(s.Alt)] for s in cluster.stars])

                # Align azimuth
                az_offset = gt_altaz[0, 0] - pred_altaz[0, 0]
                pred_altaz[:, 0] += az_offset

                errors = np.linalg.norm(pred_altaz - gt_altaz, axis=1)
                mean_error = np.mean(errors)

                print(f"\n📷 {case['name']}")
                for i, err in enumerate(errors):
                    print(f"  Star {i+1}: error = {err:.2f}°")
                print(f"  ✅ Mean error: {mean_error:.2f}°\n")

                self.assertLess(
                    mean_error,
                    1.0,
                    f"Mean error too high for {case['name']} ({mean_error:.2f}°)"
                )


if __name__ == "__main__":
    unittest.main()
