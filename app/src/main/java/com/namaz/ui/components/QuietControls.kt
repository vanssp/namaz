package com.namaz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val background = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun QuietTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
    )
}

@Composable
fun ChoiceRow(title: String, value: String, detail: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Mark(checked)
    }
}

@Composable
private fun Mark(checked: Boolean) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .clip(shape)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (checked) "вкл" else "выкл",
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeadChoices(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 5, 10, 15, 30).forEach { minutes ->
            val chosen = minutes == selected
            val shape = RoundedCornerShape(100.dp)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                    .border(1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
                    .clickable { onSelect(minutes) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = if (minutes == 0) "вовремя" else "$minutes",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
