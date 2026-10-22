package com.shohankhan.nexcalc.math

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/** Runtime context for evaluation: angle interpretation, bound variables and recursion bound. */
data class EvalContext(
    val angleMode: AngleMode = AngleMode.RADIANS,
    val variables: Map<String, Complex> = emptyMap(),
    val maxDepth: Int = 128
)

/**
 * Result of evaluating an expression.
 *
 * @property value numeric value (possibly complex).
 * @property exact true only when the engine can prove the value is the exact integer the user
 *   would obtain by hand. Approximate values must always be presented as such by the UI.
 */
data class EvalValue(val value: Complex, val exact: Boolean)

/**
 * Tree-walking evaluator.
 *
 * Numeric policy: values are IEEE-754 doubles wrapped in [Complex]. Exactness is *tracked* rather
 * than assumed: integer literals and integer-preserving operations stay exact while their results
 * are representable exactly (|value| ≤ 2^53); anything involving transcendental functions or a
 * lossy division is marked approximate. This keeps the UI honest about precision.
 *
 * Safety: recursion depth is bounded, operands must be finite, and every domain rule raises a
 * structured [MathException] instead of returning NaN.
 */
class Evaluator(private val context: EvalContext = EvalContext()) {

    fun evaluate(expression: String): EvalValue = evaluate(ExpressionParser.parse(expression))

    fun evaluate(expr: Expr): EvalValue = eval(expr, 0)

    private fun eval(expr: Expr, depth: Int): EvalValue {
        if (depth > context.maxDepth) {
            throw MathException.limit("The expression is nested too deeply (limit ${context.maxDepth}).")
        }
        return when (expr) {
            is Expr.Num -> literal(expr.value)
            is Expr.Const -> constant(expr.name)
            is Expr.Var -> {
                val bound = context.variables[expr.name]
                    ?: throw MathException(MathErrorCode.UNSUPPORTED, "Unknown symbol '${expr.name}'.", hint = "Assign a value to '${expr.name}' before evaluating.")
                EvalValue(bound, exact = false)
            }
            is Expr.Unary -> {
                val operand = eval(expr.operand, depth + 1)
                if (expr.op == "-") EvalValue(operand.value.negate(), operand.exact) else operand
            }
            is Expr.Postfix -> {
                val operand = eval(expr.operand, depth + 1)
                when (expr.op) {
                    "!" -> factorial(operand)
                    "%" -> percent(operand)
                    else -> throw MathException.unsupported("Unknown postfix operator '${expr.op}'.")
                }
            }
            is Expr.Binary -> {
                val left = eval(expr.left, depth + 1)
                val right = eval(expr.right, depth + 1)
                when (expr.op) {
                    "+" -> arithmetic(left, right) { a, b -> a.plus(b) }
                    "-" -> arithmetic(left, right) { a, b -> a.minus(b) }
                    "*" -> arithmetic(left, right) { a, b -> a.times(b) }
                    "/" -> divide(left, right)
                    "^" -> power(left, right)
                    else -> throw MathException.unsupported("Unknown operator '${expr.op}'.")
                }
            }
            is Expr.Call -> {
                val args = expr.args.map { eval(it, depth + 1) }
                val arity = EngineFunctions.arity[expr.name]
                    ?: throw MathException.unsupported("Unknown function '${expr.name}'.")
                if (args.size !in arity) {
                    val expected = if (arity.first == arity.last) "${arity.first}" else "${arity.first} to ${arity.last}"
                    throw MathException.invalid(
                        "Function '${expr.name}' expects $expected argument(s) but received ${args.size}."
                    )
                }
                val outcome = EngineFunctions.apply(expr.name, args.map { it.value }, context.angleMode)
                if (!outcome.isFinite) throw MathException.overflow("'${expr.name}' produced a value outside the supported range.")
                EvalValue(outcome, exact = isExactFunction(expr.name, args, outcome))
            }
        }
    }

