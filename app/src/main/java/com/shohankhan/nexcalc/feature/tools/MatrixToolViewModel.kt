package com.shohankhan.nexcalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.MathException
import com.shohankhan.nexcalc.math.Matrix
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MatrixOperation(val label: String) {
    TRANSPOSE("Transpose A"),
    DETERMINANT("Determinant A"),
    INVERSE("Inverse A"),
    RANK("Rank A"),
    TRACE("Trace A"),
    ADD("A + B"),
    SUBTRACT("A − B"),
    MULTIPLY("A × B"),
    SOLVE("Solve A·x = b")
}

data class MatrixUiState(
    val aText: String = "1, 2\n3, 4",
    val bText: String = "5, 6\n7, 8",
    val vectorText: String = "1\n2",
    val result: String? = null,
    val error: String? = null,
    val lastOperation: String? = null,
    val saved: Boolean = false
) {
    val aDimensions: String get() = MatrixToolViewModel.dimensionsOf(aText)
    val bDimensions: String get() = MatrixToolViewModel.dimensionsOf(bText)
}

/** Matrix and vector workspace. Every operation validates dimensions and reports singular matrices. */
class MatrixToolViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(MatrixUiState())
    val state: StateFlow<MatrixUiState> = _state.asStateFlow()

    private val format = MutableStateFlow(NumberFormatOptions.Default)

    init {
        viewModelScope.launch { container.settings.collect { format.value = it.numberFormat } }
    }

    fun setA(text: String) = _state.update { it.copy(aText = text, saved = false) }

    fun setB(text: String) = _state.update { it.copy(bText = text, saved = false) }

    fun setVector(text: String) = _state.update { it.copy(vectorText = text, saved = false) }

    fun reset() {
        _state.value = MatrixUiState()
    }

    fun compute(operation: MatrixOperation) {
        try {
            val options = format.value
            val output = when (operation) {
                MatrixOperation.TRANSPOSE -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    "Aᵀ =\n${formatMatrix(a.transpose(), options)}"
                }
                MatrixOperation.DETERMINANT -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    requireSquare(a, "The determinant")
                    "det(A) = ${NumberFormatter.format(a.determinant(), options)}"
                }
                MatrixOperation.INVERSE -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    requireSquare(a, "The inverse")
                    "A⁻¹ =\n${formatMatrix(a.inverse(), options)}"
                }
                MatrixOperation.RANK -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    "rank(A) = ${a.rank()}"
                }
                MatrixOperation.TRACE -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    requireSquare(a, "The trace")
                    "trace(A) = ${NumberFormatter.format(a.trace(), options)}"
                }
                MatrixOperation.ADD -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    val b = parseMatrix(_state.value.bText, "Matrix B")
                    "A + B =\n${formatMatrix(a + b, options)}"
                }
                MatrixOperation.SUBTRACT -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    val b = parseMatrix(_state.value.bText, "Matrix B")
                    "A − B =\n${formatMatrix(a - b, options)}"
                }
                MatrixOperation.MULTIPLY -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    val b = parseMatrix(_state.value.bText, "Matrix B")
                    "A × B =\n${formatMatrix(a * b, options)}"
                }
                MatrixOperation.SOLVE -> {
                    val a = parseMatrix(_state.value.aText, "Matrix A")
                    requireSquare(a, "Solving A·x = b")
                    val b = parseVector(_state.value.vectorText)
                    if (b.size != a.rows) {
                        throw MathException.dimension("b must have ${a.rows} entries for a ${a.rows} × ${a.columns} matrix.")
                    }
                    val solution = a.solve(b)
                    val residual = a.residual(b, solution)
                    "x =\n${solution.joinToString("\n") { NumberFormatter.format(it, options) }}\n\nresidual ‖Ax − b‖ = ${NumberFormatter.format(residual, options)}"
                }
            }
            _state.update { it.copy(result = output, error = null, lastOperation = operation.label, saved = false) }
            viewModelScope.launch {
                container.historyRepository.record(
                    expression = "${operation.label} — A = ${_state.value.aText.replace("\n", "; ")}",
                    result = output.lineSequence().take(3).joinToString(" "),
                    source = HistorySource.MATRIX
                )
            }
        } catch (error: MathException) {
            _state.update { it.copy(result = null, error = error.describe(), lastOperation = operation.label) }
        }
    }

    fun saveToWorkspace() {
        val result = _state.value.result ?: return
        val payload = buildString {
            appendLine("A:")
            appendLine(_state.value.aText)
            appendLine("B:")
            appendLine(_state.value.bText)
            appendLine("b:")
            appendLine(_state.value.vectorText)
            appendLine()
            appendLine(result)
        }
        viewModelScope.launch {
            container.savedRepository.save(
                SavedKind.CALCULATION,
                _state.value.lastOperation ?: "Matrix result",
                payload
            )
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        fun dimensionsOf(text: String): String = try {
            val a = parseMatrixStatic(text)
            "${a.rows} × ${a.columns}"
        } catch (error: MathException) {
            "—"
        }

        fun parseMatrix(text: String, name: String = "Matrix"): Matrix = parseMatrixStatic(text, name)

        private fun parseMatrixStatic(text: String, name: String = "Matrix"): Matrix {
            val rows = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (rows.isEmpty()) throw MathException.invalid("Enter at least one row for $name.")
            if (rows.size > Matrix.MAX_DIMENSION) {
                throw MathException.limit("$name can have at most ${Matrix.MAX_DIMENSION} rows.")
            }
            val values = rows.mapIndexed { index, row ->
                val parts = row.split(Regex("[,\\s]+")).filter { it.isNotBlank() }
                if (parts.size > Matrix.MAX_DIMENSION) {
                    throw MathException.limit("$name can have at most ${Matrix.MAX_DIMENSION} columns.")
                }
                parts.map { part ->
                    part.toDoubleOrNull() ?: throw MathException.invalid("'$part' in row ${index + 1} of $name is not a number.")
                }
            }
            val columns = values.first().size
            if (values.any { it.size != columns }) {
                throw MathException.invalid("Every row of $name must contain the same number of values.")
            }
            return Matrix.of(values.size, columns, values)
        }

        fun parseVector(text: String): List<Double> {
            val parts = text.split(Regex("[,\\s]+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) throw MathException.invalid("Enter the right-hand side vector b.")
            return parts.map { part ->
                part.toDoubleOrNull() ?: throw MathException.invalid("'$part' in b is not a number.")
            }
        }

        fun formatMatrix(matrix: Matrix, options: NumberFormatOptions): String {
            val cells = matrix.toRows().map { row -> row.map { NumberFormatter.format(it, options) } }
            val width = cells.flatten().maxOfOrNull { it.length } ?: 0
            return cells.joinToString("\n") { row -> row.joinToString("   ") { it.padStart(width) } }
        }

        private fun requireSquare(matrix: Matrix, what: String) {
            if (!matrix.isSquare) throw MathException.dimension("$what requires a square matrix; A is ${matrix.rows} × ${matrix.columns}.")
        }
    }
}
