package com.shohankhan.nexcalc.feature.quicksolve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.MathException
import com.shohankhan.nexcalc.math.QuickTool
import com.shohankhan.nexcalc.math.QuickToolRegistry
import com.shohankhan.nexcalc.math.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolInputState(val id: String, val value: String, val error: String? = null)

data class QuickToolState(
    val inputs: List<ToolInputState> = emptyList(),
    val result: ToolResult? = null,
    val failure: String? = null,
    val saved: Boolean = false
)

/** Runs one Quick Solve tool, recording successful results and offering workspace save. */
class QuickToolDetailViewModel(
    private val container: AppContainer,
    private val toolId: String
) : ViewModel() {

    val tool: QuickTool? = QuickToolRegistry.find(toolId)

    private val _state = MutableStateFlow(
        QuickToolState(inputs = tool?.inputs?.map { ToolInputState(it.id, it.defaultValue) }.orEmpty())
    )
    val state: StateFlow<QuickToolState> = _state.asStateFlow()

    fun setInput(id: String, value: String) {
        _state.update { current ->
            current.copy(
                inputs = current.inputs.map { if (it.id == id) it.copy(value = value, error = null) else it },
                failure = null,
                saved = false
            )
        }
    }

    fun reset() {
        _state.value = QuickToolState(inputs = tool?.inputs?.map { ToolInputState(it.id, it.defaultValue) }.orEmpty())
    }

    fun calculate() {
        val target = tool ?: return
        val values = _state.value.inputs.associate { it.id to it.value }
        try {
            val result = target.compute(values)
            _state.update { it.copy(result = result, failure = null, saved = false) }
            viewModelScope.launch {
                container.historyRepository.record(
                    expression = describe(target, _state.value.inputs),
                    result = result.headline,
                    source = HistorySource.tool(target.id)
                )
            }
        } catch (error: MathException) {
            val message = error.describe() + (error.hint?.let { " $it" } ?: "")
            _state.update { current ->
                current.copy(
                    result = null,
                    failure = message,
                    inputs = current.inputs.map { input ->
                        input.copy(error = if (error.code == com.shohankhan.nexcalc.math.MathErrorCode.EMPTY_INPUT) "Required." else null)
                    }
                )
            }
        } catch (error: Exception) {
            _state.update { it.copy(result = null, failure = error.message ?: "The tool could not complete this calculation.") }
        }
    }

    fun saveToWorkspace() {
        val target = tool ?: return
        val result = _state.value.result ?: return
        val payload = buildString {
            appendLine(describe(target, _state.value.inputs))
            appendLine(result.headline)
            result.outputs.forEach { appendLine("${it.label}: ${it.value}") }
        }
        viewModelScope.launch {
            container.savedRepository.save(SavedKind.CALCULATION, target.name, payload)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun describe(target: QuickTool, inputs: List<ToolInputState>): String {
        val parts = target.inputs.mapNotNull { definition ->
            val value = inputs.firstOrNull { it.id == definition.id }?.value?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            "${definition.label}: $value"
        }
        return "${target.name} — ${parts.joinToString("; ")}"
    }
}
