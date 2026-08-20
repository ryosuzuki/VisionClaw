package com.meta.wearable.dat.externalsampleapps.cameraaccess.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which realtime model answers. The choice travels to the agent worker as
 * room-token metadata; the phone never talks to either provider directly.
 */
enum class IntelligenceEngine(val value: String, val label: String) {
    GEMINI("gemini", "Gemini"),
    OPENAI("openai", "OpenAI");

    companion object {
        fun fromValue(value: String?): IntelligenceEngine =
            entries.firstOrNull { it.value == value } ?: GEMINI
    }
}

/** Complete runtime backend. CLOUD uses VisionClaw's hosted LiveKit path.
 * SELF_HOSTED runs Gemini Live and OpenClaw directly on the phone, so Sean's
 * access code and hosted gateway are not involved. */
enum class ActionBackend(val value: String, val label: String) {
    CLOUD("cloud", "Cloud"),
    SELF_HOSTED("openclaw", "Self-hosted OpenClaw");

    companion object {
        fun fromValue(value: String?): ActionBackend =
            entries.firstOrNull { it.value == value } ?: CLOUD
    }
}

/**
 * Where video comes from. The app is a vision assistant first -- it opens
 * looking at the world through the phone -- and glasses are one capture
 * source, selected in Settings, rather than a mode the user must decide about
 * at launch. Exposed as a flow so the root scaffold swaps the capture
 * pipeline live when the setting changes.
 */
enum class CaptureSource(val value: String, val label: String) {
    PHONE("phone", "Phone Camera"),
    GLASSES("glasses", "Glasses");

    companion object {
        fun fromValue(value: String?): CaptureSource =
            entries.firstOrNull { it.value == value } ?: PHONE
    }
}

object SettingsManager {
    private const val PREFS_NAME = "visionclaw_settings"
    private const val DEFAULT_SIGNALING_URL = "wss://YOUR_SIGNALING_SERVER"
    private const val DEFAULT_GATEWAY_BASE_URL = "https://api.visionagents.app"

    private lateinit var prefs: SharedPreferences

    private val _captureSourceFlow = MutableStateFlow(CaptureSource.PHONE)
    val captureSourceFlow: StateFlow<CaptureSource> = _captureSourceFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _captureSourceFlow.value = CaptureSource.fromValue(prefs.getString("captureSource", null))
    }

    var actionBackend: ActionBackend
        get() = ActionBackend.fromValue(prefs.getString("actionBackend", null))
        set(value) = prefs.edit().putString("actionBackend", value.value).apply()

    var captureSource: CaptureSource
        get() = _captureSourceFlow.value
        set(value) {
            prefs.edit().putString("captureSource", value.value).apply()
            _captureSourceFlow.value = value
        }

    /** Full base URL of the hosted gateway, scheme included (e.g. "https://gw.example.com"). */
    var gatewayBaseUrl: String
        get() = prefs.getString("gatewayBaseUrl", null) ?: DEFAULT_GATEWAY_BASE_URL
        set(value) = prefs.edit().putString("gatewayBaseUrl", value).apply()

    var gatewayToken: String
        get() = prefs.getString("gatewayToken", "").orEmpty()
        set(value) = prefs.edit().putString("gatewayToken", value).apply()

    /** Gemini Live credential used only by direct Self-hosted mode. Existing
     * Ryo builds stored this under the same key, so an in-place upgrade keeps
     * working without exporting or re-entering the secret. */
    var geminiAPIKey: String
        get() = prefs.getString("geminiAPIKey", "").orEmpty()
        set(value) = prefs.edit().putString("geminiAPIKey", value).apply()

    var geminiSystemPrompt: String
        get() = prefs.getString("geminiSystemPrompt", null) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString("geminiSystemPrompt", value).apply()

    // An unfilled Secrets.kt.example placeholder is not empty, so without this
    // a fresh clone reports "configured" and then fails with a 401 that looks
    // like a server problem rather than a missing token.
    val isGatewayConfigured: Boolean
        get() = gatewayBaseUrl.startsWith("http") &&
            gatewayToken.isNotEmpty() &&
            !gatewayToken.startsWith("YOUR_")

    /** Phone-reachable OpenClaw URL, normally its Tailscale address. */
    var openClawBaseUrl: String
        get() = prefs.getString("openClawBaseUrl", null)
            // Migrate the older Ryo build without reading or logging its token.
            ?: prefs.getString("openClawHost", null)?.let { host ->
                val port = prefs.getInt("openClawPort", 18789)
                "${host.trimEnd('/')}:$port"
            }
            ?: "http://100.118.73.1:18789"
        set(value) = prefs.edit().putString("openClawBaseUrl", value).apply()

    var openClawGatewayToken: String
        get() = prefs.getString("openClawGatewayToken", "").orEmpty()
        set(value) = prefs.edit().putString("openClawGatewayToken", value).apply()

    /** Compatibility properties consumed by the proven direct OpenClaw
     * client from Ryo's beta branch. New settings store one canonical URL. */
    val openClawHost: String
        get() {
            val url = openClawBaseUrl.trimEnd('/')
            val schemeEnd = url.indexOf("://")
            val portSeparator = url.lastIndexOf(':')
            return if (schemeEnd >= 0 && portSeparator > schemeEnd + 2) {
                url.substring(0, portSeparator)
            } else {
                url
            }
        }

    val openClawPort: Int
        get() = openClawBaseUrl.trimEnd('/').substringAfterLast(':').toIntOrNull() ?: 18789

    var openClawHookToken: String
        get() = prefs.getString("openClawHookToken", "").orEmpty()
        set(value) = prefs.edit().putString("openClawHookToken", value).apply()

    val isOpenClawConfigured: Boolean
        get() = openClawBaseUrl.startsWith("http") && openClawGatewayToken.isNotBlank()

    var intelligenceEngine: IntelligenceEngine
        get() = IntelligenceEngine.fromValue(prefs.getString("intelligenceEngine", null))
        set(value) = prefs.edit().putString("intelligenceEngine", value.value).apply()

    var showCaptions: Boolean
        get() = prefs.getBoolean("showCaptions", true)
        set(value) = prefs.edit().putBoolean("showCaptions", value).apply()

    var videoStreamingEnabled: Boolean
        get() = prefs.getBoolean("videoStreamingEnabled", true)
        set(value) = prefs.edit().putBoolean("videoStreamingEnabled", value).apply()

    var proactiveNotificationsEnabled: Boolean
        get() = prefs.getBoolean("proactiveNotificationsEnabled", true)
        set(value) = prefs.edit().putBoolean("proactiveNotificationsEnabled", value).apply()

    var demoSpeakerModeEnabled: Boolean
        get() = prefs.getBoolean("demoSpeakerModeEnabled", false)
        set(value) = prefs.edit().putBoolean("demoSpeakerModeEnabled", value).apply()

    var webrtcSignalingURL: String
        get() = prefs.getString("webrtcSignalingURL", null) ?: DEFAULT_SIGNALING_URL
        set(value) = prefs.edit().putString("webrtcSignalingURL", value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
        _captureSourceFlow.value = CaptureSource.PHONE
    }

    const val DEFAULT_SYSTEM_PROMPT = """You are a concise real-time voice assistant for someone using a phone or Meta Ray-Ban glasses. You can see the current camera stream. You have two tools: execute and capture_photo. Use execute for any task requiring personal data, memory, web research, messaging, calendars, apps, services, or persistent actions. Use capture_photo when asked to save the current view. Before execute, briefly acknowledge the request. After execute returns, answer from its result in the user's language. Never claim an external action succeeded without a successful tool result."""
}
