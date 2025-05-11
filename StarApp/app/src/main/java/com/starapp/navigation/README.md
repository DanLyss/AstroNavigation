# StarApp Navigation Package

## Overview

The `com.starapp.navigation` package is the main package for the AstroNavigation Android application. This application determines geographical coordinates from night sky photographs without GPS or internet connectivity, using astronomical calculations and star pattern recognition.

## Package Structure

The package is organized into a modular structure for maintainability:

```
com.starapp.navigation/
├── App.kt                  # Application class
├── astro/                  # Astronomical calculations and processing
├── camera/                 # Camera functionality
├── file/                   # File operations
├── gesture/                # Gesture handling
├── image/                  # Image handling
├── intent/                 # Intent handling
├── location/               # Location and sensor handling
├── navigation/             # Navigation handling
├── permission/             # Permission handling
├── result/                 # Result handling
├── star/                   # Star processing
├── ui/                     # UI components
└── util/                   # Utility classes
```

## Key Components

### UI Components
- **MainActivity**: Entry point, handles camera preview and photo capture
- **CropActivity**: Image cropping functionality
- **ResultActivity**: Displays image with identified stars
- **StarsActivity**: Processes astronomical data and displays star information
- **LocationActivity**: Shows calculated coordinates and GPS comparison

### Core Managers
- **AstrometryManager**: Runs the astrometry solver
- **CameraManager**: Handles camera operations
- **LocationManager**: Manages location calculations
- **StarProcessingManager**: Processes star data for coordinate calculation

## Workflow

The application follows this workflow for determining geographical coordinates:

1. **Main Screen (MainActivity)**
   - Display camera preview with exposure and astrometry time settings
   - Capture photo or select existing image
   - Record device orientation and GPS coordinates if available

2. **Image Cropping (CropActivity)**
   - Crop image symmetrically from center
   - Adjust crop size by dragging rectangle edges
   - Proceed with or skip cropping

3. **Astrometry Solving**
   - Convert image to FITS format
   - Identify star patterns
   - Extract star coordinates

4. **Results Display (ResultActivity)**
   - Show image with identified stars overlaid
   - Continue if star identification was successful

5. **Star Analysis (StarsActivity)**
   - Process star data and create star clusters
   - Display processing status and results

6. **Location Display (LocationActivity)**
   - Show calculated latitude and longitude
   - Compare with GPS data if available
   - Display distance between predicted and actual locations

## Dependencies

- AndroidX Camera and ExifInterface
- Google Play Services Location
- KotlinTranslation (core astronomical algorithms)
- nom.tam.fits (FITS file operations)

## Development Guidelines

- Follow Kotlin coding conventions
- Place new features in appropriate packages
- Write unit tests for new functionality
- Test on different devices and without internet connectivity

## Troubleshooting

- Ensure all required permissions are granted (camera, location, storage)
- Check logcat for error messages
- Verify astrometry solver configuration
- Test with different images and conditions
