package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Converter verification.
 *
 * Expected factors are derived independently from the physical relations below (inch = 25.4 mm,
 * mile = 5280 ft, pound = 0.45359237 kg, US gallon = 231 in³, calorie = 4.184 J, atmosphere =
 * 101 325 Pa, horsepower = 550 ft·lbf/s, ...), then compared with the registry's published factor.
 */
class UnitsTest {

    private val inch = 0.0254
    private val foot = 12 * inch
    private val yard = 3 * foot
    private val mile = 5280 * foot
    private val pound = 0.45359237
    private val gallon = 231 * inch * inch * inch
    private val lbf = pound * 9.80665

    @Test
    fun `every category is well formed`() {
        assertTrue(UnitRegistry.categories.size >= 24, "expected at least 24 categories")
        for (category in UnitRegistry.categories) {
            assertTrue(category.units.isNotEmpty(), "${category.id} has no units")
            assertTrue(category.unit(category.baseUnitId) != null, "${category.id} has no base unit")
            val ids = category.units.map { it.id }
            assertEquals(ids.size, ids.toSet().size, "${category.id} has duplicate unit ids")
            for (unit in category.units) {
                assertTrue(unit.name.isNotBlank(), "${category.id}/${unit.id} has no name")
                assertTrue(unit.symbol.isNotBlank(), "${category.id}/${unit.id} has no symbol")
            }
        }
    }

    @Test
    fun `base unit converts to itself`() {
        for (category in UnitRegistry.categories) {
            val base = category.baseUnit
            assertEquals(1.0, base.toBase(1.0), 1e-12)
            assertEquals(1.0, base.fromBase(1.0), 1e-12)
        }
    }

