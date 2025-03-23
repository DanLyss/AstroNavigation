import numpy as np
import scipy as sp
from datetime import datetime


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
def mean_latitude(cluster):
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
                ans.append([az, phi, np.max(np.abs(info['fvec']))])

    ans.sort(key=lambda x: x[-1])
    return ans[0] #returns [az, lat, max_error] in radians


def time_eq(days, year):
    d = 6.24 + 0.0172 * (365.35 * (year - 2000) + days)
    return (-7.659 * np.sin(d) + 9.863 * np.sin(2 * d + 3.5932)) / 60


def true_local_time(star, days, year):
    t = np.arcsin(np.cos(star.Alt) * np.sin(star.Az) / np.cos(star.dec))
    ra_sun = np.arcsin(np.sin(2 * np.pi * (days - day_eq) / year_length) * np.cos(obliquity))
    local_time = 12 + (t + ra_sun - star.RA) / 2 / np.pi * 24
    return local_time - time_eq(days, year)


def to_hours(dt):
    return dt.hour + (dt.minute / 60) + (dt.second / 3600) + (dt.microsecond / 3_600_000_000)


def longitude(star, days, GMT, year):
    return np.deg2rad(norm((true_local_time(star, days, year) - GMT) * 15, -180, 180))

#Main function for longitude calculations (cur_time in ISO format)
def mean_longitude(cluster, cur_time):
    dt = datetime.fromisoformat(cur_time[:-1])
    return np.mean([longitude(star, dt.timetuple().tm_yday, to_hours(dt), dt.year) for star in cluster.stars], axis=0)



