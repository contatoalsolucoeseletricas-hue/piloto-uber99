package br.com.andreluis.pilotouber99

object RideOfferParser {
    private val moneyRegex = Regex("""R\$\s*(\d+(?:[.,]\d{1,2})?)""")
    private val distRegex = Regex("""\(?\s*(\d+(?:[.,]\d+)?)\s*(km|m)\b""", RegexOption.IGNORE_CASE)
    private val minRegex = Regex("""(\d+)\s*(?:minutos?|min)\b""", RegexOption.IGNORE_CASE)

    private fun toDoubleFlexible(raw: String): Double? =
        raw.replace(",", ".").toDoubleOrNull()

    fun parseNodes(appName: String, lines: List<String>): RideOffer? {
        val cleanLines = lines.filter {
            !it.contains("Taxa de deslocamento", ignoreCase = true) &&
                !it.contains("Dinâmico", ignoreCase = true)
        }
        val joined = cleanLines.joinToString("\n")
        val fareMatch = moneyRegex.find(joined) ?: return null
        val fare = toDoubleFlexible(fareMatch.groupValues[1]) ?: return null

        val distancesKm = distRegex.findAll(joined).mapNotNull { m ->
            val value = toDoubleFlexible(m.groupValues[1]) ?: return@mapNotNull null
            val unit = m.groupValues[2].lowercase()
            if (unit == "m") value / 1000.0 else value
        }.toList()

        if (distancesKm.size < 2) return null

        val mins = minRegex.findAll(joined).mapNotNull { m ->
            m.groupValues[1].toIntOrNull()
        }.toList()

        return RideOffer(
            appName = appName,
            fareBrl = fare,
            pickupKm = distancesKm[0],
            tripKm = distancesKm[1],
            pickupMinutes = mins.getOrElse(0) { 0 },
            tripMinutes = mins.getOrElse(1) { 0 },
            rawText = joined
        )
    }
}
