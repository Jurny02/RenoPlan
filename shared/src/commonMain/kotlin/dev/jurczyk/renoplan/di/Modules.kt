package dev.jurczyk.renoplan.di

import com.russhwolf.settings.Settings
import dev.jurczyk.renoplan.data.local.RenoPlanDatabase
import dev.jurczyk.renoplan.data.local.buildDatabase
import dev.jurczyk.renoplan.data.repository.LocalLevelRepository
import dev.jurczyk.renoplan.data.repository.LocalRoomRepository
import dev.jurczyk.renoplan.data.repository.LocalTemplateRepository
import dev.jurczyk.renoplan.data.settings.LocalAppSettings
import dev.jurczyk.renoplan.domain.repository.AppSettings
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.domain.repository.TemplateRepository
import dev.jurczyk.renoplan.presentation.calculator.CalculatorViewModel
import dev.jurczyk.renoplan.presentation.level.LevelViewModel
import dev.jurczyk.renoplan.presentation.levels.LevelsViewModel
import dev.jurczyk.renoplan.presentation.onboarding.OnboardingViewModel
import dev.jurczyk.renoplan.presentation.operation.OperationEditorViewModel
import dev.jurczyk.renoplan.presentation.roomdetails.RoomDetailsViewModel
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorViewModel
import dev.jurczyk.renoplan.presentation.templates.TemplateEditorViewModel
import dev.jurczyk.renoplan.presentation.templates.TemplatesViewModel
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
        single { get<RenoPlanDatabase>().templateDao() }
        singleOf(::LocalLevelRepository) bind LevelRepository::class
        singleOf(::LocalRoomRepository) bind RoomRepository::class
        singleOf(::LocalTemplateRepository) bind TemplateRepository::class
        single<Settings> { Settings() }
        singleOf(::LocalAppSettings) bind AppSettings::class
    }

/** Use cases and calculations. */
val domainModule = module { }

/** ViewModels. Screens pass ids as parameters: `koinViewModel { parametersOf(levelId) }`. */
val presentationModule =
    module {
        viewModelOf(::LevelsViewModel)
        viewModelOf(::CalculatorViewModel)
        viewModel { (levelId: Uuid) -> LevelViewModel(levelId, get(), get()) }
        viewModel { (levelId: Uuid, roomId: Uuid?) -> RoomEditorViewModel(levelId, roomId, get()) }
        viewModel { (levelId: Uuid, roomId: Uuid) -> RoomDetailsViewModel(levelId, roomId, get()) }
        viewModel { (levelId: Uuid, roomId: Uuid, operationId: Uuid?) ->
            OperationEditorViewModel(levelId, roomId, operationId, get(), get())
        }
        viewModelOf(::TemplatesViewModel)
        viewModelOf(::OnboardingViewModel)
        viewModel { (templateId: Uuid?) -> TemplateEditorViewModel(templateId, get()) }
    }

val appModules: List<Module> = listOf(platformModule, dataModule, domainModule, presentationModule)
