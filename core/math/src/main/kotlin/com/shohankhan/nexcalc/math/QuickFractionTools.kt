package com.shohankhan.nexcalc.math

import java.math.BigDecimal
import java.math.BigInteger

/** Exact fraction tools backed by [Rational]. */
internal object QuickFractionTools {

    private fun input(id: String, label: String, placeholder: String = "e.g. 3/4") = ToolInput(id, label, placeholder)

    private fun tool(
        id: String,
        name: String,
        description: String,
        inputs: List<ToolInput>,
        tests: List<ToolTest>,
        compute: (Map<String, String>) -> ToolResult
    ) = QuickTool(id, name, ToolCategory.FRACTIONS, description, inputs, compute, tests)

    private fun fractionResult(headline: String, value: Rational, vararg extra: Pair<String, String>): ToolResult {
        val decimal = BigDecimal(value.numerator).divide(BigDecimal(value.denominator), java.math.MathContext.DECIMAL64)
        val outputs = mutableListOf(
            "Simplified" to value.toString(),
            "Mixed number" to value.mixedString(),
            "Decimal" to ToolSupport.decimal(decimal.setScale(12, java.math.RoundingMode.HALF_UP))
        )
        extra.forEach { outputs.add(it) }
        return ToolSupport.result(headline, *outputs.toTypedArray())
    }

    val simplify = tool(
        "fraction-simplify", "Simplify a fraction",
        "Reduces a fraction to lowest terms and shows its mixed and decimal forms.",
        listOf(input("fraction", "Fraction")),
        listOf(
            ToolTest(mapOf("fraction" to "18/24"), mapOf("Simplified" to "3/4")),
            ToolTest(mapOf("fraction" to "7/3"), mapOf("Mixed number" to "2 1/3"))
        )
    ) { values ->
        val value = ToolSupport.rational(values, "fraction")
        fractionResult("${value.mixedString()}", value)
    }

    val add = tool(
        "fraction-add", "Add fractions",
        "Adds two fractions exactly and simplifies the result.",
        listOf(input("a", "First fraction"), input("b", "Second fraction")),
        listOf(ToolTest(mapOf("a" to "1/3", "b" to "1/6"), mapOf("Simplified" to "1/2")))
    ) { values ->
        val result = ToolSupport.rational(values, "a").plus(ToolSupport.rational(values, "b"))
        fractionResult(result.mixedString(), result)
    }

    val subtract = tool(
        "fraction-subtract", "Subtract fractions",
        "Subtracts the second fraction from the first exactly.",
        listOf(input("a", "First fraction"), input("b", "Second fraction")),
        listOf(ToolTest(mapOf("a" to "3/4", "b" to "1/3"), mapOf("Simplified" to "5/12")))
    ) { values ->
        val result = ToolSupport.rational(values, "a").minus(ToolSupport.rational(values, "b"))
        fractionResult(result.mixedString(), result)
    }

    val multiply = tool(
        "fraction-multiply", "Multiply fractions",
        "Multiplies two fractions exactly.",
        listOf(input("a", "First fraction"), input("b", "Second fraction")),
        listOf(ToolTest(mapOf("a" to "2/3", "b" to "9/4"), mapOf("Simplified" to "3/2", "Mixed number" to "1 1/2")))
    ) { values ->
        val result = ToolSupport.rational(values, "a").times(ToolSupport.rational(values, "b"))
        fractionResult(result.mixedString(), result)
    }

    val divide = tool(
        "fraction-divide", "Divide fractions",
        "Divides the first fraction by the second.",
        listOf(input("a", "Dividend fraction"), input("b", "Divisor fraction")),
        listOf(ToolTest(mapOf("a" to "3/4", "b" to "2/5"), mapOf("Simplified" to "15/8", "Mixed number" to "1 7/8")))
    ) { values ->
        val result = ToolSupport.rational(values, "a").div(ToolSupport.rational(values, "b"))
        fractionResult(result.mixedString(), result)
    }

    val improperToMixed = tool(
        "improper-to-mixed", "Improper fraction to mixed number",
        "Converts an improper fraction into a whole number plus a proper fraction.",
        listOf(input("fraction", "Improper fraction")),
        listOf(
            ToolTest(mapOf("fraction" to "17/5"), mapOf("Mixed number" to "3 2/5")),
            ToolTest(mapOf("fraction" to "-17/5"), mapOf("Mixed number" to "-3 2/5"))
        )
    ) { values ->
        val value = ToolSupport.rational(values, "fraction")
        ToolSupport.result(
            value.mixedString(),
            "Mixed number" to value.mixedString(),
            "Numerator" to value.numerator.toString(),
            "Denominator" to value.denominator.toString()
        )
    }

