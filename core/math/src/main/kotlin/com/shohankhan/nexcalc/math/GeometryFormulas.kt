package com.shohankhan.nexcalc.math

import com.shohankhan.nexcalc.math.FormulaSupport.format
import com.shohankhan.nexcalc.math.FormulaSupport.input
import com.shohankhan.nexcalc.math.FormulaSupport.result
import com.shohankhan.nexcalc.math.FormulaSupport.testCase
import com.shohankhan.nexcalc.math.FormulaSupport.value

/**
 * Geometry and mensuration.
 *
 * Reverse calculations are only offered where the supplied inputs determine a unique result;
 * otherwise the forward formula is published and the user is asked for the missing measurement.
 */
internal object GeometryFormulas {

    private fun area(label: String, amount: Double, primary: Boolean = false) = value(label, amount, "square units", primary)
    private fun length(label: String, amount: Double, primary: Boolean = false) = value(label, amount, "units", primary)
    private fun volume(label: String, amount: Double, primary: Boolean = false) = value(label, amount, "cubic units", primary)

    private fun r2(label: String, amount: Double, unit: String = "square units", primary: Boolean = false) = value(label, amount, unit, primary)
    private fun l2(label: String, amount: Double, unit: String = "units", primary: Boolean = false) = value(label, amount, unit, primary)

    private const val REFERENCE = "Standard Euclidean geometry."

    val squareArea = Formula(
        id = "square-area", name = "Square area", category = FormulaCategory.GEOMETRY, subcategory = "Quadrilaterals",
        description = "Area of a square from its side length.", notation = "A = a²",
        assumptions = listOf("All four sides are equal."),
        inputs = listOf(input("side", "Side", "units", positive = true, default = "5")),
        outputLabels = listOf("Area" to "square units", "Perimeter" to "units", "Diagonal" to "units"),
        compute = { v ->
            val side = v.getValue("side")
            result(area("Area", side * side, primary = true), l2("Perimeter", 4 * side), l2("Diagonal", side * Math.sqrt(2.0)))
        },
        tests = listOf(testCase(mapOf("side" to 5.0), "Area" to 25.0, "Perimeter" to 20.0, "Diagonal" to 7.0710678118654755)),
        reference = REFERENCE
    )

    val squarePerimeter = Formula(
        id = "square-perimeter", name = "Square perimeter", category = FormulaCategory.GEOMETRY, subcategory = "Quadrilaterals",
        description = "Perimeter of a square from its side length.", notation = "P = 4a",
        assumptions = listOf("All four sides are equal."),
        inputs = listOf(input("side", "Side", "units", positive = true, default = "5")),
        outputLabels = listOf("Perimeter" to "units"),
        compute = { v -> result(l2("Perimeter", 4 * v.getValue("side"), primary = true)) },
        tests = listOf(testCase(mapOf("side" to 5.0), "Perimeter" to 20.0)),
        reference = REFERENCE
    )

    val rectangleArea = Formula(
        id = "rectangle-area", name = "Rectangle area and perimeter", category = FormulaCategory.GEOMETRY, subcategory = "Quadrilaterals",
        description = "Area, perimeter and diagonal of a rectangle.", notation = "A = l × w;   P = 2(l + w);   d = √(l² + w²)",
        assumptions = listOf("Opposite sides are equal and all angles are right angles."),
        inputs = listOf(input("length", "Length", "units", positive = true, default = "6"), input("width", "Width", "units", positive = true, default = "4")),
        outputLabels = listOf("Area" to "square units", "Perimeter" to "units", "Diagonal" to "units"),
        compute = { v ->
            val length = v.getValue("length")
            val width = v.getValue("width")
            result(
                area("Area", length * width, primary = true),
                l2("Perimeter", 2 * (length + width)),
                l2("Diagonal", Math.hypot(length, width))
            )
        },
        tests = listOf(testCase(mapOf("length" to 6.0, "width" to 4.0), "Area" to 24.0, "Perimeter" to 20.0, "Diagonal" to 7.211102550927978)),
        reference = REFERENCE
    )

