package com.shohankhan.nexcalc.math

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/**
 * Symbolic differentiation and structural simplification over [Expr].
 *
 * Scope (documented, and deliberately narrower than a full computer-algebra system):
 * - rational functions, powers with constant or variable exponents;
 * - sin, cos, tan, sec, csc, cot, inverse trigonometric functions;
 * - sinh, cosh, tanh and inverse hyperbolic functions;
 * - exp, ln, log (base 10), log2, log10, sqrt, cbrt, abs (non-differentiable form rejected).
 *
 * Factorial, percent and multi-argument functions other than `logb` are rejected with
 * [MathErrorCode.UNSUPPORTED] instead of returning a plausible-looking wrong derivative.
 */
object Calculus {

    fun differentiate(expression: String, variable: String = "x"): String {
        val parsed = ExpressionParser.parse(expression)
        return ExprPrinter.toInfix(simplify(derivative(parsed, variable)))
    }

    fun derivative(expr: Expr, variable: String): Expr = when (expr) {
        is Expr.Num, is Expr.Const -> number(0.0)
        is Expr.Var -> if (expr.name == variable) number(1.0) else number(0.0)
        is Expr.Unary -> Expr.Unary(expr.op, derivative(expr.operand, variable))
        is Expr.Binary -> when (expr.op) {
            "+", "-" -> Expr.Binary(expr.op, derivative(expr.left, variable), derivative(expr.right, variable))
            "*" -> sum(
                product(derivative(expr.left, variable), expr.right),
                product(expr.left, derivative(expr.right, variable))
            )
            "/" -> quotient(
                difference(
                    product(derivative(expr.left, variable), expr.right),
                    product(expr.left, derivative(expr.right, variable))
                ),
                power(expr.right, number(2.0))
            )
            "^" -> derivativeOfPower(expr, variable)
            else -> throw MathException.unsupported("Cannot differentiate the operator '${expr.op}'.")
        }
        is Expr.Postfix -> throw MathException.unsupported("Symbolic differentiation of '${expr.op}' is not supported.")
        is Expr.Call -> derivativeOfCall(expr, variable)
    }

    private fun derivativeOfPower(expr: Expr.Binary, variable: String): Expr {
        val base = expr.left
        val exponent = expr.right
        val exponentDerivative = derivative(exponent, variable)
        val baseDerivative = derivative(base, variable)
        val constantExponent = exponentDerivative == number(0.0)
        return if (constantExponent) {
            // d(u^n) = n·u^(n-1)·u'
            product(
                product(exponent, power(base, difference(exponent, number(1.0)))),
                baseDerivative
            )
        } else {
            // d(u^v) = u^v · (v'·ln(u) + v·u'/u)
            product(
                power(base, exponent),
                sum(
                    product(exponentDerivative, Expr.Call("ln", listOf(base))),
                    quotient(product(exponent, baseDerivative), base)
                )
            )
        }
    }

    private fun derivativeOfCall(call: Expr.Call, variable: String): Expr {
        if (call.args.isEmpty()) throw MathException.unsupported("Cannot differentiate '${call.name}' without arguments.")
        val argument = call.args[0]
        val inner = derivative(argument, variable)
        val outer: Expr = when (call.name) {
            "sin" -> Expr.Call("cos", listOf(argument))
            "cos" -> Expr.Unary("-", Expr.Call("sin", listOf(argument)))
            "tan" -> quotient(number(1.0), power(Expr.Call("cos", listOf(argument)), number(2.0)))
            "sec" -> product(Expr.Call("sec", listOf(argument)), Expr.Call("tan", listOf(argument)))
            "csc" -> Expr.Unary("-", product(Expr.Call("csc", listOf(argument)), Expr.Call("cot", listOf(argument))))
            "cot" -> Expr.Unary("-", quotient(number(1.0), power(Expr.Call("sin", listOf(argument)), number(2.0))))
            "asin" -> quotient(number(1.0), power(difference(number(1.0), power(argument, number(2.0))), number(0.5)))
            "acos" -> Expr.Unary("-", quotient(number(1.0), power(difference(number(1.0), power(argument, number(2.0))), number(0.5))))
            "atan" -> quotient(number(1.0), sum(number(1.0), power(argument, number(2.0))))
            "sinh" -> Expr.Call("cosh", listOf(argument))
            "cosh" -> Expr.Call("sinh", listOf(argument))
            "tanh" -> quotient(number(1.0), power(Expr.Call("cosh", listOf(argument)), number(2.0)))
            "asinh" -> quotient(number(1.0), power(sum(power(argument, number(2.0)), number(1.0)), number(0.5)))
            "acosh" -> quotient(number(1.0), product(
                power(difference(argument, number(1.0)), number(0.5)),
                power(sum(argument, number(1.0)), number(0.5))
            ))
            "atanh" -> quotient(number(1.0), difference(number(1.0), power(argument, number(2.0))))
            "exp" -> Expr.Call("exp", listOf(argument))
            "ln" -> quotient(number(1.0), argument)
            "log", "log10" -> quotient(number(1.0), product(argument, number(Math.log(10.0))))
            "log2" -> quotient(number(1.0), product(argument, number(Math.log(2.0))))
            "sqrt" -> quotient(number(1.0), product(number(2.0), Expr.Call("sqrt", listOf(argument))))
            "cbrt" -> quotient(number(1.0), product(number(3.0), power(Expr.Call("cbrt", listOf(argument)), number(2.0))))
            "abs", "floor", "ceil", "round", "trunc", "sign", "frac" ->
                throw MathException.unsupported("The derivative of '${call.name}' is not a smooth function and is not supported.")
            else -> throw MathException.unsupported("Symbolic differentiation of '${call.name}' is not supported.")
        }
        return product(outer, inner)
    }

