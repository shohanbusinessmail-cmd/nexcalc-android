package com.shohankhan.nexcalc.math

import java.math.BigInteger

/** Number theory tools for the Quick Solve tab. */
internal object QuickNumberTools {

    private fun input(id: String, label: String, placeholder: String = "Whole number", default: String = "") =
        ToolInput(id, label, placeholder, default)

    private fun tool(
        id: String,
        name: String,
        description: String,
        inputs: List<ToolInput>,
        tests: List<ToolTest>,
        compute: (Map<String, String>) -> ToolResult
    ) = QuickTool(id, name, ToolCategory.NUMBER_THEORY, description, inputs, compute, tests)

    private const val TWO_LIMIT = 1_000_000_000_000L

    val gcdLcm = tool(
        "gcd-lcm", "GCD and LCM",
        "Greatest common divisor and least common multiple of two integers.",
        listOf(input("a", "First number"), input("b", "Second number")),
        listOf(
            ToolTest(
                mapOf("a" to "48", "b" to "18"),
                mapOf("GCD" to "6", "LCM" to "144")
            ),
            ToolTest(
                mapOf("a" to "-12", "b" to "18"),
                mapOf("GCD" to "6", "LCM" to "36")
            )
        )
    ) { values ->
        val a = ToolSupport.big(values, "a", TWO_LIMIT)
        val b = ToolSupport.big(values, "b", TWO_LIMIT)
        val gcd = NumberTheory.gcd(a, b)
        val lcm = NumberTheory.lcm(a, b)
        ToolSupport.result(
            "GCD ${ToolSupport.fmtInteger(gcd)} · LCM ${ToolSupport.fmtInteger(lcm)}",
            "GCD" to ToolSupport.fmtInteger(gcd),
            "LCM" to ToolSupport.fmtInteger(lcm),
            steps = listOf("gcd(${a}, ${b}) = ${NumberTheory.gcd(a, b)}", "lcm = |a × b| ÷ gcd")
        )
    }

    val primeCheck = tool(
        "prime-check", "Prime check",
        "Tests whether an integer is prime.",
        listOf(input("n", "Number")),
        listOf(
            ToolTest(mapOf("n" to "97"), mapOf("Prime" to "Yes")),
            ToolTest(mapOf("n" to "91"), mapOf("Prime" to "No", "Smallest prime factor" to "7")),
            ToolTest(mapOf("n" to "1"), mapOf("Prime" to "No"))
        )
    ) { values ->
        val n = ToolSupport.big(values, "n", TWO_LIMIT)
        val prime = NumberTheory.isPrime(n)
        if (prime) {
            ToolSupport.result("${n} is prime", "Prime" to "Yes")
        } else {
            val factors = NumberTheory.primeFactors(n)
            val smallest = factors.firstOrNull()?.toString() ?: "—"
            ToolSupport.result(
                "${n} is not prime",
                "Prime" to "No",
                "Smallest prime factor" to smallest,
                warnings = if (n < BigInteger.valueOf(2)) listOf("Prime numbers are greater than 1.") else emptyList()
            )
        }
    }

    val primeFactorization = tool(
        "prime-factorization", "Prime factorization",
        "Expresses an integer as a product of primes with exponent notation.",
        listOf(input("n", "Number")),
        listOf(
            ToolTest(mapOf("n" to "360"), mapOf("Factorization" to "2³ × 3² × 5")),
            ToolTest(mapOf("n" to "97"), mapOf("Factorization" to "97"))
        )
    ) { values ->
        val n = ToolSupport.big(values, "n", TWO_LIMIT).abs()
        if (n <= BigInteger.ONE) throw MathException.domain("Prime factorization is defined for integers greater than 1.")
        val factors = NumberTheory.primeFactors(n)
        val grouped = factors.groupingBy { it }.eachCount()
        val text = grouped.entries.joinToString(" × ") { (prime, power) ->
            when (power) {
                1 -> prime.toString()
                2 -> "$prime\u00B2"
                3 -> "$prime\u00B3"
                else -> "$prime^$power"
            }
        }
        ToolSupport.result(
            "$n = $text",
            "Factorization" to text,
            "Number of prime factors (with multiplicity)" to factors.size.toString(),
            "Distinct primes" to grouped.size.toString()
        )
    }

