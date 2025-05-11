package com.starapp.navigation.gesture

import android.app.Activity
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

/**
 * Manager class for handling gesture detection
 */
class GestureManager(private val activity: Activity) {

    private lateinit var gestureDetector: GestureDetectorCompat
    private var onSwipeBackCallback: (() -> Unit)? = null

    /**
     * Initialize the gesture detector
     * @param onSwipeBack Callback to be executed when a swipe back gesture is detected
     */
    fun initializeGestureDetector(onSwipeBack: () -> Unit) {
        onSwipeBackCallback = onSwipeBack
        
        gestureDetector = GestureDetectorCompat(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                // Check if it's a left-to-right swipe (to go back)
                if (e1 != null && e2 != null && e2.x - e1.x > 100 && Math.abs(velocityX) > 100) {
                    onSwipeBackCallback?.invoke()
                    return true
                }
                return false
            }
        })
    }

    /**
     * Process a touch event
     * @param event The motion event to process
     * @return true if the event was handled, false otherwise
     */
    fun processTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}