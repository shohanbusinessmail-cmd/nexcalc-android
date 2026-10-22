package com.shohankhan.nexcalc.math

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Dense real matrix with bounded dimensions.
 *
 * Every operation validates shape compatibility and finiteness, and raises a structured error
 * instead of returning a misleading value. The size bound (12 × 12) keeps determinant, inverse
 * and elimination work far below the point where the UI could stall.
 */
class Matrix private constructor(
    val rows: Int,
    val columns: Int,
    private val values: Array<DoubleArray>
) {

    init {
        require(values.size == rows) { "Row count mismatch" }
        values.forEach { require(it.size == columns) { "Column count mismatch" } }
    }

    operator fun get(row: Int, column: Int): Double {
        if (row !in 0 until rows || column !in 0 until columns) {
            throw MathException.invalid("Cell ($row, $column) is outside this matrix.")
        }
        return values[row][column]
    }

    fun row(row: Int): List<Double> {
        if (row !in 0 until rows) throw MathException.invalid("Row $row does not exist.")
        return values[row].toList()
    }

    fun column(column: Int): List<Double> {
        if (column !in 0 until columns) throw MathException.invalid("Column $column does not exist.")
        return values.map { it[column] }
    }

    fun toRows(): List<List<Double>> = values.map { it.toList() }

    val isSquare: Boolean get() = rows == columns

    operator fun plus(other: Matrix): Matrix {
        requireSameShape(other, "addition")
        val result = Array(rows) { r -> DoubleArray(columns) { c -> values[r][c] + other.values[r][c] } }
        return wrap(rows, columns, result)
    }

    operator fun minus(other: Matrix): Matrix {
        requireSameShape(other, "subtraction")
        val result = Array(rows) { r -> DoubleArray(columns) { c -> values[r][c] - other.values[r][c] } }
        return wrap(rows, columns, result)
    }

    operator fun times(other: Matrix): Matrix {
        if (columns != other.rows) {
            throw MathException.dimension(
                "Cannot multiply a ${rows}×$columns matrix by a ${other.rows}×${other.columns} matrix."
            )
        }
        val result = Array(rows) { DoubleArray(other.columns) }
        for (r in 0 until rows) {
            for (c in 0 until other.columns) {
                var sum = 0.0
                for (k in 0 until columns) sum += values[r][k] * other.values[k][c]
                result[r][c] = sum
            }
        }
        return wrap(rows, other.columns, result)
    }

    operator fun times(scalar: Double): Matrix {
        requireFiniteScalar(scalar)
        return wrap(rows, columns, Array(rows) { r -> DoubleArray(columns) { c -> values[r][c] * scalar } })
    }

    fun transpose(): Matrix = wrap(columns, rows, Array(columns) { c -> DoubleArray(rows) { r -> values[r][c] } })

    fun trace(): Double {
        requireSquare("trace")
        var sum = 0.0
        for (i in 0 until rows) sum += values[i][i]
        return sum
    }

    /** Determinant via LU decomposition with partial pivoting. */
    fun determinant(): Double {
        requireSquare("determinant")
        val n = rows
        val a = Array(n) { values[it].clone() }
        var determinant = 1.0
        for (pivot in 0 until n) {
            var best = pivot
            for (r in pivot + 1 until n) if (abs(a[r][pivot]) > abs(a[best][pivot])) best = r
            if (abs(a[best][pivot]) < EPSILON) return 0.0
            if (best != pivot) {
                val swap = a[best]
                a[best] = a[pivot]
                a[pivot] = swap
                determinant = -determinant
            }
            determinant *= a[pivot][pivot]
            for (r in pivot + 1 until n) {
                val factor = a[r][pivot] / a[pivot][pivot]
                for (c in pivot until n) a[r][c] -= factor * a[pivot][c]
            }
        }
        return determinant
    }

    /** Gauss-Jordan inverse; raises [MathErrorCode.SINGULAR] for singular matrices. */
    fun inverse(): Matrix {
        requireSquare("inverse")
        val n = rows
        val a = Array(n) { values[it].clone() }
        val identity = Array(n) { r -> DoubleArray(n) { c -> if (r == c) 1.0 else 0.0 } }
        for (pivot in 0 until n) {
            var best = pivot
            for (r in pivot + 1 until n) if (abs(a[r][pivot]) > abs(a[best][pivot])) best = r
            if (abs(a[best][pivot]) < EPSILON) throw MathException.singular("This matrix is singular and has no inverse.")
            if (best != pivot) {
                val swapA = a[best]; a[best] = a[pivot]; a[pivot] = swapA
                val swapI = identity[best]; identity[best] = identity[pivot]; identity[pivot] = swapI
            }
            val pivotValue = a[pivot][pivot]
            for (c in 0 until n) {
                a[pivot][c] /= pivotValue
                identity[pivot][c] /= pivotValue
            }
            for (r in 0 until n) {
                if (r == pivot) continue
                val factor = a[r][pivot]
                if (factor == 0.0) continue
                for (c in 0 until n) {
                    a[r][c] -= factor * a[pivot][c]
                    identity[r][c] -= factor * identity[pivot][c]
                }
            }
        }
        return wrap(n, n, identity)
    }

    /** Rank via row-echelon reduction. */
    fun rank(): Int {
        val a = Array(rows) { values[it].clone() }
        var rank = 0
        var row = 0
        var column = 0
        while (row < rows && column < columns) {
            var best = row
            for (r in row + 1 until rows) if (abs(a[r][column]) > abs(a[best][column])) best = r
            if (abs(a[best][column]) < EPSILON) {
                column++
                continue
            }
            val swap = a[best]; a[best] = a[row]; a[row] = swap
            val pivot = a[row][column]
            for (r in row + 1 until rows) {
                val factor = a[r][column] / pivot
                for (c in column until columns) a[r][c] -= factor * a[row][c]
            }
            rank++
            row++
            column++
        }
        return rank
    }

    /** Solves A·x = b for a square matrix, raising an error for singular systems. */
    fun solve(rhs: List<Double>): DoubleArray {
        requireSquare("solve")
        if (rhs.size != rows) throw MathException.dimension("The right-hand side must have ${rows} entries.")
        val n = rows
        val a = Array(n) { r -> DoubleArray(n + 1) { c -> if (c < n) values[r][c] else rhs[r] } }
        for (pivot in 0 until n) {
            var best = pivot
            for (r in pivot + 1 until n) if (abs(a[r][pivot]) > abs(a[best][pivot])) best = r
            if (abs(a[best][pivot]) < EPSILON) throw MathException.singular("The system has no unique solution.")
            val swap = a[best]; a[best] = a[pivot]; a[pivot] = swap
            for (r in pivot + 1 until n) {
                val factor = a[r][pivot] / a[pivot][pivot]
                for (c in pivot..n) a[r][c] -= factor * a[pivot][c]
            }
        }
        val solution = DoubleArray(n)
        for (r in n - 1 downTo 0) {
            var sum = a[r][n]
            for (c in r + 1 until n) sum -= a[r][c] * solution[c]
            solution[r] = sum / a[r][r]
        }
        return solution
    }

    /** Maximum absolute residual of A·x = b, used to verify a solve. */
    fun residual(rhs: List<Double>, solution: DoubleArray): Double {
        var worst = 0.0
        for (r in 0 until rows) {
            var sum = 0.0
            for (c in 0 until columns) sum += values[r][c] * solution[c]
            worst = maxOf(worst, abs(sum - rhs[r]))
        }
        return worst
    }

    private fun requireSquare(operation: String) {
        if (!isSquare) throw MathException.dimension("The $operation is only defined for square matrices.")
    }

    private fun requireSameShape(other: Matrix, operation: String) {
        if (rows != other.rows || columns != other.columns) {
            throw MathException.dimension(
                "Matrix $operation requires equal dimensions; got ${rows}×$columns and ${other.rows}×${other.columns}."
            )
        }
    }

    fun formatted(): String = toRows().joinToString("\n") { row ->
        row.joinToString("  ") { value -> NumberFormatter.format(value, NumberFormatOptions(decimalPrecision = 6, useGrouping = false)) }
    }

    companion object {
        const val MAX_DIMENSION = 12
        private const val EPSILON = 1e-12

        fun of(rows: Int, columns: Int, entries: List<List<Double>>): Matrix {
            validateDimensions(rows, columns)
            if (entries.size != rows) throw MathException.invalid("Expected $rows rows but received ${entries.size}.")
            val array = Array(rows) { r ->
                val rowValues = entries[r]
                if (rowValues.size != columns) {
                    throw MathException.invalid("Row ${r + 1} must contain $columns values.")
                }
                DoubleArray(columns) { c ->
                    val value = rowValues[c]
                    if (!value.isFinite()) throw MathException.invalid("Every matrix entry must be a finite number.")
                    value
                }
            }
            return Matrix(rows, columns, array)
        }

        fun identity(size: Int): Matrix {
            validateDimensions(size, size)
            return Matrix(size, size, Array(size) { r -> DoubleArray(size) { c -> if (r == c) 1.0 else 0.0 } })
        }

        fun zero(rows: Int, columns: Int): Matrix {
            validateDimensions(rows, columns)
            return Matrix(rows, columns, Array(rows) { DoubleArray(columns) })
        }

        /**
         * Parses text such as `1, 2; 3, 4` or one row per line. Entries may be separated by
         * commas, semicolons, tabs or spaces; rows by newlines or semicolons when no newline exists.
         */
        fun parse(text: String): Matrix {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) throw MathException(MathErrorCode.EMPTY_INPUT, "Enter matrix values.")
            val lines = trimmed.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            val rowStrings = if (lines.size > 1) lines else trimmed.split(';').map { it.trim() }.filter { it.isNotEmpty() }
            val parsed = rowStrings.map { row ->
                row.split(Regex("[,;\\t ]+")).filter { it.isNotEmpty() }.map { token ->
                    token.toDoubleOrNull() ?: throw MathException.invalid("'$token' is not a valid number.")
                }
            }
            val columns = parsed.first().size
            if (parsed.any { it.size != columns }) throw MathException.invalid("Every row must contain the same number of values.")
            return of(parsed.size, columns, parsed)
        }

        private fun validateDimensions(rows: Int, columns: Int) {
            if (rows !in 1..MAX_DIMENSION || columns !in 1..MAX_DIMENSION) {
                throw MathException.limit("Matrices are limited to $MAX_DIMENSION × $MAX_DIMENSION.")
            }
        }

        private fun requireFiniteScalar(value: Double) {
            if (!value.isFinite()) throw MathException.invalid("The value must be finite.")
        }
    }

    private fun wrap(rows: Int, columns: Int, array: Array<DoubleArray>): Matrix {
        array.forEach { row -> row.forEach { checkFinite(it) } }
        return Matrix(rows, columns, array)
    }

    private fun checkFinite(value: Double) {
        if (!value.isFinite()) throw MathException.overflow("The result contains a value outside the supported range.")
    }

    override fun toString(): String = formatted()
}

