import dev.detekt.gradle.Detekt

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

// Formatting: `./gradlew spotlessCheck` to verify, `./gradlew spotlessApply` to fix.
// ktlint rules are configured in .editorconfig.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).customRuleSets(
            listOf(
                libs.composeRules.ktlint
                    .get()
                    .toString(),
            ),
        )
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint(ktlintVersion)
    }
    format("misc") {
        target("*.md", ".gitignore", "**/*.toml", "**/*.properties")
        targetExclude("**/build/**", "local.properties")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Static analysis: `./gradlew detekt`. Rules live in config/detekt/detekt.yml.
detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(fileTree(rootDir) { include("*/src/**/*.kt") })
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required = true
        sarif.required = true
    }
}
