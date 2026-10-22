package com.shohankhan.nexcalc.math

import java.math.BigInteger

import java.math.RoundingMode

/**
 * Mental-maths shortcuts.
 *
 * A method is only shown when it is mathematically valid for the supplied numbers; otherwise the
 * tool explains why the shortcut does not apply instead of showing a misleading derivation.
 */
internal object QuickMentalTools {

    private fun input(id: String, label: String, placeholder: String = "") = ToolInput(id, label, placeholder)

    private fun tool(
        id: String,
        name: String,
        description: String,
        inputs: List<ToolInput>,
        tests: List<ToolTest>,
        compute: (Map<String, String>) -> ToolResult
    ) = QuickTool(id, name, ToolCategory.MENTAL, description, inputs, compute, tests)

    val squareEndingFive = tool(
        "square-ending-five", "Square numbers ending in 5",
        "Squares a number ending in 5 using n(n+1) followed by 25.",
        listOf(input("n", "Number ending in 5", "e.g. 35")),
        listOf(
            ToolTest(mapOf("n" to "35"), mapOf("Square" to "1225")),
            ToolTest(mapOf("n" to "85"), mapOf("Square" to "7225"))
        )
    ) { values ->
        val n = ToolSupport.long(values, "n")
        if (n < 5 || n % 10 != 5L) throw MathException.invalid("This shortcut requires a whole number ending in 5.")
        val prefix = n / 10
        val square = n * n
        ToolSupport.result(
            "$n² = $square",
            "Square" to square.toString(),
            steps = listOf("${prefix} × ${prefix + 1} = ${prefix * (prefix + 1)}", "Append 25 → $square")
        )
    }

    val nearBase = tool(
        "near-base-multiplication", "Multiply numbers near 100",
        "Multiplies two numbers just below or above 100 using deficits and surpluses.",
        listOf(input("a", "First number", "80–120"), input("b", "Second number", "80–120")),
        listOf(ToolTest(mapOf("a" to "98", "b" to "97"), mapOf("Product" to "9506")))
    ) { values ->
        val a = ToolSupport.long(values, "a")
        val b = ToolSupport.long(values, "b")
        if (a !in 80..120 || b !in 80..120) throw MathException.invalid("This shortcut is published for numbers from 80 to 120.")
        val da = a - 100
        val db = b - 100
        val product = a * b
        ToolSupport.result(
            "$a × $b = $product",
            "Product" to product.toString(),
            steps = listOf(
                "Deficits: ${-da} and ${-db}",
                "Left part: 100 ${if (da < 0) "−" else "+"} ${kotlin.math.abs(da)} ${if (db < 0) "−" else "+"} ${kotlin.math.abs(db)} = ${100 + da + db}",
                "Right part: ${kotlin.math.abs(da * db)}",
                "Result: $product"
            )
        )
    }

    val differenceOfSquares = tool(
        "difference-of-squares-product", "Multiply using difference of squares",
        "Multiplies two numbers with the same parity via the mean and half-difference.",
        listOf(input("a", "First number"), input("b", "Second number")),
        listOf(
            ToolTest(mapOf("a" to "52", "b" to "48"), mapOf("Product" to "2496")),
            ToolTest(mapOf("a" to "103", "b" to "97"), mapOf("Product" to "9991"))
        )
    ) { values ->
        val a = ToolSupport.long(values, "a")
        val b = ToolSupport.long(values, "b")
        if ((a + b) % 2 != 0L) throw MathException.invalid("Both numbers must have the same parity for this shortcut.")
        val mean = (a + b) / 2
        val half = (a - b) / 2
        val product = a * b
        ToolSupport.result(
            "$a × $b = $product",
            "Product" to product.toString(),
            steps = listOf("Mean = $mean, half-difference = $half", "$mean² − $half² = ${mean * mean} − ${half * half} = $product")
        )
    }

    val percentageShortcut = tool(
        "percentage-shortcut", "Percentage shortcut",
        "Computes a percentage by splitting it into easy parts.",
        listOf(input("percent", "Percentage"), input("value", "Value")),
        listOf(ToolTest(mapOf("percent" to "15", "value" to "240"), mapOf("Result" to "36")))
    ) { values ->
        val percent = ToolSupport.double(values, "percent")
        val value = ToolSupport.double(values, "value")
        val tenPercent = value / 10.0
        val fivePercent = value / 20.0
        val onePercent = value / 100.0
        val result = percent / 100.0 * value
        ToolSupport.result(
            "${ToolSupport.fmt(percent)}% of ${ToolSupport.fmt(value)} = ${ToolSupport.fmt(result)}",
            "Result" to ToolSupport.fmt(result),
            steps = listOf(
                "10% = ${ToolSupport.fmt(tenPercent)}",
                "5% = ${ToolSupport.fmt(fivePercent)}",
                "1% = ${ToolSupport.fmt(onePercent)}"
            )
        )
    }

