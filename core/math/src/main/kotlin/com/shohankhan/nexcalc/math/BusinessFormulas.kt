package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/**
 * Business and finance arithmetic.
 *
 * Deliberately excludes banking integration, live tax tables and currency exchange. Every rate is
 * entered by the user and every rounding assumption is stated on the formula.
 */
internal object BusinessFormulas {

    private const val REFERENCE = "Standard commercial arithmetic."

    private fun money(label: String, amount: Double, primary: Boolean = false) = value(label, amount, "", primary)

    val profit = Formula(
        id = "profit",
        name = "Profit and profit percentage",
        category = FormulaCategory.BUSINESS,
        subcategory = "Profit & loss",
        description = "Computes absolute profit and profit as a percentage of cost price.",
        notation = "profit = selling − cost;   profit% = profit ÷ cost × 100",
        assumptions = listOf("Profit percentage is measured against the cost price."),
        inputs = listOf(
            input("cost", "Cost price", "", positive = true, default = "100"),
            input("selling", "Selling price", "", nonNegative = true, default = "130")
        ),
        outputLabels = listOf("Profit" to "", "Profit percentage" to "%"),
        compute = { values ->
            val cost = values.getValue("cost")
            val selling = values.getValue("selling")
            val profitAmount = selling - cost
            result(
                money("Profit", profitAmount, primary = true),
                value("Profit percentage", profitAmount / cost * 100.0, "%"),
                steps = listOf("profit = ${format(selling)} − ${format(cost)} = ${format(profitAmount)}"),
                warnings = if (profitAmount < 0) listOf("The selling price is below cost, so this is a loss.") else emptyList()
            )
        },
        tests = listOf(testCase(mapOf("cost" to 100.0, "selling" to 130.0), "Profit" to 30.0, "Profit percentage" to 30.0)),
        reference = REFERENCE
    )

    val loss = Formula(
        id = "loss",
        name = "Loss and loss percentage",
        category = FormulaCategory.BUSINESS,
        subcategory = "Profit & loss",
        description = "Computes absolute loss and loss as a percentage of cost price.",
        notation = "loss = cost − selling;   loss% = loss ÷ cost × 100",
        assumptions = listOf("Loss percentage is measured against the cost price."),
        inputs = listOf(
            input("cost", "Cost price", "", positive = true, default = "100"),
            input("selling", "Selling price", "", nonNegative = true, default = "80")
        ),
        outputLabels = listOf("Loss" to "", "Loss percentage" to "%"),
        compute = { values ->
            val cost = values.getValue("cost")
            val selling = values.getValue("selling")
            val lossAmount = cost - selling
            result(
                money("Loss", lossAmount, primary = true),
                value("Loss percentage", lossAmount / cost * 100.0, "%"),
                warnings = if (lossAmount < 0) listOf("The selling price is above cost, so this is a profit.") else emptyList()
            )
        },
        tests = listOf(testCase(mapOf("cost" to 100.0, "selling" to 80.0), "Loss" to 20.0, "Loss percentage" to 20.0)),
        reference = REFERENCE
    )

    val sellingFromProfit = Formula(
        id = "selling-from-profit",
        name = "Selling price from profit percentage",
        category = FormulaCategory.BUSINESS,
        subcategory = "Profit & loss",
        description = "Finds the selling price that yields a required profit percentage on cost.",
        notation = "selling = cost × (1 + profit% ÷ 100)",
        assumptions = listOf("The profit percentage is applied to the cost price."),
        inputs = listOf(
            input("cost", "Cost price", "", positive = true, default = "100"),
            input("percent", "Required profit", "%", default = "20")
        ),
        outputLabels = listOf("Selling price" to "", "Profit" to ""),
        compute = { values ->
            val cost = values.getValue("cost")
            val percent = values.getValue("percent")
            val selling = cost * (1 + percent / 100.0)
            result(money("Selling price", selling, primary = true), money("Profit", selling - cost))
        },
        tests = listOf(testCase(mapOf("cost" to 100.0, "percent" to 20.0), "Selling price" to 120.0, "Profit" to 20.0)),
        reference = REFERENCE
    )