    val factors = tool(
        "factor-list", "Factors and divisors",
        "All positive divisors, with count and sum.",
        listOf(input("n", "Number")),
        listOf(
            ToolTest(mapOf("n" to "36"), mapOf("Divisors" to "1, 2, 3, 4, 6, 9, 12, 18, 36", "Divisor count" to "9", "Divisor sum" to "91")),
            ToolTest(mapOf("n" to "97"), mapOf("Divisor count" to "2", "Divisor sum" to "98"))
        )
    ) { values ->
        val n = ToolSupport.big(values, "n", TWO_LIMIT).abs()
        if (n.signum() == 0) throw MathException.domain("Zero has infinitely many divisors.")
        val divisors = NumberTheory.divisors(n)
        val sum = divisors.fold(BigInteger.ZERO) { acc, value -> acc.add(value) }
        ToolSupport.result(
            "${divisors.size} divisors",
            "Divisors" to divisors.joinToString(", "),
            "Divisor count" to divisors.size.toString(),
            "Divisor sum" to sum.toString(),
            "Proper divisors" to divisors.dropLast(1).joinToString(", ")
        )
    }

    val divisorsOfGcd = tool(
        "common-factors", "Common factors",
        "Factors shared by two integers, excluding 1.",
        listOf(input("a", "First number"), input("b", "Second number")),
        listOf(
            ToolTest(mapOf("a" to "48", "b" to "18"), mapOf("Common factors" to "2, 3, 6"))
        )
    ) { values ->
        val a = ToolSupport.big(values, "a", TWO_LIMIT)
        val b = ToolSupport.big(values, "b", TWO_LIMIT)
        val shared = NumberTheory.sharedFactors(a, b)
        ToolSupport.result(
            "${shared.size} common factors above 1",
            "Common factors" to if (shared.isEmpty()) "None" else shared.joinToString(", "),
            "All common factors" to NumberTheory.commonFactors(a, b).joinToString(", ")
        )
    }

    val commonMultiples = tool(
        "common-multiples", "Common multiples",
        "First five common multiples of two integers.",
        listOf(input("a", "First number"), input("b", "Second number")),
        listOf(
            ToolTest(mapOf("a" to "4", "b" to "6"), mapOf("Common multiples" to "12, 24, 36, 48, 60"))
        )
    ) { values ->
        val a = ToolSupport.big(values, "a", TWO_LIMIT)
        val b = ToolSupport.big(values, "b", TWO_LIMIT)
        val multiples = NumberTheory.commonMultiples(a, b, 5)
        ToolSupport.result(
            "LCM is ${NumberTheory.lcm(a, b)}",
            "Common multiples" to multiples.joinToString(", "),
            "LCM" to NumberTheory.lcm(a, b).toString()
        )
    }

    val divisibility = tool(
        "divisibility", "Division with remainder",
        "Integer quotient and remainder for integer division truncating toward zero.",
        listOf(input("a", "Dividend"), input("b", "Divisor")),
        listOf(
            ToolTest(mapOf("a" to "17", "b" to "5"), mapOf("Quotient" to "3", "Remainder" to "2", "Divisible" to "No")),
            ToolTest(mapOf("a" to "-17", "b" to "5"), mapOf("Quotient" to "-3", "Remainder" to "-2"))
        )
    ) { values ->
        val a = ToolSupport.big(values, "a", TWO_LIMIT)
        val b = ToolSupport.big(values, "b", TWO_LIMIT)
        if (b.signum() == 0) throw MathException.divideByZero()
        val result = a.divideAndRemainder(b)
        val quotient = result[0]
        val remainder = result[1]
        ToolSupport.result(
            "${a} ÷ ${b} = $quotient remainder $remainder",
            "Quotient" to quotient.toString(),
            "Remainder" to remainder.toString(),
            "Divisible" to if (remainder.signum() == 0) "Yes" else "No"
        )
    }

