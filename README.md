# CampusGPS

Aplicación Android desarrollada en **Kotlin**, **Jetpack Compose** y **Material 3 Expressive** (sin XML de vistas) que obtiene la ubicación actual del usuario mediante **Google Play Services Location (`FusedLocationProviderClient`)**, la muestra en un mapa interactivo con **Maps Compose** y calcula la distancia hacia cinco puntos del campus ordenándolos del más cercano al más lejano bajo arquitectura **MVVM + Repository**.

---

## Arquitectura MVVM + Repository

```text
+-------------------------------------------------------------------+
|                   View (ui/ - Jetpack Compose)                    |
|   PantallaMapa · MapaUbicacion · ListaLugares · MensajeError      |
|                                                                   |
|   * Observa StateFlow con collectAsState()                        |
|   * Envía eventos de usuario (buscar, seleccionar) al ViewModel   |
|   * Sin lógica de negocio, sin cálculo de distancias, sin Repos   |
+---------------------------------+---------------------------------+
                                  |
                     evento       |       StateFlow (estado, lugares,
                   al ViewModel   |       seleccionado, masCercano)
                                  v
+---------------------------------+---------------------------------+
|                    ViewModel (MapaViewModel)                      |
|   * Depende únicamente de interfaces (sin Context ni Composables) |
|   * Valida Play Services, ejecuta búsqueda y actualiza distancias |
|   * Expone estado inmutable vía StateFlow                         |
+-----------------+-------------------------------+-----------------+
                  |                               |
                  v                               v
+---------------------------------+   +-----------------------------+
|     UbicacionRepository         |   |     LugaresRepository       |
|         (interface)             |   |         (interface)         |
+-----------------+---------------+   +---------------+-------------+
                  |                                   |
                  v                                   v
+---------------------------------+   +-----------------------------+
|   UbicacionRepositoryGoogle     |   |   LugaresRepositoryLocal    |
|  * FusedLocationProviderClient  |   |  * 5 puntos del campus      |
|  * GoogleApiAvailability        |   |    (datos ficticios)        |
+---------------------------------+   +-----------------------------+
                  ^
                  |   funciones puras sin estado
+-----------------+-------------------------------------------------+
|            util/ (Distancia.kt, Formato.kt)                       |
|   distanciaMetros() -> Location.distanceBetween                   |
|   formatearDistancia() -> "350 m" / "1.5 km"                      |
+-------------------------------------------------------------------+
```

---

## Árbol de paquetes (`com.tecsup.campusgps`)

```text
com.tecsup.campusgps/
├── MainActivity.kt
├── model/
│   └── Modelos.kt                  # Posicion, Lugar, LugarConDistancia, TipoError, UbicacionUi
├── data/
│   ├── UbicacionRepository.kt      # Interfaz de ubicación y disponibilidad de Play Services
│   ├── UbicacionRepositoryGoogle.kt# Implementación con FusedLocationProviderClient
│   ├── LugaresRepository.kt        # Interfaz de lugares del campus
│   └── LugaresRepositoryLocal.kt   # Repositorio local con los 5 lugares del campus
├── util/
│   ├── Distancia.kt                # Función pura distanciaMetros(desde, hasta)
│   └── Formato.kt                  # Función pura formatearDistancia(metros)
├── viewmodel/
│   ├── MapaViewModel.kt            # Estado UI, búsqueda, ordenamiento y lugar más cercano
│   └── CampusViewModelFactory.kt   # Factory para inyectar implementaciones de Repository
└── ui/
    ├── PantallaMapa.kt             # Pantalla principal, solicitud de permisos y controles
    ├── MapaUbicacion.kt            # Mapa GoogleMap, cámara animada y marcadores
    ├── ListaLugares.kt             # LazyColumn con tarjetas expresivas y selección
    ├── MensajeError.kt             # Tarjeta de error con acción (Abrir ajustes / Reintentar)
    └── theme/
        ├── Color.kt                # Esquema de color Material 3 (claro y oscuro)
        ├── Type.kt                 # Escala tipográfica Material 3 Expressive
        └── Theme.kt                # MaterialExpressiveTheme con MotionScheme.expressive()
```

---

## Capturas de pantalla

| 1. Solicitud de permiso | 2. Ubicación actual en el mapa | 3. Marcadores del campus |
| :---: | :---: | :---: |
| ![Solicitud de permiso](docs/capturas/01-permiso.png) | ![Ubicación actual](docs/capturas/02-ubicacion.png) | ![Marcadores del campus](docs/capturas/03-marcadores.png) |

| 4. Lista ordenada y más cercano | 5. Manejo de errores |
| :---: | :---: |
| ![Lista ordenada](docs/capturas/04-lista-ordenada.png) | ![Estado de error](docs/capturas/05-error.png) |

---

## Para pensar (respuestas técnicas)

