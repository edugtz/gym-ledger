package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
import com.edu.gymledger.data.db.entity.ExerciseType
import com.edu.gymledger.data.db.entity.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExerciseRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var repository: ExerciseRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        repository = ExerciseRepository(database.exerciseDao())
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun create_exerciseWithValidName_succeeds() = runTest {
        val exercise = repository.create(
            name = "Bench Press",
            type = ExerciseType.COMPOUND,
            muscleGroup = MuscleGroup.CHEST
        )

        assertTrue(exercise.id > 0)
        assertEquals("Bench Press", exercise.name)
        assertEquals(ExerciseType.COMPOUND, exercise.type)
        assertEquals(MuscleGroup.CHEST, exercise.muscleGroup)
    }

    @Test
    fun create_exerciseNameIsTrimmed() = runTest {
        val exercise = repository.create(
            name = "  Squat  ",
            type = ExerciseType.COMPOUND,
            muscleGroup = MuscleGroup.LEGS
        )

        assertEquals("Squat", exercise.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_blankName_throws() = runTest {
        repository.create(
            name = "",
            type = ExerciseType.ISOLATION,
            muscleGroup = MuscleGroup.ARMS
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_whitespaceOnlyName_throws() = runTest {
        repository.create(
            name = "   ",
            type = ExerciseType.ISOLATION,
            muscleGroup = MuscleGroup.ARMS
        )
    }

    @Test
    fun getById_existingId_returnsExercise() = runTest {
        val created = repository.create(
            name = "Deadlift",
            type = ExerciseType.COMPOUND,
            muscleGroup = MuscleGroup.BACK
        )

        val found = repository.getById(created.id)

        assertNotNull(found)
        assertEquals(created.name, found!!.name)
    }

    @Test
    fun getById_nonExistentId_returnsNull() = runTest {
        val found = repository.getById(999)
        assertNull(found)
    }

    @Test
    fun getAll_returnsAllExercises() = runTest {
        repository.create("Bench Press", ExerciseType.COMPOUND, MuscleGroup.CHEST)
        repository.create("Squat", ExerciseType.COMPOUND, MuscleGroup.LEGS)

        val all = repository.getAll().first()

        assertEquals(2, all.size)
    }

    @Test
    fun update_existingExercise_succeeds() = runTest {
        val created = repository.create(
            name = "Old Name",
            type = ExerciseType.ISOLATION,
            muscleGroup = MuscleGroup.ARMS
        )

        val updated = repository.update(created.copy(name = "  New Name  "))

        assertEquals("New Name", updated.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_blankName_throws() = runTest {
        val created = repository.create(
            name = "Valid",
            type = ExerciseType.ISOLATION,
            muscleGroup = MuscleGroup.ARMS
        )

        repository.update(created.copy(name = ""))
    }

    @Test
    fun delete_existingExercise_removesIt() = runTest {
        val created = repository.create(
            name = "Overhead Press",
            type = ExerciseType.COMPOUND,
            muscleGroup = MuscleGroup.SHOULDERS
        )

        repository.delete(created)

        val afterDelete = repository.getById(created.id)
        assertNull(afterDelete)
    }
}
