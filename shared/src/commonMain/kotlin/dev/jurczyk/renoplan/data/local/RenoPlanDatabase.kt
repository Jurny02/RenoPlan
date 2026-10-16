package dev.jurczyk.renoplan.data.local

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val DATABASE_NAME = "renoplan.db"

@Database(
    entities = [
        LevelEntity::class,
        RoomEntity::class,
        WallEntity::class,
        OpeningEntity::class,
        OperationEntity::class,
        TemplateEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // Adds the operations and templates tables.
        AutoMigration(from = 1, to = 2),
    ],
)
@ConstructedBy(RenoPlanDatabaseConstructor::class)
abstract class RenoPlanDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao

    abstract fun roomDao(): RoomDao

    abstract fun templateDao(): TemplateDao
}

// The Room compiler generates the actual implementations.
@Suppress("KotlinNoActualForExpect")
expect object RenoPlanDatabaseConstructor : RoomDatabaseConstructor<RenoPlanDatabase> {
    override fun initialize(): RenoPlanDatabase
}

/** Finishes a platform builder: bundled SQLite driver and queries off the main thread. */
fun buildDatabase(builder: RoomDatabase.Builder<RenoPlanDatabase>): RenoPlanDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
