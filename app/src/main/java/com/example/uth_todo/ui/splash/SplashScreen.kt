package com.example.uth_todo.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uth_todo.BuildConfig
import com.example.uth_todo.R
import com.example.uth_todo.ui.components.BrandLogo
import com.example.uth_todo.ui.theme.UthBlue

@Composable
fun SplashScreen() {
    Column(Modifier.fillMaxSize().background(UthBlue).safeDrawingPadding()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandLogo(size = 104.dp)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.splash_tagline), color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
