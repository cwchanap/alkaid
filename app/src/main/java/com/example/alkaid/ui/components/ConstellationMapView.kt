
package com.example.alkaid.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.alkaid.data.astronomy.Constellation
import com.example.alkaid.data.astronomy.Star

class ConstellationMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs) {

    private val starPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
    }

    private var constellations: List<Constellation> = emptyList()
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    fun setConstellations(constellations: List<Constellation>) {
        this.constellations = constellations
        invalidate()
    }

    fun setLocation(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        constellations.forEach { constellation ->
            var lastX: Float? = null
            var lastY: Float? = null
            var firstX: Float? = null
            var firstY: Float? = null

            constellation.stars.forEach { star ->
                val (x, y) = projectStar(star)
                canvas.drawCircle(x, y, 5f, starPaint)

                if (firstX == null) {
                    firstX = x
                    firstY = y
                }

                if (lastX != null && lastY != null) {
                    canvas.drawLine(lastX!!, lastY!!, x, y, linePaint)
                }
                lastX = x
                lastY = y
            }

            if (lastX != null && lastY != null && firstX != null && firstY != null) {
                canvas.drawLine(lastX!!, lastY!!, firstX!!, firstY!!, linePaint)
            }

            // Draw constellation name
            val centerX = constellation.stars.map { projectStar(it).first }.average().toFloat()
            val centerY = constellation.stars.map { projectStar(it).second }.average().toFloat()
            canvas.drawText(constellation.name, centerX, centerY, textPaint)
        }
    }

    private fun projectStar(star: Star): Pair<Float, Float> {
        val lst = calculateLST()
        val (az, alt) = raDecToAzAlt(star.ra, star.dec, lst)

        // Simple stereographic projection
        val r = (90 - alt) * (height / 180f)
        val x = (width / 2 + r * Math.cos(Math.toRadians(az))).toFloat()
        val y = (height / 2 + r * Math.sin(Math.toRadians(az))).toFloat()

        return Pair(x, y)
    }

    private fun raDecToAzAlt(ra: Double, dec: Double, lst: Double): Pair<Double, Double> {
        val ha = lst - ra * 15 // Hour Angle in degrees
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(dec)
        val haRad = Math.toRadians(ha)

        val altRad = Math.asin(Math.sin(decRad) * Math.sin(latRad) + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad))
        val azRad = Math.acos((Math.sin(decRad) - Math.sin(altRad) * Math.sin(latRad)) / (Math.cos(altRad) * Math.cos(latRad)))

        var az = Math.toDegrees(azRad)
        if (Math.sin(haRad) > 0) {
            az = 360 - az
        }

        val alt = Math.toDegrees(altRad)
        return Pair(az, alt)
    }

    private fun calculateLST(): Double {
        // Simplified LST calculation
        val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val jd = toJulianDate(now)
        val gmst = calculateGMST(jd)
        return gmst + longitude
    }

    private fun toJulianDate(calendar: java.util.Calendar): Double {
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val second = calendar.get(java.util.Calendar.SECOND)

        if (month <= 2) {
            // year--
            // month += 12
        }

        val a = Math.floor(year / 100.0)
        val b = 2 - a + Math.floor(a / 4.0)

        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + (hour + minute / 60.0 + second / 3600.0) / 24.0 + b - 1524.5
    }

    private fun calculateGMST(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        var gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        return gmst % 360
    }
}
