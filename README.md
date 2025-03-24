
# AstroNavigation Project 🌌📷

## Overview

This project aims to determine coordinates just from a photo of a night sky without use of GPS or internet connection.

## Core Components

### 1. `Star_Star_Cluster.py` ⭐🌠

Defines the core data structures for representing stars and clusters of stars.

#### Classes:

-   **Star**: Represents a single star with properties:
    
    -   `x_measured_coord`, `y_measured_coord`: Coordinates in the rotated reference frame.
        
    -   `RA`, `dec`: Right Ascension and Declination.
        
    -   `Alt`, `Az`: Altitude and Azimuth after transformation.
        
    -   Methods for computing distances (angular, planar, projections) and coordinate transformations.
        
-   **Star_Cluster**: Represents a collection of stars from a processed image.
    
    -   Stores rotational and positional angles.
        
    -   Computes altitude and azimuth for all stars.
        
    -   Determines the angular size of the star cluster.
        
    -   Calculates approximate latitude and longitude.
        
    -   Writes star data to an output file.
        
-   **Solver** (Internal): Helper class for solving projection and transformation equations.
    
    -   Computes image scale transformations.
        
    -   Filters results using statistical methods.
        

### 2. `astrometry_math1_integration.py` 🛰️🔭

Handles the processing of images to extract visible star coordinates using astrometry.

#### Functionality:

-   `**process_astrometry_image(image_path, ...)**`
    
    -   Runs `solve-field` (astrometry tool) to analyze the image.
        
    -   Extracts celestial coordinates from `.corr` files.
        
    -   Filters stars based on match weight threshold.
        
    -   Converts matched stars into a `Star_Cluster` object.
        
    -   Outputs extracted data to a file.
        

### 3. `latt_long_calc.py` 🌍🧭

Calculates the latitude, longitude, and azimuth based on extracted star positions.

#### Functions:

-   `**mean_lattitude(cluster)**`
    
    -   Computes the latitude from a star cluster using an optimization method.
        
-   `**mean_longitude(cluster, time)**`
    
    -   Computes the longitude based on star positions, date, and time.
        
-   `**true_local_time(star, day, year, latitude)**`
    
    -   Determines the local time based on star positions.
        
-   `**sun_RA(day, year)**`
    
    -   Calculates the Right Ascension of the Sun for reference.
        

## Usage 🛠️

1.  Capture an image of the night sky.
    
2.  Use `astrometry_math1_integration.py` to process the image and generate a `Star_Cluster`.
    
3.  Use `latt_long_calc.py` to determine the latitude, longitude, and azimuth from the extracted data.
    

## Dependencies 📦

-   Python 3
    
-   `numpy`, `scipy`
    
-   `astropy`
    
-   `solve-field` (Astrometry.net tool)
    

## Notes 📝

-   Ensure `solve-field` is installed and accessible.
    
-   The accuracy of latitude/longitude calculations depends on star recognition precision.
    

