package br.com.andreluis.pilotouber99

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideRuleEvaluatorTest {
    @Test
    fun claudeCase1_accepts_1_65_per_km() {
        val offer = RideOffer("Uber", 37.09, 1.8, 20.66)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertTrue(res.accepted)
    }

    @Test
    fun claudeCase2_rejects_1_25_per_km() {
        val offer = RideOffer("99", 10.0, 3.0, 5.0)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertFalse(res.accepted)
    }

    @Test
    fun claudeCase3_rejects_missing_km() {
        val offer = RideOffer("Uber", 25.0, 0.0, 0.0)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertFalse(res.accepted)
    }

    @Test
    fun andreRealUberPrint_calculates_rate_and_hourly_and_rejects_1_34() {
        val lines = listOf(
            "UberX",
            "R$ 60,46",
            "4 minutos (1,4 km) de distância",
            "Viagem de 51 minutos (43.8 km)",
            "Selecionar"
        )
        val offer = RideOfferParser.parseNodes("Uber", lines)
        assertNotNull(offer)
        assertEquals(55, offer!!.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertFalse(res.accepted)
        assertEquals(65.96, res.ratePerHour, 0.5)
    }

    @Test
    fun andreReal99Print_converts_meters_calculates_hourly_accepts() {
        val lines = listOf(
            "Moto",
            "R$ 72,47",
            "Taxa de deslocamento R$0,53",
            "2min (432m)",
            "14min (6,7km)",
            "Aceitar"
        )
        val offer = RideOfferParser.parseNodes("99", lines)
        assertNotNull(offer)
        assertEquals(72.47, offer!!.fareBrl, 0.01)
        assertEquals(0.432, offer.pickupKm, 0.001)
        assertEquals(6.7, offer.tripKm, 0.01)
        assertEquals(16, offer.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertTrue(res.accepted)
        assertEquals(271.76, res.ratePerHour, 0.5)
    }

    @Test
    fun cityFilter_blocks_unmarked_city_and_allows_marked_city() {
        val offerCanoas = RideOffer(
            appName = "Uber",
            fareBrl = 25.00,
            pickupKm = 1.0,
            tripKm = 10.0,
            rawText = "Av. Farroupilha, Canoas - RS"
        )
        val offerAlvorada = RideOffer(
            appName = "Uber",
            fareBrl = 25.00,
            pickupKm = 1.0,
            tripKm = 10.0,
            rawText = "Av. Getúlio Vargas, Alvorada - RS"
        )
        val allowed = setOf("Canoas", "Porto Alegre", "Esteio")

        assertTrue(RideRuleEvaluator.evaluate(offerCanoas, 1.40, true, allowed).accepted)
        val blocked = RideRuleEvaluator.evaluate(offerAlvorada, 1.40, true, allowed)
        assertFalse(blocked.accepted)
        assertEquals("Alvorada", blocked.blockedCity)
    }
}
