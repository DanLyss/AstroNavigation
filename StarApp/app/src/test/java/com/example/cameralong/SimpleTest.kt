package com.example.cameralong

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class SimpleTest {
    
    init {
        println("[DEBUG_LOG] SimpleTest class initialized")
    }
    
    @Test
    fun simpleTest() {
        println("[DEBUG_LOG] Running simple test")
        assertEquals(4, 2 + 2)
    }
}