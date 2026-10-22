package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatrixTest {

    private fun matrix(text: String): Matrix = Matrix.parse(text)

    @Test
    fun `addition subtraction and multiplication`() {
        val a = matrix("1, 2; 3, 4")
        val b = matrix("5, 6; 7, 8")
        assertEquals(listOf(listOf(6.0, 8.0), listOf(10.0, 12.0)), (a + b).toRows())
        assertEquals(listOf(listOf(-4.0, -4.0), listOf(-4.0, -4.0)), (a - b).toRows())
        assertEquals(listOf(listOf(19.0, 22.0), listOf(43.0, 50.0)), (a * b).toRows())
        assertEquals(listOf(listOf(1.0, 3.0), listOf(2.0, 4.0)), a.transpose().toRows())
    }

    @Test
    fun `determinant trace and rank`() {
        assertEquals(-2.0, matrix("1, 2; 3, 4").determinant(), 1e-12)
        assertEquals(0.0, matrix("1, 2; 2, 4").determinant(), 1e-12)
        assertEquals(5.0, matrix("1, 2; 3, 4").trace(), 1e-12)
        assertEquals(1, matrix("1, 2; 2, 4").rank())
        assertEquals(2, matrix("1, 2; 3, 4").rank())
        assertEquals(2, matrix("1, 2, 3; 4, 5, 6").rank())
    }

    @Test
    fun `inverse multiplied by the original is the identity`() {
        val samples = listOf(
            "4, 7; 2, 6",
            "2, 1, 0; 1, 3, 1; 0, 1, 2",
            "1, 2; 3, 5"
        )
        for (sample in samples) {
            val a = matrix(sample)
            val product = a * a.inverse()
            for (r in 0 until product.rows) {
                for (c in 0 until product.columns) {
                    val expected = if (r == c) 1.0 else 0.0
                    assertEquals(expected, product[r, c], 1e-9, "inverse check failed for $sample at ($r,$c)")
                }
            }
        }
    }

    @Test
    fun `singular and non-square matrices are reported`() {
        assertEquals(MathErrorCode.SINGULAR, assertFailsWith<MathException> { matrix("1, 2; 2, 4").inverse() }.code)
        assertEquals(MathErrorCode.SINGULAR, assertFailsWith<MathException> { matrix("1, 2; 2, 4").solve(listOf(1.0, 2.0)) }.code)
        assertEquals(MathErrorCode.DIMENSION_MISMATCH, assertFailsWith<MathException> { matrix("1, 2, 3; 4, 5, 6").determinant() }.code)
        assertEquals(MathErrorCode.DIMENSION_MISMATCH, assertFailsWith<MathException> { matrix("1, 2; 3, 4") * matrix("1, 2, 3") }.code)
    }

    @Test
    fun `linear system solution is verified by its residual`() {
        val coefficients = matrix("2, 3; 4, -1")
        val rhs = listOf(12.0, 10.0)
        val solution = coefficients.solve(rhs)
        assertEquals(3.0, solution[0], 1e-12)
        assertEquals(2.0, solution[1], 1e-12)
        assertTrue(coefficients.residual(rhs, solution) < 1e-12)
    }

    @Test
    fun `identity and zero matrices`() {
        val identity = Matrix.identity(3)
        assertEquals(1.0, identity.determinant(), 1e-12)
        assertEquals(3, identity.rank())
        val zero = Matrix.zero(2, 3)
        assertEquals(0, zero.rank())
    }

    @Test
    fun `size and content validation`() {
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> { Matrix.parse("1,2,3,4,5,6,7,8,9,10,11,12,13") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Matrix.parse("1, 2; 3") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Matrix.parse("1, x") }.code)
        assertEquals(MathErrorCode.EMPTY_INPUT, assertFailsWith<MathException> { Matrix.parse("   ") }.code)
        assertEquals(Matrix.MAX_DIMENSION, 12)
    }

    @Test
    fun `vector operations`() {
        assertEquals(5.0, VectorMath.magnitude(listOf(3.0, 4.0)), 1e-12)
        assertEquals(14.0, VectorMath.dot(listOf(1.0, 2.0, 3.0), listOf(1.0, 2.0, 3.0)), 1e-12)
        assertEquals(listOf(-3.0, 6.0, -3.0), VectorMath.cross(listOf(1.0, 2.0, 3.0), listOf(4.0, 5.0, 6.0)))
        assertEquals(listOf(0.6, 0.8), VectorMath.normalize(listOf(3.0, 4.0)))
        assertEquals(90.0, VectorMath.angleBetween(listOf(1.0, 0.0), listOf(0.0, 1.0)), 1e-9)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { VectorMath.normalize(listOf(0.0, 0.0)) }.code)
        assertEquals(MathErrorCode.DIMENSION_MISMATCH, assertFailsWith<MathException> { VectorMath.dot(listOf(1.0), listOf(1.0, 2.0)) }.code)
    }
}

class StatisticsTest {

