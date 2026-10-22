package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/**
 * Physics and engineering formulas.
 *
 * Each formula states its physical assumptions. Values use SI units unless the input label says
 * otherwise, and g = 9.80665 m/s² is the exact standard gravity.
 */
internal object PhysicsFormulas {

    private const val G = 9.80665

    private fun v(label: String, amount: Double, unit: String = "", primary: Boolean = false) =
        value(label, amount, unit, primary)

    val motionVelocity = Formula(
        id = "motion-velocity", name = "Motion — final velocity", category = FormulaCategory.PHYSICS, subcategory = "Kinematics",
        description = "Final velocity under constant acceleration.", notation = "v = u + a·t",
        assumptions = listOf("Acceleration is constant along a straight line."),
        inputs = listOf(
            input("initial", "Initial velocity", "m/s", default = "5"),
            input("acceleration", "Acceleration", "m/s²", default = "2"),
            input("time", "Time", "s", nonNegative = true, default = "3")
        ),
        outputLabels = listOf("Final velocity" to "m/s", "Displacement" to "m"),
        compute = { x ->
            val u = x.getValue("initial"); val a = x.getValue("acceleration"); val t = x.getValue("time")
            result(v("Final velocity", u + a * t, "m/s", primary = true), v("Displacement", u * t + 0.5 * a * t * t, "m"))
        },
        tests = listOf(testCase(mapOf("initial" to 5.0, "acceleration" to 2.0, "time" to 3.0), "Final velocity" to 11.0, "Displacement" to 24.0))
    )

    val motionDisplacement = Formula(
        id = "motion-displacement", name = "Motion — displacement", category = FormulaCategory.PHYSICS, subcategory = "Kinematics",
        description = "Displacement under constant acceleration.", notation = "s = u·t + ½·a·t²",
        assumptions = listOf("Acceleration is constant."),
        inputs = listOf(
            input("initial", "Initial velocity", "m/s", default = "5"),
            input("acceleration", "Acceleration", "m/s²", default = "2"),
            input("time", "Time", "s", nonNegative = true, default = "3")
        ),
        outputLabels = listOf("Displacement" to "m", "Final velocity" to "m/s"),
        compute = { x ->
            val u = x.getValue("initial"); val a = x.getValue("acceleration"); val t = x.getValue("time")
            result(
                v("Displacement", u * t + 0.5 * a * t * t, "m", primary = true),
                v("Final velocity", u + a * t, "m/s")
            )
        },
        tests = listOf(testCase(mapOf("initial" to 5.0, "acceleration" to 2.0, "time" to 3.0), "Displacement" to 24.0))
    )

    val motionVelocitySquared = Formula(
        id = "motion-velocity-squared", name = "Motion — velocity from displacement", category = FormulaCategory.PHYSICS, subcategory = "Kinematics",
        description = "Speed after a displacement under constant acceleration, without knowing the time.",
        notation = "v = √(u² + 2·a·s)",
        assumptions = listOf("Acceleration is constant and the expression under the root is non-negative."),
        inputs = listOf(
            input("initial", "Initial velocity", "m/s", nonNegative = true, default = "0"),
            input("acceleration", "Acceleration", "m/s²", default = "2"),
            input("displacement", "Displacement", "m", nonNegative = true, default = "9")
        ),
        outputLabels = listOf("Final velocity" to "m/s"),
        compute = { x ->
            val radicand = x.getValue("initial") * x.getValue("initial") + 2 * x.getValue("acceleration") * x.getValue("displacement")
            if (radicand < 0) throw MathException.domain("This combination of inputs gives no real final velocity.")
            result(v("Final velocity", Math.sqrt(radicand), "m/s", primary = true))
        },
        tests = listOf(testCase(mapOf("initial" to 0.0, "acceleration" to 2.0, "displacement" to 9.0), "Final velocity" to 6.0))
    )

