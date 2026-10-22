package com.shohankhan.nexcalc.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.math.AngleMode
import com.shohankhan.nexcalc.math.CalculationOutcome
import com.shohankhan.nexcalc.math.Complex
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter

/** App-level snackbar host so screens can confirm actions without owning a Scaffold. */
val LocalSnackbarHost = staticCompositionLocalOf<androidx.compose.material3.SnackbarHostState?> { null }

/** Whether key presses should trigger haptics; provided from settings by the theme. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Copies text and confirms through the app-level snackbar. */
@Composable
fun rememberCopyAction(): (String) -> Unit {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val snackbar = LocalSnackbarHost.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val message = stringResource(R.string.action_copied)
    return { text ->
        clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
        if (snackbar != null) scope.launch { snackbar.showSnackbar(message) }
    }
}

/** Formats a value using the active settings; kept in one place so every screen agrees. */
@Composable
fun rememberFormatter(): (Double, Boolean) -> String {
    val options = LocalNumberFormat.current
    return { value, exact -> NumberFormatter.format(value, options, exact) }
}

/** Current display options, provided by [com.shohankhan.nexcalc.core.nav.NexCalcApp]. */
val LocalNumberFormat = staticCompositionLocalOf { NumberFormatOptions.Default }

val LocalSettings = staticCompositionLocalOf { AppSettings() }

@Composable
fun NexTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), content = actions)
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!title.isNullOrBlank()) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, tone: BannerTone = BannerTone.INFO) {
    val container = when (tone) {
        BannerTone.INFO -> MaterialTheme.colorScheme.secondaryContainer
        BannerTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        BannerTone.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (tone) {
        BannerTone.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        BannerTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        BannerTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(modifier.fillMaxWidth(), color = container, contentColor = content, shape = RoundedCornerShape(14.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

enum class BannerTone { INFO, WARNING, ERROR }

/**
 * Calculator key. Minimum touch target is 52dp with a 4.5:1 contrast pair from the design system,
 * and every key exposes a text label as its accessible name.
 */
@Composable
fun CalcKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CalcKeyStyle = CalcKeyStyle.NUMBER,
    description: String? = null,
    enabled: Boolean = true
) {
    val background = when (style) {
        CalcKeyStyle.NUMBER -> MaterialTheme.colorScheme.surfaceVariant
        CalcKeyStyle.OPERATOR -> MaterialTheme.colorScheme.secondaryContainer
        CalcKeyStyle.FUNCTION -> MaterialTheme.colorScheme.primaryContainer
        CalcKeyStyle.EQUALS -> MaterialTheme.colorScheme.primary
        CalcKeyStyle.MEMORY -> Color.Transparent
    }
    val foreground = when (style) {
        CalcKeyStyle.NUMBER -> MaterialTheme.colorScheme.onSurfaceVariant
        CalcKeyStyle.OPERATOR -> MaterialTheme.colorScheme.onSecondaryContainer
        CalcKeyStyle.FUNCTION -> MaterialTheme.colorScheme.onPrimaryContainer
        CalcKeyStyle.EQUALS -> MaterialTheme.colorScheme.onPrimary
        CalcKeyStyle.MEMORY -> MaterialTheme.colorScheme.primary
    }
    val haptics = LocalHapticsEnabled.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(if (style == CalcKeyStyle.MEMORY) 10.dp else 16.dp)
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .background(background, shape)
            .clickable(enabled = enabled, role = Role.Button) {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = description ?: label }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = if (style == CalcKeyStyle.MEMORY) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            fontWeight = if (style == CalcKeyStyle.EQUALS) FontWeight.SemiBold else FontWeight.Medium,
            color = foreground,
            maxLines = 1
        )
    }
}

enum class CalcKeyStyle { NUMBER, OPERATOR, FUNCTION, EQUALS, MEMORY }

data class CalcErrorUi(val message: String, val hint: String? = null, val position: Int? = null)

data class CalcOutcomeUi(
    val value: String = "",
    val exact: Boolean = false,
    val approximate: Boolean = false,
    val error: CalcErrorUi? = null
)

@Composable
fun ChoiceChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val supporting: (@Composable () -> Unit)? = if (errorText != null) {
        { Text(errorText, color = MaterialTheme.colorScheme.error) }
    } else {
        null
    }
    val placeholderContent: (@Composable () -> Unit)? = if (placeholder != null) {
        { Text(placeholder) }
    } else {
        null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        supportingText = supporting,
        placeholder = placeholderContent,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled) {
        Text(text)
    }
}

@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text)
    }
}

@Composable
fun ResultCard(title: String, value: String, modifier: Modifier = Modifier, note: String? = null) {
    ElevatedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FormattedValue(
    value: Double,
    exact: Boolean = false,
    modifier: Modifier = Modifier,
    decimals: Int? = null
) {
    val options = LocalNumberFormat.current
    val effective = if (decimals != null) options.copy(decimalPrecision = decimals) else options
    Text(NumberFormatter.format(value, effective, exact), modifier = modifier, fontFamily = FontFamily.Monospace)
}

/** Renders complex results consistently with the calculator result panel. */
@Composable
fun FormattedComplex(value: Complex, exact: Boolean = false, options: NumberFormatOptions = LocalNumberFormat.current) {
    Text(
        NumberFormatter.formatComplex(value, options, exact),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyLarge
    )
}

/** Angle mode chip used by the scientific screen; shows the current mode as text, never colour alone. */
@Composable
fun AngleModeChip(mode: AngleMode, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(
            when (mode) {
                AngleMode.DEGREES -> "DEG"
                AngleMode.RADIANS -> "RAD"
                AngleMode.GRADIANS -> "GRAD"
            },
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun DividerSpacer(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun IconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

/** Converts an engine outcome into the UI model, applying the active number format. */
fun CalculationOutcome.toUi(options: NumberFormatOptions): CalcOutcomeUi = when (this) {
    is CalculationOutcome.Success -> CalcOutcomeUi(
        value = NumberFormatter.formatComplex(value, options, exact),
        exact = exact,
        approximate = !exact
    )
    is CalculationOutcome.Failure -> CalcOutcomeUi(
        error = CalcErrorUi(message, hint, position)
    )
}

@Composable
fun LabeledContent(label: String, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        content()
    }
}

/** Compact expression chip for function and constant strips. */
@Composable
fun ExpressionChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
