
package com.example.alkaid.data.astronomy

data class Star(
    val name: String,
    val ra: Double, // Right Ascension in hours
    val dec: Double // Declination in degrees
)

data class Constellation(
    val name: String,
    val stars: List<Star>
)