    val coprime = tool(
        "coprime", "Coprime check",
        "Tests whether two integers share no factor other than 1.",
        listOf(input("a", "First number"), input("b", "Second number")),
        listOf(
            ToolTest(mapOf("a" to "14", "b" to "25"), mapOf("Coprime" to "Yes", "GCD" to "1")),
            ToolTest(mapOf("a" to "14", "b" to "21"), mapOf("Coprime" to "No", "GCD" to "7"))
        )
    ) { values ->
        val a = ToolSupport.big(values, "a", TWO_LIMIT)
        val b = ToolSupport.big(values, "b", TWO_LIMIT)
        val gcd = NumberTheory.gcd(a, b)
        ToolSupport.result(
            if (gcd == BigInteger.ONE) "Coprime" else "Not coprime",
            "Coprime" to if (gcd == BigInteger.ONE) "Yes" else "No",
            "GCD" to gcd.toString()
        )
    }

    val modularArithmetic = tool(
        "modular-arithmetic", "Modular arithmetic",
        "Remainder modulo n, using the sign convention of the divisor.",
        listOf(input("a", "Value"), input("modulus", "Modulus")),
        listOf(
            ToolTest(mapOf("a" to "27", "modulus" to "5"), mapOf("Remainder" to "2")),
            ToolTest(mapOf("a" to "-27", "modulus" to "5"), mapOf("Remainder" to "3"))
        )
    ) { values ->
        val a = ToolSupport.big(values, "a", TWO_LIMIT)
        val modulus = ToolSupport.big(values, "modulus", TWO_LIMIT)
        if (modulus.signum() == 0) throw MathException.divideByZero("The modulus cannot be zero.")
        val remainder = a.mod(modulus.abs())
        ToolSupport.result(
            "$a ≡ $remainder (mod ${modulus.abs()})",
            "Remainder" to remainder.toString(),
            "Modulus" to modulus.abs().toString()
        )
    }

    val integerRoots = tool(
        "integer-roots", "Integer roots",
        "Integer square root, integer cube root and perfect-power checks.",
        listOf(input("n", "Non-negative integer")),
        listOf(
            ToolTest(
                mapOf("n" to "144"),
                mapOf("Integer square root" to "12", "Perfect square" to "Yes", "Perfect cube" to "No")
            ),
            ToolTest(
                mapOf("n" to "27"),
                mapOf("Integer cube root" to "3", "Perfect cube" to "Yes", "Perfect square" to "No")
            )
        )
    ) { values ->
        val n = ToolSupport.big(values, "n", 1_000_000_000_000_000_000L)
        if (n.signum() < 0) throw MathException.domain("Integer roots are shown for non-negative integers.")
        val sqrtValue = NumberTheory.integerSquareRoot(n)
        val cbrtValue = NumberTheory.integerCubeRoot(n)
        ToolSupport.result(
            "√$n = $sqrtValue",
            "Integer square root" to sqrtValue.toString(),
            "Integer cube root" to cbrtValue.toString(),
            "Perfect square" to if (NumberTheory.isPerfectSquare(n)) "Yes" else "No",
            "Perfect cube" to if (NumberTheory.isPerfectCube(n)) "Yes" else "No"
        )
    }

