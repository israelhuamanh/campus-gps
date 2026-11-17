package com.tecsup.campusgps.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "campus_gps_prefs")

class PreferenciasRepositoryDataStore(
    private val context: Context
) : PreferenciasRepository {

    override val ultimoLugarId: Flow<Int?> = context.dataStore.data
        .catch { excepcion ->
            if (excepcion is IOException) {
                emit(emptyPreferences())
            } else {
                throw excepcion
            }
        }
        .map { prefs -> prefs[CLAVE_ULTIMO_LUGAR] }

    override suspend fun guardarUltimoLugar(id: Int?) {
        context.dataStore.edit { prefs ->
            if (id == null) {
                prefs.remove(CLAVE_ULTIMO_LUGAR)
            } else {
                prefs[CLAVE_ULTIMO_LUGAR] = id
            }
        }
    }

    private companion object {
        val CLAVE_ULTIMO_LUGAR = intPreferencesKey("ultimo_lugar_id")
    }
}
