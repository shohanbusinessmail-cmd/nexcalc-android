package com.shohankhan.nexcalc.math

import java.math.BigDecimal

/**
 * Typed expression tree produced by [Parser].
 *
 * The tree is always explicit: there is no string concatenation or dynamic evaluation anywhere in
 * the engine, which makes every expression inspectable (symbolic differentiation, printing,
 * structural simplification) and safe to evaluate with bounded recursion.
 */
sealed interface Expr {

    /** Numeric literal exactly as written by the user. */
    data class Num(val value: BigDecimal) : Expr

    /** Named mathematical constant such as pi, e, tau, phi or i. */
    data class Const(val name: String) : Expr

    /** Prefix sign: `-x` or `+x`. */
    data class Unary(val op: String, val operand: Expr) : Expr

    /** Infix binary operator: `+ - * / ^`. */
    data class Binary(val op: String, val left: Expr, val right: Expr) : Expr

    /** Function call with one or more arguments. */
    data class Call(val name: String, val args: List<Expr>) : Expr

    /** Postfix operator: factorial (`!`) or percent (`%`). */
    data class Postfix(val op: String, val operand: Expr) : Expr

    /** Symbolic variable used by equation, calculus and statistics tooling. */
    data class Var(val name: String) : Expr
}

/** Renders an [Expr] back to a canonical, human-readable infix string. */
object ExprPrinter {

    fun toInfix(expr: Expr): String = render(expr, parentPrecedence = 0)

    private fun precedence(expr: Expr): Int = when (expr) {
        is Expr.Binary -> when (expr.op) {
            "+", "-" -> 1
            "*", "/" -> 2
            "^" -> 4
            else -> 0
        }
        is Expr.Unary -> 3
        else -> 5
    }

    private fun render(expr: Expr, parentPrecedence: Int): String {
        val text = when (expr) {
            is Expr.Num -> plainNumber(expr.value)
            is Expr.Const -> expr.name
            is Expr.Var -> expr.name
            is Expr.Unary -> {
                val inner = render(expr.operand, 3)
                if (expr.op == "-") "-$inner" else "+$inner"
            }
            is Expr.Postfix -> render(expr.operand, 5) + expr.op
            is Expr.Binary -> {
                val left = render(expr.left, precedence(expr))
                // Exponentiation is right-associative, so its right operand may share precedence.
                val right = render(expr.right, precedence(expr) + if (expr.op == "^") 0 else 1)
                if (expr.op == "^") "$left^$right" else "$left ${expr.op} $right"
            }
            is Expr.Call -> expr.name + "(" + expr.args.joinToString(", ") { render(it, 0) } + ")"
        }
        return if (precedence(expr) < parentPrecedence) "($text)" else text
    }

    /** Avoids scientific notation such as `1E+3` for ordinary literal values. */
    fun plainNumber(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            stripped.setScale(0).toPlainString()
        } else {
            value.toPlainString()
        }
    }
}
