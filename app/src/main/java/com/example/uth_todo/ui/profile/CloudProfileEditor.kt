package com.example.uth_todo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.FirebaseServiceError
import com.example.uth_todo.ui.components.PrimaryButton

@Composable
fun CloudProfileEditor(
    state: FirebaseServicesState,
    initialName: String,
    onSave: (String) -> Unit,
    onRetry: () -> Unit,
    onEdited: () -> Unit,
) {
    var draft by rememberSaveable(state.uid) { mutableStateOf(state.displayName ?: initialName) }
    var edited by rememberSaveable(state.uid) { mutableStateOf(false) }
    LaunchedEffect(state.displayName, state.saved) {
        if (!edited || state.saved) {
            draft = state.displayName ?: initialName
            edited = false
        }
    }
    val error = state.saveError ?: state.loadError
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.profile_sync_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.profile_sync_description))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it; edited = true; onEdited() },
                label = { Text(stringResource(R.string.profile_name)) },
                supportingText = { Text(stringResource(R.string.profile_name_limit)) },
                singleLine = true,
                enabled = !state.isLoading && !state.isSaving && state.loadError == null,
                isError = state.saveError == FirebaseServiceError.INVALID_NAME,
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
            )
            if (state.isLoading) Text(stringResource(R.string.profile_sync_loading))
            if (state.saved) {
                Text(stringResource(R.string.profile_sync_saved), modifier = Modifier.testTag("profile_saved"))
            }
            if (error != null) {
                Text(stringResource(error.messageResource()), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("profile_sync_error"))
            }
            PrimaryButton(
                stringResource(if (state.isSaving) R.string.saving else R.string.profile_save_name),
                onClick = { onSave(draft) },
                enabled = !state.isLoading && !state.isSaving && state.loadError == null,
                tag = "profile_save_name",
            )
            if (state.loadError != null) {
                TextButton(onClick = onRetry, enabled = !state.isSaving) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

private fun FirebaseServiceError.messageResource() = when (this) {
    FirebaseServiceError.NOT_CONFIGURED -> R.string.profile_sync_unavailable
    FirebaseServiceError.PERMISSION_DENIED -> R.string.profile_sync_denied
    FirebaseServiceError.NETWORK -> R.string.auth_error_network
    FirebaseServiceError.TIMEOUT -> R.string.profile_sync_timeout
    FirebaseServiceError.INVALID_NAME -> R.string.profile_name_invalid
    FirebaseServiceError.UNKNOWN -> R.string.profile_sync_failed
}
