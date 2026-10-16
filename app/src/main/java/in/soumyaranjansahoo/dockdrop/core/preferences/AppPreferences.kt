package `in`.soumyaranjansahoo.dockdrop.core.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

class AppPreferences(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences("dockdrop_prefs", Context.MODE_PRIVATE)
    )

    private val _hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        }.getOrDefault(ThemeMode.DARK)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _reduceMotion = MutableStateFlow(
        prefs.getBoolean(KEY_REDUCE_MOTION, false)
    )
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _defaultMinBatteryPct = MutableStateFlow(
        prefs.getInt(KEY_DEFAULT_MIN_BATTERY, 20)
    )
    val defaultMinBatteryPct: StateFlow<Int> = _defaultMinBatteryPct.asStateFlow()

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setReduceMotion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()
        _reduceMotion.value = enabled
    }

    fun setDefaultMinBatteryPct(pct: Int) {
        prefs.edit().putInt(KEY_DEFAULT_MIN_BATTERY, pct).apply()
        _defaultMinBatteryPct.value = pct
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "has_completed_onboarding"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_DEFAULT_MIN_BATTERY = "default_min_battery"
    }
}
