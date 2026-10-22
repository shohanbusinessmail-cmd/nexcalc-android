package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/** Academic mathematics: sequences, probability, algebra and logarithms. */
internal object AcademicFormulas {

    private fun v(label: String, amount: Double, unit: String = "", primary: Boolean = false) = value(label, amount, unit, primary)

    val apNthTerm = Formula(
        id = "ap-nth-term", name = "Arithmetic progression — nth term", category = FormulaCategory.ACADEMIC, subcategory = "Sequences",
        description = "nth term of an arithmetic progression.", notation = "aₙ = a + (n − 1)d",
        assumptions = listOf("The difference between consecutive terms is constant."),
        inputs = listOf(
            input("first", "First term", "", default = "3"),
            input("difference", "Common difference", "", default = "4"),
            input("n", "Term number", "", positive = true, integer = true, maximum = 100000.0, default = "10")
        ),
        outputLabels = listOf("nth term" to "", "Sum of first n terms" to ""),
        compute = { x ->
            val a = x.getValue("first"); val d = x.getValue("difference"); val n = x.getValue("n")
            val term = a + (n - 1) * d
            result(v("nth term", term, primary = true), v("Sum of first n terms", n / 2.0 * (2 * a + (n - 1) * d)))
        },
        tests = listOf(testCase(mapOf("first" to 3.0, "difference" to 4.0, "n" to 10.0), "nth term" to 39.0, "Sum of first n terms" to 210.0))
    )

    val apSum = Formula(
        id = "ap-sum", name = "Arithmetic progression — sum", category = FormulaCategory.ACADEMIC, subcategory = "Sequences",
        description = "Sum of the first n terms of an arithmetic progression.", notation = "Sₙ = n/2 × (2a + (n − 1)d)",
        assumptions = listOf("The progression is arithmetic with constant difference."),
        inputs = listOf(
            input("first", "First term", "", default = "3"),
            input("difference", "Common difference", "", default = "4"),
            input("n", "Number of terms", "", positive = true, integer = true, maximum = 100000.0, default = "10")
        ),
        outputLabels = listOf("Sum" to ""),
        compute = { x ->
            val a = x.getValue("first"); val d = x.getValue("difference"); val n = x.getValue("n")
            result(v("Sum", n / 2.0 * (2 * a + (n - 1) * d), primary = true))
        },
        tests = listOf(testCase(mapOf("first" to 3.0, "difference" to 4.0, "n" to 10.0), "Sum" to 210.0))
    )

    val gpNthTerm = Formula(
        id = "gp-nth-term", name = "Geometric progression — nth term", category = FormulaCategory.ACADEMIC, subcategory = "Sequences",
        description = "nth term of a geometric progression.", notation = "aₙ = a·r^(n−1)",
        assumptions = listOf("The ratio between consecutive terms is constant."),
        inputs = listOf(
            input("first", "First term", "", default = "2"),
            input("ratio", "Common ratio", "", default = "3"),
            input("n", "Term number", "", positive = true, integer = true, maximum = 10000.0, default = "5")
        ),
        outputLabels = listOf("nth term" to "", "Sum of first n terms" to ""),
        compute = { x ->
            val a = x.getValue("first"); val r = x.getValue("ratio"); val n = x.getValue("n")
            val term = a * Math.pow(r, n - 1)
            val sum = if (r == 1.0) a * n else a * (Math.pow(r, n) - 1) / (r - 1)
            result(v("nth term", term, primary = true), v("Sum of first n terms", sum))
        },
        tests = listOf(testCase(mapOf("first" to 2.0, "ratio" to 3.0, "n" to 5.0), "nth term" to 162.0, "Sum of first n terms" to 242.0))
    )

