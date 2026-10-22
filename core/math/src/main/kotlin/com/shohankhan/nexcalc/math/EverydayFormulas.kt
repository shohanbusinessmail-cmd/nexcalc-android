package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/** Work, time, speed, mixture and everyday arithmetic. */
internal object EverydayFormulas {

    private fun val_(label: String, amount: Double, unit: String = "", primary: Boolean = false) = value(label, amount, unit, primary)

    val workRate = Formula(
        id = "work-rate",
        name = "Work rate",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Work & time",
        description = "Rate of work completed per unit of time.",
        notation = "rate = work ÷ time",
        assumptions = listOf("Work is measured in consistent units and time is positive."),
        inputs = listOf(input("work", "Work completed", "", nonNegative = true, default = "120"), input("time", "Time", "h", positive = true, default = "4")),
        outputLabels = listOf("Rate" to "per hour"),
        compute = { v ->
            val_( "Rate", v.getValue("work") / v.getValue("time"), "per hour", primary = true)
                .let { result(it) }
        },
        tests = listOf(testCase(mapOf("work" to 120.0, "time" to 4.0), "Rate" to 30.0))
    )

    val timeFromWork = Formula(
        id = "time-from-work",
        name = "Time to finish a job",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Work & time",
        description = "Time needed to complete a known amount of work at a known rate.",
        notation = "time = work ÷ rate",
        assumptions = listOf("The rate is constant."),
        inputs = listOf(input("work", "Work to complete", "", nonNegative = true, default = "120"), input("rate", "Rate", "per hour", positive = true, default = "30")),
        outputLabels = listOf("Time" to "h"),
        compute = { v -> result(val_("Time", v.getValue("work") / v.getValue("rate"), "h", primary = true)) },
        tests = listOf(testCase(mapOf("work" to 120.0, "rate" to 30.0), "Time" to 4.0))
    )

    val combinedWork = Formula(
        id = "combined-work",
        name = "Combined work",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Work & time",
        description = "Time two workers need when working together at constant rates.",
        notation = "combined = 1 ÷ (1 ÷ t₁ + 1 ÷ t₂)",
        assumptions = listOf("Both rates are constant and independent; times are positive."),
        inputs = listOf(
            input("timeA", "Worker A alone", "h", positive = true, default = "6"),
            input("timeB", "Worker B alone", "h", positive = true, default = "12")
        ),
        outputLabels = listOf("Together" to "h", "Combined rate" to "per hour"),
        compute = { v ->
            val a = v.getValue("timeA")
            val b = v.getValue("timeB")
            val combined = 1.0 / (1.0 / a + 1.0 / b)
            result(
                val_("Together", combined, "h", primary = true),
                val_("Combined rate", 1.0 / combined, "per hour"),
                steps = listOf("1 ÷ (1 ÷ ${format(a)} + 1 ÷ ${format(b)}) = ${format(combined)}")
            )
        },
        tests = listOf(testCase(mapOf("timeA" to 6.0, "timeB" to 12.0), "Together" to 4.0, "Combined rate" to 0.25))
    )

    val pipesCistern = Formula(
        id = "pipes-cistern",
        name = "Pipes and cisterns",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Work & time",
        description = "Net fill time when one pipe fills and another drains a tank.",
        notation = "net = 1 ÷ (1 ÷ fill − 1 ÷ drain)",
        assumptions = listOf("The filling pipe must be faster than the drain, otherwise the tank never fills."),
        inputs = listOf(
            input("fill", "Fill time", "h", positive = true, default = "6"),
            input("drain", "Drain time", "h", positive = true, default = "12")
        ),
        outputLabels = listOf("Net fill time" to "h"),
        compute = { v ->
            val fill = v.getValue("fill")
            val drain = v.getValue("drain")
            val netRate = 1.0 / fill - 1.0 / drain
            if (netRate <= 0) throw MathException.domain("The drain is at least as fast as the fill pipe, so the tank never fills.")
            result(val_("Net fill time", 1.0 / netRate, "h", primary = true))
        },
        tests = listOf(testCase(mapOf("fill" to 6.0, "drain" to 12.0), "Net fill time" to 12.0))
    )

