package com.namaz.ui.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.namaz.domain.formatDistanceKm
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    contentPadding: PaddingValues,
    viewModel: QiblaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }
    val turn = remember(state.heading, state.bearing) {
        val heading = state.heading
        val bearing = state.bearing
        if (heading == null || bearing == null) null else signedDelta(heading, bearing)
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            "Кибла",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            state.place.ifBlank { "Направление на Каабу" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        CompassDial(
            heading = state.heading ?: 0f,
            bearing = state.bearing,
            aligned = turn != null && abs(turn) <= 3f,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = "Компас киблы" },
        )
        Text(
            state.bearing?.let { "${it.roundToInt()}°" } ?: "—",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        state.distanceKm?.let { distance ->
            Text(
                "до Каабы ${formatDistanceKm(distance)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            statusText(state, turn),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        if (state.hasLocation && state.sensorAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Магнитное склонение учтено",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CompassDial(
    heading: Float,
    bearing: Double?,
    aligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val brass = MaterialTheme.colorScheme.secondary
    val line = MaterialTheme.colorScheme.outline
    val measurer = rememberTextMeasurer()
    val letterStyle = TextStyle(color = ink, fontSize = 16.sp, fontFamily = FontFamily.Serif)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2f * 0.72f
            fun at(angle: Double, distance: Float): Offset {
                val radians = Math.toRadians(angle - heading)
                return Offset(
                    center.x + (sin(radians) * distance).toFloat(),
                    center.y + (-cos(radians) * distance).toFloat(),
                )
            }
            drawCircle(line, radius, center, style = Stroke(1.4.dp.toPx()))
            drawCircle(line.copy(alpha = 0.55f), radius * 0.68f, center, style = Stroke(1.dp.toPx()))
            for (degree in 0 until 360 step 5) {
                val inner = radius - when {
                    degree % 90 == 0 -> 16.dp.toPx()
                    degree % 15 == 0 -> 10.dp.toPx()
                    else -> 5.dp.toPx()
                }
                drawLine(
                    color = if (degree % 90 == 0) ink else muted.copy(alpha = 0.65f),
                    start = at(degree.toDouble(), radius),
                    end = at(degree.toDouble(), inner),
                    strokeWidth = if (degree % 90 == 0) 1.6.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            listOf("С" to 0.0, "В" to 90.0, "Ю" to 180.0, "З" to 270.0).forEach { (letter, angle) ->
                val point = at(angle, radius - 34.dp.toPx())
                val layout = measurer.measure(letter, style = letterStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        point.x - layout.size.width / 2f,
                        point.y - layout.size.height / 2f,
                    ),
                )
            }
            if (bearing != null) {
                drawLine(
                    brass,
                    at(bearing, radius * 0.18f),
                    at(bearing, radius - 8.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(brass, 5.dp.toPx(), at(bearing, radius + 2.dp.toPx()))
            }
            drawCircle(ink, 3.5.dp.toPx(), center)
            val marker = if (aligned) brass else ink
            val top = Offset(center.x, center.y - radius - 14.dp.toPx())
            val triangle = Path().apply {
                moveTo(top.x, top.y)
                lineTo(top.x - 6.dp.toPx(), top.y + 10.dp.toPx())
                lineTo(top.x + 6.dp.toPx(), top.y + 10.dp.toPx())
                close()
            }
            drawPath(triangle, marker)
        }
    }
}

private fun signedDelta(heading: Float, bearing: Double): Float {
    var delta = bearing.toFloat() - heading
    delta = (delta + 540f) % 360f - 180f
    return delta
}

private fun statusText(state: QiblaUiState, turn: Float?): String = when {
    !state.hasLocation -> "Сначала определите место — кибла считается от него"
    !state.sensorAvailable -> "В этом устройстве нет магнитометра"
    state.heading == null -> "Поверните телефон, компас ищет север"
    state.lowAccuracy -> "Отойдите от металла и опишите телефоном восьмёрку"
    turn == null -> "Поверните устройство"
    abs(turn) <= 3f -> "Вы смотрите в сторону киблы"
    turn > 0f -> "Поверните вправо на ${abs(turn).roundToInt()}°"
    else -> "Поверните влево на ${abs(turn).roundToInt()}°"
}
