package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.math.sqrt

/** Descriptive summary of a dataset. Sample and population measures are always reported separately. */
data class DescriptiveStats(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val modes: List<Double>,
    val minimum: Double,
    val maximum: Double,
    val range: Double,
    val firstQuartile: Double,
    val thirdQuartile: Double,
    val interquartileRange: Double,
    val variancePopulation: Double,
    val varianceSample: Double?,
    val standardDeviationPopulation: Double,
    val standardDeviationSample: Double?,
    val sumOfSquares: Double
)

data class FrequencyEntry(val value: Double, val frequency: Int, val relativeFrequency: Double)

data class GroupedStats(
    val totalFrequency: Int,
    val weightedMean: Double,
    val variancePopulation: Double,
    val varianceSample: Double?,
    val standardDeviationPopulation: Double,
    val standardDeviationSample: Double?
)

data class Regression(
    val count: Int,
    val slope: Double,
    val intercept: Double,
    val correlation: Double,
    val rSquared: Double,
    val slopeStandardError: Double?,
    val interceptStandardError: Double?
)

/**
 * Statistics helpers for the Scientific and Tools tabs.
 *
 * Conventions (documented in docs/mathematical-contracts.md):
 * - quartiles and percentiles use linear interpolation between closest ranks (R type 7);
 * - "variance" values are reported for both population and sample denominators;
 * - a dataset with tied top frequencies returns all modes, a dataset with no repeated value has no mode.
 */
object Statistics {

    fun describe(values: List<Double>): DescriptiveStats {
        validate(values)
        val sorted = values.sorted()
        val count = sorted.size
        val sum = sorted.sum()
        val mean = sum / count
        val median = percentile(sorted, 50.0)
        val q1 = percentile(sorted, 25.0)
        val q3 = percentile(sorted, 75.0)
        val squares = sorted.sumOf { (it - mean) * (it - mean) }
        val variancePopulation = squares / count
        val varianceSample = if (count > 1) squares / (count - 1) else null
        return DescriptiveStats(
            count = count,
            sum = sum,
            mean = mean,
            median = median,
            modes = modes(sorted),
            minimum = sorted.first(),
            maximum = sorted.last(),
            range = sorted.last() - sorted.first(),
            firstQuartile = q1,
            thirdQuartile = q3,
            interquartileRange = q3 - q1,
            variancePopulation = variancePopulation,
            varianceSample = varianceSample,
            standardDeviationPopulation = sqrt(variancePopulation),
            standardDeviationSample = varianceSample?.let { sqrt(it) },
            sumOfSquares = squares
        )
    }

    fun mean(values: List<Double>): Double {
        validate(values)
        return values.sum() / values.size
    }