    private fun literal(value: BigDecimal): EvalValue {
        val asDouble = value.toDouble()
        if (!asDouble.isFinite()) throw MathException.overflow("The number ${ExprPrinter.plainNumber(value)} is too large.")
        val integral = value.stripTrailingZeros().scale() <= 0
        val exact = integral && abs(asDouble) <= MAX_EXACT_INTEGER
        return EvalValue(Complex(asDouble), exact)
    }

    private fun constant(name: String): EvalValue = when (name) {
        "pi" -> EvalValue(Complex(Math.PI), exact = false)
        "e" -> EvalValue(Complex(Math.E), exact = false)
        "tau" -> EvalValue(Complex(2.0 * Math.PI), exact = false)
        "phi" -> EvalValue(Complex((1.0 + sqrt(5.0)) / 2.0), exact = false)
        "i" -> EvalValue(Complex.I, exact = true)
        else -> throw MathException.unsupported("Unknown constant '$name'.")
    }

    private inline fun arithmetic(
        left: EvalValue,
        right: EvalValue,
        operation: (Complex, Complex) -> Complex
    ): EvalValue {
        val result = operation(left.value, right.value)
        if (!result.isFinite) throw MathException.overflow("The result is too large to represent.")
        val exact = left.exact && right.exact && result.isReal && abs(result.re) <= MAX_EXACT_INTEGER
        return EvalValue(result, exact)
    }

    private fun divide(left: EvalValue, right: EvalValue): EvalValue {
        if (right.value == Complex.ZERO) throw MathException.divideByZero()
        val result = left.value.divide(right.value)
        if (!result.isFinite) throw MathException.overflow("The result is too large to represent.")
        val exact = left.exact && right.exact && result.isReal &&
            result.re == floor(result.re) && abs(result.re) <= MAX_EXACT_INTEGER
        return EvalValue(result, exact)
    }

    private fun power(base: EvalValue, exponent: EvalValue): EvalValue {
        val b = base.value
        val e = exponent.value
        if (b == Complex.ZERO && e.isReal && e.re == 0.0) {
            throw MathException.domain("0 raised to the power 0 is undefined.")
        }
        if (base.exact && exponent.exact && b.isReal && e.isReal &&
            e.re >= 0.0 && e.re == floor(e.re) && e.re <= 63.0 && abs(b.re) <= MAX_EXACT_INTEGER
        ) {
            val exactResult = BigDecimal(b.re.toLong()).pow(e.re.toInt())
            if (exactResult.abs() <= BigDecimal(MAX_EXACT_INTEGER)) {
                return EvalValue(Complex(exactResult.toDouble()), exact = true)
            }
        }
        val result = b.pow(e)
        if (!result.isFinite) throw MathException.overflow("The power is too large to represent.")
        return EvalValue(result, exact = false)
    }

    private fun factorial(operand: EvalValue): EvalValue {
        val value = operand.value.requireReal("Factorial")
        if (value < 0 || value != floor(value)) {
            throw MathException.domain("Factorial requires a non-negative integer.")
        }
        if (value > 170.0) {
            throw MathException.overflow("Factorial above 170! exceeds the supported range.")
        }
        var result = 1.0
        var factor = 2.0
        while (factor <= value) {
            result *= factor
            factor += 1.0
        }
        return EvalValue(Complex(result), exact = operand.exact && value <= 18.0)
    }

    private fun percent(operand: EvalValue): EvalValue {
        val value = operand.value
        val result = value.divide(Complex(100.0))
        val exact = operand.exact && value.isReal && value.re % 100.0 == 0.0
        return EvalValue(result, exact)
    }

    private fun isExactFunction(name: String, args: List<EvalValue>, outcome: Complex): Boolean {
        if (args.any { !it.exact }) return false
        if (!outcome.isReal) return false
        val integral: Boolean = outcome.re == floor(outcome.re) && abs(outcome.re) <= MAX_EXACT_INTEGER
        return when (name) {
            "abs", "sign", "floor", "ceil", "round", "trunc", "min", "max", "gcd", "lcm",
            "mod", "fact", "ncr", "npr", "sum", "square", "cube", "hypot" -> integral
            else -> false
        }
    }

    companion object {
        /** Largest integer that IEEE-754 double represents without loss. */
        const val MAX_EXACT_INTEGER: Double = 9007199254740992.0
    }
}

