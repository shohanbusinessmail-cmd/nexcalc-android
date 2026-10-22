package com.shohankhan.nexcalc.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Parser precedence, associativity and tokenization. */
class ParserTest {

    private fun value(expression: String, mode: AngleMode = AngleMode.RADIANS): Double =
        Evaluator(EvalContext(angleMode = mode)).evaluate(expression).value.re

    @Test
    fun `multiplication binds tighter than addition`() {
        assertEquals(14.0, value("2 + 3 * 4"))
        assertEquals(20.0, value("(2 + 3) * 4"))
    }

    @Test
    fun `division is left associative`() {
        assertEquals(4.0, value("16 / 4 / 1"))
        assertEquals(4.0, value("100 / 5 / 5"))
    }

    @Test
    fun `exponentiation is right associative and binds tighter than unary minus`() {
        assertEquals(512.0, value("2^3^2"))
        assertEquals(-4.0, value("-2^2"))
        assertEquals(0.25, value("2^-2"))
        assertEquals(36.0, value("3!^2"))
    }

    @Test
    fun `implicit multiplication is supported`() {
        assertEquals(2 * Math.PI, value("2pi"), 1e-12)
        assertEquals(15.0, value("3(4+1)"))
        assertEquals(12.0, value("(2)(6)"))
        assertEquals(2 * Math.E, value("2e"), 1e-12)
    }

    @Test
    fun `scientific notation literal is a single number`() {
        assertEquals(1500.0, value("1.5e3"))
        assertEquals(0.0015, value("1.5e-3"))
        assertEquals(2000.0, value("2e3"))
    }

    @Test
    fun `unicode operators are accepted`() {
        assertEquals(14.0, value("2 + 3 × 4"))
        assertEquals(5.0, value("10 ÷ 2"))
        assertEquals(1.0, value("3 − 2"))
        assertEquals(Math.PI, value("π"), 1e-12)
    }

    @Test
    fun `factorial and percent postfix`() {
        assertEquals(120.0, value("5!"))
        assertEquals(0.15, value("15%"))
        assertEquals(30.0, value("200 * 15%"))
    }

    @Test
    fun `nested parentheses and unary signs`() {
        assertEquals(7.0, value("((((3)))) + 4"))
        assertEquals(-8.0, value("-(3 + 5)"))
        assertEquals(5.0, value("+5"))
        assertEquals(-3.0, value("-(-3)"))
    }

    @Test
    fun `empty and malformed expressions report syntax errors`() {
        assertFailsWith<MathException> { ExpressionParser.parse("") }
        assertFailsWith<MathException> { ExpressionParser.parse("2 +") }
        assertFailsWith<MathException> { ExpressionParser.parse("(2 + 3") }
        assertFailsWith<MathException> { ExpressionParser.parse("2 3 4 /") }
        assertFailsWith<MathException> { ExpressionParser.parse("2 $$ 3") }
        assertFailsWith<MathException> { ExpressionParser.parse("sin") }
        assertFailsWith<MathException> { ExpressionParser.parse("unknown(2)") }
    }

    @Test
    fun `syntax errors carry a position`() {
        val error = assertFailsWith<MathException> { ExpressionParser.parse("1 + * 2") }
        assertEquals(MathErrorCode.SYNTAX, error.code)
        assertTrue(error.position != null)
    }

    @Test
    fun `deep nesting is bounded`() {
        val expression = "(".repeat(300) + "1" + ")".repeat(300)
        assertFailsWith<MathException> { Evaluator(EvalContext(maxDepth = 64)).evaluate(expression) }
    }

    @Test
    fun `printer round trips canonical form`() {
        val expression = ExpressionParser.parse("2 + 3 * (4 - 1)")
        val printed = ExprPrinter.toInfix(expression)
        assertEquals(11.0, Evaluator().evaluate(printed).value.re)
        assertEquals("2 + 3 * (4 - 1)", printed)
    }
}

