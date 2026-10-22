package com.shohankhan.nexcalc.feature.formulas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.core.design.CalcOutcomeUi
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.Formula
import com.shohankhan.nexcalc.math.FormulaEngine
import com.shohankhan.nexcalc.math.FormulaEvaluation
import com.shohankhan.nexcalc.math.FormulaRegistry
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormulaInputState(val id: String, val value: String, val error: String? = null)

data class FormulaValueView(val label: String, val value: String, val unit: String, val primary: Boolean)

data class FormulaResultView(
    val values: List<FormulaValueView>,
    val steps: List<String>,
    val warnings: List<String>
) {
    val primaryText: String get() = values.firstOrNull { it.primary }?.let { "${it.value} ${it.unit}".trim() }
        ?: values.firstOrNull()?.let { "${it.value} ${it.unit}".trim() }
        ?: ""
}

data class FormulaDetailState(
    val inputs: List<FormulaInputState> = emptyList(),
    val result: FormulaResultView? = null,
    val failure: String? = null,
    val hint: String? = null,
    val saved: Boolean = false
)

/** Calculates one formula from the catalogue and records successful evaluations in history. */
class FormulaDetailViewModel(
    private val container: AppContainer,
    private val formulaId: String
) : ViewModel() {

    val formula: Formula? = FormulaRegistry.find(formulaId)

    private val format = MutableStateFlow(NumberFormatOptions.Default)
    private val _state = MutableStateFlow(
        FormulaDetailState(
            inputs = formula?.inputs?.map { FormulaInputState(it.id, it.defaultValue) }.orEmpty()
        )
    )
    val state: StateFlow<FormulaDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.collect { format.value = it.numberFormat }
        }
    }

    fun setInput(id: String, value: String) {
        _state.update { current ->
            current.copy(
                inputs = current.inputs.map { if (it.id == id) it.copy(value = value, error = null) else it },
                failure = null,
                hint = null,
                saved = false
            )
        }
    }

    fun reset() {
        _state.value = FormulaDetailState(
            inputs = formula?.inputs?.map { FormulaInputState(it.id, it.defaultValue) }.orEmpty()
        )
    }

    fun calculate() {
        val target = formula ?: return
        val raw = _state.value.inputs.associate { it.id to it.value }
        when (val evaluation = FormulaEngine.calculate(target, raw)) {
            is FormulaEvaluation.Invalid -> {
                _state.update { current ->
                    current.copy(
                        inputs = current.inputs.map { input ->
                            input.copy(error = evaluation.errors[input.id])
                        },
                        result = null,
                        saved = false
                    )
                }
            }
            is FormulaEvaluation.Failed -> {
                _state.update { it.copy(result = null, failure = evaluation.message, hint = evaluation.hint, saved = false) }
            }
            is FormulaEvaluation.Success -> {
                val formatted = evaluation.result.values.map { value ->
                    FormulaValueView(
                        label = value.label,
                        value = NumberFormatter.format(value.value, format.value),
                        unit = value.unit,
                        primary = value.primary
                    )
                }
                val view = FormulaResultView(formatted, evaluation.result.steps, evaluation.result.warnings)
                _state.update { it.copy(result = view, failure = null, hint = null, saved = false) }
                viewModelScope.launch {
                    container.historyRepository.record(
                        expression = describe(target, _state.value.inputs),
                        result = view.primaryText.ifBlank { formatted.firstOrNull()?.value.orEmpty() },
                        source = HistorySource.formula(target.id)
                    )
                }
            }
        }
    }

    /** Persists this calculation in the personal workspace. */
    fun saveToWorkspace() {
        val target = formula ?: return
        val result = _state.value.result ?: return
        val payload = buildString {
            appendLine(describe(target, _state.value.inputs))
            appendLine("= ${result.primaryText}")
            result.values.forEach { appendLine("${it.label}: ${it.value} ${it.unit}".trim()) }
        }
        viewModelScope.launch {
            container.savedRepository.save(SavedKind.FORMULA, target.name, payload)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun describe(target: Formula, inputs: List<FormulaInputState>): String {
        val parts = target.inputs.mapNotNull { definition ->
            val value = inputs.firstOrNull { it.id == definition.id }?.value?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            "${definition.label}=$value ${definition.unit}".trim()
        }
        return "${target.name}(${parts.joinToString(", ")})"
    }
}

/** Converts the formula list state for the catalogue screen. */
data class FormulaListState(
    val query: String = "",
    val category: String? = null,
    val formulas: List<Formula> = FormulaRegistry.all,
    val count: Int = FormulaRegistry.count
)