    /** Structural simplification with constant folding. Idempotent and safe to call repeatedly. */
    fun simplify(expr: Expr): Expr = when (expr) {
        is Expr.Num, is Expr.Const, is Expr.Var -> expr
        is Expr.Unary -> {
            val operand = simplify(expr.operand)
            when {
                expr.op == "+" -> operand
                operand is Expr.Num -> Expr.Num(operand.value.negate())
                operand is Expr.Unary && operand.op == "-" -> operand.operand
                else -> Expr.Unary("-", operand)
            }
        }
        is Expr.Postfix -> Expr.Postfix(expr.op, simplify(expr.operand))
        is Expr.Call -> Expr.Call(expr.name, expr.args.map { simplify(it) })
        is Expr.Binary -> simplifyBinary(simplify(expr.left), expr.op, simplify(expr.right))
    }

    private fun simplifyBinary(left: Expr, op: String, right: Expr): Expr {
        val leftNumber = (left as? Expr.Num)?.value
        val rightNumber = (right as? Expr.Num)?.value
        if (leftNumber != null && rightNumber != null) {
            val folded = fold(leftNumber, op, rightNumber)
            if (folded != null) return Expr.Num(folded)
        }
        return when (op) {
            "+" -> when {
                left.isZero() -> right
                right.isZero() -> left
                else -> Expr.Binary("+", left, right)
            }
            "-" -> when {
                right.isZero() -> left
                left == right -> number(0.0)
                else -> Expr.Binary("-", left, right)
            }
            "*" -> when {
                left.isZero() || right.isZero() -> number(0.0)
                left.isOne() -> right
                right.isOne() -> left
                left == number(-1.0) -> Expr.Unary("-", right)
                right == number(-1.0) -> Expr.Unary("-", left)
                else -> Expr.Binary("*", left, right)
            }
            "/" -> when {
                right.isOne() -> left
                left.isZero() -> number(0.0)
                left == right -> number(1.0)
                else -> Expr.Binary("/", left, right)
            }
            "^" -> when {
                right.isZero() -> number(1.0)
                right.isOne() -> left
                left.isOne() -> number(1.0)
                else -> Expr.Binary("^", left, right)
            }
            else -> Expr.Binary(op, left, right)
        }
    }

    private fun fold(left: BigDecimal, op: String, right: BigDecimal): BigDecimal? {
        return try {
            when (op) {
                "+" -> left.add(right)
                "-" -> left.subtract(right)
                "*" -> left.multiply(right)
                "/" -> if (right.signum() == 0) null else left.divide(right, MathContext.DECIMAL64)
                "^" -> {
                    val exponent = right.toDouble()
                    if (exponent == Math.floor(exponent) && abs(exponent) <= 64) {
                        if (exponent >= 0) left.pow(exponent.toInt())
                        else BigDecimal.ONE.divide(left.pow(-exponent.toInt()), MathContext.DECIMAL64)
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (error: ArithmeticException) {
            null
        }
    }

    fun parseAndSimplify(expression: String): String = ExprPrinter.toInfix(simplify(ExpressionParser.parse(expression)))

    private fun Expr.isZero(): Boolean = this is Expr.Num && value.signum() == 0

    private fun Expr.isOne(): Boolean = this is Expr.Num && value.compareTo(BigDecimal.ONE) == 0

    private fun number(value: Double): Expr = Expr.Num(BigDecimal.valueOf(value))

    private fun sum(left: Expr, right: Expr): Expr = Expr.Binary("+", left, right)

    private fun difference(left: Expr, right: Expr): Expr = Expr.Binary("-", left, right)

    private fun product(left: Expr, right: Expr): Expr = Expr.Binary("*", left, right)

    private fun quotient(left: Expr, right: Expr): Expr = Expr.Binary("/", left, right)

    private fun power(left: Expr, right: Expr): Expr = Expr.Binary("^", left, right)
}
