
package com.example.alkaid.data.repository

import com.example.alkaid.data.astronomy.Constellation
import com.example.alkaid.data.astronomy.Star

class ConstellationRepository {

    fun getConstellations(): List<Constellation> {
        return listOf(
            Constellation(
                "Orion",
                listOf(
                    Star("Betelgeuse", 5.9195, 7.4071),
                    Star("Rigel", 5.2423, -8.2016),
                    Star("Bellatrix", 5.4206, 6.3497),
                    Star("Mintaka", 5.5319, -0.2992),
                    Star("Alnilam", 5.6036, -1.2019),
                    Star("Alnitak", 5.6792, -1.9428),
                    Star("Saiph", 5.7895, -9.6697)
                )
            )
        )
    }
}
