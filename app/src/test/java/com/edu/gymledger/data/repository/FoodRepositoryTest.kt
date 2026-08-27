package com.edu.gymledger.data.repository

import androidx.room.Room
import com.edu.gymledger.data.db.GymLedgerDatabase
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
class FoodRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var repository: FoodRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        repository = FoodRepository(database.foodDao())
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun create_validFoodWithMacros_succeeds() = runTest {
        val food = repository.create(
            name = "Chicken Breast",
            caloriesPerServing = 165,
            servingSize = 100.0,
            proteinPerServing = 31.0,
            carbsPerServing = 0.0,
            fatPerServing = 3.6
        )

        assertTrue(food.id > 0)
        assertEquals("Chicken Breast", food.name)
        assertEquals(165, food.caloriesPerServing)
        assertEquals(100.0, food.servingSize!!, 0.001)
        assertEquals(31.0, food.proteinPerServing, 0.001)
        assertEquals(0.0, food.carbsPerServing, 0.001)
        assertEquals(3.6, food.fatPerServing, 0.001)
    }

    @Test
    fun create_trimsName() = runTest {
        val food = repository.create(
            name = "  Banana  ",
            caloriesPerServing = 89,
            servingSize = 100.0
        )

        assertEquals("Banana", food.name)
    }

    @Test
    fun create_defaultsFavoriteAndUsageMetadata() = runTest {
        val food = repository.create(name = "Plain rice", caloriesPerServing = 130)

        assertFalse(food.isFavorite)
        assertNull(food.favoriteAt)
        assertNull(food.lastUsedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_blankName_throws() = runTest {
        repository.create(
            name = "",
            caloriesPerServing = 100,
            servingSize = 100.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_whitespaceOnlyName_throws() = runTest {
        repository.create(
            name = "   ",
            caloriesPerServing = 100,
            servingSize = 100.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_caloriesBelowZero_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = -1,
            servingSize = 100.0
        )
    }

    @Test
    fun create_zeroCalories_succeeds() = runTest {
        val food = repository.create(
            name = "Water",
            caloriesPerServing = 0,
            servingSize = 500.0
        )

        assertEquals(0, food.caloriesPerServing)
    }

    @Test
    fun create_nullServingSize_succeeds() = runTest {
        val food = repository.create(
            name = "Apple",
            caloriesPerServing = 52,
            servingSize = null
        )

        assertNull(food.servingSize)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_servingSizeZero_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 0.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_servingSizeBelowZero_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = -1.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_servingSizeNaN_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = Double.NaN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_servingSizeInfinity_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = Double.POSITIVE_INFINITY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_proteinBelowZero_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0,
            proteinPerServing = -1.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_carbsBelowZero_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0,
            carbsPerServing = -1.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_fatBelowZero_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0,
            fatPerServing = -1.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_proteinNaN_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0,
            proteinPerServing = Double.NaN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_carbsInfinity_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0,
            carbsPerServing = Double.POSITIVE_INFINITY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_fatNegativeInfinity_throws() = runTest {
        repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0,
            fatPerServing = Double.NEGATIVE_INFINITY
        )
    }

    @Test
    fun create_defaultMacros_storeZero() = runTest {
        val food = repository.create(
            name = "Plain Food",
            caloriesPerServing = 50,
            servingSize = 100.0
        )

        assertEquals(0.0, food.proteinPerServing, 0.001)
        assertEquals(0.0, food.carbsPerServing, 0.001)
        assertEquals(0.0, food.fatPerServing, 0.001)
    }

    @Test
    fun getById_existingId_returnsFullFood() = runTest {
        val created = repository.create(
            name = "Salmon",
            caloriesPerServing = 208,
            servingSize = 100.0,
            proteinPerServing = 20.0,
            carbsPerServing = 0.0,
            fatPerServing = 13.0
        )

        val found = repository.getById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found!!.id)
        assertEquals("Salmon", found.name)
        assertEquals(208, found.caloriesPerServing)
        assertEquals(100.0, found.servingSize!!, 0.001)
        assertEquals(20.0, found.proteinPerServing, 0.001)
        assertEquals(0.0, found.carbsPerServing, 0.001)
        assertEquals(13.0, found.fatPerServing, 0.001)
    }

    @Test
    fun getById_nonExistentId_returnsNull() = runTest {
        val found = repository.getById(999)
        assertNull(found)
    }

    @Test
    fun getAll_returnsCreatedFoods() = runTest {
        repository.create(
            name = "Rice",
            caloriesPerServing = 130,
            servingSize = 100.0
        )
        repository.create(
            name = "Beans",
            caloriesPerServing = 127,
            servingSize = 100.0
        )

        val all = repository.getAll().first()

        assertEquals(2, all.size)
    }

    @Test
    fun searchByName_findsPartialName() = runTest {
        repository.create(name = "Chicken Breast", caloriesPerServing = 165)
        repository.create(name = "Chicken Thigh", caloriesPerServing = 209)
        repository.create(name = "Beef Steak", caloriesPerServing = 271)

        val results = repository.searchByName("Chicken").first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.name.contains("Chicken") })
    }

    @Test
    fun searchByName_isCaseInsensitive() = runTest {
        repository.create(name = "Banana", caloriesPerServing = 89)

        val lowerResults = repository.searchByName("banana").first()
        val upperResults = repository.searchByName("BANANA").first()
        val mixedResults = repository.searchByName("bAnAnA").first()

        assertEquals(1, lowerResults.size)
        assertEquals(1, upperResults.size)
        assertEquals(1, mixedResults.size)
    }

    @Test
    fun searchByName_blankQuery_returnsAllFoods() = runTest {
        repository.create(name = "Apple", caloriesPerServing = 52)
        repository.create(name = "Banana", caloriesPerServing = 89)

        val results = repository.searchByName("").first()

        assertEquals(2, results.size)
    }

    @Test
    fun searchByName_whitespaceQuery_returnsAllFoods() = runTest {
        repository.create(name = "Apple", caloriesPerServing = 52)
        repository.create(name = "Banana", caloriesPerServing = 89)

        val results = repository.searchByName("   ").first()

        assertEquals(2, results.size)
    }

    @Test
    fun rankedFiltersAndExplicitSetters_preserveLocalState() = runTest {
        val older = repository.create(name = "Banana", caloriesPerServing = 89)
        val newer = repository.create(name = "Apple", caloriesPerServing = 52)

        repository.setFavorite(older.id, true, 100L)
        repository.markUsed(newer.id, 200L)

        assertEquals(listOf("Banana", "Apple"), repository.getAllRanked().first().map { it.name })
        assertEquals(listOf("Banana"), repository.getFavorites().first().map { it.name })
        assertEquals(listOf("Apple"), repository.getRecent().first().map { it.name })
        assertEquals(200L, repository.getById(newer.id)!!.lastUsedAt)
    }

    @Test
    fun favoriteTimestamps_areAssignedClearedAndIndependentFromUsage() = runTest {
        val older = repository.create(name = "Older", caloriesPerServing = 1)
        val newer = repository.create(name = "Newer", caloriesPerServing = 1)

        repository.setFavorite(older.id, true, 100L)
        repository.setFavorite(newer.id, true, 200L)
        assertEquals(listOf("Newer", "Older"), repository.getFavorites().first().map { it.name })

        repository.markUsed(newer.id, 300L)
        assertEquals(200L, repository.getById(newer.id)!!.favoriteAt)
        assertEquals(listOf("Newer"), repository.getRecent().first().map { it.name })

        repository.setFavorite(newer.id, false, 400L)
        assertEquals(null, repository.getById(newer.id)!!.favoriteAt)
        repository.setFavorite(newer.id, true, 500L)
        assertEquals(listOf("Newer", "Older"), repository.getFavorites().first().map { it.name })
    }

    @Test
    fun update_preservesFavoriteUsageAndFavoriteTimestamps() = runTest {
        val created = repository.create(name = "Rice", caloriesPerServing = 130)
        val marked = created.copy(isFavorite = true, lastUsedAt = 42L, favoriteAt = 99L)

        val updated = repository.update(marked.copy(name = "  Brown Rice  "))

        assertTrue(updated.isFavorite)
        assertEquals(42L, updated.lastUsedAt)
        assertEquals(99L, updated.favoriteAt)
        assertEquals("Brown Rice", updated.name)
    }

    @Test
    fun update_persistsChangedMacros() = runTest {
        val created = repository.create(
            name = "Rice",
            caloriesPerServing = 130,
            servingSize = 100.0,
            proteinPerServing = 2.7,
            carbsPerServing = 28.0,
            fatPerServing = 0.3
        )

        val updated = repository.update(
            created.copy(
                proteinPerServing = 3.0,
                carbsPerServing = 30.0,
                fatPerServing = 0.5
            )
        )

        assertEquals(3.0, updated.proteinPerServing, 0.001)
        assertEquals(30.0, updated.carbsPerServing, 0.001)
        assertEquals(0.5, updated.fatPerServing, 0.001)
    }

    @Test
    fun update_trimsName() = runTest {
        val created = repository.create(
            name = "Old Name",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        val updated = repository.update(created.copy(name = "  New Name  "))

        assertEquals("New Name", updated.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_blankName_throws() = runTest {
        val created = repository.create(
            name = "Valid",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        repository.update(created.copy(name = ""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_negativeCalories_throws() = runTest {
        val created = repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        repository.update(created.copy(caloriesPerServing = -1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_zeroServingSize_throws() = runTest {
        val created = repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        repository.update(created.copy(servingSize = 0.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_negativeProtein_throws() = runTest {
        val created = repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        repository.update(created.copy(proteinPerServing = -1.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_nanCarbs_throws() = runTest {
        val created = repository.create(
            name = "Test Food",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        repository.update(created.copy(carbsPerServing = Double.NaN))
    }

    @Test
    fun delete_removesFood() = runTest {
        val created = repository.create(
            name = "Temporary Food",
            caloriesPerServing = 100,
            servingSize = 100.0
        )

        repository.delete(created)

        val afterDelete = repository.getById(created.id)
        assertNull(afterDelete)
    }
}
