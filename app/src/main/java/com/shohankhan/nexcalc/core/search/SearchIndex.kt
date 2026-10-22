package com.shohankhan.nexcalc.core.search

import com.shohankhan.nexcalc.math.FormulaCategory
import com.shohankhan.nexcalc.math.FormulaRegistry
import com.shohankhan.nexcalc.math.FunctionCatalog
import com.shohankhan.nexcalc.math.NumberFormatter
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.PhysicalConstants
import com.shohankhan.nexcalc.math.QuickToolRegistry
import com.shohankhan.nexcalc.math.UnitRegistry

/** Groups shown by the global search screen. */
enum class SearchGroup(val label: String) {
    FUNCTION("Functions"),
    FORMULA("Formulas"),
    TOOL("Quick Solve tools"),
    UNIT("Units"),
    CONSTANT("Constants"),
    APP_TOOL("Tools"),
    HISTORY("History"),
    SAVED("Saved items"),
    FAVORITE("Favorites")
}

/** Where a search result leads. Screens are resolved to navigation routes by the UI. */
sealed interface SearchTarget {
    data class Formula(val formulaId: String) : SearchTarget
    data class QuickTool(val toolId: String) : SearchTarget
    data class UnitCategory(val categoryId: String) : SearchTarget
    data class Screen(val screenId: String) : SearchTarget
}

data class SearchHit(
    val key: String,
    val title: String,
    val subtitle: String,
    val group: SearchGroup,
    val target: SearchTarget,
    val score: Int
)

/**
 * Offline, deterministic search over the entire catalogue: engine functions, formulas, Quick Solve
 * tools, unit categories and individual units, physical constants and the Tools workspaces.
 *
 * Ranking is intentionally simple and explainable: an exact title match outranks a prefix, which
 * outranks a substring, which outranks a description match. Multiple words must all match.
 */
class SearchIndex {

    private data class Entry(
        val key: String,
        val title: String,
        val subtitle: String,
        val keywords: String,
        val group: SearchGroup,
        val target: SearchTarget
    )

    private val entries: List<Entry> = buildList {
        for (function in FunctionCatalog.functions) {
            add(
                Entry(
                    key = "function:${function.name}",
                    title = function.label,
                    subtitle = "${function.signature} · ${function.category.label}",
                    keywords = "${function.name} ${function.description}",
                    group = SearchGroup.FUNCTION,
                    target = SearchTarget.Screen("scientific")
                )
            )
        }
        for (formula in FormulaRegistry.all) {
            add(
                Entry(
                    key = "formula:${formula.id}",
                    title = formula.name,
                    subtitle = "${formula.category.label} · ${formula.subcategory}",
                    keywords = "${formula.id} ${formula.notation} ${formula.description}",
                    group = SearchGroup.FORMULA,
                    target = SearchTarget.Formula(formula.id)
                )
            )
        }
        for (tool in QuickToolRegistry.all) {
            add(
                Entry(
                    key = "tool:${tool.id}",
                    title = tool.name,
                    subtitle = tool.category.label,
                    keywords = "${tool.id} ${tool.description}",
                    group = SearchGroup.TOOL,
                    target = SearchTarget.QuickTool(tool.id)
                )
            )
        }
        for (category in UnitRegistry.categories) {
            add(
                Entry(
                    key = "unitcat:${category.id}",
                    title = category.name,
                    subtitle = "${category.units.size} units",
                    keywords = "${category.id} ${category.description} " + category.units.joinToString(" ") { it.name },
                    group = SearchGroup.UNIT,
                    target = SearchTarget.UnitCategory(category.id)
                )
            )
            for (unit in category.units) {
                add(
                    Entry(
                        key = "unit:${category.id}:${unit.id}",
                        title = "${unit.name} (${unit.symbol})",
                        subtitle = category.name,
                        keywords = "${unit.id} ${category.name}",
                        group = SearchGroup.UNIT,
                        target = SearchTarget.UnitCategory(category.id)
                    )
                )
            }
        }
        for (constant in PhysicalConstants.all) {
            add(
                Entry(
                    key = "constant:${constant.id}",
                    title = constant.name,
                    subtitle = "${constant.symbol} = ${NumberFormatter.format(constant.value, NumberFormatOptions(decimalPrecision = 8, useGrouping = false))} ${constant.unit}",
                    keywords = "${constant.id} ${constant.symbol} ${constant.description}",
                    group = SearchGroup.CONSTANT,
                    target = SearchTarget.Screen("constants")
                )
            )
        }
        for (tool in appTools) {
            add(tool)
        }
    }

    fun search(query: String, limit: Int = 60): List<SearchHit> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val hits = ArrayList<SearchHit>(64)
        for (entry in entries) {
            var total = 0
            var matchedAll = true
            for (token in tokens) {
                val score = scoreOf(entry, token)
                if (score == 0) {
                    matchedAll = false
                    break
                }
                total += score
            }
            if (matchedAll) {
                hits += SearchHit(entry.key, entry.title, entry.subtitle, entry.group, entry.target, total)
            }
        }
        return hits.sortedWith(compareByDescending<SearchHit> { it.score }.thenBy { it.title }).take(limit)
    }

    private fun scoreOf(entry: Entry, token: String): Int {
        val title = entry.title.lowercase()
        val subtitle = entry.subtitle.lowercase()
        val keywords = entry.keywords.lowercase()
        return when {
            title == token -> 1_000
            title.startsWith(token) -> 700
            title.contains(token) -> 450
            entry.key.lowercase().substringAfter(':').startsWith(token) -> 380
            subtitle.contains(token) -> 220
            keywords.contains(token) -> 120
            else -> 0
        }
    }

    val entryCount: Int get() = entries.size

    val formulaCategories: List<FormulaCategory> get() = FormulaRegistry.categories

    private companion object {
        val appTools: List<Entry> = listOf(
            Entry("screen:matrix", "Matrix & vector workspace", "Tools", "matrix determinant inverse transpose solve vector", SearchGroup.APP_TOOL, SearchTarget.Screen("matrix")),
            Entry("screen:statistics", "Statistics workspace", "Tools", "statistics dataset mean median regression csv frequency", SearchGroup.APP_TOOL, SearchTarget.Screen("statistics")),
            Entry("screen:equations", "Equation workspace", "Tools", "equation system polynomial roots newton bisection secant solver", SearchGroup.APP_TOOL, SearchTarget.Screen("equations")),
            Entry("screen:bases", "Number systems & bitwise", "Tools", "binary octal hexadecimal bitwise shift two's complement base", SearchGroup.APP_TOOL, SearchTarget.Screen("bases")),
            Entry("screen:constants", "Constants & identities", "Tools", "physical constants identities reference CODATA", SearchGroup.APP_TOOL, SearchTarget.Screen("constants")),
            Entry("screen:resistor", "Resistor colour code", "Tools", "resistor colour color band ohm tolerance", SearchGroup.APP_TOOL, SearchTarget.Screen("resistor")),
            Entry("screen:saved", "Saved workspace", "Tools", "saved expressions formulas datasets notes", SearchGroup.APP_TOOL, SearchTarget.Screen("saved")),
            Entry("screen:resistor-note", "Calculation notes", "Tools", "notes annotations saved calculations", SearchGroup.APP_TOOL, SearchTarget.Screen("saved"))
        )
    }
}
