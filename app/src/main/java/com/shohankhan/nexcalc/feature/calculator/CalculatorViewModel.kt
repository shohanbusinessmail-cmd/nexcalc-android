package com.shohankhan.nexcalc.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.core.design.CalcOutcomeUi
import com.shohankhan.nexcalc.core.design.toUi
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.math.AngleMode
import com.shohankhan.nexcalc.math.CalculationOutcome
import com.shohankhan.nexcalc.math.Complex
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sources stored with each history row. */
object HistorySource {
    const val STANDARD = "STANDARD"
    const val SCIENTIFIC = "SCIENTIFIC"
    const val CONVERTER = "CONVERTER"
    const val MATRIX = "MATRIX"
    const val STATISTICS = "STATISTICS"
    const val EQUATION = "EQUATION"
    const val BASES = "BASES"
    const val RESISTOR = "RESISTOR"
    fun formula(id: String) = "FORMULA:$id"
    fun tool(id: String) = "TOOL:$id"

    fun label(source: String): String = when {
        source == STANDARD -> "Standard"
        source == SCIENTIFIC -> "Scientific"
        source == CONVERTER -> "Converters"
        source == MATRIX -> "Matrix"
        source == STATISTICS -> "Statistics"
        source == EQUATION -> "Equations"
        source == BASES -> "Number systems"
        source == RESISTOR -> "Resistor"
        source.startsWith("FORMULA:") -> "Formula"
        source.startsWith("TOOL:") -> "Quick Solve"
        else -> source
    }
}

data class CalculatorUiState(
    val outcome: CalcOutcomeUi? = null,
    val lastExpression: String = "",
    val memory: Double = 0.0,
    val memorySet: Boolean = false,
    val ans: Complex? = null,
    val angleMode: AngleMode = AngleMode.DEGREES,
    val format: NumberFormatOptions = NumberFormatOptions.Default,
    val settings: AppSettings = AppSettings()
)

/**
 * Shared calculator logic for the Standard and Scientific destinations.
 *
 * The expression draft lives in the screen (cursor-aware text field, saved across process death);
 * this ViewModel owns evaluation, memory, Ans, angle mode and history recording. A calculation is
 * recorded only when [evaluate] is called from an explicit equals press.
 */
class CalculatorViewModel(
    private val container: AppContainer,
    private val source: String
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.collect { settings ->
                _state.update {
                    it.copy(
                        angleMode = settings.angleMode,
                        format = settings.numberFormat,
                        settings = settings
                    )
                }
            }
        }
    }

    fun evaluate(expression: String) {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(outcome = CalcOutcomeUi(error = com.shohankhan.nexcalc.core.design.CalcErrorUi("Enter an expression to evaluate."))) }
            return
        }
        val current = _state.value
        val outcome = CalculatorEngineBridge.evaluate(trimmed, current.angleMode, current.ans)
        when (outcome) {
            is CalculationOutcome.Success -> {
                val ui = outcome.toUi(current.format)
                _state.update {
                    it.copy(outcome = ui, lastExpression = trimmed, ans = outcome.value)
                }
                viewModelScope.launch {
                    container.historyRepository.record(trimmed, ui.value, source)
                }
            }
            is CalculationOutcome.Failure -> {
                _state.update { it.copy(outcome = outcome.toUi(current.format), lastExpression = trimmed) }
            }
        }
    }

    /** Live preview while typing: updates the displayed value without touching history or Ans. */
    fun preview(expression: String) {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(outcome = null) }
            return
        }
        val current = _state.value
        _state.update { it.copy(outcome = CalculatorEngineBridge.evaluate(trimmed, current.angleMode, current.ans).toUi(current.format)) }
    }

    fun clearResult() {
        _state.update { it.copy(outcome = null) }
    }

    fun storeMemory(value: Complex) {
        if (!value.isReal) return
        _state.update { it.copy(memory = value.re, memorySet = true) }
    }

    fun addToMemory(value: Complex) {
        if (!value.isReal) return
        _state.update { it.copy(memory = it.memory + value.re, memorySet = true) }
    }

    fun subtractFromMemory(value: Complex) {
        if (!value.isReal) return
        _state.update { it.copy(memory = it.memory - value.re, memorySet = true) }
    }

    fun clearMemory() {
        _state.update { it.copy(memory = 0.0, memorySet = false) }
    }

    /** Text inserted by MR; null when memory is empty. */
    fun memoryText(): String? {
        val current = _state.value
        if (!current.memorySet) return null
        return NumberFormatter.format(current.memory, current.format, exact = true)
    }

    fun setAngleMode(mode: AngleMode) {
        viewModelScope.launch { container.preferences.update { it.copy(angleMode = mode) } }
    }

    fun cycleAngleMode() {
        val next = when (_state.value.angleMode) {
            AngleMode.DEGREES -> AngleMode.RADIANS
            AngleMode.RADIANS -> AngleMode.GRADIANS
            AngleMode.GRADIANS -> AngleMode.DEGREES
        }
        setAngleMode(next)
    }

    fun saveExpression(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            container.savedRepository.save(
                com.shohankhan.nexcalc.core.data.local.SavedKind.EXPRESSION,
                title = text.take(80),
                payload = text
            )
        }
    }
}

/** Thin indirection so the ViewModel stays testable without touching the object's internals. */
internal object CalculatorEngineBridge {
    fun evaluate(expression: String, angleMode: AngleMode, ans: Complex?): CalculationOutcome =
        com.shohankhan.nexcalc.math.CalculatorEngine.evaluate(expression, angleMode, ans = ans)
}
