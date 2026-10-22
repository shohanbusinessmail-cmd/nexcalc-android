package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/**
 * Percentage, ratio and proportion formulas.
 *
 * Percentage semantics are explicit in every notation string; no formula guesses what an
 * ambiguous "percentage" input means.
 */
internal object PercentageFormulas {

    private const val REFERENCE = "Standard percentage and ratio arithmetic."

    val percentOf = Formula(
        id = "percent-of",
        name = "Percentage of a number",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Basic percentage",
        description = "Finds x percent of a value.",
        notation = "result = (percent ÷ 100) × value",
        assumptions = listOf("The percentage is applied once to the whole value."),
        inputs = listOf(input("percent", "Percentage", "%", positive = false, default = "15"), input("value", "Value", "", default = "200")),
        outputLabels = listOf("Result" to ""),
        compute = { values ->
            val percent = values.getValue("percent")
            val amount = values.getValue("value")
            val computed = percent / 100.0 * amount
            result(
                value("Result", computed, primary = true),
                steps = listOf("(percent ÷ 100) × value = (${format(percent)} ÷ 100) × ${format(amount)} = ${format(computed)}")
            )
        },
        tests = listOf(testCase(mapOf("percent" to 15.0, "value" to 200.0), "Result" to 30.0)),
        reference = REFERENCE
    )

    val percentRatio = Formula(
        id = "percent-ratio",
        name = "What percentage is x of y",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Basic percentage",
        description = "Expresses one value as a percentage of another.",
        notation = "percent = (part ÷ whole) × 100",
        assumptions = listOf("The whole must be non-zero."),
        inputs = listOf(input("part", "Part", "", default = "30"), input("whole", "Whole", "", default = "200")),
        outputLabels = listOf("Percentage" to "%"),
        compute = { values ->
            val whole = values.getValue("whole")
            if (whole == 0.0) throw MathException.divideByZero("The whole cannot be zero.")
            val percent = values.getValue("part") / whole * 100.0
            result(
                value("Percentage", percent, "%", primary = true),
                steps = listOf("(${format(values.getValue("part"))} ÷ ${format(whole)}) × 100 = ${format(percent)}%")
            )
        },
        tests = listOf(testCase(mapOf("part" to 30.0, "whole" to 200.0), "Percentage" to 15.0)),
        reference = REFERENCE
    )

    val percentIncrease = Formula(
        id = "percent-increase",
        name = "Percentage increase",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Change",
        description = "Increases a value by a percentage and reports both the increase and the new value.",
        notation = "new = value × (1 + percent ÷ 100)",
        assumptions = listOf("A single percentage increase applied once."),
        inputs = listOf(input("value", "Original value", "", default = "200"), input("percent", "Increase", "%", positive = true, default = "15")),
        outputLabels = listOf("Increase" to "", "New value" to ""),
        compute = { values ->
            val original = values.getValue("value")
            val percent = values.getValue("percent")
            val increase = original * percent / 100.0
            result(
                value("Increase", increase),
                value("New value", original + increase, primary = true),
                steps = listOf("increase = ${format(original)} × ${format(percent)} ÷ 100 = ${format(increase)}")
            )
        },
        tests = listOf(testCase(mapOf("value" to 200.0, "percent" to 15.0), "Increase" to 30.0, "New value" to 230.0)),
        reference = REFERENCE
    )

    val percentDecrease = Formula(
        id = "percent-decrease",
        name = "Percentage decrease",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Change",
        description = "Reduces a value by a percentage and reports the reduction and the new value.",
        notation = "new = value × (1 − percent ÷ 100)",
        assumptions = listOf("A single percentage decrease applied once."),
        inputs = listOf(input("value", "Original value", "", default = "200"), input("percent", "Decrease", "%", positive = true, maximum = 100.0, default = "15")),
        outputLabels = listOf("Reduction" to "", "New value" to ""),
        compute = { values ->
            val original = values.getValue("value")
            val percent = values.getValue("percent")
            val reduction = original * percent / 100.0
            result(
                value("Reduction", reduction),
                value("New value", original - reduction, primary = true),
                steps = listOf("reduction = ${format(original)} × ${format(percent)} ÷ 100 = ${format(reduction)}")
            )
        },
        tests = listOf(testCase(mapOf("value" to 200.0, "percent" to 15.0), "Reduction" to 30.0, "New value" to 170.0)),
        reference = REFERENCE
    )