    val triangleArea = Formula(
        id = "triangle-area", name = "Triangle area (base and height)", category = FormulaCategory.GEOMETRY, subcategory = "Triangles",
        description = "Area of a triangle from a base and the perpendicular height.", notation = "A = ½ × b × h",
        assumptions = listOf("The height must be perpendicular to the chosen base."),
        inputs = listOf(input("base", "Base", "units", positive = true, default = "10"), input("height", "Height", "units", positive = true, default = "6")),
        outputLabels = listOf("Area" to "square units"),
        compute = { v -> result(area("Area", 0.5 * v.getValue("base") * v.getValue("height"), primary = true)) },
        tests = listOf(testCase(mapOf("base" to 10.0, "height" to 6.0), "Area" to 30.0)),
        reference = REFERENCE
    )

    val triangleHeron = Formula(
        id = "triangle-heron", name = "Triangle area (Heron)", category = FormulaCategory.GEOMETRY, subcategory = "Triangles",
        description = "Area of a triangle from its three sides using Heron's formula.", notation = "s = (a+b+c)÷2;   A = √(s(s−a)(s−b)(s−c))",
        assumptions = listOf("The three sides satisfy the triangle inequality."),
        inputs = listOf(
            input("a", "Side a", "units", positive = true, default = "3"),
            input("b", "Side b", "units", positive = true, default = "4"),
            input("c", "Side c", "units", positive = true, default = "5")
        ),
        outputLabels = listOf("Area" to "square units", "Perimeter" to "units", "Semi-perimeter" to "units"),
        compute = { v ->
            val a = v.getValue("a"); val b = v.getValue("b"); val c = v.getValue("c")
            if (a + b <= c || a + c <= b || b + c <= a) throw MathException.domain("These sides do not form a triangle.")
            val s = (a + b + c) / 2.0
            val areaValue = Math.sqrt(s * (s - a) * (s - b) * (s - c))
            result(area("Area", areaValue, primary = true), l2("Perimeter", a + b + c), l2("Semi-perimeter", s))
        },
        tests = listOf(testCase(mapOf("a" to 3.0, "b" to 4.0, "c" to 5.0), "Area" to 6.0, "Perimeter" to 12.0, "Semi-perimeter" to 6.0)),
        reference = REFERENCE
    )

    val circle = Formula(
        id = "circle", name = "Circle area and circumference", category = FormulaCategory.GEOMETRY, subcategory = "Circles",
        description = "Area, circumference and diameter of a circle.", notation = "A = πr²;   C = 2πr",
        assumptions = listOf("π is used at double precision."),
        inputs = listOf(input("radius", "Radius", "units", positive = true, default = "7")),
        outputLabels = listOf("Area" to "square units", "Circumference" to "units", "Diameter" to "units"),
        compute = { v ->
            val r = v.getValue("radius")
            result(area("Area", Math.PI * r * r, primary = true), l2("Circumference", 2 * Math.PI * r), l2("Diameter", 2 * r))
        },
        tests = listOf(testCase(mapOf("radius" to 7.0), "Area" to 153.93804002589985, "Circumference" to 43.982297150257104, "Diameter" to 14.0)),
        reference = REFERENCE
    )

    val circleRadiusFromArea = Formula(
        id = "circle-radius-from-area", name = "Radius from circle area", category = FormulaCategory.GEOMETRY, subcategory = "Circles",
        description = "Recovers the radius of a circle from its area (unique positive solution).", notation = "r = √(A ÷ π)",
        assumptions = listOf("The area is positive, so exactly one positive radius exists."),
        inputs = listOf(input("area", "Area", "square units", positive = true, default = "153.93804002589985")),
        outputLabels = listOf("Radius" to "units", "Diameter" to "units"),
        compute = { v ->
            val r = Math.sqrt(v.getValue("area") / Math.PI)
            result(l2("Radius", r, primary = true), l2("Diameter", 2 * r))
        },
        tests = listOf(testCase(mapOf("area" to 153.93804002589985), "Radius" to 7.0)),
        reference = REFERENCE
    )

    val sector = Formula(
        id = "sector", name = "Sector area and arc length", category = FormulaCategory.GEOMETRY, subcategory = "Circles",
        description = "Area of a circular sector and the length of its arc.", notation = "A = πr²·θ÷360;   arc = 2πr·θ÷360",
        assumptions = listOf("The angle is entered in degrees."),
        inputs = listOf(input("radius", "Radius", "units", positive = true, default = "7"), input("angle", "Central angle", "°", positive = true, maximum = 360.0, default = "90")),
        outputLabels = listOf("Sector area" to "square units", "Arc length" to "units"),
        compute = { v ->
            val r = v.getValue("radius")
            val angle = v.getValue("angle")
            val fraction = angle / 360.0
            result(area("Sector area", Math.PI * r * r * fraction, primary = true), l2("Arc length", 2 * Math.PI * r * fraction))
        },
        tests = listOf(testCase(mapOf("radius" to 7.0, "angle" to 90.0), "Sector area" to 38.48451000647496, "Arc length" to 10.995574287564276)),
        reference = REFERENCE
    )

