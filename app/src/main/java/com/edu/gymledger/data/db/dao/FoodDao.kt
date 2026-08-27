package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun listAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY name COLLATE NOCASE ASC, id DESC")
    fun searchByName(query: String): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        ORDER BY isFavorite DESC, favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
    """)
    fun listAllRanked(): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY isFavorite DESC, favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
    """)
    fun searchRanked(query: String): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE isFavorite = 1
        ORDER BY favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
    """)
    fun listFavorites(): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE isFavorite = 1
          AND name LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
    """)
    fun searchFavorites(query: String): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE lastUsedAt IS NOT NULL
        ORDER BY lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
    """)
    fun listRecent(): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE lastUsedAt IS NOT NULL
          AND name LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
    """)
    fun searchRecent(query: String): Flow<List<FoodEntity>>

    @Query("UPDATE foods SET isFavorite = :isFavorite, favoriteAt = CASE WHEN :isFavorite THEN :favoriteAt ELSE NULL END WHERE id = :foodId")
    suspend fun setFavorite(foodId: Long, isFavorite: Boolean, favoriteAt: Long?)

    @Query("UPDATE foods SET lastUsedAt = :usedAtMillis WHERE id = :foodId")
    suspend fun markUsed(foodId: Long, usedAtMillis: Long)
}
