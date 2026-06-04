package com.edu.gymledger.data.db

import androidx.room.Room
import com.edu.gymledger.data.db.dao.ExerciseDao
import com.edu.gymledger.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExerciseDaoTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }
    
    @Test
    fun insertExercise_insertsSuccessfully_returnsId() = runTest {
        val exercise = ExerciseEntity(
            name = "Bench Press",
            category = "Upper Body",
            primaryMuscle = "Chest",
            secondaryMuscles = "Triceps, Shoulders",
            equipment = "Barbell",
            notes = null,
            createdAt = "2026-05-23T10:00:00Z",
            updatedAt = "2026-05-23T10:00:00Z"
        )

        val id = exerciseDao.insert(exercise)

        assertTrue(id > 0)
    }

    @Test
    fun insertExercise_thenGetById_returnsCorrectEntity() = runTest {
        val exercise = ExerciseEntity(
            name = "Squat",
            category = "Lower Body",
            primaryMuscle = "Quadriceps",
            secondaryMuscles = "Glutes, Hamstrings",
            equipment = "Barbell",
            notes = null,
            createdAt = "2026-05-23T10:00:00Z",
            updatedAt = "2026-05-23T10:00:00Z"
        )

        val id = exerciseDao.insert(exercise)
        val retrievedExercise = exerciseDao.getById(id)

        assertNotNull(retrievedExercise)
        assertEquals(id, retrievedExercise!!.id)
        assertEquals("Squat", retrievedExercise.name)
    }

    @Test
    fun insertMultipleExercises_thenListAll_returnsAll() = runTest {
        val exercise1 = ExerciseEntity(
            name = "Deadlift",
            category = "Full Body",
            primaryMuscle = "Erector Spinae",
            secondaryMuscles = "Hamstrings, Glutes",
            equipment = "Barbell",
            notes = null,
            createdAt = "2026-05-23T10:00:00Z",
            updatedAt = "2026-05-23T10:00:00Z"
        )

        val exercise2 = ExerciseEntity(
            name = "Bicep Curls",
            category = "Upper Body",
            primaryMuscle = "Biceps",
            secondaryMuscles = null,
            equipment = "Dumbbell",
            notes = null,
            createdAt = "2026-05-23T10:00:00Z",
            updatedAt = "2026-05-23T10:00:00Z"
        )

        exerciseDao.insert(exercise1)
        val id2 = exerciseDao.insert(exercise2)

        val exercisesList = exerciseDao.listAll().first()

        assertTrue(exercisesList.size >= 2)
    }

    @Test
    fun insertExercise_thenDelete_removesFromDatabase() = runTest {
        val exercise = ExerciseEntity(
            name = "Overhead Press",
            category = "Upper Body",
            primaryMuscle = "Deltoids",
            secondaryMuscles = "Triceps",
            equipment = "Barbell",
            notes = null,
            createdAt = "2026-05-23T10:00:00Z",
            updatedAt = "2026-05-23T10:00:00Z"
        )

        val id = exerciseDao.insert(exercise)
        assertNotNull(exerciseDao.getById(id))

        val persistedExercise = exerciseDao.getById(id)!!
        exerciseDao.delete(persistedExercise)
        val deletedExercise = exerciseDao.getById(id)

        assertNull(deletedExercise)
    }
}
