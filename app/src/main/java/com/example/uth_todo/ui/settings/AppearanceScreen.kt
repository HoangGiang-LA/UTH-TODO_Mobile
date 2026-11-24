package com.example.uth_todo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.ThemeMode

@Composable
fun AppearanceScreen(state: AppearanceUiState, onBack: () -> Unit, onMode: (ThemeMode) -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
            .testTag("appearance_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("appearance_back")) { Text(stringResource(R.string.auth_back)) }
            Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.appearance_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.isLoading || state.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("appearance_progress"))
            state.error?.let { error ->
                Text(stringResource(if (error == AppearanceError.READ) R.string.appearance_read_error else R.string.appearance_save_error),
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("appearance_error"))
                if (error == AppearanceError.READ) TextButton(onClick = onRetry, modifier = Modifier.testTag("appearance_retry")) {
                    Text(stringResource(R.string.retry))
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth().selectable(selected = state.mode == mode,
                            enabled = !state.isLoading && !state.isSaving, role = Role.RadioButton, onClick = { onMode(mode) })
                            .padding(horizontal = 12.dp, vertical = 10.dp).heightIn(min = 56.dp).testTag("appearance_mode_${mode.name}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RadioButton(selected = state.mode == mode, onClick = null, enabled = !state.isLoading && !state.isSaving)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(when (mode) {
                                    ThemeMode.SYSTEM -> R.string.appearance_system
                                    ThemeMode.LIGHT -> R.string.appearance_light
                                    ThemeMode.DARK -> R.string.appearance_dark
                                }), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(when (mode) {
                                    ThemeMode.SYSTEM -> R.string.appearance_system_description
                                    ThemeMode.LIGHT -> R.string.appearance_light_description
                                    ThemeMode.DARK -> R.string.appearance_dark_description
                                }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Text(stringResource(if (state.isSaving) R.string.appearance_saving else R.string.appearance_applies_everywhere),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
