package com.shohankhan.nexcalc.math

/**
 * Stable, machine-checkable error taxonomy for the NEXCALC mathematical engine.
 *
 * Every failure raised by the engine is a [MathException] carrying one of these codes so the
 * UI can present a truthful explanation and automated tests can assert the failure *kind*
 * (not merely a localized message).
 */
enum class MathErrorCode {
    /** The expression could not be tokenized or parsed. */
    SYNTAX,

    /** An operand is outside the mathematical domain of the operation. */
    DOMAIN,

    /** Division (or modulo) by zero. */
    DIVIDE_BY_ZERO,

    /** The result exceeds the representable range of the numeric policy. */
    OVERFLOW,

    /** The result is too small to represent distinctly from zero. */
    UNDERFLOW,

    /** The requested operation is not implemented by the engine. */
    UNSUPPORTED,

    /** A computational budget (depth, iterations, matrix size, ...) was exceeded. */
    LIMIT_EXCEEDED,

    /** An iterative method stopped without meeting its convergence criteria. */
    NON_CONVERGENT,

    /** Operands do not have compatible dimensions. */
    DIMENSION_MISMATCH,

    /** A matrix is singular (or numerically indistinguishable from singular). */
    SINGULAR,

    /** A caller supplied value that fails validation (empty, malformed, out of range). */
    INVALID_INPUT,

    /** Required input was missing. */
    EMPTY_INPUT
}

/**
 * Structured failure type used across the whole engine, formula registry, converters and solvers.
 *
 * @property code stable category of the failure.
 * @property message concise, user-facing English explanation.
 * @property position optional 0-based character offset in the user's expression.
 * @property hint optional actionable suggestion ("Use a positive value for the radius").
 */
class MathException(
    val code: MathErrorCode,
    override val message: String,
    val position: Int? = null,
    val hint: String? = null
) : Exception(message) {

    fun describe(): String = if (position != null) "$message (at character ${position + 1})" else message

    companion object {
        fun syntax(message: String, position: Int? = null): MathException =
            MathException(MathErrorCode.SYNTAX, message, position)

        fun domain(message: String): MathException =
            MathException(MathErrorCode.DOMAIN, message)

        fun divideByZero(message: String = "Division by zero is undefined."): MathException =
            MathException(MathErrorCode.DIVIDE_BY_ZERO, message)

        fun overflow(message: String): MathException =
            MathException(MathErrorCode.OVERFLOW, message)

        fun unsupported(message: String): MathException =
            MathException(MathErrorCode.UNSUPPORTED, message)

        fun limit(message: String): MathException =
            MathException(MathErrorCode.LIMIT_EXCEEDED, message)

        fun convergence(message: String): MathException =
            MathException(MathErrorCode.NON_CONVERGENT, message)

        fun dimension(message: String): MathException =
            MathException(MathErrorCode.DIMENSION_MISMATCH, message)

        fun singular(message: String): MathException =
            MathException(MathErrorCode.SINGULAR, message)

        fun invalid(message: String, hint: String? = null): MathException =
            MathException(MathErrorCode.INVALID_INPUT, message, null, hint)
    }
}

/** Angle interpretation used by trigonometric functions. */
enum class AngleMode(val label: String, val pluralLabel: String) {
    DEGREES("Degree", "Degrees"),
    RADIANS("Radian", "Radians"),
    GRADIANS("Gradian", "Gradians");
}
