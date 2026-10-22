package com.shohankhan.nexcalc.math

/**
 * One embedded reference value.
 *
 * @property exact true when the value is exact by definition (SI defining constants, conventional
 *   standards) or exact by derivation from them; false for measured values with uncertainty.
 */
data class PhysicalConstant(
    val id: String,
    val name: String,
    val symbol: String,
    val value: Double,
    val unit: String,
    val exact: Boolean,
    val description: String,
    val source: String
)

/**
 * Curated offline reference of physical constants.
 *
 * SI defining constants (since the 2019 redefinition) and conventional standards are exact;
 * measured constants carry their CODATA release. Nothing here is fetched at runtime.
 */
object PhysicalConstants {

    private const val CODATA = "CODATA 2022 (NIST)"
    private const val EXACT_SI = "Exact by SI definition (2019 redefinition)"
    private const val DERIVED = "Exact by derivation from SI defining constants"

    const val SPEED_OF_LIGHT = 299792458.0
    const val PLANCK = 6.62607015e-34
    const val ELEMENTARY_CHARGE = 1.602176634e-19
    const val BOLTZMANN = 1.380649e-23
    const val AVOGADRO = 6.02214076e23
    const val GAS_CONSTANT = AVOGADRO * BOLTZMANN
    const val FARADAY = AVOGADRO * ELEMENTARY_CHARGE
    const val STANDARD_GRAVITY = 9.80665
    const val STANDARD_ATMOSPHERE = 101325.0

    /** Reduced Planck constant ħ = h / 2π. */
    val REDUCED_PLANCK = PLANCK / (2 * Math.PI)

    /** Stefan–Boltzmann constant σ = 2π⁵k⁴ / (15 h³ c²). */
    val STEFAN_BOLTZMANN = 2 * Math.pow(Math.PI, 5.0) * Math.pow(BOLTZMANN, 4.0) /
        (15 * Math.pow(PLANCK, 3.0) * Math.pow(SPEED_OF_LIGHT, 2.0))

    val all: List<PhysicalConstant> = listOf(
        PhysicalConstant("c", "Speed of light in vacuum", "c", SPEED_OF_LIGHT, "m/s", true,
            "Upper limit for the propagation of information.", EXACT_SI),
        PhysicalConstant("h", "Planck constant", "h", PLANCK, "J·s", true,
            "Quantum of action.", EXACT_SI),
        PhysicalConstant("hbar", "Reduced Planck constant", "ħ", REDUCED_PLANCK, "J·s", true,
            "h divided by 2π.", DERIVED),
        PhysicalConstant("e", "Elementary charge", "e", ELEMENTARY_CHARGE, "C", true,
            "Charge of a proton.", EXACT_SI),
        PhysicalConstant("k", "Boltzmann constant", "k_B", BOLTZMANN, "J/K", true,
            "Relates temperature to energy.", EXACT_SI),
        PhysicalConstant("na", "Avogadro constant", "N_A", AVOGADRO, "1/mol", true,
            "Number of entities in one mole.", EXACT_SI),
        PhysicalConstant("r", "Molar gas constant", "R", GAS_CONSTANT, "J/(mol·K)", true,
            "N_A × k_B.", DERIVED),
        PhysicalConstant("f", "Faraday constant", "F", FARADAY, "C/mol", true,
            "N_A × e.", DERIVED),
        PhysicalConstant("sigma", "Stefan–Boltzmann constant", "σ", STEFAN_BOLTZMANN, "W/(m²·K⁴)", true,
            "2π⁵k⁴/(15h³c²).", DERIVED),
        PhysicalConstant("g0", "Standard gravity", "g₀", STANDARD_GRAVITY, "m/s²", true,
            "Conventional standard acceleration of free fall.", "ISO 80000-3 conventional value"),
        PhysicalConstant("atm", "Standard atmosphere", "atm", STANDARD_ATMOSPHERE, "Pa", true,
            "Conventional standard pressure.", "ISO 80000 conventional value"),
        PhysicalConstant("G", "Newtonian constant of gravitation", "G", 6.67430e-11, "m³/(kg·s²)", false,
            "Measured; relative standard uncertainty about 2.2×10⁻⁵.", CODATA),
        PhysicalConstant("me", "Electron mass", "m_e", 9.1093837139e-31, "kg", false,
            "Rest mass of the electron.", CODATA),
        PhysicalConstant("mp", "Proton mass", "m_p", 1.67262192595e-27, "kg", false,
            "Rest mass of the proton.", CODATA),
        PhysicalConstant("mn", "Neutron mass", "m_n", 1.67492750056e-27, "kg", false,
            "Rest mass of the neutron.", CODATA),
        PhysicalConstant("u", "Atomic mass constant", "u", 1.66053906892e-27, "kg", false,
            "One twelfth of the mass of a carbon-12 atom.", CODATA),
        PhysicalConstant("alpha", "Fine-structure constant", "α", 7.2973525643e-3, "", false,
            "Dimensionless electromagnetic coupling constant.", CODATA),
        PhysicalConstant("eps0", "Vacuum electric permittivity", "ε₀", 8.8541878188e-12, "F/m", false,
            "Measured relative to the defined constants.", CODATA),
        PhysicalConstant("mu0", "Vacuum magnetic permeability", "μ₀", 1.25663706127e-6, "N/A²", false,
            "Measured after the 2019 SI redefinition.", CODATA),
        PhysicalConstant("ry", "Rydberg constant", "R∞", 10973731.568157, "1/m", false,
            "Wave-number limit of the hydrogen spectrum.", CODATA),
        PhysicalConstant("a0", "Bohr radius", "a₀", 5.29177210544e-11, "m", false,
            "Most probable electron distance in hydrogen.", CODATA)
    )