    val multiplyByEleven = tool(
        "multiply-by-eleven", "Multiply a two-digit number by 11",
        "Uses the digit-sum rule for two-digit numbers.",
        listOf(input("n", "Two-digit number", "10–99")),
        listOf(ToolTest(mapOf("n" to "47"), mapOf("Product" to "517")))
    ) { values ->
        val n = ToolSupport.long(values, "n")
        if (n !in 10..99) throw MathException.invalid("This shortcut is published for two-digit numbers only.")
        val tens = n / 10
        val units = n % 10
        val middle = tens + units
        val product = n * 11
        ToolSupport.result(
            "$n × 11 = $product",
            "Product" to product.toString(),
            steps = listOf("Middle digit = $tens + $units = $middle", "Write ${tens}${if (middle >= 10) middle % 10 else middle}${units} with a carry if needed")
        )
    }

    val rounding = tool(
        "rounding", "Rounding and significant figures",
        "Rounds a number to a number of decimal places and significant figures.",
        listOf(input("value", "Number"), input("places", "Decimal places", "0–12")),
        listOf(ToolTest(mapOf("value" to "3.14159", "places" to "2"), mapOf("Rounded" to "3.14")))
    ) { values ->
        val value = ToolSupport.double(values, "value")
        val places = ToolSupport.long(values, "places")
        if (places !in 0..12) throw MathException.invalid("Decimal places must be between 0 and 12.")
        val rounded = java.math.BigDecimal(value).setScale(places.toInt(), RoundingMode.HALF_UP)
        val significant = NumberFormatter.significant(value, 3, ToolSupport.formatOptions)
        ToolSupport.result(
            rounded.toPlainString(),
            "Rounded" to rounded.toPlainString(),
            "Three significant figures" to significant,
            "Nearest whole number" to NumberFormatter.format(Math.round(value).toDouble(), ToolSupport.formatOptions)
        )
    }

    val multiplicationTable = tool(
        "multiplication-table", "Multiplication table",
        "Prints the twelve-row multiplication table for a number.",
        listOf(input("n", "Number")),
        listOf(ToolTest(mapOf("n" to "7"), mapOf("7 × 12" to "84")))
    ) { values ->
        val n = ToolSupport.long(values, "n")
        if (kotlin.math.abs(n) > 1_000_000) throw MathException.limit("The table is limited to values up to one million.")
        val rows = (1..12).map { n * it }
        ToolSupport.result(
            "Table of $n",
            "Rows" to (1..12).joinToString(", ") { "$n × $it = ${n * it}" },
            "7 × 12" to (n * 12).toString()
        )
    }

    val powersOfTwo = tool(
        "powers-of-two", "Powers of two",
        "Lists powers of two up to a chosen exponent.",
        listOf(input("exponent", "Largest exponent", "0–62")),
        listOf(ToolTest(mapOf("exponent" to "10"), mapOf("2^10" to "1024")))
    ) { values ->
        val exponent = ToolSupport.long(values, "exponent")
        if (exponent !in 0..62) throw MathException.limit("The exponent must be between 0 and 62.")
        val doubled = (1L shl exponent.toInt())
        ToolSupport.result(
            "2^$exponent = $doubled",
            "2^$exponent" to doubled.toString(),
            "Values" to (0..exponent.toInt()).joinToString(", ") { (1L shl it).toString() }
        )
    }

    val powersOfTen = tool(
        "powers-of-ten", "Powers of ten",
        "Lists powers of ten up to a chosen exponent.",
        listOf(input("exponent", "Largest exponent", "0–18")),
        listOf(ToolTest(mapOf("exponent" to "6"), mapOf("10^6" to "1000000")))
    ) { values ->
        val exponent = ToolSupport.long(values, "exponent")
        if (exponent !in 0..18) throw MathException.limit("The exponent must be between 0 and 18.")
        val value = Math.pow(10.0, exponent.toDouble())
        ToolSupport.result(
            "10^$exponent = ${ToolSupport.fmt(value)}",
            "10^$exponent" to ToolSupport.fmt(value),
            "Values" to (0..exponent.toInt()).joinToString(", ") { ToolSupport.fmt(Math.pow(10.0, it.toDouble())) }
        )
    }