    val gpSum = Formula(
        id = "gp-sum", name = "Geometric progression — sum", category = FormulaCategory.ACADEMIC, subcategory = "Sequences",
        description = "Sum of the first n terms of a geometric progression.", notation = "Sₙ = a(1 − rⁿ) ÷ (1 − r) for r ≠ 1",
        assumptions = listOf("The progression is geometric. A ratio of exactly 1 uses Sₙ = n·a."),
        inputs = listOf(
            input("first", "First term", "", default = "2"),
            input("ratio", "Common ratio", "", default = "3"),
            input("n", "Number of terms", "", positive = true, integer = true, maximum = 10000.0, default = "5")
        ),
        outputLabels = listOf("Sum" to ""),
        compute = { x ->
            val a = x.getValue("first"); val r = x.getValue("ratio"); val n = x.getValue("n")
            val sum = if (r == 1.0) a * n else a * (1 - Math.pow(r, n)) / (1 - r)
            result(v("Sum", sum, primary = true))
        },
        tests = listOf(testCase(mapOf("first" to 2.0, "ratio" to 3.0, "n" to 5.0), "Sum" to 242.0))
    )

    val quadraticRoots = Formula(
        id = "quadratic-roots", name = "Quadratic equation roots", category = FormulaCategory.ACADEMIC, subcategory = "Algebra",
        description = "Roots of ax² + bx + c = 0, including complex roots.", notation = "x = (−b ± √(b² − 4ac)) ÷ (2a)",
        assumptions = listOf("a is non-zero. Complex roots are reported as real and imaginary parts."),
        inputs = listOf(
            input("a", "a", "", default = "1"),
            input("b", "b", "", default = "5"),
            input("c", "c", "", default = "6")
        ),
        outputLabels = listOf("Discriminant" to "", "Root 1 real" to "", "Root 1 imaginary" to "", "Root 2 real" to "", "Root 2 imaginary" to ""),
        compute = { x ->
            val a = x.getValue("a"); val b = x.getValue("b"); val c = x.getValue("c")
            if (a == 0.0) throw MathException.domain("This is not a quadratic equation because a is zero.")
            val roots = Solvers.quadraticRoots(a, b, c)
            val first = roots.first()
            val second = if (roots.size > 1) roots[1] else roots[0]
            result(
                v("Discriminant", b * b - 4 * a * c),
                v("Root 1 real", first.re, primary = true),
                v("Root 1 imaginary", first.im),
                v("Root 2 real", second.re),
                v("Root 2 imaginary", second.im),
                steps = listOf("discriminant = ${format(b)}² − 4×${format(a)}×${format(c)} = ${format(b * b - 4 * a * c)}")
            )
        },
        tests = listOf(
            testCase(mapOf("a" to 1.0, "b" to 5.0, "c" to 6.0), "Discriminant" to 1.0, "Root 1 real" to -3.0, "Root 2 real" to -2.0),
            testCase(mapOf("a" to 1.0, "b" to 0.0, "c" to 4.0), "Discriminant" to -16.0, "Root 1 real" to 0.0, "Root 1 imaginary" to 2.0)
        )
    )

    val combinations = Formula(
        id = "combinations", name = "Combinations (nCr)", category = FormulaCategory.ACADEMIC, subcategory = "Combinatorics",
        description = "Number of ways to choose r items from n.", notation = "nCr = n! ÷ (r!(n − r)!)",
        assumptions = listOf("Items are distinct and order does not matter."),
        inputs = listOf(
            input("n", "n", "", integer = true, minimum = 0.0, maximum = 100000.0, default = "10"),
            input("r", "r", "", integer = true, minimum = 0.0, maximum = 100000.0, default = "3")
        ),
        outputLabels = listOf("Combinations" to ""),
        compute = { x ->
            val n = x.getValue("n"); val r = x.getValue("r")
            if (r > n) throw MathException.domain("r cannot be greater than n.")
            val value = EngineFunctions.apply("ncr", listOf(Complex(n), Complex(r)), AngleMode.RADIANS).re
            result(v("Combinations", value, primary = true))
        },
        tests = listOf(testCase(mapOf("n" to 10.0, "r" to 3.0), "Combinations" to 120.0))
    )

