package com.example.cameralong

import android.util.Log
import okhttp3.*
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

object TelegramSender {
    private const val botToken = "7780735429:AAGM-z5st0BNIlftDWneOPhSsb15SDhcJIs"
    private const val chatId = "-1002506438840"
    private val client = OkHttpClient()

    fun sendPhoto(photo: File, location: String, angles: String) {
        if (!photo.exists() || photo.length() == 0L) {
            Log.e("TelegramSender", "❌ File missing or empty: ${photo.absolutePath}")
            return
        }

        val caption = "📍 Location: $location\n🎯 Angles: $angles"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart(
                "document", photo.name,
                photo.asRequestBody("application/octet-stream".toMediaTypeOrNull())
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
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e("TelegramSender", "❌ Telegram error ${response.code}: $body")
                } else {
                    Log.d("TelegramSender", "✅ Document sent: $body")
                }
            }
        })
    }

    fun sendText(message: String) {
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
                val resp = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e("TelegramSender", "❌ Telegram sendMessage error ${response.code}: $resp")
                } else {
                    Log.d("TelegramSender", "✅ Text sent: $resp")
                }
            }
        })
    }
}