    val estimateProduct = tool(
        "estimate-product", "Estimate a product",
        "Rounds both factors to one significant figure and reports the estimate with the exact product.",
        listOf(input("a", "First factor"), input("b", "Second factor")),
        listOf(ToolTest(mapOf("a" to "48", "b" to "21"), mapOf("Estimate" to "1000", "Exact product" to "1008")))
    ) { values ->
        val a = ToolSupport.double(values, "a")
        val b = ToolSupport.double(values, "b")
        fun roundOne(value: Double): Double {
            if (value == 0.0) return 0.0
            val magnitude = Math.pow(10.0, Math.floor(Math.log10(kotlin.math.abs(value))))
            return Math.round(value / magnitude).toDouble() * magnitude
        }
        val roundedA = roundOne(a)
        val roundedB = roundOne(b)
        ToolSupport.result(
            "≈ ${ToolSupport.fmt(roundedA * roundedB)}",
            "Estimate" to ToolSupport.fmt(roundedA * roundedB),
            "Exact product" to ToolSupport.fmt(a * b),
            "Rounded factors" to "${ToolSupport.fmt(roundedA)} × ${ToolSupport.fmt(roundedB)}"
        )
    }

    val squareEndingFive = tool(
        "square-ending-five", "Square a number ending in 5",
        "Fast method: n5 squared equals n(n+1) followed by 25.",
        listOf(input("number", "Number ending in 5", "35")),
        listOf(
            ToolTest(mapOf("number" to "35"), mapOf("Square" to "1225")),
            ToolTest(mapOf("number" to "5"), mapOf("Square" to "25")),
            ToolTest(mapOf("number" to "105"), mapOf("Square" to "11025"))
        )
    ) { values ->
        val number = ToolSupport.long(values, "number")
        if (number < 5 || number % 10 != 5L) throw MathException.invalid("Enter a positive whole number ending in 5.")
        if (number > 100_000_000L) throw MathException.limit("Values are limited to 100 000 000.")
        val n = BigInteger.valueOf(number / 10)
        val square = n.multiply(n.add(BigInteger.ONE)).multiply(BigInteger.valueOf(100)).add(BigInteger.valueOf(25))
        ToolSupport.result(
            "$number² = $square",
            "Square" to square.toString(),
            steps = listOf("n = ${number / 10}; n(n + 1) = ${n.multiply(n.add(BigInteger.ONE))}; append 25")
        )
    }

    val all: List<QuickTool> = listOf(
        squareEndingFive, nearBase, differenceOfSquares, percentageShortcut, multiplyByEleven, rounding,
        multiplicationTable, powersOfTwo, powersOfTen, estimateProduct
    )
}

/** Sequence generation and nth-term tools. */
internal object QuickSequenceTools {

    private fun input(id: String, label: String, placeholder: String = "") = ToolInput(id, label, placeholder)

    private fun tool(
        id: String,
        name: String,
        description: String,
        inputs: List<ToolInput>,
        tests: List<ToolTest>,
        compute: (Map<String, String>) -> ToolResult
    ) = QuickTool(id, name, ToolCategory.SEQUENCES, description, inputs, compute, tests)

    private fun terms(values: List<String>, limit: Int = 20): String {
        val shown = values.take(limit).joinToString(", ")
        return if (values.size > limit) "$shown, …" else shown
    }

    val arithmetic = tool(
        "arithmetic-sequence", "Arithmetic sequence",
        "Generates an arithmetic sequence with its nth term and sum.",
        listOf(input("first", "First term", "e.g. 3"), input("difference", "Common difference", "e.g. 4"), input("n", "Number of terms", "1–100")),
        listOf(
            ToolTest(
                mapOf("first" to "3", "difference" to "4", "n" to "5"),
                mapOf("Terms" to "3, 7, 11, 15, 19", "nth term" to "19", "Sum" to "55")
            )
        )
    ) { values ->
        val first = ToolSupport.double(values, "first")
        val difference = ToolSupport.double(values, "difference")
        val n = ToolSupport.positiveInt(values, "n", 100)
        val generated = (0 until n).map { ToolSupport.fmt(first + it * difference) }
        val nth = first + (n - 1) * difference
        val sum = n / 2.0 * (2 * first + (n - 1) * difference)
        ToolSupport.result(
            "nth term ${ToolSupport.fmt(nth)}",
            "Terms" to terms(generated),
            "nth term" to ToolSupport.fmt(nth),
            "Sum" to ToolSupport.fmt(sum),
            steps = listOf("aₙ = ${ToolSupport.fmt(first)} + (${n} − 1) × ${ToolSupport.fmt(difference)}")
        )
    }

