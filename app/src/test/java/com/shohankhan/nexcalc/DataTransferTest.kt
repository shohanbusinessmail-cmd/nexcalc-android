package com.shohankhan.nexcalc

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.nexcalc.core.data.DataTransfer
import com.shohankhan.nexcalc.core.data.ImportOutcome
import com.shohankhan.nexcalc.core.data.PreferencesRepository
import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import com.shohankhan.nexcalc.core.data.local.NexCalcDatabase
import com.shohankhan.nexcalc.core.data.local.SavedRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataTransferTest {

    private lateinit var source: NexCalcDatabase
    private lateinit var target: NexCalcDatabase
    private lateinit var preferences: PreferencesRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        source = Room.inMemoryDatabaseBuilder(context, NexCalcDatabase::class.java).allowMainThreadQueries().build()
        target = Room.inMemoryDatabaseBuilder(context, NexCalcDatabase::class.java).allowMainThreadQueries().build()
        preferences = PreferencesRepository(context)
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun `export and import move history saved work and favorites`() = runBlocking {
        source.historyDao().insert(
            HistoryRecord(expression = "2+3*4", result = "14", source = "STANDARD", createdAt = 1_700_000_000_000)
        )
        source.savedDao().insert(
            SavedRecord(kind = "EXPRESSION", title = "Quadratic", payload = "x^2-5x+6", createdAt = 1_700_000_000_000)
        )
        source.favoriteDao().upsert(
            com.shohankhan.nexcalc.core.data.local.FavoriteRecord(
                key = "FORMULA:triangle-heron",
                kind = "FORMULA",
                label = "Triangle area (Heron)",
                subtitle = "Geometry",
                createdAt = 1_700_000_000_000
            )
        )

        val json = DataTransfer(source, preferences).exportJson()
        assertTrue(json.contains("NEXCALC"))

        val outcome = DataTransfer(target, preferences).importJson(json)
        assertTrue(outcome is ImportOutcome.Success)
        assertEquals(1, target.historyDao().count())
        assertEquals(1, target.savedDao().count())
        assertEquals(1, target.favoriteDao().all().size)
    }

    @Test
    fun `malformed json is rejected without writing`() = runBlocking {
        val outcome = DataTransfer(target, preferences).importJson("this is not json")
        assertTrue(outcome is ImportOutcome.Failure)
        assertEquals(0, target.historyDao().count())
    }

    @Test
    fun `foreign export bundle is rejected`() = runBlocking {
        val outcome = DataTransfer(target, preferences).importJson("""{"app":"OTHER","formatVersion":1}""")
        assertTrue(outcome is ImportOutcome.Failure)
    }
}
