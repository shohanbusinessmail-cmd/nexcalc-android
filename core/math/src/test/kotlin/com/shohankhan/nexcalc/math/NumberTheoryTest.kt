package com.shohankhan.nexcalc.math

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberTheoryTest {

    @Test
    fun `gcd and lcm satisfy the product identity`() {
        val pairs = listOf(12L to 18L, 7L to 13L, 100L to 75L, 1L to 1L, 0L to 5L)
        for ((a, b) in pairs) {
            val bigA = BigInteger.valueOf(a)
            val bigB = BigInteger.valueOf(b)
            val gcd = NumberTheory.gcd(bigA, bigB)
            val lcm = NumberTheory.lcm(bigA, bigB)
            if (a != 0L && b != 0L) {
                assertEquals(bigA.multiply(bigB).abs(), gcd.multiply(lcm), "identity failed for $a and $b")
            }
            assertTrue(gcd.signum() >= 0)
        }
        assertEquals(BigInteger.valueOf(6), NumberTheory.gcd(BigInteger.valueOf(-48), BigInteger.valueOf(18)))
        assertEquals(BigInteger.valueOf(144), NumberTheory.lcm(BigInteger.valueOf(-48), BigInteger.valueOf(18)))
    }

    @Test
    fun `primality agrees with trial division on a range`() {
        for (n in 2..500) {
            var expected = true
            var divisor = 2
            while (divisor * divisor <= n) {
                if (n % divisor == 0) expected = false
                divisor++
            }
            assertEquals(expected, NumberTheory.isPrime(BigInteger.valueOf(n.toLong())), "primality mismatch for $n")
        }
        assertTrue(NumberTheory.isPrime(BigInteger("1000000007")))
        assertFalse(NumberTheory.isPrime(BigInteger("1000000009").multiply(BigInteger("3"))))
    }

    @Test
    fun `factorization multiplies back to the original`() {
        for (n in 2L..200L) {
            val factors = NumberTheory.primeFactors(BigInteger.valueOf(n))
            val product = factors.fold(BigInteger.ONE) { acc, value -> acc.multiply(value) }
            assertEquals(BigInteger.valueOf(n), product, "factorization of $n")
            assertTrue(factors.all { NumberTheory.isPrime(it) })
        }
        assertEquals(3, NumberTheory.primeFactors(BigInteger.valueOf(360)).filter { it == BigInteger.valueOf(2) }.size)
    }

    @Test
    fun `divisors are complete and sum correctly`() {
        val divisors = NumberTheory.divisors(BigInteger.valueOf(36))
        assertEquals(listOf(1, 2, 3, 4, 6, 9, 12, 18, 36), divisors.map { it.toInt() })
        assertEquals(9, NumberTheory.divisorCount(BigInteger.valueOf(36)))
        assertEquals(listOf(1, 2, 3, 6), NumberTheory.commonFactors(BigInteger.valueOf(12), BigInteger.valueOf(18)).map { it.toInt() })
        assertEquals(listOf(2, 3, 6), NumberTheory.sharedFactors(BigInteger.valueOf(12), BigInteger.valueOf(18)).map { it.toInt() })
    }

    @Test
    fun `bounds protect the application from expensive inputs`() {
        val tooLarge = BigInteger.valueOf(NumberTheory.FACTORIZATION_LIMIT).add(BigInteger.ONE)
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> { NumberTheory.primeFactors(tooLarge) }.code)
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> { NumberTheory.primesUpTo(2_000_000) }.code)
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> { NumberTheory.factorial(5001) }.code)
    }

    @Test
    fun `digit helpers handle signs and zeros`() {
        assertEquals(BigInteger.valueOf(10), NumberTheory.digitSum(BigInteger.valueOf(1234)))
        assertEquals(4, NumberTheory.digitCount(BigInteger.valueOf(-1234)))
        assertEquals(BigInteger.valueOf(4321), NumberTheory.reverseDigits(BigInteger.valueOf(1234)))
        assertEquals(BigInteger.valueOf(-507), NumberTheory.reverseDigits(BigInteger.valueOf(-705)))
        assertEquals(BigInteger.ZERO, NumberTheory.reverseDigits(BigInteger.ZERO))
    }

    @Test
    fun `trailing zeros of factorial`() {
        assertEquals(0, NumberTheory.trailingZerosOfFactorial(4))
        assertEquals(1, NumberTheory.trailingZerosOfFactorial(5))
        assertEquals(24, NumberTheory.trailingZerosOfFactorial(100))
        assertEquals(249, NumberTheory.trailingZerosOfFactorial(1000))
    }

    @Test
    fun `integer roots are exact for perfect powers`() {
        assertEquals(BigInteger.valueOf(12), NumberTheory.integerSquareRoot(BigInteger.valueOf(144)))
        assertEquals(BigInteger.valueOf(11), NumberTheory.integerSquareRoot(BigInteger.valueOf(143)))
        assertEquals(BigInteger.valueOf(3), NumberTheory.integerCubeRoot(BigInteger.valueOf(27)))
        assertEquals(BigInteger.valueOf(-3), NumberTheory.integerCubeRoot(BigInteger.valueOf(-27)))
        assertTrue(NumberTheory.isPerfectSquare(BigInteger.valueOf(1024)))
        assertFalse(NumberTheory.isPerfectSquare(BigInteger.valueOf(1025)))
        assertTrue(NumberTheory.isPerfectCube(BigInteger.valueOf(1000)))
        assertFalse(NumberTheory.isPerfectCube(BigInteger.valueOf(999)))
        assertEquals(BigInteger.valueOf(1_000_000), NumberTheory.integerSquareRoot(BigInteger.valueOf(1_000_000_000_000)))
    }

    @Test
    fun `sieve matches the first primes`() {
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29), NumberTheory.primesUpTo(30))
        assertEquals(25, NumberTheory.primesUpTo(100).size)
        assertTrue(NumberTheory.primesUpTo(1).isEmpty())
    }

    @Test
    fun `common multiples are multiples of the lcm`() {
        val multiples = NumberTheory.commonMultiples(BigInteger.valueOf(4), BigInteger.valueOf(6), 5)
        assertEquals(listOf(12, 24, 36, 48, 60), multiples.map { it.toInt() })
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            NumberTheory.commonMultiples(BigInteger.ZERO, BigInteger.ONE, 5)
        }.code)
    }

    @Test
    fun `factorial and double factorial`() {
        assertEquals(BigInteger.valueOf(120), NumberTheory.factorial(5))
        assertEquals(BigInteger.valueOf(15), NumberTheory.doubleFactorial(5))
        assertEquals(BigInteger.ONE, NumberTheory.doubleFactorial(0))
    }

    @Test
    fun `highest power divisor`() {
        assertEquals(3, NumberTheory.highestPowerDivisor(BigInteger.valueOf(8), BigInteger.valueOf(2)))
        assertEquals(2, NumberTheory.highestPowerDivisor(BigInteger.valueOf(72), BigInteger.valueOf(2)))
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> {
            NumberTheory.highestPowerDivisor(BigInteger.ZERO, BigInteger.valueOf(2))
        }.code)
    }
}

