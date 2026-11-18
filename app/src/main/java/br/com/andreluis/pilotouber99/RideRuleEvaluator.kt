package br.com.andreluis.pilotouber99

import java.util.Locale

data class RideOffer(
    val appName: String,
    val fareBrl: Double,
    val pickupKm: Double,
    val tripKm: Double,
    val pickupMinutes: Int = 0,
    val tripMinutes: Int = 0,
    val rawText: String = ""
) {
    val totalKm: Double get() = pickupKm + tripKm
    val totalMinutes: Int get() = pickupMinutes + tripMinutes
    val ratePerKm: Double get() = if (totalKm > 0.0) fareBrl / totalKm else 0.0
    val ratePerHour: Double get() = if (totalMinutes > 0) fareBrl / (totalMinutes / 60.0) else 0.0
}

data class EvaluationResult(
    val accepted: Boolean,
    val ratePerKm: Double,
    val ratePerHour: Double,
    val reason: String,
    val blockedCity: String? = null
)

object RideRuleEvaluator {
    fun evaluate(
        offer: RideOffer?,
        minRatePerKm: Double = 1.40,
        cityFilterEnabled: Boolean = false,
        allowedCities: Set<String> = emptySet()
    ): EvaluationResult {
        if (offer == null) return EvaluationResult(false, 0.0, 0.0, "Oferta incompleta")
        if (offer.fareBrl <= 0.0 || offer.tripKm <= 0.0 || offer.pickupKm < 0.0) {
            return EvaluationResult(false, 0.0, 0.0, "Valor ou distância ausente")
        }
        val totalKm = offer.pickupKm + offer.tripKm
        if (totalKm <= 0.0) return EvaluationResult(false, 0.0, 0.0, "Distância total inválida")
        val rate = offer.fareBrl / totalKm
        val rateHour = offer.ratePerHour

        val (cityOk, blockedCity) = CityFilter.isRouteAllowed(offer.rawText, cityFilterEnabled, allowedCities)
        if (!cityOk) {
            return EvaluationResult(false, rate, rateHour, "Cidade fora da rota: $blockedCity", blockedCity)
        }

        val ok = rate >= minRatePerKm
        val msg = if (ok) "Aprovada" else String.format(Locale.US, "Abaixo de R$ %.2f/km", minRatePerKm)
        return EvaluationResult(ok, rate, rateHour, msg)
    }
}
