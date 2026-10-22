package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Outcome of an iterative numerical method.
 *
 * @property converged true only when the stated criterion was met. A false value is a failure the
 *   UI must present as such; no method ever returns a value while pretending to have converged.
 */
data class NumericSolution(
    val converged: Boolean,
    val root: Double?,
    val iterations: Int,
    val residual: Double?,
    val message: String,
    val method: String
)

data class IntegrationResult(
    val value: Double,
    val estimatedError: Double,
    val evaluations: Int,
    val converged: Boolean,
    val message: String
)

data class LimitResult(
    val value: Double?,
    val leftApproach: Double?,
    val rightApproach: Double?,
    val converged: Boolean,
    val message: String
)

/**
 * Numerical analysis used by the Scientific and Tools tabs.
 *
 * Every method validates its inputs, bounds its iteration count and reports non-convergence
 * explicitly. Methods never loop without a hard upper bound.
 */
object Solvers {

    private const val MIN_TOLERANCE = 1e-14
    private const val MAX_TOLERANCE = 1.0
    private const val MAX_ITERATIONS = 10_000
    private const val MAX_INTEGRATION_DEPTH = 22

    fun bisection(
        f: (Double) -> Double,
        lower: Double,
        upper: Double,
        tolerance: Double = 1e-10,
        maxIterations: Int = 200
    ): NumericSolution {
        requireTolerance(tolerance)
        requireIterations(maxIterations)
        if (lower >= upper) throw MathException.invalid("The lower bound must be smaller than the upper bound.")
        var a = lower
        var b = upper
        var fa = safe(f, a)
        val fb = safe(f, b)
        if (fa == 0.0) return NumericSolution(true, a, 0, 0.0, "Exact root at the lower bound.", "Bisection")
        if (fb == 0.0) return NumericSolution(true, b, 0, 0.0, "Exact root at the upper bound.", "Bisection")
        if (fa * fb > 0) {
            throw MathException.domain("f(x) has the same sign at both bounds, so bisection cannot bracket a root.")
        }
        var iterations = 0
        var midpoint = (a + b) / 2.0
        while (iterations < maxIterations) {
            midpoint = (a + b) / 2.0
            val fm = safe(f, midpoint)
            iterations++
            if (fm == 0.0 || (b - a) / 2.0 < tolerance) {
                return NumericSolution(true, midpoint, iterations, abs(fm), "Converged within the requested tolerance.", "Bisection")
            }
            if (fa * fm < 0) {
                b = midpoint
            } else {
                a = midpoint
                fa = fm
            }
        }
        return NumericSolution(false, midpoint, iterations, abs(safe(f, midpoint)), "Bisection reached the iteration limit without meeting the tolerance.", "Bisection")
    }

    fun newtonRaphson(
        f: (Double) -> Double,
        derivative: (Double) -> Double,
        initialGuess: Double,
        tolerance: Double = 1e-10,
        maxIterations: Int = 100
    ): NumericSolution {
        requireTolerance(tolerance)
        requireIterations(maxIterations)
        var x = initialGuess
        var iterations = 0
        while (iterations < maxIterations) {
            val fx = safe(f, x)
            val dfx = safe(derivative, x)
            iterations++
            if (abs(fx) < tolerance) {
                return NumericSolution(true, x, iterations, abs(fx), "Converged: residual is within tolerance.", "Newton–Raphson")
            }
            if (abs(dfx) < 1e-14) {
                return NumericSolution(false, x, iterations, abs(fx), "The derivative is approximately zero; Newton–Raphson cannot continue.", "Newton–Raphson")
            }
            val next = x - fx / dfx
            if (!next.isFinite()) {
                return NumericSolution(false, x, iterations, abs(fx), "The iteration left the representable range.", "Newton–Raphson")
            }
            if (abs(next - x) < tolerance && abs(safe(f, next)) < max(tolerance, 1e-9)) {
                return NumericSolution(true, next, iterations, abs(safe(f, next)), "Converged.", "Newton–Raphson")
            }
            x = next
        }
        return NumericSolution(false, x, iterations, abs(safe(f, x)), "Newton–Raphson reached the iteration limit.", "Newton–Raphson")
    }