    val geometric = tool(
        "geometric-sequence", "Geometric sequence",
        "Generates a geometric sequence with its nth term and sum.",
        listOf(input("first", "First term", "e.g. 2"), input("ratio", "Common ratio", "e.g. 3"), input("n", "Number of terms", "1–100")),
        listOf(
            ToolTest(
                mapOf("first" to "2", "ratio" to "3", "n" to "4"),
                mapOf("Terms" to "2, 6, 18, 54", "nth term" to "54", "Sum" to "80")
            )
        )
    ) { values ->
        val first = ToolSupport.double(values, "first")
        val ratio = ToolSupport.double(values, "ratio")
        val n = ToolSupport.positiveInt(values, "n", 100)
        val generated = (0 until n).map { ToolSupport.fmt(first * Math.pow(ratio, it.toDouble())) }
        val nth = first * Math.pow(ratio, n - 1.0)
        val sum = if (ratio == 1.0) first * n else first * (1 - Math.pow(ratio, n.toDouble())) / (1 - ratio)
        ToolSupport.result(
            "nth term ${ToolSupport.fmt(nth)}",
            "Terms" to terms(generated),
            "nth term" to ToolSupport.fmt(nth),
            "Sum" to ToolSupport.fmt(sum)
        )
    }

    val fibonacci = tool(
        "fibonacci", "Fibonacci sequence",
        "First n Fibonacci numbers with the nth term and their sum.",
        listOf(input("n", "Number of terms", "1–90")),
        listOf(ToolTest(mapOf("n" to "10"), mapOf("nth term" to "55", "Sum" to "143")))
    ) { values ->
        val n = ToolSupport.positiveInt(values, "n", 90)
        val numbers = ArrayList<Long>(n)
        var previous = 1L
        var current = 1L
        var sum = 0L
        repeat(n) {
            numbers.add(previous)
            sum += previous
            val next = previous + current
            previous = current
            current = next
        }
        ToolSupport.result(
            "F($n) = ${numbers.last()}",
            "Terms" to terms(numbers.map { it.toString() }, 30),
            "nth term" to numbers.last().toString(),
            "Sum" to sum.toString()
        )
    }

    val nextTerms = tool(
        "sequence-next-terms", "Next terms of a sequence",
        "Detects an arithmetic or geometric rule in the first three terms and shows the next three.",
        listOf(input("t1", "Term 1"), input("t2", "Term 2"), input("t3", "Term 3")),
        listOf(
            ToolTest(
                mapOf("t1" to "2", "t2" to "6", "t3" to "18"),
                mapOf("Rule" to "Geometric with ratio 3", "Next terms" to "54, 162, 486")
            ),
            ToolTest(
                mapOf("t1" to "5", "t2" to "9", "t3" to "13"),
                mapOf("Rule" to "Arithmetic with difference 4", "Next terms" to "17, 21, 25")
            )
        )
    ) { values ->
        val t1 = ToolSupport.double(values, "t1")
        val t2 = ToolSupport.double(values, "t2")
        val t3 = ToolSupport.double(values, "t3")
        val d1 = t2 - t1
        val d2 = t3 - t2
        val next: List<Double>
        val rule: String
        if (d1 == d2) {
            rule = "Arithmetic with difference ${ToolSupport.fmt(d1)}"
            next = listOf(t3 + d1, t3 + 2 * d1, t3 + 3 * d1)
        } else if (t1 != 0.0 && t2 / t1 == t3 / t2) {
            val ratio = t2 / t1
            rule = "Geometric with ratio ${ToolSupport.fmt(ratio)}"
            next = listOf(t3 * ratio, t3 * ratio * ratio, t3 * ratio * ratio * ratio)
        } else {
            throw MathException.domain(
                "The first three terms fit neither an arithmetic nor a geometric rule. NEXCALC will not guess a pattern."
            )
        }
        ToolSupport.result(
            rule,
            "Rule" to rule,
            "Next terms" to next.joinToString(", ") { ToolSupport.fmt(it) },
            warnings = listOf("Many sequences can share these first terms; the shown rule is one valid interpretation.")
        )
    }

    val naturalSum = tool(
        "natural-sum", "Sum of the first n natural numbers",
        "Computes 1 + 2 + … + n with the closed form.",
        listOf(input("n", "n", "up to 1 000 000")),
        listOf(ToolTest(mapOf("n" to "100"), mapOf("Sum" to "5050")))
    ) { values ->
        val n = ToolSupport.positiveInt(values, "n", 1_000_000)
        val sum = n.toLong() * (n + 1L) / 2L
        ToolSupport.result(
            "1 + … + $n = $sum",
            "Sum" to sum.toString(),
            steps = listOf("n(n + 1) ÷ 2 = $n × ${n + 1} ÷ 2")
        )
    }

    val all: List<QuickTool> = listOf(arithmetic, geometric, fibonacci, nextTerms, naturalSum)
}
