package dev.jurczyk.renoplan.domain.repository

/** Small app-wide flags kept on the device. */
interface AppSettings {
    /** Whether onboarding has been shown (finished or skipped), so it does not show again. */
    var onboardingSeen: Boolean
}