/**
 * Implementations of every function in [FunctionCatalog].
 *
 * Domain rules are documented in docs/mathematical-contracts.md; failures always raise a
 * structured error rather than returning NaN or a silently clamped value.
 */
internal object EngineFunctions {

    val arity: Map<String, IntRange> = buildMap {
        val unary = listOf(
            "abs", "sign", "floor", "ceil", "round", "trunc", "frac",
            "sqrt", "cbrt", "exp", "pow10", "pow2", "square", "cube",
            "sin", "cos", "tan", "asin", "acos", "atan", "sec", "csc", "cot", "asec", "acsc", "acot",
            "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
            "ln", "log", "log10", "log2", "fact", "deg", "rad"
        )
        unary.forEach { put(it, 1..1) }
        listOf("hypot", "root", "atan2", "logb", "mod", "gcd", "lcm", "ncr", "npr").forEach { put(it, 2..2) }
        listOf("min", "max", "mean", "median", "sum").forEach { put(it, 1..64) }
    }

    fun apply(name: String, args: List<Complex>, angleMode: AngleMode): Complex = when (name) {
        "abs" -> Complex(args[0].modulus())
        "sign" -> {
            val v = args[0].requireReal("sign")
            Complex(when {
                v > 0 -> 1.0
                v < 0 -> -1.0
                else -> 0.0
            })
        }
        "floor" -> Complex(floor(args[0].requireReal("floor")))
        "ceil" -> Complex(ceil(args[0].requireReal("ceil")))
        "round" -> Complex(round(args[0].requireReal("round")))
        "trunc" -> Complex(truncate(args[0].requireReal("trunc")))
        "frac" -> {
            val v = args[0].requireReal("frac")
            Complex(v - truncate(v))
        }
        "min" -> Complex(args.minOf { it.requireReal("min") })
        "max" -> Complex(args.maxOf { it.requireReal("max") })
        "hypot" -> Complex(kotlin.math.hypot(args[0].requireReal("hypot"), args[1].requireReal("hypot")))

        "sqrt" -> args[0].sqrt()
        "cbrt" -> {
            val v = args[0].requireReal("cbrt")
            Complex(Math.cbrt(v))
        }
        "root" -> root(args[0], args[1])
        "exp" -> args[0].exp()
        "pow10" -> checkOverflow(Math.pow(10.0, args[0].requireReal("pow10")))
        "pow2" -> checkOverflow(Math.pow(2.0, args[0].requireReal("pow2")))
        "square" -> checkOverflow(args[0].times(args[0]))
        "cube" -> checkOverflow(args[0].times(args[0]).times(args[0]))

        "sin" -> Complex(sin(toRadians(args[0].requireReal("sin"), angleMode)))
        "cos" -> Complex(cos(toRadians(args[0].requireReal("cos"), angleMode)))
        "tan" -> {
            val radians = toRadians(args[0].requireReal("tan"), angleMode)
            if (abs(cos(radians)) < NEAR_ZERO) throw MathException.domain("Tangent is undefined at this angle.")
            Complex(tan(radians))
        }
        "asin" -> {
            val v = requireInverseDomain(args[0], "asin")
            Complex(fromRadians(asin(v), angleMode))
        }
        "acos" -> {
            val v = requireInverseDomain(args[0], "acos")
            Complex(fromRadians(acos(v), angleMode))
        }
        "atan" -> Complex(fromRadians(atan(args[0].requireReal("atan")), angleMode))
        "atan2" -> Complex(
            fromRadians(
                kotlin.math.atan2(args[0].requireReal("atan2"), args[1].requireReal("atan2")),
                angleMode
            )
        )
        "sec" -> {
            val radians = toRadians(args[0].requireReal("sec"), angleMode)
            val c = cos(radians)
            if (abs(c) < NEAR_ZERO) throw MathException.domain("Secant is undefined at this angle.")
            Complex(1.0 / c)
        }
        "csc" -> {
            val radians = toRadians(args[0].requireReal("csc"), angleMode)
            val s = sin(radians)
            if (abs(s) < NEAR_ZERO) throw MathException.domain("Cosecant is undefined at this angle.")
            Complex(1.0 / s)
        }
        "cot" -> {
            val radians = toRadians(args[0].requireReal("cot"), angleMode)
            val s = sin(radians)
            if (abs(s) < NEAR_ZERO) throw MathException.domain("Cotangent is undefined at this angle.")
            Complex(cos(radians) / s)
        }
        "asec" -> {
            val v = args[0].requireReal("asec")
            if (abs(v) < 1.0 - 1e-12) throw MathException.domain("Arcsecant is defined only for |x| ≥ 1.")
            Complex(fromRadians(acos(1.0 / v), angleMode))
        }
        "acsc" -> {
            val v = args[0].requireReal("acsc")
            if (abs(v) < 1.0 - 1e-12) throw MathException.domain("Arccosecant is defined only for |x| ≥ 1.")
            Complex(fromRadians(asin(1.0 / v), angleMode))
        }
        "acot" -> {
            val v = args[0].requireReal("acot")
            val radians = when {
                v > 0 -> atan(1.0 / v)
                v < 0 -> atan(1.0 / v) + Math.PI
                else -> Math.PI / 2.0
            }
            Complex(fromRadians(radians, angleMode))
        }

        "sinh" -> Complex(sinh(args[0].requireReal("sinh")))
        "cosh" -> Complex(cosh(args[0].requireReal("cosh")))
        "tanh" -> Complex(tanh(args[0].requireReal("tanh")))
        "asinh" -> Complex(asinh(args[0].requireReal("asinh")))
        "acosh" -> {
            val v = args[0].requireReal("acosh")
            if (v < 1.0) throw MathException.domain("Inverse hyperbolic cosine is defined only for x ≥ 1.")
            Complex(acosh(v))
        }
        "atanh" -> {
            val v = args[0].requireReal("atanh")
            if (abs(v) >= 1.0) throw MathException.domain("Inverse hyperbolic tangent is defined only for |x| < 1.")
            Complex(atanh(v))
        }

        "ln" -> args[0].ln()
        "log" -> logarithm(args[0], 10.0)
        "log10" -> logarithm(args[0], 10.0)
        "log2" -> logarithm(args[0], 2.0)
        "logb" -> {
            val base = args[1].requireReal("logb")
            if (base <= 0.0) throw MathException.domain("The logarithm base must be positive.")
            if (base == 1.0) throw MathException.domain("The logarithm base cannot be 1.")
            logarithm(args[0], base)
        }

        "fact" -> Complex(factorialValue(args[0]))
        "ncr" -> Complex(binomial(args[0], args[1], ordered = false))
        "npr" -> Complex(binomial(args[0], args[1], ordered = true))

        "mod" -> {
            val divisor = args[1].requireReal("mod")
            if (divisor == 0.0) throw MathException.divideByZero("The remainder is undefined when the divisor is zero.")
            Complex(args[0].requireReal("mod") % divisor)
        }
        "gcd" -> Complex(integerValue(integerOf(args[0], "gcd").gcd(integerOf(args[1], "gcd")).abs()))
        "lcm" -> {
            val a = integerOf(args[0], "lcm").abs()
            val b = integerOf(args[1], "lcm").abs()
            val value = if (a.signum() == 0 || b.signum() == 0) BigInteger.ZERO else a.divide(a.gcd(b)).multiply(b)
            Complex(integerValue(value))
        }

        "mean" -> Complex(args.sumOf { it.requireReal("mean") } / args.size)
        "median" -> Complex(sortedMedian(args))
        "sum" -> Complex(args.sumOf { it.requireReal("sum") })

        "deg" -> Complex(Math.toDegrees(args[0].requireReal("deg")))
        "rad" -> Complex(Math.toRadians(args[0].requireReal("rad")))

        else -> throw MathException.unsupported("Function '$name' is not implemented.")
    }