### 1. ¿Por qué `FusedLocationProviderClient` está dentro del Repository y no en el ViewModel?
Porque `FusedLocationProviderClient` pertenece a la capa de infraestructura/datos de Android y depende directamente de un `Context` y de APIs de Google Play Services. Si el ViewModel lo instanciara o recibiera un `Context`:
- Se acoplaría al framework de Android, dificultando las pruebas unitarias (en un test de JVM no existe `FusedLocationProviderClient` ni `Context` real).
- Rompería el principio de inversión de dependencias y la separación MVVM + Repository: el ViewModel solo debe conocer la interfaz `UbicacionRepository`, lo que permite reemplazar `UbicacionRepositoryGoogle` por un repositorio falso (`FakeUbicacionRepository`) en pruebas o cambiar el proveedor de ubicación sin tocar el ViewModel ni la UI.

### 2. ¿Qué diferencia hay entre el permiso de ubicación y la clave de API de Maps? ¿Qué pasa si falta cada uno?
Son mecanismos de seguridad independientes para servicios distintos:
- **Permiso de ubicación (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`):** Es una autorización de **privacidad en tiempo de ejecución** otorgada por el usuario al sistema operativo Android para leer las coordenadas GPS/red del dispositivo. Si falta o es denegado, `FusedLocationProviderClient` lanza `SecurityException` y la app no puede obtener la posición del usuario (aunque el mapa sí puede seguir mostrándose).
- **Clave de API de Google Maps (`MAPS_API_KEY`):** Es una credencial de **autenticación y facturación ante Google Cloud** restringida por nombre de paquete y huella SHA-1 para autorizar el uso del SDK de mapas y la descarga de teselas cartográficas. Si falta en `AndroidManifest.xml`, la app puede lanzar un error al inicializar el mapa; si es inválida o carece de permisos/restricción correcta, el mapa aparece en blanco o gris y Logcat registra `Authorization failure`.

### 3. ¿Por qué la clave va en `local.properties` y no en el código? ¿Qué harías si se subiera a GitHub por error?
- `local.properties` está excluido del control de versiones en `.gitignore`, por lo que la clave permanece únicamente en la máquina local de desarrollo y se inyecta en tiempo de compilación mediante `manifestPlaceholders["MAPS_API_KEY"]` sin quedar expuesta en el código fuente ni en el historial de Git.
- Si una clave se subiera por error a GitHub:
  1. **Revocar/eliminar inmediatamente** la clave comprometida en Google Cloud Console (pues cualquier persona o bot que inspeccione el historial público ya pudo capturarla).
  2. **Generar una nueva clave de API** restringida exclusivamente a **Maps SDK for Android**, al paquete `com.tecsup.campusgps` y a la huella SHA-1 correspondiente.
  3. Guardar la nueva clave solo en `local.properties`, verificar que `.gitignore` la excluya y limpiar el historial de Git (por ejemplo con `git filter-repo`) si el repositorio lo requiere.

### 4. ¿Dónde calculas la distancia y por qué no en el Composable?
La función pura `distanciaMetros(desde, hasta)` vive en `util/Distancia.kt` y es invocada exclusivamente desde `MapaViewModel.actualizarDistancias(pos)` al recibir una nueva posición con éxito.
No debe calcularse dentro de un `@Composable` porque:
- Los Composables se recomponen frecuentemente (por animaciones, desplazamientos o cambios de estado); calcular y ordenar distancias en cada recomposición desperdiciaría CPU y podría desordenar la lista inesperadamente.
- Rompería la arquitectura **MVVM** (Flujo Unidireccional de Datos): la Vista debe ser declarativa y limitarse a dibujar el estado (`StateFlow<List<LugarConDistancia>>`) ya procesado por el ViewModel, además de permitir probar la lógica de cálculo y ordenamiento sin renderizar UI.

### 5. ¿Qué pasa con tu app si el usuario niega el permiso dos veces? ¿Cómo lo manejas?
En Android, cuando el usuario rechaza el permiso de ubicación dos veces (o selecciona *"No volver a preguntar"*), el sistema marca el permiso como denegado permanentemente: llamadas posteriores a `lanzador.launch(...)` no muestran el diálogo del sistema y devuelven inmediatamente `false` en el callback de `RequestMultiplePermissions`.
En **CampusGPS** esto se maneja sin cierres ni bloqueos:
1. El callback detecta que ningún permiso fue concedido y llama a `vm.permisoDenegado()`.
2. `MapaViewModel` emite `UbicacionUi.Error(..., TipoError.SIN_PERMISO)`.
3. `MensajeError` muestra el aviso claro en pantalla junto al botón **"Abrir ajustes"**, que lanza un `Intent` con `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` apuntando al paquete de la aplicación (`Uri.fromParts("package", contexto.packageName, null)`), permitiendo al usuario habilitar el permiso manualmente en Ajustes del sistema y volver a usar la app.
