package com.shohankhan.nexcalc.math

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Everyday solvers that do not fit the formula catalogue neatly. */
internal object QuickSolverTools {

    private fun input(id: String, label: String, placeholder: String = "", optional: Boolean = false) =
        ToolInput(id, label, placeholder, help = if (optional) "Leave blank to solve for this value." else "")

    private fun tool(
        id: String,
        name: String,
        description: String,
        inputs: List<ToolInput>,
        tests: List<ToolTest>,
        compute: (Map<String, String>) -> ToolResult
    ) = QuickTool(id, name, ToolCategory.SOLVERS, description, inputs, compute, tests)

    val average = tool(
        "dataset-statistics", "Dataset statistics",
        "Mean, median, mode, range and standard deviation of a list of numbers.",
        listOf(ToolInput("data", "Dataset", "Comma or space separated", "2, 4, 9")),
        listOf(
            ToolTest(
                mapOf("data" to "2, 4, 9"),
                mapOf("Mean" to "5", "Median" to "4", "Mode" to "None", "Range" to "7")
            )
        )
    ) { values ->
        val dataset = Statistics.parseDataset(ToolSupport.text(values, "data"))
        val stats = Statistics.describe(dataset)
        val modes = if (stats.modes.isEmpty()) "None" else stats.modes.joinToString(", ") { ToolSupport.fmt(it) }
        ToolSupport.result(
            "Mean ${ToolSupport.fmt(stats.mean)}",
            "Count" to stats.count.toString(),
            "Mean" to ToolSupport.fmt(stats.mean),
            "Median" to ToolSupport.fmt(stats.median),
            "Mode" to modes,
            "Range" to ToolSupport.fmt(stats.range),
            "Population standard deviation" to ToolSupport.fmt(stats.standardDeviationPopulation),
            "Sample standard deviation" to (stats.standardDeviationSample?.let { ToolSupport.fmt(it) } ?: "—"),
            "Q1" to ToolSupport.fmt(stats.firstQuartile),
            "Q3" to ToolSupport.fmt(stats.thirdQuartile)
        )
    }

    val travel = tool(
        "travel-solver", "Speed, distance and time",
        "Fills in the missing one of distance, speed and time.",
        listOf(
            input("distance", "Distance", "e.g. 150", optional = true),
            input("speed", "Speed", "e.g. 50", optional = true),
            input("time", "Time", "e.g. 3", optional = true)
        ),
        listOf(
            ToolTest(
                mapOf("distance" to "150", "speed" to "50", "time" to ""),
                mapOf("Time" to "3")
            ),
            ToolTest(
                mapOf("distance" to "", "speed" to "60", "time" to "2.5"),
                mapOf("Distance" to "150")
            )
        )
    ) { values ->
        val distance = values["distance"]?.trim()?.toDoubleOrNull()
        val speed = values["speed"]?.trim()?.toDoubleOrNull()
        val time = values["time"]?.trim()?.toDoubleOrNull()
        val provided = listOfNotNull(distance, speed, time).size
        if (provided != 2) throw MathException.invalid("Enter exactly two of distance, speed and time.")
        when {
            distance == null -> ToolSupport.result(
                "Distance = ${ToolSupport.fmt(speed!! * time!!)}",
                "Distance" to ToolSupport.fmt(speed * time), "Speed" to ToolSupport.fmt(speed), "Time" to ToolSupport.fmt(time)
            )
            speed == null -> {
                if (time == null || time <= 0) throw MathException.invalid("Time must be greater than zero.")
                ToolSupport.result(
                    "Speed = ${ToolSupport.fmt(distance / time)}",
                    "Speed" to ToolSupport.fmt(distance / time), "Distance" to ToolSupport.fmt(distance), "Time" to ToolSupport.fmt(time)
                )
            }
            else -> {
                if (speed <= 0) throw MathException.invalid("Speed must be greater than zero.")
                ToolSupport.result(
                    "Time = ${ToolSupport.fmt(distance!! / speed)}",
                    "Time" to ToolSupport.fmt(distance / speed), "Distance" to ToolSupport.fmt(distance), "Speed" to ToolSupport.fmt(speed)
                )
            }
        }
    }

