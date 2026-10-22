package com.shohankhan.nexcalc.math

/**
 * Resistor colour bands with the standard E-series conventions:
 * digit colours, multiplier colours, tolerance colours and the 6-band temperature coefficient.
 */
enum class ResistorColor(
    val label: String,
    val digit: Int?,
    val multiplier: Double?,
    val tolerancePercent: Double?,
    val temperatureCoefficientPpm: Int?
) {
    BLACK("Black", 0, 1.0, null, 250),
    BROWN("Brown", 1, 10.0, 1.0, 100),
    RED("Red", 2, 100.0, 2.0, 50),
    ORANGE("Orange", 3, 1e3, null, 15),
    YELLOW("Yellow", 4, 1e4, null, 25),
    GREEN("Green", 5, 1e5, 0.5, null),
    BLUE("Blue", 6, 1e6, 0.25, 10),
    VIOLET("Violet", 7, 1e7, 0.1, 5),
    GREY("Grey", 8, 1e8, 0.05, null),
    WHITE("White", 9, 1e9, null, null),
    GOLD("Gold", null, 0.1, 5.0, null),
    SILVER("Silver", null, 0.01, 10.0, null),
    NONE("None", null, null, 20.0, null)
}

data class ResistorResult(
    val bandCount: Int,
    val resistanceOhms: Double,
    val tolerancePercent: Double?,
    val temperatureCoefficientPpm: Int?,
    val formattedResistance: String,
    val minimumOhms: Double?,
    val maximumOhms: Double?,
    val explanation: String
)

/**
 * Decodes 4, 5 and 6 band resistors.
 *
 * Verified against reference components in ResistorCalculatorTest; an attractive but incorrect
 * resistor tool would be worse than no tool at all, so every band role is validated strictly.
 */
object ResistorCalculator {

    val supportedBandCounts: List<Int> = listOf(4, 5, 6)

    fun decode(bands: List<ResistorColor>): ResistorResult {
        if (bands.size !in supportedBandCounts) {
            throw MathException.invalid("Choose 4, 5 or 6 bands.")
        }
        val digitCount = if (bands.size == 6) 3 else bands.size - 2
        val digitBands = bands.take(digitCount)
        digitBands.forEach { band ->
            if (band.digit == null) {
                throw MathException.invalid("${band.label} cannot be used as a digit band.")
            }
        }
        val multiplierBand = bands[digitCount]
        val multiplier = multiplierBand.multiplier
            ?: throw MathException.invalid("${multiplierBand.label} cannot be used as a multiplier band.")
        val toleranceBand = bands[digitCount + 1]
        val tolerance = toleranceBand.tolerancePercent
        val temperatureBand = if (bands.size == 6) bands[digitCount + 2] else null
        val temperatureCoefficient = temperatureBand?.temperatureCoefficientPpm

        var significant = 0.0
        digitBands.forEach { significant = significant * 10 + (it.digit ?: 0) }
        val resistance = significant * multiplier
        if (resistance <= 0) {
            throw MathException.domain("The decoded resistance is zero; check the digit bands.")
        }
        val minimum = tolerance?.let { resistance * (1 - it / 100.0) }
        val maximum = tolerance?.let { resistance * (1 + it / 100.0) }
        val formatted = formatResistance(resistance)
        val toleranceText = tolerance?.let { "±${NumberFormatter.format(it, NumberFormatOptions(decimalPrecision = 3, useGrouping = false))}%" }
            ?: "unspecified"
        val temperatureText = temperatureCoefficient?.let { ", ${it} ppm/K" } ?: ""
        return ResistorResult(
            bandCount = bands.size,
            resistanceOhms = resistance,
            tolerancePercent = tolerance,
            temperatureCoefficientPpm = temperatureCoefficient,
            formattedResistance = formatted,
            minimumOhms = minimum,
            maximumOhms = maximum,
            explanation = "${bands.size}-band resistor: $formatted $toleranceText$temperatureText"
        )
    }

    fun formatResistance(ohms: Double, options: NumberFormatOptions = NumberFormatOptions(decimalPrecision = 3)): String {
        val absolute = kotlin.math.abs(ohms)
        return when {
            absolute >= 1e9 -> NumberFormatter.format(ohms / 1e9, options) + " GΩ"
            absolute >= 1e6 -> NumberFormatter.format(ohms / 1e6, options) + " MΩ"
            absolute >= 1e3 -> NumberFormatter.format(ohms / 1e3, options) + " kΩ"
            else -> NumberFormatter.format(ohms, options) + " Ω"
        }
    }
}
