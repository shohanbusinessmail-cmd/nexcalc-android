package com.shohankhan.nexcalc

import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import com.shohankhan.nexcalc.feature.history.HistoryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class HistoryGroupingTest {

    private fun record(id: Long, day: LocalDate): HistoryRecord = HistoryRecord(
        id = id,
        expression = "expression $id",
        result = "result $id",
        source = "STANDARD",
        createdAt = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    @Test
    fun `records are grouped by local day in descending order`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val groups = HistoryViewModel.group(
            listOf(record(1, today), record(2, yesterday), record(3, today))
        )
        assertEquals(2, groups.size)
        assertEquals(today.toEpochDay(), groups.first().epochDay)
        assertEquals(2, groups.first().records.size)
        assertTrue(groups[1].epochDay < groups[0].epochDay)
    }

    @Test
    fun `empty history has no groups`() {
        assertTrue(HistoryViewModel.group(emptyList()).isEmpty())
    }

    @Test
    fun `grouping uses the local zone not UTC`() {
        val instant = Instant.now()
        val expected = instant.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val groups = HistoryViewModel.group(
            listOf(HistoryRecord(id = 9, expression = "e", result = "r", source = "STANDARD", createdAt = instant.toEpochMilli()))
        )
        assertEquals(expected, groups.single().epochDay)
    }
}