    val work = tool(
        "work-solver", "Work, rate and time",
        "Fills in the missing one of work, rate and time.",
        listOf(
            input("work", "Work", "e.g. 120", optional = true),
            input("rate", "Rate", "e.g. 30", optional = true),
            input("time", "Time", "e.g. 4", optional = true)
        ),
        listOf(
            ToolTest(mapOf("work" to "120", "rate" to "30", "time" to ""), mapOf("Time" to "4")),
            ToolTest(mapOf("work" to "", "rate" to "25", "time" to "4"), mapOf("Work" to "100"))
        )
    ) { values ->
        val workValue = values["work"]?.trim()?.toDoubleOrNull()
        val rate = values["rate"]?.trim()?.toDoubleOrNull()
        val time = values["time"]?.trim()?.toDoubleOrNull()
        if (listOfNotNull(workValue, rate, time).size != 2) throw MathException.invalid("Enter exactly two of work, rate and time.")
        when {
            workValue == null -> ToolSupport.result(
                "Work = ${ToolSupport.fmt(rate!! * time!!)}",
                "Work" to ToolSupport.fmt(rate * time), "Rate" to ToolSupport.fmt(rate), "Time" to ToolSupport.fmt(time)
            )
            rate == null -> {
                if (time == null || time <= 0) throw MathException.invalid("Time must be greater than zero.")
                ToolSupport.result(
                    "Rate = ${ToolSupport.fmt(workValue / time)}",
                    "Rate" to ToolSupport.fmt(workValue / time), "Work" to ToolSupport.fmt(workValue), "Time" to ToolSupport.fmt(time)
                )
            }
            else -> {
                if (rate <= 0) throw MathException.invalid("Rate must be greater than zero.")
                ToolSupport.result(
                    "Time = ${ToolSupport.fmt(workValue!! / rate)}",
                    "Time" to ToolSupport.fmt(workValue / rate), "Work" to ToolSupport.fmt(workValue), "Rate" to ToolSupport.fmt(rate)
                )
            }
        }
    }

    val ageRatio = tool(
        "age-ratio", "Ages from a ratio and a total",
        "Splits a combined age according to a ratio.",
        listOf(input("ratio", "Ratio (older ÷ younger)", "e.g. 3"), input("sum", "Sum of ages", "e.g. 48")),
        listOf(
            ToolTest(mapOf("ratio" to "3", "sum" to "48"), mapOf("Older age" to "36", "Younger age" to "12"))
        )
    ) { values ->
        val ratio = ToolSupport.double(values, "ratio")
        val total = ToolSupport.double(values, "sum")
        if (ratio <= 0) throw MathException.invalid("The ratio must be greater than zero.")
        val younger = total / (ratio + 1.0)
        ToolSupport.result(
            "Older ${ToolSupport.fmt(younger * ratio)} · Younger ${ToolSupport.fmt(younger)}",
            "Older age" to ToolSupport.fmt(younger * ratio),
            "Younger age" to ToolSupport.fmt(younger)
        )
    }

    val alligation = tool(
        "alligation", "Mixture alligation",
        "Ratio in which two concentrations must be mixed to reach a target concentration.",
        listOf(
            input("c1", "First concentration", "%"),
            input("c2", "Second concentration", "%"),
            input("target", "Target concentration", "%")
        ),
        listOf(
            ToolTest(mapOf("c1" to "20", "c2" to "40", "target" to "30"), mapOf("Ratio" to "1 : 1"))
        )
    ) { values ->
        val c1 = ToolSupport.double(values, "c1")
        val c2 = ToolSupport.double(values, "c2")
        val target = ToolSupport.double(values, "target")
        if (c1 == c2) throw MathException.domain("Both concentrations are equal, so any ratio reaches the target.")
        if (target < minOf(c1, c2) || target > maxOf(c1, c2)) {
            throw MathException.domain("The target must lie between the two concentrations.")
        }
        val first = kotlin.math.abs(c2 - target)
        val second = kotlin.math.abs(target - c1)
        val ratio = simplifyRatio(first, second)
        ToolSupport.result(
            "Mix ${ratio.first} : ${ratio.second}",
            "Ratio" to "${ratio.first} : ${ratio.second}",
            "First part" to ToolSupport.fmt(first),
            "Second part" to ToolSupport.fmt(second)
        )
    }

