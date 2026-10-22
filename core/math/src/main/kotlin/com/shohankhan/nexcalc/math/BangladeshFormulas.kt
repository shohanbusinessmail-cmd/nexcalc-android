package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/**
 * Bangladesh-oriented calculators.
 *
 * No official grading scale, tariff or land-conversion rule is hard-coded. Every institutional or
 * regional value is entered by the user, and conventional land values are labelled as such.
 */
internal object BangladeshFormulas {

    private const val SQ_FOOT_IN_SQUARE_METRE = 0.09290304
    private const val KATHA_IN_SQ_FOOT = 720.0
    private const val BIGH_TO_KATHA = 20.0
    private const val DECIMAL_IN_SQ_FOOT = 435.6

    private fun v(label: String, amount: Double, unit: String = "", primary: Boolean = false) = value(label, amount, unit, primary)

    val examPercentage = Formula(
        id = "exam-percentage", name = "Exam marks percentage", category = FormulaCategory.BANGLADESH, subcategory = "Education",
        description = "Percentage of marks obtained in an exam.", notation = "percentage = obtained ÷ total × 100",
        assumptions = listOf("Marks are measured on the same total."),
        inputs = listOf(
            input("obtained", "Marks obtained", "", nonNegative = true, default = "450"),
            input("total", "Total marks", "", positive = true, default = "600")
        ),
        outputLabels = listOf("Percentage" to "%", "Marks lost" to ""),
        compute = { x ->
            val obtained = x.getValue("obtained")
            val total = x.getValue("total")
            if (obtained > total) throw MathException.invalid("Marks obtained cannot exceed the total.")
            result(v("Percentage", obtained / total * 100.0, "%", primary = true), v("Marks lost", total - obtained))
        },
        tests = listOf(testCase(mapOf("obtained" to 450.0, "total" to 600.0), "Percentage" to 75.0, "Marks lost" to 150.0))
    )

    val weightedMarks = Formula(
        id = "weighted-marks", name = "Weighted marks", category = FormulaCategory.BANGLADESH, subcategory = "Education",
        description = "Combines two assessments with user-defined weights.", notation = "weighted = (m₁w₁ + m₂w₂) ÷ (w₁ + w₂)",
        assumptions = listOf("Weights are entered by the user; NEXCALC assumes no institutional weight scheme."),
        inputs = listOf(
            input("marks1", "First marks", "", nonNegative = true, default = "80"),
            input("weight1", "First weight", "%", nonNegative = true, default = "40"),
            input("marks2", "Second marks", "", nonNegative = true, default = "90"),
            input("weight2", "Second weight", "%", nonNegative = true, default = "60")
        ),
        outputLabels = listOf("Weighted marks" to "%", "Total weight" to "%"),
        compute = { x ->
            val m1 = x.getValue("marks1"); val w1 = x.getValue("weight1")
            val m2 = x.getValue("marks2"); val w2 = x.getValue("weight2")
            val totalWeight = w1 + w2
            if (totalWeight <= 0) throw MathException.invalid("The weights must add up to a positive value.")
            result(
                v("Weighted marks", (m1 * w1 + m2 * w2) / totalWeight, "%", primary = true),
                v("Total weight", totalWeight, "%")
            )
        },
        tests = listOf(testCase(mapOf("marks1" to 80.0, "weight1" to 40.0, "marks2" to 90.0, "weight2" to 60.0), "Weighted marks" to 86.0))
    )

