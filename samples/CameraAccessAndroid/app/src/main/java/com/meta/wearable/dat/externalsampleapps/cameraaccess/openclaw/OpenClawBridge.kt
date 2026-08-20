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

/** Executes an action requested by Sean's LiveKit agent against a phone-local
 * OpenClaw configuration. The URL and credential never enter LiveKit. */
class OpenClawBridge {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(330, TimeUnit.SECONDS)
        .build()

    suspend fun execute(task: String, imageBase64: String?): String = withContext(Dispatchers.IO) {
        if (!SettingsManager.isOpenClawConfigured) {
            throw IOException("OpenClaw is not configured")
        }
        val agentId = SettingsManager.openClawAgentId
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
            .put("model", "openclaw/$agentId")
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
            .header("x-openclaw-agent-id", agentId)
            .header("x-openclaw-session-key", "agent:$agentId:glass")
            .header("x-openclaw-message-channel", "glass")
            .header("x-openclaw-scopes", "operator.write")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                }.getOrNull()?.takeIf { it.isNotBlank() }
                throw IOException(detail ?: "OpenClaw returned HTTP ${response.code}")
            }
            val result = runCatching {
                JSONObject(responseBody).optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
            }.getOrNull()
            result?.takeIf { it.isNotBlank() }
                ?: responseBody.takeIf { it.isNotBlank() }
                ?: "OpenClaw completed the request."
        }
    }
}