    val speed = Formula(
        id = "speed",
        name = "Speed, distance, time — speed",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Average speed from distance and time.",
        notation = "speed = distance ÷ time",
        assumptions = listOf("Distance and time are measured in consistent units."),
        inputs = listOf(input("distance", "Distance", "km", nonNegative = true, default = "150"), input("time", "Time", "h", positive = true, default = "3")),
        outputLabels = listOf("Speed" to "km/h"),
        compute = { v -> result(val_("Speed", v.getValue("distance") / v.getValue("time"), "km/h", primary = true)) },
        tests = listOf(testCase(mapOf("distance" to 150.0, "time" to 3.0), "Speed" to 50.0))
    )

    val distance = Formula(
        id = "distance",
        name = "Distance from speed and time",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Distance covered at a constant speed.",
        notation = "distance = speed × time",
        assumptions = listOf("The speed is constant."),
        inputs = listOf(input("speed", "Speed", "km/h", nonNegative = true, default = "50"), input("time", "Time", "h", positive = true, default = "3")),
        outputLabels = listOf("Distance" to "km"),
        compute = { v -> result(val_("Distance", v.getValue("speed") * v.getValue("time"), "km", primary = true)) },
        tests = listOf(testCase(mapOf("speed" to 50.0, "time" to 3.0), "Distance" to 150.0))
    )

    val travelTime = Formula(
        id = "travel-time",
        name = "Time from distance and speed",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Time required to cover a distance at a constant speed.",
        notation = "time = distance ÷ speed",
        assumptions = listOf("The speed is constant and non-zero."),
        inputs = listOf(input("distance", "Distance", "km", nonNegative = true, default = "150"), input("speed", "Speed", "km/h", positive = true, default = "50")),
        outputLabels = listOf("Time (hours)" to "h", "Time (minutes)" to "min"),
        compute = { v ->
            val hours = v.getValue("distance") / v.getValue("speed")
            result(value("Time (hours)", hours, "h", primary = true), value("Time (minutes)", hours * 60.0, "min"))
        },
        tests = listOf(testCase(mapOf("distance" to 150.0, "speed" to 50.0), "Time (hours)" to 3.0, "Time (minutes)" to 180.0))
    )

    val averageSpeed = Formula(
        id = "average-speed",
        name = "Average speed for two legs",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Average speed over two distances travelled at different speeds.",
        notation = "average = (d₁ + d₂) ÷ (d₁ ÷ v₁ + d₂ ÷ v₂)",
        assumptions = listOf("Each leg is travelled at a constant speed."),
        inputs = listOf(
            input("d1", "First distance", "km", nonNegative = true, default = "100"),
            input("s1", "First speed", "km/h", positive = true, default = "50"),
            input("d2", "Second distance", "km", nonNegative = true, default = "100"),
            input("s2", "Second speed", "km/h", positive = true, default = "100")
        ),
        outputLabels = listOf("Average speed" to "km/h", "Total time" to "h"),
        compute = { v ->
            val d1 = v.getValue("d1")
            val s1 = v.getValue("s1")
            val d2 = v.getValue("d2")
            val s2 = v.getValue("s2")
            val totalTime = d1 / s1 + d2 / s2
            if (totalTime <= 0) throw MathException.invalid("The total time must be positive.")
            result(
                val_("Average speed", (d1 + d2) / totalTime, "km/h", primary = true),
                val_("Total time", totalTime, "h"),
                warnings = listOf("Average speed is not the arithmetic mean of the two speeds.")
            )
        },
        tests = listOf(testCase(mapOf("d1" to 100.0, "s1" to 50.0, "d2" to 100.0, "s2" to 100.0), "Average speed" to 66.66666666666667, "Total time" to 3.0))
    )

    val relativeSpeed = Formula(
        id = "relative-speed",
        name = "Relative speed",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Relative speed of two bodies moving in the same or opposite directions.",
        notation = "same direction = |v₁ − v₂|;   opposite = v₁ + v₂",
        assumptions = listOf("Speeds are measured in the same unit along a straight line."),
        inputs = listOf(input("v1", "Speed 1", "km/h", nonNegative = true, default = "60"), input("v2", "Speed 2", "km/h", nonNegative = true, default = "40")),
        outputLabels = listOf("Same direction" to "km/h", "Opposite direction" to "km/h"),
        compute = { v ->
            val a = v.getValue("v1")
            val b = v.getValue("v2")
            result(val_("Same direction", Math.abs(a - b), "km/h", primary = true), val_("Opposite direction", a + b, "km/h"))
        },
        tests = listOf(testCase(mapOf("v1" to 60.0, "v2" to 40.0), "Same direction" to 20.0, "Opposite direction" to 100.0))
    )

