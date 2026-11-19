package br.com.andreluis.pilotouber99

object RideOfferParser {
    // Aceita R$15,80 e R$ 15,80
    private val moneyRegex = Regex("""R\$\s*(\d+(?:[.,]\d{1,2})?)""")
    // Aceita 1 km, 1,6 km, 10.2km, (1,6 km)
    private val distRegex = Regex("""\(?\s*(\d+(?:[.,]\d+)?)\s*(km|m)\b""", RegexOption.IGNORE_CASE)
    // Aceita 5 min, 24 minutos
    private val minRegex = Regex("""(\d+)\s*(?:minutos?|min)\b""", RegexOption.IGNORE_CASE)

    private fun toDoubleFlexible(raw: String): Double? =
        raw.replace(",", ".").toDoubleOrNull()

    fun parseNodes(appName: String, lines: List<String>): RideOffer? {
        // Ignora linhas de adicionais secundários que contêm outros valores em R$
        val cleanLines = lines.filter { line ->
            !line.contains("dirigir entre", ignoreCase = true) &&
            !line.contains("espera longa", ignoreCase = true) &&
            !line.contains("Taxa de deslocamento", ignoreCase = true) &&
            !line.contains("Dinâmico", ignoreCase = true) &&
            !line.contains("incl.", ignoreCase = true)
        }

        // Se houver múltiplos valores em R$, escolhe o maior valor monetário principal do card
        val allFares = cleanLines.mapNotNull { line ->
            moneyRegex.find(line)?.let { m -> toDoubleFlexible(m.groupValues[1]) }
        }
        val fare = allFares.maxOrNull() ?: return null
        if (fare <= 0.0) return null

        val joined = lines.joinToString("\n")

        val distancesKm = distRegex.findAll(joined).mapNotNull { m ->
            val value = toDoubleFlexible(m.groupValues[1]) ?: return@mapNotNull null
            val unit = m.groupValues[2].lowercase()
            if (unit == "m") value / 1000.0 else value
        }.toList()

        if (distancesKm.isEmpty()) return null

        val pickupKm: Double
        val tripKm: Double
        if (distancesKm.size >= 2) {
            pickupKm = distancesKm[0]
            tripKm = distancesKm[1]
        } else {
            pickupKm = 0.0
            tripKm = distancesKm[0]
        }

        val mins = minRegex.findAll(joined).mapNotNull { m ->
            m.groupValues[1].toIntOrNull()
        }.toList()

        val pickupMin = if (mins.size >= 2) mins[0] else 0
        val tripMin = if (mins.size >= 2) mins[1] else (mins.firstOrNull() ?: 0)

        return RideOffer(
            appName = appName,
            fareBrl = fare,
            pickupKm = pickupKm,
            tripKm = tripKm,
            pickupMinutes = pickupMin,
            tripMinutes = tripMin,
            rawText = joined
        )
    }
}
