import numpy as np
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
                total_sizes.append(total_size1)
                total_sizes.append(total_size2)
        #sort
        total_sizes = np.array(total_sizes)
        median_value = np.median(total_sizes)
        lower_group = total_sizes[total_sizes < median_value]
        upper_group = total_sizes[total_sizes > median_value]

        lower_variance = np.var(lower_group) if len(lower_group) > 1 else float('inf')
        upper_variance = np.var(upper_group) if len(upper_group) > 1 else float('inf')

        if lower_variance < upper_variance:
            true_values = lower_group
        else:
            true_values = upper_group

        true_value = np.mean(true_values)
        return true_value

class Star:
    def __init__(self, x_coord: float, y_coord: float, RA: float, dec: float):
        self.x_coord = x_coord
        self.y_coord = y_coord
        self.RA = RA
        self.dec = dec
        self.Alt = None
        self.Az = None

    def angular_dist(self, other) -> float:
        delta_x = self.RA - other.RA
        delta_y = self.dec - other.dec
        return np.arccos(np.cos(delta_x) * np.cos(delta_y))

    def planar_dist(self, other) -> float:
        return np.sqrt((self.x_coord - other.x_coord)**2 +
                       (self.y_coord - other.y_coord)**2)

    def delta_x(self, other) -> float:
        return abs(self.x_coord - other.x_coord)

    def delta_y(self, other) -> float:
        return abs(self.y_coord - other.y_coord)

    def angular_x_proj_dist(self, other) -> float:
        return self.angular_dist(other) * (self.delta_x(other) / self.planar_dist(other))

    def angular_y_proj_dist(self, other) -> float:
        return self.angular_dist(other) * (self.delta_y(other) / self.planar_dist(other))


    def precalculate_a_b_c_beta(self, other, axis: int, pix_length: float) -> \
            (float, float, float, float): # axis 0 - x, 1 - y
        if axis == 0:
            if self.x_coord > other.x_coord:
                P1 = other
                P2 = self
            else:
                P1 = self
                P2 = other
            return P1.x_coord, P2.x_coord - P1.x_coord, pix_length - P2.x_coord, self.angular_x_proj_dist(other)
        else:
            if self.y_coord > other.y_coord:
                P1 = other
                P2 = self
            else:
                P1 = self
                P2 = other
            return P1.y_coord, P2.y_coord - P1.y_coord, pix_length - P2.y_coord, self.angular_y_proj_dist(other)

    def set_Alt_Az(self, angular_length_x: float, angular_length_y: float,
                          pixes_length_x: float, pixes_length_y: float,
                          positional_angle: float):
        alpha = np.arctan((self.y_coord * np.tan(angular_length_x)) / pixes_length_y)
        beta = np.arctan((self.x_coord * np.tan(angular_length_y)) / pixes_length_x)
        self.Alt = positional_angle + alpha
        self.Az = beta

class Star_Cluster:
    def __init__(self, stars: list[Star], pix_length_x: float, pix_length_y: float,
                 positional_angle: float):
        self.stars = stars
        self.pix_length_x = pix_length_x
        self.pix_length_y = pix_length_y
        self.angular_x_size = None
        self.angular_y_size = None
        self.solve_for_angular_sizes()
        self.positional_angle = positional_angle
        self.set_forall_stars_Alt_Az()

    def solve_for_angular_sizes(self):
        a_b_c_beta_for_x = []
        a_b_c_beta_for_y = []
        for star1 in self.stars:
            for star2 in self.stars:
                if star1 != star2:
                    a_b_c_beta_for_x.append(star1.precalculate_a_b_c_beta(star2, axis=0,
                                                      pix_length=self.pix_length_x / 2))
                    a_b_c_beta_for_y.append(star1.precalculate_a_b_c_beta(star2, axis=1,
                                                      pix_length=self.pix_length_y / 2))
        solver = Solver()
        self.angular_x_size = solver.imag_solve_total(a_b_c_beta_for_x) * 2
        self.angular_y_size = solver.imag_solve_total(a_b_c_beta_for_y) * 2
    def set_forall_stars_Alt_Az(self):
        for star in self.stars:
            star.set_Alt_Az(self.angular_x_size, self.angular_y_size,
                            self.pix_length_x, self.pix_length_y, self.positional_angle)

    def write_to_file(self, filename):
        with open(filename, 'w') as f:
            f.write(f'Number of stars: {len(self.stars)}\n')
            f.write(f'Angular X size: {self.angular_x_size:.6f} radians\n')
            f.write(f'Angular Y size: {self.angular_y_size:.6f} radians\n')
            for star in self.stars:
                f.write(f'{star.RA:.6f} {star.dec:.6f} {star.Alt:.6f} {star.Az:.6f}\n')
                f.write('\n')
