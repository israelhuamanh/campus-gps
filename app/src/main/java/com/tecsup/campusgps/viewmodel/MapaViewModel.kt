package com.tecsup.campusgps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.campusgps.data.LugaresRepository
import com.tecsup.campusgps.data.UbicacionRepository
import com.tecsup.campusgps.model.Lugar
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.Posicion
import com.tecsup.campusgps.model.TipoError
import com.tecsup.campusgps.model.UbicacionUi
import com.tecsup.campusgps.util.distanciaMetros
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapaViewModel(
    private val ubicacionRepo: UbicacionRepository,
    private val lugaresRepo: LugaresRepository
) : ViewModel() {

    val categorias: List<String> = listOf("Todos", "Estudio", "Comida", "Eventos", "Servicios")
    val opcionesRadio: List<Int> = listOf(100, 200, 500)

    private val _estado = MutableStateFlow<UbicacionUi>(UbicacionUi.Inicial)
    val estado: StateFlow<UbicacionUi> = _estado.asStateFlow()

    private val _lugaresBase = MutableStateFlow(
        lugaresRepo.obtenerLugares().map { LugarConDistancia(it, null) }
    )

    private val _categoriaSeleccionada = MutableStateFlow("Todos")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    private val _radioMetros = MutableStateFlow(200)
    val radioMetros: StateFlow<Int> = _radioMetros.asStateFlow()

    val lugares: StateFlow<List<LugarConDistancia>> = combine(
        _lugaresBase,
        _categoriaSeleccionada,
        _radioMetros
    ) { lista, categoria, radio ->
        val filtrados = if (categoria == "Todos") {
            lista
        } else {
            lista.filter { it.lugar.categoria == categoria }
        }
        filtrados.map { item ->
            item.copy(dentroDelRadio = item.metros != null && item.metros <= radio)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _lugaresBase.value
    )

    private val _seleccionado = MutableStateFlow<Int?>(null)
    val seleccionado: StateFlow<Int?> = _seleccionado.asStateFlow()

    val masCercano: StateFlow<Lugar?> = lugares
        .map { lista -> lista.firstOrNull { it.metros != null }?.lugar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun buscar() {
        if (!ubicacionRepo.playServicesDisponible()) {
            _estado.value = UbicacionUi.Error(
                mensaje = "Este dispositivo no tiene Google Play Services",
                tipo = TipoError.SIN_PLAY_SERVICES
            )
            return
        }

        _estado.value = UbicacionUi.Buscando
        viewModelScope.launch {
            ubicacionRepo.obtenerPosicion()
                .onSuccess {
                    _estado.value = UbicacionUi.Lista(it)
                    actualizarDistancias(it)
                }
                .onFailure { _estado.value = errorDe(it) }
        }
    }

    private fun actualizarDistancias(pos: Posicion) {
        _lugaresBase.value = lugaresRepo.obtenerLugares()
            .map { lugar -> LugarConDistancia(lugar, distanciaMetros(pos, lugar)) }
            .sortedBy { it.metros }
    }

    fun seleccionarCategoria(categoria: String) {
        _categoriaSeleccionada.value = categoria
    }

    fun seleccionarRadio(metros: Int) {
        _radioMetros.value = metros
    }

    fun permisoDenegado() {
        _estado.value = UbicacionUi.Error(
            mensaje = "Sin permiso de ubicación no puedo mostrar dónde estás",
            tipo = TipoError.SIN_PERMISO
        )
    }

    fun seleccionar(id: Int?) {
        _seleccionado.value = id
    }

    private fun errorDe(e: Throwable): UbicacionUi.Error = when (e) {
        is SecurityException -> UbicacionUi.Error(
            mensaje = "Sin permiso de ubicación no puedo mostrar dónde estás",
            tipo = TipoError.SIN_PERMISO
        )
        else -> UbicacionUi.Error(
            mensaje = "No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo",
            tipo = TipoError.GPS_APAGADO
        )
    }
}
