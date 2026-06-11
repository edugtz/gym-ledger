package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
import com.edu.gymledger.data.db.entity.ExerciseEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoutineRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var routineRepository: RoutineRepository
    private lateinit var routineExerciseRepository: RoutineExerciseRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        routineRepository = RoutineRepository(
            database.routineDao(),
            database.routineExerciseDao()
        )
        routineExerciseRepository = RoutineExerciseRepository(
            database.routineExerciseDao(),
            database.routineDao(),
            database.exerciseDao()
        )
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun create_routineWithValidName_succeeds() = runTest {
        val routine = routineRepository.create(name = "Push Day", description = null)

        assertTrue(routine.id > 0)
        assertEquals("Push Day", routine.name)
        assertNull(routine.description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_blankName_throws() = runTest {
        routineRepository.create(name = "", description = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_whitespaceOnlyName_throws() = runTest {
        routineRepository.create(name = "   ", description = null)
    }

    @Test
    fun create_blankDescription_convertsToNull() = runTest {
        val routine = routineRepository.create(name = "Push Day", description = "   ")

        assertNull(routine.description)
    }

    @Test
    fun getById_existingId_returnsRoutine() = runTest {
        val created = routineRepository.create(name = "Push Day", description = null)

        val found = routineRepository.getById(created.id)

        assertNotNull(found)
        assertEquals(created.name, found!!.name)
        assertEquals(created.description, found.description)
    }

    @Test
    fun getById_nonExistentId_returnsNull() = runTest {
        val found = routineRepository.getById(999)

        assertNull(found)
    }

    @Test
    fun getAll_returnsAllRoutines() = runTest {
        routineRepository.create(name = "Push Day", description = null)
        routineRepository.create(name = "Pull Day", description = null)

        val all = routineRepository.getAll().first()

        assertEquals(2, all.size)
    }

    @Test
    fun update_existingRoutine_succeeds() = runTest {
        val created = routineRepository.create(name = "  Old Name  ", description = "  Old Desc  ")

        val updated = routineRepository.update(
            created.copy(name = "  New Name  ", description = "  New Desc  ")
        )

        assertEquals("New Name", updated.name)
        assertEquals("New Desc", updated.description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_blankName_throws() = runTest {
        val created = routineRepository.create(name = "Valid", description = null)

        routineRepository.update(created.copy(name = ""))
    }

    @Test
    fun delete_existingRoutine_removesIt() = runTest {
        val created = routineRepository.create(name = "To Delete", description = null)

        routineRepository.delete(created)

        val afterDelete = routineRepository.getById(created.id)
        assertNull(afterDelete)
    }

    @Test
    fun delete_routineAlsoDeletesRoutineExercises() = runTest {
        val exerciseEntity = ExerciseEntity(
            name = "Bench Press", category = null, primaryMuscle = null,
            secondaryMuscles = null, equipment = null, notes = null,
            createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
        )
        val exerciseId = database.exerciseDao().insert(exerciseEntity)

        val routine = routineRepository.create(name = "Push Day", description = null)
        routineExerciseRepository.create(
            routineId = routine.id,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        routineRepository.delete(routine)

        val exercises = database.routineExerciseDao().listByRoutine(routine.id).first()
        assertTrue(exercises.isEmpty())
    }

    @Test
    fun delete_routineDoesNotDeleteExerciseCatalog() = runTest {
        val exerciseEntity = ExerciseEntity(
            name = "Bench Press", category = null, primaryMuscle = null,
            secondaryMuscles = null, equipment = null, notes = null,
            createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
        )
        val exerciseId = database.exerciseDao().insert(exerciseEntity)

        val routine = routineRepository.create(name = "Push Day", description = null)
        routineExerciseRepository.create(
            routineId = routine.id,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        routineRepository.delete(routine)

        val exercise = database.exerciseDao().getById(exerciseId)
        assertNotNull(exercise)
    }
}
