import math

import numpy as np
import scipy as sp
from datetime import datetime, timezone

### Actual constants
iters = 5
obliquity = np.deg2rad(23 + 27 / 60 + 21.406 / 3600 - 46.837 / 3600 / 4)
year_length = 365.2422
day_eq = 79




def eq_setup(star, az):
    a = np.cos(star.Alt) ** 2 * np.cos(az) ** 2 + np.sin(star.Alt) ** 2
    b = 2 * np.cos(star.Alt) * np.cos(az) * np.sin(star.dec)
    c = np.sin(star.dec) ** 2 - np.sin(star.Alt) ** 2
    return a, b, c


def equation(star, az, phi):
    a, b, c = eq_setup(star, az)
    return phi ** 2 * a + phi * b + c


def norm(x, lower, upper):
    while x < lower: x += upper - lower
    while x > upper: x -= upper - lower
    return x

#Main function for latitude calculations
def mean_lattitude(cluster, hemisphere = "North"):
    def for_solve(x):
        return [equation(cluster.stars[i], x[0] + cluster.stars[i].Az - cluster.stars[0].Az, x[1]) for i in
                range(len(cluster.stars))]

    ans = []
    for i in range(1, iters + 1):
        for j in range(1, iters + 1):
            cnt, info, flag, mesg = sp.optimize.fsolve(for_solve,
                                                       np.array([2 * np.pi * i / iters, np.pi * j / iters] + [0] * max(
                                                           len(cluster.stars) - 2, 0)), full_output=True)

            if cnt[1] >= -1 and cnt[1] <= 1:
                az = norm(cnt[0], 0, 2 * np.pi)
                phi = np.arccos(cnt[1])
                if phi > np.pi / 2: phi -= np.pi
                if (cnt[1] < 0 and hemisphere == "North") or (cnt[1] > 0 and hemisphere == "South"):
                    continue
                ans.append([az, phi, np.max(np.abs((info['fvec'])))])


    ans.sort(key=lambda x: x[-1])
    return ans[0] #returns [az, lat, max_error] in radians


def time_eq(days, year):
    d = 6.24 + 0.0172 * (365.35 * (year - 2000) + days)
    return (-7.659 * np.sin(d) + 9.863 * np.sin(2 * d + 3.5932)) / 60


def true_local_time(star, N, year, phi):
    az = (star.Az) % (2 * np.pi)
    cos_H = (np.sin(star.Alt) - np.sin(phi) * np.sin(star.dec)) / (np.cos(phi) * np.cos(star.dec))
    if np.abs(cos_H) > 1:
        return None

    t = np.arccos(cos_H)
    if az > np.pi:
        t = np.pi * 2 - t
    ra_sun = (lambda l: l + 2 * math.pi if l < 0 else l)(
        math.atan2(
            math.cos(math.radians(23.44)) *
            math.sin(math.radians(((360 * (N - 81) / 365.2422) +
                                   (7.6 * math.sin(math.radians(0.986 * (N - 4))) -
                                    9.8 * math.sin(math.radians(1.973 * (N - 81)))) * 4 / 60) % 360)),
            math.cos(math.radians(((360 * (N - 81) / 365.2422) +
                                   (7.6 * math.sin(math.radians(0.986 * (N - 4))) -
                                    9.8 * math.sin(math.radians(1.973 * (N - 81)))) * 4 / 60) % 360))
        )
    )
    print(ra_sun, "rasun", time_eq(N, year))
    local_time = (12 + (t + star.RA - ra_sun) / 2 / np.pi * 24) % 24
    print(local_time)
    return local_time - time_eq(N, year)


def to_hours(dt):
    return dt.hour + (dt.minute / 60) + (dt.second / 3600) + (dt.microsecond / 3_600_000_000)


def day_of_year_fraction(dt):
    start_of_year = datetime(dt.year, 1, 1, tzinfo=dt.tzinfo)
    seconds_since_start = (dt - start_of_year).total_seconds()
    return seconds_since_start / 86400

def longitude(phi, star, days, GMT, year):
    time = true_local_time(star, days, year, phi)
    if time is not None:
        return np.deg2rad(norm((time - GMT) * 15, -180, 180))
    else:
        return np.nan

#Main function for longitude calculations (cur_time in ISO format)
from datetime import datetime, timezone

def mean_longitude(cluster, dt):
    phi = np.array(mean_lattitude(cluster)[:-1])[1]
    day = day_of_year_fraction(dt)
    year = dt.year
    hour = to_hours(dt)
    print(year, day, hour)
    return np.nanmean([
        longitude(phi, star, day, hour, year)
        for star in cluster.stars
    ], axis=0)