    @Test
    fun `every unit round trips within tolerance`() {
        val samples = listOf(-2.5, 0.0, 0.001, 1.0, 7.0, 1234.5)
        for (category in UnitRegistry.categories) {
            for (from in category.units) {
                for (to in category.units) {
                    for (value in samples) {
                        val converted = to.fromBase(from.toBase(value))
                        if (!converted.isFinite()) continue
                        val back = from.fromBase(to.toBase(converted))
                        if (!back.isFinite()) continue
                        val tolerance = 1e-9 * maxOf(1.0, abs(value))
                        assertTrue(
                            abs(back - value) <= tolerance || abs(value) < 1e-9,
                            "${category.id}: $value ${from.id} -> ${to.id} -> $back"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `length factors match independently derived values`() {
        val expected = mapOf(
            "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "um" to 1e-6, "nm" to 1e-9,
            "mi" to mile, "yd" to yard, "ft" to foot, "in" to inch, "nmi" to 1852.0,
            "angstrom" to 1e-10, "au" to 1.495978707e11, "ly" to 9460730472580800.0,
            "pc" to 3.0856775814913673e16
        )
        assertFactors("length", expected)
    }

    @Test
    fun `area factors match derived values`() {
        val expected = mapOf(
            "m2" to 1.0, "km2" to 1e6, "cm2" to 1e-4, "mm2" to 1e-6, "ha" to 1e4,
            "acre" to 4840 * yard * yard, "ft2" to foot * foot, "in2" to inch * inch,
            "yd2" to yard * yard, "mi2" to mile * mile,
            "katha" to 720 * foot * foot, "bigha" to 14400 * foot * foot, "decimal" to 435.6 * foot * foot
        )
        assertFactors("area", expected)
        assertEquals(4046.8564224, UnitRegistry.convert(1.0, "area", "acre", "m2"), 1e-9)
    }

    @Test
    fun `volume factors respect US and imperial gallons`() {
        assertFactors(
            "volume",
            mapOf(
                "m3" to 1.0, "l" to 0.001, "ml" to 1e-6, "cm3" to 1e-6, "mm3" to 1e-9,
                "ft3" to foot * foot * foot, "in3" to inch * inch * inch,
                "gal_us" to gallon, "gal_imp" to 0.00454609,
                "qt_us" to gallon / 4, "pt_us" to gallon / 8, "cup_us" to gallon / 16,
                "floz_us" to gallon / 128, "floz_imp" to 0.00454609 / 160, "tsp_us" to gallon / 768
            )
        )
        assertEquals(3.785411784, UnitRegistry.convert(1.0, "volume", "gal_us", "l"), 1e-12)
        assertEquals(4.54609, UnitRegistry.convert(1.0, "volume", "gal_imp", "l"), 1e-12)
    }

    @Test
    fun `mass and force factors match derived values`() {
        assertFactors(
            "mass",
            mapOf(
                "kg" to 1.0, "g" to 0.001, "mg" to 1e-6, "ug" to 1e-9, "t" to 1000.0,
                "lb" to pound, "oz" to pound / 16, "stone" to 14 * pound, "ton_us" to 2000 * pound,
                "ton_uk" to 2240 * pound, "carat" to 0.0002, "grain" to 64.79891e-6
            )
        )
        assertFactors(
            "force",
            mapOf("n" to 1.0, "kn" to 1000.0, "dyn" to 1e-5, "lbf" to lbf, "kgf" to 9.80665)
        )
    }

    @Test
    fun `pressure energy and power factors are correct`() {
        assertFactors(
            "pressure",
            mapOf(
                "pa" to 1.0, "kpa" to 1000.0, "mpa" to 1e6, "bar" to 1e5, "mbar" to 100.0,
                "atm" to 101325.0, "psi" to lbf / (inch * inch), "torr" to 101325.0 / 760,
                "mmhg" to 101325.0 / 760, "inhg" to 101325.0 / 760 * 25.4
            )
        )
        assertEquals(6894.757293168361, UnitRegistry.convert(1.0, "pressure", "psi", "pa"), 1e-6)
        assertFactors(
            "energy",
            mapOf(
                "j" to 1.0, "kj" to 1000.0, "mj" to 1e6, "cal" to 4.184, "kcal" to 4184.0,
                "wh" to 3600.0, "kwh" to 3.6e6, "ev" to 1.602176634e-19, "btu" to 1055.05585262,
                "erg" to 1e-7, "ftlb" to foot * lbf
            )
        )
        assertFactors(
            "power",
            mapOf(
                "w" to 1.0, "kw" to 1000.0, "mw" to 1e6, "gw" to 1e9,
                "hp" to 550 * foot * lbf, "hp_metric" to 75 * 9.80665,
                "btuh" to 1055.05585262 / 3600, "cals" to 4.184
            )
        )
        assertEquals(745.6998715822702, UnitRegistry.convert(1.0, "power", "hp", "w"), 1e-9)
    }

    @Test
    fun `temperature uses offsets not ratios`() {
        assertEquals(273.15, UnitRegistry.convert(0.0, "temperature", "c", "k"), 1e-12)
        assertEquals(32.0, UnitRegistry.convert(0.0, "temperature", "c", "f"), 1e-12)
        assertEquals(212.0, UnitRegistry.convert(100.0, "temperature", "c", "f"), 1e-12)
        assertEquals(-40.0, UnitRegistry.convert(-40.0, "temperature", "c", "f"), 1e-12)
        assertEquals(491.67, UnitRegistry.convert(32.0, "temperature", "f", "r"), 1e-9)
        assertEquals(300.0, UnitRegistry.convert(26.85, "temperature", "c", "k"), 1e-9)
    }

    @Test
    fun `data storage separates decimal and binary prefixes`() {
        assertEquals(1000.0, UnitRegistry.convert(1.0, "data", "kb", "byte"), 1e-9)
        assertEquals(1024.0, UnitRegistry.convert(1.0, "data", "kib", "byte"), 1e-9)
        assertEquals(8.0, UnitRegistry.convert(1.0, "data", "byte", "bit"), 1e-12)
        assertEquals(1048576.0, UnitRegistry.convert(1.0, "data", "mib", "byte"), 1e-9)
        assertEquals(0.125, UnitRegistry.convert(1.0, "data_rate", "mbps", "MBps"), 1e-9)
    }

    @Test
    fun `speed and angle reference values`() {
        assertEquals(1.852, UnitRegistry.convert(1.0, "speed", "knot", "kmh"), 1e-12)
        assertEquals(3.6, UnitRegistry.convert(1.0, "speed", "mps", "kmh"), 1e-12)
        assertEquals(1609.344, UnitRegistry.convert(1.0, "speed", "mph", "mps") * 3600, 1e-9)
        assertEquals(180.0, UnitRegistry.convert(Math.PI, "angle", "rad", "deg"), 1e-12)
        assertEquals(100.0, UnitRegistry.convert(90.0, "angle", "deg", "grad"), 1e-12)
        assertEquals(1.0, UnitRegistry.convert(60.0, "frequency", "rpm", "hz"), 1e-12)
    }

    @Test
    fun `fuel economy conversion is inverse`() {
        // 5 L/100km equals 20 km/L.
        assertEquals(20.0, UnitRegistry.convert(5.0, "fuel", "l100km", "kmpl"), 1e-12)
        assertEquals(5.0, UnitRegistry.convert(20.0, "fuel", "kmpl", "l100km"), 1e-12)
        // 1 US mpg equals 0.425143707 km/L.
        assertEquals(0.42514370749763044, UnitRegistry.convert(1.0, "fuel", "mpg_us", "kmpl"), 1e-12)
        assertEquals(MathErrorCode.DOMAIN, assertFailsWith<MathException> {
            UnitRegistry.convert(0.0, "fuel", "l100km", "kmpl")
        }.code)
    }

    @Test
    fun `geometric factors for geometry derived conversions`() {
        assertFactors(
            "acceleration",
            mapOf("mps2" to 1.0, "ftps2" to foot, "g0" to 9.80665, "gal" to 0.01)
        )
        assertFactors(
            "density",
            mapOf(
                "kgm3" to 1.0, "gcm3" to 1000.0, "gml" to 1000.0, "kgl" to 1000.0,
                "lbft3" to pound / (foot * foot * foot), "lbin3" to pound / (inch * inch * inch),
                "tm3" to 1000.0
            )
        )
        assertFactors(
            "torque",
            mapOf("nm" to 1.0, "kgfm" to 9.80665, "lbfft" to foot * lbf, "lbfin" to inch * lbf)
        )
        assertFactors(
            "charge",
            mapOf("uc" to 1e-6, "mc" to 1e-3, "c" to 1.0, "ah" to 3600.0, "mah" to 3.6)
        )
        assertFactors(
            "prefix",
            mapOf(
                "yocto" to 1e-24, "zepto" to 1e-21, "atto" to 1e-18, "femto" to 1e-15,
                "pico" to 1e-12, "nano" to 1e-9, "micro" to 1e-6, "milli" to 1e-3,
                "centi" to 1e-2, "deci" to 1e-1, "none" to 1.0, "deca" to 1e1, "hecto" to 1e2,
                "kilo" to 1e3, "mega" to 1e6, "giga" to 1e9, "tera" to 1e12, "peta" to 1e15,
                "exa" to 1e18, "zetta" to 1e21, "yotta" to 1e24
            )
        )
    }

    @Test
    fun `incompatible dimensions and unknown units are rejected`() {
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            UnitRegistry.convert(1.0, "mass", "kg", "m")
        }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            UnitRegistry.convert(1.0, "unknown", "a", "b")
        }.code)
        assertEquals(MathErrorCode.INVALID_INPUT, assertFailsWith<MathException> {
            UnitRegistry.convert(Double.NaN, "length", "m", "km")
        }.code)
    }

    private fun assertFactors(categoryId: String, expected: Map<String, Double>, tolerance: Double = 1e-9) {
        val category = UnitRegistry.category(categoryId) ?: error("missing category $categoryId")
        for ((unitId, factor) in expected) {
            val unit = category.unit(unitId) ?: error("missing unit $categoryId/$unitId")
            val published = unit.linearFactor ?: error("$categoryId/$unitId is not linear")
            assertEquals(factor, published, tolerance * maxOf(1.0, abs(factor)), "factor mismatch for $categoryId/$unitId")
            assertEquals(factor, unit.toBase(1.0), tolerance * maxOf(1.0, abs(factor)), "toBase mismatch for $categoryId/$unitId")
        }
    }
}
