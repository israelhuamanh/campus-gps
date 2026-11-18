package com.tecsup.campusgps.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.tecsup.campusgps.R
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.UbicacionUi

@Composable
fun MapaUbicacion(
    estado: UbicacionUi,
    lugares: List<LugarConDistancia>,
    seleccionado: Int?,
    masCercanoId: Int? = null,
    radioMetros: Int = 200,
    tienePermiso: Boolean = false,
    modifier: Modifier = Modifier
) {
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-12.046374, -77.042793), 16f)
    }

    val contexto = LocalContext.current
    val esOscuro = isSystemInDarkTheme()

    val estiloMapa = remember(esOscuro) {
        if (!esOscuro) null else try {
            MapStyleOptions.loadRawResourceStyle(contexto, R.raw.map_style_dark)
        } catch (_: Exception) {
            null
        }
    }

    var propiedades by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false,
                mapStyleOptions = estiloMapa
            )
        )
    }

    LaunchedEffect(tienePermiso, estiloMapa) {
        propiedades = propiedades.copy(
            isMyLocationEnabled = tienePermiso,
            mapStyleOptions = estiloMapa
        )
    }

    val configuracionUi = remember {
        MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            compassEnabled = true
        )
    }

    val miPosicion = (estado as? UbicacionUi.Lista)?.posicion?.let {
        LatLng(it.latitud, it.longitud)
    }

    LaunchedEffect(miPosicion) {
        miPosicion?.let { pos ->
            camara.animate(CameraUpdateFactory.newLatLngZoom(pos, 16.5f))
        }
    }

    LaunchedEffect(seleccionado) {
        val lugarSel = lugares.firstOrNull { it.lugar.id == seleccionado }?.lugar
        if (lugarSel != null) {
            val destino = LatLng(lugarSel.latitud, lugarSel.longitud)
            camara.animate(CameraUpdateFactory.newLatLngZoom(destino, 17.5f))
        }
    }

    val colorCirculo = MaterialTheme.colorScheme.primary

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camara,
        properties = propiedades,
        uiSettings = configuracionUi
    ) {
        if (miPosicion != null) {
            Circle(
                center = miPosicion,
                radius = radioMetros.toDouble(),
                fillColor = colorCirculo.copy(alpha = 0.14f),
                strokeColor = colorCirculo.copy(alpha = 0.65f),
                strokeWidth = 3f
            )
            Marker(
                state = rememberMarkerState(position = miPosicion).apply { position = miPosicion },
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
                val pos = LatLng(item.lugar.latitud, item.lugar.longitud)
                Marker(
                    state = rememberMarkerState(key = item.lugar.id.toString(), position = pos).apply { position = pos },
                    title = item.lugar.nombre,
                    snippet = item.lugar.categoria,
                    icon = BitmapDescriptorFactory.defaultMarker(colorMarcador)
                )
            }
        }
    }
}
