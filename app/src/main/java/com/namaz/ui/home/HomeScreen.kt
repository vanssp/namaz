package com.namaz.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.namaz.ui.components.Hairline
import com.namaz.ui.components.QuietButton
import com.namaz.ui.components.QuietTextButton
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshSystemHints()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermission(granted)
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshSystemHints() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Header(state.weekday, state.dateLine, state.hijriLine)
        Spacer(Modifier.height(36.dp))
        if (!state.hasLocation) {
            EmptyPlace(
                locating = state.locating,
                banner = state.banner,
                onLocate = {
                    locationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                onManual = onOpenSettings,
            )
        } else {
            Text(
                "Следующий намаз",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                state.heroTitle.orEmpty(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                state.heroTime.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.countdown,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(28.dp))
            Hairline()
            state.prayers.forEach { prayer ->
                PrayerLine(prayer)
            }
            Spacer(Modifier.height(22.dp))
            PlaceFooter(
                place = state.place,
                coordinates = state.coordinates,
                method = state.method,
                locating = state.locating,
                onRefresh = { viewModel.refreshLocation(quiet = false) },
            )
            state.banner?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.askNotifications || state.askExactAlarm) {
            Spacer(Modifier.height(22.dp))
            Hint(
                text = if (state.askNotifications) {
                    "Разрешите уведомления, чтобы азан звучал вовремя."
                } else {
                    "Разрешите точные будильники, иначе напоминание может запоздать."
                },
                action = if (state.askNotifications) "Разрешить" else "Открыть",
                onAction = {
                    if (state.askNotifications && Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            },
                        )
                    }
                },
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Header(weekday: String, date: String, hijri: String) {
    Box(Modifier.fillMaxWidth()) {
        StarMark(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp)
                .size(92.dp),
        )
        Column {
            Text(
                weekday.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(date, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(hijri, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun StarMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        val inner = outer * 0.42f
        val path = Path()
        for (index in 0 until 16) {
            val radius = if (index % 2 == 0) outer else inner
            val angle = Math.toRadians(index * 22.5 - 90.0)
            val x = center.x + (cos(angle) * radius).toFloat()
            val y = center.y + (sin(angle) * radius).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

@Composable
private fun PrayerLine(prayer: PrayerRowState) {
    val color = when {
        prayer.highlighted -> MaterialTheme.colorScheme.onBackground
        prayer.passed || prayer.quiet -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onBackground
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(16.dp)
                .background(
                    if (prayer.highlighted) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.background,
                ),
        )
        Spacer(Modifier.width(14.dp))
        Text(prayer.title, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        Text(
            prayer.time,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = color,
        )
    }
}

@Composable
private fun PlaceFooter(
    place: String,
    coordinates: String,
    method: String,
    locating: Boolean,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(place, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "$coordinates · $method",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QuietTextButton(if (locating) "Определяем…" else "Обновить место", onClick = onRefresh)
    }
}

@Composable
private fun EmptyPlace(
    locating: Boolean,
    banner: String?,
    onLocate: () -> Unit,
    onManual: () -> Unit,
) {
    Text(
        "Место не задано",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Время намаза и направление киблы считаются от вашей точки на земле.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(22.dp))
    QuietButton(
        text = if (locating) "Определяем…" else "Определить место",
        onClick = onLocate,
        enabled = !locating,
    )
    Spacer(Modifier.height(8.dp))
    QuietTextButton("Ввести координаты", onClick = onManual)
    if (banner != null) {
        Spacer(Modifier.height(8.dp))
        Text(banner, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Hint(text: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            action,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onAction),
            textAlign = TextAlign.Start,
        )
    }
}
