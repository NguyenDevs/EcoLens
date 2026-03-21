package com.nguyendevs.ecolens.managers.main

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "EcoLocationManager"
private const val DEFAULT_LAT = 16.0544   // Đà Nẵng
private const val DEFAULT_LNG = 108.2022  // Đà Nẵng
private const val TIMEOUT_MS = 5000L
private const val CACHE_TTL_MS = 15 * 60 * 1000L  // 15 phút

/**
 * Manager lấy vị trí GPS hiện tại của người dùng
 * Sử dụng FusedLocationProviderClient để tối ưu pin
 * Cache vị trí trong RAM với TTL 15 phút — tự clear khi app bị kill
 * Fallback về tọa độ Đà Nẵng nếu không lấy được vị trí
 */
object EcoLocationManager {

    data class Coordinates(val lat: Double, val lng: Double)

    val defaultCoordinates = Coordinates(DEFAULT_LAT, DEFAULT_LNG)

    /** Lưu đệm tọa độ tạm thời trong phiên hoạt động. */
    private var cachedLocation: Coordinates? = null
    private var cachedAt: Long = 0L

    private fun isCacheValid(): Boolean {
        return cachedLocation != null && (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MS
    }

    /**
     * Lấy vị trí GPS hiện tại của người dùng
     * - Nếu cache còn hợp lệ (< 15 phút) → trả về ngay, không fetch mới
     * - Thử lastKnownLocation trước (nhanh, ít tốn pin)
     * - Nếu không có → request vị trí mới, timeout 5 giây
     * - Fallback: tọa độ Đà Nẵng (16.0544, 108.2022)
     *
     * @param context Context (phải đã có quyền ACCESS_FINE_LOCATION)
     * @return Coordinates chứa lat/lng thực hoặc tọa độ mặc định
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Coordinates {
        // Dùng cache nếu còn hợp lệ (trong vòng 15 phút)
        if (isCacheValid()) {
            Log.d(TAG, "Using cached location: ${cachedLocation!!.lat}, ${cachedLocation!!.lng}")
            return cachedLocation!!
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        // Thử lấy lastKnownLocation nhanh trước
        val lastKnown = suspendCancellableCoroutine<android.location.Location?> { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
        }

        if (lastKnown != null) {
            Log.d(TAG, "Using last known location: ${lastKnown.latitude}, ${lastKnown.longitude}")
            val coords = Coordinates(lastKnown.latitude, lastKnown.longitude)
            cachedLocation = coords
            cachedAt = System.currentTimeMillis()
            return coords
        }

        // Nếu không có lastKnownLocation → request vị trí mới, timeout 5s
        Log.d(TAG, "No last location, requesting fresh location...")
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000L
                ).setMaxUpdates(1).build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            Log.d(TAG, "Got live location: ${location.latitude}, ${location.longitude}")
                            fusedClient.removeLocationUpdates(this)
                            if (cont.isActive) cont.resume(Coordinates(location.latitude, location.longitude))
                        }
                    }
                }

                cont.invokeOnCancellation { fusedClient.removeLocationUpdates(callback) }
                
                try {
                    fusedClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        Looper.getMainLooper()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request location updates: ${e.message}")
                    fusedClient.removeLocationUpdates(callback)
                    if (cont.isActive) cont.resume(defaultCoordinates)
                }
            }
        }

        return if (result != null) {
            cachedLocation = result
            cachedAt = System.currentTimeMillis()
            result
        } else {
            Log.w(TAG, "Location timeout — using Da Nang fallback")
            defaultCoordinates
        }
    }
}