    val permutations = Formula(
        id = "permutations", name = "Permutations (nPr)", category = FormulaCategory.ACADEMIC, subcategory = "Combinatorics",
        description = "Number of ordered arrangements of r items from n.", notation = "nPr = n! ÷ (n − r)!",
        assumptions = listOf("Items are distinct and order matters."),
        inputs = listOf(
            input("n", "n", "", integer = true, minimum = 0.0, maximum = 100000.0, default = "10"),
            input("r", "r", "", integer = true, minimum = 0.0, maximum = 100000.0, default = "3")
        ),
        outputLabels = listOf("Permutations" to ""),
        compute = { x ->
            val n = x.getValue("n"); val r = x.getValue("r")
            if (r > n) throw MathException.domain("r cannot be greater than n.")
            val value = EngineFunctions.apply("npr", listOf(Complex(n), Complex(r)), AngleMode.RADIANS).re
            result(v("Permutations", value, primary = true))
        },
        tests = listOf(testCase(mapOf("n" to 10.0, "r" to 3.0), "Permutations" to 720.0))
    )

    val logarithm = Formula(
        id = "log-change-of-base", name = "Logarithm in a chosen base", category = FormulaCategory.ACADEMIC, subcategory = "Logarithms",
        description = "Logarithm of a value in any valid base.", notation = "log_b(x) = ln(x) ÷ ln(b)",
        assumptions = listOf("x > 0, b > 0 and b ≠ 1."),
        inputs = listOf(
            input("x", "Value", "", positive = true, default = "32"),
            input("base", "Base", "", positive = true, default = "2")
        ),
        outputLabels = listOf("Logarithm" to "", "Natural logarithm" to ""),
        compute = { x ->
            val valueX = x.getValue("x"); val base = x.getValue("base")
            if (base == 1.0) throw MathException.domain("The logarithm base cannot be 1.")
            result(v("Logarithm", Math.log(valueX) / Math.log(base), primary = true), v("Natural logarithm", Math.log(valueX)))
        },
        tests = listOf(testCase(mapOf("x" to 32.0, "base" to 2.0), "Logarithm" to 5.0))
    )

    val setUnion = Formula(
        id = "set-union", name = "Set union size", category = FormulaCategory.ACADEMIC, subcategory = "Sets & probability",
        description = "Size of the union of two sets from their sizes and intersection.", notation = "|A ∪ B| = |A| + |B| − |A ∩ B|",
        assumptions = listOf("Sizes are non-negative whole numbers and the intersection is not larger than either set."),
        inputs = listOf(
            input("a", "|A|", "", integer = true, nonNegative = true, default = "10"),
            input("b", "|B|", "", integer = true, nonNegative = true, default = "8"),
            input("intersection", "|A ∩ B|", "", integer = true, nonNegative = true, default = "3")
        ),
        outputLabels = listOf("|A ∪ B|" to "", "|A only|" to "", "|B only|" to ""),
        compute = { x ->
            val a = x.getValue("a"); val b = x.getValue("b"); val i = x.getValue("intersection")
            if (i > a || i > b) throw MathException.domain("The intersection cannot exceed either set.")
            result(
                v("|A ∪ B|", a + b - i, primary = true),
                v("|A only|", a - i),
                v("|B only|", b - i)
            )
        },
        tests = listOf(testCase(mapOf("a" to 10.0, "b" to 8.0, "intersection" to 3.0), "|A ∪ B|" to 15.0, "|A only|" to 7.0, "|B only|" to 5.0))
    )

