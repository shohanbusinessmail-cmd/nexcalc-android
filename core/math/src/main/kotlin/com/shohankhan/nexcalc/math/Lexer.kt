package com.shohankhan.nexcalc.math

internal enum class TokenKind { NUMBER, IDENTIFIER, OPERATOR, LEFT_PAREN, RIGHT_PAREN, COMMA, FACTORIAL, PERCENT, END }

internal data class Token(val kind: TokenKind, val text: String, val position: Int)

/**
 * Converts user input into a token stream.
 *
 * Accepted operator spellings are deliberately forgiving for on-screen keyboards and pasted text:
 * `×`, `·`, `⋅` mean multiplication, `÷` means division, `−`, `–` mean subtraction and
 * `π`, `τ`, `φ` are treated as constant identifiers.
 */
internal class Lexer(private val source: String) {

    private var index = 0
    private val tokens = ArrayList<Token>()

    fun tokenize(): List<Token> {
        while (index < source.length) {
            val c = source[index]
            when {
                c.isWhitespace() -> index++
                c.isDigit() -> tokens.add(readNumber())
                c == '.' -> {
                    if (index + 1 < source.length && source[index + 1].isDigit()) tokens.add(readNumber())
                    else throw MathException.syntax("A decimal point must be followed by a digit.", index)
                }
                c.isLetter() || c == '_' -> tokens.add(readIdentifier())
                c == '(' || c == '[' || c == '{' -> { tokens.add(Token(TokenKind.LEFT_PAREN, "(", index)); index++ }
                c == ')' || c == ']' || c == '}' -> { tokens.add(Token(TokenKind.RIGHT_PAREN, ")", index)); index++ }
                c == ',' -> { tokens.add(Token(TokenKind.COMMA, ",", index)); index++ }
                c == '!' -> { tokens.add(Token(TokenKind.FACTORIAL, "!", index)); index++ }
                c == '%' -> { tokens.add(Token(TokenKind.PERCENT, "%", index)); index++ }
                c == '^' -> tokens.add(operator("^"))
                c == '+' -> tokens.add(operator("+"))
                c == '-' || c == '\u2212' || c == '\u2013' || c == '\u2014' -> tokens.add(operator("-"))
                c == '*' || c == '\u00D7' || c == '\u00B7' || c == '\u22C5' -> tokens.add(operator("*"))
                c == '/' || c == '\u00F7' || c == '\u2215' -> tokens.add(operator("/"))
                else -> throw MathException.syntax("Unexpected character '$c'.", index)
            }
        }
        tokens.add(Token(TokenKind.END, "", source.length))
        return tokens
    }

    private fun operator(text: String): Token {
        val token = Token(TokenKind.OPERATOR, text, index)
        index++
        return token
    }

    private fun readNumber(): Token {
        val start = index
        val builder = StringBuilder()
        while (index < source.length && source[index].isDigit()) {
            builder.append(source[index])
            index++
        }
        if (index < source.length && source[index] == '.') {
            builder.append('.')
            index++
            while (index < source.length && source[index].isDigit()) {
                builder.append(source[index])
                index++
            }
        }
        if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
            var lookahead = index + 1
            if (lookahead < source.length && (source[lookahead] == '+' || source[lookahead] == '-')) lookahead++
            val digitsStart = lookahead
            while (lookahead < source.length && source[lookahead].isDigit()) lookahead++
            // Only consume the exponent when it is a valid one; otherwise `2e` means 2 * e.
            if (lookahead > digitsStart) {
                builder.append(source, index, lookahead)
                index = lookahead
            }
        }
        val text = builder.toString()
        if (text.isEmpty() || text == ".") throw MathException.syntax("Malformed number.", start)
        return Token(TokenKind.NUMBER, text, start)
    }

    private fun readIdentifier(): Token {
        val start = index
        val builder = StringBuilder()
        while (index < source.length && (source[index].isLetter() || source[index] == '_' || source[index].isDigit())) {
            builder.append(source[index])
            index++
        }
        return Token(TokenKind.IDENTIFIER, builder.toString(), start)
    }
}
