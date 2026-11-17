package com.tecsup.campusgps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.campusgps.model.UbicacionUi
import com.tecsup.campusgps.viewmodel.MapaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMapa(vm: MapaViewModel) {
    val estado by vm.estado.collectAsState()

    // TODO 1 (Paso 2): rememberLauncherForActivityResult(RequestMultiplePermissions)
    fun alPulsar() {
        // TODO 2 (Paso 2): ¿ya tienes FINE o COARSE? -> vm.buscar()
        // TODO 3 (Paso 2): si no -> lanzador.launch(...)
        vm.buscar()
    }

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
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { alPulsar() },
                enabled = estado !is UbicacionUi.Buscando
            ) {
                Text("Obtener mi ubicación")
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                is UbicacionUi.Lista -> {
                    Text(
                        text = "Latitud: ${actual.posicion.latitud}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Longitud: ${actual.posicion.longitud}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is UbicacionUi.Error -> {
                    Text(
                        text = actual.mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
