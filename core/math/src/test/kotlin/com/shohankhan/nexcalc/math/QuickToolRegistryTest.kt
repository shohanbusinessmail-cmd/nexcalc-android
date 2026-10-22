package com.shohankhan.nexcalc.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Verification of every Quick Solve tool and its published examples. */
class QuickToolRegistryTest {

    @Test
    fun `registry is complete and every category is populated`() {
        assertTrue(QuickToolRegistry.count >= 50, "only ${QuickToolRegistry.count} tools")
        assertEquals(QuickToolRegistry.count, QuickToolRegistry.all.map { it.id }.toSet().size, "duplicate tool ids")
        assertEquals(ToolCategory.entries.toSet(), QuickToolRegistry.categories.toSet())
        for (tool in QuickToolRegistry.all) {
            assertTrue(tool.name.isNotBlank(), "blank name for ${tool.id}")
            assertTrue(tool.description.isNotBlank(), "blank description for ${tool.id}")
            assertTrue(tool.inputs.isNotEmpty(), "no inputs for ${tool.id}")
            assertTrue(tool.tests.isNotEmpty(), "no tests for ${tool.id}")
            assertEquals(tool.inputs.size, tool.inputs.map { it.id }.toSet().size, "duplicate inputs in ${tool.id}")
        }
        for (category in ToolCategory.entries) {
            assertTrue(QuickToolRegistry.byCategory(category).isNotEmpty(), "empty category $category")
        }
    }

    @Test
    fun `every published example matches its expected output exactly`() {
        val failures = mutableListOf<String>()
        for (tool in QuickToolRegistry.all) {
            tool.tests.forEachIndexed { index, test ->
                try {
                    val result = tool.compute(test.inputs)
                    val outputs = result.outputs.associate { it.label to it.value }
                    for ((label, expected) in test.expected) {
                        val actual = outputs[label]
                        if (actual == null) {
                            failures += "${tool.id} case $index: missing output '$label' (present: ${outputs.keys})"
                        } else if (actual != expected) {
                            failures += "${tool.id} case $index: '$label' was '$actual', expected '$expected'"
                        }
                    }
                } catch (error: Exception) {
                    failures += "${tool.id} case $index threw ${error::class.simpleName}: ${error.message}"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `input errors are structured`() {
        val gcd = QuickToolRegistry.find("gcd-lcm")!!
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            gcd.compute(mapOf("a" to "abc", "b" to "12"))
        }.code)
        assertEquals(MathErrorCode.EMPTY_INPUT, assertFailsWith<MathException> {
            gcd.compute(mapOf("a" to "", "b" to "12"))
        }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> {
            QuickToolRegistry.find("prime-factorization")!!.compute(mapOf("n" to "1"))
        }.code)
        assertEquals(MathErrorCode.DIVIDE_BY_ZERO, assertFailsWith<MathException> {
            QuickToolRegistry.find("modular-arithmetic")!!.compute(mapOf("a" to "5", "modulus" to "0"))
        }.code)
    }

    @Test
    fun `shortcut tools refuse inputs outside their valid domain`() {
        val square = QuickToolRegistry.find("square-ending-five")!!
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            square.compute(mapOf("n" to "34"))
        }.code)
        val nearBase = QuickToolRegistry.find("near-base-multiplication")!!
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            nearBase.compute(mapOf("a" to "300", "b" to "98"))
        }.code)
        val eleven = QuickToolRegistry.find("multiply-by-eleven")!!
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            eleven.compute(mapOf("n" to "123"))
        }.code)
    }

    @Test
    fun `sequence ambiguity is never hidden`() {
        val next = QuickToolRegistry.find("sequence-next-terms")!!
        val result = next.compute(mapOf("t1" to "2", "t2" to "6", "t3" to "18"))
        assertTrue(result.warnings.isNotEmpty())
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> {
            next.compute(mapOf("t1" to "1", "t2" to "2", "t3" to "4"))
        }.code)
    }

    @Test
    fun `solvers validate their inputs`() {
        val travel = QuickToolRegistry.find("travel-solver")!!
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            travel.compute(mapOf("distance" to "100", "speed" to "10", "time" to "10"))
        }.code)
        val simultaneous = QuickToolRegistry.find("simultaneous-2x2")!!
        assertEquals(MathErrorCode.SINGULAR, assertFailsWith<MathException> {
            simultaneous.compute(mapOf("a1" to "1", "b1" to "1", "c1" to "2", "a2" to "2", "b2" to "2", "c2" to "4"))
        }.code)
        val dates = QuickToolRegistry.find("date-difference")!!
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            dates.compute(mapOf("start" to "2024-13-01", "end" to "2024-01-01"))
        }.code)
    }

    @Test
    fun `search finds tools by name and description`() {
        assertTrue(QuickToolRegistry.search("gcd").any { it.id == "gcd-lcm" })
        assertTrue(QuickToolRegistry.search("fraction").all { it.category == ToolCategory.FRACTIONS })
        assertTrue(QuickToolRegistry.search("definitely-not-a-tool").isEmpty())
        assertEquals(QuickToolRegistry.count, QuickToolRegistry.search(" ").size)
    }
}
