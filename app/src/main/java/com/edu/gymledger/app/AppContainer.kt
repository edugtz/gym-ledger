package com.edu.gymledger.app

import android.content.Context
import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
import com.edu.gymledger.data.repository.BodyMeasurementRepository
import com.edu.gymledger.data.repository.ExerciseRepository
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.data.repository.RoutineExerciseRepository
import com.edu.gymledger.data.repository.RoutineRepository
import com.edu.gymledger.data.repository.SettingsRepository
import com.edu.gymledger.data.repository.WorkoutRepository
import com.edu.gymledger.data.repository.WorkoutSessionExerciseRepository

object AppContainer {
    private var database: GymLedgerDatabase? = null
        get() {
            if (field == null) {
                throw IllegalStateException("Database not initialized. Call initialize(context)")
            }
            return field
        }

    fun initialize(context: Context) {
        database = Room.databaseBuilder(
            context.applicationContext,
            GymLedgerDatabase::class.java,
            "gym_ledger_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        settingsRepository = SettingsRepository(context.applicationContext)
    }

    val exerciseDao: com.edu.gymledger.data.db.dao.ExerciseDao
        get() = database!!.exerciseDao()

    val workoutSessionDao: com.edu.gymledger.data.db.dao.WorkoutSessionDao
        get() = database!!.workoutSessionDao()

    val workoutSetDao: com.edu.gymledger.data.db.dao.WorkoutSetDao
        get() = database!!.workoutSetDao()

    val routineDao: com.edu.gymledger.data.db.dao.RoutineDao
        get() = database!!.routineDao()

    val routineExerciseDao: com.edu.gymledger.data.db.dao.RoutineExerciseDao
        get() = database!!.routineExerciseDao()

    val foodDao: com.edu.gymledger.data.db.dao.FoodDao
        get() = database!!.foodDao()

    val mealDao: com.edu.gymledger.data.db.dao.MealDao
        get() = database!!.mealDao()

    val mealItemDao: com.edu.gymledger.data.db.dao.MealItemDao
        get() = database!!.mealItemDao()

    val bodyMeasurementDao: com.edu.gymledger.data.db.dao.BodyMeasurementDao
        get() = database!!.bodyMeasurementDao()

    val workoutSessionExerciseDao: com.edu.gymledger.data.db.dao.WorkoutSessionExerciseDao
        get() = database!!.workoutSessionExerciseDao()

    val exerciseRepository: ExerciseRepository
        get() = ExerciseRepository(database!!.exerciseDao())

    val workoutRepository: WorkoutRepository
        get() = WorkoutRepository(
            database!!.workoutSessionDao(),
            database!!.workoutSetDao(),
            database!!.exerciseDao()
        )

    val routineRepository: RoutineRepository
        get() = RoutineRepository(
            database!!.routineDao(),
            database!!.routineExerciseDao()
        )

    val routineExerciseRepository: RoutineExerciseRepository
        get() = RoutineExerciseRepository(
            database!!.routineExerciseDao(),
            database!!.routineDao(),
            database!!.exerciseDao()
        )

    val foodRepository: FoodRepository
        get() = FoodRepository(database!!.foodDao())

    val bodyMeasurementRepository: BodyMeasurementRepository
        get() = BodyMeasurementRepository(database!!.bodyMeasurementDao())

    val workoutSessionExerciseRepository: WorkoutSessionExerciseRepository
        get() = WorkoutSessionExerciseRepository(
            database!!.workoutSessionExerciseDao(),
            database!!.workoutSessionDao(),
            database!!.exerciseDao()
        )

    lateinit var settingsRepository: SettingsRepository
        private set
}