/** Vector helpers used by the matrix workspace. */
object VectorMath {

    fun magnitude(values: List<Double>): Double {
        if (values.isEmpty()) throw MathException.invalid("A vector needs at least one component.")
        var sum = 0.0
        for (value in values) sum += value * value
        return sqrt(sum)
    }

    fun dot(a: List<Double>, b: List<Double>): Double {
        if (a.size != b.size) throw MathException.dimension("Dot product requires vectors of equal length.")
        var sum = 0.0
        for (index in a.indices) sum += a[index] * b[index]
        return sum
    }

    fun cross(a: List<Double>, b: List<Double>): List<Double> {
        if (a.size != 3 || b.size != 3) throw MathException.dimension("The cross product is defined for 3-dimensional vectors.")
        return listOf(
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        )
    }

    fun normalize(values: List<Double>): List<Double> {
        val length = magnitude(values)
        if (length == 0.0) throw MathException.domain("A zero vector cannot be normalised.")
        return values.map { it / length }
    }

    fun angleBetween(a: List<Double>, b: List<Double>): Double {
        val lengths = magnitude(a) * magnitude(b)
        if (lengths == 0.0) throw MathException.domain("The angle is undefined for a zero vector.")
        val cosine = (dot(a, b) / lengths).coerceIn(-1.0, 1.0)
        return Math.toDegrees(kotlin.math.acos(cosine))
    }
}
