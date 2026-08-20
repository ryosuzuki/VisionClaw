package com.meta.wearable.dat.externalsampleapps.cameraaccess.settings

import android.content.Context
import android.content.SharedPreferences
import com.meta.wearable.dat.externalsampleapps.cameraaccess.Secrets
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

/** Selects only the action/tool executor. Voice, video, captions, cards, and
 * session orchestration remain on Sean's current LiveKit implementation. */
enum class ActionBackend(val value: String, val label: String) {
    CLOUD("cloud", "Cloud tools"),
    OPENCLAW("openclaw", "OpenClaw");

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
    private const val DEFAULT_LOCAL_LIVEKIT_GATEWAY = "http://100.118.73.1:8788"

    private lateinit var prefs: SharedPreferences

    private val _captureSourceFlow = MutableStateFlow(CaptureSource.PHONE)
    val captureSourceFlow: StateFlow<CaptureSource> = _captureSourceFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _captureSourceFlow.value = CaptureSource.fromValue(prefs.getString("captureSource", null))
    }

    var captureSource: CaptureSource
        get() = _captureSourceFlow.value
        set(value) {
            prefs.edit().putString("captureSource", value.value).apply()
            _captureSourceFlow.value = value
        }

    var actionBackend: ActionBackend
        get() = ActionBackend.fromValue(prefs.getString("actionBackend", null))
        set(value) = prefs.edit().putString("actionBackend", value.value).apply()

    /** Full base URL of the hosted gateway, scheme included (e.g. "https://gw.example.com"). */
    var gatewayBaseUrl: String
        get() = prefs.getString("gatewayBaseUrl", null) ?: Secrets.gatewayBaseUrl
        set(value) = prefs.edit().putString("gatewayBaseUrl", value).apply()

    var gatewayToken: String
        get() = prefs.getString("gatewayToken", null) ?: Secrets.gatewayToken
        set(value) = prefs.edit().putString("gatewayToken", value).apply()

    // An unfilled Secrets.kt.example placeholder is not empty, so without this
    // a fresh clone reports "configured" and then fails with a 401 that looks
    // like a server problem rather than a missing token.
    val isGatewayConfigured: Boolean
        get() = gatewayBaseUrl.startsWith("http") &&
            gatewayToken.isNotEmpty() &&
            !gatewayToken.startsWith("YOUR_")

    /** Phone-reachable OpenClaw endpoint, normally exposed only on Tailnet. */
    var openClawBaseUrl: String
        get() = prefs.getString("openClawBaseUrl", null)
            ?: prefs.getString("openClawHost", null)?.let { host ->
                val port = prefs.getInt("openClawPort", 18789)
                "${host.trimEnd('/')}:$port"
            }
            ?: ""
        set(value) = prefs.edit().putString("openClawBaseUrl", value).apply()

    var openClawGatewayToken: String
        get() = prefs.getString("openClawGatewayToken", "").orEmpty()
        set(value) = prefs.edit().putString("openClawGatewayToken", value).apply()

    var openClawAgentId: String
        get() = prefs.getString("openClawAgentId", "main").orEmpty().ifBlank { "main" }
        set(value) = prefs.edit().putString("openClawAgentId", value.ifBlank { "main" }).apply()

    val isOpenClawConfigured: Boolean
        get() = openClawBaseUrl.startsWith("http") && openClawGatewayToken.isNotBlank()

    /** Tailnet-only ticket service hosted beside OpenClaw on the Mac Studio. */
    var localLiveKitGatewayUrl: String
        get() = prefs.getString("localLiveKitGatewayUrl", DEFAULT_LOCAL_LIVEKIT_GATEWAY)
            .orEmpty().ifBlank { DEFAULT_LOCAL_LIVEKIT_GATEWAY }
        set(value) = prefs.edit().putString("localLiveKitGatewayUrl", value).apply()

    val isSelfHostedConfigured: Boolean
        get() = isOpenClawConfigured && localLiveKitGatewayUrl.startsWith("http")

    var intelligenceEngine: IntelligenceEngine
        get() = IntelligenceEngine.fromValue(prefs.getString("intelligenceEngine", null))
        set(value) = prefs.edit().putString("intelligenceEngine", value.value).apply()

    var showCaptions: Boolean
        get() = prefs.getBoolean("showCaptions", true)
        set(value) = prefs.edit().putBoolean("showCaptions", value).apply()

    var webrtcSignalingURL: String
        get() = prefs.getString("webrtcSignalingURL", null) ?: DEFAULT_SIGNALING_URL
        set(value) = prefs.edit().putString("webrtcSignalingURL", value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
        _captureSourceFlow.value = CaptureSource.PHONE
    }
}