    val simultaneous = tool(
        "simultaneous-2x2", "Two simultaneous equations",
        "Solves a₁x + b₁y = c₁ and a₂x + b₂y = c₂ and verifies the residuals.",
        listOf(
            input("a1", "a₁"), input("b1", "b₁"), input("c1", "c₁"),
            input("a2", "a₂"), input("b2", "b₂"), input("c2", "c₂")
        ),
        listOf(
            ToolTest(
                mapOf("a1" to "2", "b1" to "3", "c1" to "12", "a2" to "4", "b2" to "-1", "c2" to "10"),
                mapOf("x" to "3", "y" to "2")
            )
        )
    ) { values ->
        val matrix = Matrix.of(
            2, 2,
            listOf(
                listOf(ToolSupport.double(values, "a1"), ToolSupport.double(values, "b1")),
                listOf(ToolSupport.double(values, "a2"), ToolSupport.double(values, "b2"))
            )
        )
        val rhs = listOf(ToolSupport.double(values, "c1"), ToolSupport.double(values, "c2"))
        val solution = matrix.solve(rhs)
        val residual = matrix.residual(rhs, solution)
        ToolSupport.result(
            "x = ${ToolSupport.fmt(solution[0])}, y = ${ToolSupport.fmt(solution[1])}",
            "x" to ToolSupport.fmt(solution[0]),
            "y" to ToolSupport.fmt(solution[1]),
            "Determinant" to ToolSupport.fmt(matrix.determinant()),
            "Maximum residual" to ToolSupport.fmt(residual)
        )
    }

    val dateDifference = tool(
        "date-difference", "Days between two dates",
        "Difference in days and weeks between two ISO dates (yyyy-mm-dd).",
        listOf(input("start", "Start date", "yyyy-mm-dd"), input("end", "End date", "yyyy-mm-dd")),
        listOf(
            ToolTest(
                mapOf("start" to "2024-01-01", "end" to "2024-03-01"),
                mapOf("Days" to "60", "Weeks" to "8")
            )
        )
    ) { values ->
        val start = parseDate(ToolSupport.text(values, "start"))
        val end = parseDate(ToolSupport.text(values, "end"))
        val days = ChronoUnit.DAYS.between(start, end)
        ToolSupport.result(
            "$days days",
            "Days" to days.toString(),
            "Weeks" to (days / 7).toString(),
            "Remaining days" to (days % 7).toString()
        )
    }

    val dateAdd = tool(
        "date-add", "Add days to a date",
        "Adds (or subtracts) a number of days from an ISO date.",
        listOf(input("start", "Start date", "yyyy-mm-dd"), input("days", "Days to add", "May be negative")),
        listOf(
            ToolTest(mapOf("start" to "2024-02-28", "days" to "2"), mapOf("Result date" to "2024-03-01"))
        )
    ) { values ->
        val start = parseDate(ToolSupport.text(values, "start"))
        val days = ToolSupport.long(values, "days")
        if (kotlin.math.abs(days) > 3_650_000) throw MathException.limit("The day offset is too large.")
        val result = start.plusDays(days)
        ToolSupport.result(
            result.toString(),
            "Result date" to result.toString(),
            "Day of week" to result.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        )
    }

    /** Converts a decimal ratio into the smallest whole-number ratio that reproduces it. */
    private fun simplifyRatio(first: Double, second: Double): Pair<Long, Long> {
        var scale = 1L
        while (scale < 1_000_000L &&
            (kotlin.math.abs(first * scale - Math.round(first * scale)) > 1e-9 ||
                kotlin.math.abs(second * scale - Math.round(second * scale)) > 1e-9)
        ) {
            scale *= 10L
        }
        val scaledFirst = Math.round(first * scale)
        val scaledSecond = Math.round(second * scale)
        if (scaledFirst == 0L || scaledSecond == 0L) return Pair(scaledFirst, scaledSecond)
        val divisor = NumberTheory.gcd(scaledFirst, scaledSecond).coerceAtLeast(1L)
        return Pair(scaledFirst / divisor, scaledSecond / divisor)
    }

    private fun parseDate(text: String): LocalDate = try {
        LocalDate.parse(text.trim())
    } catch (error: DateTimeParseException) {
        throw MathException.invalid("'$text' is not a valid date. Use the format yyyy-mm-dd.")
    }

    val profitLoss = tool(
        "profit-loss", "Profit and loss",
        "Compares cost price with selling price and reports the gain or loss both absolutely and as a percentage of cost.",
        listOf(input("cost", "Cost price"), input("selling", "Selling price")),
        listOf(
            ToolTest(mapOf("cost" to "100", "selling" to "120"), mapOf("Type" to "Profit", "Amount" to "20", "Percent" to "20")),
            ToolTest(mapOf("cost" to "250", "selling" to "200"), mapOf("Type" to "Loss", "Amount" to "50", "Percent" to "20")),
            ToolTest(mapOf("cost" to "75", "selling" to "75"), mapOf("Type" to "Break-even", "Amount" to "0", "Percent" to "0"))
        )
    ) { values ->
        val cost = ToolSupport.double(values, "cost")
        val selling = ToolSupport.double(values, "selling")
        if (cost <= 0) throw MathException.invalid("The cost price must be greater than zero.")
        val difference = selling - cost
        val type = when {
            difference > 0 -> "Profit"
            difference < 0 -> "Loss"
            else -> "Break-even"
        }
        val percent = kotlin.math.abs(difference) / cost * 100.0
        ToolSupport.result(
            "$type ${ToolSupport.fmt(kotlin.math.abs(difference))} (${ToolSupport.fmt(percent)}%)",
            "Type" to type,
            "Amount" to ToolSupport.fmt(kotlin.math.abs(difference)),
            "Percent" to ToolSupport.fmt(percent),
            steps = listOf("Percentage = |selling − cost| ÷ cost × 100")
        )
    }

