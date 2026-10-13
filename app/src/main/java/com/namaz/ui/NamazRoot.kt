package com.namaz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.namaz.ui.home.HomeScreen
import com.namaz.ui.qibla.QiblaScreen
import com.namaz.ui.settings.SettingsScreen

private enum class Tab(val label: String) {
    Times("Время"),
    Qibla("Кибла"),
    Settings("Настройки"),
}

@Composable
fun NamazRoot() {
    var tab by rememberSaveable { mutableStateOf(Tab.Times) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline),
                )
                Row(Modifier.fillMaxWidth()) {
                    Tab.entries.forEach { item ->
                        val selected = item == tab
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { tab = item }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .padding(bottom = 6.dp)
                                    .fillMaxWidth(0.28f)
                                    .height(2.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.background,
                                    ),
                            )
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.Times -> HomeScreen(padding, onOpenSettings = { tab = Tab.Settings })
            Tab.Qibla -> QiblaScreen(padding)
            Tab.Settings -> SettingsScreen(padding)
        }
    }
}
