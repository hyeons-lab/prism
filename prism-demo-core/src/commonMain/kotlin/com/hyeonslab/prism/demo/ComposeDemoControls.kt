package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ComposeDemoControls(
  state: DemoUiState,
  onIntent: (DemoIntent) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.width(260.dp).padding(8.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Prism PBR Controls",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = "FPS: ${state.fps.toInt()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))

      // Env intensity slider
      Text(
        "Env Intensity: ${state.envIntensity.fmt2()}",
        style = MaterialTheme.typography.bodySmall,
      )
      Slider(
        value = state.envIntensity,
        onValueChange = { onIntent(DemoIntent.SetEnvIntensity(it)) },
        valueRange = 0f..2f,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))

      // Metallic preview slider
      Text("Metallic: ${state.metallic.fmt2()}", style = MaterialTheme.typography.bodySmall)
      Slider(
        value = state.metallic,
        onValueChange = { onIntent(DemoIntent.SetMetallic(it)) },
        valueRange = 0f..1f,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))

      // Roughness preview slider
      Text("Roughness: ${state.roughness.fmt2()}", style = MaterialTheme.typography.bodySmall)
      Slider(
        value = state.roughness,
        onValueChange = { onIntent(DemoIntent.SetRoughness(it)) },
        valueRange = 0f..1f,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(16.dp))

      // Pause / Resume
      Button(onClick = { onIntent(DemoIntent.TogglePause) }, modifier = Modifier.fillMaxWidth()) {
        Text(if (state.isPaused) "Resume" else "Pause")
      }
    }
  }
}

/**
 * KMP-compatible 2-decimal-place float formatter.
 *
 * Replaces `"%.2f".format(x)` which is unavailable in Kotlin/WASM.
 */
private fun Float.fmt2(): String {
  val v = (this * 100 + 0.5f).toInt().coerceAtLeast(0)
  return "${v / 100}.${(v % 100).toString().padStart(2, '0')}"
}
