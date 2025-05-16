# Navigation Module

## Overview
The `com.starapp.navigation` package contains all components for the AstroNavigation Android application, handling UI, camera operations, image processing, and astronomical calculations.

## Key Components

### UI Activities
- **MainActivity**: Camera preview, photo capture, exposure settings
- **CropActivity**: Image cropping for star detection
- **ResultActivity**: Displays identified stars overlay
- **StarsActivity**: Processes star data
- **LocationActivity**: Shows calculated coordinates

### Core Managers
- **AstrometryManager**: Star pattern identification and offline astrometry processing
- **CameraManager**: Camera API 2 implementation with manual exposure control
- **LocationHandler**: GPS and sensor data
- **FileManager**: Image and data storage

## Module Structure
```
com.starapp.navigation/
├── astro/       # Astronomical calculations
├── camera/      # Camera functionality
├── file/        # File operations
├── gesture/     # Gesture handling
├── image/       # Image processing
├── intent/      # Intent handling
├── location/    # Location services
├── main/        # Main application logic
├── navigation/  # Navigation components
├── permission/  # Permission handling
├── result/      # Result processing
├── star/        # Star data processing
├── ui/          # User interface
└── util/        # Utilities
```

## Application Flow
1. **Capture**: Take photo with adjustable exposure
2. **Process**: Crop image and identify star patterns
3. **Calculate**: Determine coordinates from star positions
4. **Display**: Show location and compare with GPS

## Implementation
The module implements offline astrometry processing by:
1. Converting captured images to FITS format
2. Running the astrometry-net solver locally on the device
3. Extracting star patterns and matching against a local star catalog
4. Calculating precise celestial coordinates from the identified patterns
5. Converting astronomical coordinates to geographical position

