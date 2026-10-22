package com.shohankhan.nexcalc.math

/** Grouping used by the searchable scientific function browser. */
enum class FunctionCategory(val label: String) {
    BASIC("Basic"),
    POWERS("Powers & roots"),
    TRIGONOMETRY("Trigonometry"),
    HYPERBOLIC("Hyperbolic"),
    LOGARITHMS("Logarithms & exponentials"),
    COMBINATORICS("Combinatorics"),
    NUMBER_THEORY("Number theory"),
    STATISTICS("Statistics"),
    ANGLES("Angles")
}

/**
 * A catalogued engine function. The catalog is the single source of truth for the scientific
 * function browser, global search, and the "every advertised function is executable" test: each
 * entry carries a runnable example whose value is asserted by [FunctionCatalogTest].
 */
data class FunctionInfo(
    val name: String,
    val label: String,
    val category: FunctionCategory,
    val signature: String,
    val description: String,
    val example: String,
    val expectedExample: Double,
    val exampleAngleMode: AngleMode = AngleMode.RADIANS,
    val insertText: String = "$name("
)

/**
 * Every function understood by [Evaluator] plus the metadata shown in the UI.
 */
object FunctionCatalog {

    val functions: List<FunctionInfo> = listOf(
        // Basic
        FunctionInfo("abs", "Absolute value", FunctionCategory.BASIC, "abs(x)", "Distance of x from zero.", "abs(-7.5)", 7.5),
        FunctionInfo("sign", "Sign", FunctionCategory.BASIC, "sign(x)", "-1, 0 or 1 depending on the sign of x.", "sign(-3)", -1.0),
        FunctionInfo("floor", "Floor", FunctionCategory.BASIC, "floor(x)", "Largest integer not greater than x.", "floor(2.9)", 2.0),
        FunctionInfo("ceil", "Ceiling", FunctionCategory.BASIC, "ceil(x)", "Smallest integer not less than x.", "ceil(2.1)", 3.0),
        FunctionInfo("round", "Round", FunctionCategory.BASIC, "round(x)", "Rounds to the nearest integer, halves away from zero.", "round(-2.5)", -3.0),
        FunctionInfo("trunc", "Truncate", FunctionCategory.BASIC, "trunc(x)", "Integer part of x, discarding the fraction.", "trunc(-2.9)", -2.0),
        FunctionInfo("frac", "Fractional part", FunctionCategory.BASIC, "frac(x)", "x minus its integer part.", "frac(3.25)", 0.25),
        FunctionInfo("min", "Minimum", FunctionCategory.BASIC, "min(a, b, ...)", "Smallest of the supplied values.", "min(4, 9, 2)", 2.0),
        FunctionInfo("max", "Maximum", FunctionCategory.BASIC, "max(a, b, ...)", "Largest of the supplied values.", "max(4, 9, 2)", 9.0),
        FunctionInfo("hypot", "Hypotenuse", FunctionCategory.BASIC, "hypot(a, b)", "sqrt(a² + b²) without intermediate overflow.", "hypot(3, 4)", 5.0),

        // Powers & roots
        FunctionInfo("sqrt", "Square root", FunctionCategory.POWERS, "sqrt(x)", "Principal square root; negative real input yields an imaginary result.", "sqrt(2)", 1.4142135623730951),
        FunctionInfo("cbrt", "Cube root", FunctionCategory.POWERS, "cbrt(x)", "Real cube root, defined for negative inputs.", "cbrt(27)", 3.0),
        FunctionInfo("root", "n-th root", FunctionCategory.POWERS, "root(x, n)", "Real n-th root; odd roots accept negative x.", "root(64, 3)", 4.0),
        FunctionInfo("exp", "Exponential", FunctionCategory.POWERS, "exp(x)", "e raised to the power x.", "exp(0)", 1.0),
        FunctionInfo("pow10", "Power of ten", FunctionCategory.POWERS, "pow10(x)", "10 raised to the power x.", "pow10(3)", 1000.0),
        FunctionInfo("pow2", "Power of two", FunctionCategory.POWERS, "pow2(x)", "2 raised to the power x.", "pow2(10)", 1024.0),
        FunctionInfo("square", "Square", FunctionCategory.POWERS, "square(x)", "x multiplied by itself.", "square(12)", 144.0),
        FunctionInfo("cube", "Cube", FunctionCategory.POWERS, "cube(x)", "x multiplied by itself twice.", "cube(-3)", -27.0),

        // Trigonometry (examples evaluated in radians unless noted)
        FunctionInfo("sin", "Sine", FunctionCategory.TRIGONOMETRY, "sin(x)", "Sine of x in the active angle mode.", "sin(pi/2)", 1.0),
        FunctionInfo("cos", "Cosine", FunctionCategory.TRIGONOMETRY, "cos(x)", "Cosine of x in the active angle mode.", "cos(0)", 1.0),
        FunctionInfo("tan", "Tangent", FunctionCategory.TRIGONOMETRY, "tan(x)", "Tangent of x; undefined at odd multiples of 90°.", "tan(pi/4)", 0.9999999999999999),
        FunctionInfo("asin", "Arcsine", FunctionCategory.TRIGONOMETRY, "asin(x)", "Inverse sine in the active angle mode, for -1 ≤ x ≤ 1.", "asin(1)", Math.PI / 2),
        FunctionInfo("acos", "Arccosine", FunctionCategory.TRIGONOMETRY, "acos(x)", "Inverse cosine in the active angle mode, for -1 ≤ x ≤ 1.", "acos(1)", 0.0),
        FunctionInfo("atan", "Arctangent", FunctionCategory.TRIGONOMETRY, "atan(x)", "Inverse tangent in the active angle mode.", "atan(1)", Math.PI / 4),
        FunctionInfo("atan2", "Arctangent of y/x", FunctionCategory.TRIGONOMETRY, "atan2(y, x)", "Quadrant-aware inverse tangent in the active angle mode.", "atan2(1, 0)", Math.PI / 2),
        FunctionInfo("sec", "Secant", FunctionCategory.TRIGONOMETRY, "sec(x)", "1 / cos(x).", "sec(0)", 1.0),
        FunctionInfo("csc", "Cosecant", FunctionCategory.TRIGONOMETRY, "csc(x)", "1 / sin(x).", "csc(pi/2)", 1.0),
        FunctionInfo("cot", "Cotangent", FunctionCategory.TRIGONOMETRY, "cot(x)", "1 / tan(x).", "cot(pi/4)", 1.0),
        FunctionInfo("asec", "Arcsecant", FunctionCategory.TRIGONOMETRY, "asec(x)", "Inverse secant, defined for |x| ≥ 1.", "asec(1)", 0.0),
        FunctionInfo("acsc", "Arccosecant", FunctionCategory.TRIGONOMETRY, "acsc(x)", "Inverse cosecant, defined for |x| ≥ 1.", "acsc(1)", Math.PI / 2),
        FunctionInfo("acot", "Arccotangent", FunctionCategory.TRIGONOMETRY, "acot(x)", "Inverse cotangent with range (0, pi).", "acot(1)", Math.PI / 4),

        // Hyperbolic
        FunctionInfo("sinh", "Hyperbolic sine", FunctionCategory.HYPERBOLIC, "sinh(x)", "Hyperbolic sine (no angle mode).", "sinh(0)", 0.0),
        FunctionInfo("cosh", "Hyperbolic cosine", FunctionCategory.HYPERBOLIC, "cosh(x)", "Hyperbolic cosine (no angle mode).", "cosh(0)", 1.0),
        FunctionInfo("tanh", "Hyperbolic tangent", FunctionCategory.HYPERBOLIC, "tanh(x)", "Hyperbolic tangent (no angle mode).", "tanh(0)", 0.0),
        FunctionInfo("asinh", "Inverse hyperbolic sine", FunctionCategory.HYPERBOLIC, "asinh(x)", "Inverse hyperbolic sine.", "asinh(0)", 0.0),
        FunctionInfo("acosh", "Inverse hyperbolic cosine", FunctionCategory.HYPERBOLIC, "acosh(x)", "Inverse hyperbolic cosine, defined for x ≥ 1.", "acosh(1)", 0.0),
        FunctionInfo("atanh", "Inverse hyperbolic tangent", FunctionCategory.HYPERBOLIC, "atanh(x)", "Inverse hyperbolic tangent, defined for |x| < 1.", "atanh(0)", 0.0),

        // Logarithms
        FunctionInfo("ln", "Natural logarithm", FunctionCategory.LOGARITHMS, "ln(x)", "Logarithm base e, defined for x > 0.", "ln(e)", 1.0),
        FunctionInfo("log", "Common logarithm", FunctionCategory.LOGARITHMS, "log(x)", "Logarithm base 10, defined for x > 0.", "log(1000)", 3.0),
        FunctionInfo("log10", "Logarithm base 10", FunctionCategory.LOGARITHMS, "log10(x)", "Logarithm base 10; same as log(x).", "log10(100)", 2.0),
        FunctionInfo("log2", "Logarithm base 2", FunctionCategory.LOGARITHMS, "log2(x)", "Logarithm base 2, defined for x > 0.", "log2(8)", 3.0),
        FunctionInfo("logb", "Logarithm in base b", FunctionCategory.LOGARITHMS, "logb(x, b)", "Logarithm of x in base b; x > 0 and b > 0, b ≠ 1.", "logb(81, 3)", 4.0),

        // Combinatorics
        FunctionInfo("fact", "Factorial", FunctionCategory.COMBINATORICS, "fact(n)", "n! for an integer 0 ≤ n ≤ 170.", "fact(5)", 120.0),
        FunctionInfo("ncr", "Combinations", FunctionCategory.COMBINATORICS, "ncr(n, r)", "Number of ways to choose r items from n.", "ncr(10, 3)", 120.0),
        FunctionInfo("npr", "Permutations", FunctionCategory.COMBINATORICS, "npr(n, r)", "Number of ordered arrangements of r items from n.", "npr(10, 3)", 720.0),

        // Number theory
        FunctionInfo("mod", "Remainder", FunctionCategory.NUMBER_THEORY, "mod(a, b)", "Remainder using truncation toward zero; b ≠ 0.", "mod(17, 5)", 2.0),
        FunctionInfo("gcd", "Greatest common divisor", FunctionCategory.NUMBER_THEORY, "gcd(a, b)", "Greatest positive integer dividing both operands.", "gcd(48, 18)", 6.0),
        FunctionInfo("lcm", "Least common multiple", FunctionCategory.NUMBER_THEORY, "lcm(a, b)", "Smallest positive integer divisible by both operands.", "lcm(4, 6)", 12.0),

        // Statistics
        FunctionInfo("mean", "Mean", FunctionCategory.STATISTICS, "mean(a, b, ...)", "Arithmetic mean of the supplied values.", "mean(2, 4, 9)", 5.0),
        FunctionInfo("median", "Median", FunctionCategory.STATISTICS, "median(a, b, ...)", "Middle value of the sorted supplied values.", "median(3, 1, 2)", 2.0),
        FunctionInfo("sum", "Sum", FunctionCategory.STATISTICS, "sum(a, b, ...)", "Sum of the supplied values.", "sum(1, 2, 3, 4)", 10.0),

        // Angles
        FunctionInfo("deg", "Radians to degrees", FunctionCategory.ANGLES, "deg(x)", "Converts an angle in radians to degrees.", "deg(pi)", 180.0),
        FunctionInfo("rad", "Degrees to radians", FunctionCategory.ANGLES, "rad(x)", "Converts an angle in degrees to radians.", "rad(180)", Math.PI)
    )

    private val byName: Map<String, FunctionInfo> = functions.associateBy { it.name }

    val names: Set<String> = byName.keys

    fun isFunction(name: String): Boolean = byName.containsKey(name.lowercase())

    fun find(name: String): FunctionInfo? = byName[name.lowercase()]

    /** Functions accepted by the evaluator; kept in sync with [functions] by an automated test. */
    val implementedNames: Set<String> = setOf(
        "abs", "sign", "floor", "ceil", "round", "trunc", "frac", "min", "max", "hypot",
        "sqrt", "cbrt", "root", "exp", "pow10", "pow2", "square", "cube",
        "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "sec", "csc", "cot", "asec", "acsc", "acot",
        "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
        "ln", "log", "log10", "log2", "logb",
        "fact", "ncr", "npr", "mod", "gcd", "lcm",
        "mean", "median", "sum", "deg", "rad"
    )

    fun categoryOrder(): List<FunctionCategory> = FunctionCategory.entries
}
