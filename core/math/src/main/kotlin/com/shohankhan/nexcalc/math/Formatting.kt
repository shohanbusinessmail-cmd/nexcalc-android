package com.shohankhan.nexcalc.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Display-only formatting policy.
 *
 * Parsing semantics are always English-style (`.` decimal point, `e` exponent). These options
 * affect presentation only, so a user who selects a decimal comma still types `1.5` and never
 * silently changes the meaning of their expression.
 */
data class NumberFormatOptions(
    val decimalPrecision: Int = 10,
    val significantDigits: Int = 10,
    val decimalSeparator: Char = '.',
    val groupingSeparator: Char = ',',
    val useGrouping: Boolean = true,
    val showTrailingZeros: Boolean = false,
    val scientificUpperLimit: Double = 1e12,
    val scientificLowerLimit: Double = 1e-9,
    val roundingMode: RoundingMode = RoundingMode.HALF_UP
) {
    init {
        require(decimalPrecision in 0..15) { "decimalPrecision must be between 0 and 15" }
        require(significantDigits in 1..15) { "significantDigits must be between 1 and 15" }
    }

    companion object {
        val Default = NumberFormatOptions()
        val Engineering = NumberFormatOptions(
            decimalPrecision = 6,
            significantDigits = 6,
            scientificUpperLimit = 1e6,
            scientificLowerLimit = 1e-4
        )
    }
}

/** Formats engine values for display. Never used for parsing. */
object NumberFormatter {

    fun format(value: Double, options: NumberFormatOptions = NumberFormatOptions.Default, exact: Boolean = false): String {
        if (value.isNaN()) return "not a number"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        if (value == 0.0) return "0"

        val magnitude = abs(value)
        if (exact && value == floor(value) && magnitude <= Evaluator.MAX_EXACT_INTEGER && options.decimalPrecision >= 0) {
            return decorate(BigDecimal(value.toLong()).toPlainString(), options)
        }
        if (magnitude >= options.scientificUpperLimit || magnitude < options.scientificLowerLimit) {
            return scientific(value, options)
        }
        val rounded = BigDecimal(value).setScale(options.decimalPrecision, options.roundingMode)
        val stripped = if (options.showTrailingZeros) rounded else rounded.stripTrailingZeros()
        val plain = if (stripped.scale() <= 0) stripped.setScale(0).toPlainString() else stripped.toPlainString()
        return decorate(plain, options)
    }

    fun formatComplex(value: Complex, options: NumberFormatOptions = NumberFormatOptions.Default, exact: Boolean = false): String {
        val realPart = if (abs(value.re) < 1e-300) 0.0 else value.re
        if (value.isReal) return format(realPart, options, exact)
        val imaginaryAbs = abs(value.im)
        val imaginary = if (imaginaryAbs == 1.0) "i" else format(imaginaryAbs, options) + "i"
        if (realPart == 0.0) {
            return if (value.im < 0) "-$imaginary" else imaginary
        }
        val sign = if (value.im < 0) "−" else "+"
        return format(realPart, options, exact) + " " + sign + " " + imaginary
    }

    /** Scientific notation with a normalized mantissa in [1, 10). */
    fun scientific(value: Double, options: NumberFormatOptions): String {
        var exponent = floor(log10(abs(value))).toInt()
        var mantissa = value / 10.0.pow(exponent)
        val decimals = (options.significantDigits - 1).coerceAtLeast(0)
        var mantissaText = BigDecimal(mantissa).setScale(decimals, options.roundingMode).stripTrailingZeros().toPlainString()
        if (mantissaText.toDoubleOrNull()?.let { abs(it) >= 10.0 } == true) {
            exponent += 1
            mantissa /= 10.0
            mantissaText = BigDecimal(mantissa).setScale(decimals, options.roundingMode).stripTrailingZeros().toPlainString()
        }
        val separator = options.decimalSeparator
        val normalized = if (separator == '.') mantissaText else mantissaText.replace('.', separator)
        val exponentText = if (exponent >= 0) "+$exponent" else "$exponent"
        return normalized + "e" + exponentText
    }