class RationalTest {

    @Test
    fun `fractions are normalised`() {
        assertEquals("3/4", Rational.parse("18/24").toString())
        assertEquals("3/4", Rational.parse("0.75").toString())
        assertEquals("-3/4", Rational.parse("-18/24").toString())
        assertEquals("-3/4", Rational.parse("18/-24").toString())
        assertEquals("5", Rational.parse("10/2").toString())
    }

    @Test
    fun `arithmetic is exact`() {
        assertEquals("1/2", Rational.parse("1/3").plus(Rational.parse("1/6")).toString())
        assertEquals("-1/12", Rational.parse("1/3").minus(Rational.parse("5/12")).toString())
        assertEquals("3/2", Rational.parse("2/3").times(Rational.parse("9/4")).toString())
        assertEquals("15/8", Rational.parse("3/4").div(Rational.parse("2/5")).toString())
    }

    @Test
    fun `mixed numbers round trip`() {
        assertEquals("2 1/3", Rational.parse("7/3").mixedString())
        assertEquals("-3 2/5", Rational.parse("-17/5").mixedString())
        assertEquals("11/4", Rational.parseMixed("2 3/4").toString())
        assertEquals("-3/2", Rational.parseMixed("-1 1/2").toString())
    }

    @Test
    fun `invalid fractions are rejected`() {
        assertEquals(MathErrorCode.DIVIDE_BY_ZERO, assertFailsWith<MathException> { Rational.parse("1/0") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Rational.parse("a/b") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Rational.parseMixed("2 5/4") }.code)
    }

    @Test
    fun `comparison and decimal conversion`() {
        assertTrue(Rational.parse("3/4") > Rational.parse("5/7"))
        assertEquals(0.125, Rational.parse("1/8").toDouble(), 1e-15)
        assertEquals(1, Rational.parse("3/4").compareTo(Rational.parse("5/7")))
    }
}

class BitwiseToolsTest {

    @Test
    fun `bitwise operations use 64-bit two's complement`() {
        assertEquals(4L, BitwiseTools.and(12L, 6L))
        assertEquals(14L, BitwiseTools.or(12L, 6L))
        assertEquals(10L, BitwiseTools.xor(12L, 6L))
        assertEquals(-1L, BitwiseTools.not(0L))
        assertEquals(-2L, BitwiseTools.not(1L))
        assertEquals(2L, BitwiseTools.shiftRight(8L, 2))
        assertEquals(32L, BitwiseTools.shiftLeft(8L, 2))
        assertEquals(-1L, BitwiseTools.shiftRight(-1L, 10))
        assertEquals(18014398509481983L, BitwiseTools.shiftRightUnsigned(-1L, 10))
    }

    @Test
    fun `base conversions round trip`() {
        assertEquals("FF", BitwiseTools.toHex(255L, 8))
        assertEquals("11111111", BitwiseTools.toBinary(255L, 8))
        assertEquals("377", BitwiseTools.toOctal(255L, 16))
        assertEquals(255L, BitwiseTools.fromRadix("FF", 16))
        assertEquals(-1L, BitwiseTools.fromRadix("FFFF", 16).toShort().toLong())
        assertEquals("18446744073709551615", BitwiseTools.unsignedDecimal(-1L))
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { BitwiseTools.shiftLeft(1L, 64) }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { BitwiseTools.fromRadix("xyz", 16) }.code)
    }
}
