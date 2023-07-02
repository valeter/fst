package ui.statusbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import ui.common.Settings

private val minFontSize = 6.sp
private val maxFontSize = 40.sp

@Composable
fun StatusBar(settings: Settings) = Box(
    Modifier
        .padding(16.dp, 4.dp, 16.dp, 16.dp)
        .height(32.dp)
        .fillMaxWidth()
) {
    Row(Modifier.fillMaxHeight().align(Alignment.CenterEnd)) {
        Text(
            text = "Text size",
            modifier = Modifier.align(Alignment.CenterVertically),
            color = LocalContentColor.current.copy(alpha = 0.60f),
            fontSize = 12.sp
        )

        Spacer(Modifier.width(8.dp))

        CompositionLocalProvider(LocalDensity provides LocalDensity.current.scale(0.5f)) {
            Slider(
                (settings.fontSize - minFontSize) / (maxFontSize - minFontSize),
                onValueChange = { settings.fontSize = lerp(minFontSize, maxFontSize, it) },
                modifier = Modifier.width(240.dp).align(Alignment.CenterVertically)
            )
        }
    }
}

private fun Density.scale(scale: Float) = Density(density * scale, fontScale * scale)
private operator fun TextUnit.minus(other: TextUnit) = (value - other.value).sp
private operator fun TextUnit.div(other: TextUnit) = value / other.value
