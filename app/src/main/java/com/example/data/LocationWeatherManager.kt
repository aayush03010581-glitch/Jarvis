package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume

class LocationWeatherManager(private val context: Context) {

    suspend fun getCurrentWeatherReport(): String = withContext(Dispatchers.IO) {
        val location = getDeviceLocation()
        if (location == null) {
            return@withContext "GPS location access not granted or unavailable. Please enable location permissions in Android settings to receive real-time local weather telemetry, Sir."
        }

        val lat = location.latitude
        val lon = location.longitude

        try {
            val client = OkHttpClient()
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (body != null) {
                val json = JSONObject(body)
                val current = json.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")
                val windSpeed = current.getDouble("windspeed")
                val weatherCode = current.getInt("weathercode")
                val condition = getWeatherConditionDescription(weatherCode)

                return@withContext "GPS Location Fix: [Lat: %.4f, Lon: %.4f]\nCurrent Weather: $condition\nTemperature: %.1f°C\nWind Speed: %.1f km/h\nTelemetry acquired via satellite GPS fix, Sir.".format(lat, lon, temp, windSpeed)
            }
        } catch (e: Exception) {
            return@withContext "Error fetching meteorological data from Open-Meteo satellite feed: ${e.localizedMessage}"
        }

        return@withContext "Unable to parse meteorological telemetry response."
    }

    private suspend fun getDeviceLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            var bestLocation: Location? = null
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }
            continuation.resume(bestLocation)
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    private fun getWeatherConditionDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear, partly cloudy, or overcast"
            45, 48 -> "Foggy or rime fog"
            51, 53, 55 -> "Drizzle: Light, moderate, or dense intensity"
            61, 63, 65 -> "Rain: Slight, moderate, or heavy intensity"
            71, 73, 75 -> "Snow fall: Slight, moderate, or heavy intensity"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers: Slight, moderate, or violent"
            85, 86 -> "Snow showers slight or heavy"
            95 -> "Thunderstorm: Slight or moderate"
            96, 99 -> "Thunderstorm with slight and heavy hail"
            else -> "Atmospheric anomaly (Code $code)"
        }
    }
}
