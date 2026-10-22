package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SolversTest {

    private val f = { x: Double -> x * x - 2.0 }
    private val df = { x: Double -> 2.0 * x }

    @Test
    fun `bisection finds a bracketed root`() {
        val solution = Solvers.bisection(f, 0.0, 2.0)
        assertTrue(solution.converged)
        assertEquals(Math.sqrt(2.0), solution.root!!, 1e-8)
        assertTrue(solution.residual!! < 1e-8)
    }

    @Test
    fun `bisection reports a missing bracket`() {
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> {
            Solvers.bisection(f, 2.0, 3.0)
        }.code)
    }

    @Test
    fun `newton raphson converges and detects a zero derivative`() {
        val solution = Solvers.newtonRaphson(f, df, 1.0)
        assertTrue(solution.converged)
        assertEquals(Math.sqrt(2.0), solution.root!!, 1e-10)

        val stalled = Solvers.newtonRaphson({ x -> x * x + 1.0 }, { x -> 2 * x }, 0.0)
        assertFalse(stalled.converged)
    }

    @Test
    fun `secant method converges`() {
        val solution = Solvers.secant(f, 1.0, 2.0)
        assertTrue(solution.converged)
        assertEquals(Math.sqrt(2.0), solution.root!!, 1e-10)
    }

    @Test
    fun `iteration limit is a failure not a success`() {
        val solution = Solvers.bisection(f, 0.0, 2.0, tolerance = 1e-14, maxIterations = 2)
        assertFalse(solution.converged)
        assertTrue(solution.message.contains("iteration limit"))
    }

    @Test
    fun `tolerance validation`() {
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            Solvers.bisection(f, 0.0, 2.0, tolerance = 0.0)
        }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            Solvers.newtonRaphson(f, df, 1.0, maxIterations = 0)
        }.code)
    }

    @Test
    fun `numeric derivatives approximate analytic derivatives`() {
        assertEquals(1.0, Solvers.derivative({ x -> Math.sin(x) }, 0.0), 1e-7)
        assertEquals(12.0, Solvers.derivative({ x -> x * x * x }, 2.0), 1e-5)
        assertEquals(2.0, Solvers.secondDerivative({ x -> x * x }, 3.0), 1e-4)
    }

    @Test
    fun `definite integration matches known values`() {
        val cubic = Solvers.integrate({ x -> x * x }, 0.0, 1.0)
        assertTrue(cubic.converged)
        assertEquals(1.0 / 3.0, cubic.value, 1e-8)

        val sine = Solvers.integrate({ x -> Math.sin(x) }, 0.0, Math.PI)
        assertTrue(sine.converged)
        assertEquals(2.0, sine.value, 1e-8)

        val reversed = Solvers.integrate({ x -> x * x }, 1.0, 0.0)
        assertEquals(-1.0 / 3.0, reversed.value, 1e-8)

        val zeroWidth = Solvers.integrate({ x -> x * x }, 2.0, 2.0)
        assertEquals(0.0, zeroWidth.value, 0.0)
    }

    @Test
    fun `two sided limit of sin(x) over x at zero`() {
        val result = Solvers.limit({ x -> if (x == 0.0) 1.0 else Math.sin(x) / x }, 0.0)
        assertTrue(result.converged)
        assertEquals(1.0, result.value!!, 1e-6)
    }

    @Test
    fun `summation and product`() {
        assertEquals(385.0, Solvers.summation({ x -> x * x }, 1, 10), 1e-9)
        assertEquals(120.0, Solvers.product({ x -> x }, 1, 5), 1e-9)
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> {
            Solvers.summation({ x -> x }, 1, 2_000_000)
        }.code)
    }

    @Test
    fun `linear and quadratic equations`() {
        assertEquals(4.0, Solvers.linearEquation(2.0, -8.0), 1e-12)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Solvers.linearEquation(0.0, 5.0) }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Solvers.linearEquation(0.0, 0.0) }.code)

        val roots = Solvers.quadraticRoots(1.0, -5.0, 6.0)
        assertEquals(2.0, roots[0].re, 1e-12)
        assertEquals(3.0, roots[1].re, 1e-12)

        val repeated = Solvers.quadraticRoots(1.0, -2.0, 1.0)
        assertEquals(2, repeated.size)
        assertEquals(1.0, repeated[0].re, 1e-12)

        val complexRoots = Solvers.quadraticRoots(1.0, 0.0, 4.0)
        assertEquals(0.0, complexRoots[0].re, 1e-12)
        assertEquals(2.0, complexRoots[0].im, 1e-12)
        assertEquals(-2.0, complexRoots[1].im, 1e-12)

        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Solvers.quadraticRoots(0.0, 0.0, 5.0) }.code)
    }

    @Test
    fun `polynomial roots of a cubic satisfy the polynomial`() {
        val coefficients = listOf(1.0, -6.0, 11.0, -6.0)
        val roots = Solvers.polynomialRoots(coefficients)
        assertEquals(3, roots.size)
        for (root in roots) {
            val value = Solvers.evaluatePolynomial(coefficients, root.re)
            assertTrue(abs(value) < 1e-6, "residual $value for root ${root.re}")
        }
        val realParts = roots.map { it.re }.sorted()
        assertEquals(1.0, realParts[0], 1e-6)
        assertEquals(2.0, realParts[1], 1e-6)
        assertEquals(3.0, realParts[2], 1e-6)
    }

    @Test
    fun `polynomial roots of a quadratic and rejections`() {
        val roots = Solvers.polynomialRoots(listOf(1.0, 0.0, -4.0))
        assertEquals(listOf(-2.0, 2.0), roots.map { it.re }.sorted())
        assertEquals(MathErrorCode.EMPTY_INPUT, assertFailsWith<MathException> { Solvers.polynomialRoots(emptyList()) }.code)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> { Solvers.polynomialRoots(listOf(0.0, 0.0, 0.0)) }.code)
        assertEquals(MathErrorCode.LIMIT_EXCEEDED, assertFailsWith<MathException> {
            Solvers.polynomialRoots(List(22) { 1.0 })
        }.code)
    }

    @Test
    fun `function of x can be used directly with the solvers`() {
        val function = CalculatorEngine.functionOf("x^2 - 2", "x")
        val solution = Solvers.bisection(function, 0.0, 2.0)
        assertEquals(Math.sqrt(2.0), solution.root!!, 1e-8)
    }
}

