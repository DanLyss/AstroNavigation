
# AstroNavigation Project 🌌📱

## Overview

AstroNavigation determines geographical coordinates (latitude and longitude) from night sky photographs without GPS or internet connectivity. The system uses astronomical calculations and star pattern recognition to determine the observer's position on Earth.

## Quick Start Guide

To build and run the AstroNavigation app:

1. **Prerequisites**: Install Android Studio, JDK 11, and Android SDK 28
2. **Clone & Open**: Clone the repository and open in Android Studio
3. **Build**: Run `gradlew assembleDebug` (Windows) or `./gradlew assembleDebug` (macOS/Linux)
   - This will generate the APK at `StarApp/app/build/outputs/apk/debug/app-debug.apk`
4. **Install**: Connect your Android device and run `gradlew installDebug` (Windows) or `./gradlew installDebug` (macOS/Linux)

For detailed instructions, see the [Development Setup](#development-setup) and [Building the App](#building-the-app) sections below.

## Project Structure

### 1. StarApp (Android Application) 📱

A Kotlin Android application that serves as the user interface for capturing and processing night sky images.

### 2. KotlinTranslation (Backend) 🧮

Core astronomical calculation algorithms for:
- Star pattern recognition
- Coordinate transformations
- Latitude/longitude calculations

### 3. Package Organization

- **KotlinTranslation Module**: `backend/KotlinTranslation/src/main/kotlin/kotlintranslation/`
- **StarApp Module**: `StarApp/app/src/main/java/com/example/cameralong/`

## Development Setup

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 11 (OpenJDK or Oracle JDK)
- Android SDK 28 (Android 9 Pie) - Do not upgrade as it may cause compatibility issues
- Git (for cloning the repository)
- A physical Android device (Android 8.0-9.0 recommended) or emulator for testing

### Environment Setup
1. Install Android Studio from [developer.android.com](https://developer.android.com/studio)
2. During installation, ensure Android SDK 28 is selected
3. Install JDK 11 if not already installed
4. Configure Android Studio to use JDK 11:
   - Go to File > Settings > Build, Execution, Deployment > Build Tools > Gradle
   - Set "Gradle JDK" to JDK 11

### Project Setup
1. Clone the repository:
   ```
   # Windows
   git clone https://github.com/yourusername/AstroNavigation.git
   cd AstroNavigation

   # macOS/Linux
   git clone https://github.com/yourusername/AstroNavigation.git
   cd AstroNavigation
   ```
2. Open the project in Android Studio:
   - Select "Open an Existing Project"
   - Navigate to the cloned repository and select it
3. Wait for the project to sync with Gradle files
4. If prompted to update Gradle or any plugins, decline to maintain compatibility
5. Ensure all dependencies are resolved (check the "Build" tab for any errors)

## Building the App

### Using Android Studio
1. Open the project in Android Studio
2. Wait for the project to sync and index
3. Select "Build" > "Build Project" from the menu
4. To create an installable APK:
   - Select "Build" > "Build Bundle(s) / APK(s)" > "Build APK(s)"
   - The APK will be generated in `StarApp/app/build/outputs/apk/debug/`
   - You can navigate to this directory in your file explorer to verify the APK was created

### Using Command Line
```
# Windows
gradlew assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

The generated APK will be located at:
`StarApp/app/build/outputs/apk/debug/app-debug.apk`

After running the build command, you can verify that the APK was generated successfully by checking this directory. If the directory or APK doesn't exist, make sure you ran the build command from the project root directory.

### Installing the App

#### From Android Studio
1. Connect your Android device to your computer via USB
2. Enable USB debugging on your device (Settings > Developer options)
3. In Android Studio, select "Run" > "Run 'app'"
4. Select your device from the list and click "OK"

#### Using Command Line
```
# Windows
gradlew installDebug

# macOS/Linux
./gradlew installDebug
```

#### Manual Installation
1. Locate the APK file at `StarApp/app/build/outputs/apk/debug/app-debug.apk` after building
2. Transfer the APK file to your Android device
3. On your device, navigate to the APK file and tap to install
4. You may need to enable "Install from unknown sources" in your device settings

## Testing

The project has comprehensive tests for both modules:

### KotlinTranslation Tests (24 tests)
Tests for astronomical calculations (latitude, longitude, azimuth)

```
# Run all KotlinTranslation tests
gradlew :KotlinTranslation:test

# Run specific test class
gradlew :KotlinTranslation:test --tests "kotlintranslation.AstroNavigationTests"
```

### StarApp Tests
Tests for utility classes (EXIF extraction, FITS processing, etc.)

```
# Run StarApp tests (uses custom task to avoid AAR metadata issues)
gradlew :StarApp:app:runUnitTests
```

### Troubleshooting

#### Build Issues
If you encounter compilation issues, try cleaning the project first:

```
# Windows
gradlew clean

# macOS/Linux
./gradlew clean
```

Then rebuild:

```
# Windows
gradlew assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

#### Common Issues and Solutions

1. **Gradle Sync Failed**
   - Make sure you're using JDK 11
   - Check your internet connection (needed for downloading dependencies)
   - Try File > Invalidate Caches / Restart in Android Studio

2. **Missing Android SDK Components**
   - Open Android Studio's SDK Manager (Tools > SDK Manager)
   - Install Android SDK Platform 28 and SDK Tools

3. **Execution failed for task ':app:checkDebugAarMetadata'**
   - This is a known issue with the project's dependencies
   - Use the custom task for running tests: `gradlew :StarApp:app:runUnitTests`

4. **Device Compatibility Issues**
   - The app is designed for Android 8.0-9.0 (API levels 26-28)
   - Using newer devices may cause compatibility issues


## Usage

1. Install the app on your Android device (Android 8.0-9.0 recommended)
2. Grant necessary permissions (camera, location, storage)
3. Capture a night sky image (use a tripod for stability)
4. The app will analyze the image and calculate your position
5. View your latitude and longitude on the results screen

## Repository Structure

```
AstroNavigation/
├── StarApp/                    # Android application
│   └── app/src/               
│       ├── main/              # Main application code
│       └── test/              # Unit tests
└── backend/                   
    └── KotlinTranslation/     # Core algorithms
```

## Key Files

- `MainActivity.kt`: Application entry point
- `ResultActivity.kt`: Display captured image with star points overlay
- `StarsActivity.kt`: Star processing and coordinate calculation
- `LocationActivity.kt`: Display calculated coordinates
- `Star_Star_Cluster.kt`: Star data structures and calculations
- `Latt_Long_Calc.kt`: Latitude/longitude algorithms

## Contributing

We welcome contributions to the AstroNavigation project! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