    val markup = Formula(
        id = "markup",
        name = "Markup and margin",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Applies a markup to a cost price and reports the resulting margin.",
        notation = "price = cost × (1 + markup% ÷ 100);   margin% = markup ÷ (100 + markup) × 100",
        assumptions = listOf("Markup is measured against cost, margin against selling price."),
        inputs = listOf(
            input("cost", "Cost price", "", positive = true, default = "100"),
            input("markup", "Markup", "%", default = "25")
        ),
        outputLabels = listOf("Selling price" to "", "Margin" to "%"),
        compute = { values ->
            val cost = values.getValue("cost")
            val markupPercent = values.getValue("markup")
            val price = cost * (1 + markupPercent / 100.0)
            val margin = if (price == 0.0) 0.0 else (price - cost) / price * 100.0
            result(money("Selling price", price, primary = true), value("Margin", margin, "%"))
        },
        tests = listOf(testCase(mapOf("cost" to 100.0, "markup" to 25.0), "Selling price" to 125.0, "Margin" to 20.0)),
        reference = REFERENCE
    )

    val margin = Formula(
        id = "margin",
        name = "Margin percentage",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Computes gross margin percentage from cost and selling price.",
        notation = "margin% = (selling − cost) ÷ selling × 100",
        assumptions = listOf("The selling price must be non-zero."),
        inputs = listOf(
            input("cost", "Cost price", "", nonNegative = true, default = "100"),
            input("selling", "Selling price", "", positive = true, default = "125")
        ),
        outputLabels = listOf("Margin" to "%", "Markup on cost" to "%"),
        compute = { values ->
            val cost = values.getValue("cost")
            val selling = values.getValue("selling")
            val marginPercent = (selling - cost) / selling * 100.0
            val markupPercent = if (cost == 0.0) 0.0 else (selling - cost) / cost * 100.0
            result(value("Margin", marginPercent, "%", primary = true), value("Markup on cost", markupPercent, "%"))
        },
        tests = listOf(testCase(mapOf("cost" to 100.0, "selling" to 125.0), "Margin" to 20.0, "Markup on cost" to 25.0)),
        reference = REFERENCE
    )

    val discount = Formula(
        id = "discount",
        name = "Discount",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Applies a percentage discount to a marked price.",
        notation = "selling = marked × (1 − discount% ÷ 100)",
        assumptions = listOf("A single discount is applied to the marked price."),
        inputs = listOf(
            input("marked", "Marked price", "", nonNegative = true, default = "1000"),
            input("discount", "Discount", "%", minimum = 0.0, maximum = 100.0, default = "15")
        ),
        outputLabels = listOf("Selling price" to "", "Amount saved" to ""),
        compute = { values ->
            val marked = values.getValue("marked")
            val percent = values.getValue("discount")
            val saved = marked * percent / 100.0
            result(money("Selling price", marked - saved, primary = true), money("Amount saved", saved))
        },
        tests = listOf(testCase(mapOf("marked" to 1000.0, "discount" to 15.0), "Selling price" to 850.0, "Amount saved" to 150.0)),
        reference = REFERENCE
    )

