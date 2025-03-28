import copy
from datetime import datetime, timezone

import numpy as np
from AstroNavigation.backend.math.math2.latt_long_calc import mean_lattitude, mean_longitude

np.set_printoptions(threshold=1000000)

class Solver:
    def imag_scale_one_input(self, a_b_c_beta: (float, float, float, float)):
        a = a_b_c_beta[0]
        b = a_b_c_beta[1]
        c = a_b_c_beta[2]
        beta = a_b_c_beta[3]
        D = b ** 2 - 4 * (a + b) * np.tan(beta) ** 2 * a
        if D < 0: #we still don't know all paramters of photo, so D could be < 0 in playing with random paramters
            return None, None
        x1 = (b + np.sqrt(D)) / (2 * (a + b) * np.tan(beta))
        x2 = (b - np.sqrt(D)) / (2 * (a + b) * np.tan(beta))
        total_size1 = np.arctan(((a + b + c) * x1) / a)
        total_size2 = np.arctan(((a + b + c) * x2) / a)
        return total_size1, total_size2

    def imag_solve_total(self, many_a_b_c_beta: list[(float, float, float, float)]):
        total_sizes = []
        for a_b_c_beta in many_a_b_c_beta:
            total_size1, total_size2 = self.imag_scale_one_input(a_b_c_beta)
            if total_size1 is not None:
                total_sizes.append(min(total_size1, total_size2))
        total_sizes = np.array(total_sizes)
        total_sizes.sort()
        #through everything not in 2-sigma
        total_sizes = total_sizes[(total_sizes > np.mean(total_sizes) -  2 * np.std(total_sizes)) &
                                (total_sizes < np.mean(total_sizes) + 2 * np.std(total_sizes))]
        return np.mean(total_sizes)

class Star:
    def __init__(self, x_measured_coord: float, y_measured_coord: float, RA: float, dec: float):
        self.x_measured_coord = x_measured_coord #in rotated coordinates
        self.y_measured_coord = y_measured_coord
        self.x_coord = None #in normally oriented in tangent plane coordinates (what we want)
        self.y_coord = None
        self.RA = RA
        self.dec = dec
        self.Alt = None
        self.Az = None

    def angular_dist(self, other) -> float:
        return np.arccos(np.sin(self.dec) * np.sin(other.dec)  + np.cos(self.dec) * np.cos(other.dec)*np.cos(self.RA-other.RA))

    def planar_dist(self, other) -> float:
        return np.sqrt((self.x_coord - other.x_coord)**2 +
                       (self.y_coord - other.y_coord)**2)

    def delta_x(self, other) -> float:
        return abs(self.x_coord - other.x_coord)

    def delta_y(self, other) -> float:
        return abs(self.y_coord - other.y_coord)

    def angular_x_proj_dist(self, other) -> float:
        return np.arcsin(np.sin(self.angular_dist(other)) * (self.delta_x(other) / self.planar_dist(other)))

    def angular_y_proj_dist(self, other) -> float:
        return np.arcsin(np.sin(self.angular_dist(other)) * (self.delta_y(other) / self.planar_dist(other)))


    def precalculate_a_b_c_beta(self, other, axis: int, pix_length: float) -> \
            (float, float, float, float): # axis 0 - x, 1 - y
        P1 = self.copy()
        P2 = other.copy()
        if axis == 0:
            P1.x_coord = abs(P1.x_coord)
            P2.x_coord = abs(P2.x_coord)
        if axis == 1:
            P1.y_coord = abs(P1.y_coord)
            P2.y_coord = abs(P2.y_coord)
        if axis == 0:
            if P1.x_coord > P2.x_coord:
                P1, P2 = P2, P1
            return P1.x_coord, P2.x_coord - P1.x_coord, pix_length - P2.x_coord, P1.angular_x_proj_dist(P2)
        else:
            if P1.y_coord > P2.y_coord:
                P1, P2 = P2, P1
            return P1.y_coord, P2.y_coord - P1.y_coord, pix_length - P2.y_coord, P1.angular_y_proj_dist(P2)

    def set_Alt_Az(self, angular_length_x: float, angular_length_y: float,
                          pixes_length_x: float, pixes_length_y: float,
                          positional_angle: float):
        pi = positional_angle
        alpha = np.arctan((self.x_coord * np.tan(angular_length_x)) / pixes_length_x)
        beta = np.arctan((self.y_coord * np.tan(angular_length_y)) / pixes_length_y)
        c = np.pi / 2 - beta
        z = alpha
        self.Alt = np.arcsin(
           np.cos(c) * np.cos(pi) + np.sin(c) * np.sin(pi) * np.cos(z)
        )
        self.Az = np.arcsin((np.sin(c)*np.sin(z))/np.cos(self.Alt))

    def set_normal_x_y(self, rotation_angle: float):
        #rotation_angle is counterclockwise from (x, y)_measured to
        # (x, y)_normal
        cos_rot = np.cos(rotation_angle)
        sin_rot = np.sin(rotation_angle)
        self.x_coord = cos_rot * self.x_measured_coord + sin_rot * self.y_measured_coord
        self.y_coord = - sin_rot * self.x_measured_coord + cos_rot * self.y_measured_coord

    def copy(self):
        #deepcopy
        return copy.deepcopy(self)

    def __repr__(self):
        return f"Star(x_coord={self.x_coord}, y_coord={self.y_coord}, RA={self.RA}, dec={self.dec}, Alt={self.Alt}, Az={self.Az})"