    private const val NEAR_ZERO = 1e-12

    private fun sortedMedian(args: List<Complex>): Double {
        val values = args.map { it.requireReal("median") }.sorted()
        return if (values.size % 2 == 1) values[values.size / 2]
        else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
    }

    private fun truncate(value: Double): Double = if (value >= 0) floor(value) else ceil(value)

    private fun checkOverflow(value: Complex): Complex {
        if (!value.isFinite) throw MathException.overflow("The result is too large to represent.")
        return value
    }

    private fun checkOverflow(value: Double): Complex = checkOverflow(Complex(value))

    private fun toRadians(value: Double, mode: AngleMode): Double = when (mode) {
        AngleMode.DEGREES -> Math.toRadians(value)
        AngleMode.RADIANS -> value
        AngleMode.GRADIANS -> value * Math.PI / 200.0
    }

    private fun fromRadians(value: Double, mode: AngleMode): Double = when (mode) {
        AngleMode.DEGREES -> Math.toDegrees(value)
        AngleMode.RADIANS -> value
        AngleMode.GRADIANS -> value * 200.0 / Math.PI
    }

    private fun requireInverseDomain(value: Complex, name: String): Double {
        val v = value.requireReal(name)
        if (abs(v) > 1.0 + 1e-12) throw MathException.domain("$name is defined only for -1 ≤ x ≤ 1.")
        return v.coerceIn(-1.0, 1.0)
    }

