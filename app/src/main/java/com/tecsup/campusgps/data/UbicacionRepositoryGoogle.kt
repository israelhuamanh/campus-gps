package com.tecsup.campusgps.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tecsup.campusgps.model.Posicion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UbicacionRepositoryGoogle(
    private val context: Context
) : UbicacionRepository {

    private val cliente by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override fun playServicesDisponible(): Boolean {
        val estado = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return estado == ConnectionResult.SUCCESS
    }

    @SuppressLint("MissingPermission")
    override suspend fun obtenerPosicion(): Result<Posicion> {
        return try {
            val token = CancellationTokenSource()
            val ubicacion = cliente
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                .await()
            if (ubicacion != null) {
                Result.success(Posicion(ubicacion.latitude, ubicacion.longitude))
            } else {
                Result.failure(
                    IllegalStateException("No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo")
                )
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun observarPosicion(): Flow<Posicion> = callbackFlow {
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val ubicacion = resultado.lastLocation ?: return
                trySend(Posicion(ubicacion.latitude, ubicacion.longitude))
            }
        }

        try {
            cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            cliente.removeLocationUpdates(callback)
        }
    }
}
