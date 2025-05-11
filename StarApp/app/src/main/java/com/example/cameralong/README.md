# CameraLong Package Documentation

## Overview

The `com.example.cameralong` package is the main package for the AstroNavigation Android application. This application determines geographical coordinates (latitude and longitude) from night sky photographs without GPS or internet connectivity, using astronomical calculations and star pattern recognition.

## Package Structure

The package has been organized into a modular structure to improve maintainability and separation of concerns:

```
com.example.cameralong/
├── App.kt                  # Application class
├── README.md               # This documentation file
├── astro/                  # Astronomical calculations and processing
│   ├── AstrometryManager.kt  # Manages astrometry solver operations
│   └── FitsManager.kt        # Handles FITS file operations
├── camera/                 # Camera functionality
│   └── CameraManager.kt      # Manages camera operations
├── file/                   # File operations
│   └── FileManager.kt        # Handles file operations
├── gesture/                # Gesture handling
│   └── GestureManager.kt     # Manages gesture detection
├── image/                  # Image handling
│   └── ImageSelectionManager.kt # Manages image selection and processing
├── intent/                 # Intent handling
│   └── IntentManager.kt      # Manages intent parameter extraction
├── location/               # Location and sensor handling
│   ├── LocationHandler.kt    # Manages device location
│   ├── LocationManager.kt    # Manages location calculations
│   └── SensorHandler.kt      # Manages device orientation sensors
├── navigation/             # Navigation handling
│   └── NavigationManager.kt  # Manages navigation between activities
├── permission/             # Permission handling
│   └── PermissionManager.kt  # Manages permission requests
├── result/                 # Result handling
│   └── ResultManager.kt      # Manages result processing
├── star/                   # Star processing
│   └── StarProcessingManager.kt # Manages star data processing
├── ui/                     # UI components
│   ├── CropActivity.kt       # Handles image cropping before processing
│   ├── CropOverlayView.kt    # Custom view for crop rectangle overlay
│   ├── LocationActivity.kt   # Displays location information
│   ├── MainActivity.kt       # Main entry point of the application
│   ├── ResultActivity.kt     # Displays image processing results
│   ├── StarOverlayView.kt    # Custom view for star overlays
│   ├── StarsActivity.kt      # Displays star analysis
│   └── manager/              # UI management
│       └── UIManager.kt      # Manages UI configuration and updates
└── util/                   # Utility classes
    └── ExifUtils.kt          # EXIF data operations
```

## Component Responsibilities

### Application

- **App**: Application class that initializes the application and sets up crash handling.

### UI Components

- **MainActivity**: Entry point of the application. Handles camera preview, photo capture, and initiating the astrometry solver.
- **CropActivity**: Allows users to crop the captured/selected image symmetrically from the center before processing.
- **ResultActivity**: Displays the captured/selected image with star points overlaid.
- **StarsActivity**: Processes astronomical data from image files and displays star information.
- **LocationActivity**: Displays predicted location coordinates and compares them with GPS data if available.
- **StarOverlayView**: Custom view for overlaying star points on an image.
- **CropOverlayView**: Custom view for displaying and adjusting the crop rectangle overlay.

### Managers

- **AstrometryManager**: Handles astrometry-related operations, including running the astrometry solver.
- **CameraManager**: Manages camera operations, including camera setup and photo capture.
- **FileManager**: Handles file operations, including saving files and extracting assets.
- **FitsManager**: Handles FITS file operations, including converting bitmaps to FITS format and extracting star coordinates.
- **GestureManager**: Manages gesture detection, particularly swipe gestures for navigation.
- **ImageSelectionManager**: Manages image selection from gallery and processing of selected images.
- **IntentManager**: Handles intent parameter extraction and creation for activity navigation.
- **LocationManager**: Manages location calculations and GPS data extraction from images.
- **NavigationManager**: Manages navigation between activities, encapsulating intent creation and parameter passing.
- **PermissionManager**: Handles permission requests and checks for required permissions.
- **ResultManager**: Manages image loading and star point extraction for the result screen.
- **StarProcessingManager**: Manages star data processing and cluster creation for astronomical calculations.
- **UIManager**: Handles UI configuration and updates, separating UI logic from activities.

### Handlers

- **LocationHandler**: Manages device location using the FusedLocationProviderClient.
- **SensorHandler**: Manages device orientation sensors to get device orientation angles.

### Utilities

- **ExifUtils**: Handles EXIF data operations, including extracting and saving orientation angles, timestamps, and location data.

## Workflow

The application follows a comprehensive workflow for determining geographical coordinates from night sky photographs:

### 1. Main Screen (MainActivity)