/** Numeric semantics, exactness tracking and domain rules. */
class EvaluatorTest {

    private fun value(expression: String, mode: AngleMode = AngleMode.RADIANS): Double =
        Evaluator(EvalContext(angleMode = mode)).evaluate(expression).value.re

    @Test
    fun `integer arithmetic is marked exact`() {
        val result = Evaluator().evaluate("2 + 3 * 4")
        assertTrue(result.exact)
        assertEquals(14.0, result.value.re)
    }

    @Test
    fun `lossy division is marked approximate`() {
        val result = Evaluator().evaluate("1 / 3")
        assertFalse(result.exact)
        assertEquals(1.0 / 3.0, result.value.re)
    }

    @Test
    fun `exact division stays exact`() {
        val result = Evaluator().evaluate("10 / 2")
        assertTrue(result.exact)
        assertEquals(5.0, result.value.re)
    }

    @Test
    fun `transcendental results are approximate and correct`() {
        val result = Evaluator().evaluate("sin(pi/2)")
        assertFalse(result.exact)
        assertEquals(1.0, result.value.re, 1e-12)
    }

    @Test
    fun `division by zero is reported as a structured error`() {
        val error = assertFailsWith<MathException> { Evaluator().evaluate("5 / 0") }
        assertEquals(MathErrorCode.DIVIDE_BY_ZERO, error.code)
    }

    @Test
    fun `power overflow is reported`() {
        val error = assertFailsWith<MathException> { Evaluator().evaluate("10^400") }
        assertEquals(MathErrorCode.OVERFLOW, error.code)
    }

