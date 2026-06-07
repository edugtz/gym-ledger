package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
import com.edu.gymledger.data.db.entity.ExerciseEntity
import com.edu.gymledger.domain.model.WorkoutSession
import com.edu.gymledger.domain.model.WorkoutSet
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
class WorkoutRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var repository: WorkoutRepository
    private var exerciseId: Long = 0L

    @Before
    fun setup() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        repository = WorkoutRepository(
            database.workoutSessionDao(),
            database.workoutSetDao(),
            database.exerciseDao()
        )

        // Insert a valid ExerciseEntity before testing createSet
        val exercise = ExerciseEntity(
            id = 0L,
            name = "Bench Press",
            category = "Upper Body",
            primaryMuscle = "Chest",
            secondaryMuscles = "Triceps",
            equipment = "Barbell",
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            notes = null
        )
        exerciseId = database.exerciseDao().insert(exercise)
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    // 1. create/read/update/delete session
    @Test
    fun session_crud_succeeds() = runTest {
        val title = "Morning Workout"
        val startedAt = Instant.now().toString()
        
        // Create
        val session = repository.createSession(null, title, startedAt, null)
        assertTrue(session.id > 0)
        assertEquals(title, session.title)

        // Read
        val retrieved = repository.getSessionById(session.id)
        assertNotNull(retrieved)
        assertEquals(title, retrieved?.title)

        // Update
        val updatedSession = session.copy(title = "Updated Title", notes = "New Notes")
        val updated = repository.updateSession(updatedSession)
        assertEquals("Updated Title", updated.title)
        assertEquals("New Notes", updated.notes)

        // Delete
        repository.deleteSession(updated)
        val afterDelete = repository.getSessionById(session.id)
        assertNull(afterDelete)
    }

    @Test
    fun createSession_trimsTitleAndNotes() = runTest {
        val session = repository.createSession(null, "  Trim Me  ", Instant.now().toString(), "  Notes Trim  ")
        assertEquals("Trim Me", session.title)
        assertEquals("Notes Trim", session.notes)
    }

    @Test
    fun createSession_blankNotesBecomeNull() = runTest {
        val session = repository.createSession(null, "Title", Instant.now().toString(), "   ")
        assertNull(session.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateSession_blankTitle_throws() = runTest {
        val session = repository.createSession(null, "Valid", Instant.now().toString(), null)
        repository.updateSession(session.copy(title = "  "))
    }

    // 2. create/read/update/delete set
    @Test
    fun set_crud_succeeds() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        
        // Create
        val set = repository.createSet(
            sessionId = session.id,
            exerciseId = exerciseId,
            setIndex = 1,
            reps = 10,
            weight = 60.0,
            rpe = 8.0,
            rir = 2,
            notes = "Good set"
        )
        assertTrue(set.id > 0)

        // Read
        val retrieved = repository.getSetsBySessionId(session.id).find { it.id == set.id }
        assertNotNull(retrieved)
        assertEquals(10, retrieved?.reps)

        // Update
        val updatedSet = set.copy(reps = 12, weight = 65.0)
        val updated = repository.updateSet(updatedSet)
        assertEquals(12, updated.reps)
        assertEquals(65.0, updated.weight)

        // Delete
        repository.deleteSet(updated)
        val afterDelete = repository.getSetsBySessionId(session.id).find { it.id == set.id }
        assertNull(afterDelete)
    }

    @Test
    fun createSet_trimsNotes() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        val set = repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, 2, "  Trim Notes  ")
        assertEquals("Trim Notes", set.notes)
    }

    @Test
    fun createSet_blankNotesBecomeNull() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        val set = repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, 2, "   ")
        assertNull(set.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateSet_invalidReps_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        val set = repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, 2, null)
        repository.updateSet(set.copy(reps = 0))
    }

    // 3. getSessionWithSets returns sets ordered by setIndex/orderNum
    @Test
    fun getSessionWithSets_returnsOrderedSets() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        
        repository.createSet(session.id, exerciseId, 2, 10, 60.0, 8.0, 2, null)
        repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, 2, null)
        repository.createSet(session.id, exerciseId, 3, 10, 60.0, 8.0, 2, null)

        val result = repository.getSessionWithSets(session.id)
        assertNotNull(result)
        assertEquals(3, result!!.sets.size)
        assertEquals(1, result.sets[0].setIndex)
        assertEquals(2, result.sets[1].setIndex)
        assertEquals(3, result.sets[2].setIndex)
    }

    // 4. deleting session deletes sets
    @Test
    fun deleteSession_cascadesToSets() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, 2, null)

        repository.deleteSession(session)

        val sets = repository.getSetsBySessionId(session.id)
        assertTrue(sets.isEmpty())
    }

    // 5. invalid title throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSession_blankTitle_throws() = runTest {
        repository.createSession(null, "   ", Instant.now().toString(), null)
    }

    // 6. invalid setIndex/orderNum throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_invalidSetIndex_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 0, 10, 60.0, 8.0, 2, null)
    }

    // 7. invalid reps throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_invalidReps_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 1, 0, 60.0, 8.0, 2, null)
    }

    // 8. invalid weight throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_negativeWeight_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 1, 10, -5.0, 8.0, 2, null)
    }

    // 9. invalid rpe throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_invalidRpe_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 1, 10, 60.0, 11.0, 2, null)
    }

    // 10. invalid rir throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_invalidRir_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, 11, null)
    }

    // 10b. negative rir throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_negativeRir_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, exerciseId, 1, 10, 60.0, 8.0, -1, null)
    }

    // 11. missing sessionId throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_missingSession_throws() = runTest {
        repository.createSet(999L, exerciseId, 1, 10, 60.0, 8.0, 2, null)
    }

    // 12. missing exerciseId throws IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun createSet_missingExercise_throws() = runTest {
        val session = repository.createSession(null, "Session", Instant.now().toString(), null)
        repository.createSet(session.id, 999L, 1, 10, 60.0, 8.0, 2, null)
    }
}
