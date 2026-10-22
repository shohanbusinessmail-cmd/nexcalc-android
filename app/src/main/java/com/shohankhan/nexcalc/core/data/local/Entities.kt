package com.shohankhan.nexcalc.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stored calculation. [source] identifies the surface that produced it so History can be
 * filtered: STANDARD, SCIENTIFIC, CONVERTER, MATRIX, STATISTICS, EQUATION, BASES, RESISTOR,
 * FORMULA:<id> or TOOL:<id>.
 */
@Entity(
    tableName = "history",
    indices = [Index("createdAt"), Index("source")]
)
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val detail: String? = null,
    val source: String,
    val createdAt: Long
)

/** Kinds of work that can be kept in the personal workspace. */
enum class SavedKind { EXPRESSION, FORMULA, CALCULATION, DATASET, EQUATION_SET }

@Entity(
    tableName = "saved",
    indices = [Index("kind"), Index("createdAt")]
)
data class SavedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val title: String,
    val payload: String,
    val note: String = "",
    val createdAt: Long
) {
    val savedKind: SavedKind get() = SavedKind.entries.firstOrNull { it.name == kind } ?: SavedKind.EXPRESSION
}

/** A favorited formula, unit category or Quick Solve tool. */
@Entity(tableName = "favorites")
data class FavoriteRecord(
    @PrimaryKey val key: String,
    val kind: String,
    val label: String,
    val subtitle: String,
    val createdAt: Long
)
