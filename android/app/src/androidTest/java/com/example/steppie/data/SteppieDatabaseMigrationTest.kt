package com.example.steppie.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.data.local.SteppieDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SteppieDatabaseMigrationTest {
    private lateinit var context: Context
    private val databaseName = "steppie-migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom1To4_preservesExistingDataAndCreatesCurrentSchema() {
        createVersion1Database()

        val database = Room.databaseBuilder(context, SteppieDatabase::class.java, databaseName)
            .addMigrations(
                SteppieDatabase.MIGRATION_1_2,
                SteppieDatabase.MIGRATION_2_3,
                SteppieDatabase.MIGRATION_3_4,
            )
            .allowMainThreadQueries()
            .build()

        database.openHelper.writableDatabase.useQuery(
            "SELECT id, localizedName, startTime FROM routine_sets WHERE id = ?",
            arrayOf(ROUTINE_SET_ID),
        ) { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(ROUTINE_SET_ID, cursor.getString(0))
            assertEquals("v1-localized-name", cursor.getString(1))
            assertNull(cursor.getString(2))
        }
        database.openHelper.writableDatabase.useQuery(
            "SELECT id, routineSetId, localizedTitle FROM routines WHERE id = ?",
            arrayOf(ROUTINE_ID),
        ) { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(ROUTINE_ID, cursor.getString(0))
            assertEquals(ROUTINE_SET_ID, cursor.getString(1))
            assertEquals("v1-localized-title", cursor.getString(2))
        }

        val tables = database.openHelper.writableDatabase.useQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
        ) { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
        assertTrue("daily_logs" in tables)
        assertTrue("daily_routine_selections" in tables)

        database.close()
    }

    private fun createVersion1Database() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase.apply {
            execSQL(
                """
                INSERT INTO routine_sets (
                    id, localizedName, isActive, createdAtEpochMillis,
                    updatedAtEpochMillis, deletedAtEpochMillis
                ) VALUES (?, ?, 1, 1, 1, NULL)
                """.trimIndent(),
                arrayOf(ROUTINE_SET_ID, "v1-localized-name"),
            )
            execSQL(
                """
                INSERT INTO routines (
                    id, routineSetId, titleKey, localizedTitle, iconType, iconName,
                    localAssetId, backupAssetName, colorToken, sortOrder, scheduledTime,
                    isActive, createdAtEpochMillis, updatedAtEpochMillis, deletedAtEpochMillis
                ) VALUES (?, ?, NULL, ?, 'builtin', 'star', NULL, NULL, 'color.card.sky',
                    0, '08:00', 1, 1, 1, NULL)
                """.trimIndent(),
                arrayOf(ROUTINE_ID, ROUTINE_SET_ID, "v1-localized-title"),
            )
        }
        helper.close()
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routine_sets (
                id TEXT NOT NULL,
                localizedName TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                deletedAtEpochMillis INTEGER,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_routine_sets_isActive_deletedAtEpochMillis " +
                "ON routine_sets (isActive, deletedAtEpochMillis)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routines (
                id TEXT NOT NULL,
                routineSetId TEXT NOT NULL,
                titleKey TEXT,
                localizedTitle TEXT NOT NULL,
                iconType TEXT NOT NULL,
                iconName TEXT,
                localAssetId TEXT,
                backupAssetName TEXT,
                colorToken TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                scheduledTime TEXT,
                isActive INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                deletedAtEpochMillis INTEGER,
                PRIMARY KEY(id),
                FOREIGN KEY(routineSetId) REFERENCES routine_sets(id)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_routines_routineSetId ON routines (routineSetId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_routines_routineSetId_sortOrder_deletedAtEpochMillis " +
                "ON routines (routineSetId, sortOrder, deletedAtEpochMillis)",
        )
    }

    private inline fun <T> SupportSQLiteDatabase.useQuery(
        sql: String,
        bindArgs: Array<out Any?> = emptyArray(),
        block: (android.database.Cursor) -> T,
    ): T = query(sql, bindArgs).use(block)

    private companion object {
        const val ROUTINE_SET_ID = "30000000-0000-4000-8000-000000000001"
        const val ROUTINE_ID = "40000000-0000-4000-8000-000000000001"
    }
}
