package com.starapp.navigation

import androidx.exifinterface.media.ExifInterface
import com.starapp.navigation.util.ExifUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.io.File

@ExtendWith(MockitoExtension::class)
class ExifUtilsTest {

    init {
        println("[DEBUG_LOG] ExifUtilsTest class initialized")
    }

    @Mock
    private lateinit var mockExif: ExifInterface

    @Mock
    private lateinit var mockFile: File

    // No setup needed for current tests

    @Test
    fun extractOrientationAngles_validData_returnsCorrectValues() {
        // Arrange
        val userComment = "Yaw: 10.5, Pitch: -20.3, Roll: 5.7"
        `when`(mockExif.getAttribute(ExifInterface.TAG_USER_COMMENT)).thenReturn(userComment)

        // Act
        val (yaw, pitch, roll) = ExifUtils.extractOrientationAngles(mockExif)

        // Assert
        assertEquals(10.5, yaw, 0.001)
        assertEquals(-20.3, pitch, 0.001)
        assertEquals(5.7, roll, 0.001)
    }

    @Test
    fun extractOrientationAngles_missingUserComment_returnsDefaultValues() {
        // Arrange
        `when`(mockExif.getAttribute(ExifInterface.TAG_USER_COMMENT)).thenReturn(null)

        // Act
        val (yaw, pitch, roll) = ExifUtils.extractOrientationAngles(mockExif)

        // Assert - should return default values
        assertEquals(0.0, yaw, 0.001)
        assertEquals(0.0, pitch, 0.001)
        assertEquals(0.0, roll, 0.001)
    }

    @Test
    fun extractOrientationAngles_invalidFormat_returnsDefaultValues() {
        // Arrange
        val userComment = "Invalid format"
        `when`(mockExif.getAttribute(ExifInterface.TAG_USER_COMMENT)).thenReturn(userComment)

        // Act
        val (yaw, pitch, roll) = ExifUtils.extractOrientationAngles(mockExif)

        // Assert - should return default values
        assertEquals(0.0, yaw, 0.001)
        assertEquals(0.0, pitch, 0.001)
        assertEquals(0.0, roll, 0.001)
    }

    @Test
    fun extractDateTime_validData_returnsCorrectIsoTime() {
        // Arrange
        val dateTime = "2023:05:15 14:30:45"

        `when`(mockExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)).thenReturn(dateTime)

        // Act
        val isoTime = ExifUtils.extractDateTime(mockExif)

        // Assert
        // Since we're now using the system default timezone, we can't assert the exact string
        // Instead, we'll check that it starts with the correct date and time
        assertTrue(isoTime.startsWith("2023-05-15T14:30:45"))
    }

    @Test
    fun extractDateTime_missingDateTime_returnsCurrentTime() {
        // Arrange
        `when`(mockExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)).thenReturn(null)
        `when`(mockExif.getAttribute(ExifInterface.TAG_DATETIME)).thenReturn(null)

        // Act
        val isoTime = ExifUtils.extractDateTime(mockExif)

        // Assert - should return a non-empty string
        assertTrue(isoTime.isNotEmpty())

        // Basic validation that it looks like a date
        assertTrue(isoTime.contains("-") && isoTime.contains(":") && isoTime.contains("T"))
    }

    @Test
    fun extractDateTime_invalidFormat_returnsCurrentTime() {
        // Arrange
        val invalidDateTime = "Invalid date format"
        `when`(mockExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)).thenReturn(invalidDateTime)

        // Act
        val isoTime = ExifUtils.extractDateTime(mockExif)

        // Assert - should return a non-empty string
        assertTrue(isoTime.isNotEmpty())

        // Basic validation that it looks like a date
        assertTrue(isoTime.contains("-") && isoTime.contains(":") && isoTime.contains("T"))
    }
}