    fun secant(
        f: (Double) -> Double,
        first: Double,
        second: Double,
        tolerance: Double = 1e-10,
        maxIterations: Int = 200
    ): NumericSolution {
        requireTolerance(tolerance)
        requireIterations(maxIterations)
        var x0 = first
        var x1 = second
        var f0 = safe(f, x0)
        var f1 = safe(f, x1)
        if (abs(f0) < tolerance) return NumericSolution(true, x0, 0, abs(f0), "The first guess is already a root.", "Secant")
        if (abs(f1) < tolerance) return NumericSolution(true, x1, 0, abs(f1), "The second guess is already a root.", "Secant")
        var iterations = 0
        while (iterations < maxIterations) {
            if (abs(f1 - f0) < 1e-300) {
                return NumericSolution(false, x1, iterations, abs(f1), "The function values are identical; the secant slope is undefined.", "Secant")
            }
            val next = x1 - f1 * (x1 - x0) / (f1 - f0)
            if (!next.isFinite()) return NumericSolution(false, x1, iterations, abs(f1), "The iteration left the representable range.", "Secant")
            x0 = x1
            f0 = f1
            x1 = next
            f1 = safe(f, x1)
            iterations++
            if (abs(f1) < tolerance || abs(x1 - x0) < tolerance) {
                return NumericSolution(true, x1, iterations, abs(f1), "Converged.", "Secant")
            }
        }
        return NumericSolution(false, x1, iterations, abs(f1), "Secant reached the iteration limit.", "Secant")
    }

    /** Five-point central difference; accurate to O(h⁴). */
    fun derivative(f: (Double) -> Double, x: Double, h: Double = 1e-4): Double {
        if (h <= 0 || !h.isFinite()) throw MathException.invalid("The step size must be a positive number.")
        val scaled = h * max(1.0, abs(x))
        return (-safe(f, x + 2 * scaled) + 8 * safe(f, x + scaled) - 8 * safe(f, x - scaled) + safe(f, x - 2 * scaled)) / (12 * scaled)
    }

    fun secondDerivative(f: (Double) -> Double, x: Double, h: Double = 1e-4): Double {
        if (h <= 0 || !h.isFinite()) throw MathException.invalid("The step size must be a positive number.")
        val scaled = h * max(1.0, abs(x))
        return (safe(f, x + scaled) - 2 * safe(f, x) + safe(f, x - scaled)) / (scaled * scaled)
    }

    /** Adaptive Simpson integration with an error estimate and bounded recursion. */
    fun integrate(
        f: (Double) -> Double,
        lower: Double,
        upper: Double,
        tolerance: Double = 1e-8
    ): IntegrationResult {
        requireTolerance(tolerance)
        if (!lower.isFinite() || !upper.isFinite()) throw MathException.invalid("The integration bounds must be finite.")
        if (lower == upper) return IntegrationResult(0.0, 0.0, 1, true, "The bounds are identical, so the integral is zero.")
        var evaluations = 0
        val sign = if (upper < lower) -1.0 else 1.0
        val a = minOf(lower, upper)
        val b = maxOf(lower, upper)

        fun evaluate(x: Double): Double {
            evaluations++
            return safe(f, x)
        }

        fun simpson(left: Double, right: Double, fLeft: Double, fMiddle: Double, fRight: Double): Double =
            (right - left) / 6.0 * (fLeft + 4 * fMiddle + fRight)

        fun adaptive(
            left: Double,
            right: Double,
            fLeft: Double,
            fMiddle: Double,
            fRight: Double,
            whole: Double,
            depth: Int
        ): Pair<Double, Double> {
            val middle = (left + right) / 2.0
            val leftMiddle = (left + middle) / 2.0
            val rightMiddle = (middle + right) / 2.0
            val fLeftMiddle = evaluate(leftMiddle)
            val fRightMiddle = evaluate(rightMiddle)
            val leftPart = simpson(left, middle, fLeft, fLeftMiddle, fMiddle)
            val rightPart = simpson(middle, right, fMiddle, fRightMiddle, fRight)
            val delta = leftPart + rightPart - whole
            if (depth >= MAX_INTEGRATION_DEPTH || abs(delta) < 15 * tolerance) {
                return Pair(leftPart + rightPart + delta / 15.0, abs(delta) / 15.0)
            }
            val leftResult = adaptive(left, middle, fLeft, fLeftMiddle, fMiddle, leftPart, depth + 1)
            val rightResult = adaptive(middle, right, fMiddle, fRightMiddle, fRight, rightPart, depth + 1)
            return Pair(leftResult.first + rightResult.first, leftResult.second + rightResult.second)
        }

        val fa = evaluate(a)
        val fm = evaluate((a + b) / 2.0)
        val fb = evaluate(b)
        val whole = simpson(a, b, fa, fm, fb)
        val (value, error) = adaptive(a, b, fa, fm, fb, whole, 0)
        val converged = error <= tolerance
        val message = if (converged) "Converged within the requested tolerance."
        else "The integral did not meet the requested tolerance (estimated error ${NumberFormatter.scientific(error, NumberFormatOptions(6, 6))})."
        return IntegrationResult(sign * value, error, evaluations, converged, message)
    }