    val parallelogram = Formula(
        id = "parallelogram", name = "Parallelogram area", category = FormulaCategory.GEOMETRY, subcategory = "Quadrilaterals",
        description = "Area of a parallelogram from a base and perpendicular height.", notation = "A = b × h",
        assumptions = listOf("The height is perpendicular to the base."),
        inputs = listOf(input("base", "Base", "units", positive = true, default = "8"), input("height", "Height", "units", positive = true, default = "5")),
        outputLabels = listOf("Area" to "square units"),
        compute = { v -> result(area("Area", v.getValue("base") * v.getValue("height"), primary = true)) },
        tests = listOf(testCase(mapOf("base" to 8.0, "height" to 5.0), "Area" to 40.0)),
        reference = REFERENCE
    )

    val rhombus = Formula(
        id = "rhombus", name = "Rhombus area", category = FormulaCategory.GEOMETRY, subcategory = "Quadrilaterals",
        description = "Area of a rhombus from its diagonals.", notation = "A = (d₁ × d₂) ÷ 2",
        assumptions = listOf("The diagonals are perpendicular."),
        inputs = listOf(input("d1", "Diagonal 1", "units", positive = true, default = "6"), input("d2", "Diagonal 2", "units", positive = true, default = "8")),
        outputLabels = listOf("Area" to "square units", "Side" to "units"),
        compute = { v ->
            val d1 = v.getValue("d1"); val d2 = v.getValue("d2")
            result(area("Area", d1 * d2 / 2.0, primary = true), l2("Side", Math.hypot(d1 / 2.0, d2 / 2.0)))
        },
        tests = listOf(testCase(mapOf("d1" to 6.0, "d2" to 8.0), "Area" to 24.0, "Side" to 5.0)),
        reference = REFERENCE
    )

    val trapezium = Formula(
        id = "trapezium", name = "Trapezium area", category = FormulaCategory.GEOMETRY, subcategory = "Quadrilaterals",
        description = "Area of a trapezium from its parallel sides and height.", notation = "A = ½(a + b)h",
        assumptions = listOf("a and b are the parallel sides."),
        inputs = listOf(
            input("a", "Parallel side a", "units", positive = true, default = "6"),
            input("b", "Parallel side b", "units", positive = true, default = "10"),
            input("height", "Height", "units", positive = true, default = "4")
        ),
        outputLabels = listOf("Area" to "square units"),
        compute = { v -> result(area("Area", 0.5 * (v.getValue("a") + v.getValue("b")) * v.getValue("height"), primary = true)) },
        tests = listOf(testCase(mapOf("a" to 6.0, "b" to 10.0, "height" to 4.0), "Area" to 32.0)),
        reference = REFERENCE
    )

    val regularPolygon = Formula(
        id = "regular-polygon", name = "Regular polygon", category = FormulaCategory.GEOMETRY, subcategory = "Polygons",
        description = "Area and perimeter of a regular polygon.", notation = "A = (n÷4)·s²·cot(π÷n);   P = n·s",
        assumptions = listOf("All sides and interior angles are equal."),
        inputs = listOf(
            input("sides", "Number of sides", "", integer = true, minimum = 3.0, maximum = 1000.0, default = "6"),
            input("side", "Side length", "units", positive = true, default = "5")
        ),
        outputLabels = listOf("Area" to "square units", "Perimeter" to "units"),
        compute = { v ->
            val n = v.getValue("sides")
            val s = v.getValue("side")
            val areaValue = n / 4.0 * s * s / Math.tan(Math.PI / n)
            result(area("Area", areaValue, primary = true), l2("Perimeter", n * s))
        },
        tests = listOf(testCase(mapOf("sides" to 6.0, "side" to 5.0), "Area" to 64.9519052838329, "Perimeter" to 30.0)),
        reference = REFERENCE
    )

