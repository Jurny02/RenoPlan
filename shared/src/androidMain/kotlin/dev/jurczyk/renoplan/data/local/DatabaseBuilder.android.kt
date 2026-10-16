package dev.jurczyk.renoplan.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<RenoPlanDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<RenoPlanDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
}
