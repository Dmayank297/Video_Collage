package com.example.videocollage.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videocollage.domain.ProcessingState

@Composable
fun ProcessingLoader(state: ProcessingState.Processing) {
    val stageText = stageToMessage(state.stage)

    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "phase"
    )
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
        label = "bounce"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(200.dp, 64.dp)) {
            val path = Path()
            val amplitude = 14f
            val halfH = size.height / 2f
            val startY = halfH + amplitude * kotlin.math.sin(phase)
            path.moveTo(0f, startY)
            var x = 4f
            while (x <= size.width) {
                val ratio = x / size.width
                val y = halfH + amplitude * kotlin.math.sin(ratio * 4f * Math.PI.toFloat() + phase)
                path.lineTo(x, y)
                x += 4f
            }
            drawPath(path, Color(0xFF7C4DFF), style = Stroke(width = 5f, cap = StrokeCap.Round))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stageText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(
                scaleX = 1f + bounce * 0.04f,
                scaleY = 1f + bounce * 0.04f
            )
        )

        Spacer(Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { state.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF7C4DFF),
            trackColor = Color(0xFFE8E0FF)
        )
    }
}

private fun stageToMessage(stage: String): String = when {
    stage.contains("Start", ignoreCase = true) -> "Getting ready... 🎬"
    stage.contains("Extract", ignoreCase = true) -> "Prepping your ingredients 🎬"
    stage.contains("Detect", ignoreCase = true) -> "Chopping frames into pieces 🔪"
    stage.contains("Cluster", ignoreCase = true) -> "Sorting people onto plates 🍽️"
    stage.contains("Track", ignoreCase = true) -> "Simmering face appearances 🍲"
    stage.contains("Select", ignoreCase = true) -> "Plating the best shots ✨"
    else -> stage.ifEmpty { "Cooking something up..." }
}
