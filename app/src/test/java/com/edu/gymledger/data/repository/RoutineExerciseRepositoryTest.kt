package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
import com.edu.gymledger.data.db.entity.ExerciseEntity
import com.edu.gymledger.data.db.entity.RoutineEntity
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
class RoutineExerciseRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var repository: RoutineExerciseRepository
    private var routineId: Long = 0L
    private var exerciseId: Long = 0L

    @Before
    fun setup() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        repository = RoutineExerciseRepository(
            database.routineExerciseDao(),
            database.routineDao(),
            database.exerciseDao()
        )

        val routineEntity = RoutineEntity(name = "Test Routine", description = null)
        routineId = database.routineDao().insert(routineEntity)

        val exerciseEntity = ExerciseEntity(
            name = "Bench Press", category = null, primaryMuscle = null,
            secondaryMuscles = null, equipment = null, notes = null,
            createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
        )
        exerciseId = database.exerciseDao().insert(exerciseEntity)
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun create_withValidFields_succeeds() = runTest {
        val routineExercise = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        assertTrue(routineExercise.id > 0)
        assertEquals(routineId, routineExercise.routineId)
        assertEquals(exerciseId, routineExercise.exerciseId)
        assertEquals(1, routineExercise.orderNum)
        assertNull(routineExercise.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_missingRoutineId_throws() = runTest {
        repository.create(
            routineId = 999L,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_missingExerciseId_throws() = runTest {
        repository.create(
            routineId = routineId,
            exerciseId = 999L,
            orderNum = 1,
            notes = null
        )
    }

    @Test
    fun create_orderNumNull_defaultsToMaxPlusOne() = runTest {
        val first = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = null,
            notes = null
        )
        assertEquals(1, first.orderNum)

        val second = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = null,
            notes = null
        )
        assertEquals(2, second.orderNum)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_orderNumLessThanOne_throws() = runTest {
        repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 0,
            notes = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_duplicateExplicitOrderNum_throws() = runTest {
        repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )
        repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )
    }

    @Test
    fun create_blankNotes_convertsToNull() = runTest {
        val routineExercise = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = "   "
        )

        assertNull(routineExercise.notes)
    }

    @Test
    fun getById_existingId_returnsRoutineExercise() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        val found = repository.getById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found!!.id)
    }

    @Test
    fun getById_nonExistentId_returnsNull() = runTest {
        val found = repository.getById(999)

        assertNull(found)
    }

    @Test
    fun listByRoutine_returnsOrderedByOrderNum() = runTest {
        repository.create(routineId = routineId, exerciseId = exerciseId, orderNum = 3, notes = null)
        repository.create(routineId = routineId, exerciseId = exerciseId, orderNum = 1, notes = null)
        repository.create(routineId = routineId, exerciseId = exerciseId, orderNum = 2, notes = null)

        val list = repository.listByRoutine(routineId).first()

        assertEquals(3, list.size)
        assertEquals(1, list[0].orderNum)
        assertEquals(2, list[1].orderNum)
        assertEquals(3, list[2].orderNum)
    }

    @Test
    fun update_existingRoutineExercise_succeeds() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = "  old notes  "
        )

        val updated = repository.update(
            created.copy(orderNum = 2, notes = "  new notes  ")
        )

        assertEquals(2, updated.orderNum)
        assertEquals("new notes", updated.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_missingRoutineId_throws() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        repository.update(created.copy(routineId = 999L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_missingExerciseId_throws() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        repository.update(created.copy(exerciseId = 999L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_orderNumLessThanOne_throws() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        repository.update(created.copy(orderNum = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_duplicateExplicitOrderNum_throws() = runTest {
        val first = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )
        val second = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 2,
            notes = null
        )

        repository.update(second.copy(orderNum = 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun from_nullOrderNum_throws() {
        val entity = com.edu.gymledger.data.db.entity.RoutineExerciseEntity(
            id = 1L,
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = null,
            notes = null
        )
        com.edu.gymledger.domain.model.RoutineExercise.from(entity)
    }

    @Test
    fun delete_existingRoutineExercise_removesIt() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        repository.delete(created)

        val afterDelete = repository.getById(created.id)
        assertNull(afterDelete)
    }

    @Test
    fun delete_routineExerciseDoesNotDeleteExerciseCatalog() = runTest {
        val created = repository.create(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = 1,
            notes = null
        )

        repository.delete(created)

        val exercise = database.exerciseDao().getById(exerciseId)
        assertNotNull(exercise)
    }
}