class CalculusTest {

    @Test
    fun `polynomial derivatives are simplified correctly`() {
        assertEquals("2 * x", Calculus.differentiate("x^2"))
        assertEquals("3 * x^2", Calculus.differentiate("x^3"))
        assertEquals("2 * x + 2", Calculus.differentiate("x^2 + 2x"))
        assertEquals("0", Calculus.differentiate("7"))
        assertEquals("1", Calculus.differentiate("x"))
    }

    @Test
    fun `chain rule for trigonometric and exponential functions`() {
        assertEquals("cos(x)", Calculus.differentiate("sin(x)"))
        assertEquals("-sin(x)", Calculus.differentiate("cos(x)").let { if (it == "-sin(x)") it else Calculus.differentiate("cos(x)") })
        val expDerivative = Calculus.differentiate("exp(x)")
        assertEquals("exp(x)", expDerivative)
        assertEquals("1 / x", Calculus.differentiate("ln(x)"))
        assertTrue(Calculus.differentiate("sin(x^2)").contains("cos(x^2)"))
    }

    @Test
    fun `symbolic derivative agrees with numeric derivative`() {
        val cases = listOf("x^3 - 3x", "sin(x)", "exp(x)", "ln(x)", "x * sin(x)", "1 / x")
        for (expression in cases) {
            val symbolic = Calculus.differentiate(expression)
            val symbolicFunction = CalculatorEngine.functionOf(symbolic, "x")
            val original = CalculatorEngine.functionOf(expression, "x")
            for (point in listOf(0.5, 1.3, 2.1)) {
                val numeric = Solvers.derivative(original, point)
                assertEquals(numeric, symbolicFunction(point), 1e-5, "mismatch for $expression at $point")
            }
        }
    }

