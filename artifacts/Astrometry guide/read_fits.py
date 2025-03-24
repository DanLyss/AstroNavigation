from astropy.io import fits

file_path = r"/mnt/c/Users/Dan/Downloads/photo_2024-07-13_22-07-41.corr"

with fits.open(file_path) as hdul:
    print("\n=== Заголовок FITS ===\n")
    print(hdul[1].data.columns)