    /** Rounds to a fixed number of significant figures without switching notation. */
    fun significant(value: Double, figures: Int, options: NumberFormatOptions = NumberFormatOptions.Default): String {
        if (value == 0.0 || !value.isFinite()) return format(value, options)
        val rounded = BigDecimal(value).round(MathContext(figures.coerceIn(1, 15), options.roundingMode))
        val plain = rounded.stripTrailingZeros().toPlainString()
        return decorate(plain, options)
    }

    fun formatPercent(ratio: Double, options: NumberFormatOptions): String =
        format(ratio * 100.0, options) + "%"

    private fun decorate(plain: String, options: NumberFormatOptions): String {
        val negative = plain.startsWith("-")
        val unsigned = if (negative) plain.substring(1) else plain
        val dot = unsigned.indexOf('.')
        val integerPart = if (dot >= 0) unsigned.substring(0, dot) else unsigned
        val fractionPart = if (dot >= 0) unsigned.substring(dot + 1) else ""
        val grouped = if (options.useGrouping) groupDigits(integerPart, options.groupingSeparator) else integerPart
        val result = StringBuilder()
        if (negative) result.append('-')
        result.append(grouped)
        if (fractionPart.isNotEmpty()) {
            result.append(options.decimalSeparator)
            result.append(if (options.decimalSeparator == '.') fractionPart else fractionPart.replace('.', options.decimalSeparator))
        }
        return result.toString()
    }

    private fun groupDigits(digits: String, separator: Char): String {
        if (digits.length <= 3) return digits
        val builder = StringBuilder()
        var count = 0
        for (index in digits.length - 1 downTo 0) {
            builder.append(digits[index])
            count++
            if (count % 3 == 0 && index != 0) builder.append(separator)
        }
        return builder.reverse().toString()
    }
}

/** Outcome of evaluating a user expression, expressed without exceptions for UI consumption. */
sealed interface CalculationOutcome {
    data class Success(val value: Complex, val exact: Boolean) : CalculationOutcome

    data class Failure(
        val code: MathErrorCode,
        val message: String,
        val hint: String? = null,
        val position: Int? = null
    ) : CalculationOutcome
}

/** High-level entry point used by every calculator screen. */
object CalculatorEngine {

    fun evaluate(
        expression: String,
        angleMode: AngleMode = AngleMode.DEGREES,
        ans: Complex? = null,
        variables: Map<String, Double> = emptyMap()
    ): CalculationOutcome {
        return try {
            val boundVariables = buildMap {
                variables.forEach { (key, value) -> put(key, Complex(value)) }
                if (ans != null) put("ans", ans)
                put("lastanswer", ans ?: Complex.ZERO)
            }
            val evaluator = Evaluator(EvalContext(angleMode = angleMode, variables = boundVariables))
            val result = evaluator.evaluate(expression)
            if (!result.value.isFinite) {
                CalculationOutcome.Failure(MathErrorCode.OVERFLOW, "The result is too large to represent.")
            } else {
                CalculationOutcome.Success(result.value, result.exact)
            }
        } catch (error: MathException) {
            CalculationOutcome.Failure(error.code, error.message, error.hint, error.position)
        } catch (error: StackOverflowError) {
            CalculationOutcome.Failure(MathErrorCode.LIMIT_EXCEEDED, "The expression is too deeply nested.")
        }
    }

    /** Parses and evaluates, propagating structured exceptions to the caller (used by tests/solvers). */
    fun evaluateOrThrow(
        expression: String,
        angleMode: AngleMode = AngleMode.DEGREES,
        variables: Map<String, Double> = emptyMap()
    ): EvalValue {
        val evaluator = Evaluator(
            EvalContext(
                angleMode = angleMode,
                variables = variables.mapValues { Complex(it.value) }
            )
        )
        return evaluator.evaluate(expression)
    }

    /** Builds a real-valued function of one variable for numerical methods. */
    fun functionOf(
        expression: String,
        variable: String,
        angleMode: AngleMode = AngleMode.RADIANS
    ): (Double) -> Double {
        ExpressionParser.parse(expression) // validate early so callers get a syntax error now
        return { x ->
            val evaluator = Evaluator(EvalContext(angleMode = angleMode, variables = mapOf(variable to Complex(x))))
            val value = evaluator.evaluate(expression).value
            if (!value.isReal) throw MathException.domain("f($x) is complex; a real function is required here.")
            value.re
        }
    }
}
