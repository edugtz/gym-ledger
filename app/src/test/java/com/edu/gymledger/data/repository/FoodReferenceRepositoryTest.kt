package com.edu.gymledger.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FoodReferenceRepositoryTest {

    private lateinit var repository: FoodReferenceRepository

    @Before
    fun setup() {
        repository = FoodReferenceRepository()
    }

    @Test
    fun `blank query returns all references`() {
        val results = repository.search("")
        assertEquals(repository.listAll().size, results.size)
    }

    @Test
    fun `search by English name`() {
        val results = repository.search("chicken breast")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name.contains("Chicken breast") })
    }

    @Test
    fun `search by English alias`() {
        val results = repository.search("egg")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.id == "whole_egg_large" })
    }

    @Test
    fun `search by Spanish alias`() {
        val results = repository.search("huevo")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.id == "whole_egg_large" })
    }

    @Test
    fun `search is case-insensitive`() {
        val lower = repository.search("chicken")
        val upper = repository.search("CHICKEN")
        val mixed = repository.search("ChIcKeN")
        assertEquals(lower.map { it.id }, upper.map { it.id })
        assertEquals(lower.map { it.id }, mixed.map { it.id })
    }

    @Test
    fun `unknown query returns empty list`() {
        val results = repository.search("xyznonexistentfood123")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `results are stable and sorted`() {
        val first = repository.listAll()
        val second = repository.listAll()
        assertEquals(first, second)
        val names = first.map { it.name.lowercase() }
        assertEquals(names, names.sorted())
    }

    @Test
    fun `search with whitespace-only query returns all`() {
        val results = repository.search("   ")
        assertEquals(repository.listAll().size, results.size)
    }

    @Test
    fun `search returns multiple matches`() {
        val results = repository.search("rice")
        assertTrue(results.size >= 2)
    }
}
