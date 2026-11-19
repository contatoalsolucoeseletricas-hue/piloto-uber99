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

    @Test
    fun doc48_andreRealPrint1_99Motorista_accepts_1_48_per_km() {
        val lines = listOf(
            "99Pop",
            "R$15,80",
            "R$1,38 por dirigir entre áreas",
            "5 min (1,6 km)",
            "19 min (9,1 km)",
            "Aceitar"
        )
        val offer = RideOfferParser.parseNodes("99", lines)
        assertNotNull(offer)
        assertEquals(15.80, offer!!.fareBrl, 0.01)
        assertEquals(1.6, offer.pickupKm, 0.01)
        assertEquals(9.1, offer.tripKm, 0.01)
        assertEquals(10.7, offer.totalKm, 0.01)
        assertEquals(24, offer.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertTrue(res.accepted)
        assertEquals(1.476, res.ratePerKm, 0.01)
    }

    @Test
    fun doc48_andreRealPrint2_99Motorista_rejects_1_37_per_km() {
        val lines = listOf(
            "99Pop",
            "R$15,40",
            "R$1,10 por espera longa incl.",
            "4 min (1 km)",
            "25 min (10,2 km)",
            "Aceitar"
        )
        val offer = RideOfferParser.parseNodes("99", lines)
        assertNotNull(offer)
        assertEquals(15.40, offer!!.fareBrl, 0.01)
        assertEquals(1.0, offer.pickupKm, 0.01)
        assertEquals(10.2, offer.tripKm, 0.01)
        assertEquals(11.2, offer.totalKm, 0.01)
        assertEquals(29, offer.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertFalse(res.accepted)
        assertEquals(1.375, res.ratePerKm, 0.01)
    }

    @Test
    fun doc50_print1_99Pop_24_40_rejects_1_21() {
        val lines = listOf(
            "99Pop",
            "R$24,40",
            "R$1,10 por embarque distante incl.",
            "5 min (2,1 km)",
            "24 min (18,1 km)",
            "Aceitar"
        )
        val offer = RideOfferParser.parseNodes("99", lines)
        assertNotNull(offer)
        assertEquals(24.40, offer!!.fareBrl, 0.01)
        assertEquals(2.1, offer.pickupKm, 0.01)
        assertEquals(18.1, offer.tripKm, 0.01)
        assertEquals(20.2, offer.totalKm, 0.01)
        assertEquals(29, offer.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertFalse(res.accepted)
        assertEquals(1.208, res.ratePerKm, 0.01)
    }

    @Test
    fun doc50_print2_99Pop_24_50_ignores_per_km_accepts_1_54() {
        val lines = listOf(
            "99Pop",
            "R$24,50",
            "R$1,54/km",
            "R$1,10 por espera longa incl.",
            "5 min (1,5 km)",
            "40 min (14,4 km)",
            "Aceitar"
        )
        val offer = RideOfferParser.parseNodes("99", lines)
        assertNotNull(offer)
        assertEquals(24.50, offer!!.fareBrl, 0.01)
        assertEquals(1.5, offer.pickupKm, 0.01)
        assertEquals(14.4, offer.tripKm, 0.01)
        assertEquals(15.9, offer.totalKm, 0.01)
        assertEquals(45, offer.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertTrue(res.accepted)
        assertEquals(1.541, res.ratePerKm, 0.01)
        assertEquals(32.66, res.ratePerHour, 0.5)
    }

    @Test
    fun doc50_print3_99Pop_17_20_accepts_1_46() {
        val lines = listOf(
            "99Pop",
            "R$17,20",
            "R$1,38 por embarque distante incl.",
            "6 min (2,2 km)",
            "25 min (9,6 km)",
            "Aceitar"
        )
        val offer = RideOfferParser.parseNodes("99", lines)
        assertNotNull(offer)
        assertEquals(17.20, offer!!.fareBrl, 0.01)
        assertEquals(2.2, offer.pickupKm, 0.01)
        assertEquals(9.6, offer.tripKm, 0.01)
        assertEquals(11.8, offer.totalKm, 0.01)
        assertEquals(31, offer.totalMinutes)
        val res = RideRuleEvaluator.evaluate(offer, 1.40)
        assertTrue(res.accepted)
        assertEquals(1.458, res.ratePerKm, 0.01)
    }
}
