package com.example.uth_todo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_todo.R
import com.example.uth_todo.model.OnboardingIllustration
import com.example.uth_todo.ui.theme.UthBlue
import com.example.uth_todo.ui.theme.UthGreen
import com.example.uth_todo.ui.theme.UthLavender

private enum class ArtworkIcon { CHECK, BOOK, HOME, BELL }

@Composable
fun OnboardingArtwork(illustration: OnboardingIllustration) {
    ArtworkFrame {
        when (illustration) {
            OnboardingIllustration.TASKS -> TaskCards()
            OnboardingIllustration.CATEGORIES -> CategoryCards()
            OnboardingIllustration.REMINDERS -> ReminderCard()
        }
    }
}

@Composable
fun WelcomeArtwork() {
    ArtworkFrame {
        TaskCards()
        Box(Modifier.align(Alignment.BottomEnd).offset((-20).dp, (-12).dp)
            .size(66.dp).background(UthGreen, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
            ArtworkSymbol(ArtworkIcon.CHECK, Color.White, Modifier.size(38.dp))
        }
    }
}

@Composable
private fun ArtworkFrame(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    // Fixed design coordinates are scaled inside a responsive layout slot.
    // The illustration is decorative: the accessible title and body describe it.
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        val scale = (maxWidth / 320.dp).coerceAtMost(1f)
        Box(Modifier.fillMaxWidth().height(270.dp * scale), contentAlignment = Alignment.Center) {
            Box(Modifier.requiredSize(320.dp, 270.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                Box(Modifier.align(Alignment.Center).size(254.dp).background(UthLavender, CircleShape))
                Box(Modifier.offset(28.dp, 38.dp).size(10.dp).background(UthBlue.copy(alpha = 0.25f), CircleShape))
                Box(Modifier.align(Alignment.BottomEnd).offset((-34).dp, (-24).dp)
                    .size(14.dp).background(UthGreen.copy(alpha = 0.35f), CircleShape))
                content()
            }
        }
    }
}

@Composable
private fun TaskCards() {
    val titles = listOf(R.string.sample_homework, R.string.sample_housework, R.string.sample_appointment)
    titles.forEachIndexed { index, title ->
        Surface(
            modifier = Modifier.offset((32 + index * 10).dp, (38 + index * 64).dp)
                .requiredSize(242.dp, 66.dp).graphicsLayer { rotationZ = if (index == 1) 3f else -5f },
            shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp, color = Color.White,
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(32.dp).background(if (index == 0) UthBlue else UthLavender, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    ArtworkSymbol(ArtworkIcon.CHECK, if (index == 0) Color.White else UthBlue, Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(0.65f).height(5.dp).background(UthLavender, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun CategoryCards() {
    CategoryCard(R.string.category_study, ArtworkIcon.BOOK, UthBlue,
        Modifier.offset(20.dp, 52.dp).graphicsLayer { rotationZ = -9f })
    CategoryCard(R.string.category_life, ArtworkIcon.HOME, UthGreen,
        Modifier.offset(164.dp, 100.dp).graphicsLayer { rotationZ = 8f })
}

@Composable
private fun CategoryCard(title: Int, icon: ArtworkIcon, color: Color, modifier: Modifier) {
    Surface(modifier.requiredSize(136.dp, 148.dp), shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp, color = Color.White) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(48.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center) {
                ArtworkSymbol(icon, color, Modifier.size(28.dp))
            }
            Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Box(Modifier.fillMaxWidth(0.7f).height(5.dp).background(UthLavender, CircleShape))
        }
    }
}

@Composable
private fun ReminderCard() {
    Box(Modifier.offset(112.dp, 38.dp).size(96.dp).graphicsLayer { rotationZ = -12f }
        .background(UthBlue, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
        ArtworkSymbol(ArtworkIcon.BELL, Color.White, Modifier.size(58.dp))
    }
    Surface(Modifier.offset(22.dp, 163.dp).requiredSize(276.dp, 78.dp),
        shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp, color = Color.White) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(42.dp).background(UthLavender, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                ArtworkSymbol(ArtworkIcon.BELL, UthBlue, Modifier.size(26.dp))
            }
            Column {
                Text(stringResource(R.string.sample_reminder_title), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(stringResource(R.string.sample_reminder_time), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ArtworkSymbol(icon: ArtworkIcon, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 48f, size.height / 48f, pivot = Offset.Zero) }) {
            val stroke = Stroke(width = 3f, cap = StrokeCap.Round)
            when (icon) {
                ArtworkIcon.CHECK -> {
                    drawLine(color, Offset(11f, 25f), Offset(20f, 34f), 4f, StrokeCap.Round)
                    drawLine(color, Offset(20f, 34f), Offset(38f, 14f), 4f, StrokeCap.Round)
                }
                ArtworkIcon.BOOK -> {
                    val book = Path().apply {
                        moveTo(6f, 10f); quadraticTo(17f, 7f, 24f, 13f)
                        quadraticTo(31f, 7f, 42f, 10f); lineTo(42f, 37f)
                        quadraticTo(31f, 34f, 24f, 40f); quadraticTo(17f, 34f, 6f, 37f); close()
                    }
                    drawPath(book, color, style = stroke)
                    drawLine(color, Offset(24f, 13f), Offset(24f, 40f), 3f)
                }
                ArtworkIcon.HOME -> {
                    val home = Path().apply {
                        moveTo(5f, 23f); lineTo(24f, 7f); lineTo(43f, 23f)
                        moveTo(11f, 20f); lineTo(11f, 41f); lineTo(37f, 41f); lineTo(37f, 20f)
                        moveTo(20f, 41f); lineTo(20f, 29f); lineTo(28f, 29f); lineTo(28f, 41f)
                    }
                    drawPath(home, color, style = stroke)
                }
                ArtworkIcon.BELL -> {
                    val bell = Path().apply {
                        moveTo(10f, 34f); lineTo(14f, 28f); lineTo(14f, 20f)
                        cubicTo(14f, 5f, 34f, 5f, 34f, 20f)
                        lineTo(34f, 28f); lineTo(38f, 34f); close()
                    }
                    drawPath(bell, color, style = stroke)
                    drawArc(color, 0f, 180f, false, Offset(20f, 35f), Size(8f, 8f), style = stroke)
                    drawLine(color, Offset(24f, 5f), Offset(24f, 9f), 3f, StrokeCap.Round)
                }
            }
        }
    }
}