    val simpleInterest = tool(
        "simple-interest", "Simple interest",
        "Interest computed only on the principal: I = P × r × t ÷ 100.",
        listOf(input("principal", "Principal"), input("rate", "Annual rate (%)"), input("years", "Time (years)")),
        listOf(
            ToolTest(mapOf("principal" to "1000", "rate" to "5", "years" to "2"), mapOf("Interest" to "100", "Amount" to "1100"))
        )
    ) { values ->
        val principal = ToolSupport.double(values, "principal")
        val rate = ToolSupport.double(values, "rate")
        val years = ToolSupport.double(values, "years")
        if (principal <= 0) throw MathException.invalid("The principal must be greater than zero.")
        if (rate < 0) throw MathException.invalid("The rate cannot be negative.")
        if (years < 0) throw MathException.invalid("The time cannot be negative.")
        val interest = principal * rate * years / 100.0
        ToolSupport.result(
            "Interest ${ToolSupport.fmt(interest)}",
            "Interest" to ToolSupport.fmt(interest),
            "Amount" to ToolSupport.fmt(principal + interest),
            steps = listOf("I = ${ToolSupport.fmt(principal)} × ${ToolSupport.fmt(rate)} × ${ToolSupport.fmt(years)} ÷ 100")
        )
    }

    val compoundInterest = tool(
        "compound-interest", "Compound interest (annual)",
        "Amount with interest compounded once per year: A = P(1 + r/100)^t.",
        listOf(input("principal", "Principal"), input("rate", "Annual rate (%)"), input("years", "Time (years)")),
        listOf(
            ToolTest(mapOf("principal" to "1000", "rate" to "10", "years" to "2"), mapOf("Amount" to "1210", "Interest" to "210"))
        )
    ) { values ->
        val principal = ToolSupport.double(values, "principal")
        val rate = ToolSupport.double(values, "rate")
        val years = ToolSupport.double(values, "years")
        if (principal <= 0) throw MathException.invalid("The principal must be greater than zero.")
        if (rate < 0) throw MathException.invalid("The rate cannot be negative.")
        if (years < 0) throw MathException.invalid("The time cannot be negative.")
        val amount = principal * Math.pow(1.0 + rate / 100.0, years)
        if (!amount.isFinite()) throw MathException.overflow("The amount exceeded the supported range.")
        ToolSupport.result(
            "Amount ${ToolSupport.fmt(amount)}",
            "Amount" to ToolSupport.fmt(amount),
            "Interest" to ToolSupport.fmt(amount - principal),
            steps = listOf("A = ${ToolSupport.fmt(principal)} × (1 + ${ToolSupport.fmt(rate)} ÷ 100)^${ToolSupport.fmt(years)}"),
            warnings = listOf("Compounding frequency is annual; NEXCALC does not assume a bank product.")
        )
    }

    val averageSpeed = tool(
        "average-speed", "Average speed",
        "Average speed from total distance and total time, not the mean of two speeds.",
        listOf(input("distance", "Total distance"), input("time", "Total time")),
        listOf(ToolTest(mapOf("distance" to "120", "time" to "2"), mapOf("Average speed" to "60")))
    ) { values ->
        val distance = ToolSupport.double(values, "distance")
        val time = ToolSupport.double(values, "time")
        if (time <= 0) throw MathException.invalid("The total time must be greater than zero.")
        if (distance < 0) throw MathException.invalid("The distance cannot be negative.")
        val speed = distance / time
        ToolSupport.result(
            "Average speed ${ToolSupport.fmt(speed)}",
            "Average speed" to ToolSupport.fmt(speed),
            steps = listOf("Average speed = total distance ÷ total time"),
            warnings = listOf("Average speed is total distance over total time; it is not the arithmetic mean of separate speeds.")
        )
    }

    val all: List<QuickTool> = listOf(
        average, travel, work, ageRatio, alligation, simultaneous, dateDifference, dateAdd,
        profitLoss, simpleInterest, compoundInterest, averageSpeed
    )
}
