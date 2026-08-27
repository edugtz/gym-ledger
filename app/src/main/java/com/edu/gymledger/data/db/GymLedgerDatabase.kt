package com.edu.gymledger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 8,
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
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): GymLedgerDatabase? {
            return INSTANCE
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN lastUsedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN favoriteAt INTEGER DEFAULT NULL")
            }
        }
    }
}
