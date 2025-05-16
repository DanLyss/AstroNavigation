
# AstroNavigation 🌌📱

## Overview
AstroNavigation determines geographical coordinates from night sky photographs without GPS or internet connectivity, using astronomical calculations and star pattern recognition.

## Quick Start
1. **Prerequisites**: Android Studio, JDK 11, Android SDK 28
2. **Build**: `gradlew assembleDebug` (Windows) or `./gradlew assembleDebug` (macOS/Linux)
3. **Install**: `gradlew installDebug` (Windows) or `./gradlew installDebug` (macOS/Linux)

## Key Features
- Calculate position using only star patterns (no GPS/internet required)
- Capture night sky images with adjustable exposure using Camera API 2
- Process images to identify stars and determine coordinates
- Compare calculated position with GPS data (if available)
- Operate completely offline in remote locations

## Project Structure
- **StarApp**: Android application (UI, camera, image processing)
- **KotlinTranslation**: Core astronomical calculation algorithms

## Development Requirements
- Android Studio (2020.3.1+)
- JDK 11
- Android SDK 28
- Android device (8.0-9.0 recommended) or emulator

## Android Version Compatibility
The application targets Android 9.0 (API 28) and supports devices running Android 8.0 (API 26) and above. We intentionally target devices lower than Android 10 because:
1. **Executable Permissions**: We chose to target Android 9 (API 28) to retain executable permissions on our app's private storage (`filesDir`). Starting with Android 10 (API 29), SELinux and the `noexec` mount flag on `filesDir`/`cacheDir` block any unpacked binaries from running ("Permission denied" on `ProcessBuilder.start()`).
2. **Astrometry Integration**: Rebuilding the entire Astrometry.net repository as a shared library (JNI) is a significant effort—compiling dozens of C/C++ modules (CFITSIO, WCSLIB, source extraction, etc.) for multiple ABIs—so targeting API 28 lets us keep the simple "unpack & exec" flow without a full NDK rewrite.
3. **Camera API 2 Stability**: Camera API 2 implementation is more stable on Android 8.0-9.0
4. **Exposure Control**: Long-exposure photography features work more consistently across device manufacturers

App still works on newer versions of Android, just warns user

## Building & Testing
```bash
# Build
gradlew assembleDebug

# Install
gradlew installDebug

# Test
gradlew :KotlinTranslation:test
gradlew :StarApp:app:runUnitTests
```

## Application Workflow
1. Capture/select night sky image
2. Crop image for optimal star detection
3. Identify star patterns
4. Process star data
5. Calculate and display coordinates

## Image Processing
The application performs astrometry (star pattern recognition) completely offline using:
1. Image conversion to FITS format for astronomical processing
2. Local execution of the astrometry-net solver algorithm
3. Star extraction and pattern matching against a local catalog
4. Calculation of celestial coordinates from identified star patterns
5. Conversion of celestial coordinates to geographical position

## Dependencies
- AndroidX Camera and ExifInterface
- Google Play Services Location
- Apache Commons Math
- nom.tam.fits (FITS file format library)


## License
MIT License - see [LICENSE](LICENSE) for details.