    val inclusionExclusion = Formula(
        id = "inclusion-exclusion", name = "Three-set inclusion–exclusion", category = FormulaCategory.ACADEMIC, subcategory = "Sets & probability",
        description = "Size of the union of three sets.", notation = "|A∪B∪C| = Σ|Aᵢ| − Σ|Aᵢ∩Aⱼ| + |A∩B∩C|",
        assumptions = listOf("All sizes are non-negative and consistent with set containment."),
        inputs = listOf(
            input("a", "|A|", "", nonNegative = true, default = "10"),
            input("b", "|B|", "", nonNegative = true, default = "8"),
            input("c", "|C|", "", nonNegative = true, default = "6"),
            input("ab", "|A ∩ B|", "", nonNegative = true, default = "3"),
            input("ac", "|A ∩ C|", "", nonNegative = true, default = "2"),
            input("bc", "|B ∩ C|", "", nonNegative = true, default = "1"),
            input("abc", "|A ∩ B ∩ C|", "", nonNegative = true, default = "1")
        ),
        outputLabels = listOf("Union size" to ""),
        compute = { x ->
            val a = x.getValue("a"); val b = x.getValue("b"); val c = x.getValue("c")
            val ab = x.getValue("ab"); val ac = x.getValue("ac"); val bc = x.getValue("bc"); val abc = x.getValue("abc")
            val union = a + b + c - ab - ac - bc + abc
            if (union < 0) throw MathException.domain("These set sizes are inconsistent.")
            result(v("Union size", union, primary = true))
        },
        tests = listOf(
            testCase(
                mapOf("a" to 10.0, "b" to 8.0, "c" to 6.0, "ab" to 3.0, "ac" to 2.0, "bc" to 1.0, "abc" to 1.0),
                "Union size" to 19.0
            )
        )
    )

    val probabilityUnion = Formula(
        id = "probability-union", name = "Probability of A or B", category = FormulaCategory.ACADEMIC, subcategory = "Sets & probability",
        description = "Addition rule for probabilities.", notation = "P(A ∪ B) = P(A) + P(B) − P(A ∩ B)",
        assumptions = listOf("Probabilities lie between 0 and 1."),
        inputs = listOf(
            input("pa", "P(A)", "", minimum = 0.0, maximum = 1.0, default = "0.4"),
            input("pb", "P(B)", "", minimum = 0.0, maximum = 1.0, default = "0.5"),
            input("pab", "P(A ∩ B)", "", minimum = 0.0, maximum = 1.0, default = "0.2")
        ),
        outputLabels = listOf("P(A ∪ B)" to "", "P(A only)" to "", "P(B only)" to ""),
        compute = { x ->
            val pa = x.getValue("pa"); val pb = x.getValue("pb"); val pab = x.getValue("pab")
            val union = pa + pb - pab
            if (union > 1.0 + 1e-12 || union < 0) throw MathException.domain("These probabilities are inconsistent.")
            result(
                v("P(A ∪ B)", union, primary = true),
                v("P(A only)", pa - pab),
                v("P(B only)", pb - pab)
            )
        },
        tests = listOf(testCase(mapOf("pa" to 0.4, "pb" to 0.5, "pab" to 0.2), "P(A ∪ B)" to 0.7))
    )

    val probabilityIndependent = Formula(
        id = "probability-independent", name = "Independent events", category = FormulaCategory.ACADEMIC, subcategory = "Sets & probability",
        description = "Probability that two independent events both occur.", notation = "P(A ∩ B) = P(A)·P(B)",
        assumptions = listOf("The events are statistically independent."),
        inputs = listOf(
            input("pa", "P(A)", "", minimum = 0.0, maximum = 1.0, default = "0.5"),
            input("pb", "P(B)", "", minimum = 0.0, maximum = 1.0, default = "0.4")
        ),
        outputLabels = listOf("P(A and B)" to "", "P(at least one)" to ""),
        compute = { x ->
            val pa = x.getValue("pa"); val pb = x.getValue("pb")
            result(v("P(A and B)", pa * pb, primary = true), v("P(at least one)", 1 - (1 - pa) * (1 - pb)))
        },
        tests = listOf(testCase(mapOf("pa" to 0.5, "pb" to 0.4), "P(A and B)" to 0.2, "P(at least one)" to 0.7))
    )

