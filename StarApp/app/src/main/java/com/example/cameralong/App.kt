package com.example.cameralong

import android.app.Application
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CrashHandler", "App crashed", throwable)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val crashFile = File(getExternalFilesDir(null), "crash_$timestamp.txt")
            crashFile.writeText(throwable.stackTraceToString())

            TelegramSender.sendPhoto(crashFile, "Crash Log", "App crashed")

            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }
}
