package com.edu.gymledger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.edu.gymledger.data.db.dao.*
import com.edu.gymledger.data.db.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionExerciseEntity::class,
        FoodEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        BodyMeasurementEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GymLedgerDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun mealItemDao(): MealItemDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun workoutSessionExerciseDao(): WorkoutSessionExerciseDao

    companion object {
        private var INSTANCE: GymLedgerDatabase? = null

        fun create(context: Context): GymLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymLedgerDatabase::class.java,
                    "gym_ledger_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): GymLedgerDatabase? {
            return INSTANCE
        }
    }
}