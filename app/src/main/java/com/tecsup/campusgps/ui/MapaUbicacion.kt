package com.tecsup.campusgps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.UbicacionUi

@Composable
fun MapaUbicacion(
    estado: UbicacionUi,
    lugares: List<LugarConDistancia>,
    seleccionado: Int?,
    masCercanoId: Int? = null,
    radioMetros: Int = 200,
    modifier: Modifier = Modifier
) {
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-12.0464, -77.0428), 12f)
    }

    LaunchedEffect(estado) {
        if (estado is UbicacionUi.Lista) {
            val destino = LatLng(estado.posicion.latitud, estado.posicion.longitud)
            camara.animate(CameraUpdateFactory.newLatLngZoom(destino, 16f))
        }
    }

    LaunchedEffect(seleccionado) {
        val lugar = lugares.firstOrNull { it.lugar.id == seleccionado }?.lugar
        if (lugar != null) {
            camara.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lugar.latitud, lugar.longitud),
                    17f
                )
            )
        }
    }

    val colorCirculo = MaterialTheme.colorScheme.primary

    GoogleMap(modifier = modifier, cameraPositionState = camara) {
        if (estado is UbicacionUi.Lista) {
            val miPosicion = LatLng(estado.posicion.latitud, estado.posicion.longitud)
            Circle(
                center = miPosicion,
                radius = radioMetros.toDouble(),
                fillColor = colorCirculo.copy(alpha = 0.14f),
                strokeColor = colorCirculo.copy(alpha = 0.65f),
                strokeWidth = 3f
            )
            Marker(
                state = rememberUpdatedMarkerState(position = miPosicion),
                title = "Estás aquí"
            )
        }

        lugares.forEach { item ->
            key(item.lugar.id) {
                val esMasCercano = item.lugar.id == masCercanoId
                val colorMarcador = if (esMasCercano) {
                    BitmapDescriptorFactory.HUE_GREEN
                } else {
                    BitmapDescriptorFactory.HUE_AZURE
                }
                Marker(
                    state = rememberUpdatedMarkerState(
                        position = LatLng(item.lugar.latitud, item.lugar.longitud)
                    ),
                    title = item.lugar.nombre,
                    snippet = item.lugar.categoria,
                    icon = BitmapDescriptorFactory.defaultMarker(colorMarcador)
                )
            }
        }
    }
}
