package dev.jurczyk.renoplan.di

import dev.jurczyk.renoplan.data.local.databaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single { databaseBuilder(androidContext()) }
    }
