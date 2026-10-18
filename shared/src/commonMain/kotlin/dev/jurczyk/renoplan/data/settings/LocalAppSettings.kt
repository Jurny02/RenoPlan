package dev.jurczyk.renoplan.data.settings

import com.russhwolf.settings.Settings
import dev.jurczyk.renoplan.domain.repository.AppSettings

private const val KEY_ONBOARDING_SEEN = "onboarding_seen"

/** [AppSettings] in the platform key-value store (SharedPreferences on Android, NSUserDefaults on iOS). */
class LocalAppSettings(
    private val settings: Settings,
) : AppSettings {
    override var onboardingSeen: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_SEEN, defaultValue = false)
        set(value) = settings.putBoolean(KEY_ONBOARDING_SEEN, value)
}