    val successiveDiscount = Formula(
        id = "successive-discount",
        name = "Successive discounts",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Combines two consecutive discounts and reports the single equivalent discount.",
        notation = "final = marked × (1 − d1 ÷ 100) × (1 − d2 ÷ 100)",
        assumptions = listOf("The second discount applies to the price after the first discount."),
        inputs = listOf(
            input("marked", "Marked price", "", nonNegative = true, default = "1000"),
            input("first", "First discount", "%", minimum = 0.0, maximum = 100.0, default = "10"),
            input("second", "Second discount", "%", minimum = 0.0, maximum = 100.0, default = "20")
        ),
        outputLabels = listOf("Final price" to "", "Equivalent discount" to "%"),
        compute = { values ->
            val marked = values.getValue("marked")
            val first = values.getValue("first")
            val second = values.getValue("second")
            val finalPrice = marked * (1 - first / 100.0) * (1 - second / 100.0)
            val equivalent = if (marked == 0.0) 0.0 else (1 - finalPrice / marked) * 100.0
            result(
                money("Final price", finalPrice, primary = true),
                value("Equivalent discount", equivalent, "%"),
                warnings = listOf("Successive discounts never simply add: 10% then 20% is 28%, not 30%.")
            )
        },
        tests = listOf(testCase(mapOf("marked" to 1000.0, "first" to 10.0, "second" to 20.0), "Final price" to 720.0, "Equivalent discount" to 28.0)),
        reference = REFERENCE
    )

    val tax = Formula(
        id = "tax-vat",
        name = "Tax or VAT on an amount",
        category = FormulaCategory.BUSINESS,
        subcategory = "Tax",
        description = "Adds a user-entered tax or VAT rate to an amount.",
        notation = "tax = amount × rate ÷ 100;   total = amount + tax",
        assumptions = listOf("The rate is supplied by the user; NEXCALC contains no current tax tables."),
        inputs = listOf(
            input("amount", "Amount before tax", "", nonNegative = true, default = "5000"),
            input("rate", "Tax rate", "%", nonNegative = true, maximum = 100.0, default = "15")
        ),
        outputLabels = listOf("Tax" to "", "Total" to ""),
        compute = { values ->
            val amount = values.getValue("amount")
            val rate = values.getValue("rate")
            val taxAmount = amount * rate / 100.0
            result(money("Tax", taxAmount), money("Total", amount + taxAmount, primary = true))
        },
        tests = listOf(testCase(mapOf("amount" to 5000.0, "rate" to 15.0), "Tax" to 750.0, "Total" to 5750.0)),
        reference = REFERENCE
    )

    val taxReverse = Formula(
        id = "tax-reverse",
        name = "Amount before tax",
        category = FormulaCategory.BUSINESS,
        subcategory = "Tax",
        description = "Extracts the pre-tax amount and tax from a gross total.",
        notation = "net = gross ÷ (1 + rate ÷ 100);   tax = gross − net",
        assumptions = listOf("The gross total already includes a single tax rate."),
        inputs = listOf(
            input("gross", "Gross total", "", positive = true, default = "5750"),
            input("rate", "Tax rate", "%", nonNegative = true, maximum = 100.0, default = "15")
        ),
        outputLabels = listOf("Net amount" to "", "Tax" to ""),
        compute = { values ->
            val gross = values.getValue("gross")
            val rate = values.getValue("rate")
            val net = gross / (1 + rate / 100.0)
            result(money("Net amount", net, primary = true), money("Tax", gross - net))
        },
        tests = listOf(testCase(mapOf("gross" to 5750.0, "rate" to 15.0), "Net amount" to 5000.0, "Tax" to 750.0)),
        reference = REFERENCE
    )

    val commission = Formula(
        id = "commission",
        name = "Commission",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Computes a commission on sales and the amount payable after commission.",
        notation = "commission = sales × rate ÷ 100",
        assumptions = listOf("The commission rate is supplied by the user."),
        inputs = listOf(
            input("sales", "Sales amount", "", nonNegative = true, default = "50000"),
            input("rate", "Commission rate", "%", nonNegative = true, maximum = 100.0, default = "2.5")
        ),
        outputLabels = listOf("Commission" to "", "Net payable" to ""),
        compute = { values ->
            val sales = values.getValue("sales")
            val rate = values.getValue("rate")
            val commissionAmount = sales * rate / 100.0
            result(money("Commission", commissionAmount, primary = true), money("Net payable", sales - commissionAmount))
        },
        tests = listOf(testCase(mapOf("sales" to 50000.0, "rate" to 2.5), "Commission" to 1250.0, "Net payable" to 48750.0)),
        reference = REFERENCE
    )