    /** Two-sided numerical limit using progressively smaller steps. */
    fun limit(f: (Double) -> Double, target: Double, tolerance: Double = 1e-8): LimitResult {
        requireTolerance(tolerance)
        val right = approach(f, target, tolerance, fromLeft = false)
        val left = approach(f, target, tolerance, fromLeft = true)
        if (left == null || right == null) {
            return LimitResult(null, left, right, false, "The function is not defined close enough to the target value.")
        }
        val difference = abs(left - right)
        val scale = max(1.0, max(abs(left), abs(right)))
        if (difference > tolerance * scale) {
            return LimitResult(null, left, right, false, "The left and right limits differ (${NumberFormatter.format(left)} vs ${NumberFormatter.format(right)}).")
        }
        return LimitResult((left + right) / 2.0, left, right, true, "The two-sided limit agrees from both directions.")
    }

    private fun approach(f: (Double) -> Double, target: Double, tolerance: Double, fromLeft: Boolean): Double? {
        var step = 1e-2
        var previous: Double? = null
        repeat(10) {
            val x = if (fromLeft) target - step else target + step
            val value = try {
                f(x)
            } catch (error: MathException) {
                return null
            }
            if (!value.isFinite()) return null
            if (previous != null && abs(value - previous!!) < tolerance * max(1.0, abs(value))) {
                return value
            }
            previous = value
            step /= 10.0
        }
        return previous
    }

    fun summation(f: (Double) -> Double, from: Long, to: Long): Double {
        if (to < from) throw MathException.invalid("The end index must not be smaller than the start index.")
        if (to - from > 1_000_000) throw MathException.limit("Summation is limited to one million terms.")
        var total = 0.0
        var index = from
        while (index <= to) {
            total += safe(f, index.toDouble())
            index++
        }
        if (!total.isFinite()) throw MathException.overflow("The sum exceeded the supported range.")
        return total
    }

    fun product(f: (Double) -> Double, from: Long, to: Long): Double {
        if (to < from) throw MathException.invalid("The end index must not be smaller than the start index.")
        if (to - from > 100_000) throw MathException.limit("Products are limited to 100 000 factors.")
        var total = 1.0
        var index = from
        while (index <= to) {
            total *= safe(f, index.toDouble())
            index++
        }
        if (!total.isFinite()) throw MathException.overflow("The product exceeded the supported range.")
        return total
    }

    fun linearEquation(a: Double, b: Double): Double {
        if (a == 0.0) {
            if (b == 0.0) throw MathException.domain("Every value of x satisfies 0·x = 0 (infinitely many solutions).")
            throw MathException.domain("The equation has no solution because a is zero.")
        }
        return -b / a
    }

    /** Roots of ax² + bx + c, returned as complex numbers (two entries, repeated roots included). */
    fun quadraticRoots(a: Double, b: Double, c: Double): List<Complex> {
        if (a == 0.0) {
            if (b == 0.0) throw MathException.domain("This is not a quadratic equation (a and b are both zero).")
            return listOf(Complex(-c / b))
        }
        val discriminant = b * b - 4 * a * c
        if (discriminant == 0.0) {
            val root = -b / (2 * a)
            return listOf(Complex(root), Complex(root))
        }
        if (discriminant > 0) {
            val sqrtDisc = sqrt(discriminant)
            // Numerically stable quadratic formula (avoids catastrophic cancellation).
            val q = -0.5 * (b + Math.copySign(sqrtDisc, b))
            val first = q / a
            val second = if (q == 0.0) first else c / q
            return listOf(Complex(first), Complex(second)).sortedBy { it.re }
        }
        val realPart = -b / (2 * a)
        val imaginaryPart = sqrt(-discriminant) / (2 * abs(a))
        return listOf(Complex(realPart, imaginaryPart), Complex(realPart, -imaginaryPart))
    }

