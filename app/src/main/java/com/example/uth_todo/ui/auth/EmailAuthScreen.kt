package com.example.uth_todo.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.ui.components.BrandHeader
import com.example.uth_todo.ui.components.PrimaryButton

@Composable
fun EmailAuthScreen(
    isBusy: Boolean,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onResetPassword: (String) -> Unit,
) {
    var register by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords intentionally never go into saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    BackHandler(enabled = isBusy) { }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack, enabled = !isBusy) { Text(stringResource(R.string.auth_back)) }
            BrandHeader(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(stringResource(if (register) R.string.auth_register else R.string.auth_sign_in),
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("email_auth_title"))
            Text(stringResource(if (register) R.string.auth_register_hint else R.string.auth_login_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(email, { email = it }, enabled = !isBusy, singleLine = true,
                label = { Text(stringResource(R.string.auth_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("auth_email"))
            OutlinedTextField(password, { password = it }, enabled = !isBusy, singleLine = true,
                label = { Text(stringResource(R.string.auth_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("auth_password"))
            if (register) {
                OutlinedTextField(confirmation, { confirmation = it }, enabled = !isBusy, singleLine = true,
                    label = { Text(stringResource(R.string.auth_confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("auth_confirmation"))
            }
            PrimaryButton(
                text = stringResource(if (isBusy) R.string.auth_processing else if (register) R.string.auth_register else R.string.auth_sign_in),
                enabled = !isBusy,
                onClick = { if (register) onRegister(email, password, confirmation) else onSignIn(email, password) },
            )
            if (!register) {
                TextButton(onClick = { onResetPassword(email) }, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_forgot_password))
                }
            }
            TextButton(
                onClick = { register = !register; password = ""; confirmation = "" },
                enabled = !isBusy, modifier = Modifier.fillMaxWidth().testTag("auth_switch_mode"),
            ) { Text(stringResource(if (register) R.string.auth_switch_login else R.string.auth_switch_register)) }
        }
    }
}