    @Test
    fun `factorial domain is enforced`() {
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("(-3)!") }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("2.5!") }.code)
        assertEquals(MathErrorCode.OVERFLOW, assertFailsWith<MathException> { Evaluator().evaluate("171!") }.code)
    }

    @Test
    fun `square root of a negative number is imaginary`() {
        val result = Evaluator().evaluate("sqrt(-9)")
        assertEquals(0.0, result.value.re, 1e-12)
        assertEquals(3.0, result.value.im, 1e-12)
    }

    @Test
    fun `complex arithmetic follows the documented rules`() {
        val product = Evaluator().evaluate("(1 + 2i) * (3 - i)")
        assertEquals(5.0, product.value.re, 1e-12)
        assertEquals(5.0, product.value.im, 1e-12)
        val sum = Evaluator().evaluate("i^2")
        assertEquals(-1.0, sum.value.re, 1e-12)
        assertEquals(0.0, sum.value.im, 1e-12)
    }

    @Test
    fun `trigonometric functions honour the angle mode`() {
        assertEquals(0.5, value("sin(30)", AngleMode.DEGREES), 1e-12)
        assertEquals(0.5, value("sin(pi/6)", AngleMode.RADIANS), 1e-12)
        assertEquals(0.5, value("sin(100/3)", AngleMode.GRADIANS), 1e-12)
        assertEquals(45.0, value("atan(1)", AngleMode.DEGREES), 1e-12)
    }

    @Test
    fun `tangent at a pole is rejected instead of returning a huge number`() {
        val error = assertFailsWith<MathException> { Evaluator(EvalContext(angleMode = AngleMode.DEGREES)).evaluate("tan(90)") }
        assertEquals(MathErrorCode.DOMAIN, error.code)
    }

    @Test
    fun `inverse trigonometric domain is enforced`() {
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("asin(2)") }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("acos(-1.5)") }.code)
    }

    @Test
    fun `logarithm domains are enforced`() {
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("ln(0)") }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("log(-5)") }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("logb(8, 1)") }.code)
        assertEquals(3.0, value("logb(8, 2)"), 1e-12)
    }

    @Test
    fun `roots handle odd and even indices`() {
        assertEquals(3.0, value("root(27, 3)"), 1e-12)
        assertEquals(-2.0, value("root(-8, 3)"), 1e-12)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("root(-16, 2)") }.code)
        assertEquals(2.0, value("cbrt(8)"), 1e-12)
    }

    @Test
    fun `combinatorics and number theory functions`() {
        assertEquals(120.0, value("ncr(10, 3)"), 1e-12)
        assertEquals(720.0, value("npr(10, 3)"), 1e-12)
        assertEquals(6.0, value("gcd(48, 18)"), 1e-12)
        assertEquals(144.0, value("lcm(48, 18)"), 1e-12)
        assertEquals(2.0, value("mod(17, 5)"), 1e-12)
        assertEquals(-2.0, value("mod(-17, 5)"), 1e-12)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Evaluator().evaluate("ncr(5, 9)") }.code)
        assertEquals(MathErrorCode.DIVIDE_BY_ZERO, assertFailsWith<MathException> { Evaluator().evaluate("mod(5, 0)") }.code)
    }

    @Test
    fun `variadic statistics helpers`() {
        assertEquals(5.0, value("mean(2, 4, 9)"), 1e-12)
        assertEquals(4.0, value("median(2, 4, 9)"), 1e-12)
        assertEquals(3.0, value("median(1, 2, 4, 5)"), 1e-12)
        assertEquals(10.0, value("sum(1, 2, 3, 4)"), 1e-12)
        assertEquals(9.0, value("max(4, 9, 2)"), 1e-12)
        assertEquals(2.0, value("min(4, 9, 2)"), 1e-12)
    }

    @Test
    fun `arity mismatches are rejected`() {
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Evaluator().evaluate("sin(1, 2)") }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> { Evaluator().evaluate("logb(2)") }.code)
    }

    @Test
    fun `constants are available`() {
        assertEquals(Math.PI, value("pi"), 1e-12)
        assertEquals(Math.E, value("e"), 1e-12)
        assertEquals(2 * Math.PI, value("tau"), 1e-12)
        assertEquals((1 + Math.sqrt(5.0)) / 2, value("phi"), 1e-12)
        assertEquals("pi", ExprPrinter.toInfix(ExpressionParser.parse("\u03C0")))
    }

    @Test
    fun `bound variables are supported but unknown symbols fail clearly`() {
        val evaluator = Evaluator(EvalContext(variables = mapOf("x" to Complex(3.0))))
        assertEquals(9.0, evaluator.evaluate("x^2").value.re)
        val error = assertFailsWith<MathException> { Evaluator().evaluate("y + 1") }
        assertEquals(MathErrorCode.UNSUPPORTED, error.code)
    }

    @Test
    fun `every catalogued function is implemented and its example is correct`() {
        assertEquals(FunctionCatalog.names, FunctionCatalog.implementedNames)
        val failures = mutableListOf<String>()
        for (info in FunctionCatalog.functions) {
            try {
                val computed = Evaluator(EvalContext(angleMode = info.exampleAngleMode)).evaluate(info.example).value.re
                if (Math.abs(computed - info.expectedExample) > 1e-9 * Math.max(1.0, Math.abs(info.expectedExample))) {
                    failures += "${info.name}: ${info.example} gave $computed, expected ${info.expectedExample}"
                }
            } catch (error: MathException) {
                failures += "${info.name}: ${info.example} failed with ${error.message}"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}

/** Display formatting policy. */
class FormatterTest {

    @Test
    fun `exact integers print without decimals`() {
        assertEquals("14", NumberFormatter.format(14.0, exact = true))
        assertEquals("1000000", NumberFormatter.format(1_000_000.0, NumberFormatOptions(useGrouping = false)))
    }

    @Test
    fun `grouping can be enabled`() {
        assertEquals("1,234,567", NumberFormatter.format(1234567.0, NumberFormatOptions(useGrouping = true)))
        assertEquals("1234567", NumberFormatter.format(1234567.0, NumberFormatOptions(useGrouping = false)))
    }

    @Test
    fun `rounding honours decimal precision`() {
        assertEquals("1.41", NumberFormatter.format(1.4142135623, NumberFormatOptions(decimalPrecision = 2, useGrouping = false)))
        assertEquals("1.414", NumberFormatter.format(1.4142135623, NumberFormatOptions(decimalPrecision = 3, useGrouping = false)))
    }

    @Test
    fun `scientific notation is used for extreme magnitudes`() {
        val text = NumberFormatter.format(1.5e15, NumberFormatOptions(significantDigits = 3, useGrouping = false))
        assertTrue(text.contains("e+15"), text)
        val small = NumberFormatter.format(2.5e-12, NumberFormatOptions(significantDigits = 3, useGrouping = false))
        assertTrue(small.contains("e-12"), small)
    }

    @Test
    fun `decimal and grouping separators are configurable`() {
        val options = NumberFormatOptions(decimalSeparator = ',', groupingSeparator = '.', useGrouping = true)
        assertEquals("1.234,5", NumberFormatter.format(1234.5, options))
    }

    @Test
    fun `complex values are formatted with an explicit sign`() {
        assertEquals("-1", NumberFormatter.formatComplex(Complex(-1.0)))
        assertEquals("2 + 3i", NumberFormatter.formatComplex(Complex(2.0, 3.0)))
        assertEquals("2 \u2212 3i", NumberFormatter.formatComplex(Complex(2.0, -3.0)))
        assertEquals("i", NumberFormatter.formatComplex(Complex(0.0, 1.0)))
        assertEquals("-i", NumberFormatter.formatComplex(Complex(0.0, -1.0)))
        assertEquals("2i", NumberFormatter.formatComplex(Complex(0.0, 2.0)))
    }

    @Test
    fun `significant figures rounding`() {
        assertEquals("3.142", NumberFormatter.significant(3.14159265, 4))
        assertEquals("0.001235", NumberFormatter.significant(0.00123456, 4))
    }

    @Test
    fun `invalid precision is rejected by the options type`() {
        assertFailsWith<IllegalArgumentException> { NumberFormatOptions(decimalPrecision = 20) }
        assertFailsWith<IllegalArgumentException> { NumberFormatOptions(significantDigits = 0) }
    }
}

/** High-level engine facade. */
class CalculatorEngineTest {

    @Test
    fun `successful evaluation returns a value`() {
        val outcome = CalculatorEngine.evaluate("2 + 3 * 4", AngleMode.DEGREES)
        assertTrue(outcome is CalculationOutcome.Success)
        assertEquals(14.0, (outcome as CalculationOutcome.Success).value.re)
    }

    @Test
    fun `failures are returned with a code and message`() {
        val outcome = CalculatorEngine.evaluate("1 / 0")
        assertTrue(outcome is CalculationOutcome.Failure)
        assertEquals(MathErrorCode.DIVIDE_BY_ZERO, (outcome as CalculationOutcome.Failure).code)
        assertTrue(outcome.message.isNotBlank())
    }

    @Test
    fun `ans variable is available`() {
        val outcome = CalculatorEngine.evaluate("ans * 2", ans = Complex(21.0))
        assertEquals(42.0, (outcome as CalculationOutcome.Success).value.re)
    }

    @Test
    fun `functionOf exposes real functions for numerical methods`() {
        val f = CalculatorEngine.functionOf("x^2 - 2", "x")
        assertEquals(2.0, f(2.0))
        assertEquals(-1.0, f(1.0))
        assertFailsWith<MathException> { CalculatorEngine.functionOf("x +", "x") }
    }

    @Test
    fun `deeply nested input cannot crash the facade`() {
        val expression = "1+" + "(-".repeat(500) + "1" + ")".repeat(500)
        val outcome = CalculatorEngine.evaluate(expression)
        assertTrue(outcome is CalculationOutcome.Success || outcome is CalculationOutcome.Failure)
    }
}