    val requiredAttendance = Formula(
        id = "attendance-required", name = "Classes needed to reach an attendance target", category = FormulaCategory.BANGLADESH, subcategory = "Education",
        description = "Number of additional attended classes needed to reach a target attendance percentage.",
        notation = "x = (target·T − 100·A) ÷ (100 − target)",
        assumptions = listOf(
            "All future classes are attended.",
            "The target is below 100% because 100% requires every remaining class."
        ),
        inputs = listOf(
            input("total", "Classes held so far", "", nonNegative = true, integer = true, default = "100"),
            input("attended", "Classes attended", "", nonNegative = true, integer = true, default = "70"),
            input("target", "Target attendance", "%", positive = true, maximum = 99.99, default = "75")
        ),
        outputLabels = listOf("Classes needed" to "classes", "Current attendance" to "%"),
        compute = { x ->
            val total = x.getValue("total"); val attended = x.getValue("attended"); val target = x.getValue("target")
            if (attended > total) throw MathException.invalid("Attended classes cannot exceed classes held.")
            val current = if (total == 0.0) 0.0 else attended / total * 100.0
            val needed = (target * total - 100.0 * attended) / (100.0 - target)
            result(
                v("Classes needed", Math.ceil(Math.max(0.0, needed)), "classes", primary = true),
                v("Current attendance", current, "%"),
                steps = listOf("(${format(target)}×${format(total)} − 100×${format(attended)}) ÷ (100 − ${format(target)})")
            )
        },
        tests = listOf(testCase(mapOf("total" to 100.0, "attended" to 70.0, "target" to 75.0), "Classes needed" to 20.0, "Current attendance" to 70.0))
    )

    val gpa = Formula(
        id = "gpa", name = "GPA from grade points", category = FormulaCategory.BANGLADESH, subcategory = "Education",
        description = "Average grade point across five subjects using the institution's own grade-point scale.",
        notation = "GPA = Σ grade points ÷ number of subjects",
        assumptions = listOf(
            "Grade points are converted from letter grades using your institution's scale before entry.",
            "NEXCALC does not assume any official grading scale."
        ),
        inputs = listOf(
            input("g1", "Subject 1 grade point", "", minimum = 0.0, maximum = 5.0, default = "5"),
            input("g2", "Subject 2 grade point", "", minimum = 0.0, maximum = 5.0, default = "4"),
            input("g3", "Subject 3 grade point", "", minimum = 0.0, maximum = 5.0, default = "4"),
            input("g4", "Subject 4 grade point", "", minimum = 0.0, maximum = 5.0, default = "3.5"),
            input("g5", "Subject 5 grade point", "", minimum = 0.0, maximum = 5.0, default = "5")
        ),
        outputLabels = listOf("GPA" to "", "Total grade points" to ""),
        compute = { x ->
            val points = listOf("g1", "g2", "g3", "g4", "g5").map { x.getValue(it) }
            val total = points.sum()
            result(v("GPA", total / points.size, primary = true), v("Total grade points", total))
        },
        tests = listOf(testCase(mapOf("g1" to 5.0, "g2" to 4.0, "g3" to 4.0, "g4" to 3.5, "g5" to 5.0), "GPA" to 4.3, "Total grade points" to 21.5))
    )

    val cgpa = Formula(
        id = "cgpa-weighted", name = "CGPA from semester results", category = FormulaCategory.BANGLADESH, subcategory = "Education",
        description = "Credit-weighted CGPA across three semesters.", notation = "CGPA = Σ(GPAᵢ × creditsᵢ) ÷ Σ creditsᵢ",
        assumptions = listOf("Semester GPAs use the same scale and credits are the official course credits."),
        inputs = listOf(
            input("gpa1", "Semester 1 GPA", "", minimum = 0.0, maximum = 5.0, default = "3.5"),
            input("credits1", "Semester 1 credits", "", positive = true, default = "20"),
            input("gpa2", "Semester 2 GPA", "", minimum = 0.0, maximum = 5.0, default = "4.0"),
            input("credits2", "Semester 2 credits", "", positive = true, default = "18"),
            input("gpa3", "Semester 3 GPA", "", minimum = 0.0, maximum = 5.0, default = "3.8"),
            input("credits3", "Semester 3 credits", "", positive = true, default = "22")
        ),
        outputLabels = listOf("CGPA" to "", "Total credits" to ""),
        compute = { x ->
            val weighted = x.getValue("gpa1") * x.getValue("credits1") +
                x.getValue("gpa2") * x.getValue("credits2") +
                x.getValue("gpa3") * x.getValue("credits3")
            val credits = x.getValue("credits1") + x.getValue("credits2") + x.getValue("credits3")
            result(v("CGPA", weighted / credits, primary = true), v("Total credits", credits))
        },
        tests = listOf(
            testCase(
                mapOf("gpa1" to 3.5, "credits1" to 20.0, "gpa2" to 4.0, "credits2" to 18.0, "gpa3" to 3.8, "credits3" to 22.0),
                "CGPA" to 3.76, "Total credits" to 60.0
            )
        )
    )

