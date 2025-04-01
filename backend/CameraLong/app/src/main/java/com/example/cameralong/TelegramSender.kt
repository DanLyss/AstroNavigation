package com.example.cameralong

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

object TelegramSender {

    private const val TAG = "TelegramSender"

    private const val BOT_TOKEN = "#####################"
    private const val CHAT_ID = "##################"

    private val client = OkHttpClient()

    fun sendFiles(photo: File, json: File) {
        sendDocument(photo, "📸 Фотка")
        sendDocument(json, "📝 Метаданные")
    }

    private fun sendDocument(file: File, caption: String) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", CHAT_ID)
            .addFormDataPart("caption", caption)
            .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$BOT_TOKEN/sendDocument")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Ошибка отправки ${file.name}: ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Telegram API ошибка для ${file.name}: ${response.code} - ${response.message}")
                    Log.e(TAG, "Ответ: ${response.body?.string()}")
                } else {
                    Log.d(TAG, "✅ Успешно отправлено: ${file.name}")
                }
            }
        })
    }
}
