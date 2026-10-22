package com.shohankhan.nexcalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.MathException
import com.shohankhan.nexcalc.math.ResistorCalculator
import com.shohankhan.nexcalc.math.ResistorColor
import com.shohankhan.nexcalc.math.ResistorResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResistorUiState(
    val bandCount: Int = 4,
    val bands: List<ResistorColor> = defaultBands(4),
    val result: ResistorResult? = null,
    val error: String? = null
) {
    companion object {
        fun defaultBands(count: Int): List<ResistorColor> = when (count) {
            4 -> listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.GOLD)
            5 -> listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.BROWN)
            else -> listOf(
                ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.BLACK, ResistorColor.BLACK,
                ResistorColor.BROWN, ResistorColor.BROWN
            )
        }
    }
}

/** Resistor colour decoder for 4, 5 and 6 band components. */
class ResistorToolViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ResistorUiState())
    val state: StateFlow<ResistorUiState> = _state.asStateFlow()

    init {
        decode()
    }

    fun setBandCount(count: Int) {
        _state.value = ResistorUiState(bandCount = count, bands = ResistorUiState.defaultBands(count))
        decode()
    }

    fun setBand(index: Int, color: ResistorColor) {
        _state.update { current ->
            current.copy(bands = current.bands.mapIndexed { position, existing -> if (position == index) color else existing })
        }
        decode()
    }

    private fun decode() {
        try {
            val result = ResistorCalculator.decode(_state.value.bands)
            _state.update { it.copy(result = result, error = null) }
        } catch (error: MathException) {
            _state.update { it.copy(result = null, error = error.describe()) }
        }
    }

    fun saveToHistory() {
        val result = _state.value.result ?: return
        viewModelScope.launch {
            container.historyRepository.record(
                expression = _state.value.bands.joinToString("–") { it.label } + " (${_state.value.bandCount} band)",
                result = result.explanation,
                source = HistorySource.RESISTOR
            )
        }
    }
}