class Star_Cluster:
    def __init__(self, stars: list[Star],
                 positional_angle: float, rotation_angle: float, timeGMT: str = "2001-09-11T22:00:00+02:00"):
        self.stars = stars
        self.rotation_angle = rotation_angle
        self.set_normal_x_y_for_all_stars()
        #its preudo size, cause it actually doesn't matter
        #since all quantites are relative
        self.pix_length_x = 100
        self.pix_length_y = 100
        self.angular_x_size = None
        self.angular_y_size = None
        self.solve_for_angular_sizes()
        # print("SIZEX", self.angular_x_size)
        # print("SIZEY", self.angular_y_size)
        self.positional_angle = positional_angle
        self.set_forall_stars_Alt_Az()
        self.Az_star_0 = mean_lattitude(self)[0]
        self.phi = mean_lattitude(self)[1]
        self.make_Az_abs()
        self.timeGMT = datetime.fromisoformat(timeGMT).astimezone(timezone.utc)
        self.long = mean_longitude(self, self.timeGMT)


    def solve_for_angular_sizes(self):
        a_b_c_beta_for_x = []
        a_b_c_beta_for_y = []
        for star1 in self.stars:
            for star2 in self.stars:
                if star1 != star2:
                    if star1.x_coord * star2.x_coord > 0:
                        a_b_c_beta_for_x.append(star1.precalculate_a_b_c_beta(star2, axis=0,
                                                      pix_length=self.pix_length_x / 2))
                    if star1.y_coord * star2.y_coord > 0:
                        a_b_c_beta_for_y.append(star1.precalculate_a_b_c_beta(star2, axis=1,
                                                      pix_length=self.pix_length_y / 2))
        solver = Solver()
        self.angular_x_size = solver.imag_solve_total(a_b_c_beta_for_x)
        self.angular_y_size = solver.imag_solve_total(a_b_c_beta_for_y)

    def set_forall_stars_Alt_Az(self):
        for star in self.stars:
            star.set_Alt_Az(self.angular_x_size, self.angular_y_size,
                            self.pix_length_x / 2, self.pix_length_y / 2, self.positional_angle)

    def make_Az_abs(self):
        for star in self.stars:
            star.Az = (star.Az + self.Az_star_0) % (2 * np.pi)

    def set_normal_x_y_for_all_stars(self):
        for star in self.stars:
            star.set_normal_x_y(self.rotation_angle)

    def write_to_file(self, filename):
        with open(filename, 'w') as f:
            f.write(f'Number of stars: {len(self.stars)}\n')
            f.write(f'Angular X size: {self.angular_x_size:.6f} radians\n')
            f.write(f'Angular Y size: {self.angular_y_size:.6f} radians\n')
            for star in self.stars:
                f.write(f'{star.RA:.6f} {star.dec:.6f} {star.Alt:.6f} {star.Az:.6f}\n')
                f.write('\n')
