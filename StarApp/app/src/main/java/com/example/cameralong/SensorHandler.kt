package com.example.cameralong

import android.content.Context
import android.hardware.*
import kotlin.math.roundToInt

class SensorHandler(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var orientationAngles = FloatArray(3)
    private var rotationMatrix = FloatArray(9)

    private var onChangeListener: ((String) -> Unit)? = null

    fun startListening() {
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun setOnChangeListener(listener: (String) -> Unit) {
        onChangeListener = listener
    }

    fun getLatestAngles(): String {
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble())
        val pitch = Math.toDegrees(orientationAngles[1].toDouble())
        val roll = Math.toDegrees(orientationAngles[2].toDouble())
        return "Azimuth: $azimuth°, Pitch: $pitch°, Roll: $roll°"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            onChangeListener?.invoke(getLatestAngles())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}