    val simpleInterest = Formula(
        id = "simple-interest",
        name = "Simple interest",
        category = FormulaCategory.BUSINESS,
        subcategory = "Interest",
        description = "Computes simple interest and the maturity amount.",
        notation = "interest = principal × rate × years ÷ 100",
        assumptions = listOf("The interest rate is an annual percentage applied to the original principal."),
        inputs = listOf(
            input("principal", "Principal", "", nonNegative = true, default = "10000"),
            input("rate", "Annual rate", "%", nonNegative = true, default = "5"),
            input("years", "Time", "years", nonNegative = true, default = "2")
        ),
        outputLabels = listOf("Interest" to "", "Maturity amount" to ""),
        compute = { values ->
            val principal = values.getValue("principal")
            val rate = values.getValue("rate")
            val years = values.getValue("years")
            val interest = principal * rate * years / 100.0
            result(
                money("Interest", interest, primary = true),
                money("Maturity amount", principal + interest),
                steps = listOf("${format(principal)} × ${format(rate)} × ${format(years)} ÷ 100 = ${format(interest)}")
            )
        },
        tests = listOf(testCase(mapOf("principal" to 10000.0, "rate" to 5.0, "years" to 2.0), "Interest" to 1000.0, "Maturity amount" to 11000.0)),
        reference = REFERENCE
    )

    val compoundInterest = Formula(
        id = "compound-interest",
        name = "Compound interest",
        category = FormulaCategory.BUSINESS,
        subcategory = "Interest",
        description = "Computes compound interest with a configurable number of compounding periods per year.",
        notation = "amount = principal × (1 + rate ÷ (100·n))^(n·t)",
        assumptions = listOf("The rate is an annual nominal percentage and n is the number of compounding periods per year."),
        inputs = listOf(
            input("principal", "Principal", "", nonNegative = true, default = "10000"),
            input("rate", "Annual rate", "%", nonNegative = true, default = "10"),
            input("years", "Time", "years", nonNegative = true, default = "2"),
            input("periods", "Compounds per year", "", positive = true, integer = true, maximum = 365.0, default = "1")
        ),
        outputLabels = listOf("Final amount" to "", "Interest earned" to ""),
        compute = { values ->
            val principal = values.getValue("principal")
            val rate = values.getValue("rate")
            val years = values.getValue("years")
            val periods = values.getValue("periods")
            val amount = principal * Math.pow(1 + rate / (100.0 * periods), periods * years)
            result(
                money("Final amount", amount, primary = true),
                money("Interest earned", amount - principal),
                steps = listOf("${format(principal)} × (1 + ${format(rate)} ÷ (100 × ${format(periods)}))^(${format(periods)} × ${format(years)})")
            )
        },
        tests = listOf(
            testCase(mapOf("principal" to 10000.0, "rate" to 10.0, "years" to 2.0, "periods" to 1.0), "Final amount" to 12100.0, "Interest earned" to 2100.0),
            testCase(mapOf("principal" to 1000.0, "rate" to 8.0, "years" to 1.0, "periods" to 4.0), "Final amount" to 1082.4321609999999)
        ),
        reference = REFERENCE
    )

    val compoundPrincipal = Formula(
        id = "compound-principal",
        name = "Principal from a compound amount",
        category = FormulaCategory.BUSINESS,
        subcategory = "Interest",
        description = "Finds the principal that grows to a known amount under compound interest.",
        notation = "principal = amount ÷ (1 + rate ÷ (100·n))^(n·t)",
        assumptions = listOf("The amount is the result of compound growth at the stated nominal rate."),
        inputs = listOf(
            input("amount", "Final amount", "", positive = true, default = "12100"),
            input("rate", "Annual rate", "%", nonNegative = true, default = "10"),
            input("years", "Time", "years", nonNegative = true, default = "2"),
            input("periods", "Compounds per year", "", positive = true, integer = true, maximum = 365.0, default = "1")
        ),
        outputLabels = listOf("Principal" to "", "Interest included" to ""),
        compute = { values ->
            val amount = values.getValue("amount")
            val rate = values.getValue("rate")
            val years = values.getValue("years")
            val periods = values.getValue("periods")
            val principal = amount / Math.pow(1 + rate / (100.0 * periods), periods * years)
            result(money("Principal", principal, primary = true), money("Interest included", amount - principal))
        },
        tests = listOf(testCase(mapOf("amount" to 12100.0, "rate" to 10.0, "years" to 2.0, "periods" to 1.0), "Principal" to 10000.0)),
        reference = REFERENCE
    )

