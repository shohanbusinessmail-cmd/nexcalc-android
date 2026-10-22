package com.shohankhan.nexcalc.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.AngleModeChip
import com.shohankhan.nexcalc.core.design.CalcKey
import com.shohankhan.nexcalc.core.design.CalcKeyStyle
import com.shohankhan.nexcalc.core.design.ExpressionChip
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.LocalSnackbarHost
import com.shohankhan.nexcalc.core.design.MonospaceResult
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.math.FunctionCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Standard and Scientific share this screen; [scientific] enables the function strip and browser. */
@Composable
fun CalculatorScreen(scientific: Boolean) {
    val source = if (scientific) HistorySource.SCIENTIFIC else HistorySource.STANDARD
    val viewModel = containerViewModel { CalculatorViewModel(it, source) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var showFunctions by rememberSaveable { mutableStateOf(false) }
    val copy = rememberCopyAction()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val needResultMessage = stringResource(R.string.memory_need_result)
    val memoryEmptyMessage = stringResource(R.string.memory_empty)
    val savedMessage = stringResource(R.string.formula_saved)

    fun insert(token: String) {
        input = input.insertAtCursor(token)
    }

    fun requireAns(): Double? {
        val ans = state.ans
        if (ans == null || !ans.isReal) {
            if (snackbar != null) scope.launch { snackbar.showSnackbar(needResultMessage) }
            return null
        }
        return ans.re
    }

    LaunchedEffect(input.text) {
        delay(120)
        viewModel.preview(input.text)
    }

    val title = stringResource(if (scientific) R.string.scientific_title else R.string.standard_title)
    val subtitle = stringResource(if (scientific) R.string.scientific_subtitle else R.string.standard_subtitle)

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = title,
            subtitle = subtitle,
            actions = {
                if (scientific) {
                    AngleModeChip(state.angleMode) { viewModel.cycleAngleMode() }
                    IconAction(
                        icon = Icons.Default.Search,
                        contentDescription = stringResource(R.string.scientific_function_browser)
                    ) { showFunctions = true }
                }
                IconAction(
                    icon = Icons.Default.Save,
                    contentDescription = stringResource(R.string.action_save_expression)
                ) {
                    viewModel.saveExpression(input.text)
                    if (snackbar != null) scope.launch { snackbar.showSnackbar(savedMessage) }
                }
            }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_expression)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.evaluate(input.text) }),
                trailingIcon = {
                    IconButton(onClick = { input = input.backspace() }) {
                        Icon(Icons.Default.Backspace, contentDescription = stringResource(R.string.action_backspace))
                    }
                },
                shape = MaterialTheme.shapes.medium
            )

            CalculatorDisplay(
                expression = input.text,
                value = state.outcome?.value.orEmpty(),
                error = state.outcome?.error,
                exact = state.outcome?.exact == true,
                approximate = state.outcome?.approximate == true,
                memorySet = state.memorySet,
                onCopy = { copy(it) }
            )

            if (scientific) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickFunctions) { entry ->
                        ExpressionChip(entry.first) { insert(entry.second) }
                    }
                }
            }

            MemoryRow(
                onMemoryClear = { viewModel.clearMemory() },
                onMemoryRecall = {
                    val text = viewModel.memoryText()
                    if (text == null) {
                        if (snackbar != null) scope.launch { snackbar.showSnackbar(memoryEmptyMessage) }
                    } else {
                        insert(text)
                    }
                },
                onMemoryAdd = { requireAns()?.let { viewModel.addToMemory(com.shohankhan.nexcalc.math.Complex(it)) } },
                onMemorySubtract = { requireAns()?.let { viewModel.subtractFromMemory(com.shohankhan.nexcalc.math.Complex(it)) } },
                onMemoryStore = { requireAns()?.let { viewModel.storeMemory(com.shohankhan.nexcalc.math.Complex(it)) } }
            )

            Keypad(
                onInsert = { insert(it) },
                onEquals = { viewModel.evaluate(input.text) },
                onClearEntry = { input = TextFieldValue("") },
                onClearAll = {
                    input = TextFieldValue("")
                    viewModel.clearResult()
                },
                onBackspace = { input = input.backspace() },
                onNegate = { input = input.negate() }
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showFunctions) {
        FunctionBrowserDialog(
            onDismiss = { showFunctions = false },
            onInsert = { token ->
                insert(token)
                showFunctions = false
            }
        )
    }
}

private val quickFunctions: List<Pair<String, String>> = listOf(
    "sin" to "sin(",
    "cos" to "cos(",
    "tan" to "tan(",
    "asin" to "asin(",
    "acos" to "acos(",
    "atan" to "atan(",
    "ln" to "ln(",
    "log" to "log(",
    "eˣ" to "exp(",
    "√" to "sqrt(",
    "x²" to "^2",
    "xʸ" to "^",
    "n!" to "!",
    "π" to "pi",
    "e" to "e",
    "Ans" to "ans",
    "(" to "(",
    ")" to ")",
    "%" to "%"
)

