package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.math.floor

/** Top-level grouping shown in the Formulas tab. */
enum class FormulaCategory(val label: String, val description: String) {
    PERCENTAGE("Percentage & ratio", "Percentages, ratios and proportions."),
    BUSINESS("Business & finance", "Cost, profit, interest and business arithmetic."),
    EVERYDAY("Work, time & everyday", "Rates, speed, mixtures and everyday problems."),
    GEOMETRY("Geometry & mensuration", "Plane and solid figures."),
    PHYSICS("Physics & engineering", "Mechanics, electricity and thermal formulas."),
    ACADEMIC("Academic mathematics", "Sequences, probability and algebra helpers."),
    BANGLADESH("Bangladesh essentials", "Marks, land area, construction and utility estimates.")
}

/**
 * Declarative description of one formula input.
 *
 * Validation lives in the definition so that the UI, tests and any future surface agree on the
 * accepted domain instead of re-implementing checks per screen.
 */
data class FormulaInput(
    val id: String,
    val label: String,
    val symbol: String = "",
    val unit: String = "",
    val positive: Boolean = false,
    val nonNegative: Boolean = false,
    val integer: Boolean = false,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val defaultValue: String = "",
    val help: String = ""
)

/** One computed output value. Exactly one output should be marked primary where it makes sense. */
data class FormulaValue(
    val label: String,
    val value: Double,
    val unit: String = "",
    val primary: Boolean = false
)

data class FormulaResult(
    val values: List<FormulaValue>,
    val steps: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/** A published, independently checked example. Every formula must provide at least one. */
data class FormulaTestCase(
    val inputs: Map<String, Double>,
    val expected: Map<String, Double>,
    val tolerance: Double = 1e-6
)

/**
 * A published formula.
 *
 * Fields required by the product specification (notation, assumptions, units, validation,
 * reference and tests) are all present, so the UI never needs hand-written copies of a formula.
 */
data class Formula(
    val id: String,
    val name: String,
    val category: FormulaCategory,
    val subcategory: String,
    val description: String,
    val notation: String,
    val assumptions: List<String>,
    val inputs: List<FormulaInput>,
    val outputLabels: List<Pair<String, String>>,
    val compute: (Map<String, Double>) -> FormulaResult,
    val tests: List<FormulaTestCase>,
    val related: List<String> = emptyList(),
    val reference: String = "",
    val precisionNote: String = "IEEE-754 double precision, rounded for display according to your number settings."
)

/** Validation of raw text input against a formula definition. */
object FormulaValidator {

    fun validate(formula: Formula, raw: Map<String, String>): Map<String, String> {
        val errors = LinkedHashMap<String, String>()
        for (input in formula.inputs) {
            val text = raw[input.id]?.trim().orEmpty()
            if (text.isEmpty()) {
                errors[input.id] = "${input.label} is required."
                continue
            }
            val value = text.toDoubleOrNull()
            if (value == null) {
                errors[input.id] = "Enter a number without units or symbols."
                continue
            }
            if (!value.isFinite()) {
                errors[input.id] = "Enter a finite number."
                continue
            }
            if (input.positive && value <= 0) {
                errors[input.id] = "${input.label} must be greater than zero."
                continue
            }
            if (input.nonNegative && value < 0) {
                errors[input.id] = "${input.label} cannot be negative."
                continue
            }
            if (input.integer && value != floor(value)) {
                errors[input.id] = "${input.label} must be a whole number."
                continue
            }
            if (input.minimum != null && value < input.minimum) {
                errors[input.id] = "${input.label} must be at least ${formatNumber(input.minimum)}."
                continue
            }
            if (input.maximum != null && value > input.maximum) {
                errors[input.id] = "${input.label} must be at most ${formatNumber(input.maximum)}."
            }
        }
        return errors
    }

    fun values(formula: Formula, raw: Map<String, String>): Map<String, Double> {
        val values = LinkedHashMap<String, Double>()
        for (input in formula.inputs) {
            val value = raw[input.id]?.trim()?.toDoubleOrNull() ?: continue
            values[input.id] = value
        }
        return values
    }

    private fun formatNumber(value: Double): String =
        NumberFormatter.format(value, NumberFormatOptions(decimalPrecision = 6, useGrouping = false))
}

sealed interface FormulaEvaluation {
    data class Success(val result: FormulaResult) : FormulaEvaluation
    data class Invalid(val errors: Map<String, String>) : FormulaEvaluation
    data class Failed(val message: String, val hint: String? = null) : FormulaEvaluation
}

/** Entry point used by the Formulas UI and by the verification tests. */
object FormulaEngine {

    fun calculate(formula: Formula, rawInputs: Map<String, String>): FormulaEvaluation {
        val errors = FormulaValidator.validate(formula, rawInputs)
        if (errors.isNotEmpty()) return FormulaEvaluation.Invalid(errors)
        val values = FormulaValidator.values(formula, rawInputs)
        return try {
            val result = formula.compute(values)
            val invalid = result.values.firstOrNull { !it.value.isFinite() }
            if (invalid != null) {
                FormulaEvaluation.Failed("${invalid.label} is outside the representable range.")
            } else {
                FormulaEvaluation.Success(result)
            }
        } catch (error: MathException) {
            FormulaEvaluation.Failed(error.message, error.hint)
        } catch (error: ArithmeticException) {
            FormulaEvaluation.Failed("The calculation is not defined for these inputs.")
        }
    }

    /** Used by tests: returns the result or fails loudly. */
    fun calculateOrThrow(formula: Formula, inputs: Map<String, Double>): FormulaResult {
        val raw = inputs.mapValues { it.value.toString() }
        return when (val evaluation = calculate(formula, raw)) {
            is FormulaEvaluation.Success -> evaluation.result
            is FormulaEvaluation.Invalid -> throw MathException.invalid("Validation failed: ${evaluation.errors}")
            is FormulaEvaluation.Failed -> throw MathException.invalid(evaluation.message)
        }
    }
}

/** Access to the complete offline catalogue. */
object FormulaRegistry {

    val all: List<Formula> = (
        PercentageFormulas.all +
            BusinessFormulas.all +
            EverydayFormulas.all +
            GeometryFormulas.all +
            PhysicsFormulas.all +
            AcademicFormulas.all +
            BangladeshFormulas.all
        ).sortedWith(compareBy<Formula> { it.category.ordinal }.thenBy { it.name })

    private val byId: Map<String, Formula> = all.associateBy { it.id }

    fun find(id: String): Formula? = byId[id]

    fun byCategory(category: FormulaCategory): List<Formula> = all.filter { it.category == category }

    fun subcategories(category: FormulaCategory): List<String> =
        byCategory(category).map { it.subcategory }.distinct().sorted()

    val categories: List<FormulaCategory> = FormulaCategory.entries.filter { category ->
        all.any { it.category == category }
    }

    val count: Int get() = all.size

    /** Case-insensitive search over name, notation, description and id. */
    fun search(query: String): List<Formula> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        val needle = trimmed.lowercase()
        return all.filter { formula ->
            formula.name.lowercase().contains(needle) ||
                formula.id.lowercase().contains(needle) ||
                formula.notation.lowercase().contains(needle) ||
                formula.description.lowercase().contains(needle) ||
                formula.subcategory.lowercase().contains(needle)
        }
    }
}

