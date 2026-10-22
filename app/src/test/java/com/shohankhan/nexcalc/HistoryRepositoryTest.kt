package com.shohankhan.nexcalc

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.nexcalc.core.data.HistoryRepository
import com.shohankhan.nexcalc.core.data.PreferencesRepository
import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import com.shohankhan.nexcalc.core.data.local.NexCalcDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryTest {

    private lateinit var database: NexCalcDatabase
    private lateinit var preferences: PreferencesRepository
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexCalcDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = PreferencesRepository(context)
        repository = HistoryRepository(database.historyDao(), preferences)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `identical evaluation inside the duplicate window is suppressed`() = runBlocking {
        assertTrue(repository.record("2+2", "4", "STANDARD"))
        assertFalse(repository.record("2+2", "4", "STANDARD"))
        assertEquals(1, repository.count())
    }

    @Test
    fun `different expressions are stored separately`() = runBlocking {
        repository.record("2+2", "4", "STANDARD")
        repository.record("3+3", "6", "STANDARD")
        assertEquals(2, repository.count())
    }

    @Test
    fun `cleanup removes expired rows and is idempotent`() = runBlocking {
        preferences.update { it.copy(historyRetentionDays = 30) }
        val day = 24L * 60L * 60L * 1000L
        val now = System.currentTimeMillis()
        database.historyDao().insert(HistoryRecord(expression = "old", result = "1", source = "STANDARD", createdAt = now - 40 * day))
        database.historyDao().insert(HistoryRecord(expression = "fresh", result = "2", source = "STANDARD", createdAt = now - 1 * day))

        assertEquals(1, repository.cleanupNow())
        assertEquals(0, repository.cleanupNow())
        assertEquals(1, repository.count())
        assertNotNull(repository.observeAll().first().firstOrNull())
    }

    @Test
    fun `never retention keeps expired rows`() = runBlocking {
        preferences.update { it.copy(historyRetentionDays = -1) }
        database.historyDao().insert(
            HistoryRecord(expression = "ancient", result = "1", source = "STANDARD", createdAt = 1L)
        )
        assertEquals(0, repository.cleanupNow())
        assertEquals(1, repository.count())
    }

    @Test
    fun `restore reinserts deleted rows with fresh ids`() = runBlocking {
        repository.record("5*5", "25", "SCIENTIFIC")
        val stored = repository.observeAll().first()
        assertEquals(1, stored.size)
        repository.delete(listOf(stored.first().id))
        assertEquals(0, repository.count())
        repository.restore(stored)
        assertEquals(1, repository.count())
    }

    @Test
    fun `search filters by expression text and source`() = runBlocking {
        repository.record("sin(30)", "0.5", "SCIENTIFIC")
        repository.record("2+2", "4", "STANDARD")
        val scientific = repository.observeFiltered("", "SCIENTIFIC").first()
        assertEquals(1, scientific.size)
        assertEquals("sin(30)", scientific.first().expression)
        val byText = repository.observeFiltered("2+2", "ALL").first()
        assertEquals(1, byText.size)
    }
}
