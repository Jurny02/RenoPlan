package dev.jurczyk.renoplan.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.presentation.onboarding.OnboardingResult
import dev.jurczyk.renoplan.presentation.onboarding.OnboardingViewModel
import dev.jurczyk.renoplan.ui.components.NameDialog
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.ic_app_glyph
import renoplan.shared.generated.resources.ic_ruler
import renoplan.shared.generated.resources.ic_status_done
import renoplan.shared.generated.resources.ic_templates
import renoplan.shared.generated.resources.level_name_label
import renoplan.shared.generated.resources.onboarding_create_level
import renoplan.shared.generated.resources.onboarding_measure_text
import renoplan.shared.generated.resources.onboarding_measure_title
import renoplan.shared.generated.resources.onboarding_next
import renoplan.shared.generated.resources.onboarding_page
import renoplan.shared.generated.resources.onboarding_plan_text
import renoplan.shared.generated.resources.onboarding_plan_title
import renoplan.shared.generated.resources.onboarding_skip
import renoplan.shared.generated.resources.onboarding_track_text
import renoplan.shared.generated.resources.onboarding_track_title
import renoplan.shared.generated.resources.onboarding_work_text
import renoplan.shared.generated.resources.onboarding_work_title
import kotlin.uuid.Uuid

/** Onboarding (docs/06): four pages, skippable at any time; the last one can create the first level. */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onOpenLevel: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val result = viewModel.result
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnOpenLevel by rememberUpdatedState(onOpenLevel)
    LaunchedEffect(result) {
        when (result) {
            OnboardingResult.Finished -> currentOnDone()
            is OnboardingResult.LevelCreated -> currentOnOpenLevel(result.levelId)
            null -> Unit
        }
    }
    OnboardingContent(onSkip = viewModel::finish, onCreateLevel = viewModel::createFirstLevel, modifier = modifier)
}

private enum class OnboardingPage(
    val icon: DrawableResource,
    val title: StringResource,
    val text: StringResource,
) {
    PLAN(Res.drawable.ic_app_glyph, Res.string.onboarding_plan_title, Res.string.onboarding_plan_text),
    MEASURE(Res.drawable.ic_ruler, Res.string.onboarding_measure_title, Res.string.onboarding_measure_text),
    WORK(Res.drawable.ic_templates, Res.string.onboarding_work_title, Res.string.onboarding_work_text),
    TRACK(Res.drawable.ic_status_done, Res.string.onboarding_track_title, Res.string.onboarding_track_text),
}

@Composable
fun OnboardingContent(
    onSkip: () -> Unit,
    onCreateLevel: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pages = OnboardingPage.entries
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    var askName by remember { mutableStateOf(false) }
    Scaffold(modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSkip) { Text(stringResource(Res.string.onboarding_skip)) }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page -> PageContent(pages[page]) }
            PageIndicator(count = pages.size, current = pagerState.currentPage)
            val isLast = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = {
                    if (isLast) {
                        askName = true
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(if (isLast) Res.string.onboarding_create_level else Res.string.onboarding_next))
            }
        }
    }
    if (askName) {
        NameDialog(
            title = stringResource(Res.string.onboarding_create_level),
            label = stringResource(Res.string.level_name_label),
            onConfirm = {
                askName = false
                onCreateLevel(it)
            },
            onDismiss = { askName = false },
        )
    }
}

@Composable
private fun PageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    // Scrolls so long texts at large font sizes are never cut off.
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(page.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp),
        )
        Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(page.text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.onboarding_page, current + 1, count)
    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val color =
                if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        }
    }
}

@Preview
@Composable
private fun OnboardingContentPreview() {
    RenoPlanTheme {
        OnboardingContent(onSkip = {}, onCreateLevel = {})
    }
}

@Preview
@Composable
private fun OnboardingContentLastPagePreview() {
    RenoPlanTheme {
        OnboardingContent(onSkip = {}, onCreateLevel = {}, initialPage = OnboardingPage.entries.lastIndex)
    }
}
