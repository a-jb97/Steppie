package com.example.steppie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RoutineSetEntity::class, RoutineEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SteppieDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var instance: SteppieDatabase? = null

        fun getInstance(context: Context): SteppieDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SteppieDatabase::class.java,
                "steppie.db",
            ).build().also { instance = it }
        }
    }
}