    @Test
    fun `unsupported derivatives are rejected honestly`() {
        assertEquals(MathErrorCode.UNSUPPORTED, assertFailsWith<MathException> { Calculus.differentiate("abs(x)") }.code)
        assertEquals(MathErrorCode.UNSUPPORTED, assertFailsWith<MathException> { Calculus.differentiate("5!") }.code)
        assertEquals(MathErrorCode.UNSUPPORTED, assertFailsWith<MathException> { Calculus.differentiate("floor(x)") }.code)
    }

    @Test
    fun `simplification is idempotent and folds constants`() {
        val simplified = Calculus.simplify(ExpressionParser.parse("2 + 3 * 4 - 1"))
        assertEquals("13", ExprPrinter.toInfix(simplified))
        assertEquals(ExprPrinter.toInfix(simplified), ExprPrinter.toInfix(Calculus.simplify(simplified)))
        assertEquals("x", ExprPrinter.toInfix(Calculus.simplify(ExpressionParser.parse("x * 1 + 0"))))
    }
}

class ConstantsTest {

    @Test
    fun `derived constants are consistent with their defining constants`() {
        assertEquals(PhysicalConstants.AVOGADRO * PhysicalConstants.BOLTZMANN, PhysicalConstants.GAS_CONSTANT, 1e-12 * PhysicalConstants.GAS_CONSTANT)
        assertEquals(PhysicalConstants.AVOGADRO * PhysicalConstants.ELEMENTARY_CHARGE, PhysicalConstants.FARADAY, 1e-12 * PhysicalConstants.FARADAY)
        assertEquals(PhysicalConstants.PLANCK / (2 * Math.PI), PhysicalConstants.REDUCED_PLANCK, 1e-30)
        val sigma = 2 * Math.pow(Math.PI, 5.0) * Math.pow(PhysicalConstants.BOLTZMANN, 4.0) /
            (15 * Math.pow(PhysicalConstants.PLANCK, 3.0) * Math.pow(PhysicalConstants.SPEED_OF_LIGHT, 2.0))
        assertEquals(sigma, PhysicalConstants.STEFAN_BOLTZMANN, 1e-30)
    }

    @Test
    fun `measured constants are mutually consistent with CODATA 2022`() {
        val epsilon0 = PhysicalConstants.find("eps0")!!.value
        val mu0 = PhysicalConstants.find("mu0")!!.value
        val alpha = PhysicalConstants.find("alpha")!!.value
        val c = PhysicalConstants.SPEED_OF_LIGHT
        val alphaDerived = PhysicalConstants.ELEMENTARY_CHARGE * PhysicalConstants.ELEMENTARY_CHARGE /
            (4 * Math.PI * epsilon0 * PhysicalConstants.REDUCED_PLANCK * c)
        assertEquals(alpha, alphaDerived, 1e-9 * alpha)
        assertEquals(1.0, mu0 * epsilon0 * c * c, 1e-9)
    }

    @Test
    fun `catalogue metadata is complete`() {
        assertTrue(PhysicalConstants.count >= 20)
        val ids = PhysicalConstants.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (constant in PhysicalConstants.all) {
            assertTrue(constant.name.isNotBlank())
            assertTrue(constant.symbol.isNotBlank())
            assertTrue(constant.source.isNotBlank())
            assertTrue(constant.value.isFinite() && constant.value > 0)
        }
        assertTrue(PhysicalConstants.find("c")!!.exact)
        assertFalse(PhysicalConstants.find("G")!!.exact)
        assertTrue(PhysicalConstants.find("g0")!!.exact)
    }

    @Test
    fun `identities are documented`() {
        assertTrue(MathematicalIdentities.all.size >= 12)
        val ids = MathematicalIdentities.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(MathematicalIdentities.all.all { it.statement.isNotBlank() && it.note.isNotBlank() })
    }
}

class ResistorTest {

