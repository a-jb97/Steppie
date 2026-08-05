package com.example.steppie.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.steppie.data.local.SteppieDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SteppieDatabaseMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SteppieDatabase::class.java,
    )

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        (1..3).forEach { context.deleteDatabase(databaseName(it)) }
    }

    @After
    fun tearDown() {
        (1..3).forEach { context.deleteDatabase(databaseName(it)) }
    }

    @Test
    fun migrationFrom1To4_preservesExistingDataAndCreatesCurrentSchema() {
        val databaseName = databaseName(1)
        migrationHelper.createDatabase(databaseName, 1).apply {
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
            close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            databaseName,
            4,
            true,
            SteppieDatabase.MIGRATION_1_2,
            SteppieDatabase.MIGRATION_2_3,
            SteppieDatabase.MIGRATION_3_4,
        )

        database.useQuery(
            "SELECT id, localizedName, startTime FROM routine_sets WHERE id = ?",
            arrayOf(ROUTINE_SET_ID),
        ) { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(ROUTINE_SET_ID, cursor.getString(0))
            assertEquals("v1-localized-name", cursor.getString(1))
            assertNull(cursor.getString(2))
        }
        database.useQuery(
            "SELECT id, routineSetId, localizedTitle FROM routines WHERE id = ?",
            arrayOf(ROUTINE_ID),
        ) { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(ROUTINE_ID, cursor.getString(0))
            assertEquals(ROUTINE_SET_ID, cursor.getString(1))
            assertEquals("v1-localized-title", cursor.getString(2))
        }

        val tables = database.useQuery(
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

    @Test
    fun migrationsFrom2And3To4_createCurrentSchema() {
        listOf(
            2 to arrayOf(SteppieDatabase.MIGRATION_2_3, SteppieDatabase.MIGRATION_3_4),
            3 to arrayOf(SteppieDatabase.MIGRATION_3_4),
        ).forEach { (startVersion, migrations) ->
            val databaseName = databaseName(startVersion)
            migrationHelper.createDatabase(databaseName, startVersion).close()

            migrationHelper.runMigrationsAndValidate(
                databaseName,
                4,
                true,
                *migrations,
            )
                .close()
        }
    }

    private fun databaseName(version: Int): String = "steppie-migration-v$version-test.db"

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