    val cube = Formula(
        id = "cube", name = "Cube", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume, surface area and space diagonal of a cube.", notation = "V = a³;   S = 6a²;   d = a√3",
        assumptions = listOf("All edges are equal."),
        inputs = listOf(input("side", "Edge", "units", positive = true, default = "3")),
        outputLabels = listOf("Volume" to "cubic units", "Surface area" to "square units", "Space diagonal" to "units"),
        compute = { v ->
            val a = v.getValue("side")
            result(volume("Volume", a * a * a, primary = true), r2("Surface area", 6 * a * a), l2("Space diagonal", a * Math.sqrt(3.0)))
        },
        tests = listOf(testCase(mapOf("side" to 3.0), "Volume" to 27.0, "Surface area" to 54.0, "Space diagonal" to 5.196152422706632)),
        reference = REFERENCE
    )

    val cuboid = Formula(
        id = "cuboid", name = "Cuboid", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume, surface area and space diagonal of a cuboid.", notation = "V = lwh;   S = 2(lw + lh + wh);   d = √(l² + w² + h²)",
        assumptions = listOf("All angles are right angles."),
        inputs = listOf(
            input("length", "Length", "units", positive = true, default = "3"),
            input("width", "Width", "units", positive = true, default = "4"),
            input("height", "Height", "units", positive = true, default = "5")
        ),
        outputLabels = listOf("Volume" to "cubic units", "Surface area" to "square units", "Space diagonal" to "units"),
        compute = { v ->
            val l = v.getValue("length"); val w = v.getValue("width"); val h = v.getValue("height")
            result(
                volume("Volume", l * w * h, primary = true),
                r2("Surface area", 2 * (l * w + l * h + w * h)),
                l2("Space diagonal", Math.sqrt(l * l + w * w + h * h))
            )
        },
        tests = listOf(testCase(mapOf("length" to 3.0, "width" to 4.0, "height" to 5.0), "Volume" to 60.0, "Surface area" to 94.0, "Space diagonal" to 7.0710678118654755)),
        reference = REFERENCE
    )

    val cylinder = Formula(
        id = "cylinder", name = "Cylinder", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume, curved surface area and total surface area of a right circular cylinder.",
        notation = "V = πr²h;   C = 2πrh;   S = 2πr(r + h)",
        assumptions = listOf("The cylinder is a right circular cylinder."),
        inputs = listOf(input("radius", "Radius", "units", positive = true, default = "2"), input("height", "Height", "units", positive = true, default = "5")),
        outputLabels = listOf("Volume" to "cubic units", "Curved surface" to "square units", "Total surface" to "square units"),
        compute = { v ->
            val r = v.getValue("radius"); val h = v.getValue("height")
            result(
                volume("Volume", Math.PI * r * r * h, primary = true),
                r2("Curved surface", 2 * Math.PI * r * h),
                r2("Total surface", 2 * Math.PI * r * (r + h))
            )
        },
        tests = listOf(testCase(mapOf("radius" to 2.0, "height" to 5.0), "Volume" to 62.83185307179586, "Curved surface" to 62.83185307179586, "Total surface" to 87.96459430051421)),
        reference = REFERENCE
    )

    val cone = Formula(
        id = "cone", name = "Cone", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume and surface areas of a right circular cone.", notation = "V = ⅓πr²h;   l = √(r² + h²);   S = πr(r + l)",
        assumptions = listOf("The cone is a right circular cone."),
        inputs = listOf(input("radius", "Radius", "units", positive = true, default = "3"), input("height", "Height", "units", positive = true, default = "4")),
        outputLabels = listOf("Volume" to "cubic units", "Slant height" to "units", "Curved surface" to "square units", "Total surface" to "square units"),
        compute = { v ->
            val r = v.getValue("radius"); val h = v.getValue("height")
            val slant = Math.hypot(r, h)
            result(
                volume("Volume", Math.PI * r * r * h / 3.0, primary = true),
                l2("Slant height", slant),
                r2("Curved surface", Math.PI * r * slant),
                r2("Total surface", Math.PI * r * (r + slant))
            )
        },
        tests = listOf(
            testCase(
                mapOf("radius" to 3.0, "height" to 4.0),
                "Volume" to 37.69911184307752,
                "Slant height" to 5.0,
                "Curved surface" to 47.12388980384689,
                "Total surface" to 75.39822368615503
            )
        ),
        reference = REFERENCE
    )