    val force = Formula(
        id = "force", name = "Force (Newton's second law)", category = FormulaCategory.PHYSICS, subcategory = "Dynamics",
        description = "Net force required to accelerate a mass.", notation = "F = m·a",
        assumptions = listOf("Mass is constant and the force is the net force."),
        inputs = listOf(input("mass", "Mass", "kg", positive = true, default = "10"), input("acceleration", "Acceleration", "m/s²", default = "3")),
        outputLabels = listOf("Force" to "N"),
        compute = { x -> result(v("Force", x.getValue("mass") * x.getValue("acceleration"), "N", primary = true)) },
        tests = listOf(testCase(mapOf("mass" to 10.0, "acceleration" to 3.0), "Force" to 30.0))
    )

    val weight = Formula(
        id = "weight", name = "Weight", category = FormulaCategory.PHYSICS, subcategory = "Dynamics",
        description = "Weight of a mass in standard gravity.", notation = "W = m·g, g = 9.80665 m/s²",
        assumptions = listOf("Measured at standard gravity; local g varies slightly with location and altitude."),
        inputs = listOf(input("mass", "Mass", "kg", nonNegative = true, default = "70")),
        outputLabels = listOf("Weight" to "N", "Mass" to "kg"),
        compute = { x ->
            val mass = x.getValue("mass")
            result(v("Weight", mass * G, "N", primary = true), v("Mass", mass, "kg"))
        },
        tests = listOf(testCase(mapOf("mass" to 70.0), "Weight" to 686.4655))
    )

    val work = Formula(
        id = "work", name = "Work done by a force", category = FormulaCategory.PHYSICS, subcategory = "Energy",
        description = "Work done when a force moves an object along its direction.", notation = "W = F·d",
        assumptions = listOf("The force acts along the displacement."),
        inputs = listOf(input("force", "Force", "N", default = "20"), input("distance", "Displacement", "m", nonNegative = true, default = "5")),
        outputLabels = listOf("Work" to "J"),
        compute = { x -> result(v("Work", x.getValue("force") * x.getValue("distance"), "J", primary = true)) },
        tests = listOf(testCase(mapOf("force" to 20.0, "distance" to 5.0), "Work" to 100.0))
    )

    val kineticEnergy = Formula(
        id = "kinetic-energy", name = "Kinetic energy", category = FormulaCategory.PHYSICS, subcategory = "Energy",
        description = "Kinetic energy of a moving mass.", notation = "Eₖ = ½·m·v²",
        assumptions = listOf("Classical (non-relativistic) mechanics."),
        inputs = listOf(input("mass", "Mass", "kg", nonNegative = true, default = "2"), input("velocity", "Velocity", "m/s", default = "10")),
        outputLabels = listOf("Kinetic energy" to "J"),
        compute = { x -> result(v("Kinetic energy", 0.5 * x.getValue("mass") * x.getValue("velocity") * x.getValue("velocity"), "J", primary = true)) },
        tests = listOf(testCase(mapOf("mass" to 2.0, "velocity" to 10.0), "Kinetic energy" to 100.0))
    )

    val potentialEnergy = Formula(
        id = "potential-energy", name = "Gravitational potential energy", category = FormulaCategory.PHYSICS, subcategory = "Energy",
        description = "Change in gravitational potential energy near Earth's surface.", notation = "Eₚ = m·g·h",
        assumptions = listOf("Height change is small compared with Earth's radius, so g is effectively constant."),
        inputs = listOf(input("mass", "Mass", "kg", nonNegative = true, default = "2"), input("height", "Height", "m", default = "10")),
        outputLabels = listOf("Potential energy" to "J"),
        compute = { x -> result(v("Potential energy", x.getValue("mass") * G * x.getValue("height"), "J", primary = true)) },
        tests = listOf(testCase(mapOf("mass" to 2.0, "height" to 10.0), "Potential energy" to 196.133))
    )

