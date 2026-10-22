package com.shohankhan.nexcalc.math

import java.math.BigDecimal

/**
 * Recursive-descent parser with the following documented grammar and precedence
 * (highest precedence first):
 *
 * 1. Primary: literals, constants, variables, function calls, parenthesised expressions.
 * 2. Postfix: factorial `!`, percent `%` — left associative.
 * 3. Power `^` — right associative; binds tighter than unary minus, so `-2^2 == -4`.
 * 4. Unary `+`/`-` (right associative).
 * 5. Implicit multiplication (`2pi`, `3(4+1)`, `2sin(x)`) — same precedence as explicit `*`.
 * 6. Multiplication `*` and division `/` — left associative.
 * 7. Addition `+` and subtraction `-` — left associative.
 */
private const val MAX_NESTING = 100

internal class Parser(private val tokens: List<Token>) {

    private var position = 0

    private var nesting = 0

    private fun <T> nested(block: () -> T): T {
        nesting++
        if (nesting > MAX_NESTING) throw MathException.limit("The expression is nested too deeply (limit $MAX_NESTING levels).")
        try {
            return block()
        } finally {
            nesting--
        }
    }

    fun parse(): Expr {
        if (peek().kind == TokenKind.END) throw MathException.syntax("Enter an expression to evaluate.", 0)
        val expression = parseAdditive()
        val trailing = peek()
        if (trailing.kind != TokenKind.END) {
            throw MathException.syntax("Unexpected '${trailing.text}' after a complete expression.", trailing.position)
        }
        return expression
    }

    private fun peek(): Token = tokens[position]

    private fun advance(): Token = tokens[position++]

    private fun parseAdditive(): Expr {
        var left = parseMultiplicative()
        while (true) {
            val token = peek()
            if (token.kind != TokenKind.OPERATOR || (token.text != "+" && token.text != "-")) return left
            advance()
            left = Expr.Binary(token.text, left, parseMultiplicative())
        }
    }

    private fun parseMultiplicative(): Expr {
        var left = parseUnary()
        while (true) {
            val token = peek()
            when {
                token.kind == TokenKind.OPERATOR && (token.text == "*" || token.text == "/") -> {
                    advance()
                    left = Expr.Binary(token.text, left, parseUnary())
                }
                token.kind == TokenKind.NUMBER || token.kind == TokenKind.IDENTIFIER || token.kind == TokenKind.LEFT_PAREN -> {
                    // Implicit multiplication: 2pi, 3(x+1), (a)(b).
                    left = Expr.Binary("*", left, parseUnary())
                }
                else -> return left
            }
        }
    }

    private fun parseUnary(): Expr {
        val token = peek()
        if (token.kind == TokenKind.OPERATOR && (token.text == "-" || token.text == "+")) {
            advance()
            val operand = nested { parseUnary() }
            return if (token.text == "-") Expr.Unary("-", operand) else Expr.Unary("+", operand)
        }
        return parsePower()
    }

    private fun parsePower(): Expr {
        val base = parsePostfix()
        val token = peek()
        if (token.kind == TokenKind.OPERATOR && token.text == "^") {
            advance()
            // Right associative, and the exponent may carry its own sign: 2^-3.
            return Expr.Binary("^", base, parseUnary())
        }
        return base
    }

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while (true) {
            val token = peek()
            when (token.kind) {
                TokenKind.FACTORIAL -> { advance(); expr = Expr.Postfix("!", expr) }
                TokenKind.PERCENT -> { advance(); expr = Expr.Postfix("%", expr) }
                else -> return expr
            }
        }
    }

    private fun parsePrimary(): Expr {
        val token = peek()
        when (token.kind) {
            TokenKind.NUMBER -> {
                advance()
                val value = try {
                    BigDecimal(token.text)
                } catch (e: NumberFormatException) {
                    throw MathException.syntax("'${token.text}' is not a valid number.", token.position)
                }
                return Expr.Num(value)
            }
            TokenKind.IDENTIFIER -> {
                advance()
                val name = token.text.lowercase()
                if (peek().kind == TokenKind.LEFT_PAREN) {
                    advance()
                    val args = ArrayList<Expr>()
                    if (peek().kind != TokenKind.RIGHT_PAREN) {
                        args.add(parseAdditive())
                        while (peek().kind == TokenKind.COMMA) {
                            advance()
                            args.add(parseAdditive())
                        }
                    }
                    val closing = peek()
                    if (closing.kind != TokenKind.RIGHT_PAREN) {
                        throw MathException.syntax("Missing ')' after the arguments of '$name'.", closing.position)
                    }
                    advance()
                    if (!FunctionCatalog.isFunction(name)) {
                        throw MathException.syntax("Unknown function '$name'.", token.position)
                    }
                    return Expr.Call(name, args)
                }
                if (FunctionCatalog.isFunction(name)) {
                    throw MathException.syntax("Function '$name' needs parentheses, for example $name(30).", token.position)
                }
                val constant = normalizeConstant(name)
                return if (constant != null) Expr.Const(constant) else Expr.Var(name)
            }
            TokenKind.LEFT_PAREN -> {
                advance()
                val inner = nested { parseAdditive() }
                val closing = peek()
                if (closing.kind != TokenKind.RIGHT_PAREN) {
                    throw MathException.syntax("Missing closing parenthesis.", closing.position)
                }
                advance()
                return inner
            }
            TokenKind.END -> throw MathException.syntax("The expression ends unexpectedly.", token.position)
            else -> throw MathException.syntax("Expected a number, function or '(' but found '${token.text}'.", token.position)
        }
    }

    private fun normalizeConstant(name: String): String? = when (name) {
        "pi", "\u03C0" -> "pi"
        "e" -> "e"
        "tau", "\u03C4" -> "tau"
        "phi", "\u03C6" -> "phi"
        "i" -> "i"
        else -> null
    }
}

/** Facade that tokenizes and parses a user-entered expression. */
object ExpressionParser {

    fun parse(expression: String): Expr {
        if (expression.isBlank()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter an expression to evaluate.")
        val tokens = Lexer(expression).tokenize()
        return Parser(tokens).parse()
    }
}