    val katha = Formula(
        id = "land-katha", name = "Katha to square feet and square metres", category = FormulaCategory.BANGLADESH, subcategory = "Land area",
        description = "Converts katha to square feet, square metres and bigha.",
        notation = "sq ft = katha × 720;   m² = sq ft × 0.09290304",
        assumptions = listOf("Uses the conventional Dhaka katha of 720 square feet. Regional practice varies."),
        inputs = listOf(input("katha", "Katha", "katha", nonNegative = true, default = "5")),
        outputLabels = listOf("Square feet" to "ft²", "Square metres" to "m²", "Bigha" to "bigha"),
        compute = { x ->
            val k = x.getValue("katha")
            val sqft = k * KATHA_IN_SQ_FOOT
            result(
                v("Square feet", sqft, "ft²", primary = true),
                v("Square metres", sqft * SQ_FOOT_IN_SQUARE_METRE, "m²"),
                v("Bigha", k / BIGH_TO_KATHA, "bigha")
            )
        },
        tests = listOf(testCase(mapOf("katha" to 5.0), "Square feet" to 3600.0, "Square metres" to 334.45094400000005, "Bigha" to 0.25))
    )

    val bigha = Formula(
        id = "land-bigha", name = "Bigha to katha and square feet", category = FormulaCategory.BANGLADESH, subcategory = "Land area",
        description = "Converts bigha to the conventional 20-katha equivalent and square feet.",
        notation = "katha = bigha × 20;   sq ft = katha × 720",
        assumptions = listOf("Uses the conventional 20-katha bigha of 14 400 square feet. Regional practice varies."),
        inputs = listOf(input("bigha", "Bigha", "bigha", nonNegative = true, default = "2")),
        outputLabels = listOf("Katha" to "katha", "Square feet" to "ft²", "Square metres" to "m²"),
        compute = { x ->
            val b = x.getValue("bigha")
            val k = b * BIGH_TO_KATHA
            val sqft = k * KATHA_IN_SQ_FOOT
            result(
                v("Katha", k, "katha", primary = true),
                v("Square feet", sqft, "ft²"),
                v("Square metres", sqft * SQ_FOOT_IN_SQUARE_METRE, "m²")
            )
        },
        tests = listOf(testCase(mapOf("bigha" to 2.0), "Katha" to 40.0, "Square feet" to 28800.0, "Square metres" to 2675.607552))
    )

    val decimal = Formula(
        id = "land-decimal", name = "Decimal (shotangsho) to square feet", category = FormulaCategory.BANGLADESH, subcategory = "Land area",
        description = "Converts decimal (shotangsho) to square feet and square metres.",
        notation = "sq ft = decimal × 435.6;   m² = sq ft × 0.09290304",
        assumptions = listOf("1 decimal = 1/100 acre = 435.6 square feet, the conventional value used in Bangladesh."),
        inputs = listOf(input("decimal", "Decimal", "decimal", nonNegative = true, default = "3")),
        outputLabels = listOf("Square feet" to "ft²", "Square metres" to "m²"),
        compute = { x ->
            val sqft = x.getValue("decimal") * DECIMAL_IN_SQ_FOOT
            result(v("Square feet", sqft, "ft²", primary = true), v("Square metres", sqft * SQ_FOOT_IN_SQUARE_METRE, "m²"))
        },
        tests = listOf(testCase(mapOf("decimal" to 3.0), "Square feet" to 1306.8, "Square metres" to 121.40569267200003))
    )