    val power = Formula(
        id = "power", name = "Power", category = FormulaCategory.PHYSICS, subcategory = "Energy",
        description = "Rate of doing work.", notation = "P = W ÷ t",
        assumptions = listOf("Work is done at a constant average rate."),
        inputs = listOf(input("work", "Work", "J", nonNegative = true, default = "1000"), input("time", "Time", "s", positive = true, default = "50")),
        outputLabels = listOf("Power" to "W"),
        compute = { x -> result(v("Power", x.getValue("work") / x.getValue("time"), "W", primary = true)) },
        tests = listOf(testCase(mapOf("work" to 1000.0, "time" to 50.0), "Power" to 20.0))
    )

    val momentum = Formula(
        id = "momentum", name = "Linear momentum", category = FormulaCategory.PHYSICS, subcategory = "Dynamics",
        description = "Momentum of a moving mass.", notation = "p = m·v",
        assumptions = listOf("Classical mechanics."),
        inputs = listOf(input("mass", "Mass", "kg", nonNegative = true, default = "2"), input("velocity", "Velocity", "m/s", default = "10")),
        outputLabels = listOf("Momentum" to "kg·m/s"),
        compute = { x -> result(v("Momentum", x.getValue("mass") * x.getValue("velocity"), "kg·m/s", primary = true)) },
        tests = listOf(testCase(mapOf("mass" to 2.0, "velocity" to 10.0), "Momentum" to 20.0))
    )

    val torque = Formula(
        id = "torque", name = "Torque", category = FormulaCategory.PHYSICS, subcategory = "Dynamics",
        description = "Torque for a force applied perpendicular to a lever arm.", notation = "τ = F·r",
        assumptions = listOf("The force is perpendicular to the lever arm. An angled force requires τ = F·r·sin θ, which is not modelled here."),
        inputs = listOf(input("force", "Force", "N", nonNegative = true, default = "20"), input("radius", "Lever arm", "m", nonNegative = true, default = "0.5")),
        outputLabels = listOf("Torque" to "N·m"),
        compute = { x -> result(v("Torque", x.getValue("force") * x.getValue("radius"), "N·m", primary = true)) },
        tests = listOf(testCase(mapOf("force" to 20.0, "radius" to 0.5), "Torque" to 10.0))
    )

    val density = Formula(
        id = "density", name = "Density", category = FormulaCategory.PHYSICS, subcategory = "Properties",
        description = "Mass per unit volume.", notation = "ρ = m ÷ V",
        assumptions = listOf("The material is uniform over the measured volume."),
        inputs = listOf(input("mass", "Mass", "kg", nonNegative = true, default = "10"), input("volume", "Volume", "m³", positive = true, default = "2")),
        outputLabels = listOf("Density" to "kg/m³"),
        compute = { x -> result(v("Density", x.getValue("mass") / x.getValue("volume"), "kg/m³", primary = true)) },
        tests = listOf(testCase(mapOf("mass" to 10.0, "volume" to 2.0), "Density" to 5.0))
    )

    val pressure = Formula(
        id = "pressure", name = "Pressure", category = FormulaCategory.PHYSICS, subcategory = "Properties",
        description = "Force per unit area.", notation = "P = F ÷ A",
        assumptions = listOf("The force acts perpendicular to the area."),
        inputs = listOf(input("force", "Force", "N", nonNegative = true, default = "100"), input("area", "Area", "m²", positive = true, default = "2")),
        outputLabels = listOf("Pressure" to "Pa"),
        compute = { x -> result(v("Pressure", x.getValue("force") / x.getValue("area"), "Pa", primary = true)) },
        tests = listOf(testCase(mapOf("force" to 100.0, "area" to 2.0), "Pressure" to 50.0))
    )

    val hydrostaticPressure = Formula(
        id = "hydrostatic-pressure", name = "Hydrostatic pressure", category = FormulaCategory.PHYSICS, subcategory = "Fluids",
        description = "Gauge pressure at a depth in a static fluid.", notation = "P = ρ·g·h",
        assumptions = listOf("The fluid is static and incompressible with uniform density."),
        inputs = listOf(
            input("density", "Fluid density", "kg/m³", positive = true, default = "1000"),
            input("depth", "Depth", "m", nonNegative = true, default = "10")
        ),
        outputLabels = listOf("Gauge pressure (Pa)" to "Pa", "Gauge pressure (kPa)" to "kPa"),
        compute = { x ->
            val pressureValue = x.getValue("density") * G * x.getValue("depth")
            result(v("Gauge pressure (Pa)", pressureValue, "Pa", primary = true), v("Gauge pressure (kPa)", pressureValue / 1000.0, "kPa"))
        },
        tests = listOf(testCase(mapOf("density" to 1000.0, "depth" to 10.0), "Gauge pressure (Pa)" to 98066.5, "Gauge pressure (kPa)" to 98.0665))
    )