    fun weightedMean(values: List<Double>, weights: List<Double>): Double {
        if (values.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one value.")
        if (values.size != weights.size) throw MathException.invalid("Each value needs exactly one weight.")
        if (weights.any { it < 0 }) throw MathException.invalid("Weights cannot be negative.")
        val totalWeight = weights.sum()
        if (totalWeight <= 0) throw MathException.invalid("The weights must add up to a positive value.")
        return values.indices.sumOf { values[it] * weights[it] } / totalWeight
    }

    /** Linear-interpolation percentile (R type 7) of an already sorted list. */
    fun percentile(sorted: List<Double>, percent: Double): Double {
        if (sorted.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one value.")
        if (percent !in 0.0..100.0) throw MathException.invalid("The percentile must be between 0 and 100.")
        if (sorted.size == 1) return sorted.first()
        val rank = (percent / 100.0) * (sorted.size - 1)
        val lower = kotlin.math.floor(rank).toInt()
        val upper = kotlin.math.ceil(rank).toInt()
        if (lower == upper) return sorted[lower]
        val fraction = rank - lower
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
    }

    fun modes(sorted: List<Double>): List<Double> {
        if (sorted.isEmpty()) return emptyList()
        val counts = LinkedHashMap<Double, Int>()
        sorted.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        val highest = counts.values.maxOrNull() ?: 0
        if (highest <= 1) return emptyList()
        return counts.filterValues { it == highest }.keys.toList()
    }

    fun frequencyTable(values: List<Double>): List<FrequencyEntry> {
        validate(values)
        val counts = LinkedHashMap<Double, Int>()
        values.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        return counts.entries.sortedBy { it.key }.map {
            FrequencyEntry(it.key, it.value, it.value.toDouble() / values.size)
        }
    }

    /** Grouped-data statistics using class midpoints and frequencies. */
    fun grouped(midpoints: List<Double>, frequencies: List<Double>): GroupedStats {
        if (midpoints.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one class.")
        if (midpoints.size != frequencies.size) throw MathException.invalid("Each class needs a frequency.")
        if (frequencies.any { it < 0 }) throw MathException.invalid("Frequencies cannot be negative.")
        val total = frequencies.sum()
        if (total <= 0) throw MathException.invalid("The frequencies must add up to a positive value.")
        val mean = midpoints.indices.sumOf { midpoints[it] * frequencies[it] } / total
        val squares = midpoints.indices.sumOf { frequencies[it] * (midpoints[it] - mean) * (midpoints[it] - mean) }
        val variancePopulation = squares / total
        val varianceSample = if (total > 1) squares / (total - 1) else null
        return GroupedStats(
            totalFrequency = total.toInt(),
            weightedMean = mean,
            variancePopulation = variancePopulation,
            varianceSample = varianceSample,
            standardDeviationPopulation = sqrt(variancePopulation),
            standardDeviationSample = varianceSample?.let { sqrt(it) }
        )
    }

    fun covariance(x: List<Double>, y: List<Double>, sample: Boolean): Double {
        validatePaired(x, y)
        val meanX = x.sum() / x.size
        val meanY = y.sum() / y.size
        val sum = x.indices.sumOf { (x[it] - meanX) * (y[it] - meanY) }
        val divisor = if (sample) x.size - 1 else x.size
        if (divisor <= 0) throw MathException.invalid("At least two pairs are required for sample covariance.")
        return sum / divisor
    }

    fun correlation(x: List<Double>, y: List<Double>): Double {
        validatePaired(x, y)
        val cov = covariance(x, y, sample = false)
        val sx = sqrt(covariance(x, x, sample = false))
        val sy = sqrt(covariance(y, y, sample = false))
        if (sx == 0.0 || sy == 0.0) throw MathException.domain("Correlation is undefined when a variable has no variation.")
        return (cov / (sx * sy)).coerceIn(-1.0, 1.0)
    }

    fun linearRegression(x: List<Double>, y: List<Double>): Regression {
        validatePaired(x, y)
        if (x.size < 2) throw MathException.invalid("At least two data pairs are required.")
        val meanX = x.sum() / x.size
        val meanY = y.sum() / y.size
        var sxx = 0.0
        var sxy = 0.0
        var syy = 0.0
        for (index in x.indices) {
            val dx = x[index] - meanX
            val dy = y[index] - meanY
            sxx += dx * dx
            sxy += dx * dy
            syy += dy * dy
        }
        if (sxx == 0.0) throw MathException.domain("The x values have no variation, so a slope cannot be estimated.")
        val slope = sxy / sxx
        val intercept = meanY - slope * meanX
        val r = if (syy == 0.0) 0.0 else (sxy / sqrt(sxx * syy)).coerceIn(-1.0, 1.0)
        val slopeError = if (x.size > 2) {
            val residualSum = x.indices.sumOf { val predicted = intercept + slope * x[it]; val d = y[it] - predicted; d * d }
            sqrt((residualSum / (x.size - 2)) / sxx)
        } else null
        val interceptError = slopeError?.let { sqrt(it * it * (1.0 / x.size + meanX * meanX / sxx)) }
        return Regression(x.size, slope, intercept, r, r * r, slopeError, interceptError)
    }

    /** Parses comma, semicolon, tab or whitespace separated numbers. Rejects non-numeric tokens. */
    fun parseDataset(text: String): List<Double> {
        if (text.isBlank()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one number.")
        val tokens = text.split(Regex("[,;\\t\\n\\r ]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one number.")
        if (tokens.size > 10000) throw MathException.limit("Datasets are limited to 10 000 values.")
        return tokens.mapIndexed { index, token ->
            val value = token.toDoubleOrNull()
                ?: throw MathException.invalid("'$token' is not a valid number (item ${index + 1}).")
            if (!value.isFinite()) throw MathException.invalid("'$token' is not a finite number (item ${index + 1}).")
            value
        }
    }

    private fun validate(values: List<Double>) {
        if (values.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one value.")
        if (values.size > 10000) throw MathException.limit("Datasets are limited to 10 000 values.")
        if (values.any { !it.isFinite() }) throw MathException.invalid("Every value must be a finite number.")
    }

    private fun validatePaired(x: List<Double>, y: List<Double>) {
        if (x.isEmpty() || y.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter at least one data pair.")
        if (x.size != y.size) throw MathException.invalid("The two variables must have the same number of values.")
        if (x.size > 10000) throw MathException.limit("Datasets are limited to 10 000 values.")
        if (x.any { !it.isFinite() } || y.any { !it.isFinite() }) throw MathException.invalid("Every value must be a finite number.")
        if (x.size < 2) throw MathException.invalid("At least two data pairs are required.")
    }

    /** Absolute deviation used by the mental-maths estimator display. */
    fun meanAbsoluteDeviation(values: List<Double>): Double {
        validate(values)
        val mean = mean(values)
        return values.sumOf { abs(it - mean) } / values.size
    }
}
