package com.example.steppie.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.repository.RoomRoutineRepository
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRoutineRepositoryTest {
    private lateinit var database: SteppieDatabase
    private lateinit var repository: RoomRoutineRepository

    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val routineSetId = "30000000-0000-4000-8000-000000000001"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SteppieDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomRoutineRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createAndRead_assignsConsecutiveOrderAndPreservesContractValues() = runBlocking {
        repository.createRoutineSet(testSet())

        val first = repository.createRoutine(testRoutine(1, requestedOrder = 12, scheduledTime = LocalTime.of(8, 5)))
        val second = repository.createRoutine(testRoutine(2, requestedOrder = 0))

        assertEquals(0, first.order)
        assertEquals(1, second.order)
        assertEquals("color.card.mint", first.colorToken)
        assertEquals(LocalTime.of(8, 5), repository.getRoutine(first.id)?.scheduledTime)
        assertEquals(listOf(first.id, second.id), repository.getRoutineSet(routineSetId)?.routines?.map { it.id })
    }

    @Test
    fun update_keepsStableIdAndChangesEditableFields() = runBlocking {
        repository.createRoutineSet(testSet())
        val original = repository.createRoutine(testRoutine(1))
        val updatedAt = Instant.parse("2026-01-01T01:00:00Z")

        val updated = repository.updateRoutine(
            original.copy(
                title = LocalizedText(mapOf("ko" to "새 제목")),
                icon = IconRef.Builtin("book"),
                colorToken = "color.card.rose",
                scheduledTime = LocalTime.of(9, 30),
                updatedAt = updatedAt,
            ),
        )

        assertEquals(original.id, updated.id)
        assertEquals("새 제목", updated.title.values["ko"])
        assertEquals(IconRef.Builtin("book"), updated.icon)
        assertEquals("color.card.rose", updated.colorToken)
        assertEquals(updatedAt, updated.updatedAt)
    }

    @Test
    fun delete_softDeletesAndNormalizesRemainingOrders() = runBlocking {
        repository.createRoutineSet(testSet())
        val first = repository.createRoutine(testRoutine(1))
        val second = repository.createRoutine(testRoutine(2))
        val third = repository.createRoutine(testRoutine(3))

        repository.deleteRoutine(second.id, Instant.parse("2026-01-01T02:00:00Z"))

        assertNull(repository.getRoutine(second.id))
        val remaining = repository.getRoutineSet(routineSetId)?.routines.orEmpty()
        assertEquals(listOf(first.id, third.id), remaining.map { it.id })
        assertEquals(listOf(0, 1), remaining.map { it.order })
    }

    @Test
    fun reorder_persistsRequestedConsecutiveOrder() = runBlocking {
        repository.createRoutineSet(testSet())
        val first = repository.createRoutine(testRoutine(1))
        val second = repository.createRoutine(testRoutine(2))
        val third = repository.createRoutine(testRoutine(3))

        repository.reorderRoutines(
            routineSetId = routineSetId,
            orderedRoutineIds = listOf(third.id, first.id, second.id),
            updatedAt = Instant.parse("2026-01-01T03:00:00Z"),
        )

        val routines = repository.getRoutineSet(routineSetId)?.routines.orEmpty()
        assertEquals(listOf(third.id, first.id, second.id), routines.map { it.id })
        assertEquals(listOf(0, 1, 2), routines.map { it.order })
    }

    @Test
    fun routineSetCrud_enforcesSingleActiveSetAndSoftDeleteRule() = runBlocking {
        val first = repository.createRoutineSet(testSet(active = true))
        val secondId = "30000000-0000-4000-8000-000000000002"
        val second = repository.createRoutineSet(testSet(id = secondId, active = true))

        assertEquals(false, repository.getRoutineSet(first.id)?.isActive)
        assertEquals(true, repository.getRoutineSet(second.id)?.isActive)

        repository.deleteRoutineSet(first.id, Instant.parse("2026-01-01T04:00:00Z"))
        assertNull(repository.getRoutineSet(first.id))
    }

    private fun testSet(id: String = routineSetId, active: Boolean = true): RoutineSet = RoutineSet(
        id = id,
        name = LocalizedText(mapOf("ko" to "테스트 루틴", "en" to "Test routine")),
        isActive = active,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun testRoutine(
        suffix: Int,
        requestedOrder: Int = 0,
        scheduledTime: LocalTime? = null,
    ): Routine = Routine(
        id = "40000000-0000-4000-8000-${suffix.toString().padStart(12, '0')}",
        routineSetId = routineSetId,
        title = LocalizedText(mapOf("ko" to "루틴 $suffix", "en" to "Routine $suffix")),
        icon = IconRef.Builtin("star"),
        colorToken = "color.card.mint",
        order = requestedOrder,
        scheduledTime = scheduledTime,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
