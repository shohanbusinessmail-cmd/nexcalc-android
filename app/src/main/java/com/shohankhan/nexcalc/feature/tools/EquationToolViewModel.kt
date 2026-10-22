package com.shohankhan.nexcalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.CalculatorEngine
import com.shohankhan.nexcalc.math.MathException
import com.shohankhan.nexcalc.math.NumericSolution
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import com.shohankhan.nexcalc.math.Solvers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NumericMethod(val label: String) {
    BISECTION("Bisection"),
    NEWTON("Newton–Raphson"),
    SECANT("Secant")
}

data class EquationUiState(
    val matrixText: String = "2, 1\n1, -1",
    val rhsText: String = "5\n1",
    val coefficientsText: String = "1, -6, 11, -6",
    val functionText: String = "x^2 - 2",
    val method: NumericMethod = NumericMethod.BISECTION,
    val lowerText: String = "1",
    val upperText: String = "2",
    val toleranceText: String = "0.000001",
    val systemResult: String? = null,
    val systemError: String? = null,
    val roots: List<String> = emptyList(),
    val rootsError: String? = null,
    val numeric: NumericSolution? = null,
    val numericError: String? = null,
    val saved: Boolean = false
)

/** Three equation workflows: linear systems, polynomial roots and bounded numerical methods. */
class EquationToolViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(EquationUiState())
    val state: StateFlow<EquationUiState> = _state.asStateFlow()

    private val format = MutableStateFlow(NumberFormatOptions.Default)
    private var angleMode = com.shohankhan.nexcalc.math.AngleMode.RADIANS

    init {
        viewModelScope.launch {
            container.settings.collect {
                format.value = it.numberFormat
                angleMode = it.angleMode
            }
        }
    }

    fun setMatrix(text: String) = _state.update { it.copy(matrixText = text, systemResult = null, systemError = null, saved = false) }
    fun setRhs(text: String) = _state.update { it.copy(rhsText = text, systemResult = null, systemError = null, saved = false) }
    fun setCoefficients(text: String) = _state.update { it.copy(coefficientsText = text, roots = emptyList(), rootsError = null) }
    fun setFunction(text: String) = _state.update { it.copy(functionText = text, numeric = null, numericError = null) }
    fun setMethod(method: NumericMethod) = _state.update { it.copy(method = method, numeric = null, numericError = null) }
    fun setLower(text: String) = _state.update { it.copy(lowerText = text) }
    fun setUpper(text: String) = _state.update { it.copy(upperText = text) }
    fun setTolerance(text: String) = _state.update { it.copy(toleranceText = text) }

    fun solveSystem() {
        try {
            val a = MatrixToolViewModel.parseMatrix(_state.value.matrixText, "Coefficient matrix")
            if (!a.isSquare) throw MathException.dimension("The coefficient matrix must be square.")
            val b = MatrixToolViewModel.parseVector(_state.value.rhsText)
            if (b.size != a.rows) throw MathException.dimension("The constant vector must have ${a.rows} entries.")
            val solution = a.solve(b)
            val residual = a.residual(b, solution)
            val text = buildString {
                solution.forEachIndexed { index, value ->
                    appendLine("x${index + 1} = ${NumberFormatter.format(value, format.value)}")
                }
                appendLine()
                append("residual ‖Ax − b‖ = ${NumberFormatter.format(residual, format.value)}")
            }
            _state.update { it.copy(systemResult = text, systemError = null) }
            record("Linear system ${a.rows}×${a.rows}", text.lineSequence().firstOrNull().orEmpty())
        } catch (error: MathException) {
            _state.update { it.copy(systemResult = null, systemError = error.describe()) }
        }
    }

    fun findRoots() {
        try {
            val coefficients = _state.value.coefficientsText
                .split(Regex("[,\\s]+"))
                .filter { it.isNotBlank() }
                .map { part -> part.toDoubleOrNull() ?: throw MathException.invalid("'$part' is not a coefficient.") }
            val roots = Solvers.polynomialRoots(coefficients)
            val text = roots.mapIndexed { index, root ->
                "x${index + 1} = ${NumberFormatter.formatComplex(root, format.value)}"
            }
            _state.update { it.copy(roots = text, rootsError = null) }
            record("Polynomial roots (degree ${coefficients.size - 1})", text.firstOrNull().orEmpty())
        } catch (error: MathException) {
            _state.update { it.copy(roots = emptyList(), rootsError = error.describe()) }
        }
    }

    fun solveNumeric() {
        try {
            val tolerance = _state.value.toleranceText.trim().toDoubleOrNull()
                ?: throw MathException.invalid("Enter a numeric tolerance such as 0.000001.")
            if (tolerance < 1e-14 || tolerance > 1.0) {
                throw MathException.invalid("Tolerance must be between 1e-14 and 1.")
            }
            val lower = _state.value.lowerText.trim().toDoubleOrNull()
                ?: throw MathException.invalid("Enter a numeric lower bound.")
            val upper = _state.value.upperText.trim().toDoubleOrNull()
                ?: throw MathException.invalid("Enter a numeric upper bound.")
            if (lower >= upper) throw MathException.invalid("The lower bound must be smaller than the upper bound.")
            val f = CalculatorEngine.functionOf(_state.value.functionText, "x", angleMode)
            val solution = when (_state.value.method) {
                NumericMethod.BISECTION -> Solvers.bisection(f, lower, upper, tolerance)
                NumericMethod.NEWTON -> {
                    val start = (lower + upper) / 2.0
                    Solvers.newtonRaphson(f, { x -> Solvers.derivative(f, x) }, start, tolerance)
                }
                NumericMethod.SECANT -> Solvers.secant(f, lower, upper, tolerance)
            }
            _state.update { it.copy(numeric = solution, numericError = null) }
            if (solution.converged) {
                record(
                    "Root of ${_state.value.functionText} in [$lower, $upper]",
                    "x = ${solution.root?.let { NumberFormatter.format(it, format.value) } ?: "—"}"
                )
            }
        } catch (error: MathException) {
            _state.update { it.copy(numeric = null, numericError = error.describe()) }
        }
    }

    fun saveEquationSet() {
        val payload = buildString {
            appendLine("System: A = ${_state.value.matrixText.replace("\n", "; ")}, b = ${_state.value.rhsText}")
            appendLine("Polynomial coefficients: ${_state.value.coefficientsText}")
            appendLine("Numeric: ${_state.value.method.label} on ${_state.value.functionText} in [${_state.value.lowerText}, ${_state.value.upperText}]")
            _state.value.systemResult?.let { appendLine(it) }
            _state.value.roots.forEach { appendLine(it) }
            _state.value.numeric?.let { appendLine("${it.method}: ${it.message}") }
        }
        viewModelScope.launch {
            container.savedRepository.save(SavedKind.EQUATION_SET, "Equation set", payload)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun record(expression: String, result: String) {
        viewModelScope.launch {
            container.historyRepository.record(expression, result, HistorySource.EQUATION)
        }
    }
}
