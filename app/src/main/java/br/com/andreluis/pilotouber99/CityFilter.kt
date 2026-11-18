package br.com.andreluis.pilotouber99

import java.text.Normalizer

object CityFilter {
    val ALL_CITIES: List<String> = listOf(
        "Porto Alegre",
        "Viamão",
        "Alvorada",
        "Canoas",
        "Eldorado do Sul",
        "Guaíba",
        "Esteio",
        "Sapucaia do Sul",
        "São Leopoldo",
        "Centro",
        "Nova Santa Rita",
        "Montenegro",
        "Triunfo"
    )

    private val ALIASES: Map<String, List<String>> = mapOf(
        "Eldorado do Sul" to listOf("eldorado do sul", "eldorado"),
        "Porto Alegre" to listOf("porto alegre", "poa"),
        "Centro" to listOf("centro", "centro historico")
    )

    fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase()
    }

    fun isRouteAllowed(
        rawOfferText: String,
        cityFilterEnabled: Boolean,
        allowedCities: Set<String>
    ): Pair<Boolean, String?> {
        if (!cityFilterEnabled) return Pair(true, null)
        if (allowedCities.isEmpty()) return Pair(false, "Nenhuma cidade marcada")

        val normText = normalize(rawOfferText)

        for (city in ALL_CITIES) {
            if (city !in allowedCities) {
                val keys = ALIASES[city] ?: listOf(normalize(city))
                if (keys.any { normText.contains(it) }) {
                    return Pair(false, city)
                }
            }
        }

        val matchesAllowed = allowedCities.any { city ->
            val keys = ALIASES[city] ?: listOf(normalize(city))
            keys.any { normText.contains(it) }
        }
        return if (matchesAllowed) {
            Pair(true, null)
        } else {
            Pair(false, "Região fora das marcadas")
        }
    }
}
