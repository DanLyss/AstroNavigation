
# AstroNavigation Project 🌌📱

## Overview

AstroNavigation determines geographical coordinates (latitude and longitude) from night sky photographs without GPS or internet connectivity. The system uses astronomical calculations and star pattern recognition to determine the observer's position on Earth.

## Quick Start Guide

To build and run the AstroNavigation app:

1. **Prerequisites**: Install Android Studio, JDK 11, and Android SDK 28
2. **Clone & Open**: Clone the repository and open in Android Studio
3. **Build**: Run `gradlew assembleDebug` (Windows) or `./gradlew assembleDebug` (macOS/Linux)
4. **Install**: Connect your Android device and run `gradlew installDebug` (Windows) or `./gradlew installDebug` (macOS/Linux)

## Features

- **No GPS or Internet Required**: Calculate your position using only the stars
- **Night Sky Photography**: Capture night sky images with adjustable exposure settings
- **Star Pattern Recognition**: Automatically identify star patterns in your images
- **Coordinate Calculation**: Determine latitude and longitude from astronomical data
- **Location Comparison**: Compare calculated position with GPS data (if available)
- **Image Processing**: Crop and process images for optimal star detection
- **Offline Operation**: Works completely offline in remote locations

## Project Structure

### 1. StarApp (Android Application) 📱

A Kotlin Android application that serves as the user interface for capturing and processing night sky images.

### 2. KotlinTranslation (Backend) 🧮

Core astronomical calculation algorithms for star pattern recognition, coordinate transformations, and latitude/longitude calculations.

### 3. Package Organization

- **KotlinTranslation Module**: `backend/KotlinTranslation/src/main/kotlin/kotlintranslation/`
- **StarApp Module**: `StarApp/app/src/main/java/com/starapp/navigation/`

### 4. Package Structure

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

## Development Setup

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 11 (OpenJDK or Oracle JDK)
- Android SDK 28 (Android 9 Pie)
- A physical Android device (Android 8.0-9.0 recommended) or emulator

### Environment Setup
1. Install Android Studio from [developer.android.com](https://developer.android.com/studio)
2. Configure Android Studio to use JDK 11 (File > Settings > Build Tools > Gradle)
3. Clone and open the repository in Android Studio

## Building and Installing

### Using Android Studio
1. Open the project in Android Studio
2. Select "Build" > "Build Project" from the menu
3. Connect your Android device and select "Run" > "Run 'app'"

### Using Command Line
```
# Build APK
gradlew assembleDebug    # Windows
./gradlew assembleDebug  # macOS/Linux

# Install on connected device
gradlew installDebug     # Windows
./gradlew installDebug   # macOS/Linux
```

## Testing

The project includes tests for both modules:

### KotlinTranslation Tests
```
gradlew :KotlinTranslation:test
```

### StarApp Tests
```
gradlew :StarApp:app:runUnitTests
```

### Troubleshooting Tips
- Use JDK 11 specifically
- For build issues, try `gradlew clean` followed by `gradlew assembleDebug`
- For test execution issues, use the custom task: `gradlew :StarApp:app:runUnitTests`
- The app is optimized for Android 8.0-9.0 (API levels 26-28)


## Usage

1. Install the app on your Android device
2. Grant necessary permissions (camera, location, storage)
3. Capture a night sky image or select an existing one
4. The app will analyze the image and calculate your position
5. View your latitude and longitude on the results screen

## Application Workflow

1. **Main Screen**: Capture photo or select image, adjust settings
2. **Image Cropping**: Crop image for optimal star detection
3. **Astrometry Solving**: Identify star patterns in the image
4. **Results Display**: View identified stars overlaid on the image
5. **Star Analysis**: Process star data and create star clusters
6. **Location Display**: View calculated coordinates and compare with GPS

## Dependencies

- AndroidX Camera and ExifInterface
- Google Play Services Location
- Apache Commons Math
- nom.tam.fits (FITS file operations)

## Key Files

- `ui/MainActivity.kt`: Application entry point
- `ui/StarsActivity.kt`: Star processing and coordinate calculation
- `kotlintranslation/LattLongCalc.kt`: Latitude/longitude algorithms
- `astro/AstrometryManager.kt`: Astrometry solver operations

## Contributing

We welcome contributions to the AstroNavigation project! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
