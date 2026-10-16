package dev.jurczyk.renoplan.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Starts Koin with all app modules. Platform-specific setup (e.g. Android context) goes in [appDeclaration]. */
fun initKoin(appDeclaration: KoinAppDeclaration): KoinApplication =
    startKoin {
        appDeclaration()
        modules(appModules)
    }

/** Entry point for iOS, called from Swift as `KoinKt.doInitKoin()`. */
fun initKoin(): KoinApplication = initKoin {}
