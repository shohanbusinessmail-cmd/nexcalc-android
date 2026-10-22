package com.shohankhan.nexcalc.core.data

import com.shohankhan.nexcalc.core.data.local.FavoriteRecord
import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import com.shohankhan.nexcalc.core.data.local.NexCalcDatabase
import com.shohankhan.nexcalc.core.data.local.SavedRecord
import com.shohankhan.nexcalc.math.AngleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Result of importing a NEXCALC JSON export. */
sealed interface ImportOutcome {
    data class Success(val records: Int) : ImportOutcome
    data class Failure(val message: String) : ImportOutcome
}

@Serializable
data class HistoryPayload(
    val expression: String,
    val result: String,
    val detail: String? = null,
    val source: String,
    val createdAt: Long
)

@Serializable
data class SavedPayload(
    val kind: String,
    val title: String,
    val payload: String,
    val note: String = "",
    val createdAt: Long
)

@Serializable
data class FavoritePayload(
    val key: String,
    val kind: String,
    val label: String,
    val subtitle: String,
    val createdAt: Long
)

@Serializable
data class SettingsPayload(
    val themeMode: String = "system",
    val accent: String = "blue",
    val dynamicColor: Boolean = false,
    val decimalPrecision: Int = 10,
    val significantDigits: Int = 10,
    val useGrouping: Boolean = true,
    val decimalSeparator: String = ".",
    val groupingSeparator: String = ",",
    val showTrailingZeros: Boolean = false,
    val angleMode: String = "DEGREES",
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val historyRetentionDays: Int = 30
)

@Serializable
data class ExportBundle(
    val app: String = "NEXCALC",
    val formatVersion: Int = 1,
    val exportedAt: Long = 0L,
    val settings: SettingsPayload? = null,
    val history: List<HistoryPayload> = emptyList(),
    val saved: List<SavedPayload> = emptyList(),
    val favorites: List<FavoritePayload> = emptyList()
)

/**
 * Explicit JSON export/import used by Settings. Nothing leaves the device unless the user picks a
 * destination through the system file picker; the format carries no identifiers, no device data and
 * no credentials.
 */
class DataTransfer(
    private val database: NexCalcDatabase,
    private val preferences: PreferencesRepository
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val settings = preferences.settings.first()
        val history = database.historyDao().latestForExport()
        val saved = database.savedDao().observeAll().first()
        val favorites = database.favoriteDao().all()
        val bundle = ExportBundle(
            exportedAt = System.currentTimeMillis(),
            settings = SettingsPayload(
                themeMode = settings.themeMode.storageKey,
                accent = settings.accent.storageKey,
                dynamicColor = settings.dynamicColor,
                decimalPrecision = settings.decimalPrecision,
                significantDigits = settings.significantDigits,
                useGrouping = settings.useGrouping,
                decimalSeparator = settings.decimalSeparator.toString(),
                groupingSeparator = settings.groupingSeparator.toString(),
                showTrailingZeros = settings.showTrailingZeros,
                angleMode = settings.angleMode.name,
                haptics = settings.haptics,
                animations = settings.animations,
                historyRetentionDays = settings.historyRetentionDays
            ),
            history = history.map {
                HistoryPayload(it.expression, it.result, it.detail, it.source, it.createdAt)
            },
            saved = saved.map { SavedPayload(it.kind, it.title, it.payload, it.note, it.createdAt) },
            favorites = favorites.map { FavoritePayload(it.key, it.kind, it.label, it.subtitle, it.createdAt) }
        )
        json.encodeToString(ExportBundle.serializer(), bundle)
    }

    suspend fun importJson(text: String): ImportOutcome = withContext(Dispatchers.IO) {
        if (text.length > MAX_IMPORT_CHARACTERS) {
            return@withContext ImportOutcome.Failure("The file is larger than the 4 MB import limit.")
        }
        val bundle = try {
            json.decodeFromString(ExportBundle.serializer(), text)
        } catch (error: Exception) {
            return@withContext ImportOutcome.Failure("The selected file is not a valid NEXCALC export.")
        }
        if (bundle.app != "NEXCALC") {
            return@withContext ImportOutcome.Failure("The file was not produced by NEXCALC.")
        }
        if (bundle.formatVersion > FORMAT_VERSION) {
            return@withContext ImportOutcome.Failure("This export comes from a newer NEXCALC version.")
        }
        val total = bundle.history.size + bundle.saved.size + bundle.favorites.size
        if (total > MAX_IMPORT_RECORDS) {
            return@withContext ImportOutcome.Failure("The export contains more than $MAX_IMPORT_RECORDS records.")
        }
        try {
            bundle.settings?.let { preferences.apply(it.toSettings()) }
            database.historyDao().insertAll(
                bundle.history
                    .filter { it.expression.isNotBlank() && it.result.isNotBlank() && it.source.isNotBlank() }
                    .map { HistoryRecord(expression = it.expression, result = it.result, detail = it.detail, source = it.source, createdAt = it.createdAt) }
            )
            database.savedDao().insertAll(
                bundle.saved
                    .filter { it.kind.isNotBlank() && it.title.isNotBlank() }
                    .map { SavedRecord(kind = it.kind, title = it.title, payload = it.payload, note = it.note, createdAt = it.createdAt) }
            )
            database.favoriteDao().insertAll(
                bundle.favorites
                    .filter { it.key.isNotBlank() && it.label.isNotBlank() }
                    .map { FavoriteRecord(key = it.key, kind = it.kind, label = it.label, subtitle = it.subtitle, createdAt = it.createdAt) }
            )
            ImportOutcome.Success(total)
        } catch (error: Exception) {
            ImportOutcome.Failure(error.message ?: "The import could not be completed.")
        }
    }

    private fun SettingsPayload.toSettings(): AppSettings = AppSettings(
        themeMode = ThemeMode.fromKey(themeMode),
        accent = AccentColor.fromKey(accent),
        dynamicColor = dynamicColor,
        decimalPrecision = decimalPrecision.coerceIn(0, 15),
        significantDigits = significantDigits.coerceIn(1, 15),
        useGrouping = useGrouping,
        decimalSeparator = decimalSeparator.firstOrNull() ?: '.',
        groupingSeparator = groupingSeparator.firstOrNull() ?: ',',
        showTrailingZeros = showTrailingZeros,
        angleMode = AngleMode.entries.firstOrNull { it.name == angleMode } ?: AngleMode.DEGREES,
        haptics = haptics,
        animations = animations,
        historyRetentionDays = if (historyRetentionDays < 0) -1 else historyRetentionDays.coerceIn(1, 3650)
    )

    companion object {
        const val FORMAT_VERSION = 1
        const val MAX_IMPORT_CHARACTERS = 4_000_000
        const val MAX_IMPORT_RECORDS = 20_000
        const val EXPORT_FILE_NAME = "nexcalc-backup.json"
    }
}
