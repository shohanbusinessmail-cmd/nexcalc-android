package com.shohankhan.nexcalc.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shohankhan.nexcalc.math.AngleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexcalc_settings")

/**
 * Single source of truth for user preferences. Persisted with DataStore, exposed as a cold Flow,
 * and safe against corrupted preference files (an [IOException] falls back to defaults rather than
 * crashing the app).
 */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val decimalPrecision = intPreferencesKey("decimal_precision")
        val significantDigits = intPreferencesKey("significant_digits")
        val useGrouping = booleanPreferencesKey("use_grouping")
        val decimalSeparator = stringPreferencesKey("decimal_separator")
        val groupingSeparator = stringPreferencesKey("grouping_separator")
        val showTrailingZeros = booleanPreferencesKey("show_trailing_zeros")
        val angleMode = stringPreferencesKey("angle_mode")
        val haptics = booleanPreferencesKey("haptics")
        val animations = booleanPreferencesKey("animations")
        val retentionDays = intPreferencesKey("history_retention_days")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences.write(transform(preferences.toSettings()))
        }
    }

    suspend fun reset() {
        context.settingsDataStore.edit { preferences -> preferences.clear() }
    }

    /** Applies a settings snapshot, used by JSON import. */
    suspend fun apply(settings: AppSettings) {
        context.settingsDataStore.edit { preferences -> preferences.write(settings) }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        themeMode = ThemeMode.fromKey(this[Keys.themeMode]),
        accent = AccentColor.fromKey(this[Keys.accent]),
        dynamicColor = this[Keys.dynamicColor] ?: false,
        decimalPrecision = (this[Keys.decimalPrecision] ?: 10).coerceIn(0, 15),
        significantDigits = (this[Keys.significantDigits] ?: 10).coerceIn(1, 15),
        useGrouping = this[Keys.useGrouping] ?: true,
        decimalSeparator = this[Keys.decimalSeparator]?.firstOrNull() ?: '.',
        groupingSeparator = this[Keys.groupingSeparator]?.firstOrNull() ?: ',',
        showTrailingZeros = this[Keys.showTrailingZeros] ?: false,
        angleMode = AngleMode.entries.firstOrNull { it.name == this[Keys.angleMode] } ?: AngleMode.DEGREES,
        haptics = this[Keys.haptics] ?: true,
        animations = this[Keys.animations] ?: true,
        historyRetentionDays = this[Keys.retentionDays] ?: 30
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.write(settings: AppSettings) {
        this[Keys.themeMode] = settings.themeMode.storageKey
        this[Keys.accent] = settings.accent.storageKey
        this[Keys.dynamicColor] = settings.dynamicColor
        this[Keys.decimalPrecision] = settings.decimalPrecision
        this[Keys.significantDigits] = settings.significantDigits
        this[Keys.useGrouping] = settings.useGrouping
        this[Keys.decimalSeparator] = settings.decimalSeparator.toString()
        this[Keys.groupingSeparator] = settings.groupingSeparator.toString()
        this[Keys.showTrailingZeros] = settings.showTrailingZeros
        this[Keys.angleMode] = settings.angleMode.name
        this[Keys.haptics] = settings.haptics
        this[Keys.animations] = settings.animations
        this[Keys.retentionDays] = settings.historyRetentionDays
    }
}
