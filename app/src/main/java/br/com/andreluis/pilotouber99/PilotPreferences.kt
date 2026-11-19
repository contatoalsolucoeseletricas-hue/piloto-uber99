package br.com.andreluis.pilotouber99

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RideHistoryEntry(
    val timestamp: String,
    val appName: String,
    val fareBrl: Double,
    val totalKm: Double,
    val totalMinutes: Int,
    val ratePerKm: Double,
    val ratePerHour: Double,
    val accepted: Boolean,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp)
        put("appName", appName)
        put("fareBrl", fareBrl)
        put("totalKm", totalKm)
        put("totalMinutes", totalMinutes)
        put("ratePerKm", ratePerKm)
        put("ratePerHour", ratePerHour)
        put("accepted", accepted)
        put("reason", reason)
    }

    companion object {
        fun fromJson(obj: JSONObject): RideHistoryEntry = RideHistoryEntry(
            timestamp = obj.optString("timestamp", ""),
            appName = obj.optString("appName", ""),
            fareBrl = obj.optDouble("fareBrl", 0.0),
            totalKm = obj.optDouble("totalKm", 0.0),
            totalMinutes = obj.optInt("totalMinutes", 0),
            ratePerKm = obj.optDouble("ratePerKm", 0.0),
            ratePerHour = obj.optDouble("ratePerHour", 0.0),
            accepted = obj.optBoolean("accepted", false),
            reason = obj.optString("reason", "")
        )
    }
}

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

    var voiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", false) // Doc 51: DESLIGADO de fábrica para passageiro
        set(value) = prefs.edit().putBoolean("voice_enabled", value).apply()

    var lastRateText: String
        get() = prefs.getString("last_rate_text", "R$ --/km · R$ --/h") ?: "R$ --/km · R$ --/h"
        set(value) = prefs.edit().putString("last_rate_text", value).apply()

    var lastDetailsText: String
        get() = prefs.getString("last_details_text", "Aguardando oferta...") ?: "Aguardando oferta..."
        set(value) = prefs.edit().putString("last_details_text", value).apply()

    var lastReadSummary: String
        get() = prefs.getString("last_read_summary", "R$ --/km · R$ --/h") ?: "R$ --/km · R$ --/h"
        set(value) = prefs.edit().putString("last_read_summary", value).apply()

    var lastDecision: String
        get() = prefs.getString("last_decision", "AGUARDANDO") ?: "AGUARDANDO"
        set(value) = prefs.edit().putString("last_decision", value).apply()

    var lastPackageSeen: String
        get() = prefs.getString("last_package_seen", "Nenhum") ?: "Nenhum"
        set(value) = prefs.edit().putString("last_package_seen", value).apply()

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

    fun addRideHistory(entry: RideHistoryEntry) {
        try {
            val list = getRideHistory().toMutableList()
            list.add(0, entry) // mais recente primeiro
            if (list.size > 50) {
                list.removeAt(list.size - 1)
            }
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            prefs.edit().putString("ride_history_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getRideHistory(): List<RideHistoryEntry> {
        return try {
            val raw = prefs.getString("ride_history_json", null) ?: return emptyList()
            val arr = JSONArray(raw)
            val list = mutableListOf<RideHistoryEntry>()
            for (i in 0 until arr.length()) {
                list.add(RideHistoryEntry.fromJson(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun averageRate(): Double = if (rateSamples > 0) rateSum / rateSamples else 0.0

    fun resetDay() {
        acceptedCount = 0
        rejectedCount = 0
        estimatedEarnings = 0.0
        rateSum = 0.0
        rateSamples = 0
        prefs.edit().remove("ride_history_json").apply()
    }
}
