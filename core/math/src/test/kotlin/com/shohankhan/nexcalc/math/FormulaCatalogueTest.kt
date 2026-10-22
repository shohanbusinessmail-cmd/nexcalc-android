package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Verification of every published formula.
 *
 * The test walks the registry, so a formula cannot be added or changed without its examples being
 * executed. Expected values were derived independently from the mathematical definitions.
 */
class FormulaCatalogueTest {

    @Test
    fun `the catalogue covers every required category`() {
        val counts = FormulaRegistry.all.groupingBy { it.category }.eachCount()
        assertTrue(FormulaRegistry.count >= 120, "only ${FormulaRegistry.count} formulas")
        assertTrue((counts[FormulaCategory.PERCENTAGE] ?: 0) >= 12, "percentage formulas: ${counts[FormulaCategory.PERCENTAGE]}")
        assertTrue((counts[FormulaCategory.BUSINESS] ?: 0) >= 20, "business formulas: ${counts[FormulaCategory.BUSINESS]}")
        assertTrue((counts[FormulaCategory.EVERYDAY] ?: 0) >= 15, "everyday formulas: ${counts[FormulaCategory.EVERYDAY]}")
        assertTrue((counts[FormulaCategory.GEOMETRY] ?: 0) >= 25, "geometry formulas: ${counts[FormulaCategory.GEOMETRY]}")
        assertTrue((counts[FormulaCategory.PHYSICS] ?: 0) >= 25, "physics formulas: ${counts[FormulaCategory.PHYSICS]}")
        assertTrue((counts[FormulaCategory.ACADEMIC] ?: 0) >= 15, "academic formulas: ${counts[FormulaCategory.ACADEMIC]}")
        assertTrue((counts[FormulaCategory.BANGLADESH] ?: 0) >= 12, "Bangladesh formulas: ${counts[FormulaCategory.BANGLADESH]}")
        assertEquals(FormulaCategory.entries.toSet(), FormulaRegistry.categories.toSet())
    }

