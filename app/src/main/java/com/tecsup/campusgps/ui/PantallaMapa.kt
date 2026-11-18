package com.tecsup.campusgps.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tecsup.campusgps.model.UbicacionUi
import com.tecsup.campusgps.util.formatearDistancia
import com.tecsup.campusgps.viewmodel.MapaViewModel

private enum class AccionUbicacion {
    BUSCAR_UNA_VEZ,
    SEGUIMIENTO
}

private fun hayPermisoUbicacion(contexto: Context): Boolean {
    val tieneFino = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val tieneAproximado = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return tieneFino || tieneAproximado
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMapa(vm: MapaViewModel) {
    val estado by vm.estado.collectAsState()
    val lugares by vm.lugares.collectAsState()
    val seleccionado by vm.seleccionado.collectAsState()
    val masCercano by vm.masCercano.collectAsState()
    val categoriaSeleccionada by vm.categoriaSeleccionada.collectAsState()
    val radioMetros by vm.radioMetros.collectAsState()
    val siguiendo by vm.siguiendo.collectAsState()
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var tienePermiso by remember { mutableStateOf(hayPermisoUbicacion(contexto)) }
    var accionPendiente by remember { mutableStateOf(AccionUbicacion.BUSCAR_UNA_VEZ) }
    val siguiendoActual by rememberUpdatedState(siguiendo)

    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val concedido = hayPermisoUbicacion(contexto)
                tienePermiso = concedido
                if (!concedido && siguiendoActual) {
                    vm.detenerSeguimiento()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        val concedido = resultado.values.any { it }
        tienePermiso = concedido
        if (concedido) {
            when (accionPendiente) {
                AccionUbicacion.BUSCAR_UNA_VEZ -> vm.buscar()
                AccionUbicacion.SEGUIMIENTO -> vm.iniciarSeguimiento()
            }
        } else {
            vm.permisoDenegado()
        }
    }

    fun ejecutarConPermiso(accion: AccionUbicacion) {
        val concedido = hayPermisoUbicacion(contexto)
        tienePermiso = concedido
        if (concedido) {
            when (accion) {
                AccionUbicacion.BUSCAR_UNA_VEZ -> vm.buscar()
                AccionUbicacion.SEGUIMIENTO -> vm.alternarSeguimiento()
            }
        } else {
            accionPendiente = accion
            lanzador.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val presionado by interactionSource.collectIsPressedAsState()
    val radioBoton by animateDpAsState(
        targetValue = if (presionado) 8.dp else 28.dp,
        animationSpec = spring(),
        label = "radioBotonPrincipal"
    )
    val escalaBoton by animateFloatAsState(
        targetValue = if (presionado) 0.96f else 1f,
        animationSpec = spring(),
        label = "escalaBotonPrincipal"
    )

    val interaccionCercano = remember { MutableInteractionSource() }
    val presionadoCercano by interaccionCercano.collectIsPressedAsState()
    val radioCercano by animateDpAsState(
        targetValue = if (presionadoCercano) 8.dp else 28.dp,
        animationSpec = spring(),
        label = "radioBotonCercano"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusGPS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapaUbicacion(
                estado = estado,
                lugares = lugares,
                seleccionado = seleccionado,
                masCercanoId = masCercano?.id,
                radioMetros = radioMetros,
                tienePermiso = tienePermiso,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { ejecutarConPermiso(AccionUbicacion.BUSCAR_UNA_VEZ) },
                    enabled = estado !is UbicacionUi.Buscando,
                    shape = RoundedCornerShape(radioBoton),
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = escalaBoton
                        scaleY = escalaBoton
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Obtener mi ubicación",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { masCercano?.let { vm.seleccionar(it.id) } },
                        enabled = masCercano != null,
                        shape = RoundedCornerShape(radioCercano),
                        interactionSource = interaccionCercano,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ir al más cercano",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    FilterChip(
                        selected = siguiendo,
                        onClick = {
                            if (siguiendo) {
                                vm.detenerSeguimiento()
                            } else {
                                ejecutarConPermiso(AccionUbicacion.SEGUIMIENTO)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (siguiendo) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (siguiendo) "Detener seguimiento" else "Iniciar seguimiento",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }

                val itemMasCercano = masCercano?.let { lugar ->
                    lugares.firstOrNull { it.lugar.id == lugar.id }
                }
                if (itemMasCercano?.metros != null) {
                    Text(
                        text = "Más cercano: ${itemMasCercano.lugar.nombre} (${formatearDistancia(itemMasCercano.metros)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                SelectorRadio(
                    radios = vm.opcionesRadio,
                    radioSeleccionado = radioMetros,
                    onSeleccionarRadio = { vm.seleccionarRadio(it) }
                )

                when (val actual = estado) {
                    UbicacionUi.Inicial -> {
                        Text(
                            text = "Pulsa el botón para empezar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    UbicacionUi.Buscando -> {
                        CircularProgressIndicator()
                    }
                    is UbicacionUi.Lista -> Unit
                    is UbicacionUi.Error -> {
                        MensajeError(
                            error = actual,
                            onReintentar = { ejecutarConPermiso(AccionUbicacion.BUSCAR_UNA_VEZ) }
                        )
                    }
                }
            }

            FiltroCategorias(
                categorias = vm.categorias,
                seleccionada = categoriaSeleccionada,
                onSeleccionar = { vm.seleccionarCategoria(it) }
            )

            ListaLugares(
                lugares = lugares,
                seleccionado = seleccionado,
                onClick = { vm.seleccionar(it) },
                masCercanoId = masCercano?.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
            )
        }
    }
}

@Composable
private fun SelectorRadio(
    radios: List<Int>,
    radioSeleccionado: Int,
    onSeleccionarRadio: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            text = "Radio:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        radios.forEach { radio ->
            val seleccionado = radio == radioSeleccionado
            FilterChip(
                selected = seleccionado,
                onClick = { onSeleccionarRadio(radio) },
                label = { Text("$radio m") }
            )
        }
    }
}

@Composable
private fun FiltroCategorias(
    categorias: List<String>,
    seleccionada: String,
    onSeleccionar: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = categorias, key = { it }) { categoria ->
            val activo = categoria == seleccionada
            val radioEsquina by animateDpAsState(
                targetValue = if (activo) 20.dp else 8.dp,
                animationSpec = spring(),
                label = "radioChipCategoria"
            )
            FilterChip(
                selected = activo,
                onClick = { onSeleccionar(categoria) },
                shape = RoundedCornerShape(radioEsquina),
                label = {
                    Text(
                        text = categoria,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    AnimatedVisibility(visible = activo) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}