    val flowRate = Formula(
        id = "flow-rate", name = "Volume flow rate", category = FormulaCategory.PHYSICS, subcategory = "Fluids",
        description = "Volume flow through a cross-section.", notation = "Q = A·v",
        assumptions = listOf("The velocity is uniform across the cross-section."),
        inputs = listOf(
            input("area", "Cross-sectional area", "m²", positive = true, default = "0.5"),
            input("velocity", "Flow velocity", "m/s", nonNegative = true, default = "2")
        ),
        outputLabels = listOf("Flow rate" to "m³/s"),
        compute = { x -> result(v("Flow rate", x.getValue("area") * x.getValue("velocity"), "m³/s", primary = true)) },
        tests = listOf(testCase(mapOf("area" to 0.5, "velocity" to 2.0), "Flow rate" to 1.0))
    )

    val ohmVoltage = Formula(
        id = "ohm-voltage", name = "Ohm's law — voltage", category = FormulaCategory.PHYSICS, subcategory = "Electricity",
        description = "Voltage across a resistor.", notation = "V = I·R",
        assumptions = listOf("The component is ohmic (constant resistance)."),
        inputs = listOf(input("current", "Current", "A", nonNegative = true, default = "2"), input("resistance", "Resistance", "Ω", nonNegative = true, default = "5")),
        outputLabels = listOf("Voltage" to "V"),
        compute = { x -> result(v("Voltage", x.getValue("current") * x.getValue("resistance"), "V", primary = true)) },
        tests = listOf(testCase(mapOf("current" to 2.0, "resistance" to 5.0), "Voltage" to 10.0))
    )

    val ohmCurrent = Formula(
        id = "ohm-current", name = "Ohm's law — current", category = FormulaCategory.PHYSICS, subcategory = "Electricity",
        description = "Current through a resistor.", notation = "I = V ÷ R",
        assumptions = listOf("The component is ohmic and the resistance is positive."),
        inputs = listOf(input("voltage", "Voltage", "V", nonNegative = true, default = "10"), input("resistance", "Resistance", "Ω", positive = true, default = "5")),
        outputLabels = listOf("Current" to "A"),
        compute = { x -> result(v("Current", x.getValue("voltage") / x.getValue("resistance"), "A", primary = true)) },
        tests = listOf(testCase(mapOf("voltage" to 10.0, "resistance" to 5.0), "Current" to 2.0))
    )

    val ohmResistance = Formula(
        id = "ohm-resistance", name = "Ohm's law — resistance", category = FormulaCategory.PHYSICS, subcategory = "Electricity",
        description = "Resistance from voltage and current.", notation = "R = V ÷ I",
        assumptions = listOf("The current is non-zero."),
        inputs = listOf(input("voltage", "Voltage", "V", nonNegative = true, default = "10"), input("current", "Current", "A", positive = true, default = "2")),
        outputLabels = listOf("Resistance" to "Ω"),
        compute = { x -> result(v("Resistance", x.getValue("voltage") / x.getValue("current"), "Ω", primary = true)) },
        tests = listOf(testCase(mapOf("voltage" to 10.0, "current" to 2.0), "Resistance" to 5.0))
    )

