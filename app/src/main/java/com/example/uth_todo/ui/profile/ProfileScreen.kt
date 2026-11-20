package com.example.uth_todo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.ui.components.BrandLogo
import com.example.uth_todo.ui.components.PrimaryButton

@Composable
fun ProfileScreen(
    session: AuthSession,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    servicesState: FirebaseServicesState? = null,
    onSaveName: (String) -> Unit = {},
    onRetryProfile: () -> Unit = {},
    onNameEdited: () -> Unit = {},
    showNotifications: Boolean = false,
) {
    val user = (session as? AuthSession.SignedIn)?.user
    val missing = stringResource(R.string.profile_not_provided)
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            TextButton(onClick = onBack, enabled = !isBusy) { Text(stringResource(R.string.auth_back)) }
            BrandLogo(size = 72.dp)
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("profile_title"))
            if (session == AuthSession.Guest) {
                Text(stringResource(R.string.guest_name), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.guest_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (user != null) {
                Card(Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            ProfileField(stringResource(R.string.profile_name),
                                servicesState?.displayName?.takeIf { it.isNotBlank() }
                                    ?: user.displayName?.takeIf { it.isNotBlank() } ?: missing)
                            ProfileField(stringResource(R.string.profile_email), user.email ?: missing)
                            ProfileField(stringResource(R.string.profile_uid), user.uid)
                            ProfileField(stringResource(R.string.profile_verification), stringResource(
                                if (user.isEmailVerified) R.string.profile_verified else R.string.profile_unverified))
                        }
                    }
                }
                if (servicesState != null) {
                    CloudProfileEditor(servicesState, user.displayName.orEmpty(), onSaveName, onRetryProfile, onNameEdited)
                }
            }
            if (showNotifications) NotificationPreferenceCard()
            PrimaryButton(
                stringResource(if (isBusy) R.string.auth_processing else if (session == AuthSession.Guest) R.string.auth_sign_in else R.string.profile_sign_out),
                onSignOut, enabled = !isBusy, tag = "sign_out_button",
            )
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