    val percentChange = Formula(
        id = "percent-change",
        name = "Percentage change",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Change",
        description = "Measures the change from an original value to a final value, including negative change.",
        notation = "change = ((final − original) ÷ original) × 100",
        assumptions = listOf("The original value must be non-zero."),
        inputs = listOf(input("original", "Original value", "", default = "250"), input("final", "Final value", "", default = "300")),
        outputLabels = listOf("Change" to "", "Percentage change" to "%"),
        compute = { values ->
            val original = values.getValue("original")
            if (original == 0.0) throw MathException.divideByZero("The original value cannot be zero.")
            val finalValue = values.getValue("final")
            val change = finalValue - original
            result(
                value("Change", change),
                value("Percentage change", change / original * 100.0, "%", primary = true),
                steps = listOf("(${format(finalValue)} − ${format(original)}) ÷ ${format(original)} × 100")
            )
        },
        tests = listOf(
            testCase(mapOf("original" to 250.0, "final" to 300.0), "Change" to 50.0, "Percentage change" to 20.0),
            testCase(mapOf("original" to 250.0, "final" to 200.0), "Change" to -50.0, "Percentage change" to -20.0)
        ),
        reference = REFERENCE
    )

    val reversePercentage = Formula(
        id = "reverse-percentage",
        name = "Reverse percentage (find the original)",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Change",
        description = "Finds the original value before a single percentage change was applied.",
        notation = "original = final ÷ (1 + percent ÷ 100)",
        assumptions = listOf("The percentage change was applied exactly once and the resulting value is known."),
        inputs = listOf(input("final", "Known final value", "", default = "230"), input("percent", "Percentage applied", "%", minimum = -99.0, default = "15")),
        outputLabels = listOf("Original value" to ""),
        compute = { values ->
            val finalValue = values.getValue("final")
            val percent = values.getValue("percent")
            val divisor = 1.0 + percent / 100.0
            if (divisor == 0.0) throw MathException.divideByZero("A percentage of -100% has no original value.")
            val original = finalValue / divisor
            result(
                value("Original value", original, primary = true),
                steps = listOf("${format(finalValue)} ÷ (1 + ${format(percent)} ÷ 100) = ${format(original)}")
            )
        },
        tests = listOf(testCase(mapOf("final" to 230.0, "percent" to 15.0), "Original value" to 200.0)),
        reference = REFERENCE
    )

    val percentagePoints = Formula(
        id = "percentage-points",
        name = "Percentage-point difference",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Change",
        description = "Reports the arithmetic difference between two percentages in percentage points.",
        notation = "points = second − first",
        assumptions = listOf("Both inputs are percentages, not proportions."),
        inputs = listOf(input("first", "First percentage", "%", default = "25"), input("second", "Second percentage", "%", default = "30")),
        outputLabels = listOf("Difference" to "points"),
        compute = { values ->
            val first = values.getValue("first")
            val second = values.getValue("second")
            result(
                value("Difference", second - first, "points", primary = true),
                warnings = listOf("Percentage points compare percentages directly; relative change would divide by the first percentage.")
            )
        },
        tests = listOf(testCase(mapOf("first" to 25.0, "second" to 30.0), "Difference" to 5.0)),
        reference = REFERENCE
    )

    val successivePercentage = Formula(
        id = "successive-percentage",
        name = "Successive percentage changes",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Change",
        description = "Combines two consecutive percentage changes into one effective change.",
        notation = "combined = ((1 + a ÷ 100)(1 + b ÷ 100) − 1) × 100",
        assumptions = listOf("The second change is applied to the result of the first change."),
        inputs = listOf(
            input("first", "First change", "%", default = "10"),
            input("second", "Second change", "%", default = "-10")
        ),
        outputLabels = listOf("Effective change" to "%", "Final value" to ""),
        compute = { values ->
            val first = values.getValue("first")
            val second = values.getValue("second")
            val factor = (1.0 + first / 100.0) * (1.0 + second / 100.0)
            result(
                value("Effective change", (factor - 1.0) * 100.0, "%", primary = true),
                value("Final value", factor * 100.0),
                steps = listOf("(${format(1 + first / 100)} × ${format(1 + second / 100)} − 1) × 100"),
                warnings = listOf("The final value assumes a starting value of 100.")
            )
        },
        tests = listOf(
            testCase(mapOf("first" to 10.0, "second" to -10.0), "Effective change" to -1.0, "Final value" to 99.0),
            testCase(mapOf("first" to 20.0, "second" to 20.0), "Effective change" to 44.0, "Final value" to 144.0)
        ),
        reference = REFERENCE
    )

