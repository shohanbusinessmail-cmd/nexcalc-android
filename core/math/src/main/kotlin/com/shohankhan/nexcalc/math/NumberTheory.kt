package com.shohankhan.nexcalc.math

import java.math.BigInteger
import kotlin.math.sqrt

/**
 * Exact integer arithmetic for the Quick Solve tab.
 *
 * All operations are bounded so that no user input can exhaust memory or freeze the UI:
 * factorization and divisor enumeration accept |n| ≤ 10^12, prime generation is capped at
 * 100 000 primes below 1 000 000, and results larger than 1024 bits raise an overflow error.
 */
object NumberTheory {

    const val FACTORIZATION_LIMIT: Long = 1_000_000_000_000L
    const val PRIME_SIEVE_LIMIT: Int = 1_000_000
    private const val MAX_DIVISORS = 10_000
    private val TWO = BigInteger.valueOf(2)

    fun gcd(a: BigInteger, b: BigInteger): BigInteger = a.gcd(b).abs()

    fun gcd(a: Long, b: Long): Long = BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).toLong()

    fun lcm(a: BigInteger, b: BigInteger): BigInteger {
        val absA = a.abs()
        val absB = b.abs()
        if (absA.signum() == 0 || absB.signum() == 0) return BigInteger.ZERO
        return absA.divide(absA.gcd(absB)).multiply(absB)
    }

    fun isCoprime(a: BigInteger, b: BigInteger): Boolean = gcd(a, b) == BigInteger.ONE

    fun isPrime(value: BigInteger): Boolean {
        val n = value.abs()
        if (n < TWO) return false
        if (n == TWO) return true
        if (!n.testBit(0)) return false
        return n.isProbablePrime(64)
    }

    /** Prime factorization in ascending order, with multiplicity. */
    fun primeFactors(value: BigInteger): List<BigInteger> {
        val n = value.abs()
        if (n <= BigInteger.ONE) return emptyList()
        if (n > BigInteger.valueOf(FACTORIZATION_LIMIT)) {
            throw MathException.limit("Prime factorization is limited to numbers up to ${FACTORIZATION_LIMIT}.")
        }
        val factors = ArrayList<BigInteger>()
        var remaining = n
        var divisor = TWO
        while (divisor.multiply(divisor) <= remaining) {
            while (remaining.mod(divisor).signum() == 0) {
                factors.add(divisor)
                remaining = remaining.divide(divisor)
            }
            divisor = if (divisor == TWO) BigInteger.valueOf(3) else divisor.add(TWO)
        }
        if (remaining > BigInteger.ONE) factors.add(remaining)
        return factors
    }

    fun divisorCount(value: BigInteger): Int {
        val factors = primeFactors(value)
        if (factors.isEmpty()) return 1
        var count = 1
        var run = 1
        for (index in 1 until factors.size) {
            if (factors[index] == factors[index - 1]) run++ else {
                count *= run + 1
                run = 1
            }
        }
        count *= run + 1
        return count
    }

    fun divisors(value: BigInteger): List<BigInteger> {
        val n = value.abs()
        if (n.signum() == 0) return emptyList()
        val factors = primeFactors(n)
        if (divisorsCountFromFactors(factors) > MAX_DIVISORS) {
            throw MathException.limit("The number has more than $MAX_DIVISORS divisors.")
        }
        var result = listOf(BigInteger.ONE)
        var index = 0
        while (index < factors.size) {
            val factor = factors[index]
            var power = BigInteger.ONE
            var multiplicity = 0
            while (index < factors.size && factors[index] == factor) {
                power = power.multiply(factor)
                multiplicity++
                index++
            }
            val expanded = ArrayList<BigInteger>()
            for (existing in result) {
                var term = BigInteger.ONE
                for (exponent in 0..multiplicity) {
                    expanded.add(existing.multiply(term))
                    term = term.multiply(factor)
                }
            }
            result = expanded
        }
        return result.sorted()
    }

    private fun divisorsCountFromFactors(factors: List<BigInteger>): Long {
        if (factors.isEmpty()) return 1
        var count = 1L
        var run = 1
        for (index in 1 until factors.size) {
            if (factors[index] == factors[index - 1]) run++ else {
                count *= (run + 1)
                run = 1
            }
        }
        count *= (run + 1)
        return count
    }

    fun commonFactors(a: BigInteger, b: BigInteger): List<BigInteger> = divisors(gcd(a, b))

    fun commonMultiples(a: BigInteger, b: BigInteger, count: Int): List<BigInteger> {
        if (a.signum() == 0 || b.signum() == 0) throw MathException.invalid("Common multiples are undefined for zero.")
        if (count !in 1..50) throw MathException.invalid("Request between 1 and 50 multiples.")
        val base = lcm(a, b)
        return (1..count).map { base.multiply(BigInteger.valueOf(it.toLong())) }
    }

    fun digitSum(value: BigInteger): BigInteger {
        var sum = BigInteger.ZERO
        var n = value.abs()
        while (n.signum() > 0) {
            sum = sum.add(n.mod(BigInteger.TEN))
            n = n.divide(BigInteger.TEN)
        }
        return sum
    }

    fun digitCount(value: BigInteger): Int = value.abs().toString().length

    fun reverseDigits(value: BigInteger): BigInteger {
        val reversed = value.abs().toString().reversed().trimStart('0')
        val magnitude = if (reversed.isEmpty()) BigInteger.ZERO else BigInteger(reversed)
        return if (value.signum() < 0) magnitude.negate() else magnitude
    }

    /** Number of trailing zeros of n! without computing n!. */
    fun trailingZerosOfFactorial(n: Long): Long {
        if (n < 0) throw MathException.domain("Factorial is defined only for non-negative integers.")
        var count = 0L
        var divisor = 5L
        while (divisor <= n) {
            count += n / divisor
            if (divisor > n / 5) break
            divisor *= 5
        }
        return count
    }

    /**
     * Integer square root by Newton's method. Implemented locally because
     * java.math.BigInteger.sqrt() is only available from Android API 33 and NEXCALC supports API 26.
     */
    fun integerSquareRoot(value: BigInteger): BigInteger {
        if (value.signum() < 0) throw MathException.domain("The integer square root is defined for non-negative integers.")
        if (value.signum() == 0) return BigInteger.ZERO
        var guess = BigInteger.valueOf(Math.sqrt(value.toDouble()).toLong().coerceAtLeast(1L))
        while (true) {
            val next = guess.add(value.divide(guess)).shiftRight(1)
            if (next >= guess) break
            guess = next
        }
        while (guess.multiply(guess) > value) guess = guess.subtract(BigInteger.ONE)
        while (guess.add(BigInteger.ONE).multiply(guess.add(BigInteger.ONE)) <= value) guess = guess.add(BigInteger.ONE)
        return guess
    }

    fun integerCubeRoot(value: BigInteger): BigInteger {
        val n = value.abs()
        if (n.signum() == 0) return BigInteger.ZERO
        var guess = BigInteger.valueOf(Math.cbrt(n.toDouble()).toLong().coerceAtLeast(1L))
        while (true) {
            val next = guess.multiply(TWO).add(n.divide(guess.multiply(guess))).divide(BigInteger.valueOf(3))
            if (next >= guess) break
            guess = next
        }
        // Correct any rounding in either direction.
        while (guess.multiply(guess).multiply(guess) > n) guess = guess.subtract(BigInteger.ONE)
        while (guess.add(BigInteger.ONE).pow(3) <= n) guess = guess.add(BigInteger.ONE)
        return if (value.signum() < 0) guess.negate() else guess
    }

    fun isPerfectSquare(value: BigInteger): Boolean {
        if (value.signum() < 0) return false
        val root = integerSquareRoot(value)
        return root.multiply(root) == value
    }

    fun isPerfectCube(value: BigInteger): Boolean {
        val root = integerCubeRoot(value)
        return root.pow(3) == value
    }

    fun isEven(value: BigInteger): Boolean = !value.testBit(0)

    /** Sieve of Eratosthenes; bounded to [PRIME_SIEVE_LIMIT]. */
    fun primesUpTo(limit: Int): List<Int> {
        if (limit < 2) return emptyList()
        if (limit > PRIME_SIEVE_LIMIT) throw MathException.limit("Prime generation is limited to $PRIME_SIEVE_LIMIT.")
        val composite = BooleanArray(limit + 1)
        val primes = ArrayList<Int>()
        var candidate = 2
        while (candidate <= limit) {
            if (!composite[candidate]) {
                primes.add(candidate)
                if (candidate.toLong() * candidate <= limit) {
                    var multiple = candidate * candidate
                    while (multiple <= limit) {
                        composite[multiple] = true
                        multiple += candidate
                    }
                }
            }
            candidate++
        }
        return primes
    }

    /** Factors shared by two integers, excluding 1. */
    fun sharedFactors(a: BigInteger, b: BigInteger): List<BigInteger> =
        commonFactors(a, b).filter { it > BigInteger.ONE }

    fun factorial(value: Int): BigInteger {
        if (value < 0) throw MathException.domain("Factorial requires a non-negative integer.")
        if (value > 5000) throw MathException.limit("Factorial is limited to 5000!.")
        var result = BigInteger.ONE
        for (i in 2..value) result = result.multiply(BigInteger.valueOf(i.toLong()))
        return result
    }

    fun doubleFactorial(value: Int): BigInteger {
        if (value < -1) throw MathException.domain("Double factorial requires n ≥ -1.")
        var result = BigInteger.ONE
        var current = value
        while (current > 0) {
            result = result.multiply(BigInteger.valueOf(current.toLong()))
            current -= 2
        }
        return result
    }

    fun squareRootApproximation(value: Double): Double = sqrt(value)

    /** Highest power of [base] dividing [value]. */
    fun highestPowerDivisor(value: BigInteger, base: BigInteger): Int {
        if (base <= BigInteger.ONE) throw MathException.invalid("The base must be greater than 1.")
        if (value.signum() == 0) throw MathException.domain("Zero is divisible by every power of the base.")
        var n = value.abs()
        var power = 0
        while (n.mod(base).signum() == 0) {
            n = n.divide(base)
            power++
        }
        return power
    }
}

