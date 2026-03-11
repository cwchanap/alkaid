package com.example.alkaid.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstellationRepositoryTest {

    private val repository = ConstellationRepository()

    @Test
    fun `getConstellations returns non-empty list`() {
        val constellations = repository.getConstellations()
        assertFalse(constellations.isEmpty())
    }

    @Test
    fun `getConstellations returns Orion`() {
        val constellations = repository.getConstellations()
        val orion = constellations.find { it.name == "Orion" }
        assertTrue("Orion constellation should be present", orion != null)
    }

    @Test
    fun `Orion constellation has seven stars`() {
        val orion = repository.getConstellations().first { it.name == "Orion" }
        assertEquals(7, orion.stars.size)
    }

    @Test
    fun `Orion constellation contains Betelgeuse`() {
        val orion = repository.getConstellations().first { it.name == "Orion" }
        val betelgeuse = orion.stars.find { it.name == "Betelgeuse" }
        assertTrue("Betelgeuse should be in Orion", betelgeuse != null)
    }

    @Test
    fun `Orion stars have valid right ascension values`() {
        val orion = repository.getConstellations().first { it.name == "Orion" }
        orion.stars.forEach { star ->
            assertTrue(
                "Star ${star.name} RA ${star.ra} should be in range [0, 24)",
                star.ra >= 0.0 && star.ra < 24.0
            )
        }
    }

    @Test
    fun `Orion stars have valid declination values`() {
        val orion = repository.getConstellations().first { it.name == "Orion" }
        orion.stars.forEach { star ->
            assertTrue(
                "Star ${star.name} Dec ${star.dec} should be in range [-90, 90]",
                star.dec in -90.0..90.0
            )
        }
    }
}
