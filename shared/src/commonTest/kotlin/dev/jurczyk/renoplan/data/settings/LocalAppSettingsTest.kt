package dev.jurczyk.renoplan.data.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAppSettingsTest {
    @Test
    fun onboardingIsNotSeenUntilMarked() {
        val store = MapSettings()
        val settings = LocalAppSettings(store)
        assertFalse(settings.onboardingSeen)

        settings.onboardingSeen = true

        assertTrue(LocalAppSettings(store).onboardingSeen)
    }
}
