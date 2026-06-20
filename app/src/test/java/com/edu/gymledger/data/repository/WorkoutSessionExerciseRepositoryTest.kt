package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
import com.edu.gymledger.data.db.entity.ExerciseEntity
import com.edu.gymledger.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WorkoutSessionExerciseRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var repository: WorkoutSessionExerciseRepository
    private var sessionId: Long = 0L
    private var exerciseId: Long = 0L

    @Before
    fun setup() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        repository = WorkoutSessionExerciseRepository(
            database.workoutSessionExerciseDao(),
            database.workoutSessionDao(),
            database.exerciseDao()
        )

        val now = Instant.now().toString()
        val exercise = ExerciseEntity(
            name = "Bench Press",
            category = "Upper Body",
            primaryMuscle = "Chest",
            secondaryMuscles = "Triceps",
            equipment = "Barbell",
            notes = null,
            createdAt = now,
            updatedAt = now
        )
        exerciseId = database.exerciseDao().insert(exercise)

        val session = WorkoutSessionEntity(
            title = "Test Session",
            startedAt = now,
            endedAt = null,
            notes = null,
            createdAt = now,
            updatedAt = now
        )
        sessionId = database.workoutSessionDao().insert(session)
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun create_withValidData_succeeds() = runTest {
        val result = repository.create(sessionId, exerciseId, 1, null)
        assertTrue(result.id > 0)
        assertEquals(sessionId, result.sessionId)
        assertEquals(exerciseId, result.exerciseId)
        assertEquals(1, result.orderNum)
        assertNull(result.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_missingSessionId_throws() = runTest {
        repository.create(999L, exerciseId, 1, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_missingExerciseId_throws() = runTest {
        repository.create(sessionId, 999L, 1, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_orderNumLessThanOne_throws() = runTest {
        repository.create(sessionId, exerciseId, 0, null)
    }

    @Test
    fun create_blankNotes_convertsToNull() = runTest {
        val result = repository.create(sessionId, exerciseId, 1, "   ")
        assertNull(result.notes)
    }

    @Test
    fun getById_existing_returnsItem() = runTest {
        val created = repository.create(sessionId, exerciseId, 1, null)
        val retrieved = repository.getById(created.id)
        assertNotNull(retrieved)
        assertEquals(created.id, retrieved?.id)
    }

    @Test
    fun getById_missing_returnsNull() = runTest {
        val result = repository.getById(999L)
        assertNull(result)
    }

    @Test
    fun listBySession_returnsOrderedByOrderNumAndId() = runTest {
        repository.create(sessionId, exerciseId, 3, null)
        repository.create(sessionId, exerciseId, 1, null)
        repository.create(sessionId, exerciseId, 2, null)

        val items = repository.listBySession(sessionId).first()
        assertEquals(3, items.size)
        assertEquals(1, items[0].orderNum)
        assertEquals(2, items[1].orderNum)
        assertEquals(3, items[2].orderNum)
    }

    @Test
    fun delete_removesSessionExerciseSnapshot() = runTest {
        val created = repository.create(sessionId, exerciseId, 1, null)
        repository.delete(created)
        val retrieved = repository.getById(created.id)
        assertNull(retrieved)
    }

    @Test
    fun deleteBySessionId_removesAllSessionExercises() = runTest {
        repository.create(sessionId, exerciseId, 1, null)
        repository.create(sessionId, exerciseId, 2, null)
        repository.deleteBySessionId(sessionId)
        val items = repository.listBySession(sessionId).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun deletingSession_cascadesToSessionExercises() = runTest {
        repository.create(sessionId, exerciseId, 1, null)
        database.workoutSessionDao().delete(
            WorkoutSessionEntity(id = sessionId, title = "", startedAt = "", createdAt = "", updatedAt = "")
        )
        val items = repository.listBySession(sessionId).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun deletingSessionExercise_doesNotDeleteExercise() = runTest {
        val created = repository.create(sessionId, exerciseId, 1, null)
        repository.delete(created)
        val exercise = database.exerciseDao().getById(exerciseId)
        assertNotNull(exercise)
    }
}
