package com.shohankhan.nexcalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.BitwiseTools
import com.shohankhan.nexcalc.math.MathException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BitwiseOperation(val label: String) {
    AND("AND"),
    OR("OR"),
    XOR("XOR"),
    NOT("NOT A"),
    SHIFT_LEFT("A << n"),
    SHIFT_RIGHT("A >> n"),
    SHIFT_RIGHT_UNSIGNED("A >>> n")
}

data class BaseUiState(
    val valueText: String = "255",
    val base: Int = 10,
    val aText: String = "12",
    val bText: String = "10",
    val operation: BitwiseOperation = BitwiseOperation.AND,
    val shiftText: String = "2",
    val outputs: List<Pair<String, String>> = emptyList(),
    val bitwise: List<Pair<String, String>> = emptyList(),
    val error: String? = null,
    val bitwiseError: String? = null
)

/** Number-system conversion and 64-bit two's-complement bitwise arithmetic. */
class BaseToolViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(BaseUiState())
    val state: StateFlow<BaseUiState> = _state.asStateFlow()

    init {
        convert()
    }

    fun setValue(text: String) {
        _state.update { it.copy(valueText = text) }
        convert()
    }

    fun setBase(base: Int) {
        _state.update { it.copy(base = base) }
        convert()
    }

    fun setA(text: String) = _state.update { it.copy(aText = text) }

    fun setB(text: String) = _state.update { it.copy(bText = text) }

    fun setOperation(operation: BitwiseOperation) = _state.update { it.copy(operation = operation) }

    fun setShift(text: String) = _state.update { it.copy(shiftText = text) }

    fun convert() {
        try {
            val value = BitwiseTools.fromRadix(_state.value.valueText, _state.value.base)
            val outputs = listOf(
                "Decimal (signed)" to value.toString(),
                "Decimal (unsigned)" to BitwiseTools.unsignedDecimal(value),
                "Binary" to binary(value),
                "Octal" to minimal(BitwiseTools.toOctal(value, 64), value),
                "Hexadecimal" to minimal(BitwiseTools.toHex(value, 64), value)
            )
            _state.update { it.copy(outputs = outputs, error = null) }
        } catch (error: MathException) {
            _state.update { it.copy(outputs = emptyList(), error = error.describe()) }
        }
    }

    fun applyBitwise() {
        try {
            val a = BitwiseTools.fromRadix(_state.value.aText, _state.value.base)
            val b = try {
                BitwiseTools.fromRadix(_state.value.bText, _state.value.base)
            } catch (error: MathException) {
                0L
            }
            val shift = _state.value.shiftText.trim().toIntOrNull() ?: 0
            val result = when (_state.value.operation) {
                BitwiseOperation.AND -> BitwiseTools.and(a, b)
                BitwiseOperation.OR -> BitwiseTools.or(a, b)
                BitwiseOperation.XOR -> BitwiseTools.xor(a, b)
                BitwiseOperation.NOT -> BitwiseTools.not(a)
                BitwiseOperation.SHIFT_LEFT -> BitwiseTools.shiftLeft(a, shift)
                BitwiseOperation.SHIFT_RIGHT -> BitwiseTools.shiftRight(a, shift)
                BitwiseOperation.SHIFT_RIGHT_UNSIGNED -> BitwiseTools.shiftRightUnsigned(a, shift)
            }
            val rows = listOf(
                "Decimal (signed)" to result.toString(),
                "Decimal (unsigned)" to BitwiseTools.unsignedDecimal(result),
                "Binary" to binary(result),
                "Octal" to minimal(BitwiseTools.toOctal(result, 64), result),
                "Hexadecimal" to minimal(BitwiseTools.toHex(result, 64), result)
            )
            _state.update { it.copy(bitwise = rows, bitwiseError = null) }
            viewModelScope.launch {
                container.historyRepository.record(
                    expression = "${_state.value.aText} ${_state.value.operation.label} ${if (_state.value.operation == BitwiseOperation.NOT || _state.value.operation.name.startsWith("SHIFT")) "" else _state.value.bText}".trim(),
                    result = result.toString(),
                    source = HistorySource.BASES
                )
            }
        } catch (error: MathException) {
            _state.update { it.copy(bitwise = emptyList(), bitwiseError = error.describe()) }
        } catch (error: IllegalArgumentException) {
            _state.update { it.copy(bitwise = emptyList(), bitwiseError = error.message ?: "Invalid shift amount.") }
        }
    }

    private fun binary(value: Long): String {
        val raw = BitwiseTools.toBinary(value, 64)
        if (value >= 0) {
            val trimmed = raw.trimStart('0')
            return if (trimmed.isEmpty()) "0" else trimmed
        }
        return raw
    }

    private fun minimal(rendered: String, value: Long): String {
        if (value < 0) return rendered
        val trimmed = rendered.trimStart('0')
        return if (trimmed.isEmpty()) "0" else trimmed
    }
}
