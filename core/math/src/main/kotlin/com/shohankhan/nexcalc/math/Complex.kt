package com.shohankhan.nexcalc.math

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Immutable complex number used for every engine-level numeric value.
 *
 * Real-valued operations simply carry an imaginary part of exactly zero. Functions that are only
 * defined for real arguments (for example `floor`) reject values with a non-zero imaginary part by
 * raising [MathErrorCode.DOMAIN] instead of silently discarding it.
 */
data class Complex(val re: Double, val im: Double = 0.0) {

    val isReal: Boolean get() = im == 0.0

    val isFinite: Boolean get() = re.isFinite() && im.isFinite()

    fun plus(other: Complex) = Complex(re + other.re, im + other.im)

    fun minus(other: Complex) = Complex(re - other.re, im - other.im)

    fun times(other: Complex) = Complex(
        re * other.re - im * other.im,
        re * other.im + im * other.re
    )

    fun negate() = Complex(-re, -im)

    fun divide(other: Complex): Complex {
        val denom = other.re * other.re + other.im * other.im
        if (denom == 0.0 || !denom.isFinite()) throw MathException.divideByZero()
        return Complex(
            (re * other.re + im * other.im) / denom,
            (im * other.re - re * other.im) / denom
        )
    }

    fun conjugate() = Complex(re, -im)

    /** Modulus |z|. */
    fun modulus(): Double = hypot(re, im)

    /** Principal argument in radians, in (-pi, pi]. */
    fun argument(): Double = atan2(im, re)

    /** Integer power using exponentiation by squaring (exact for real integers within range). */
    fun powInteger(exponent: Int): Complex {
        if (exponent == 0) return ONE
        var base = if (exponent < 0) ONE.divide(this) else this
        var exp = if (exponent < 0) -exponent.toLong() else exponent.toLong()
        var result = ONE
        while (exp > 0) {
            if (exp and 1L == 1L) result = result.times(base)
            exp = exp shr 1
            if (exp > 0) base = base.times(base)
        }
        return result
    }

    /**
     * General complex power using the principal branch: z^w = exp(w * Ln z).
     * Zero raised to a positive real power is zero; zero raised to a non-positive power raises
     * [MathErrorCode.DOMAIN] because no branch value is defined for it.
     */
    fun pow(other: Complex): Complex {
        if (this == ZERO) {
            if (other.im == 0.0 && other.re > 0.0) return ZERO
            throw MathException.domain("0 raised to a non-positive power is undefined.")
        }
        if (this.isReal && other.isReal && this.re > 0.0) {
            val v = Math.pow(this.re, other.re)
            if (v.isInfinite()) throw MathException.overflow("The power is too large to represent.")
            return Complex(v)
        }
        val lnMod = ln(modulus())
        val theta = argument()
        val realPart = other.re * lnMod - other.im * theta
        val imagPart = other.re * theta + other.im * lnMod
        val magnitude = exp(realPart)
        if (magnitude.isInfinite()) throw MathException.overflow("The power is too large to represent.")
        return Complex(magnitude * cos(imagPart), magnitude * sin(imagPart))
    }

    fun sqrt(): Complex {
        if (isReal && re >= 0.0) return Complex(sqrt(re))
        val m = modulus()
        val realPart = sqrt((m + re) / 2.0)
        val imagPart = if (im < 0) -sqrt((m - re) / 2.0) else sqrt((m - re) / 2.0)
        return Complex(realPart, imagPart)
    }

    fun exp(): Complex {
        val magnitude = exp(re)
        if (magnitude.isInfinite()) throw MathException.overflow("The exponential is too large to represent.")
        return Complex(magnitude * cos(im), magnitude * sin(im))
    }

    /** Principal natural logarithm ln|z| + i·arg(z). */
    fun ln(): Complex {
        if (this == ZERO) throw MathException.domain("The logarithm of zero is undefined.")
        return Complex(ln(modulus()), argument())
    }

    /** Requires a real value; raises a domain error otherwise. */
    fun requireReal(operation: String): Double {
        if (im != 0.0) {
            throw MathException.domain("$operation is only defined for real numbers.")
        }
        return re
    }

    fun isApproximately(other: Complex, tolerance: Double): Boolean =
        kotlin.math.abs(re - other.re) <= tolerance && kotlin.math.abs(im - other.im) <= tolerance

    override fun toString(): String {
        if (isReal) return re.toString()
        val sign = if (im < 0) "-" else "+"
        return "$re $sign ${kotlin.math.abs(im)}i"
    }

    companion object {
        val ZERO = Complex(0.0)
        val ONE = Complex(1.0)
        val I = Complex(0.0, 1.0)

        fun of(value: Double) = Complex(value)
    }
}