    val exponentialGrowth = Formula(
        id = "exponential-growth", name = "Continuous exponential growth", category = FormulaCategory.ACADEMIC, subcategory = "Exponential",
        description = "Value after continuous growth at a constant rate.", notation = "N = N₀·e^(r·t)",
        assumptions = listOf("The growth rate is continuous and constant."),
        inputs = listOf(
            input("initial", "Initial value", "", default = "1000"),
            input("rate", "Continuous rate", "per unit time", default = "0.05"),
            input("time", "Time", "units", default = "10")
        ),
        outputLabels = listOf("Final value" to "", "Growth factor" to ""),
        compute = { x ->
            val initial = x.getValue("initial"); val rate = x.getValue("rate"); val time = x.getValue("time")
            val factor = Math.exp(rate * time)
            result(v("Final value", initial * factor, primary = true), v("Growth factor", factor))
        },
        tests = listOf(testCase(mapOf("initial" to 1000.0, "rate" to 0.05, "time" to 10.0), "Final value" to 1648.7212707001281, "Growth factor" to 1.6487212707001282))
    )

    val exponentialDecay = Formula(
        id = "exponential-decay", name = "Continuous exponential decay", category = FormulaCategory.ACADEMIC, subcategory = "Exponential",
        description = "Value after continuous decay at a constant rate.", notation = "N = N₀·e^(−r·t)",
        assumptions = listOf("The decay rate is continuous and constant."),
        inputs = listOf(
            input("initial", "Initial value", "", default = "500"),
            input("rate", "Decay rate", "per unit time", nonNegative = true, default = "0.5"),
            input("time", "Time", "units", nonNegative = true, default = "2")
        ),
        outputLabels = listOf("Final value" to "", "Remaining fraction" to ""),
        compute = { x ->
            val initial = x.getValue("initial"); val rate = x.getValue("rate"); val time = x.getValue("time")
            val fraction = Math.exp(-rate * time)
            result(v("Final value", initial * fraction, primary = true), v("Remaining fraction", fraction))
        },
        tests = listOf(testCase(mapOf("initial" to 500.0, "rate" to 0.5, "time" to 2.0), "Final value" to 183.93972058572117))
    )

    val binomialSquare = Formula(
        id = "algebraic-square", name = "Square of a binomial", category = FormulaCategory.ACADEMIC, subcategory = "Algebra",
        description = "Expands (a + b)² using the algebraic identity.", notation = "(a + b)² = a² + 2ab + b²",
        assumptions = listOf("a and b are real numbers."),
        inputs = listOf(input("a", "a", "", default = "3"), input("b", "b", "", default = "4")),
        outputLabels = listOf("(a + b)²" to "", "a² + 2ab + b²" to ""),
        compute = { x ->
            val a = x.getValue("a"); val b = x.getValue("b")
            result(v("(a + b)²", (a + b) * (a + b), primary = true), v("a² + 2ab + b²", a * a + 2 * a * b + b * b))
        },
        tests = listOf(testCase(mapOf("a" to 3.0, "b" to 4.0), "(a + b)²" to 49.0, "a² + 2ab + b²" to 49.0))
    )

    val differenceOfSquares = Formula(
        id = "difference-of-squares", name = "Difference of squares", category = FormulaCategory.ACADEMIC, subcategory = "Algebra",
        description = "Computes a² − b² and its factorised form.", notation = "a² − b² = (a − b)(a + b)",
        assumptions = listOf("a and b are real numbers."),
        inputs = listOf(input("a", "a", "", default = "7"), input("b", "b", "", default = "4")),
        outputLabels = listOf("a² − b²" to "", "(a − b)(a + b)" to ""),
        compute = { x ->
            val a = x.getValue("a"); val b = x.getValue("b")
            result(v("a² − b²", a * a - b * b, primary = true), v("(a − b)(a + b)", (a - b) * (a + b)))
        },
        tests = listOf(testCase(mapOf("a" to 7.0, "b" to 4.0), "a² − b²" to 33.0, "(a − b)(a + b)" to 33.0))
    )

    val all: List<Formula> = listOf(
        apNthTerm, apSum, gpNthTerm, gpSum, quadraticRoots, combinations, permutations, logarithm,
        setUnion, inclusionExclusion, probabilityUnion, probabilityIndependent, exponentialGrowth,
        exponentialDecay, binomialSquare, differenceOfSquares
    )
}