    val mixedToImproper = tool(
        "mixed-to-improper", "Mixed number to improper fraction",
        "Converts a mixed number such as 2 3/4 into an improper fraction.",
        listOf(ToolInput("mixed", "Mixed number", "e.g. 2 3/4")),
        listOf(
            ToolTest(mapOf("mixed" to "2 3/4"), mapOf("Improper fraction" to "11/4")),
            ToolTest(mapOf("mixed" to "-1 1/2"), mapOf("Improper fraction" to "-3/2"))
        )
    ) { values ->
        val value = Rational.parseMixed(ToolSupport.text(values, "mixed"))
        ToolSupport.result(
            "Improper fraction ${value}",
            "Improper fraction" to value.toString(),
            "Decimal" to ToolSupport.fmt(value.toDouble())
        )
    }

    val toDecimal = tool(
        "fraction-to-decimal", "Fraction to decimal and percentage",
        "Converts a fraction to a decimal and a percentage.",
        listOf(input("fraction", "Fraction")),
        listOf(
            ToolTest(mapOf("fraction" to "3/8"), mapOf("Decimal" to "0.375", "Percentage" to "37.5%"))
        )
    ) { values ->
        val value = ToolSupport.rational(values, "fraction")
        val decimal = value.toDouble()
        ToolSupport.result(
            ToolSupport.fmt(decimal),
            "Decimal" to ToolSupport.fmt(decimal),
            "Percentage" to ToolSupport.fmt(decimal * 100.0) + "%"
        )
    }

    val compare = tool(
        "fraction-compare", "Compare fractions",
        "Compares two fractions exactly and reports the difference.",
        listOf(input("a", "First fraction"), input("b", "Second fraction")),
        listOf(
            ToolTest(mapOf("a" to "3/4", "b" to "5/7"), mapOf("Comparison" to "3/4 is greater than 5/7")),
            ToolTest(mapOf("a" to "1/2", "b" to "2/4"), mapOf("Comparison" to "The fractions are equal"))
        )
    ) { values ->
        val a = ToolSupport.rational(values, "a")
        val b = ToolSupport.rational(values, "b")
        val comparison = when (a.compareTo(b)) {
            0 -> "The fractions are equal"
            1 -> "$a is greater than $b"
            else -> "$a is less than $b"
        }
        val difference = a.minus(b)
        ToolSupport.result(comparison, "Comparison" to comparison, "Difference" to difference.toString())
    }

    val commonDenominator = tool(
        "common-denominator", "Common denominator",
        "Finds the least common denominator of two fractions and rewrites both.",
        listOf(input("a", "First fraction"), input("b", "Second fraction")),
        listOf(
            ToolTest(
                mapOf("a" to "3/4", "b" to "5/6"),
                mapOf("Least common denominator" to "12", "First fraction" to "9/12", "Second fraction" to "10/12")
            )
        )
    ) { values ->
        val a = ToolSupport.rational(values, "a")
        val b = ToolSupport.rational(values, "b")
        val lcd = a.denominator.divide(a.denominator.gcd(b.denominator)).multiply(b.denominator)
        val first = Rational.of(a.numerator.multiply(lcd.divide(a.denominator)), lcd)
        val second = Rational.of(b.numerator.multiply(lcd.divide(b.denominator)), lcd)
        ToolSupport.result(
            "LCD = $lcd",
            "Least common denominator" to lcd.toString(),
            "First fraction" to first.toString(),
            "Second fraction" to second.toString()
        )
    }

    val missingTerm = tool(
        "fraction-missing-term", "Missing value in a proportion",
        "Solves a/b = c/x exactly for x.",
        listOf(input("a", "a"), input("b", "b"), input("c", "c")),
        listOf(ToolTest(mapOf("a" to "2", "b" to "3", "c" to "4"), mapOf("x" to "6")))
    ) { values ->
        val a = ToolSupport.rational(values, "a")
        val b = ToolSupport.rational(values, "b")
        val c = ToolSupport.rational(values, "c")
        if (c.isZero) throw MathException.divideByZero("c cannot be zero.")
        val x = b.times(c).div(a)
        ToolSupport.result("x = $x", "x" to x.toString(), "x (mixed)" to x.mixedString(), "x (decimal)" to ToolSupport.fmt(x.toDouble()))
    }

    val all: List<QuickTool> = listOf(
        simplify, add, subtract, multiply, divide, improperToMixed, mixedToImproper, toDecimal, compare,
        commonDenominator, missingTerm
    )
}
