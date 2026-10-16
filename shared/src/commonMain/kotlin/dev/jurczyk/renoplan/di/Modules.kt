package dev.jurczyk.renoplan.di

import dev.jurczyk.renoplan.data.local.RenoPlanDatabase
import dev.jurczyk.renoplan.data.local.buildDatabase
import dev.jurczyk.renoplan.data.repository.LocalLevelRepository
import dev.jurczyk.renoplan.data.repository.LocalRoomRepository
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.presentation.level.LevelViewModel
import dev.jurczyk.renoplan.presentation.levels.LevelsViewModel
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.uuid.Uuid

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

/** ViewModels. Screens pass ids as parameters: `koinViewModel { parametersOf(levelId) }`. */
val presentationModule =
    module {
        viewModelOf(::LevelsViewModel)
        viewModel { (levelId: Uuid) -> LevelViewModel(levelId, get(), get()) }
        viewModel { (levelId: Uuid, roomId: Uuid?) -> RoomEditorViewModel(levelId, roomId, get()) }
    }

val appModules: List<Module> = listOf(platformModule, dataModule, domainModule, presentationModule)
