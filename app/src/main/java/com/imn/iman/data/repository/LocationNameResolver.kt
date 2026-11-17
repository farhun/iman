package com.imn.iman.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.imn.iman.i18n.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOCATION_TAG = "ImanLocation"
const val DEFAULT_LOCATION_LATITUDE = 21.4225
const val DEFAULT_LOCATION_LONGITUDE = 39.8262

data class ResolvedPlace(val name: String, val countryCode: String)

class LocationNameResolver(context: Context) {
    private val appContext = context.applicationContext

    suspend fun resolve(lat: Double, lng: Double): ResolvedPlace? {
        val geocoderPresent = try {
            Geocoder.isPresent()
        } catch (error: Exception) {
            Log.w(LOCATION_TAG, "Geocoder availability check failed", error)
            false
        }
        if (!geocoderPresent) {
            Log.w(LOCATION_TAG, "Geocoder is not present")
            return null
        }

        repeat(3) { index ->
            val attempt = index + 1
            Log.d(LOCATION_TAG, "Attempt $attempt of 3 to resolve $lat, $lng")
            var failureReason = "no usable address"
            val place = try {
                val addresses = withTimeoutOrNull(8_000L) {
                    val geocoder = Geocoder(appContext, currentAppLocale())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<List<Address>> { continuation ->
                            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (continuation.isActive) continuation.resume(addresses)
                                }

                                override fun onError(errorMessage: String?) {
                                    if (continuation.isActive) {
                                        continuation.resumeWithException(
                                            IllegalStateException(errorMessage ?: "Geocoding failed")
                                        )
                                    }
                                }
                            })
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(lat, lng, 1).orEmpty()
                        }
                    }
                }
                if (addresses == null) {
                    failureReason = "timed out"
                    null
                } else if (addresses.isEmpty()) {
                    failureReason = "empty result"
                    null
                } else {
                    val address = addresses.first()
                    val name = listOfNotNull(
                        address.locality?.takeIf(String::isNotBlank),
                        address.subAdminArea?.takeIf(String::isNotBlank),
                        address.adminArea?.takeIf(String::isNotBlank)
                    ).firstOrNull()?.trim()
                    val countryCode = address.countryCode
                        ?.trim()
                        ?.uppercase(Locale.ROOT)
                        ?.takeIf { code ->
                            code.length == 2 && Locale.getISOCountries().any { it == code }
                        }
                    if (name.isNullOrBlank() || countryCode == null) {
                        failureReason = "address has no valid name or country code"
                        null
                    } else {
                        ResolvedPlace(name, countryCode)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failureReason = error.message ?: error.javaClass.simpleName
                null
            }

            if (place != null) {
                Log.d(LOCATION_TAG, "Lookup succeeded: ${place.name}, ${place.countryCode}")
                return place
            }
            Log.w(LOCATION_TAG, "Lookup failed on attempt $attempt of 3: $failureReason")
            if (attempt < 3) delay(if (attempt == 1) 2_000L else 5_000L)
        }
        return null
    }

    fun currentLanguageTag(): String = currentAppLocale().toLanguageTag()

    private fun currentAppLocale(): Locale {
        val savedTag = LocaleHelper.savedTag(appContext)
        return if (savedTag != "system" && savedTag.isNotBlank()) {
            Locale.forLanguageTag(savedTag)
        } else {
            Locale.getDefault()
        }
    }
}

fun displayLocationName(
    latitude: Double,
    longitude: Double,
    label: String,
    labelLatitude: Double?,
    labelLongitude: Double?,
    defaultLabel: String
): String {
    val storedLabel = label.takeIf(String::isNotBlank)
    if (storedLabel == null) {
        return if (isDefaultLocation(latitude, longitude)) defaultLabel else formatCoordinates(latitude, longitude)
    }

    val hasStoredReference = labelLatitude != null && labelLongitude != null
    val referenceLatitude = if (hasStoredReference) labelLatitude!! else DEFAULT_LOCATION_LATITUDE
    val referenceLongitude = if (hasStoredReference) labelLongitude!! else DEFAULT_LOCATION_LONGITUDE
    return if (distanceMeters(latitude, longitude, referenceLatitude, referenceLongitude) <= 25_000.0) {
        storedLabel
    } else {
        formatCoordinates(latitude, longitude)
    }
}

internal fun isDefaultLocation(latitude: Double, longitude: Double): Boolean =
    kotlin.math.abs(latitude - DEFAULT_LOCATION_LATITUDE) <= 0.0001 &&
        kotlin.math.abs(longitude - DEFAULT_LOCATION_LONGITUDE) <= 0.0001

internal fun distanceMeters(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double
): Double {
    val earthRadiusMeters = 6_371_000.0
    val lat1 = Math.toRadians(latitude1)
    val lat2 = Math.toRadians(latitude2)
    val deltaLat = Math.toRadians(latitude2 - latitude1)
    val deltaLng = Math.toRadians(longitude2 - longitude1)
    val haversine = (sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)).coerceIn(0.0, 1.0)
    return earthRadiusMeters * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

private fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.2f, %.2f", latitude, longitude)
