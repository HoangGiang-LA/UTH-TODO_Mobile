package com.example.uth_todo.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.ui.components.BrandHeader
import com.example.uth_todo.ui.components.PrimaryButton
import com.example.uth_todo.ui.components.WelcomeArtwork

@Composable
fun WelcomeScreen(onGoogleClick: () -> Unit, onGuestClick: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxHeight < 540.dp || fontScale >= 1.5f
        val screenModifier = Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 24.dp)
        Column(if (compact) screenModifier.verticalScroll(rememberScrollState()) else screenModifier) {
            BrandHeader(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp))
            Column(
                modifier = (if (compact) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()))
                    .fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WelcomeArtwork()
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() }.testTag("welcome_title"))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            PrimaryButton(
                stringResource(R.string.continue_google), onGoogleClick,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
                leadingIcon = { Image(painterResource(R.drawable.ic_google), null, Modifier.size(24.dp)) },
            )
            TextButton(onClick = onGuestClick, modifier = Modifier.fillMaxWidth().testTag("guest_button")) {
                Text(stringResource(R.string.try_without_account), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
