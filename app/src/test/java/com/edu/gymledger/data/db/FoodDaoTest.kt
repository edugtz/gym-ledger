package com.edu.gymledger.data.db

import androidx.room.Room
import com.edu.gymledger.data.db.entity.FoodEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FoodDaoTest {
    private lateinit var database: GymLedgerDatabase
    private lateinit var dao: com.edu.gymledger.data.db.dao.FoodDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GymLedgerDatabase::class.java).build()
        dao = database.foodDao()
    }

    @After
    fun teardown() = database.close()

    @Test
    fun rankedAndFilteredQueries_followPhaseOrdering() = runTest {
        dao.insert(FoodEntity(id = 1, name = "Banana", caloriesPerServing = 1, lastUsedAt = 100))
        dao.insert(FoodEntity(id = 2, name = "Apple", caloriesPerServing = 1, isFavorite = true, favoriteAt = 50, lastUsedAt = 50))
        dao.insert(FoodEntity(id = 3, name = "Avocado", caloriesPerServing = 1, isFavorite = true, favoriteAt = 100, lastUsedAt = 100))

        assertEquals(listOf("Avocado", "Apple", "Banana"), dao.listAllRanked().first().map { it.name })
        assertEquals(listOf("Avocado", "Apple"), dao.listFavorites().first().map { it.name })
        assertEquals(listOf("Avocado", "Banana", "Apple"), dao.listRecent().first().map { it.name })
        assertEquals(listOf("Apple"), dao.searchFavorites("app").first().map { it.name })
    }

    @Test
    fun setters_updateOnlyRequestedFields() = runTest {
        val id = dao.insert(FoodEntity(name = "Rice", caloriesPerServing = 1))
        dao.setFavorite(id, true, 2000L)
        dao.markUsed(id, 1234L)
        val food = dao.getById(id)!!
        assertTrue(food.isFavorite)
        assertEquals(1234L, food.lastUsedAt)
        assertEquals(2000L, food.favoriteAt)

        dao.setFavorite(id, false, 3000L)
        val unfavorited = dao.getById(id)!!
        assertTrue(!unfavorited.isFavorite)
        assertEquals(null, unfavorited.favoriteAt)
        assertEquals(1234L, unfavorited.lastUsedAt)

        dao.setFavorite(id, true, 4000L)
        dao.markUsed(id, 5678L)
        val refavorited = dao.getById(id)!!
        assertEquals(4000L, refavorited.favoriteAt)
        assertEquals(5678L, refavorited.lastUsedAt)
    }

    @Test
    fun searchQueries_preserveTheirFilterOrdering() = runTest {
        dao.insert(FoodEntity(id = 1, name = "Oat bar", caloriesPerServing = 1, isFavorite = true, favoriteAt = 100, lastUsedAt = 900))
        dao.insert(FoodEntity(id = 2, name = "Oatmeal", caloriesPerServing = 1, isFavorite = true, favoriteAt = 200, lastUsedAt = 100))
        dao.insert(FoodEntity(id = 3, name = "Oat drink", caloriesPerServing = 1, lastUsedAt = 800, favoriteAt = 300))

        assertEquals(listOf("Oatmeal", "Oat bar", "Oat drink"), dao.searchRanked("Oat").first().map { it.name })
        assertEquals(listOf("Oatmeal", "Oat bar"), dao.searchFavorites("Oat").first().map { it.name })
        assertEquals(listOf("Oat bar", "Oat drink", "Oatmeal"), dao.searchRecent("Oat").first().map { it.name })
    }
}