    val bricks = Formula(
        id = "construction-bricks", name = "Brick estimate", category = FormulaCategory.BANGLADESH, subcategory = "Construction",
        description = "Estimates bricks for a wall using a user-supplied consumption rate and wastage allowance.",
        notation = "bricks = area × bricks per sq ft × (1 + wastage ÷ 100)",
        assumptions = listOf("The consumption rate depends on brick size and wall thickness, so it is entered by the user."),
        inputs = listOf(
            input("area", "Wall area", "ft²", nonNegative = true, default = "100"),
            input("rate", "Bricks per square foot", "", positive = true, default = "4.5"),
            input("wastage", "Wastage allowance", "%", minimum = 0.0, maximum = 100.0, default = "5")
        ),
        outputLabels = listOf("Bricks required" to "pieces", "Bricks including wastage" to "pieces"),
        compute = { x ->
            val area = x.getValue("area"); val rate = x.getValue("rate"); val wastage = x.getValue("wastage")
            val exact = area * rate
            val withWastage = exact * (1 + wastage / 100.0)
            result(
                v("Bricks required", Math.ceil(withWastage), "pieces", primary = true),
                v("Bricks including wastage", withWastage, "pieces"),
                warnings = listOf("Estimate only: brick size and mortar joints change the real quantity.")
            )
        },
        tests = listOf(testCase(mapOf("area" to 100.0, "rate" to 4.5, "wastage" to 5.0), "Bricks required" to 473.0, "Bricks including wastage" to 472.5))
    )

    val cement = Formula(
        id = "construction-cement", name = "Cement bag estimate", category = FormulaCategory.BANGLADESH, subcategory = "Construction",
        description = "Estimates cement bags from a concrete volume and a user-supplied bag rate.",
        notation = "bags = volume × bags per m³ × (1 + wastage ÷ 100)",
        assumptions = listOf("Mix design varies, so the bag rate per cubic metre is entered by the user."),
        inputs = listOf(
            input("volume", "Concrete volume", "m³", nonNegative = true, default = "10"),
            input("rate", "Bags per cubic metre", "", positive = true, default = "8"),
            input("wastage", "Wastage allowance", "%", minimum = 0.0, maximum = 100.0, default = "5")
        ),
        outputLabels = listOf("Cement bags" to "bags", "Bags including wastage" to "bags"),
        compute = { x ->
            val volume = x.getValue("volume"); val rate = x.getValue("rate"); val wastage = x.getValue("wastage")
            val withWastage = volume * rate * (1 + wastage / 100.0)
            result(
                v("Cement bags", Math.ceil(withWastage), "bags", primary = true),
                v("Bags including wastage", withWastage, "bags"),
                warnings = listOf("Estimate only: confirm the mix design with a qualified engineer.")
            )
        },
        tests = listOf(testCase(mapOf("volume" to 10.0, "rate" to 8.0, "wastage" to 5.0), "Cement bags" to 84.0, "Bags including wastage" to 84.0))
    )

    val paint = Formula(
        id = "construction-paint", name = "Paint estimate", category = FormulaCategory.BANGLADESH, subcategory = "Construction",
        description = "Estimates paint volume from area, coverage and number of coats.",
        notation = "litres = area × coats ÷ coverage × (1 + wastage ÷ 100)",
        assumptions = listOf("Coverage per litre is supplied by the paint manufacturer."),
        inputs = listOf(
            input("area", "Paintable area", "m²", nonNegative = true, default = "200"),
            input("coverage", "Coverage per litre", "m²/L", positive = true, default = "10"),
            input("coats", "Number of coats", "", positive = true, integer = true, maximum = 10.0, default = "2"),
            input("wastage", "Wastage allowance", "%", minimum = 0.0, maximum = 100.0, default = "10")
        ),
        outputLabels = listOf("Paint required" to "L"),
        compute = { x ->
            val litres = x.getValue("area") * x.getValue("coats") / x.getValue("coverage") * (1 + x.getValue("wastage") / 100.0)
            result(v("Paint required", litres, "L", primary = true))
        },
        tests = listOf(testCase(mapOf("area" to 200.0, "coverage" to 10.0, "coats" to 2.0, "wastage" to 10.0), "Paint required" to 44.0))
    )

