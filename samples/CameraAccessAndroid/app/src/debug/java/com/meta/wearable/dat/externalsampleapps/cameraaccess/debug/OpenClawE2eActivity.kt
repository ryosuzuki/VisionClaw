package com.meta.wearable.dat.externalsampleapps.cameraaccess.debug

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.OpenClawBridge
import com.meta.wearable.dat.externalsampleapps.cameraaccess.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Debug-only fixed-input smoke test. No credential or arbitrary prompt is exposed. */
class OpenClawE2eActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var status by mutableStateOf("Starting OpenClaw device test…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        SettingsManager.init(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Sean VisionClaw + OpenClaw", style = MaterialTheme.typography.headlineMedium)
                        Text(status, modifier = Modifier.padding(top = 24.dp))
                    }
                }
            }
        }
        scope.launch {
            if (!SettingsManager.isOpenClawConfigured) {
                status = "BLOCKED: OpenClaw settings missing"
                return@launch
            }
            status = try {
                val result = OpenClawBridge().execute(
                    "Authorized VisionClaw device smoke test. Reply exactly: VISIONCLAW_SEAN_OPENCLAW_OK",
                    null,
                )
                if (result.contains("VISIONCLAW_SEAN_OPENCLAW_OK")) {
                    "PASS: VISIONCLAW_SEAN_OPENCLAW_OK"
                } else {
                    "FAIL: unexpected response"
                }
            } catch (e: Exception) {
                "FAIL: ${e.message ?: e::class.java.simpleName}"
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
