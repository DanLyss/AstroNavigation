package com.starapp.navigation.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Manager class for handling permission requests
 */
class PermissionManager(private val context: Context) {

    companion object {
        const val CAMERA_PERMISSION_CODE = 100
    }

    /**
     * Check if all required permissions are granted
     * @return true if all permissions are granted, false otherwise
     */
    fun areAllPermissionsGranted(): Boolean {
        val neededPermissions = getRequiredPermissions()
        return neededPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request all required permissions
     * @param activity The activity to request permissions from
     */
    fun requestPermissions(activity: Activity) {
        val neededPermissions = getRequiredPermissions()
        val permissionsToRequest = neededPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    /**
     * Check if permission results indicate all permissions were granted
     * @param requestCode The request code from onRequestPermissionsResult
     * @param grantResults The grant results from onRequestPermissionsResult
     * @return true if all permissions were granted, false otherwise
     */
    fun handlePermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        return requestCode == CAMERA_PERMISSION_CODE && 
               grantResults.isNotEmpty() && 
               grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Get the list of required permissions
     * @return Array of required permission strings
     */
    private fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}