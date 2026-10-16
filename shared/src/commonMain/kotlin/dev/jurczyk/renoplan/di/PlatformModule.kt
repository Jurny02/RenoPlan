package dev.jurczyk.renoplan.di

import org.koin.core.module.Module

/** Platform dependencies: the database builder (Android needs the application context). */
expect val platformModule: Module