    val electricalPower = Formula(
        id = "electrical-power", name = "Electrical power", category = FormulaCategory.PHYSICS, subcategory = "Electricity",
        description = "Power dissipated by a DC component.", notation = "P = V·I",
        assumptions = listOf("Direct current or the RMS values of an alternating signal."),
        inputs = listOf(input("voltage", "Voltage", "V", nonNegative = true, default = "10"), input("current", "Current", "A", nonNegative = true, default = "2")),
        outputLabels = listOf("Power" to "W", "Resistance" to "Ω"),
        compute = { x ->
            val voltage = x.getValue("voltage")
            val current = x.getValue("current")
            result(
                v("Power", voltage * current, "W", primary = true),
                v("Resistance", if (current == 0.0) 0.0 else voltage / current, "Ω"),
                warnings = listOf("Resistance is undefined at zero current and is shown as 0 in that case.")
            )
        },
        tests = listOf(testCase(mapOf("voltage" to 10.0, "current" to 2.0), "Power" to 20.0, "Resistance" to 5.0))
    )

    val seriesResistance = Formula(
        id = "series-resistance", name = "Resistors in series", category = FormulaCategory.PHYSICS, subcategory = "Electricity",
        description = "Total resistance of three resistors in series.", notation = "R = R₁ + R₂ + R₃",
        assumptions = listOf("The resistors are connected end to end."),
        inputs = listOf(
            input("r1", "R₁", "Ω", nonNegative = true, default = "2"),
            input("r2", "R₂", "Ω", nonNegative = true, default = "3"),
            input("r3", "R₃", "Ω", nonNegative = true, default = "5")
        ),
        outputLabels = listOf("Total resistance" to "Ω"),
        compute = { x -> result(v("Total resistance", x.getValue("r1") + x.getValue("r2") + x.getValue("r3"), "Ω", primary = true)) },
        tests = listOf(testCase(mapOf("r1" to 2.0, "r2" to 3.0, "r3" to 5.0), "Total resistance" to 10.0))
    )

    val parallelResistance = Formula(
        id = "parallel-resistance", name = "Resistors in parallel", category = FormulaCategory.PHYSICS, subcategory = "Electricity",
        description = "Equivalent resistance of two resistors in parallel.", notation = "R = 1 ÷ (1/R₁ + 1/R₂)",
        assumptions = listOf("Both resistances are positive."),
        inputs = listOf(input("r1", "R₁", "Ω", positive = true, default = "6"), input("r2", "R₂", "Ω", positive = true, default = "3")),
        outputLabels = listOf("Equivalent resistance" to "Ω"),
        compute = { x -> result(v("Equivalent resistance", 1.0 / (1.0 / x.getValue("r1") + 1.0 / x.getValue("r2")), "Ω", primary = true)) },
        tests = listOf(testCase(mapOf("r1" to 6.0, "r2" to 3.0), "Equivalent resistance" to 2.0))
    )

    val frequencyPeriod = Formula(
        id = "frequency-period", name = "Frequency and period", category = FormulaCategory.PHYSICS, subcategory = "Waves",
        description = "Period of a periodic signal from its frequency.", notation = "T = 1 ÷ f",
        assumptions = listOf("The signal is periodic and the frequency is positive."),
        inputs = listOf(input("frequency", "Frequency", "Hz", positive = true, default = "50")),
        outputLabels = listOf("Period" to "s", "Angular frequency" to "rad/s"),
        compute = { x ->
            val f = x.getValue("frequency")
            result(v("Period", 1.0 / f, "s", primary = true), v("Angular frequency", 2 * Math.PI * f, "rad/s"))
        },
        tests = listOf(testCase(mapOf("frequency" to 50.0), "Period" to 0.02, "Angular frequency" to 314.1592653589793))
    )

    val wavelength = Formula(
        id = "wavelength", name = "Wavelength", category = FormulaCategory.PHYSICS, subcategory = "Waves",
        description = "Wavelength from wave speed and frequency.", notation = "λ = v ÷ f",
        assumptions = listOf("The wave travels at the given constant speed."),
        inputs = listOf(input("speed", "Wave speed", "m/s", positive = true, default = "343"), input("frequency", "Frequency", "Hz", positive = true, default = "100")),
        outputLabels = listOf("Wavelength" to "m", "Period" to "s"),
        compute = { x ->
            val speed = x.getValue("speed")
            val frequency = x.getValue("frequency")
            result(v("Wavelength", speed / frequency, "m", primary = true), v("Period", 1.0 / frequency, "s"))
        },
        tests = listOf(testCase(mapOf("speed" to 343.0, "frequency" to 100.0), "Wavelength" to 3.43))
    )

