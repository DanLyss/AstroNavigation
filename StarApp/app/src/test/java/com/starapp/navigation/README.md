# Unit Tests for AstroNavigation StarApp

This directory contains unit tests for the utility classes in the AstroNavigation StarApp.

## Test Classes

### ExifUtilsTest

Tests for the `ExifUtils` class, which handles EXIF data extraction from image files.

- Tests extracting orientation angles (yaw, pitch, roll) from EXIF data
- Tests extracting date/time information from EXIF data
- Tests error cases (missing or invalid data)

### FitsUtilsTest

Tests for the `FitsUtils` class, which handles FITS file operations.

- Tests basic error handling (file not found)
- Contains placeholders for more comprehensive tests with real FITS files

### TelegramSenderTest

Tests for the `TelegramSender` class, which handles sending messages and files to Telegram.

- Tests that it doesn't send requests when the file doesn't exist
- Tests that it doesn't send requests when the file is empty
- Tests that it doesn't send requests when there's no internet connection
- Contains placeholders for more comprehensive tests with mocked network requests

## Running the Tests

To run the tests, use the following Gradle command:

```
./gradlew test
```

Or run the tests from Android Studio by right-clicking on the test class and selecting "Run".

## Test Coverage

The current tests focus on basic functionality and error handling. More comprehensive tests would include:

- Testing with real FITS files
- Testing with various error conditions
- Testing with mocked network requests
- Testing with various network conditions
- Testing with different Android versions