/**
 * Shared helpers for formula definitions.
 *
 * These are intentionally tiny and pure so that a formula definition stays declarative and every
 * numeric constant in the catalogue is visible at the place it is used.
 */
internal object FormulaSupport {

    fun input(
        id: String,
        label: String,
        unit: String = "",
        symbol: String = "",
        positive: Boolean = false,
        nonNegative: Boolean = false,
        integer: Boolean = false,
        minimum: Double? = null,
        maximum: Double? = null,
        default: String = "",
        help: String = ""
    ): FormulaInput = FormulaInput(
        id = id,
        label = label,
        symbol = symbol,
        unit = unit,
        positive = positive,
        nonNegative = nonNegative,
        integer = integer,
        minimum = minimum,
        maximum = maximum,
        defaultValue = default,
        help = help
    )

    fun value(label: String, amount: Double, unit: String = "", primary: Boolean = false): FormulaValue =
        FormulaValue(label, amount, unit, primary)

    fun result(vararg values: FormulaValue, steps: List<String> = emptyList(), warnings: List<String> = emptyList()): FormulaResult =
        FormulaResult(values.toList(), steps, warnings)

    fun format(value: Double, precision: Int = 6): String =
        NumberFormatter.format(value, NumberFormatOptions(decimalPrecision = precision, useGrouping = false))

    fun requirePositive(value: Double, label: String): Double {
        if (!(value > 0)) throw MathException.invalid("$label must be greater than zero.")
        return value
    }

    fun requireNonNegative(value: Double, label: String): Double {
        if (value < 0) throw MathException.invalid("$label cannot be negative.")
        return value
    }

    fun testCase(inputs: Map<String, Double>, vararg expected: Pair<String, Double>): FormulaTestCase =
        FormulaTestCase(inputs, expected.toMap())

    fun close(a: Double, b: Double, tolerance: Double = 1e-9): Boolean = abs(a - b) <= tolerance * maxOf(1.0, abs(b))
}