    val electricityCost = Formula(
        id = "electricity-cost", name = "Electricity bill estimate", category = FormulaCategory.BANGLADESH, subcategory = "Utilities",
        description = "Estimates a bill from units consumed, a user-entered rate, a fixed charge and duty.",
        notation = "bill = (units × rate + fixed charge) × (1 + duty ÷ 100)",
        assumptions = listOf(
            "Tariffs, slabs and duties change and differ by provider, so every rate is entered by the user.",
            "A slab-based bill requires one calculation per slab."
        ),
        inputs = listOf(
            input("units", "Units consumed", "kWh", nonNegative = true, default = "150"),
            input("rate", "Rate per unit", "per kWh", nonNegative = true, default = "7.5"),
            input("fixed", "Fixed charge", "", nonNegative = true, default = "100"),
            input("duty", "Duty or surcharge", "%", minimum = 0.0, maximum = 100.0, default = "5")
        ),
        outputLabels = listOf("Total bill" to "", "Energy charge" to "", "Duty amount" to ""),
        compute = { x ->
            val units = x.getValue("units"); val rate = x.getValue("rate")
            val fixed = x.getValue("fixed"); val duty = x.getValue("duty")
            val energy = units * rate
            val subtotal = energy + fixed
            val dutyAmount = subtotal * duty / 100.0
            result(
                v("Total bill", subtotal + dutyAmount, primary = true),
                v("Energy charge", energy),
                v("Duty amount", dutyAmount)
            )
        },
        tests = listOf(testCase(mapOf("units" to 150.0, "rate" to 7.5, "fixed" to 100.0, "duty" to 5.0), "Total bill" to 1286.25, "Energy charge" to 1125.0, "Duty amount" to 61.25))
    )

    val savings = Formula(
        id = "savings-plan", name = "Monthly savings maturity", category = FormulaCategory.BANGLADESH, subcategory = "Utilities",
        description = "Projected maturity of equal monthly deposits at a user-entered annual rate.",
        notation = "F = P·((1 + r)ⁿ − 1) ÷ r, r = annual rate ÷ 1200",
        assumptions = listOf("Deposits are made at the end of each month and interest is compounded monthly.", "No tax or fees are included."),
        inputs = listOf(
            input("monthly", "Monthly deposit", "", nonNegative = true, default = "1000"),
            input("rate", "Annual interest rate", "%", nonNegative = true, default = "6"),
            input("months", "Months", "", positive = true, integer = true, maximum = 1200.0, default = "12")
        ),
        outputLabels = listOf("Maturity value" to "", "Total deposited" to "", "Interest earned" to ""),
        compute = { x ->
            val monthly = x.getValue("monthly"); val annual = x.getValue("rate"); val months = x.getValue("months")
            val r = annual / 1200.0
            val maturity = if (r == 0.0) monthly * months else monthly * (Math.pow(1 + r, months) - 1) / r
            val deposited = monthly * months
            result(
                v("Maturity value", maturity, primary = true),
                v("Total deposited", deposited),
                v("Interest earned", maturity - deposited)
            )
        },
        tests = listOf(
            testCase(
                mapOf("monthly" to 1000.0, "rate" to 6.0, "months" to 12.0),
                "Maturity value" to 12335.56237289966,
                "Total deposited" to 12000.0,
                "Interest earned" to 335.5623728996612
            )
        )
    )

    val all: List<Formula> = listOf(
        examPercentage, weightedMarks, requiredAttendance, gpa, cgpa, katha, bigha, decimal,
        bricks, cement, paint, electricityCost, savings
    )
}
