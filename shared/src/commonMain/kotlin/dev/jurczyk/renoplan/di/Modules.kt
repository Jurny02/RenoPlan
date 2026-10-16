package dev.jurczyk.renoplan.di

import dev.jurczyk.renoplan.data.local.RenoPlanDatabase
import dev.jurczyk.renoplan.data.local.buildDatabase
import dev.jurczyk.renoplan.data.repository.LocalLevelRepository
import dev.jurczyk.renoplan.data.repository.LocalRoomRepository
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Repositories and the database. The platform database builder comes from [platformModule]. */
val dataModule =
    module {
        single { buildDatabase(get()) }
        single { get<RenoPlanDatabase>().levelDao() }
        single { get<RenoPlanDatabase>().roomDao() }
        singleOf(::LocalLevelRepository) bind LevelRepository::class
        singleOf(::LocalRoomRepository) bind RoomRepository::class
    }

/** Use cases and calculations. */
val domainModule = module { }

/** ViewModels. */
val presentationModule = module { }

val appModules: List<Module> = listOf(platformModule, dataModule, domainModule, presentationModule)
