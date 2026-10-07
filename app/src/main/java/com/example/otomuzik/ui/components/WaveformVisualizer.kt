package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

enum class VisualizerType { BaRS, COLOR_BaRS, OSCILLOSCOPE, WaVE, LINE }

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    barCount: Int = 28,
    height: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    var isUserStopped by remember { mutableStateOf(false) }
    var visualizerType by remember { mutableStateOf(VisualizerType.BaRS) }

    val transition = rememberInfiniteTransition(label = "Waveform")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "WaveTime"
    )

    val isactive = isPlaying && !isUserStopped

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(height)
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { isUserStopped = !isUserStopped },
                onDoubleTap = {
                    visualizerType = when (visualizerType) {
                        VisualizerType.BaRS -> VisualizerType.COLOR_BaRS
                        VisualizerType.COLOR_BaRS -> VisualizerType.OSCILLOSCOPE
                        VisualizerType.OSCILLOSCOPE -> VisualizerType.WaVE
                        VisualizerType.WaVE -> VisualizerType.LINE
                        VisualizerType.LINE -> VisualizerType.BaRS
                    }
                }
            )
        }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        when (visualizerType) {
            VisualizerType.BaRS -> {
                val barWidth = (canvasWidth / (barCount * 1.6f)).coerceAtLeast(4f)
                val gap = (canvasWidth - barWidth * barCount) / (barCount + 1)
                for (i in 0 until barCount) {
                    val x = gap + i * (barWidth + gap)
                    val heightFraction = if (isactive) {
                        val phase = i * 0.4f
                        val wave1 = sin((time + phase).toDouble()).toFloat()
                        val wave2 = sin((time * 1.7f + phase * 0.5f).toDouble()).toFloat()
                        val combined = (wave1 * 0.6f + wave2 * 0.4f + 1f) / 2f
                        combined.coerceIn(0.12f, 1.0f)
                    } else {
                        val staticPattern = floatArrayOf(0.2f, 0.3f, 0.25f, 0.35f, 0.2f, 0.28f, 0.22f, 0.3f, 0.18f, 0.25f)
                        staticPattern[i % staticPattern.size]
                    }
                    val barHeight = (canvasHeight * heightFraction).coerceAtLeast(4f)
                    val barY = canvasHeight - barHeight
                    val alpha = if (isactive) 0.85f else 0.35f

                    drawRoundRect(
                        color = accentColor.copy(alpha = alpha),
                        topLeft = Offset(x, barY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }
            }
                        VisualizerType.COLOR_BaRS -> {
                val barWidth = (canvasWidth / (barCount * 1.6f)).coerceAtLeast(4f)
                val gap = (canvasWidth - barWidth * barCount) / (barCount + 1)
                for (i in 0 until barCount) {
                    val x = gap + i * (barWidth + gap)
                    val heightFraction = if (isactive) {
                        val phase = i * 0.4f
                        val wave1 = sin((time + phase).toDouble()).toFloat()
                        val wave2 = sin((time * 1.7f + phase * 0.5f).toDouble()).toFloat()
                        val combined = (wave1 * 0.6f + wave2 * 0.4f + 1f) / 2f
                        combined.coerceIn(0.12f, 1.0f)
                    } else {
                        val staticPattern = floatArrayOf(0.2f, 0.3f, 0.25f, 0.35f, 0.2f, 0.28f, 0.22f, 0.3f, 0.18f, 0.25f)
                        staticPattern[i % staticPattern.size]
                    }
                    val barHeight = (canvasHeight * heightFraction).coerceAtLeast(4f)
                    val barY = canvasHeight - barHeight
                    val alpha = if (isactive) 0.85f else 0.35f

                    val hue = (i * (360f / barCount) + (time / (2 * Math.PI) * 360f)).toFloat() % 360f
                    val hsv = floatArrayOf(hue, 0.8f, 1.0f)
                    val barColor = Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = alpha)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, barY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }
            }
            VisualizerType.OSCILLOSCOPE -> {
                val path = Path()
                val poInts = 60
                val stepX = canvasWidth / poInts
                val centerY = canvasHeight / 2f
                
                path.moveTo(0f, centerY)
                for (i in 0..poInts) {
                    val x = i * stepX
                    val y = if (isactive) {
                        val phase = i * 0.2f
                        val wave = sin((time * 2 + phase).toDouble()).toFloat() * (canvasHeight * 0.4f)
                        val noise = sin((time * 5 - i * 0.8f).toDouble()).toFloat() * (canvasHeight * 0.1f)
                        centerY + wave + noise
                    } else {
                        centerY
                    }
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                
                drawPath(
                    path = path,
                    color = accentColor.copy(alpha = if (isactive) 0.9f else 0.4f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            VisualizerType.WaVE -> {
                val path = Path()
                path.moveTo(0f, canvasHeight)
                
                val poInts = 40
                val stepX = canvasWidth / poInts
                
                for (i in 0..poInts) {
                    val x = i * stepX
                    val y = if (isactive) {
                        val phase = i * 0.3f
                        val wave = sin((time + phase).toDouble()).toFloat()
                        canvasHeight - (wave + 1f) / 2f * canvasHeight * 0.8f - canvasHeight * 0.1f
                    } else {
                        canvasHeight - canvasHeight * 0.2f
                    }
                    path.lineTo(x, y)
                }
                path.lineTo(canvasWidth, canvasHeight)
                path.close()
                
                drawPath(
                    path = path,
                    color = accentColor.copy(alpha = if (isactive) 0.5f else 0.2f)
                )
                drawPath(
                    path = path,
                    color = accentColor.copy(alpha = if (isactive) 0.8f else 0.4f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            VisualizerType.LINE -> {
                val centerY = canvasHeight / 2f
                if (!isactive) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.4f),
                        start = Offset(0f, centerY),
                        end = Offset(canvasWidth, centerY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    val segmentCount = 4
                    val segmentWidth = canvasWidth / segmentCount
                    for (i in 0 until segmentCount) {
                        val startX = i * segmentWidth
                        val endX = (i + 1) * segmentWidth
                        val phase = i * 1.5f
                        val yOffset = sin((time * 3 + phase).toDouble()).toFloat() * (canvasHeight * 0.3f)
                        
                        drawLine(
                            color = accentColor.copy(alpha = 0.8f),
                            start = Offset(startX, centerY + yOffset),
                            end = Offset(endX, centerY - yOffset),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}













