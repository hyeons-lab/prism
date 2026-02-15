package engine.prism.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import engine.prism.compose.PrismOverlay
import engine.prism.compose.PrismTheme
import engine.prism.compose.rememberEngineState
import engine.prism.core.EngineConfig

@Composable
fun ComposeDemoApp() {
  val engineState =
    rememberEngineState(
      config = EngineConfig(appName = "Prism Compose Demo", targetFps = 60, enableDebug = true)
    )
  var showOverlay by remember { mutableStateOf(true) }

  MaterialTheme {
    PrismTheme(engineState) {
      Box(modifier = Modifier.fillMaxSize()) {
        PrismOverlay(engineState = engineState, modifier = Modifier.fillMaxSize()) {
          if (showOverlay) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Surface(color = Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    text = "Prism 3D Engine",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                  )
                  Text(
                    text = "FPS: ${engineState.fps.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(onClick = { showOverlay = !showOverlay }) {
            Text(if (showOverlay) "Hide HUD" else "Show HUD")
          }
        }
      }
    }
  }
}