    val trainCrossing = Formula(
        id = "train-crossing",
        name = "Train crossing a platform",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Time for a train of known length to completely cross a platform.",
        notation = "time = (train length + platform length) ÷ speed",
        assumptions = listOf("Speed is given in km/h and lengths in metres; the result is in seconds."),
        inputs = listOf(
            input("train", "Train length", "m", positive = true, default = "120"),
            input("platform", "Platform length", "m", nonNegative = true, default = "180"),
            input("speed", "Speed", "km/h", positive = true, default = "72")
        ),
        outputLabels = listOf("Time" to "s"),
        compute = { v ->
            val metresPerSecond = v.getValue("speed") / 3.6
            result(val_("Time", (v.getValue("train") + v.getValue("platform")) / metresPerSecond, "s", primary = true))
        },
        tests = listOf(testCase(mapOf("train" to 120.0, "platform" to 180.0, "speed" to 72.0), "Time" to 15.0))
    )

    val upstreamDownstream = Formula(
        id = "upstream-downstream",
        name = "Upstream and downstream speed",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Speed & distance",
        description = "Boat speeds with and against a current.",
        notation = "upstream = boat − current;   downstream = boat + current",
        assumptions = listOf("The current speed is lower than the boat speed in still water."),
        inputs = listOf(input("boat", "Boat speed in still water", "km/h", positive = true, default = "15"), input("current", "Current speed", "km/h", nonNegative = true, default = "3")),
        outputLabels = listOf("Upstream" to "km/h", "Downstream" to "km/h"),
        compute = { v ->
            val boat = v.getValue("boat")
            val current = v.getValue("current")
            if (current >= boat) throw MathException.domain("The current must be slower than the boat for upstream travel to be possible.")
            result(val_("Upstream", boat - current, "km/h", primary = true), val_("Downstream", boat + current, "km/h"))
        },
        tests = listOf(testCase(mapOf("boat" to 15.0, "current" to 3.0), "Upstream" to 12.0, "Downstream" to 18.0))
    )

    val ages = Formula(
        id = "ages",
        name = "Age problem",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Everyday problems",
        description = "Ages after a number of years and the constant age difference.",
        notation = "future age = current age + years",
        assumptions = listOf("Ages advance at the same rate; the difference stays constant."),
        inputs = listOf(
            input("older", "Older person's age", "years", nonNegative = true, default = "40"),
            input("younger", "Younger person's age", "years", nonNegative = true, default = "10"),
            input("years", "Years from now", "years", default = "5")
        ),
        outputLabels = listOf("Older after years" to "years", "Younger after years" to "years", "Age difference" to "years"),
        compute = { v ->
            val older = v.getValue("older")
            val younger = v.getValue("younger")
            val years = v.getValue("years")
            result(
                val_("Older after years", older + years, "years", primary = true),
                val_("Younger after years", younger + years, "years"),
                val_("Age difference", Math.abs(older - younger), "years")
            )
        },
        tests = listOf(testCase(mapOf("older" to 40.0, "younger" to 10.0, "years" to 5.0), "Older after years" to 45.0, "Younger after years" to 15.0, "Age difference" to 30.0))
    )