    /**
     * All roots of a polynomial given as coefficients from highest to lowest degree.
     * Uses the closed form for degrees 1–2 and Durand–Kerner for degree ≥ 3 (bounded at degree 20).
     */
    fun polynomialRoots(coefficients: List<Double>): List<Complex> {
        if (coefficients.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter polynomial coefficients.")
        if (coefficients.any { !it.isFinite() }) throw MathException.invalid("Every coefficient must be finite.")
        val trimmed = coefficients.dropWhile { abs(it) < 1e-14 }
        if (trimmed.isEmpty()) throw MathException.domain("All coefficients are zero; every number is a root.")
        val degree = trimmed.size - 1
        if (degree == 0) throw MathException.invalid("A constant polynomial has no roots. Enter a non-zero leading coefficient.")
        if (degree > 20) throw MathException.limit("Polynomials are limited to degree 20.")
        if (degree == 1) return listOf(Complex(-trimmed[1] / trimmed[0]))
        if (degree == 2) return quadraticRoots(trimmed[0], trimmed[1], trimmed[2])

        val monic = trimmed.map { it / trimmed[0] }
        fun evaluate(z: Complex): Complex {
            var accumulator = Complex.ZERO
            for (coefficient in monic) accumulator = accumulator.times(z).plus(Complex(coefficient))
            return accumulator
        }

        val radius = 1.0 + monic.drop(1).maxOfOrNull { abs(it) }!!
        var roots = List(degree) { index ->
            val angle = 2 * Math.PI * index / degree
            Complex(radius * cos(angle) + 0.4, radius * sin(angle) + 0.4)
        }
        var converged = false
        var iterations = 0
        while (iterations < 4000 && !converged) {
            val updated = ArrayList<Complex>(degree)
            var largestStep = 0.0
            for (index in 0 until degree) {
                var denominator = Complex.ONE
                for (other in 0 until degree) {
                    if (other != index) denominator = denominator.times(roots[index].minus(roots[other]))
                }
                if (denominator.modulus() < 1e-300) {
                    updated.add(roots[index].plus(Complex(1e-6, 1e-6)))
                    largestStep = 1.0
                    continue
                }
                val delta = evaluate(roots[index]).divide(denominator)
                updated.add(roots[index].minus(delta))
                largestStep = max(largestStep, delta.modulus())
            }
            roots = updated
            converged = largestStep < 1e-13
            iterations++
        }
        if (!converged) throw MathException.convergence("Root finding did not converge for this polynomial.")
        val polished = roots.map { root ->
            var current = root
            repeat(6) {
                val value = evaluate(current)
                var derivative = Complex.ZERO
                for (index in 0 until degree) {
                    derivative = derivative.times(current).plus(Complex(monic[index] * (degree - index)))
                }
                if (derivative.modulus() < 1e-14) return@repeat
                current = current.minus(value.divide(derivative))
            }
            val tolerance = 1e-9 * max(1.0, current.modulus())
            if (abs(current.im) < tolerance) Complex(current.re, 0.0) else current
        }
        return polished.sortedWith(compareBy<Complex> { it.re }.thenBy { it.im })
    }

    /** Evaluates a polynomial (highest degree first) with Horner's method. */
    fun evaluatePolynomial(coefficients: List<Double>, x: Double): Double {
        if (coefficients.isEmpty()) throw MathException.invalid("Enter polynomial coefficients.")
        var accumulator = 0.0
        for (coefficient in coefficients) accumulator = accumulator * x + coefficient
        return accumulator
    }

    private fun safe(f: (Double) -> Double, x: Double): Double {
        val value = try {
            f(x)
        } catch (error: MathException) {
            throw error
        } catch (error: ArithmeticException) {
            throw MathException.domain("The function is undefined at x = ${NumberFormatter.format(x)}.")
        }
        if (!value.isFinite()) throw MathException.domain("The function is not finite at x = ${NumberFormatter.format(x)}.")
        return value
    }

    private fun requireTolerance(tolerance: Double) {
        if (!tolerance.isFinite() || tolerance < MIN_TOLERANCE || tolerance > MAX_TOLERANCE) {
            throw MathException.invalid("Tolerance must be between 1e-14 and 1.", "Use a tolerance such as 0.000001.")
        }
    }

    private fun requireIterations(iterations: Int) {
        if (iterations !in 1..MAX_ITERATIONS) throw MathException.invalid("Iterations must be between 1 and $MAX_ITERATIONS.")
    }
}
