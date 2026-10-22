package com.shohankhan.nexcalc.math

/**
 * One unit inside a dimension.
 *
 * Conversions go through a single base unit per dimension:
 * `toBase` converts this unit's value into the base unit and `fromBase` reverses it.
 * Linear units expose [linearFactor] so tests can verify every published factor against
 * independently derived reference values.
 */
data class UnitDef(
    val id: String,
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
    val description: String = "",
    val linearFactor: Double? = null
)

/**
 * A dimension with its own base unit. Units from different categories can never be converted,
 * which is what makes `5 kg → 3 m` impossible by construction.
 */
data class UnitCategory(
    val id: String,
    val name: String,
    val description: String,
    val baseUnitId: String,
    val units: List<UnitDef>
) {
    fun unit(id: String): UnitDef? = units.firstOrNull { it.id == id }

    val baseUnit: UnitDef get() = units.first { it.id == baseUnitId }
}

/**
 * Offline unit registry. No currency, no live rates and no network access of any kind.
 *
 * Bangladeshi land units use the widely used Dhaka convention (1 katha = 720 sq ft,
 * 1 bigha = 20 katha, 1 decimal/shotangsho = 435.6 sq ft) and are labelled as conventional
 * values because regional practice varies.
 */
object UnitRegistry {

    private const val INCH = 0.0254
    private const val FOOT = 12 * INCH
    private const val YARD = 3 * FOOT
    private const val MILE = 5280 * FOOT
    private const val NAUTICAL_MILE = 1852.0
    private const val POUND = 0.45359237
    private const val OUNCE = POUND / 16.0
    private const val GRAIN = 64.79891e-6
    private const val US_GALLON = 231 * INCH * INCH * INCH
    private const val IMPERIAL_GALLON = 4.54609e-3
    private const val LBF = POUND * 9.80665
    private const val STANDARD_GRAVITY = 9.80665
    private const val CALORIE = 4.184
    private const val BTU = 1055.05585262
    private const val HP_MECHANICAL = 550 * FOOT * LBF
    private const val HP_METRIC = 75 * STANDARD_GRAVITY
    private const val ATMOSPHERE = 101325.0
    private const val SQ_FOOT = FOOT * FOOT
    private const val KATHA_SQ_FOOT = 720.0

    private fun linear(id: String, name: String, symbol: String, factor: Double, description: String = ""): UnitDef =
        UnitDef(id, name, symbol, toBase = { it * factor }, fromBase = { it / factor }, description = description, linearFactor = factor)