    val growth = Formula(
        id = "growth",
        name = "Compound growth",
        category = FormulaCategory.BUSINESS,
        subcategory = "Growth & decay",
        description = "Applies a constant percentage growth rate over a number of periods.",
        notation = "final = initial × (1 + rate ÷ 100)^periods",
        assumptions = listOf("Growth is compounded once per period."),
        inputs = listOf(
            input("initial", "Initial value", "", default = "1000"),
            input("rate", "Growth per period", "%", default = "5"),
            input("periods", "Periods", "", positive = true, default = "3")
        ),
        outputLabels = listOf("Final value" to "", "Total growth" to ""),
        compute = { values ->
            val initial = values.getValue("initial")
            val rate = values.getValue("rate")
            val periods = values.getValue("periods")
            val finalValue = initial * Math.pow(1 + rate / 100.0, periods)
            result(money("Final value", finalValue, primary = true), value("Total growth", (finalValue - initial) / initial * 100.0, "%"))
        },
        tests = listOf(testCase(mapOf("initial" to 1000.0, "rate" to 5.0, "periods" to 3.0), "Final value" to 1157.625, "Total growth" to 15.7625)),
        reference = REFERENCE
    )

    val cagr = Formula(
        id = "cagr",
        name = "Compound annual growth rate",
        category = FormulaCategory.BUSINESS,
        subcategory = "Growth & decay",
        description = "Computes the constant annual rate that turns a start value into an end value.",
        notation = "CAGR% = ((end ÷ start)^(1 ÷ years) − 1) × 100",
        assumptions = listOf("The start value is positive and the period is measured in years."),
        inputs = listOf(
            input("start", "Start value", "", positive = true, default = "1000"),
            input("end", "End value", "", positive = true, default = "1331"),
            input("years", "Years", "", positive = true, default = "3")
        ),
        outputLabels = listOf("CAGR" to "%"),
        compute = { values ->
            val start = values.getValue("start")
            val end = values.getValue("end")
            val years = values.getValue("years")
            val rate = (Math.pow(end / start, 1.0 / years) - 1.0) * 100.0
            result(value("CAGR", rate, "%", primary = true), steps = listOf("((${format(end)} ÷ ${format(start)})^(1 ÷ ${format(years)}) − 1) × 100"))
        },
        tests = listOf(testCase(mapOf("start" to 1000.0, "end" to 1331.0, "years" to 3.0), "CAGR" to 10.0)),
        reference = REFERENCE
    )

    val depreciation = Formula(
        id = "depreciation",
        name = "Depreciation (declining balance)",
        category = FormulaCategory.BUSINESS,
        subcategory = "Growth & decay",
        description = "Computes the book value of an asset after declining-balance depreciation.",
        notation = "book = initial × (1 − rate ÷ 100)^years",
        assumptions = listOf("A constant percentage of the remaining book value is written off each year."),
        inputs = listOf(
            input("initial", "Initial value", "", nonNegative = true, default = "100000"),
            input("rate", "Annual depreciation", "%", minimum = 0.0, maximum = 100.0, default = "10"),
            input("years", "Years", "years", positive = true, default = "2")
        ),
        outputLabels = listOf("Book value" to "", "Total depreciation" to ""),
        compute = { values ->
            val initial = values.getValue("initial")
            val rate = values.getValue("rate")
            val years = values.getValue("years")
            val book = initial * Math.pow(1 - rate / 100.0, years)
            result(money("Book value", book, primary = true), money("Total depreciation", initial - book))
        },
        tests = listOf(testCase(mapOf("initial" to 100000.0, "rate" to 10.0, "years" to 2.0), "Book value" to 81000.0, "Total depreciation" to 19000.0)),
        reference = REFERENCE
    )

