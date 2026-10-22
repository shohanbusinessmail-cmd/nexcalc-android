package com.shohankhan.nexcalc.math

import java.math.BigInteger

/**
 * Exact rational number backed by arbitrary-precision integers.
 *
 * Used wherever fractions must not be rounded: the fraction tools, ratio simplification and
 * exact combinatorial helpers. Values are always stored in lowest terms with a positive
 * denominator.
 */
class Rational private constructor(val numerator: BigInteger, val denominator: BigInteger) : Comparable<Rational> {

    val isInteger: Boolean get() = denominator == BigInteger.ONE

    val isZero: Boolean get() = numerator.signum() == 0

    val signum: Int get() = numerator.signum()

    operator fun plus(other: Rational): Rational {
        val gcd = denominator.gcd(other.denominator)
        val leftFactor = other.denominator.divide(gcd)
        val rightFactor = denominator.divide(gcd)
        return of(numerator.multiply(leftFactor).add(other.numerator.multiply(rightFactor)), denominator.multiply(leftFactor))
    }

    operator fun minus(other: Rational): Rational = plus(other.negate())

    operator fun times(other: Rational): Rational = of(numerator.multiply(other.numerator), denominator.multiply(other.denominator))

    operator fun div(other: Rational): Rational {
        if (other.isZero) throw MathException.divideByZero("Cannot divide by the fraction 0.")
        return of(numerator.multiply(other.denominator), denominator.multiply(other.numerator))
    }

    fun negate(): Rational = Rational(numerator.negate(), denominator)

    fun reciprocal(): Rational {
        if (isZero) throw MathException.divideByZero("Zero has no reciprocal.")
        return of(denominator, numerator)
    }

    fun abs(): Rational = if (signum < 0) negate() else this

    fun toDouble(): Double = numerator.toDouble() / denominator.toDouble()

    /** True when the value is an integer. */
    fun integerValue(): BigInteger {
        if (!isInteger) throw MathException.invalid("The fraction is not an integer.")
        return numerator
    }

    fun mixedString(): String {
        if (isInteger) return numerator.toString()
        val sign = if (signum < 0) "-" else ""
        val absoluteNumerator = numerator.abs()
        val whole = absoluteNumerator.divide(denominator)
        val remainder = absoluteNumerator.mod(denominator)
        return if (whole.signum() == 0) "$sign$remainder/$denominator"
        else "$sign$whole $remainder/$denominator"
    }

    override fun compareTo(other: Rational): Int =
        numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator))

    override fun equals(other: Any?): Boolean =
        other is Rational && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String = if (isInteger) numerator.toString() else "$numerator/$denominator"

    companion object {
        fun of(numerator: BigInteger, denominator: BigInteger): Rational {
            if (denominator.signum() == 0) throw MathException.divideByZero("A fraction cannot have a zero denominator.")
            if (numerator.signum() == 0) return Rational(BigInteger.ZERO, BigInteger.ONE)
            val sign = if (denominator.signum() < 0) -1 else 1
            val gcd = numerator.gcd(denominator)
            return Rational(numerator.divide(gcd).multiply(BigInteger.valueOf(sign.toLong())), denominator.divide(gcd).abs())
        }

        fun of(numerator: Long, denominator: Long): Rational =
            of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

        fun of(value: Long): Rational = Rational(BigInteger.valueOf(value), BigInteger.ONE)

        /** Parses `a/b`, an integer, or a decimal literal exactly. */
        fun parse(text: String): Rational {
            val trimmed = text.trim().replace(" ", "")
            if (trimmed.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter a fraction such as 3/4.")
            return if (trimmed.contains('/')) {
                val parts = trimmed.split('/')
                if (parts.size != 2) throw MathException.invalid("A fraction must contain exactly one '/'.")
                val n = parts[0].toBigIntegerOrNull() ?: throw MathException.invalid("'${parts[0]}' is not an integer.")
                val d = parts[1].toBigIntegerOrNull() ?: throw MathException.invalid("'${parts[1]}' is not an integer.")
                of(n, d)
            } else {
                val decimal = trimmed.toBigDecimalOrNull() ?: throw MathException.invalid("'$trimmed' is not a number.")
                if (decimal.scale() < 0) of(decimal.unscaledValue().multiply(BigInteger.TEN.pow(-decimal.scale())), BigInteger.ONE)
                else of(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()))
            }
        }

        /** Parses a mixed number such as `2 3/4` (or `-1 1/2`). */
        fun parseMixed(text: String): Rational {
            val trimmed = text.trim()
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size != 2 || !parts[1].contains('/')) {
                throw MathException.invalid("Enter a mixed number such as 2 3/4.")
            }
            val whole = parts[0].toBigIntegerOrNull() ?: throw MathException.invalid("'${parts[0]}' is not an integer.")
            val fraction = parse(parts[1])
            if (fraction.abs() >= Rational.of(1)) throw MathException.invalid("The fractional part must be less than 1.")
            return Rational(whole, BigInteger.ONE).plus(if (whole.signum() < 0) fraction.negate() else fraction)
        }

        private fun String.toBigIntegerOrNull(): BigInteger? = try {
            BigInteger(this)
        } catch (e: NumberFormatException) {
            null
        }
    }
}