    private fun affine(
        id: String,
        name: String,
        symbol: String,
        toBase: (Double) -> Double,
        fromBase: (Double) -> Double,
        description: String = ""
    ): UnitDef = UnitDef(id, name, symbol, toBase, fromBase, description)

    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            id = "length",
            name = "Length",
            description = "Base unit: metre (m).",
            baseUnitId = "m",
            units = listOf(
                linear("m", "Metre", "m", 1.0),
                linear("km", "Kilometre", "km", 1000.0),
                linear("cm", "Centimetre", "cm", 0.01),
                linear("mm", "Millimetre", "mm", 0.001),
                linear("um", "Micrometre", "µm", 1e-6),
                linear("nm", "Nanometre", "nm", 1e-9),
                linear("mi", "Mile", "mi", MILE, "International mile = 1609.344 m."),
                linear("yd", "Yard", "yd", YARD),
                linear("ft", "Foot", "ft", FOOT, "International foot = 0.3048 m exactly."),
                linear("in", "Inch", "in", INCH, "International inch = 25.4 mm exactly."),
                linear("nmi", "Nautical mile", "nmi", NAUTICAL_MILE, "Defined as exactly 1852 m."),
                linear("angstrom", "Ångström", "Å", 1e-10),
                linear("au", "Astronomical unit", "au", 1.495978707e11, "IAU 2012 definition."),
                linear("ly", "Light-year", "ly", 9460730472580800.0, "Julian year at c."),
                linear("pc", "Parsec", "pc", 3.0856775814913673e16)
            )
        ),
        UnitCategory(
            id = "area",
            name = "Area",
            description = "Base unit: square metre (m²).",
            baseUnitId = "m2",
            units = listOf(
                linear("m2", "Square metre", "m²", 1.0),
                linear("km2", "Square kilometre", "km²", 1e6),
                linear("cm2", "Square centimetre", "cm²", 1e-4),
                linear("mm2", "Square millimetre", "mm²", 1e-6),
                linear("ha", "Hectare", "ha", 10000.0),
                linear("acre", "Acre", "ac", 4840 * YARD * YARD, "International acre = 4046.8564224 m²."),
                linear("ft2", "Square foot", "ft²", SQ_FOOT),
                linear("in2", "Square inch", "in²", INCH * INCH),
                linear("yd2", "Square yard", "yd²", YARD * YARD),
                linear("mi2", "Square mile", "mi²", MILE * MILE),
                linear("katha", "Katha", "katha", KATHA_SQ_FOOT * SQ_FOOT, "Conventional Dhaka katha = 720 sq ft. Regional practice varies."),
                linear("bigha", "Bigha", "bigha", 20 * KATHA_SQ_FOOT * SQ_FOOT, "Conventional 20-katha bigha = 14 400 sq ft. Regional practice varies."),
                linear("decimal", "Decimal / Shotangsho", "dec", 435.6 * SQ_FOOT, "1/100 acre = 435.6 sq ft. Regional practice varies.")
            )
        ),
        UnitCategory(
            id = "volume",
            name = "Volume & capacity",
            description = "Base unit: cubic metre (m³). Volumetric and liquid capacity units share this dimension.",
            baseUnitId = "m3",
            units = listOf(
                linear("m3", "Cubic metre", "m³", 1.0),
                linear("l", "Litre", "L", 0.001),
                linear("ml", "Millilitre", "mL", 1e-6),
                linear("cm3", "Cubic centimetre", "cm³", 1e-6),
                linear("mm3", "Cubic millimetre", "mm³", 1e-9),
                linear("ft3", "Cubic foot", "ft³", FOOT * FOOT * FOOT),
                linear("in3", "Cubic inch", "in³", INCH * INCH * INCH),
                linear("gal_us", "US gallon", "gal (US)", US_GALLON, "US liquid gallon = 231 in³."),
                linear("gal_imp", "Imperial gallon", "gal (Imp)", IMPERIAL_GALLON, "Imperial gallon = 4.54609 L exactly."),
                linear("qt_us", "US quart", "qt (US)", US_GALLON / 4.0),
                linear("pt_us", "US pint", "pt (US)", US_GALLON / 8.0),
                linear("cup_us", "US cup", "cup", US_GALLON / 16.0, "US customary cup = 236.588 mL."),
                linear("floz_us", "US fluid ounce", "fl oz (US)", US_GALLON / 128.0),
                linear("floz_imp", "Imperial fluid ounce", "fl oz (Imp)", IMPERIAL_GALLON / 160.0),
                linear("tsp_us", "US teaspoon", "tsp", US_GALLON / 768.0)
            )
        ),
        UnitCategory(
            id = "mass",
            name = "Mass",
            description = "Base unit: kilogram (kg).",
            baseUnitId = "kg",
            units = listOf(
                linear("kg", "Kilogram", "kg", 1.0),
                linear("g", "Gram", "g", 0.001),
                linear("mg", "Milligram", "mg", 1e-6),
                linear("ug", "Microgram", "µg", 1e-9),
                linear("t", "Tonne", "t", 1000.0),
                linear("lb", "Pound", "lb", POUND, "International avoirdupois pound = 0.45359237 kg exactly."),
                linear("oz", "Ounce", "oz", OUNCE),
                linear("stone", "Stone", "st", 14 * POUND),
                linear("ton_us", "US short ton", "ton (US)", 2000 * POUND),
                linear("ton_uk", "UK long ton", "ton (UK)", 2240 * POUND),
                linear("carat", "Carat", "ct", 0.0002),
                linear("grain", "Grain", "gr", GRAIN)
            )
        ),
        UnitCategory(
            id = "temperature",
            name = "Temperature",
            description = "Base unit: kelvin (K). Uses scale + offset conversions, not simple ratios.",
            baseUnitId = "k",
            units = listOf(
                affine("k", "Kelvin", "K", { it }, { it }),
                affine("c", "Celsius", "°C", { it + 273.15 }, { it - 273.15 }, "Offset scale: 0 °C = 273.15 K."),
                affine("f", "Fahrenheit", "°F", { (it + 459.67) * 5.0 / 9.0 }, { it * 9.0 / 5.0 - 459.67 }, "0 °C = 32 °F."),
                affine("r", "Rankine", "°R", { it * 5.0 / 9.0 }, { it * 9.0 / 5.0 }, "Absolute scale: 0 °R = 0 K.")
            )
        ),
        UnitCategory(
            id = "time",
            name = "Time",
            description = "Base unit: second (s).",
            baseUnitId = "s",
            units = listOf(
                linear("s", "Second", "s", 1.0),
                linear("ms", "Millisecond", "ms", 1e-3),
                linear("us", "Microsecond", "µs", 1e-6),
                linear("ns", "Nanosecond", "ns", 1e-9),
                linear("min", "Minute", "min", 60.0),
                linear("h", "Hour", "h", 3600.0),
                linear("day", "Day", "d", 86400.0),
                linear("week", "Week", "wk", 604800.0),
                linear("fortnight", "Fortnight", "fn", 1209600.0),
                linear("month", "Month (mean Gregorian)", "mo", 2629746.0, "365.2425 days / 12. Calendar months differ in length."),
                linear("year", "Year (mean Gregorian)", "yr", 31556952.0, "365.2425 days.")
            )
        ),
        UnitCategory(
            id = "speed",
            name = "Speed",
            description = "Base unit: metre per second (m/s).",
            baseUnitId = "mps",
            units = listOf(
                linear("mps", "Metre per second", "m/s", 1.0),
                linear("kmh", "Kilometre per hour", "km/h", 1000.0 / 3600.0),
                linear("mph", "Mile per hour", "mph", MILE / 3600.0),
                linear("fps", "Foot per second", "ft/s", FOOT),
                linear("knot", "Knot", "kn", NAUTICAL_MILE / 3600.0),
                linear("kmps", "Kilometre per second", "km/s", 1000.0),
                linear("mach", "Mach (sea level, 15 °C)", "Ma", 340.29, "Approximate: the speed of sound varies with temperature and altitude.")
            )
        ),
        UnitCategory(
            id = "acceleration",
            name = "Acceleration",
            description = "Base unit: metre per second squared (m/s²).",
            baseUnitId = "mps2",
            units = listOf(
                linear("mps2", "Metre per second squared", "m/s²", 1.0),
                linear("ftps2", "Foot per second squared", "ft/s²", FOOT),
                linear("g0", "Standard gravity", "g₀", STANDARD_GRAVITY, "Exact conventional value 9.80665 m/s²."),
                linear("gal", "Gal", "Gal", 0.01, "1 Gal = 1 cm/s² (CGS).")
            )
        ),
        UnitCategory(
            id = "force",
            name = "Force",
            description = "Base unit: newton (N).",
            baseUnitId = "n",
            units = listOf(
                linear("n", "Newton", "N", 1.0),
                linear("kn", "Kilonewton", "kN", 1000.0),
                linear("dyn", "Dyne", "dyn", 1e-5),
                linear("lbf", "Pound-force", "lbf", LBF, "1 lbf = 1 lb × 9.80665 m/s²."),
                linear("kgf", "Kilogram-force", "kgf", STANDARD_GRAVITY)
            )
        ),
        UnitCategory(
            id = "pressure",
            name = "Pressure",
            description = "Base unit: pascal (Pa).",
            baseUnitId = "pa",
            units = listOf(
                linear("pa", "Pascal", "Pa", 1.0),
                linear("kpa", "Kilopascal", "kPa", 1000.0),
                linear("mpa", "Megapascal", "MPa", 1e6),
                linear("bar", "Bar", "bar", 1e5),
                linear("mbar", "Millibar", "mbar", 100.0),
                linear("atm", "Atmosphere", "atm", ATMOSPHERE, "Standard atmosphere = 101 325 Pa exactly."),
                linear("psi", "Pound per square inch", "psi", LBF / (INCH * INCH)),
                linear("torr", "Torr", "Torr", ATMOSPHERE / 760.0),
                linear("mmhg", "Millimetre of mercury", "mmHg", ATMOSPHERE / 760.0, "Conventional mmHg equals one torr."),
                linear("inhg", "Inch of mercury", "inHg", (ATMOSPHERE / 760.0) * 25.4)
            )
        ),
        UnitCategory(
            id = "energy",
            name = "Energy & work",
            description = "Base unit: joule (J).",
            baseUnitId = "j",
            units = listOf(
                linear("j", "Joule", "J", 1.0),
                linear("kj", "Kilojoule", "kJ", 1000.0),
                linear("mj", "Megajoule", "MJ", 1e6),
                linear("cal", "Calorie (thermochemical)", "cal", CALORIE),
                linear("kcal", "Kilocalorie", "kcal", 1000 * CALORIE, "The dietary Calorie."),
                linear("wh", "Watt-hour", "Wh", 3600.0),
                linear("kwh", "Kilowatt-hour", "kWh", 3.6e6),
                linear("ev", "Electronvolt", "eV", 1.602176634e-19, "Exact SI value."),
                linear("btu", "British thermal unit (IT)", "BTU", BTU, "International Table BTU."),
                linear("erg", "Erg", "erg", 1e-7),
                linear("ftlb", "Foot-pound", "ft·lb", FOOT * LBF)
            )
        ),
        UnitCategory(
            id = "power",
            name = "Power",
            description = "Base unit: watt (W).",
            baseUnitId = "w",
            units = listOf(
                linear("w", "Watt", "W", 1.0),
                linear("kw", "Kilowatt", "kW", 1000.0),
                linear("mw", "Megawatt", "MW", 1e6),
                linear("gw", "Gigawatt", "GW", 1e9),
                linear("hp", "Horsepower (mechanical)", "hp", HP_MECHANICAL, "550 ft·lbf/s = 745.6999 W."),
                linear("hp_metric", "Horsepower (metric)", "PS", HP_METRIC, "75 kgf·m/s = 735.49875 W."),
                linear("btuh", "BTU per hour", "BTU/h", BTU / 3600.0),
                linear("cals", "Calorie per second", "cal/s", CALORIE)
            )
        ),
        UnitCategory(
            id = "torque",
            name = "Torque",
            description = "Base unit: newton metre (N·m).",
            baseUnitId = "nm",
            units = listOf(
                linear("nm", "Newton metre", "N·m", 1.0),
                linear("kgfm", "Kilogram-force metre", "kgf·m", STANDARD_GRAVITY),
                linear("lbfft", "Pound-force foot", "lbf·ft", FOOT * LBF),
                linear("lbfin", "Pound-force inch", "lbf·in", INCH * LBF)
            )
        ),
        UnitCategory(
            id = "density",
            name = "Density",
            description = "Base unit: kilogram per cubic metre (kg/m³).",
            baseUnitId = "kgm3",
            units = listOf(
                linear("kgm3", "Kilogram per cubic metre", "kg/m³", 1.0),
                linear("gcm3", "Gram per cubic centimetre", "g/cm³", 1000.0),
                linear("gml", "Gram per millilitre", "g/mL", 1000.0),
                linear("kgl", "Kilogram per litre", "kg/L", 1000.0),
                linear("lbft3", "Pound per cubic foot", "lb/ft³", POUND / (FOOT * FOOT * FOOT)),
                linear("lbin3", "Pound per cubic inch", "lb/in³", POUND / (INCH * INCH * INCH)),
                linear("tm3", "Tonne per cubic metre", "t/m³", 1000.0)
            )
        ),
        UnitCategory(
            id = "frequency",
            name = "Frequency",
            description = "Base unit: hertz (Hz). Angular frequency (rad/s) is included with a 2π factor and labelled.",
            baseUnitId = "hz",
            units = listOf(
                linear("hz", "Hertz", "Hz", 1.0),
                linear("khz", "Kilohertz", "kHz", 1000.0),
                linear("mhz", "Megahertz", "MHz", 1e6),
                linear("ghz", "Gigahertz", "GHz", 1e9),
                linear("thz", "Terahertz", "THz", 1e12),
                linear("rpm", "Revolutions per minute", "rpm", 1.0 / 60.0),
                linear("rads", "Radians per second", "rad/s", 1.0 / (2 * Math.PI), "Angular frequency: 1 rad/s = 1/(2π) Hz.")
            )
        ),
        UnitCategory(
            id = "angle",
            name = "Angle",
            description = "Base unit: radian (rad).",
            baseUnitId = "rad",
            units = listOf(
                linear("rad", "Radian", "rad", 1.0),
                linear("deg", "Degree", "°", Math.PI / 180.0),
                linear("grad", "Gradian", "grad", Math.PI / 200.0),
                linear("turn", "Turn", "turn", 2 * Math.PI),
                linear("arcmin", "Arcminute", "′", Math.PI / 10800.0),
                linear("arcsec", "Arcsecond", "″", Math.PI / 648000.0),
                linear("mrad", "Milliradian", "mrad", 1e-3)
            )
        ),
        UnitCategory(
            id = "data",
            name = "Data storage",
            description = "Base unit: byte (B). Decimal SI prefixes use powers of 1000; binary IEC prefixes use powers of 1024.",
            baseUnitId = "byte",
            units = listOf(
                linear("bit", "Bit", "bit", 0.125),
                linear("byte", "Byte", "B", 1.0),
                linear("kb", "Kilobyte (1000 B)", "kB", 1e3, "Decimal definition (1 kB = 1000 B)."),
                linear("mb", "Megabyte (10⁶ B)", "MB", 1e6),
                linear("gb", "Gigabyte (10⁹ B)", "GB", 1e9),
                linear("tb", "Terabyte (10¹² B)", "TB", 1e12),
                linear("pb", "Petabyte (10¹⁵ B)", "PB", 1e15),
                linear("kib", "Kibibyte (1024 B)", "KiB", 1024.0, "Binary definition (1 KiB = 1024 B)."),
                linear("mib", "Mebibyte (1024² B)", "MiB", 1048576.0),
                linear("gib", "Gibibyte (1024³ B)", "GiB", 1073741824.0),
                linear("tib", "Tebibyte (1024⁴ B)", "TiB", 1099511627776.0),
                linear("pib", "Pebibyte (1024⁵ B)", "PiB", 1125899906842624.0)
            )
        ),
        UnitCategory(
            id = "data_rate",
            name = "Data transfer rate",
            description = "Base unit: bit per second (bit/s).",
            baseUnitId = "bps",
            units = listOf(
                linear("bps", "Bit per second", "bit/s", 1.0),
                linear("kbps", "Kilobit per second", "kbit/s", 1e3),
                linear("mbps", "Megabit per second", "Mbit/s", 1e6),
                linear("gbps", "Gigabit per second", "Gbit/s", 1e9),
                linear("Bps", "Byte per second", "B/s", 8.0),
                linear("kBps", "Kilobyte per second", "kB/s", 8e3),
                linear("MBps", "Megabyte per second", "MB/s", 8e6),
                linear("GBps", "Gigabyte per second", "GB/s", 8e9),
                linear("KiBps", "Kibibyte per second", "KiB/s", 8192.0),
                linear("MiBps", "Mebibyte per second", "MiB/s", 8388608.0)
            )
        ),
        UnitCategory(
            id = "fuel",
            name = "Fuel economy",
            description = "Base unit: kilometre per litre (km/L). Litres per 100 km is an inverse measure, so this category uses non-linear conversion.",
            baseUnitId = "kmpl",
            units = listOf(
                linear("kmpl", "Kilometre per litre", "km/L", 1.0),
                affine("l100km", "Litres per 100 km", "L/100km", { 100.0 / it }, { 100.0 / it }, "Lower values are more efficient; zero consumption is undefined."),
                linear("mpg_us", "Miles per US gallon", "mpg (US)", MILE / US_GALLON),
                linear("mpg_uk", "Miles per Imperial gallon", "mpg (Imp)", MILE / IMPERIAL_GALLON),
                linear("mipl", "Miles per litre", "mi/L", MILE)
            )
        ),
        UnitCategory(
            id = "voltage",
            name = "Electric potential",
            description = "Base unit: volt (V).",
            baseUnitId = "v",
            units = listOf(
                linear("uv", "Microvolt", "µV", 1e-6),
                linear("mv", "Millivolt", "mV", 1e-3),
                linear("v", "Volt", "V", 1.0),
                linear("kv", "Kilovolt", "kV", 1000.0),
                linear("megav", "Megavolt", "MV", 1e6)
            )
        ),
        UnitCategory(
            id = "current",
            name = "Electric current",
            description = "Base unit: ampere (A).",
            baseUnitId = "a",
            units = listOf(
                linear("ua", "Microampere", "µA", 1e-6),
                linear("ma", "Milliampere", "mA", 1e-3),
                linear("a", "Ampere", "A", 1.0),
                linear("ka", "Kiloampere", "kA", 1000.0)
            )
        ),
        UnitCategory(
            id = "resistance",
            name = "Electrical resistance",
            description = "Base unit: ohm (Ω).",
            baseUnitId = "ohm",
            units = listOf(
                linear("mohm", "Milliohm", "mΩ", 1e-3),
                linear("ohm", "Ohm", "Ω", 1.0),
                linear("kohm", "Kiloohm", "kΩ", 1e3),
                linear("mohm_big", "Megaohm", "MΩ", 1e6),
                linear("gohm", "Gigaohm", "GΩ", 1e9)
            )
        ),
        UnitCategory(
            id = "charge",
            name = "Electric charge",
            description = "Base unit: coulomb (C).",
            baseUnitId = "c",
            units = listOf(
                linear("uc", "Microcoulomb", "µC", 1e-6),
                linear("mc", "Millicoulomb", "mC", 1e-3),
                linear("c", "Coulomb", "C", 1.0),
                linear("ah", "Ampere-hour", "Ah", 3600.0),
                linear("mah", "Milliampere-hour", "mAh", 3.6)
            )
        ),
        UnitCategory(
            id = "prefix",
            name = "SI prefixes",
            description = "Dimensionless multipliers used with SI units.",
            baseUnitId = "none",
            units = listOf(
                linear("yocto", "Yocto (10⁻²⁴)", "y", 1e-24),
                linear("zepto", "Zepto (10⁻²¹)", "z", 1e-21),
                linear("atto", "Atto (10⁻¹⁸)", "a", 1e-18),
                linear("femto", "Femto (10⁻¹⁵)", "f", 1e-15),
                linear("pico", "Pico (10⁻¹²)", "p", 1e-12),
                linear("nano", "Nano (10⁻⁹)", "n", 1e-9),
                linear("micro", "Micro (10⁻⁶)", "µ", 1e-6),
                linear("milli", "Milli (10⁻³)", "m", 1e-3),
                linear("centi", "Centi (10⁻²)", "c", 1e-2),
                linear("deci", "Deci (10⁻¹)", "d", 1e-1),
                linear("none", "One (10⁰)", "—", 1.0),
                linear("deca", "Deca (10¹)", "da", 1e1),
                linear("hecto", "Hecto (10²)", "h", 1e2),
                linear("kilo", "Kilo (10³)", "k", 1e3),
                linear("mega", "Mega (10⁶)", "M", 1e6),
                linear("giga", "Giga (10⁹)", "G", 1e9),
                linear("tera", "Tera (10¹²)", "T", 1e12),
                linear("peta", "Peta (10¹⁵)", "P", 1e15),
                linear("exa", "Exa (10¹⁸)", "E", 1e18),
                linear("zetta", "Zetta (10²¹)", "Z", 1e21),
                linear("yotta", "Yotta (10²⁴)", "Y", 1e24)
            )
        )
    )

    private val categoryById: Map<String, UnitCategory> = categories.associateBy { it.id }

    fun category(id: String): UnitCategory? = categoryById[id]

    val categoryCount: Int get() = categories.size

    val unitCount: Int get() = categories.sumOf { it.units.size }

    /**
     * Converts [value] between two units of the same category.
     * Mixing categories, non-finite input and undefined results raise structured errors.
     */
    fun convert(value: Double, categoryId: String, fromUnitId: String, toUnitId: String): Double {
        val category = category(categoryId)
            ?: throw MathException.invalid("Unknown unit category '$categoryId'.")
        val from = category.unit(fromUnitId)
            ?: throw MathException.invalid("Unknown unit '$fromUnitId' in category '${category.name}'.")
        val to = category.unit(toUnitId)
            ?: throw MathException.invalid("Unknown unit '$toUnitId' in category '${category.name}'.")
        if (!value.isFinite()) throw MathException.invalid("Enter a finite number to convert.")
        val base = from.toBase(value)
        if (!base.isFinite()) throw MathException.domain("This value cannot be represented in ${from.name}.")
        val result = to.fromBase(base)
        if (!result.isFinite()) throw MathException.domain("This conversion is undefined for the supplied value.")
        return result
    }
}
