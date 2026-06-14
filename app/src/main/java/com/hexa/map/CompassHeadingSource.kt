package com.hexa.map

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.hexa.location.HeadingSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.sample

/**
 * [HeadingSource] adossée à la boussole de l'appareil via le capteur **virtuel** de vecteur de
 * rotation (fusion accéléromètre + magnétomètre + gyroscope), plus stable qu'un magnétomètre nu.
 *
 * Frontière avec le framework Android : l'écoute du capteur est ouverte à la collecte du flux et
 * libérée à l'annulation (`awaitClose`), si bien que la boussole ne tourne que tant que la caméra
 * est observée.
 *
 * Cette source émet le cap **brut** mais **rate-limité** : la boussole peut émettre à haute
 * fréquence ; on échantillonne à [SAMPLE_MS] pour ne pas saturer l'animation de caméra en aval
 * (chaque mesure relancerait l'interpolation et la figerait). Le lissage et la circularité sont
 * traités en aval par [HeadingSmoother][com.hexa.location.HeadingSmoother].
 *
 * Le cap est l'azimut de l'appareil tenu **à plat** (référentiel capteur par défaut) ; un éventuel
 * remappage d'axes pour une tenue verticale relève de la validation visuelle sur device.
 *
 * @param sensorManager service capteurs de l'app (obtenu via `Context.getSystemService`).
 */
class CompassHeadingSource(private val sensorManager: SensorManager) : HeadingSource {
    @OptIn(FlowPreview::class)
    override fun headings(): Flow<Double> = callbackFlow {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector == null) {
            // Appareil sans capteur d'orientation : flux vide, la caméra garde le cap d'amorçage.
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(MATRIX_SIZE)
        val orientation = FloatArray(ORIENTATION_SIZE)
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val rawDeg = Math.toDegrees(orientation[AZIMUTH].toDouble())
                    trySend((rawDeg + FULL_TURN_DEG) % FULL_TURN_DEG)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

        // Cadence UI (~16 Hz) : suffisante pour un cap fluide, puis encore réduite par sample().
        sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.sample(SAMPLE_MS)

    private companion object {
        const val MATRIX_SIZE = 9
        const val ORIENTATION_SIZE = 3
        const val AZIMUTH = 0
        const val FULL_TURN_DEG = 360.0

        /** Période d'échantillonnage du cap (ms) : ~10 Hz, fluide sans saturer la caméra ni le CPU. */
        const val SAMPLE_MS = 100L
    }
}
