package com.example.steppie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoutineSetEntity::class,
        RoutineEntity::class,
        DailyLogEntity::class,
        DailyRoutineSelectionEntity::class,
    ],
    version = 3,
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
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_logs` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `routineId` TEXT NOT NULL,
                        `routineSetId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `completedAtEpochMillis` INTEGER,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`routineSetId`) REFERENCES `routine_sets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_date` ON `daily_logs` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_routineId` ON `daily_logs` (`routineId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_routineSetId` ON `daily_logs` (`routineSetId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_logs_date_routineId` ON `daily_logs` (`date`, `routineId`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_routine_selections` (
                        `date` TEXT NOT NULL,
                        `routineSetId` TEXT NOT NULL,
                        `selectedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`date`),
                        FOREIGN KEY(`routineSetId`) REFERENCES `routine_sets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_routine_selections_routineSetId` ON `daily_routine_selections` (`routineSetId`)",
                )
            }
        }
    }
}