    val sphere = Formula(
        id = "sphere", name = "Sphere", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume and surface area of a sphere.", notation = "V = 4/3·πr³;   S = 4πr²",
        assumptions = listOf("A perfect sphere is assumed."),
        inputs = listOf(input("radius", "Radius", "units", positive = true, default = "3")),
        outputLabels = listOf("Volume" to "cubic units", "Surface area" to "square units"),
        compute = { v ->
            val r = v.getValue("radius")
            result(volume("Volume", 4.0 / 3.0 * Math.PI * r * r * r, primary = true), r2("Surface area", 4 * Math.PI * r * r))
        },
        tests = listOf(testCase(mapOf("radius" to 3.0), "Volume" to 113.09733552923254, "Surface area" to 113.09733552923255)),
        reference = REFERENCE
    )

    val hemisphere = Formula(
        id = "hemisphere", name = "Hemisphere", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume and total surface area of a solid hemisphere.", notation = "V = 2/3·πr³;   S = 3πr²",
        assumptions = listOf("A solid hemisphere includes its flat circular face."),
        inputs = listOf(input("radius", "Radius", "units", positive = true, default = "3")),
        outputLabels = listOf("Volume" to "cubic units", "Total surface" to "square units", "Curved surface" to "square units"),
        compute = { v ->
            val r = v.getValue("radius")
            result(
                volume("Volume", 2.0 / 3.0 * Math.PI * r * r * r, primary = true),
                r2("Total surface", 3 * Math.PI * r * r),
                r2("Curved surface", 2 * Math.PI * r * r)
            )
        },
        tests = listOf(testCase(mapOf("radius" to 3.0), "Volume" to 56.54866776461627, "Total surface" to 84.82300164692441, "Curved surface" to 56.54866776461627)),
        reference = REFERENCE
    )

    val prism = Formula(
        id = "prism", name = "Prism volume", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume of a right prism from its base area and height.", notation = "V = A_base × h",
        assumptions = listOf("The base area is constant along the height."),
        inputs = listOf(input("baseArea", "Base area", "square units", positive = true, default = "12"), input("height", "Height", "units", positive = true, default = "5")),
        outputLabels = listOf("Volume" to "cubic units"),
        compute = { v -> result(volume("Volume", v.getValue("baseArea") * v.getValue("height"), primary = true)) },
        tests = listOf(testCase(mapOf("baseArea" to 12.0, "height" to 5.0), "Volume" to 60.0)),
        reference = REFERENCE
    )

    val pyramid = Formula(
        id = "pyramid", name = "Pyramid volume", category = FormulaCategory.GEOMETRY, subcategory = "Solids",
        description = "Volume of a pyramid from its base area and perpendicular height.", notation = "V = ⅓ × A_base × h",
        assumptions = listOf("The height is measured perpendicular to the base."),
        inputs = listOf(input("baseArea", "Base area", "square units", positive = true, default = "12"), input("height", "Height", "units", positive = true, default = "5")),
        outputLabels = listOf("Volume" to "cubic units"),
        compute = { v -> result(volume("Volume", v.getValue("baseArea") * v.getValue("height") / 3.0, primary = true)) },
        tests = listOf(testCase(mapOf("baseArea" to 12.0, "height" to 5.0), "Volume" to 20.0)),
        reference = REFERENCE
    )

    val pythagoras = Formula(
        id = "pythagoras", name = "Pythagoras — hypotenuse", category = FormulaCategory.GEOMETRY, subcategory = "Triangles",
        description = "Hypotenuse of a right triangle from the two legs.", notation = "c = √(a² + b²)",
        assumptions = listOf("The triangle has a right angle between a and b."),
        inputs = listOf(input("a", "Leg a", "units", positive = true, default = "3"), input("b", "Leg b", "units", positive = true, default = "4")),
        outputLabels = listOf("Hypotenuse" to "units"),
        compute = { v -> result(l2("Hypotenuse", Math.hypot(v.getValue("a"), v.getValue("b")), primary = true)) },
        tests = listOf(testCase(mapOf("a" to 3.0, "b" to 4.0), "Hypotenuse" to 5.0)),
        reference = REFERENCE
    )

    val pythagorasLeg = Formula(
        id = "pythagoras-leg", name = "Pythagoras — leg", category = FormulaCategory.GEOMETRY, subcategory = "Triangles",
        description = "Missing leg of a right triangle from the hypotenuse and the other leg.", notation = "b = √(c² − a²)",
        assumptions = listOf("The hypotenuse is the longest side, so c > a."),
        inputs = listOf(input("hypotenuse", "Hypotenuse", "units", positive = true, default = "5"), input("leg", "Known leg", "units", positive = true, default = "3")),
        outputLabels = listOf("Missing leg" to "units"),
        compute = { v ->
            val c = v.getValue("hypotenuse")
            val a = v.getValue("leg")
            if (c <= a) throw MathException.domain("The hypotenuse must be longer than the known leg.")
            result(l2("Missing leg", Math.sqrt(c * c - a * a), primary = true))
        },
        tests = listOf(testCase(mapOf("hypotenuse" to 5.0, "leg" to 3.0), "Missing leg" to 4.0)),
        reference = REFERENCE
    )