    val heat = Formula(
        id = "heat", name = "Heat energy", category = FormulaCategory.PHYSICS, subcategory = "Thermal",
        description = "Heat needed to change the temperature of a mass.", notation = "Q = m·c·ΔT",
        assumptions = listOf("No phase change occurs and specific heat is constant over the range."),
        inputs = listOf(
            input("mass", "Mass", "kg", nonNegative = true, default = "2"),
            input("specificHeat", "Specific heat capacity", "J/(kg·K)", nonNegative = true, default = "4186"),
            input("deltaT", "Temperature change", "K", default = "10")
        ),
        outputLabels = listOf("Heat energy (J)" to "J", "Heat energy (kJ)" to "kJ"),
        compute = { x ->
            val energy = x.getValue("mass") * x.getValue("specificHeat") * x.getValue("deltaT")
            result(v("Heat energy (J)", energy, "J", primary = true), v("Heat energy (kJ)", energy / 1000.0, "kJ"))
        },
        tests = listOf(testCase(mapOf("mass" to 2.0, "specificHeat" to 4186.0, "deltaT" to 10.0), "Heat energy (J)" to 83720.0, "Heat energy (kJ)" to 83.72))
    )

    val thermalExpansion = Formula(
        id = "thermal-expansion", name = "Linear thermal expansion", category = FormulaCategory.PHYSICS, subcategory = "Thermal",
        description = "Change in length of a material when heated.", notation = "ΔL = L·α·ΔT",
        assumptions = listOf("Small temperature change and an isotropic material."),
        inputs = listOf(
            input("length", "Original length", "m", nonNegative = true, default = "10"),
            input("coefficient", "Linear expansion coefficient", "1/K", nonNegative = true, default = "0.000012"),
            input("deltaT", "Temperature change", "K", default = "50")
        ),
        outputLabels = listOf("Change in length" to "m", "New length" to "m"),
        compute = { x ->
            val change = x.getValue("length") * x.getValue("coefficient") * x.getValue("deltaT")
            result(v("Change in length", change, "m", primary = true), v("New length", x.getValue("length") + change, "m"))
        },
        tests = listOf(testCase(mapOf("length" to 10.0, "coefficient" to 0.000012, "deltaT" to 50.0), "Change in length" to 0.006))
    )

    val efficiency = Formula(
        id = "efficiency", name = "Efficiency", category = FormulaCategory.PHYSICS, subcategory = "Energy",
        description = "Useful output as a percentage of total input.", notation = "η% = useful ÷ total × 100",
        assumptions = listOf("Both values are measured in the same unit over the same interval."),
        inputs = listOf(input("useful", "Useful output", "", nonNegative = true, default = "800"), input("total", "Total input", "", positive = true, default = "1000")),
        outputLabels = listOf("Efficiency" to "%", "Loss" to ""),
        compute = { x ->
            val useful = x.getValue("useful")
            val total = x.getValue("total")
            result(
                v("Efficiency", useful / total * 100.0, "%", primary = true),
                v("Loss", total - useful),
                warnings = if (useful > total) listOf("Useful output exceeds input, which violates energy conservation.") else emptyList()
            )
        },
        tests = listOf(testCase(mapOf("useful" to 800.0, "total" to 1000.0), "Efficiency" to 80.0, "Loss" to 200.0))
    )

    val all: List<Formula> = listOf(
        motionVelocity, motionDisplacement, motionVelocitySquared, force, weight, work, kineticEnergy,
        potentialEnergy, power, momentum, torque, density, pressure, hydrostaticPressure, flowRate,
        ohmVoltage, ohmCurrent, ohmResistance, electricalPower, seriesResistance, parallelResistance,
        frequencyPeriod, wavelength, heat, thermalExpansion, efficiency
    )
}
