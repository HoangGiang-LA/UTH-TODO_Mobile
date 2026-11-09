package com.example.uth_todo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.ui.theme.UthBlue

@Composable
fun BrandLogo(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Image(
        painter = painterResource(R.drawable.ic_uth_logo),
        contentDescription = null,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(UthBlue).padding(size * 0.08f),
    )
}

@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BrandLogo(size = 36.dp)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
    }
}
