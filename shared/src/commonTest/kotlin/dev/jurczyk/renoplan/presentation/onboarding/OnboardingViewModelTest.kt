package dev.jurczyk.renoplan.presentation.onboarding

import dev.jurczyk.renoplan.domain.repository.AppSettings
import dev.jurczyk.renoplan.testing.FakeStore
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingViewModelTest : MainDispatcherTest() {
    private val settings =
        object : AppSettings {
            override var onboardingSeen = false
        }
    private val store = FakeStore()
    private val viewModel = OnboardingViewModel(settings, store.levelRepository)

    @Test
    fun skippingMarksOnboardingSeen() {
        viewModel.finish()

        assertTrue(settings.onboardingSeen)
        assertEquals(OnboardingResult.Finished, viewModel.result)
    }

    @Test
    fun creatingTheFirstLevelSavesItAndOpensIt() =
        runTest {
            viewModel.createFirstLevel("  Parter ")

            val level = store.levels.value.single()
            assertEquals("Parter", level.name)
            assertTrue(settings.onboardingSeen)
            assertEquals(OnboardingResult.LevelCreated(level.id), viewModel.result)
        }

    @Test
    fun blankLevelNameDoesNothing() {
        viewModel.createFirstLevel("  ")

        assertTrue(store.levels.value.isEmpty())
        assertFalse(settings.onboardingSeen)
        assertNull(viewModel.result)
    }
}