    val ratioSimplify = Formula(
        id = "ratio-simplify",
        name = "Simplify a ratio",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Ratio & proportion",
        description = "Reduces a two-term ratio of whole numbers to its simplest form.",
        notation = "a : b = (a ÷ g) : (b ÷ g), g = gcd(a, b)",
        assumptions = listOf("Both terms are whole numbers."),
        inputs = listOf(
            input("a", "First term", "", integer = true, positive = true, maximum = 1e12, default = "18"),
            input("b", "Second term", "", integer = true, positive = true, maximum = 1e12, default = "24")
        ),
        outputLabels = listOf("First term" to "", "Second term" to "", "Greatest common divisor" to ""),
        compute = { values ->
            val a = values.getValue("a").toLong()
            val b = values.getValue("b").toLong()
            val divisor = java.math.BigInteger.valueOf(a).gcd(java.math.BigInteger.valueOf(b)).toLong().coerceAtLeast(1L)
            result(
                value("First term", (a / divisor).toDouble(), primary = true),
                value("Second term", (b / divisor).toDouble()),
                value("Greatest common divisor", divisor.toDouble()),
                steps = listOf("gcd(${a}, ${b}) = $divisor", "Simplified ratio = ${a / divisor} : ${b / divisor}")
            )
        },
        tests = listOf(
            testCase(mapOf("a" to 18.0, "b" to 24.0), "First term" to 3.0, "Second term" to 4.0, "Greatest common divisor" to 6.0),
            testCase(mapOf("a" to 100.0, "b" to 25.0), "First term" to 4.0, "Second term" to 1.0, "Greatest common divisor" to 25.0)
        ),
        reference = REFERENCE
    )

    val ratioDivide = Formula(
        id = "ratio-divide",
        name = "Divide a quantity in a ratio",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Ratio & proportion",
        description = "Splits an amount into two shares according to a ratio.",
        notation = "shareA = amount × a ÷ (a + b)",
        assumptions = listOf("The ratio terms are non-negative and not both zero."),
        inputs = listOf(
            input("amount", "Total amount", "", default = "100"),
            input("a", "Share A ratio", "", nonNegative = true, default = "2"),
            input("b", "Share B ratio", "", nonNegative = true, default = "3")
        ),
        outputLabels = listOf("Share A" to "", "Share B" to ""),
        compute = { values ->
            val amount = values.getValue("amount")
            val a = values.getValue("a")
            val b = values.getValue("b")
            val total = a + b
            if (total <= 0) throw MathException.invalid("The ratio terms cannot both be zero.")
            val shareA = amount * a / total
            result(
                value("Share A", shareA, primary = true),
                value("Share B", amount - shareA),
                steps = listOf("share A = ${format(amount)} × ${format(a)} ÷ ${format(total)}")
            )
        },
        tests = listOf(testCase(mapOf("amount" to 100.0, "a" to 2.0, "b" to 3.0), "Share A" to 40.0, "Share B" to 60.0)),
        reference = REFERENCE
    )

    val proportionDirect = Formula(
        id = "proportion-direct",
        name = "Direct proportion",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Ratio & proportion",
        description = "Solves a ÷ b = c ÷ x for x in a direct proportion.",
        notation = "x = b × c ÷ a",
        assumptions = listOf("The relation is directly proportional and a is non-zero."),
        inputs = listOf(
            input("a", "Known a", "", positive = true, default = "2"),
            input("b", "Known b", "", default = "4"),
            input("c", "Known c", "", default = "6")
        ),
        outputLabels = listOf("x" to ""),
        compute = { values ->
            val a = values.getValue("a")
            val b = values.getValue("b")
            val c = values.getValue("c")
            result(
                value("x", b * c / a, primary = true),
                steps = listOf("x = ${format(b)} × ${format(c)} ÷ ${format(a)}")
            )
        },
        tests = listOf(testCase(mapOf("a" to 2.0, "b" to 4.0, "c" to 6.0), "x" to 12.0)),
        reference = REFERENCE
    )

    val proportionInverse = Formula(
        id = "proportion-inverse",
        name = "Inverse proportion",
        category = FormulaCategory.PERCENTAGE,
        subcategory = "Ratio & proportion",
        description = "Solves a × b = c × x for x in an inverse proportion.",
        notation = "x = a × b ÷ c",
        assumptions = listOf("The relation is inversely proportional and c is non-zero."),
        inputs = listOf(
            input("a", "Known a", "", default = "4"),
            input("b", "Known b", "", default = "6"),
            input("c", "Known c", "", positive = true, default = "3")
        ),
        outputLabels = listOf("x" to ""),
        compute = { values ->
            val a = values.getValue("a")
            val b = values.getValue("b")
            val c = values.getValue("c")
            result(
                value("x", a * b / c, primary = true),
                steps = listOf("x = ${format(a)} × ${format(b)} ÷ ${format(c)}")
            )
        },
        tests = listOf(testCase(mapOf("a" to 4.0, "b" to 6.0, "c" to 3.0), "x" to 8.0)),
        reference = REFERENCE
    )

    val all: List<Formula> = listOf(
        percentOf, percentRatio, percentIncrease, percentDecrease, percentChange, reversePercentage,
        percentagePoints, successivePercentage, ratioSimplify, ratioDivide, proportionDirect, proportionInverse
    )
}
