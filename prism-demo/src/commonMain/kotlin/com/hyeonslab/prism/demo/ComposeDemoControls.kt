package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import com.hyeonslab.prism.renderer.Color as RendererColor

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
        text = "Prism Controls",
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

      // Rotation speed slider
      Text(
        "Rotation Speed: ${state.rotationSpeed.toInt()}\u00B0/s",
        style = MaterialTheme.typography.bodySmall,
      )
      Slider(
        value = state.rotationSpeed,
        onValueChange = { onIntent(DemoIntent.SetRotationSpeed(it)) },
        valueRange = 0f..360f,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))

      // Pause / Resume
      Button(onClick = { onIntent(DemoIntent.TogglePause) }, modifier = Modifier.fillMaxWidth()) {
        Text(if (state.isPaused) "Resume" else "Pause")
      }
      Spacer(Modifier.height(16.dp))

      // Color presets
      Text("Cube Color", style = MaterialTheme.typography.bodySmall)
      Spacer(Modifier.height(4.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        ColorPresetButton(RendererColor(0.3f, 0.5f, 0.9f), onIntent)
        ColorPresetButton(RendererColor(0.9f, 0.2f, 0.2f), onIntent)
        ColorPresetButton(RendererColor(0.2f, 0.8f, 0.3f), onIntent)
        ColorPresetButton(RendererColor(1.0f, 0.84f, 0f), onIntent)
        ColorPresetButton(RendererColor(0.6f, 0.2f, 0.9f), onIntent)
        ColorPresetButton(RendererColor(1f, 1f, 1f), onIntent)
      }
    }
  }
}

@Composable
private fun ColorPresetButton(color: RendererColor, onIntent: (DemoIntent) -> Unit) {
  Button(
    onClick = { onIntent(DemoIntent.SetCubeColor(color)) },
    colors =
      ButtonDefaults.buttonColors(
        containerColor = ComposeColor(color.r, color.g, color.b, color.a)
      ),
    shape = CircleShape,
    contentPadding = PaddingValues(0.dp),
    modifier = Modifier.size(32.dp),
  ) {}
}