    val digits = tool(
        "digit-properties", "Digits and parity",
        "Parity, digit count, digit sum and reversed digits.",
        listOf(input("n", "Integer")),
        listOf(
            ToolTest(
                mapOf("n" to "1234"),
                mapOf("Parity" to "Even", "Digit count" to "4", "Digit sum" to "10", "Reversed digits" to "4321")
            ),
            ToolTest(mapOf("n" to "-507"), mapOf("Parity" to "Odd", "Digit count" to "3", "Digit sum" to "12"))
        )
    ) { values ->
        val n = ToolSupport.big(values, "n", 1_000_000_000_000_000_000L)
        ToolSupport.result(
            "${NumberTheory.digitCount(n)} digits",
            "Parity" to if (NumberTheory.isEven(n)) "Even" else "Odd",
            "Digit count" to NumberTheory.digitCount(n).toString(),
            "Digit sum" to NumberTheory.digitSum(n).toString(),
            "Reversed digits" to NumberTheory.reverseDigits(n).toString()
        )
    }

    val trailingZeros = tool(
        "trailing-zeros", "Trailing zeros of n!",
        "Number of trailing zeros of n factorial, computed without expanding the factorial.",
        listOf(input("n", "n")),
        listOf(
            ToolTest(mapOf("n" to "100"), mapOf("Trailing zeros" to "24")),
            ToolTest(mapOf("n" to "5"), mapOf("Trailing zeros" to "1"))
        )
    ) { values ->
        val n = ToolSupport.positiveInt(values, "n", 1_000_000_000)
        val zeros = NumberTheory.trailingZerosOfFactorial(n.toLong())
        ToolSupport.result(
            "$zeros trailing zeros",
            "Trailing zeros" to zeros.toString(),
            steps = listOf("⌊${n}/5⌋ + ⌊${n}/25⌋ + ⌊${n}/125⌋ + …")
        )
    }

    val primesUpTo = tool(
        "prime-list", "Primes up to a limit",
        "Generates primes below a limit using a sieve.",
        listOf(input("limit", "Upper limit", "At most 1 000 000")),
        listOf(
            ToolTest(mapOf("limit" to "30"), mapOf("Count" to "10", "Largest prime" to "29"))
        )
    ) { values ->
        val limit = ToolSupport.positiveInt(values, "limit", NumberTheory.PRIME_SIEVE_LIMIT)
        val primes = NumberTheory.primesUpTo(limit)
        val preview = if (primes.size <= 100) primes.joinToString(", ") else primes.take(100).joinToString(", ") + ", …"
        ToolSupport.result(
            "${primes.size} primes up to $limit",
            "Count" to primes.size.toString(),
            "Largest prime" to (primes.lastOrNull()?.toString() ?: "—"),
            "Primes" to preview
        )
    }

    val modularExponentiation = tool(
        "modular-exponentiation", "Modular exponentiation",
        "Computes base^exponent mod modulus exactly with arbitrary-precision integers.",
        listOf(input("base", "Base"), input("exponent", "Exponent"), input("modulus", "Modulus")),
        listOf(
            ToolTest(mapOf("base" to "2", "exponent" to "10", "modulus" to "1000"), mapOf("Result" to "24")),
            ToolTest(mapOf("base" to "3", "exponent" to "100", "modulus" to "7"), mapOf("Result" to "4"))
        )
    ) { values ->
        val base = ToolSupport.big(values, "base", TWO_LIMIT)
        val exponent = ToolSupport.big(values, "exponent", TWO_LIMIT)
        val modulus = ToolSupport.big(values, "modulus", TWO_LIMIT)
        if (exponent.signum() < 0) throw MathException.invalid("The exponent must be zero or greater.")
        if (modulus <= BigInteger.ONE) throw MathException.invalid("The modulus must be greater than 1.")
        val result = base.mod(modulus).modPow(exponent, modulus)
        ToolSupport.result(
            "$result",
            "Result" to result.toString(),
            steps = listOf("Reduce the base modulo $modulus, then use binary exponentiation.")
        )
    }

    val all: List<QuickTool> = listOf(
        gcdLcm, primeCheck, primeFactorization, factors, divisorsOfGcd, commonMultiples, divisibility,
        coprime, modularArithmetic, integerRoots, digits, trailingZeros, primesUpTo, modularExponentiation
    )
}