    private val black = ResistorColor.BLACK
    private val brown = ResistorColor.BROWN
    private val red = ResistorColor.RED
    private val orange = ResistorColor.ORANGE
    private val yellow = ResistorColor.YELLOW
    private val green = ResistorColor.GREEN
    private val violet = ResistorColor.VIOLET
    private val gold = ResistorColor.GOLD
    private val silver = ResistorColor.SILVER
    private val none = ResistorColor.NONE

    @Test
    fun `four band reference resistors`() {
        val brownBlackRedGold = ResistorCalculator.decode(listOf(brown, black, red, gold))
        assertEquals(1000.0, brownBlackRedGold.resistanceOhms, 1e-9)
        assertEquals(5.0, brownBlackRedGold.tolerancePercent!!, 1e-9)
        assertEquals("1 kΩ", brownBlackRedGold.formattedResistance)
        assertEquals(950.0, brownBlackRedGold.minimumOhms!!, 1e-9)
        assertEquals(1050.0, brownBlackRedGold.maximumOhms!!, 1e-9)

        val yellowVioletOrangeGold = ResistorCalculator.decode(listOf(yellow, violet, orange, gold))
        assertEquals(47000.0, yellowVioletOrangeGold.resistanceOhms, 1e-9)
        assertEquals("47 kΩ", yellowVioletOrangeGold.formattedResistance)

        val brownGreenBrownSilver = ResistorCalculator.decode(listOf(brown, green, brown, silver))
        assertEquals(150.0, brownGreenBrownSilver.resistanceOhms, 1e-9)
        assertEquals(10.0, brownGreenBrownSilver.tolerancePercent!!, 1e-9)
    }

    @Test
    fun `five and six band reference resistors`() {
        val fiveBand = ResistorCalculator.decode(listOf(brown, black, black, red, brown))
        assertEquals(10000.0, fiveBand.resistanceOhms, 1e-9)
        assertEquals(1.0, fiveBand.tolerancePercent!!, 1e-9)
        assertEquals("10 kΩ", fiveBand.formattedResistance)

        val sixBand = ResistorCalculator.decode(listOf(orange, orange, black, brown, brown, red))
        assertEquals(3300.0, sixBand.resistanceOhms, 1e-9)
        assertEquals(1.0, sixBand.tolerancePercent!!, 1e-9)
        assertEquals(50, sixBand.temperatureCoefficientPpm)
    }

    @Test
    fun `gold and silver multipliers are supported`() {
        val goldMultiplier = ResistorCalculator.decode(listOf(red, red, gold, gold))
        assertEquals(2.2, goldMultiplier.resistanceOhms, 1e-12)
        val silverMultiplier = ResistorCalculator.decode(listOf(red, red, silver, silver))
        assertEquals(0.22, silverMultiplier.resistanceOhms, 1e-12)
    }

    @Test
    fun `band validation rejects impossible combinations`() {
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            ResistorCalculator.decode(listOf(gold, black, red, gold))
        }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            ResistorCalculator.decode(listOf(brown, black, red))
        }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            ResistorCalculator.decode(listOf(brown, black, none, gold))
        }.code)
    }

    @Test
    fun `no tolerance band means twenty percent`() {
        val result = ResistorCalculator.decode(listOf(red, red, red, none))
        assertEquals(20.0, result.tolerancePercent!!, 1e-9)
        assertEquals(2200.0, result.resistanceOhms, 1e-9)
        assertEquals(1760.0, result.minimumOhms!!, 1e-9)
        assertEquals(2640.0, result.maximumOhms!!, 1e-9)
    }

    @Test
    fun `resistance formatting uses engineering prefixes`() {
        assertEquals("220 Ω", ResistorCalculator.formatResistance(220.0))
        assertEquals("2.2 kΩ", ResistorCalculator.formatResistance(2200.0))
        assertEquals("4.7 MΩ", ResistorCalculator.formatResistance(4.7e6))
        assertEquals("1.5 GΩ", ResistorCalculator.formatResistance(1.5e9))
    }
}
