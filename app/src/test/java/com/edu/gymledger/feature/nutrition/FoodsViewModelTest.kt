package com.edu.gymledger.feature.nutrition

import com.edu.gymledger.data.db.dao.FoodDao
import com.edu.gymledger.data.db.entity.FoodEntity
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.domain.model.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeFoodDao
    private lateinit var viewModel: FoodsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeFoodDao()
        viewModel = FoodsViewModel(FoodRepository(fakeDao))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. The visible query must update synchronously on every onValueChange.

    @Test
    fun updateSearchQuery_immediatelyUpdatesUiState() = runTest {
        viewModel.updateSearchQuery("c")

        assertEquals("c", viewModel.uiState.value.searchQuery)
    }

    // 2. Rapid sequential typing must land in the visible query without loss or reordering.

    @Test
    fun rapidTyping_immediatelyReflectsFullQuery() = runTest {
        viewModel.updateSearchQuery("c")
        viewModel.updateSearchQuery("ch")
        viewModel.updateSearchQuery("chi")
        viewModel.updateSearchQuery("chic")
        viewModel.updateSearchQuery("chick")
        viewModel.updateSearchQuery("chicke")
        viewModel.updateSearchQuery("chicken")

        assertEquals("chicken", viewModel.uiState.value.searchQuery)
    }

    // 3. A delayed repository result must not roll the visible query back.

    @Test
    fun delayedRepositoryResult_cannotRollBackVisibleQuery() = runTest {
        fakeDao.seed(
            FoodEntity(id = 1, name = "Chicken breast", caloriesPerServing = 165),
            FoodEntity(id = 2, name = "Chickpeas", caloriesPerServing = 134)
        )
        fakeDao.searchDelayMillis = 500L

        viewModel.updateSearchQuery("c")
        viewModel.updateSearchQuery("ch")

        assertEquals("ch", viewModel.uiState.value.searchQuery)

        advanceUntilIdle()

        assertEquals("ch", viewModel.uiState.value.searchQuery)
        assertEquals(
            listOf("Chicken breast", "Chickpeas"),
            viewModel.uiState.value.foods.map { it.name }
        )
    }

    // 4. Clearing the search must immediately produce an empty visible query.

    @Test
    fun clearSearch_immediatelyProducesEmptyQuery() = runTest {
        fakeDao.seed(
            FoodEntity(id = 1, name = "Chicken breast", caloriesPerServing = 165)
        )

        viewModel.updateSearchQuery("chicken")
        advanceUntilIdle()
        assertEquals("chicken", viewModel.uiState.value.searchQuery)

        viewModel.updateSearchQuery("")

        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    // 5. Food filtering must still follow the current query.

    @Test
    fun filtering_followsCurrentQuery() = runTest {
        fakeDao.seed(
            FoodEntity(id = 1, name = "Chicken breast", caloriesPerServing = 165),
            FoodEntity(id = 2, name = "Chickpeas", caloriesPerServing = 134),
            FoodEntity(id = 3, name = "Egg", caloriesPerServing = 78)
        )

        viewModel.updateSearchQuery("chicken")
        advanceUntilIdle()
        assertEquals(
            listOf("Chicken breast"),
            viewModel.uiState.value.foods.map { it.name }
        )

        viewModel.updateSearchQuery("chick")
        advanceUntilIdle()
        assertEquals(
            listOf("Chicken breast", "Chickpeas"),
            viewModel.uiState.value.foods.map { it.name }
        )

        viewModel.updateSearchQuery("")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.foods.size)
    }

    // 6. Existing add/edit/delete behavior must remain unaffected.

    @Test
    fun addFood_savesAndEmitsSuccess() = runTest {
        viewModel.addFood("Oatmeal", "150", "40", "5", "27", "3")
        advanceUntilIdle()

        assertEquals(FoodsUiEvent.SaveSucceeded, viewModel.events.first())
        assertEquals(1, fakeDao.storedFoods().size)
        assertEquals("Oatmeal", fakeDao.storedFoods()[0].name)
    }

    @Test
    fun addFood_blankName_emitsErrorAndSavesNothing() = runTest {
        viewModel.addFood("   ", "150")
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is FoodsUiEvent.Error)
        assertEquals(0, fakeDao.storedFoods().size)
    }

    @Test
    fun updateFood_savesAndEmitsSuccess() = runTest {
        val food = Food(id = 1, name = "Oatmeal", caloriesPerServing = 150)
        fakeDao.seed(food.toEntity())

        viewModel.updateFood(food, "Oatmeal flakes", "160")
        advanceUntilIdle()

        assertEquals(FoodsUiEvent.SaveSucceeded, viewModel.events.first())
        assertEquals("Oatmeal flakes", fakeDao.storedFoods()[0].name)
    }

    @Test
    fun deleteFood_removesAndEmitsSuccess() = runTest {
        val food = Food(id = 1, name = "Oatmeal", caloriesPerServing = 150)
        fakeDao.seed(food.toEntity())

        viewModel.deleteFood(food)
        advanceUntilIdle()

        assertEquals(FoodsUiEvent.DeleteSucceeded, viewModel.events.first())
        assertTrue(fakeDao.storedFoods().isEmpty())
    }

    // --- Handwritten fake ---

    class FakeFoodDao : FoodDao {
        private val stored = mutableListOf<FoodEntity>()
        private val listAllFlow = MutableStateFlow(emptyList<FoodEntity>())
        var searchDelayMillis: Long = 0L

        fun seed(vararg foods: FoodEntity) {
            stored.clear()
            stored.addAll(foods)
            listAllFlow.value = stored.toList()
        }

        fun storedFoods(): List<FoodEntity> = stored.toList()

        override suspend fun insert(food: FoodEntity): Long {
            val id = (stored.maxOfOrNull { it.id } ?: 0L) + 1
            val inserted = food.copy(id = id)
            stored.add(inserted)
            listAllFlow.value = stored.toList()
            return id
        }

        override suspend fun update(food: FoodEntity) {
            val idx = stored.indexOfFirst { it.id == food.id }
            if (idx >= 0) {
                stored[idx] = food
                listAllFlow.value = stored.toList()
            }
        }

        override suspend fun delete(food: FoodEntity) {
            stored.removeAll { it.id == food.id }
            listAllFlow.value = stored.toList()
        }

        override suspend fun getById(id: Long): FoodEntity? {
            return stored.find { it.id == id }
        }

        override fun listAll(): Flow<List<FoodEntity>> = listAllFlow

        override fun searchByName(query: String): Flow<List<FoodEntity>> = flow {
            if (searchDelayMillis > 0L) {
                delay(searchDelayMillis)
            }
            val lower = query.lowercase()
            listAllFlow.collect { items ->
                emit(items.filter { it.name.lowercase().contains(lower) })
            }
        }
    }
}
