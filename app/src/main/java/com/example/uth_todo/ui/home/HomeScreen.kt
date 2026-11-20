package com.example.uth_todo.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.ui.components.BrandHeader
import com.example.uth_todo.ui.components.PrimaryButton
import com.example.uth_todo.ui.components.WelcomeArtwork

@Composable
fun HomeScreen(session: AuthSession, onProfile: () -> Unit, displayName: String? = null, announcement: String? = null) {
    val user = (session as? AuthSession.SignedIn)?.user
    val name = displayName?.takeIf { it.isNotBlank() } ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore('@') ?: stringResource(R.string.guest_name)
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BrandHeader(Modifier.fillMaxWidth())
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.home_greeting, name), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("home_title"))
            if (!announcement.isNullOrBlank()) {
                Card(Modifier.fillMaxWidth().testTag("home_announcement")) {
                    Text(announcement, modifier = Modifier.padding(20.dp))
                }
            }
            WelcomeArtwork()
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.home_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth()) {
                Text(stringResource(if (session == AuthSession.Guest) R.string.guest_description else R.string.home_account_hint),
                    modifier = Modifier.padding(20.dp))
            }
            PrimaryButton(stringResource(R.string.profile_title), onProfile, tag = "open_profile")
            Spacer(Modifier.height(8.dp))
        }
    }
}