- **Launch**: The app starts with MainActivity, displaying a camera preview.
- **Camera Controls**:
  - Exposure slider: Adjusts camera exposure time for better night sky photography.
  - Astrometry time slider: Sets the CPU time limit for the astrometry solver.
  - Capture button: Takes a photo using the device camera.
  - Choose button: Opens the gallery to select an existing image.
- **Behind the Scenes**:
  - **CameraManager**: Configures the camera with custom exposure settings.
  - **PermissionManager**: Ensures camera, location, and storage permissions are granted.
  - **LocationHandler**: Collects current GPS coordinates if available.
  - **SensorHandler**: Records device orientation angles (pitch, roll, yaw).

### 2. Image Cropping (CropActivity)

- After capturing a photo or selecting one from the gallery, the user is presented with the CropActivity.
- **Cropping Interface**:
  - Displays the captured/selected image with a rectangular crop overlay.
  - The crop is always symmetric around the center of the image.
  - User can adjust the crop size by dragging the edges of the rectangle.
  - "Crop and Continue" button: Crops the image and proceeds to astrometry solving.
  - "Skip Crop" button: Proceeds with the original uncropped image.
- **Behind the Scenes**:
  - **CropOverlayView**: Renders the crop rectangle and handles user interactions.
  - **NavigationManager**: Manages the transition to the next activity.

### 3. Astrometry Solving

- After cropping (or skipping crop), the astrometry solver processes the image.
- **Processing Steps**:
  - The image is converted to FITS format.
  - The astrometry.net solver identifies star patterns in the image.
  - Star coordinates are extracted and saved.
  - A .corr file is generated containing star correspondence data.
- **Behind the Scenes**:
  - **AstrometryManager**: Runs the astrometry solver process.
  - **FitsManager**: Handles image format conversion.
  - **FileManager**: Manages temporary files and results.

### 4. Results Display (ResultActivity)

- After processing, the ResultActivity displays the image with identified stars.
- **Results Interface**:
  - Shows the processed image with star points overlaid.
  - "Continue" button: Enabled if star identification was successful.
  - Swipe gesture: Returns to the previous screen.
- **Behind the Scenes**:
  - **ResultManager**: Loads the image and extracts star coordinates.
  - **StarOverlayView**: Renders star points on top of the image.
  - **GestureManager**: Handles swipe gestures for navigation.

### 5. Star Analysis (StarsActivity)

- If star identification was successful, StarsActivity analyzes the star data.
- **Analysis Interface**:
  - Displays the image with star clusters and celestial coordinates.
  - Shows processing status and results.
  - "Continue" button: Proceeds to location calculation if analysis is successful.
- **Behind the Scenes**:
  - **StarProcessingManager**: Processes star data and creates star clusters.
  - **UIManager**: Updates the UI based on processing results.

### 6. Location Display (LocationActivity)

- Finally, LocationActivity displays the calculated geographical coordinates.
- **Location Interface**:
  - Shows predicted latitude and longitude based on star positions.
  - Compares predicted location with GPS data if available.
  - Displays the distance between predicted and actual locations.
  - Map view: Visualizes the predicted location.
- **Behind the Scenes**:
  - **LocationManager**: Calculates geographical coordinates from astronomical data.
  - **ExifUtils**: Extracts GPS data from image EXIF metadata.

Throughout the entire workflow, **NavigationManager** and **IntentManager** handle transitions between activities and parameter passing, ensuring a smooth user experience.

## Dependencies

- **AndroidX Camera**: For camera functionality.
- **AndroidX ExifInterface**: For EXIF data operations.
- **Google Play Services Location**: For device location.
- **KotlinTranslation**: Core astronomical calculation algorithms.
- **nom.tam.fits**: For FITS file operations.

## Development Guidelines

### Adding New Features

1. **Identify the appropriate package**: Determine which package the new feature belongs to based on its functionality.
2. **Create a new class or extend an existing one**: Follow the single responsibility principle.
3. **Update the README**: Document the new feature in this README file.

### Code Style

- Follow Kotlin coding conventions.
- Use descriptive variable and method names.
- Add comments for complex logic.
- Include KDoc comments for public methods.

### Testing

- Write unit tests for new functionality.
- Test on different devices and Android versions.
- Verify that the application works without internet connectivity.

## Troubleshooting

### Common Issues

- **Camera permissions**: Ensure that the application has the necessary permissions.
- **Location permissions**: Ensure that the application has location permissions.
- **Storage permissions**: Ensure that the application has storage permissions.
- **Astrometry solver**: Ensure that the astrometry solver is properly installed and configured.

### Debugging

- Check the logcat output for error messages.
- Use the crash logs saved in the external files directory.
- Test with different images and conditions.
