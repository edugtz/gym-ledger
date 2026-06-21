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
class BodyMeasurementRepositoryTest {

    private lateinit var database: GymLedgerDatabase
    private lateinit var repository: BodyMeasurementRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GymLedgerDatabase::class.java
        ).build()
        repository = BodyMeasurementRepository(database.bodyMeasurementDao())
    }

    @After
    fun teardown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun create_validMeasurement_succeeds() = runTest {
        val measurement = repository.create(
            date = "2025-01-15",
            weight = 75.5
        )

        assertTrue(measurement.id > 0)
        assertEquals("2025-01-15", measurement.date)
        assertEquals(75.5, measurement.weight!!, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_blankDate_throws() = runTest {
        repository.create(
            date = "  ",
            weight = 75.5
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_negativeWeight_throws() = runTest {
        repository.create(
            date = "2025-01-15",
            weight = -1.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_zeroWeight_throws() = runTest {
        repository.create(
            date = "2025-01-15",
            weight = 0.0
        )
    }

    @Test
    fun create_nullWeight_succeeds() = runTest {
        val measurement = repository.create(
            date = "2025-01-15",
            weight = null
        )

        assertTrue(measurement.id > 0)
        assertNull(measurement.weight)
    }

    @Test
    fun getById_existingId_returnsMeasurement() = runTest {
        val created = repository.create(
            date = "2025-01-15",
            weight = 75.5
        )

        val found = repository.getById(created.id)

        assertNotNull(found)
        assertEquals(created.date, found!!.date)
        assertEquals(created.weight!!, found!!.weight!!, 0.001)
    }

    @Test
    fun getById_nonExistentId_returnsNull() = runTest {
        val found = repository.getById(999)
        assertNull(found)
    }

    @Test
    fun getAll_returnsAllMeasurements() = runTest {
        repository.create(date = "2025-01-15", weight = 75.5)
        repository.create(date = "2025-01-16", weight = 76.0)

        val all = repository.getAll().first()

        assertEquals(2, all.size)
    }

    @Test
    fun update_trimsDate() = runTest {
        val created = repository.create(
            date = "  2025-01-15  ",
            weight = 75.5
        )

        val updated = repository.update(created.copy(date = "  2025-01-16  "))

        assertEquals("2025-01-16", updated.date)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_blankDate_throws() = runTest {
        val created = repository.create(
            date = "2025-01-15",
            weight = 75.5
        )

        repository.update(created.copy(date = ""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_invalidWeight_throws() = runTest {
        val created = repository.create(
            date = "2025-01-15",
            weight = 75.5
        )

        repository.update(created.copy(weight = -1.0))
    }

    @Test
    fun delete_removesMeasurement() = runTest {
        val created = repository.create(
            date = "2025-01-15",
            weight = 75.5
        )

        repository.delete(created)

        val afterDelete = repository.getById(created.id)
        assertNull(afterDelete)
    }

    @Test
    fun getLatest_returnsNewestByDate() = runTest {
        repository.create(date = "2025-01-15", weight = 75.5)
        repository.create(date = "2025-01-16", weight = 76.0)
        repository.create(date = "2025-01-14", weight = 74.0)

        val latest = repository.getLatest().first()

        assertNotNull(latest)
        assertEquals("2025-01-16", latest!!.date)
        assertEquals(76.0, latest!!.weight!!, 0.001)
    }

    @Test
    fun getLatest_tieOnSameDate_returnsHighestId() = runTest {
        repository.create(date = "2025-01-15", weight = 75.0)
        val last = repository.create(date = "2025-01-15", weight = 76.0)

        val latest = repository.getLatest().first()

        assertNotNull(latest)
        assertEquals(last.id, latest!!.id)
        assertEquals(76.0, latest!!.weight!!, 0.001)
    }

    @Test
    fun getLatest_returnsNullWhenEmpty() = runTest {
        val latest = repository.getLatest().first()
        assertNull(latest)
    }
}
