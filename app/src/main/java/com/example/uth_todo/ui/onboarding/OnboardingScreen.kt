package com.example.uth_todo.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.OnboardingPage
import com.example.uth_todo.ui.components.BrandHeader
import com.example.uth_todo.ui.components.OnboardingArtwork
import com.example.uth_todo.ui.components.PrimaryButton
import com.example.uth_todo.ui.theme.UthBlue
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(isSaving: Boolean, onComplete: () -> Unit) {
    val pages = OnboardingPage.pages
    // rememberPagerState has a Saver: page and offset survive Activity recreation.
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val busy = isSaving || pagerState.isScrollInProgress
    BackHandler(enabled = pagerState.currentPage > 0 || isSaving) {
        if (!busy && pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxHeight < 540.dp || fontScale >= 1.5f
        val screenModifier = Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 24.dp)
        Column(if (compact) screenModifier.verticalScroll(rememberScrollState()) else screenModifier) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandHeader(Modifier.weight(1f))
                TextButton(onClick = onComplete, enabled = !busy, modifier = Modifier.testTag("skip_button")) {
                    Text(stringResource(R.string.skip))
                }
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSaving,
                modifier = (if (compact) Modifier.height(440.dp * fontScale.coerceAtMost(2f)) else Modifier.weight(1f))
                    .fillMaxWidth().testTag("onboarding_pager"),
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OnboardingArtwork(page.illustration)
                    Spacer(Modifier.height(32.dp))
                    Text(
                        stringResource(page.title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("page_title_$index"),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(page.description), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                }
            }
            PageIndicator(pagerState.currentPage, pages.size)
            PrimaryButton(
                text = stringResource(
                    if (isSaving) R.string.saving
                    else if (pagerState.currentPage == pages.lastIndex) R.string.get_started
                    else R.string.continue_action,
                ),
                enabled = !busy,
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) onComplete()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    val description = stringResource(R.string.page_indicator, currentPage + 1, pageCount)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 28.dp)
            .semantics { contentDescription = description }.testTag("page_indicator"),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            val width by animateDpAsState(if (currentPage == index) 26.dp else 8.dp, label = "page_dot")
            Box(Modifier.size(width, 8.dp).background(
                if (currentPage == index) UthBlue else UthBlue.copy(alpha = 0.16f), CircleShape))
        }
    }
}
