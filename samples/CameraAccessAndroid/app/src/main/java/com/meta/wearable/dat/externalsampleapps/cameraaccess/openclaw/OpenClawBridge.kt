package com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw

import com.meta.wearable.dat.externalsampleapps.cameraaccess.settings.SettingsManager
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Executes LiveKit tool requests against the phone-reachable OpenClaw
 * gateway. Secrets stay in Android SharedPreferences and are never included
 * in LiveKit metadata or logs. */
class OpenClawBridge {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(330, TimeUnit.SECONDS)
        .build()

    suspend fun execute(task: String, imageBase64: String?): String = withContext(Dispatchers.IO) {
        if (!SettingsManager.isOpenClawConfigured) {
            throw IOException("Self-hosted OpenClaw is not configured")
        }
        val content: Any = if (imageBase64.isNullOrBlank()) {
            task
        } else {
            JSONArray()
                .put(JSONObject().put("type", "text").put("text", task))
                .put(
                    JSONObject()
                        .put("type", "image_url")
                        .put(
                            "image_url",
                            JSONObject().put("url", "data:image/jpeg;base64,$imageBase64"),
                        ),
                )
        }
        val body = JSONObject()
            .put("model", "openclaw")
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", content)),
            )
            .put("stream", false)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${SettingsManager.openClawBaseUrl.trimEnd('/')}/v1/chat/completions")
            .header("Authorization", "Bearer ${SettingsManager.openClawGatewayToken}")
            .header("x-openclaw-session-key", "agent:main:glass")
            .header("x-openclaw-message-channel", "glass")
            .header("x-openclaw-scopes", "operator.write")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("OpenClaw returned HTTP ${response.code}")
            val contentResult = runCatching {
                JSONObject(responseBody).optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
            }.getOrNull()
            contentResult?.takeIf { it.isNotBlank() } ?: responseBody.takeIf { it.isNotBlank() }
                ?: "OpenClaw completed the request."
        }
    }
}
