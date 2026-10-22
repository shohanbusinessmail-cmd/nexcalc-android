package com.shohankhan.nexcalc.feature.converters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.MathException
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import com.shohankhan.nexcalc.math.UnitCategory
import com.shohankhan.nexcalc.math.UnitRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConverterUiState(
    val categoryId: String = "",
    val fromUnitId: String = "",
    val toUnitId: String = "",
    val input: String = "1",
    val result: String = "",
    val error: String? = null,
    val format: NumberFormatOptions = NumberFormatOptions.Default,
    val saved: Boolean = false
) {
    val category: UnitCategory? get() = UnitRegistry.category(categoryId)
    val fromUnitName: String get() = category?.unit(fromUnitId)?.name.orEmpty()
    val toUnitName: String get() = category?.unit(toUnitId)?.name.orEmpty()
    val fromSymbol: String get() = category?.unit(fromUnitId)?.symbol.orEmpty()
    val toSymbol: String get() = category?.unit(toUnitId)?.symbol.orEmpty()
}

/**
 * Converter state machine. Conversions are pure and local; switching category resets the unit pair
 * to the category's base unit and its first alternative, so no stale cross-dimension pair survives.
 */
class ConverterViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(initialState(UnitRegistry.categories.first()))
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.collect { settings ->
                _state.update { it.copy(format = settings.numberFormat) }
                recalculate()
            }
        }
    }

    fun selectCategory(categoryId: String) {
        val category = UnitRegistry.category(categoryId) ?: return
        _state.update { current -> initialState(category).copy(input = current.input, format = current.format) }
        recalculate()
    }

    fun selectFrom(unitId: String) {
        _state.update { it.copy(fromUnitId = unitId, saved = false) }
        recalculate()
    }

    fun selectTo(unitId: String) {
        _state.update { it.copy(toUnitId = unitId, saved = false) }
        recalculate()
    }

    fun swap() {
        _state.update { it.copy(fromUnitId = it.toUnitId, toUnitId = it.fromUnitId, saved = false) }
        recalculate()
    }

    fun setInput(text: String) {
        _state.update { it.copy(input = text, saved = false) }
        recalculate()
    }

    fun saveToHistory() {
        val current = _state.value
        if (current.error != null || current.result.isBlank()) return
        viewModelScope.launch {
            container.historyRepository.record(
                expression = "${current.input} ${current.fromUnitName} → ${current.toUnitName}",
                result = "${current.result} ${current.toSymbol}".trim(),
                source = HistorySource.CONVERTER
            )
            _state.update { it.copy(saved = true) }
        }
    }

    private fun recalculate() {
        val current = _state.value
        val category = current.category
        if (category == null) {
            _state.update { it.copy(result = "", error = null) }
            return
        }
        val value = current.input.trim().replace(",", "").toDoubleOrNull()
        if (value == null) {
            _state.update { it.copy(result = "", error = "Enter a number to convert.") }
            return
        }
        try {
            val converted = UnitRegistry.convert(value, category.id, current.fromUnitId, current.toUnitId)
            _state.update {
                it.copy(result = NumberFormatter.format(converted, it.format), error = null)
            }
        } catch (error: MathException) {
            _state.update { it.copy(result = "", error = error.describe()) }
        }
    }

    private fun initialState(category: UnitCategory): ConverterUiState {
        val from = category.baseUnit
        val to = category.units.firstOrNull { it.id != from.id } ?: from
        return ConverterUiState(categoryId = category.id, fromUnitId = from.id, toUnitId = to.id)
    }
}