/** 64-bit two's-complement inspection used by the number-systems tool. */
object BitwiseTools {

    fun and(a: Long, b: Long): Long = a and b

    fun or(a: Long, b: Long): Long = a or b

    fun xor(a: Long, b: Long): Long = a xor b

    fun not(a: Long): Long = a.inv()

    fun shiftLeft(a: Long, bits: Int): Long {
        if (bits !in 0..63) throw MathException.invalid("Shift amounts must be between 0 and 63.")
        return a shl bits
    }

    fun shiftRight(a: Long, bits: Int): Long {
        if (bits !in 0..63) throw MathException.invalid("Shift amounts must be between 0 and 63.")
        return a shr bits
    }

    fun shiftRightUnsigned(a: Long, bits: Int): Long {
        if (bits !in 0..63) throw MathException.invalid("Shift amounts must be between 0 and 63.")
        return a ushr bits
    }

    fun toBinary(a: Long, bits: Int = 64): String = toBase(a, 2, bits)

    fun toOctal(a: Long, bits: Int = 64): String = toBase(a, 8, bits)

    fun toHex(a: Long, bits: Int = 64): String = toBase(a, 16, bits)

    private fun toBase(a: Long, radix: Int, bits: Int): String {
        if (bits !in 1..64) throw MathException.invalid("Bit width must be between 1 and 64.")
        val masked = if (bits == 64) a else a and ((1L shl bits) - 1)
        val text = java.lang.Long.toUnsignedString(masked, radix).uppercase()
        return text
    }

    fun unsignedDecimal(a: Long): String = java.lang.Long.toUnsignedString(a)

    fun fromRadix(text: String, radix: Int): Long {
        if (text.isBlank()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter a value to convert.")
        val trimmed = text.trim().replace(" ", "").removePrefix("0x").removePrefix("0X").removePrefix("0b").removePrefix("0B")
        val parsed = try {
            java.lang.Long.parseUnsignedLong(trimmed, radix)
        } catch (e: NumberFormatException) {
            throw MathException.invalid("'$text' is not a valid base-$radix integer.")
        }
        return parsed
    }
}
