package com.shohankhan.nexcalc

import com.shohankhan.nexcalc.core.search.SearchGroup
import com.shohankhan.nexcalc.core.search.SearchIndex
import com.shohankhan.nexcalc.core.search.SearchTarget
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexTest {

    private val index = SearchIndex()

    @Test
    fun `blank query returns nothing`() {
        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("   ").isEmpty())
    }

    @Test
    fun `exact formula name ranks first`() {
        val hits = index.search("Heron")
        assertTrue(hits.isNotEmpty())
        assertEquals(SearchGroup.FORMULA, hits.first().group)
        assertTrue(hits.first().target is SearchTarget.Formula)
    }

    @Test
    fun `quick solve tools are searchable by name`() {
        val hits = index.search("prime factorization")
        assertTrue(hits.any { it.group == SearchGroup.TOOL })
    }

    @Test
    fun `unit categories expose a converter target`() {
        val hits = index.search("length")
        val unitHit = hits.firstOrNull { it.group == SearchGroup.UNIT && it.target is SearchTarget.UnitCategory }
        assertTrue(unitHit != null)
    }

    @Test
    fun `constants link to the constants reference`() {
        val hits = index.search("speed of light")
        assertTrue(hits.any { it.group == SearchGroup.CONSTANT })
    }

    @Test
    fun `every result is ranked by descending score`() {
        val hits = index.search("area")
        assertEquals(hits.sortedByDescending { it.score }, hits)
    }

    @Test
    fun `source labels are human readable`() {
        assertEquals("Standard", HistorySource.label(HistorySource.STANDARD))
        assertEquals("Formula", HistorySource.label(HistorySource.formula("heron")))
        assertEquals("Quick Solve", HistorySource.label(HistorySource.tool("gcd-lcm")))
    }
}
