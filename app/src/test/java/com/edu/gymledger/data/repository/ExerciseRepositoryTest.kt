package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
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
            category = "Upper Body",
            primaryMuscle = "Chest",
            secondaryMuscles = "Triceps, Shoulders",
            equipment = "Barbell",
            notes = null
        )

        assertTrue(exercise.id > 0)
        assertEquals("Bench Press", exercise.name)
        assertEquals("Upper Body", exercise.category)
        assertEquals("Chest", exercise.primaryMuscle)
        assertEquals("Barbell", exercise.equipment)
    }

    @Test
    fun create_exerciseNameIsTrimmed() = runTest {
        val exercise = repository.create(
            name = "  Squat  ",
            category = "Lower Body",
            primaryMuscle = "Quadriceps",
            secondaryMuscles = "Glutes, Hamstrings",
            equipment = "Barbell",
            notes = null
        )

        assertEquals("Squat", exercise.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_blankName_throws() = runTest {
        repository.create(
            name = "",
            category = "Upper Body",
            primaryMuscle = "Biceps",
            secondaryMuscles = null,
            equipment = "Dumbbell",
            notes = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_whitespaceOnlyName_throws() = runTest {
        repository.create(
            name = "   ",
            category = "Upper Body",
            primaryMuscle = "Biceps",
            secondaryMuscles = null,
            equipment = "Dumbbell",
            notes = null
        )
    }

    @Test
    fun getById_existingId_returnsExercise() = runTest {
        val created = repository.create(
            name = "Deadlift",
            category = "Full Body",
            primaryMuscle = "Erector Spinae",
            secondaryMuscles = "Hamstrings, Glutes",
            equipment = "Barbell",
            notes = null
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
        repository.create(
            name = "Bench Press",
            category = "Upper Body",
            primaryMuscle = "Chest",
            secondaryMuscles = null,
            equipment = "Barbell",
            notes = null
        )
        repository.create(
            name = "Squat",
            category = "Lower Body",
            primaryMuscle = "Quadriceps",
            secondaryMuscles = null,
            equipment = "Barbell",
            notes = null
        )

        val all = repository.getAll().first()

        assertEquals(2, all.size)
    }

    @Test
    fun update_existingExercise_succeeds() = runTest {
        val created = repository.create(
            name = "Old Name",
            category = "Upper Body",
            primaryMuscle = "Biceps",
            secondaryMuscles = null,
            equipment = "Dumbbell",
            notes = null
        )

        val updated = repository.update(created.copy(name = "  New Name  "))

        assertEquals("New Name", updated.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_blankName_throws() = runTest {
        val created = repository.create(
            name = "Valid",
            category = "Upper Body",
            primaryMuscle = "Biceps",
            secondaryMuscles = null,
            equipment = "Dumbbell",
            notes = null
        )

        repository.update(created.copy(name = ""))
    }

    @Test
    fun delete_existingExercise_removesIt() = runTest {
        val created = repository.create(
            name = "Overhead Press",
            category = "Upper Body",
            primaryMuscle = "Deltoids",
            secondaryMuscles = "Triceps",
            equipment = "Barbell",
            notes = null
        )

        repository.delete(created)

        val afterDelete = repository.getById(created.id)
        assertNull(afterDelete)
    }
}
