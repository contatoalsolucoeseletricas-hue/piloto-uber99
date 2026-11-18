package br.com.andreluis.pilotouber99

import android.content.Context

class PilotPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("piloto_uber99_prefs", Context.MODE_PRIVATE)

    var pilotEnabled: Boolean
        get() = prefs.getBoolean("pilot_enabled", false)
        set(value) = prefs.edit().putBoolean("pilot_enabled", value).apply()

    var calibrationMode: Boolean
        get() = prefs.getBoolean("calibration_mode", true)
        set(value) = prefs.edit().putBoolean("calibration_mode", value).apply()

    var minRatePerKm: Double
        get() = prefs.getFloat("min_rate_per_km", 1.40f).toDouble()
        set(value) = prefs.edit().putFloat("min_rate_per_km", value.toFloat()).apply()

    var cityFilterEnabled: Boolean
        get() = prefs.getBoolean("city_filter_enabled", true)
        set(value) = prefs.edit().putBoolean("city_filter_enabled", value).apply()

    var allowedCities: Set<String>
        get() = prefs.getStringSet("allowed_cities", CityFilter.ALL_CITIES.toSet()) ?: CityFilter.ALL_CITIES.toSet()
        set(value) = prefs.edit().putStringSet("allowed_cities", value).apply()

    var lastReadSummary: String
        get() = prefs.getString("last_read_summary", "R$ --/km · R$ --/h") ?: "R$ --/km · R$ --/h"
        set(value) = prefs.edit().putString("last_read_summary", value).apply()

    var acceptedCount: Int
        get() = prefs.getInt("accepted_count", 0)
        set(value) = prefs.edit().putInt("accepted_count", value).apply()

    var rejectedCount: Int
        get() = prefs.getInt("rejected_count", 0)
        set(value) = prefs.edit().putInt("rejected_count", value).apply()

    var estimatedEarnings: Double
        get() = prefs.getFloat("estimated_earnings", 0f).toDouble()
        set(value) = prefs.edit().putFloat("estimated_earnings", value.toFloat()).apply()

    var rateSum: Double
        get() = prefs.getFloat("rate_sum", 0f).toDouble()
        set(value) = prefs.edit().putFloat("rate_sum", value.toFloat()).apply()

    var rateSamples: Int
        get() = prefs.getInt("rate_samples", 0)
        set(value) = prefs.edit().putInt("rate_samples", value).apply()

    var pendingSwitchPackage: String?
        get() = prefs.getString("pending_switch_pkg", null)
        set(value) = prefs.edit().putString("pending_switch_pkg", value).apply()

    var pendingSwitchAction: String?
        get() = prefs.getString("pending_switch_action", null)
        set(value) = prefs.edit().putString("pending_switch_action", value).apply()

    fun recordOffer(accepted: Boolean, fareBrl: Double, ratePerKm: Double) {
        if (accepted) {
            acceptedCount += 1
            estimatedEarnings += fareBrl
        } else {
            rejectedCount += 1
        }
        rateSum += ratePerKm
        rateSamples += 1
    }

    fun averageRate(): Double = if (rateSamples > 0) rateSum / rateSamples else 0.0

    fun resetDay() {
        acceptedCount = 0
        rejectedCount = 0
        estimatedEarnings = 0.0
        rateSum = 0.0
        rateSamples = 0
    }
}