    val installment = Formula(
        id = "installment",
        name = "Loan installment estimate",
        category = FormulaCategory.BUSINESS,
        subcategory = "Installment",
        description = "Estimates an equal monthly installment for a fixed-rate amortising loan.",
        notation = "payment = P·r ÷ (1 − (1 + r)^−n), r = annual rate ÷ 1200",
        assumptions = listOf(
            "Interest is charged on the reducing balance at a constant nominal annual rate.",
            "No fees, taxes, insurance or bank-specific charges are included."
        ),
        inputs = listOf(
            input("principal", "Loan principal", "", positive = true, default = "100000"),
            input("rate", "Annual interest rate", "%", nonNegative = true, default = "12"),
            input("months", "Months", "", positive = true, integer = true, maximum = 600.0, default = "12")
        ),
        outputLabels = listOf("Monthly installment" to "", "Total repayment" to "", "Total interest" to ""),
        compute = { values ->
            val principal = values.getValue("principal")
            val annualRate = values.getValue("rate")
            val months = values.getValue("months")
            val monthlyRate = annualRate / 1200.0
            val payment = if (monthlyRate == 0.0) {
                principal / months
            } else {
                principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months))
            }
            val total = payment * months
            result(
                money("Monthly installment", payment, primary = true),
                money("Total repayment", total),
                money("Total interest", total - principal),
                warnings = listOf("Estimate only. Actual installments depend on the lender's schedule and charges.")
            )
        },
        tests = listOf(
            testCase(
                mapOf("principal" to 100000.0, "rate" to 12.0, "months" to 12.0),
                "Monthly installment" to 8884.87886783416,
                "Total repayment" to 106618.54641400994,
                "Total interest" to 6618.546414009936
            ),
            testCase(mapOf("principal" to 120000.0, "rate" to 0.0, "months" to 12.0), "Monthly installment" to 10000.0)
        ),
        reference = "Standard amortising loan payment formula."
    )

    val breakEvenQuantity = Formula(
        id = "break-even-quantity",
        name = "Break-even quantity",
        category = FormulaCategory.BUSINESS,
        subcategory = "Break-even",
        description = "Number of units that must be sold to cover fixed costs.",
        notation = "quantity = fixed cost ÷ (price − variable cost)",
        assumptions = listOf("The selling price is higher than the variable cost per unit."),
        inputs = listOf(
            input("fixed", "Fixed cost", "", nonNegative = true, default = "10000"),
            input("price", "Price per unit", "", positive = true, default = "50"),
            input("variable", "Variable cost per unit", "", nonNegative = true, default = "30")
        ),
        outputLabels = listOf("Break-even quantity" to "units", "Contribution per unit" to ""),
        compute = { values ->
            val fixed = values.getValue("fixed")
            val price = values.getValue("price")
            val variable = values.getValue("variable")
            val contribution = price - variable
            if (contribution <= 0) {
                throw MathException.domain("The price must exceed the variable cost per unit to break even.")
            }
            result(
                value("Break-even quantity", fixed / contribution, "units", primary = true),
                money("Contribution per unit", contribution),
                steps = listOf("${format(fixed)} ÷ (${format(price)} − ${format(variable)}) = ${format(fixed / contribution)} units")
            )
        },
        tests = listOf(testCase(mapOf("fixed" to 10000.0, "price" to 50.0, "variable" to 30.0), "Break-even quantity" to 500.0, "Contribution per unit" to 20.0)),
        reference = REFERENCE
    )

    val breakEvenRevenue = Formula(
        id = "break-even-revenue",
        name = "Break-even revenue",
        category = FormulaCategory.BUSINESS,
        subcategory = "Break-even",
        description = "Sales revenue at the break-even point.",
        notation = "revenue = break-even quantity × price",
        assumptions = listOf("Uses the same quantity and price as the break-even calculation."),
        inputs = listOf(
            input("fixed", "Fixed cost", "", nonNegative = true, default = "10000"),
            input("price", "Price per unit", "", positive = true, default = "50"),
            input("variable", "Variable cost per unit", "", nonNegative = true, default = "30")
        ),
        outputLabels = listOf("Break-even revenue" to "", "Break-even quantity" to "units"),
        compute = { values ->
            val fixed = values.getValue("fixed")
            val price = values.getValue("price")
            val variable = values.getValue("variable")
            val contribution = price - variable
            if (contribution <= 0) throw MathException.domain("The price must exceed the variable cost per unit.")
            val quantity = fixed / contribution
            result(money("Break-even revenue", quantity * price, primary = true), value("Break-even quantity", quantity, "units"))
        },
        tests = listOf(testCase(mapOf("fixed" to 10000.0, "price" to 50.0, "variable" to 30.0), "Break-even revenue" to 25000.0, "Break-even quantity" to 500.0)),
        reference = REFERENCE
    )

    val unitPrice = Formula(
        id = "unit-price",
        name = "Unit price",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Price per unit of quantity, useful for comparing package deals.",
        notation = "unit price = total price ÷ quantity",
        assumptions = listOf("Quantity is expressed in the same unit for every compared product."),
        inputs = listOf(
            input("price", "Total price", "", nonNegative = true, default = "240"),
            input("quantity", "Quantity", "", positive = true, default = "12")
        ),
        outputLabels = listOf("Price per unit" to ""),
        compute = { values ->
            result(money("Price per unit", values.getValue("price") / values.getValue("quantity"), primary = true))
        },
        tests = listOf(testCase(mapOf("price" to 240.0, "quantity" to 12.0), "Price per unit" to 20.0)),
        reference = REFERENCE
    )

    val weightedAveragePrice = Formula(
        id = "weighted-average-price",
        name = "Weighted average price",
        category = FormulaCategory.BUSINESS,
        subcategory = "Pricing",
        description = "Average price across two purchases with different quantities.",
        notation = "average = (q₁p₁ + q₂p₂) ÷ (q₁ + q₂)",
        assumptions = listOf("Quantities are measured in the same unit."),
        inputs = listOf(
            input("quantity1", "First quantity", "", positive = true, default = "2"),
            input("price1", "First price", "", nonNegative = true, default = "100"),
            input("quantity2", "Second quantity", "", positive = true, default = "3"),
            input("price2", "Second price", "", nonNegative = true, default = "200")
        ),
        outputLabels = listOf("Weighted average price" to "", "Total cost" to ""),
        compute = { values ->
            val q1 = values.getValue("quantity1")
            val p1 = values.getValue("price1")
            val q2 = values.getValue("quantity2")
            val p2 = values.getValue("price2")
            val average = (q1 * p1 + q2 * p2) / (q1 + q2)
            result(
                money("Weighted average price", average, primary = true),
                money("Total cost", q1 * p1 + q2 * p2),
                steps = listOf("(${format(q1)}×${format(p1)} + ${format(q2)}×${format(p2)}) ÷ ${format(q1 + q2)} = ${format(average)}")
            )
        },
        tests = listOf(testCase(mapOf("quantity1" to 2.0, "price1" to 100.0, "quantity2" to 3.0, "price2" to 200.0), "Weighted average price" to 160.0, "Total cost" to 800.0)),
        reference = REFERENCE
    )

    val all: List<Formula> = listOf(
        profit, loss, sellingFromProfit, markup, margin, discount, successiveDiscount, tax, taxReverse,
        commission, simpleInterest, compoundInterest, compoundPrincipal, growth, cagr, depreciation,
        installment, breakEvenQuantity, breakEvenRevenue, unitPrice, weightedAveragePrice
    )
}