@Composable
private fun CalculatorDisplay(
    expression: String,
    value: String,
    error: com.shohankhan.nexcalc.core.design.CalcErrorUi?,
    exact: Boolean,
    approximate: Boolean,
    memorySet: Boolean,
    onCopy: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    expression.ifBlank { stringResource(R.string.standard_hint) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (memorySet) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            stringResource(R.string.memory_indicator),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (error != null) error.message else value.ifBlank { "—" },
                    style = MonospaceResult,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (error == null && value.isNotBlank()) {
                    IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy)) { onCopy(value) }
                }
            }
            if (error != null && error.hint != null) {
                Text(error.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (error != null && error.position != null) {
                Text(
                    stringResource(R.string.error_position, error.position + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (error == null) {
                Text(
                    text = when {
                        exact -> stringResource(R.string.label_exact)
                        approximate -> stringResource(R.string.label_approximate)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MemoryRow(
    onMemoryClear: () -> Unit,
    onMemoryRecall: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onMemoryStore: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CalcKey(stringResource(R.string.memory_clear), onMemoryClear, Modifier.weight(1f), CalcKeyStyle.MEMORY)
        CalcKey(stringResource(R.string.memory_recall), onMemoryRecall, Modifier.weight(1f), CalcKeyStyle.MEMORY)
        CalcKey(stringResource(R.string.memory_add), onMemoryAdd, Modifier.weight(1f), CalcKeyStyle.MEMORY)
        CalcKey(stringResource(R.string.memory_subtract), onMemorySubtract, Modifier.weight(1f), CalcKeyStyle.MEMORY)
        CalcKey(stringResource(R.string.memory_store), onMemoryStore, Modifier.weight(1f), CalcKeyStyle.MEMORY)
    }
}

@Composable
private fun Keypad(
    onInsert: (String) -> Unit,
    onEquals: () -> Unit,
    onClearEntry: () -> Unit,
    onClearAll: () -> Unit,
    onBackspace: () -> Unit,
    onNegate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KeyRow {
            CalcKey(stringResource(R.string.action_clear_all), onClearAll, Modifier.weight(1f), CalcKeyStyle.OPERATOR)
            CalcKey(stringResource(R.string.action_clear_entry), onClearEntry, Modifier.weight(1f), CalcKeyStyle.OPERATOR)
            CalcKey("⌫", onBackspace, Modifier.weight(1f), CalcKeyStyle.OPERATOR, description = stringResource(R.string.action_backspace))
            CalcKey("÷", { onInsert("/") }, Modifier.weight(1f), CalcKeyStyle.OPERATOR)
        }
        KeyRow {
            CalcKey("7", { onInsert("7") }, Modifier.weight(1f))
            CalcKey("8", { onInsert("8") }, Modifier.weight(1f))
            CalcKey("9", { onInsert("9") }, Modifier.weight(1f))
            CalcKey("×", { onInsert("*") }, Modifier.weight(1f), CalcKeyStyle.OPERATOR)
        }
        KeyRow {
            CalcKey("4", { onInsert("4") }, Modifier.weight(1f))
            CalcKey("5", { onInsert("5") }, Modifier.weight(1f))
            CalcKey("6", { onInsert("6") }, Modifier.weight(1f))
            CalcKey("−", { onInsert("-") }, Modifier.weight(1f), CalcKeyStyle.OPERATOR)
        }
        KeyRow {
            CalcKey("1", { onInsert("1") }, Modifier.weight(1f))
            CalcKey("2", { onInsert("2") }, Modifier.weight(1f))
            CalcKey("3", { onInsert("3") }, Modifier.weight(1f))
            CalcKey("+", { onInsert("+") }, Modifier.weight(1f), CalcKeyStyle.OPERATOR)
        }
        KeyRow {
            CalcKey("(−)", onNegate, Modifier.weight(1f), CalcKeyStyle.FUNCTION)
            CalcKey("0", { onInsert("0") }, Modifier.weight(1f))
            CalcKey(".", { onInsert(".") }, Modifier.weight(1f))
            CalcKey("=", onEquals, Modifier.weight(1f), CalcKeyStyle.EQUALS)
        }
    }
}

@Composable
private fun KeyRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun FunctionBrowserDialog(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val functions = remember(query) {
        if (query.isBlank()) {
            FunctionCatalog.functions
        } else {
            val needle = query.lowercase()
            FunctionCatalog.functions.filter {
                it.name.contains(needle) || it.label.lowercase().contains(needle) || it.description.lowercase().contains(needle)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        title = { Text(stringResource(R.string.scientific_function_browser)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.scientific_search_functions)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(functions, key = { it.name }) { function ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onInsert(function.insertText) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(function.signature, style = MaterialTheme.typography.titleSmall)
                            Text(
                                function.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}

/** Inserts [token] at the caret, replacing any selection. */
fun TextFieldValue.insertAtCursor(token: String): TextFieldValue {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(0, text.length)
    val updated = text.replaceRange(start, end, token)
    return copy(text = updated, selection = TextRange(start + token.length))
}

/** Deletes the selection, or the character before the caret. */
fun TextFieldValue.backspace(): TextFieldValue {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(0, text.length)
    if (start != end) {
        return copy(text = text.removeRange(start, end), selection = TextRange(start))
    }
    if (start == 0) return this
    return copy(text = text.removeRange(start - 1, start), selection = TextRange(start - 1))
}

/** Toggles the sign of a plain numeric expression, otherwise wraps it in a negation. */
fun TextFieldValue.negate(): TextFieldValue {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return insertAtCursor("-")
    val isPlainNumber = Regex("^-?\\d+(\\.\\d+)?$").matches(trimmed)
    if (isPlainNumber) {
        val toggled = if (trimmed.startsWith("-")) trimmed.removePrefix("-") else "-$trimmed"
        return copy(text = toggled, selection = TextRange(toggled.length))
    }
    val wrapped = "-($trimmed)"
    return copy(text = wrapped, selection = TextRange(wrapped.length))
}
