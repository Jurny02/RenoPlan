package dev.jurczyk.renoplan.presentation.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.repository.AppSettings
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** Where onboarding ends: back to the app, or into the level the user just created. */
sealed interface OnboardingResult {
    data object Finished : OnboardingResult

    data class LevelCreated(
        val levelId: Uuid,
    ) : OnboardingResult
}

/** Onboarding (shown once on the first run, reopened from the help icon). Any way out marks it seen. */
class OnboardingViewModel(
    private val appSettings: AppSettings,
    private val levelRepository: LevelRepository,
) : ViewModel() {
    /** Set when onboarding ends; the screen then navigates away. */
    var result: OnboardingResult? by mutableStateOf(null)
        private set

    /** "Skip" on any page, or "Done" on the last. */
    fun finish() {
        appSettings.onboardingSeen = true
        result = OnboardingResult.Finished
    }

    /** "Create your first level" on the last page. */
    fun createFirstLevel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val level = Level(id = Uuid.random(), name = trimmed)
        viewModelScope.launch {
            levelRepository.saveLevel(level)
            appSettings.onboardingSeen = true
            result = OnboardingResult.LevelCreated(level.id)
        }
    }
}
