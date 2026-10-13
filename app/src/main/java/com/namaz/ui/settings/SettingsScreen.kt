package com.namaz.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.namaz.data.reminds
import com.namaz.domain.AsrMadhab
import com.namaz.domain.CalculationMethod
import com.namaz.domain.HighLatitudeRule
import com.namaz.domain.Prayer
import com.namaz.ui.components.ChoiceRow
import com.namaz.ui.components.Hairline
import com.namaz.ui.components.LeadChoices
import com.namaz.ui.components.QuietTextButton
import com.namaz.ui.components.SectionLabel
import com.namaz.ui.components.SwitchRow

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var methodOpen by rememberSaveable { mutableStateOf(false) }
    var latitudeRuleOpen by rememberSaveable { mutableStateOf(false) }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.reloadCalendars() }

    LaunchedEffect(settings.latitude, settings.longitude) {
        if (latitude.isBlank()) latitude = settings.latitude?.toString().orEmpty()
        if (longitude.isBlank()) longitude = settings.longitude?.toString().orEmpty()
    }
    LaunchedEffect(settings.calendarEnabled) {
        if (settings.calendarEnabled) viewModel.reloadCalendars()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Настройки", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(28.dp))

        SectionLabel("Расчёт")
        ChoiceRow(title = "Метод", value = settings.method.title, detail = settings.method.detail) {
            methodOpen = true
        }
        Text("Аср", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AsrMadhab.entries.forEach { madhab ->
            SelectLine(
                title = madhab.title,
                detail = if (madhab == AsrMadhab.HANAFI) "тень в два роста" else "тень в один рост",
                selected = settings.madhab == madhab,
            ) { viewModel.setMadhab(madhab) }
        }
        ChoiceRow(
            title = "Высокие широты",
            value = settings.highLatitude.title,
            detail = "Для белых ночей, когда фаджр или иша не наступают",
        ) { latitudeRuleOpen = true }
        Spacer(Modifier.height(8.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))

        SectionLabel("Место")
        Spacer(Modifier.height(8.dp))
        SwitchRow("Определять автоматически", settings.autoLocation, viewModel::setAutoLocation)
        if (!settings.autoLocation) {
            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = { Text("Широта") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = { Text("Долгота") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            QuietTextButton(
                text = "Сохранить координаты",
                onClick = { viewModel.setManualLocation(latitude, longitude) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))

        SectionLabel("Напоминания")
        Spacer(Modifier.height(8.dp))
        SwitchRow("Напоминать о намазе", settings.remindersEnabled, viewModel::setReminders)
        if (settings.remindersEnabled) {
            SwitchRow("Звук азана", settings.soundEnabled, viewModel::setSound)
            Spacer(Modifier.height(8.dp))
            Text("За сколько", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            LeadChoices(settings.leadMinutes, viewModel::setLead)
            Spacer(Modifier.height(8.dp))
            Prayer.entries.forEach { prayer ->
                SwitchRow(prayer.title, settings.reminds(prayer), { viewModel.setReminder(prayer, it) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))

        SectionLabel("Поправка, минуты")
        Text(
            "Если мечеть рядом считает иначе, сдвиньте время.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        Prayer.entries.forEach { prayer ->
            OffsetRow(
                title = prayer.title,
                minutes = settings.offsets[prayer] ?: 0,
                onChange = { viewModel.setOffset(prayer, it) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))

        SectionLabel("Календарь")
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            title = "Записывать намазы",
            subtitle = "На семь дней вперёд, без отдельного будильника календаря",
            checked = settings.calendarEnabled,
            onChecked = { enabled ->
                if (enabled) {
                    calendarPermission.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                    )
                }
                viewModel.setCalendarEnabled(enabled)
            },
        )
        if (settings.calendarEnabled) {
            if (calendars.isEmpty()) {
                Text(
                    "Нет календаря, в который можно писать. Добавьте его в системе и обновите список.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                QuietTextButton("Обновить список", viewModel::reloadCalendars)
            } else {
                calendars.forEach { calendar ->
                    SelectLine(
                        title = calendar.name,
                        detail = calendar.account,
                        selected = settings.calendarId == calendar.id,
                    ) { viewModel.selectCalendar(calendar.id) }
                }
                QuietTextButton("Синхронизировать сейчас", viewModel::syncNow)
                QuietTextButton("Удалить события намаза", viewModel::clearCalendar)
            }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Время считается по положению солнца. У разных методов разница бывает в несколько минут.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
    }

    if (methodOpen) {
        OptionDialog(
            title = "Метод расчёта",
            options = CalculationMethod.entries,
            selected = settings.method,
            label = { it.title },
            detail = { it.detail },
            onSelect = viewModel::setMethod,
            onDismiss = { methodOpen = false },
        )
    }
    if (latitudeRuleOpen) {
        OptionDialog(
            title = "Высокие широты",
            options = HighLatitudeRule.entries,
            selected = settings.highLatitude,
            label = { it.title },
            detail = {
                when (it) {
                    HighLatitudeRule.NONE -> "Оставить пустым, если угла нет"
                    HighLatitudeRule.MIDDLE_OF_NIGHT -> "Половина ночи от заката до восхода"
                    HighLatitudeRule.ANGLE_BASED -> "Доля ночи по углу метода"
                    HighLatitudeRule.ONE_SEVENTH -> "Седьмая часть ночи"
                }
            },
            onSelect = viewModel::setHighLatitude,
            onDismiss = { latitudeRuleOpen = false },
        )
    }
}

@Composable
private fun SelectLine(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            if (selected) "●" else "○",
            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun OffsetRow(title: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { onChange(minutes - 1) }, enabled = minutes > -30) { Text("−") }
        Text(
            text = if (minutes > 0) "+$minutes" else minutes.toString(),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        TextButton(onClick = { onChange(minutes + 1) }, enabled = minutes < 30) { Text("+") }
    }
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    detail: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    SelectLine(label(option), detail(option), option == selected) {
                        onSelect(option)
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}