    private fun root(value: Complex, index: Complex): Complex {
        val x = value.requireReal("root")
        val n = index.requireReal("root")
        if (n != floor(n) || n < 1.0 || n > 1000.0) {
            throw MathException.domain("The root index must be a positive integer (1–1000).")
        }
        if (x < 0.0) {
            if (n % 2.0 == 0.0) throw MathException.domain("Even roots of negative numbers are not real.")
            return Complex(-Math.pow(-x, 1.0 / n))
        }
        return Complex(Math.pow(x, 1.0 / n))
    }

    private fun logarithm(value: Complex, base: Double): Complex {
        if (value == Complex.ZERO) throw MathException.domain("The logarithm of zero is undefined.")
        if (value.isReal && value.re < 0.0) throw MathException.domain("The logarithm of a negative number is not real.")
        if (!value.isReal) return value.ln().divide(Complex(ln(base)))
        return Complex(log10(value.re) / log10(base))
    }

    private fun factorialValue(value: Complex): Double {
        val v = value.requireReal("fact")
        if (v < 0 || v != floor(v)) throw MathException.domain("Factorial requires a non-negative integer.")
        if (v > 170.0) throw MathException.overflow("Factorial above 170! exceeds the supported range.")
        var result = 1.0
        var i = 2.0
        while (i <= v) {
            result *= i
            i += 1.0
        }
        return result
    }

    private fun integerOf(value: Complex, name: String): BigInteger {
        val v = value.requireReal(name)
        if (!v.isFinite() || v != floor(v)) throw MathException.domain("$name requires integer arguments.")
        if (abs(v) > 1e18) throw MathException.limit("$name is limited to integers with magnitude up to 10^18.")
        return BigDecimal(v).toBigInteger()
    }

    private fun integerValue(value: BigInteger): Double {
        if (value.bitLength() > 1024) throw MathException.overflow("The integer result is too large to represent.")
        return value.toDouble()
    }

    private fun binomial(nValue: Complex, rValue: Complex, ordered: Boolean): Double {
        val n = integerOf(nValue, "ncr").toLong()
        val r = integerOf(rValue, "ncr").toLong()
        if (n < 0 || r < 0) throw MathException.domain("Combinations require non-negative integers.")
        if (r > n) throw MathException.domain("r cannot be greater than n.")
        if (n > 100000) throw MathException.limit("n is limited to 100 000.")
        var result = BigInteger.ONE
        val k = min(r, n - r)
        var i = 0L
        while (i < k) {
            result = result.multiply(BigInteger.valueOf(n - i)).divide(BigInteger.valueOf(i + 1))
            i++
        }
        if (ordered) {
            var factorial = BigInteger.ONE
            var j = 2L
            while (j <= r) {
                factorial = factorial.multiply(BigInteger.valueOf(j))
                j++
            }
            result = result.multiply(factorial)
        }
        if (result.bitLength() > 1024) throw MathException.overflow("The result is too large to represent.")
        return result.toDouble()
    }
}
