package dev.jurczyk.renoplan.di

import org.koin.core.module.Module
import org.koin.dsl.module

/** Repositories and the database. */
val dataModule = module { }

/** Use cases and calculations. */
val domainModule = module { }

/** ViewModels. */
val presentationModule = module { }

val appModules: List<Module> = listOf(dataModule, domainModule, presentationModule)
