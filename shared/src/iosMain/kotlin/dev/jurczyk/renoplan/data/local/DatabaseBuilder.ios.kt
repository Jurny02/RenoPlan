package dev.jurczyk.renoplan.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun databaseBuilder(): RoomDatabase.Builder<RenoPlanDatabase> =
    Room.databaseBuilder<RenoPlanDatabase>(name = "${documentDirectory()}/$DATABASE_NAME")

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val directory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(directory?.path) { "Documents directory not found" }
}
