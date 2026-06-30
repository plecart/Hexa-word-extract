package com.hexa.map

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.hexa.core.geo.LatLng
import com.hexa.location.PositionFilter.filteredPositions
import com.hexa.location.PositionSample
import com.hexa.location.PositionSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [PositionSource] adossée au `FusedLocationProviderClient` de Google Play services, dont la sortie
 * brute est **filtrée** par [com.hexa.location.PositionFilter] (lissage + rejet des points imprécis)
 * avant d'alimenter la caméra. Remplace la source simulée de #9 — câblée dans
 * [MapScreen]/`chaseCameraViewModelFactory`.
 *
 * Frontière avec le framework : les mises à jour sont demandées à la collecte et libérées à
 * l'annulation (`awaitClose`), donc le GPS ne tourne que tant que la position est observée.
 * **Précondition** : `ACCESS_FINE_LOCATION` doit être accordée avant collecte — garanti par la porte
 * [LocationPermissionGate] (d'où le `@SuppressLint("MissingPermission")`).
 *
 * @param client client de localisation fusionnée (obtenu via `LocationServices`).
 * @param intervalMs intervalle souhaité entre deux mises à jour, en millisecondes.
 * @param smoothingFactor coefficient de lissage de position (cf. [com.hexa.location.PositionFilter]).
 * @param accuracyThresholdM précision maximale acceptée, en mètres (au-delà, le point est rejeté).
 */
class FusedLocationSource(
    private val client: FusedLocationProviderClient,
    private val intervalMs: Long,
    private val smoothingFactor: Double,
    private val accuracyThresholdM: Double,
) : PositionSource {
    override fun positions(): Flow<LatLng> = rawSamples().filteredPositions(smoothingFactor, accuracyThresholdM)

    @SuppressLint("MissingPermission") // permission garantie par LocationPermissionGate avant collecte
    private fun rawSamples(): Flow<PositionSample> =
        callbackFlow {
            val request =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs).build()
            val callback =
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation ?: return
                        trySend(
                            PositionSample(
                                position = LatLng(location.latitude, location.longitude),
                                accuracyM = location.accuracy.toDouble(),
                            ),
                        )
                    }
                }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            awaitClose { client.removeLocationUpdates(callback) }
        }
}
