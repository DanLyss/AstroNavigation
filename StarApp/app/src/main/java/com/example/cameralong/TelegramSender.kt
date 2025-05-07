package com.example.cameralong

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import okhttp3.*
import java.io.File
import java.io.IOException

/**
 * Utility class for sending messages and files to Telegram
 */
object TelegramSender {
    // These should ideally be stored in a secure way, e.g., in BuildConfig or encrypted preferences
    private const val BOT_TOKEN_KEY = "telegram_bot_token"
    private const val CHAT_ID_KEY = "telegram_chat_id"

    private val client = OkHttpClient()
    private lateinit var appContext: Context
    private var botToken: String = ""
    private var chatId: String = ""

    /**
     * Initializes the TelegramSender with the application context
     * @param context The application context
     */
    fun init(context: Context) {
        appContext = context.applicationContext

        // In a real app, these would be retrieved from a secure storage
        // For now, we're using the same values as before
        botToken = "7780735429:AAGM-z5st0BNIlftDWneOPhSsb15SDhcJIs"
        chatId = "-1002506438840"
    }

    /**
     * Checks if internet connection is available
     * @return true if internet is available, false otherwise
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Sends a photo to Telegram
     * @param photo The photo file to send
     * @param location The location string to include in the caption
     * @param angles The angles string to include in the caption
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun sendPhoto(photo: File, location: String, angles: String) {
        if (!photo.exists() || photo.length() == 0L) {
            Log.e("TelegramSender", "❌ File missing or empty: ${photo.absolutePath}")
            return
        }

        if (!this::appContext.isInitialized) {
            Log.e("TelegramSender", "❌ TelegramSender not initialized with context")
            return
        }

        if (!isInternetAvailable()) {
            Log.w("TelegramSender", "⚠️ No internet connection - photo not sent")
            return
        }

        val caption = "📍 Location: $location\n🎯 Angles: $angles"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart(
                "document", photo.name,
                RequestBody.create(MediaType.parse("application/octet-stream"), photo)
            )
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendDocument")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TelegramSender", "❌ Failed to send document: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()
                val body = responseBody?.string()
                if (!response.isSuccessful) {
                    Log.e("TelegramSender", "❌ Telegram error ${response.code()}: $body")
                } else {
                    Log.d("TelegramSender", "✅ Document sent: $body")
                }
                responseBody?.close()
            }
        })
    }

    /**
     * Sends a text message to Telegram
     * @param message The message to send
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun sendText(message: String) {
        if (!this::appContext.isInitialized) {
            Log.e("TelegramSender", "❌ TelegramSender not initialized with context")
            return
        }

        if (!isInternetAvailable()) {
            Log.w("TelegramSender", "⚠️ No internet connection - text not sent")
            return
        }

        val url = "https://api.telegram.org/bot$botToken/sendMessage"

        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", message)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TelegramSender", "❌ Failed to send text: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()
                val resp = responseBody?.string()
                if (!response.isSuccessful) {
                    Log.e("TelegramSender", "❌ Telegram sendMessage error ${response.code()}: $resp")
                } else {
                    Log.d("TelegramSender", "✅ Text sent: $resp")
                }
                responseBody?.close()
            }
        })
    }
}