    private val dataset = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)

    @Test
    fun `descriptive statistics match reference values`() {
        val stats = Statistics.describe(dataset)
        assertEquals(8, stats.count)
        assertEquals(40.0, stats.sum, 1e-12)
        assertEquals(5.0, stats.mean, 1e-12)
        assertEquals(4.5, stats.median, 1e-12)
        assertEquals(listOf(4.0), stats.modes)
        assertEquals(2.0, stats.minimum, 1e-12)
        assertEquals(9.0, stats.maximum, 1e-12)
        assertEquals(7.0, stats.range, 1e-12)
        assertEquals(4.0, stats.firstQuartile, 1e-12)
        assertEquals(5.5, stats.thirdQuartile, 1e-12)
        assertEquals(1.5, stats.interquartileRange, 1e-12)
        assertEquals(4.0, stats.variancePopulation, 1e-12)
        assertEquals(2.0, stats.standardDeviationPopulation, 1e-12)
        assertEquals(4.571428571428571, stats.varianceSample!!, 1e-12)
        assertEquals(2.138089935299395, stats.standardDeviationSample!!, 1e-12)
    }

    @Test
    fun `single value datasets degrade honestly`() {
        val stats = Statistics.describe(listOf(7.0))
        assertEquals(7.0, stats.mean, 1e-12)
        assertEquals(7.0, stats.median, 1e-12)
        assertTrue(stats.varianceSample == null)
        assertTrue(stats.standardDeviationSample == null)
        assertTrue(stats.modes.isEmpty())
    }

    @Test
    fun `multiple modes are all reported`() {
        val stats = Statistics.describe(listOf(1.0, 2.0, 2.0, 3.0, 3.0))
        assertEquals(listOf(2.0, 3.0), stats.modes)
    }

    @Test
    fun `weighted mean uses the weights`() {
        assertEquals(160.0, Statistics.weightedMean(listOf(100.0, 200.0), listOf(2.0, 3.0)), 1e-12)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            Statistics.weightedMean(listOf(1.0, 2.0), listOf(0.0, 0.0))
        }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            Statistics.weightedMean(listOf(1.0, 2.0), listOf(1.0))
        }.code)
    }

    @Test
    fun `frequency table and grouped statistics`() {
        val table = Statistics.frequencyTable(dataset)
        assertEquals(5, table.size)
        assertEquals(3, table.first { it.value == 4.0 }.frequency)
        assertEquals(0.375, table.first { it.value == 4.0 }.relativeFrequency, 1e-12)
        val grouped = Statistics.grouped(listOf(5.0, 15.0, 25.0), listOf(4.0, 6.0, 10.0))
        assertEquals(20, grouped.totalFrequency)
        assertEquals(18.0, grouped.weightedMean, 1e-12)
        assertEquals(61.0, grouped.variancePopulation, 1e-12)
    }

    @Test
    fun `regression and correlation`() {
        val regression = Statistics.linearRegression(listOf(1.0, 2.0, 3.0, 4.0), listOf(2.0, 4.0, 6.0, 8.0))
        assertEquals(2.0, regression.slope, 1e-12)
        assertEquals(0.0, regression.intercept, 1e-12)
        assertEquals(1.0, regression.correlation, 1e-12)
        assertEquals(1.0, regression.rSquared, 1e-12)
        assertEquals(4, regression.count)
        assertEquals(0.0, regression.slopeStandardError!!, 1e-12)
        assertEquals(-1.0, Statistics.correlation(listOf(1.0, 2.0, 3.0), listOf(3.0, 2.0, 1.0)), 1e-12)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> {
            Statistics.correlation(listOf(1.0, 1.0, 1.0), listOf(1.0, 2.0, 3.0))
        }.code)
    }

    @Test
    fun `dataset parsing validates tokens`() {
        assertEquals(listOf(1.0, 2.5, -3.0), Statistics.parseDataset("1, 2.5 -3"))
        assertEquals(MathErrorCode.EMPTY_INPUT, assertFailsWith<MathException> { Statistics.parseDataset("  ") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Statistics.parseDataset("1, two") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Statistics.parseDataset("1, NaN") }.code)
        val many = (1..10001).joinToString(",")
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> { Statistics.parseDataset(many) }.code)
    }

    @Test
    fun `percentile boundaries`() {
        val sorted = listOf(1.0, 2.0, 3.0, 4.0)
        assertEquals(1.0, Statistics.percentile(sorted, 0.0), 1e-12)
        assertEquals(4.0, Statistics.percentile(sorted, 100.0), 1e-12)
        assertEquals(2.5, Statistics.percentile(sorted, 50.0), 1e-12)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Statistics.percentile(sorted, 101.0) }.code)
    }

    @Test
    fun `mean absolute deviation`() {
        assertEquals(1.0, Statistics.meanAbsoluteDeviation(listOf(1.0, 2.0, 3.0, 4.0)), 1e-12)
        assertTrue(abs(Statistics.meanAbsoluteDeviation(dataset) - 1.5) < 1e-12)
    }
}
