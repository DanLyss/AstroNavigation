package com.starapp.navigation.location

import android.content.Context
import android.hardware.*
import android.util.Log
import android.view.Surface
import android.view.WindowManager


class SensorHandler(private val context: Context) : SensorEventListener {
    private val TAG = "SensorHandler"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var lastRotationVector = FloatArray(3)
    private var orientationAngles = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var remappedMatrix = FloatArray(9)
    private var onChangeListener: ((String) -> Unit)? = null

    fun startListening() {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun setOnChangeListener(listener: (String) -> Unit) {
        onChangeListener = listener
    }

    private fun getOrientation(): Pair<Int, Int>{
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay
        val rotation = display.rotation

        return when (rotation) {
            Surface.ROTATION_0   -> SensorManager.AXIS_X to SensorManager.AXIS_Z
            Surface.ROTATION_90  -> SensorManager.AXIS_Z to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Z
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Z to SensorManager.AXIS_X
            else                 -> SensorManager.AXIS_X to SensorManager.AXIS_Z
        }
    }

    fun getLatestAngles(): String {

        //refresh values based on last raw data
        SensorManager.getRotationMatrixFromVector(rotationMatrix, lastRotationVector)


        //get the orientation of the phone
        val axis = getOrientation()
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            axis.first,
            axis.second,
            remappedMatrix
        )
        SensorManager.getOrientation(remappedMatrix, orientationAngles)




        val rawAz = Math.toDegrees(orientationAngles[0].toDouble())
        val azimuth = (rawAz + 360.0) % 360.0
        val pitch = -Math.toDegrees(orientationAngles[1].toDouble())
        val roll = Math.toDegrees(orientationAngles[2].toDouble())

        //uncomment for dynamic angles logging
        //Log.d("SensorHandler", "pre-remap: " + "yaw=$azimuth, pitch=$pitch, roll=$roll")

        return "yaw=$azimuth, pitch=$pitch, roll=$roll"
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            lastRotationVector = event.values.clone()
            onChangeListener?.invoke(getLatestAngles())
        }
    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}