    val coordinateDistance = Formula(
        id = "coordinate-distance", name = "Distance between two points", category = FormulaCategory.GEOMETRY, subcategory = "Coordinates",
        description = "Euclidean distance between two points in the plane.", notation = "d = √((x₂−x₁)² + (y₂−y₁)²)",
        assumptions = listOf("Coordinates are Cartesian with equal unit scales."),
        inputs = listOf(
            input("x1", "x₁", "", default = "0"), input("y1", "y₁", "", default = "0"),
            input("x2", "x₂", "", default = "3"), input("y2", "y₂", "", default = "4")
        ),
        outputLabels = listOf("Distance" to "units"),
        compute = { v ->
            val dx = v.getValue("x2") - v.getValue("x1")
            val dy = v.getValue("y2") - v.getValue("y1")
            result(l2("Distance", Math.hypot(dx, dy), primary = true))
        },
        tests = listOf(testCase(mapOf("x1" to 0.0, "y1" to 0.0, "x2" to 3.0, "y2" to 4.0), "Distance" to 5.0)),
        reference = REFERENCE
    )

    val midpoint = Formula(
        id = "midpoint", name = "Midpoint", category = FormulaCategory.GEOMETRY, subcategory = "Coordinates",
        description = "Midpoint of the segment joining two points.", notation = "M = ((x₁+x₂)÷2, (y₁+y₂)÷2)",
        assumptions = listOf("Coordinates are Cartesian."),
        inputs = listOf(
            input("x1", "x₁", "", default = "0"), input("y1", "y₁", "", default = "0"),
            input("x2", "x₂", "", default = "4"), input("y2", "y₂", "", default = "6")
        ),
        outputLabels = listOf("Midpoint x" to "", "Midpoint y" to ""),
        compute = { v ->
            result(
                value("Midpoint x", (v.getValue("x1") + v.getValue("x2")) / 2.0, "", primary = true),
                value("Midpoint y", (v.getValue("y1") + v.getValue("y2")) / 2.0)
            )
        },
        tests = listOf(testCase(mapOf("x1" to 0.0, "y1" to 0.0, "x2" to 4.0, "y2" to 6.0), "Midpoint x" to 2.0, "Midpoint y" to 3.0)),
        reference = REFERENCE
    )

    val slope = Formula(
        id = "slope", name = "Slope of a line", category = FormulaCategory.GEOMETRY, subcategory = "Coordinates",
        description = "Slope of the line through two points, with a clear error for vertical lines.",
        notation = "m = (y₂ − y₁) ÷ (x₂ − x₁)",
        assumptions = listOf("A vertical line has an undefined slope, which is reported as an error rather than infinity."),
        inputs = listOf(
            input("x1", "x₁", "", default = "0"), input("y1", "y₁", "", default = "0"),
            input("x2", "x₂", "", default = "4"), input("y2", "y₂", "", default = "2")
        ),
        outputLabels = listOf("Slope" to "", "Angle" to "°"),
        compute = { v ->
            val dx = v.getValue("x2") - v.getValue("x1")
            if (dx == 0.0) throw MathException.domain("The line is vertical, so its slope is undefined.")
            val m = (v.getValue("y2") - v.getValue("y1")) / dx
            result(value("Slope", m, "", primary = true), value("Angle", Math.toDegrees(Math.atan(m)), "°"))
        },
        tests = listOf(testCase(mapOf("x1" to 0.0, "y1" to 0.0, "x2" to 4.0, "y2" to 2.0), "Slope" to 0.5)),
        reference = REFERENCE
    )

    val all: List<Formula> = listOf(
        squareArea, squarePerimeter, rectangleArea, triangleArea, triangleHeron, circle, circleRadiusFromArea,
        sector, parallelogram, rhombus, trapezium, regularPolygon, cube, cuboid, cylinder, cone, sphere,
        hemisphere, prism, pyramid, pythagoras, pythagorasLeg, coordinateDistance, midpoint, slope
    )
}
