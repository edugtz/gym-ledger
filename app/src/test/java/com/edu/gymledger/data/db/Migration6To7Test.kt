package com.edu.gymledger.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.edu.gymledger.data.db.entity.FoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration6To7Test {
    private val context = RuntimeEnvironment.getApplication() as Context
    private val databaseName = "migration-6-to-7-${System.nanoTime()}"
    private lateinit var legacyHelper: SupportSQLiteOpenHelper
    private lateinit var database: GymLedgerDatabase

    @Before
    fun setup() {
        legacyHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(LegacyCallback(6, includePhase17HColumns = false))
                .build()
        )
        legacyHelper.writableDatabase.close()
        legacyHelper.close()

        database = Room.databaseBuilder(context, GymLedgerDatabase::class.java, databaseName)
            .addMigrations(GymLedgerDatabase.MIGRATION_6_7, GymLedgerDatabase.MIGRATION_7_8)
            .build()
        database.openHelper.writableDatabase
    }

    @After
    fun teardown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun roomOpenMigratesAndValidatesTheCompleteV6Database() = runBlocking {
        val legacy = requireNotNull(database.foodDao().getById(1L))

        assertEquals("Distinctive legacy food", legacy.name)
        assertEquals(987, legacy.caloriesPerServing)
        assertEquals(123.45, legacy.servingSize!!, 0.0)
        assertEquals(67.89, legacy.proteinPerServing, 0.0)
        assertEquals(45.67, legacy.carbsPerServing, 0.0)
        assertEquals(23.45, legacy.fatPerServing, 0.0)
        assertFalse(legacy.isFavorite)
        assertNull(legacy.lastUsedAt)

        val currentId = database.foodDao().insert(
            FoodEntity(
                name = "Current food",
                caloriesPerServing = 111,
                servingSize = 10.0,
                proteinPerServing = 2.0,
                carbsPerServing = 3.0,
                fatPerServing = 4.0,
                isFavorite = true,
                lastUsedAt = 123456789L
                ,favoriteAt = 987654321L
            )
        )
        val reread = requireNotNull(database.foodDao().getById(currentId))

        assertTrue(reread.isFavorite)
        assertEquals(123456789L, reread.lastUsedAt)
        assertEquals(987654321L, reread.favoriteAt)
    }

    @Test
    fun roomOpenMigratesAndValidatesARealV7DatabaseToV8() = runBlocking {
        val v7Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("$databaseName-v7")
                .callback(LegacyCallback(7, includePhase17HColumns = true))
                .build()
        )
        v7Helper.writableDatabase.close()
        v7Helper.close()

        val v8Database = Room.databaseBuilder(context, GymLedgerDatabase::class.java, "$databaseName-v7")
            .addMigrations(GymLedgerDatabase.MIGRATION_7_8)
            .build()
        v8Database.openHelper.writableDatabase

        try {
            val food = requireNotNull(v8Database.foodDao().getById(1L))
            assertTrue(food.isFavorite)
            assertEquals(765L, food.lastUsedAt)
            assertNull(food.favoriteAt)
        } finally {
            v8Database.close()
            context.deleteDatabase("$databaseName-v7")
        }
    }

    private class LegacyCallback(
        version: Int,
        private val includePhase17HColumns: Boolean
    ) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT, primaryMuscle TEXT, secondaryMuscles TEXT, equipment TEXT, notes TEXT, isFavorite INTEGER NOT NULL, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL)")
            db.execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER, title TEXT NOT NULL, startedAt TEXT NOT NULL, endedAt TEXT, notes TEXT, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL)")
            db.execSQL("CREATE TABLE workout_sets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, setIndex INTEGER NOT NULL, reps INTEGER NOT NULL, weight REAL, rpe REAL, rir INTEGER, notes TEXT, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE TABLE workout_session_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderNum INTEGER NOT NULL, notes TEXT, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT)")
            db.execSQL("CREATE TABLE routine_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderNum INTEGER, notes TEXT, FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            val phase17HColumns = if (includePhase17HColumns) ", isFavorite INTEGER NOT NULL DEFAULT 0, lastUsedAt INTEGER DEFAULT NULL" else ""
            db.execSQL("CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, caloriesPerServing INTEGER NOT NULL, servingSize REAL, proteinPerServing REAL NOT NULL DEFAULT 0.0, carbsPerServing REAL NOT NULL DEFAULT 0.0, fatPerServing REAL NOT NULL DEFAULT 0.0$phase17HColumns)")
            db.execSQL("CREATE TABLE meals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, notes TEXT)")
            db.execSQL("CREATE TABLE meal_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mealId INTEGER NOT NULL, foodId INTEGER NOT NULL, quantity REAL NOT NULL, FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(foodId) REFERENCES foods(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE TABLE body_measurements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, weight REAL, waist REAL, chest REAL, arm REAL, thigh REAL, hip REAL, notes TEXT)")
            db.execSQL("CREATE INDEX index_workout_sets_sessionId ON workout_sets(sessionId)")
            db.execSQL("CREATE INDEX index_workout_sets_exerciseId ON workout_sets(exerciseId)")
            db.execSQL("CREATE INDEX index_workout_session_exercises_sessionId ON workout_session_exercises(sessionId)")
            db.execSQL("CREATE INDEX index_workout_session_exercises_exerciseId ON workout_session_exercises(exerciseId)")
            db.execSQL("CREATE INDEX index_routine_exercises_routineId ON routine_exercises(routineId)")
            db.execSQL("CREATE INDEX index_routine_exercises_exerciseId ON routine_exercises(exerciseId)")
            db.execSQL("CREATE INDEX index_meal_items_mealId ON meal_items(mealId)")
            db.execSQL("CREATE INDEX index_meal_items_foodId ON meal_items(foodId)")
            if (includePhase17HColumns) {
                db.execSQL("INSERT INTO foods (id, name, caloriesPerServing, servingSize, proteinPerServing, carbsPerServing, fatPerServing, isFavorite, lastUsedAt) VALUES (1, 'Distinctive legacy food', 987, 123.45, 67.89, 45.67, 23.45, 1, 765)")
            } else {
                db.execSQL("INSERT INTO foods (id, name, caloriesPerServing, servingSize, proteinPerServing, carbsPerServing, fatPerServing) VALUES (1, 'Distinctive legacy food', 987, 123.45, 67.89, 45.67, 23.45)")
            }
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