    @Test
    fun `every formula has complete metadata and unique ids`() {
        val ids = FormulaRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate formula ids")
        val problems = mutableListOf<String>()
        for (formula in FormulaRegistry.all) {
            if (formula.id.isBlank()) problems += "blank id"
            if (formula.name.isBlank()) problems += "${formula.id}: blank name"
            if (formula.description.isBlank()) problems += "${formula.id}: blank description"
            if (formula.notation.isBlank()) problems += "${formula.id}: blank notation"
            if (formula.assumptions.isEmpty()) problems += "${formula.id}: no assumptions"
            if (formula.inputs.isEmpty()) problems += "${formula.id}: no inputs"
            if (formula.outputLabels.isEmpty()) problems += "${formula.id}: no outputs"
            if (formula.tests.isEmpty()) problems += "${formula.id}: no test cases"
            if (formula.inputs.map { it.id }.toSet().size != formula.inputs.size) problems += "${formula.id}: duplicate input ids"
            if (formula.outputLabels.map { it.first }.toSet().size != formula.outputLabels.size) problems += "${formula.id}: duplicate output labels"
            for (related in formula.related) {
                if (FormulaRegistry.find(related) == null) problems += "${formula.id}: unknown related id $related"
            }
            for (case in formula.tests) {
                for (input in formula.inputs) {
                    if (!case.inputs.containsKey(input.id)) problems += "${formula.id}: test case missing input ${input.id}"
                }
                for (label in case.expected.keys) {
                    if (formula.outputLabels.none { it.first == label }) problems += "${formula.id}: test expects unknown output '$label'"
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `every published test case produces its expected value`() {
        val failures = mutableListOf<String>()
        for (formula in FormulaRegistry.all) {
            formula.tests.forEachIndexed { index, case ->
                try {
                    val result = FormulaEngine.calculateOrThrow(formula, case.inputs)
                    val byLabel = result.values.associateBy { it.label }
                    for ((label, expected) in case.expected) {
                        val actual = byLabel[label]
                        if (actual == null) {
                            failures += "${formula.id} case $index: missing output '$label'"
                            continue
                        }
                        val tolerance = if (case.tolerance > 0) case.tolerance * maxOf(1.0, abs(expected)) else 1e-9
                        if (abs(actual.value - expected) > tolerance) {
                            failures += "${formula.id} case $index: '$label' was ${actual.value}, expected $expected"
                        }
                    }
                } catch (error: MathException) {
                    failures += "${formula.id} case $index threw ${error.code}: ${error.message}"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `reference values from independent calculations`() {
        fun value(id: String, inputs: Map<String, Double>, label: String): Double {
            val formula = FormulaRegistry.find(id) ?: error("missing formula $id")
            val result = FormulaEngine.calculateOrThrow(formula, inputs)
            return result.values.first { it.label == label }.value
        }
        assertEquals(30.0, value("percent-of", mapOf("percent" to 15.0, "value" to 200.0), "Result"), 1e-12)
        assertEquals(200.0, value("reverse-percentage", mapOf("final" to 230.0, "percent" to 15.0), "Original value"), 1e-12)
        assertEquals(8884.87886783416, value("installment", mapOf("principal" to 100000.0, "rate" to 12.0, "months" to 12.0), "Monthly installment"), 1e-8)
        assertEquals(6.0, value("triangle-heron", mapOf("a" to 3.0, "b" to 4.0, "c" to 5.0), "Area"), 1e-12)
        assertEquals(113.09733552923254, value("sphere", mapOf("radius" to 3.0), "Volume"), 1e-9)
        assertEquals(686.4655, value("weight", mapOf("mass" to 70.0), "Weight"), 1e-9)
        assertEquals(3600.0, value("land-katha", mapOf("katha" to 5.0), "Square feet"), 1e-9)
        assertEquals(1286.25, value("electricity-cost", mapOf("units" to 150.0, "rate" to 7.5, "fixed" to 100.0, "duty" to 5.0), "Total bill"), 1e-9)
        assertEquals(20.0, value("attendance-required", mapOf("total" to 100.0, "attended" to 70.0, "target" to 75.0), "Classes needed"), 1e-9)
        assertEquals(4.3, value("gpa", mapOf("g1" to 5.0, "g2" to 4.0, "g3" to 4.0, "g4" to 3.5, "g5" to 5.0), "GPA"), 1e-12)
    }

    @Test
    fun `validation rejects missing non-numeric and out-of-range input`() {
        val positive = FormulaRegistry.find("circle")!!
        val missing = FormulaEngine.calculate(positive, emptyMap())
        assertTrue(missing is FormulaEvaluation.Invalid)
        assertTrue((missing as FormulaEvaluation.Invalid).errors.containsKey("radius"))

        val nonNumeric = FormulaEngine.calculate(positive, mapOf("radius" to "five"))
        assertTrue(nonNumeric is FormulaEvaluation.Invalid)

        val zeroRadius = FormulaEngine.calculate(positive, mapOf("radius" to "0"))
        assertTrue(zeroRadius is FormulaEvaluation.Invalid)

        val negativeSides = FormulaEngine.calculate(FormulaRegistry.find("triangle-heron")!!, mapOf("a" to "1", "b" to "2", "c" to "10"))
        assertTrue(negativeSides is FormulaEvaluation.Failed)
    }

    @Test
    fun `domain failures are reported instead of returning a number`() {
        val heron = FormulaRegistry.find("triangle-heron")!!
        val evaluation = FormulaEngine.calculate(heron, mapOf("a" to "1", "b" to "2", "c" to "10"))
        assertTrue(evaluation is FormulaEvaluation.Failed)

        val parallel = FormulaRegistry.find("parallel-resistance")!!
        val zero = FormulaEngine.calculate(parallel, mapOf("r1" to "0", "r2" to "3"))
        assertTrue(zero is FormulaEvaluation.Invalid)

        val breakEven = FormulaRegistry.find("break-even-quantity")!!
        val impossible = FormulaEngine.calculate(breakEven, mapOf("fixed" to "100", "price" to "10", "variable" to "20"))
        assertTrue(impossible is FormulaEvaluation.Failed)
    }

    @Test
    fun `search is case insensitive and covers notation`() {
        assertTrue(FormulaRegistry.search("heron").any { it.id == "triangle-heron" })
        assertTrue(FormulaRegistry.search("PYTHAGORAS").any { it.id == "pythagoras" })
        assertTrue(FormulaRegistry.search("π").any { it.category == FormulaCategory.GEOMETRY })
        assertTrue(FormulaRegistry.search("qwertyuiop").isEmpty())
        assertEquals(FormulaRegistry.all.size, FormulaRegistry.search("   ").size)
    }

    @Test
    fun `subcategories are available for navigation`() {
        for (category in FormulaRegistry.categories) {
            assertTrue(FormulaRegistry.subcategories(category).isNotEmpty(), "no subcategories for ${category.name}")
            assertTrue(FormulaRegistry.byCategory(category).isNotEmpty())
        }
        assertFailsWith<IllegalArgumentException> { FormulaRegistry.byCategory(FormulaCategory.GEOMETRY).first().compute(emptyMap()) }
    }
}
