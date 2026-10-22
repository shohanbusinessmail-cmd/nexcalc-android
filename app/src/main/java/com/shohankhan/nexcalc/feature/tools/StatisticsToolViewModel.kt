package com.shohankhan.nexcalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import com.shohankhan.nexcalc.math.Statistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val datasetText: String = "2, 4, 4, 4, 5, 5, 7, 9",
    val xText: String = "1, 2, 3, 4, 5",
    val yText: String = "2, 4, 5, 4, 5",
    val summary: List<Pair<String, String>> = emptyList(),
    val frequency: List<Pair<String, String>> = emptyList(),
    val regression: List<Pair<String, String>> = emptyList(),
    val error: String? = null,
    val regressionError: String? = null,
    val savedName: String = "",
    val saved: Boolean = false
)

/** Statistics workspace with descriptive measures, frequency table, regression and CSV export. */
class StatisticsToolViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state.asStateFlow()

    private val format = MutableStateFlow(NumberFormatOptions.Default)
    private var lastDataset: List<Double> = emptyList()

    init {
        viewModelScope.launch { container.settings.collect { format.value = it.numberFormat } }
    }

    fun setDataset(text: String) = _state.update { it.copy(datasetText = text, saved = false) }
    fun setX(text: String) = _state.update { it.copy(xText = text, regressionError = null) }
    fun setY(text: String) = _state.update { it.copy(yText = text, regressionError = null) }
    fun setSavedName(name: String) = _state.update { it.copy(savedName = name, saved = false) }

    fun describe() {
        try {
            val values = Statistics.parseDataset(_state.value.datasetText)
            val stats = Statistics.describe(values)
            val options = format.value
            val summary = listOf(
                "Count" to stats.count.toString(),
                "Sum" to NumberFormatter.format(stats.sum, options),
                "Mean" to NumberFormatter.format(stats.mean, options),
                "Median" to NumberFormatter.format(stats.median, options),
                "Mode" to if (stats.modes.isEmpty()) "none" else stats.modes.joinToString(", ") { NumberFormatter.format(it, options) },
                "Minimum" to NumberFormatter.format(stats.minimum, options),
                "Maximum" to NumberFormatter.format(stats.maximum, options),
                "Range" to NumberFormatter.format(stats.range, options),
                "Q1 (type 7)" to NumberFormatter.format(stats.firstQuartile, options),
                "Q3 (type 7)" to NumberFormatter.format(stats.thirdQuartile, options),
                "IQR" to NumberFormatter.format(stats.interquartileRange, options),
                "Variance (population)" to NumberFormatter.format(stats.variancePopulation, options),
                "Variance (sample)" to (stats.varianceSample?.let { NumberFormatter.format(it, options) } ?: "not defined for n = 1"),
                "SD (population)" to NumberFormatter.format(stats.standardDeviationPopulation, options),
                "SD (sample)" to (stats.standardDeviationSample?.let { NumberFormatter.format(it, options) } ?: "not defined for n = 1"),
                "Mean absolute deviation" to NumberFormatter.format(Statistics.meanAbsoluteDeviation(values), options)
            )
            val frequency = Statistics.frequencyTable(values).map { entry ->
                NumberFormatter.format(entry.value, options) to "${entry.frequency} (${NumberFormatter.format(entry.relativeFrequency * 100, options)}%)"
            }
            lastDataset = values
            _state.update { it.copy(summary = summary, frequency = frequency, error = null, saved = false) }
            viewModelScope.launch {
                container.historyRepository.record(
                    expression = "Statistics of ${values.size} values",
                    result = "mean ${NumberFormatter.format(stats.mean, options)}, median ${NumberFormatter.format(stats.median, options)}",
                    source = HistorySource.STATISTICS
                )
            }
        } catch (error: Exception) {
            _state.update { it.copy(summary = emptyList(), frequency = emptyList(), error = error.message ?: "The dataset could not be read.") }
        }
    }

    fun regress() {
        try {
            val x = Statistics.parseDataset(_state.value.xText)
            val y = Statistics.parseDataset(_state.value.yText)
            if (x.size != y.size) throw IllegalArgumentException("x and y must contain the same number of values.")
            val regression = Statistics.linearRegression(x, y)
            val options = format.value
            val rows = listOf(
                "Pairs" to regression.count.toString(),
                "Slope" to NumberFormatter.format(regression.slope, options),
                "Intercept" to NumberFormatter.format(regression.intercept, options),
                "Correlation r" to NumberFormatter.format(regression.correlation, options),
                "R²" to NumberFormatter.format(regression.rSquared, options),
                "Slope SE" to (regression.slopeStandardError?.let { NumberFormatter.format(it, options) } ?: "not defined for n = 2"),
                "Intercept SE" to (regression.interceptStandardError?.let { NumberFormatter.format(it, options) } ?: "not defined for n = 2")
            )
            _state.update { it.copy(regression = rows, regressionError = null) }
            viewModelScope.launch {
                container.historyRepository.record(
                    expression = "Linear regression of ${x.size} pairs",
                    result = "y = ${NumberFormatter.format(regression.slope, options)}x + ${NumberFormatter.format(regression.intercept, options)}",
                    source = HistorySource.STATISTICS
                )
            }
        } catch (error: Exception) {
            _state.update { it.copy(regression = emptyList(), regressionError = error.message ?: "The paired data could not be read.") }
        }
    }

    fun saveDataset() {
        if (lastDataset.isEmpty()) return
        val name = _state.value.savedName.ifBlank { "Dataset (${lastDataset.size} values)" }
        val payload = lastDataset.joinToString(", ")
        viewModelScope.launch {
            container.savedRepository.save(SavedKind.DATASET, name, payload)
            _state.update { it.copy(saved = true) }
        }
    }

    fun csvText(): String {
        val options = format.value
        val builder = StringBuilder("value,frequency,relative_frequency\n")
        _state.value.frequency.forEach { (value, count) ->
            val frequency = count.substringBefore(" ")
            val relative = count.substringAfter("(").substringBefore(")").removeSuffix("%")
            builder.append(value).append(',').append(frequency).append(',').append(relative).append('\n')
        }
        if (_state.value.frequency.isEmpty() && lastDataset.isNotEmpty()) {
            lastDataset.forEach { builder.append(NumberFormatter.format(it, options)).append(",1,100\n") }
        }
        return builder.toString()
    }
}