    fun find(id: String): PhysicalConstant? = all.firstOrNull { it.id == id }

    val count: Int get() = all.size
}

/** Mathematical identities shown in the reference panel. Each one is asserted by a unit test. */
data class MathematicalIdentity(
    val id: String,
    val name: String,
    val statement: String,
    val note: String
)

object MathematicalIdentities {

    val all: List<MathematicalIdentity> = listOf(
        MathematicalIdentity("pythagorean", "Pythagorean identity", "sin²θ + cos²θ = 1", "Holds for every real angle."),
        MathematicalIdentity("double_angle_sin", "Double-angle formula (sine)", "sin(2θ) = 2·sin θ·cos θ", "Angle mode independent."),
        MathematicalIdentity("double_angle_cos", "Double-angle formula (cosine)", "cos(2θ) = cos²θ − sin²θ", "Angle mode independent."),
        MathematicalIdentity("log_product", "Product rule for logarithms", "log_b(x·y) = log_b(x) + log_b(y)", "Requires x > 0, y > 0, b > 0, b ≠ 1."),
        MathematicalIdentity("log_power", "Power rule for logarithms", "log_b(xⁿ) = n·log_b(x)", "Requires x > 0."),
        MathematicalIdentity("euler", "Euler's formula", "e^(iθ) = cos θ + i·sin θ", "Links exponential and trigonometric functions."),
        MathematicalIdentity("euler_identity", "Euler's identity", "e^(iπ) + 1 = 0", "Special case of Euler's formula at θ = π."),
        MathematicalIdentity("binomial", "Binomial theorem", "(a + b)ⁿ = Σ C(n,k)·a^(n−k)·b^k", "Sum runs from k = 0 to n."),
        MathematicalIdentity("difference_squares", "Difference of squares", "a² − b² = (a − b)(a + b)", "Useful for fast multiplication."),
        MathematicalIdentity("arithmetic_sum", "Arithmetic series sum", "Sₙ = n/2·(2a + (n − 1)d)", "For an arithmetic progression with first term a and difference d."),
        MathematicalIdentity("geometric_sum", "Geometric series sum", "Sₙ = a·(1 − rⁿ)/(1 − r)", "Valid for r ≠ 1."),
        MathematicalIdentity("heron", "Heron's formula", "A = √(s(s−a)(s−b)(s−c))", "s is the semi-perimeter of the triangle."),
        MathematicalIdentity("quadratic_formula", "Quadratic formula", "x = (−b ± √(b² − 4ac)) / 2a", "Complex roots appear when the discriminant is negative."),
        MathematicalIdentity("taylor_exp", "Exponential series", "eˣ = Σ xⁿ/n!", "Converges for every real x.")
    )

    fun find(id: String): MathematicalIdentity? = all.firstOrNull { it.id == id }
}