    val mixture = Formula(
        id = "mixture",
        name = "Mixture concentration",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Mixtures",
        description = "Final concentration when two solutions are mixed.",
        notation = "final% = (v₁c₁ + v₂c₂) ÷ (v₁ + v₂)",
        assumptions = listOf("Concentrations are percentages of the same substance and volumes are additive."),
        inputs = listOf(
            input("v1", "Volume 1", "L", nonNegative = true, default = "100"),
            input("c1", "Concentration 1", "%", minimum = 0.0, maximum = 100.0, default = "20"),
            input("v2", "Volume 2", "L", nonNegative = true, default = "100"),
            input("c2", "Concentration 2", "%", minimum = 0.0, maximum = 100.0, default = "40")
        ),
        outputLabels = listOf("Final concentration" to "%", "Total volume" to "L", "Solute amount" to ""),
        compute = { v ->
            val v1 = v.getValue("v1")
            val c1 = v.getValue("c1")
            val v2 = v.getValue("v2")
            val c2 = v.getValue("c2")
            val totalVolume = v1 + v2
            if (totalVolume <= 0) throw MathException.invalid("The total volume must be positive.")
            val solute = v1 * c1 / 100.0 + v2 * c2 / 100.0
            result(
                value("Final concentration", solute / totalVolume * 100.0, "%", primary = true),
                val_("Total volume", totalVolume, "L"),
                val_("Solute amount", solute)
            )
        },
        tests = listOf(testCase(mapOf("v1" to 100.0, "c1" to 20.0, "v2" to 100.0, "c2" to 40.0), "Final concentration" to 30.0, "Total volume" to 200.0))
    )

    val dilution = Formula(
        id = "dilution",
        name = "Dilution with water",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Mixtures",
        description = "New concentration after adding pure water to a solution.",
        notation = "new% = (v·c) ÷ (v + water)",
        assumptions = listOf("Added water contains none of the solute."),
        inputs = listOf(
            input("volume", "Initial volume", "L", positive = true, default = "4"),
            input("concentration", "Initial concentration", "%", minimum = 0.0, maximum = 100.0, default = "50"),
            input("water", "Water added", "L", nonNegative = true, default = "4")
        ),
        outputLabels = listOf("New concentration" to "%", "Final volume" to "L"),
        compute = { v ->
            val volume = v.getValue("volume")
            val concentration = v.getValue("concentration")
            val water = v.getValue("water")
            val finalVolume = volume + water
            if (finalVolume <= 0) throw MathException.invalid("The final volume must be positive.")
            result(
                value("New concentration", volume * concentration / finalVolume, "%", primary = true),
                val_("Final volume", finalVolume, "L")
            )
        },
        tests = listOf(testCase(mapOf("volume" to 4.0, "concentration" to 50.0, "water" to 4.0), "New concentration" to 25.0, "Final volume" to 8.0))
    )

    val attendance = Formula(
        id = "attendance",
        name = "Attendance percentage",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Everyday problems",
        description = "Present days as a percentage of total days.",
        notation = "attendance% = present ÷ total × 100",
        assumptions = listOf("Only present and absent days are counted."),
        inputs = listOf(input("present", "Days present", "", nonNegative = true, default = "42"), input("total", "Total days", "", positive = true, default = "50")),
        outputLabels = listOf("Attendance" to "%", "Absent days" to ""),
        compute = { v ->
            val present = v.getValue("present")
            val total = v.getValue("total")
            if (present > total) throw MathException.invalid("Present days cannot exceed total days.")
            result(value("Attendance", present / total * 100.0, "%", primary = true), val_("Absent days", total - present))
        },
        tests = listOf(testCase(mapOf("present" to 42.0, "total" to 50.0), "Attendance" to 84.0, "Absent days" to 8.0))
    )

    val workCompletion = Formula(
        id = "work-completion",
        name = "Work completion percentage",
        category = FormulaCategory.EVERYDAY,
        subcategory = "Work & time",
        description = "Progress of a task as a percentage, with remaining work.",
        notation = "completed% = completed ÷ total × 100",
        assumptions = listOf("Units of work are consistent."),
        inputs = listOf(input("completed", "Work completed", "", nonNegative = true, default = "18"), input("total", "Total work", "", positive = true, default = "24")),
        outputLabels = listOf("Completed" to "%", "Remaining" to ""),
        compute = { v ->
            val completed = v.getValue("completed")
            val total = v.getValue("total")
            result(
                value("Completed", completed / total * 100.0, "%", primary = true),
                val_("Remaining", total - completed),
                warnings = if (completed > total) listOf("Completed work exceeds the planned total.") else emptyList()
            )
        },
        tests = listOf(testCase(mapOf("completed" to 18.0, "total" to 24.0), "Completed" to 75.0, "Remaining" to 6.0))
    )

    val all: List<Formula> = listOf(
        workRate, timeFromWork, combinedWork, pipesCistern, speed, distance, travelTime, averageSpeed,
        relativeSpeed, trainCrossing, upstreamDownstream, ages, mixture, dilution, attendance, workCompletion
    )
}
