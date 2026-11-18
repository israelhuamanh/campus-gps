package com.tecsup.campusgps.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.UbicacionUi
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

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
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val temaOscuro = isSystemInDarkTheme()
    val colorCirculo = MaterialTheme.colorScheme.primary

    remember(contexto) {
        Configuration.getInstance().apply {
            load(
                contexto,
                contexto.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            )
            userAgentValue = contexto.packageName
            osmdroidBasePath = contexto.cacheDir
            osmdroidTileCache = File(contexto.cacheDir, "osmdroid")
        }
    }

    val mapView = remember(contexto) {
        MapView(contexto).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(-12.0464, -77.0428))
        }
    }

    val miUbicacionOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(contexto), mapView)
    }

    val iconoUsuario = remember(contexto) {
        crearPinDrawable(contexto, AndroidColor.parseColor("#E53935"))
    }
    val iconoLugar = remember(contexto) {
        crearPinDrawable(contexto, AndroidColor.parseColor("#1E88E5"))
    }
    val iconoMasCercano = remember(contexto) {
        crearPinDrawable(contexto, AndroidColor.parseColor("#2E7D32"))
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observador = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observador)
            miUbicacionOverlay.disableMyLocation()
            mapView.onDetach()
        }
    }

    LaunchedEffect(temaOscuro, mapView) {
        mapView.overlayManager.tilesOverlay.setColorFilter(
            if (temaOscuro) TilesOverlay.INVERT_COLORS else null
        )
        mapView.invalidate()
    }

    var forzarCentrado by remember { mutableStateOf(false) }

    LaunchedEffect(estado) {
        when (estado) {
            UbicacionUi.Buscando -> forzarCentrado = true
            is UbicacionUi.Lista -> {
                if (forzarCentrado || seleccionado == null) {
                    val destino = GeoPoint(estado.posicion.latitud, estado.posicion.longitud)
                    mapView.controller.animateTo(destino, 16.0, 600L)
                    forzarCentrado = false
                }
            }
            else -> forzarCentrado = false
        }
    }

    LaunchedEffect(seleccionado, lugares) {
        val lugar = lugares.firstOrNull { it.lugar.id == seleccionado }?.lugar
        if (lugar != null) {
            mapView.controller.animateTo(
                GeoPoint(lugar.latitud, lugar.longitud),
                17.0,
                600L
            )
        }
    }

    LaunchedEffect(estado, lugares, masCercanoId, radioMetros, tienePermiso, colorCirculo) {
        mapView.overlays.clear()

        if (estado is UbicacionUi.Lista) {
            val miGeoPoint = GeoPoint(estado.posicion.latitud, estado.posicion.longitud)
            val circulo = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(miGeoPoint, radioMetros.toDouble())
                fillPaint.color = colorCirculo.copy(alpha = 0.14f).toArgb()
                outlinePaint.color = colorCirculo.copy(alpha = 0.65f).toArgb()
                outlinePaint.strokeWidth = 3f
                infoWindow = null
            }
            mapView.overlays.add(circulo)

            val marcadorUsuario = Marker(mapView).apply {
                position = miGeoPoint
                title = "Estás aquí"
                icon = iconoUsuario
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marcadorUsuario)
        }

        lugares.forEach { item ->
            val esMasCercano = item.lugar.id == masCercanoId
            val marcadorLugar = Marker(mapView).apply {
                position = GeoPoint(item.lugar.latitud, item.lugar.longitud)
                title = item.lugar.nombre
                snippet = item.lugar.categoria
                icon = if (esMasCercano) iconoMasCercano else iconoLugar
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marcadorLugar)
        }

        if (tienePermiso) {
            if (!miUbicacionOverlay.isMyLocationEnabled) {
                miUbicacionOverlay.enableMyLocation()
            }
            mapView.overlays.add(miUbicacionOverlay)
        } else {
            if (miUbicacionOverlay.isMyLocationEnabled) {
                miUbicacionOverlay.disableMyLocation()
            }
        }

        mapView.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        if (tienePermiso) {
            SmallFloatingActionButton(
                onClick = {
                    val destino = when {
                        estado is UbicacionUi.Lista ->
                            GeoPoint(estado.posicion.latitud, estado.posicion.longitud)
                        miUbicacionOverlay.myLocation != null ->
                            miUbicacionOverlay.myLocation
                        else -> null
                    }
                    if (destino != null) {
                        mapView.controller.animateTo(destino, 16.0, 500L)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Centrar en mi ubicación"
                )
            }
        }
    }
}

private fun crearPinDrawable(contexto: Context, colorArgb: Int): Drawable {
    val densidad = contexto.resources.displayMetrics.density
    val ancho = (30 * densidad).toInt().coerceAtLeast(24)
    val alto = (38 * densidad).toInt().coerceAtLeast(30)
    val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = ancho.toFloat()
    val h = alto.toFloat()
    val cx = w / 2f
    val radio = w * 0.40f
    val cy = radio + (2f * densidad)

    val ruta = Path().apply {
        addCircle(cx, cy, radio, Path.Direction.CW)
        moveTo(cx - radio * 0.72f, cy + radio * 0.45f)
        lineTo(cx, h - (2f * densidad))
        lineTo(cx + radio * 0.72f, cy + radio * 0.45f)
        close()
    }

    val paintRelleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        style = Paint.Style.FILL
    }
    val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * densidad
    }
    val paintCentro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }

    canvas.drawPath(ruta, paintRelleno)
    canvas.drawCircle(cx, cy, radio, paintBorde)
    canvas.drawCircle(cx, cy, radio * 0.36f, paintCentro)

    return BitmapDrawable(contexto.resources, bitmap)
}
