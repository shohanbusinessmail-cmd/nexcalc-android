package com.shohankhan.nexcalc.math

import java.math.BigDecimal
import java.math.BigInteger

/** Grouping used by the Quick Solve tab. */
enum class ToolCategory(val label: String) {
    NUMBER_THEORY("Number theory"),
    FRACTIONS("Fractions"),
    MENTAL("Mental maths"),
    SEQUENCES("Sequences"),
    SOLVERS("Solvers")
}

/** Input descriptor for a Quick Solve tool. All raw values are text so fractions can be entered. */
data class ToolInput(
    val id: String,
    val label: String,
    val placeholder: String = "",
    val defaultValue: String = "",
    val help: String = ""
)

data class ToolOutput(val label: String, val value: String)

data class ToolResult(
    val headline: String,
    val outputs: List<ToolOutput>,
    val steps: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ToolTest(val inputs: Map<String, String>, val expected: Map<String, String>)

/**
 * A Quick Solve tool: declarative metadata, a pure solver and verified examples.
 * Every tool in the registry is exercised by QuickToolRegistryTest with its own examples.
 */
data class QuickTool(
    val id: String,
    val name: String,
    val category: ToolCategory,
    val description: String,
    val inputs: List<ToolInput>,
    val compute: (Map<String, String>) -> ToolResult,
    val tests: List<ToolTest>
)

/** Shared parsing and formatting helpers for tool implementations. */
internal object ToolSupport {

    val formatOptions = NumberFormatOptions(decimalPrecision = 12, useGrouping = false)

    fun fmt(value: Double): String = NumberFormatter.format(value, formatOptions)

    fun fmtInteger(value: BigInteger): String = value.toString()

    fun fmtLong(value: Long): String = value.toString()

    fun text(inputs: Map<String, String>, id: String): String {
        val value = inputs[id]?.trim().orEmpty()
        if (value.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter a value for this field.")
        return value
    }

    fun double(inputs: Map<String, String>, id: String): Double {
        val raw = text(inputs, id)
        val value = raw.toDoubleOrNull() ?: throw MathException.invalid("'$raw' is not a valid number.")
        if (!value.isFinite()) throw MathException.invalid("'$raw' is not a finite number.")
        return value
    }

    fun long(inputs: Map<String, String>, id: String): Long {
        val raw = text(inputs, id)
        val value = raw.toLongOrNull() ?: throw MathException.invalid("'$raw' must be a whole number.")
        return value
    }

    fun boundLong(inputs: Map<String, String>, id: String, limit: Long = NumberTheory.FACTORIZATION_LIMIT): Long {
        val value = long(inputs, id)
        if (kotlin.math.abs(value) > limit) throw MathException.limit("Values are limited to $limit in magnitude.")
        return value
    }

    fun big(inputs: Map<String, String>, id: String, limit: Long = NumberTheory.FACTORIZATION_LIMIT): BigInteger {
        val raw = text(inputs, id).replace(",", "").replace(" ", "")
        val value = try {
            BigInteger(raw)
        } catch (error: NumberFormatException) {
            throw MathException.invalid("'$raw' must be a whole number.")
        }
        if (value.abs() > BigInteger.valueOf(limit)) throw MathException.limit("Values are limited to $limit in magnitude.")
        return value
    }

    fun positiveInt(inputs: Map<String, String>, id: String, maximum: Int = 1_000_000): Int {
        val value = long(inputs, id)
        if (value < 1) throw MathException.invalid("Enter a whole number of at least 1.")
        if (value > maximum) throw MathException.limit("Values are limited to $maximum.")
        return value.toInt()
    }

    fun rational(inputs: Map<String, String>, id: String): Rational {
        val raw = text(inputs, id)
        return try {
            Rational.parse(raw)
        } catch (error: MathException) {
            throw MathException.invalid("'$raw' is not a valid fraction.")
        }
    }

    fun result(headline: String, vararg outputs: Pair<String, String>, steps: List<String> = emptyList(), warnings: List<String> = emptyList()): ToolResult =
        ToolResult(headline, outputs.map { ToolOutput(it.first, it.second) }, steps, warnings)

    fun decimal(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        return if (stripped.scale() <= 0) stripped.setScale(0).toPlainString() else stripped.toPlainString()
    }
}

/** The complete Quick Solve catalogue. */
object QuickToolRegistry {

    val all: List<QuickTool> =
        (QuickNumberTools.all + QuickFractionTools.all + QuickMentalTools.all + QuickSequenceTools.all + QuickSolverTools.all)
            .sortedWith(compareBy<QuickTool> { it.category.ordinal }.thenBy { it.name })

    private val byId: Map<String, QuickTool> = all.associateBy { it.id }

    fun find(id: String): QuickTool? = byId[id]

    fun byCategory(category: ToolCategory): List<QuickTool> = all.filter { it.category == category }

    val categories: List<ToolCategory> = ToolCategory.entries.filter { category -> all.any { it.category == category } }

    val count: Int get() = all.size

    fun search(query: String): List<QuickTool> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return all
        return all.filter {
            it.name.lowercase().contains(needle) ||
                it.id.lowercase().contains(needle) ||
                it.description.lowercase().contains(needle)
        }
    }
}
