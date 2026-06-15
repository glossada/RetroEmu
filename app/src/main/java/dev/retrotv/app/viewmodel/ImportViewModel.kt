package dev.retrotv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.retrotv.app.data.RomImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    sealed class ImportState {
        object Idle : ImportState()
        data class SelectingVolume(val volumes: List<File>) : ImportState()
        data class Copying(val copied: Int, val total: Int, val system: String) : ImportState()
        data class Done(val results: Map<String, Int>) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    /** Detects USB volumes and either auto-starts or asks the user to choose. */
    fun startImport() {
        if (_state.value !is ImportState.Idle) return
        val volumes = RomImporter.findRemovableVolumes()
        when {
            volumes.isEmpty() -> _state.value = ImportState.Error(
                "No se encontró ningún USB ni tarjeta SD conectada.\n" +
                "Conecta el dispositivo y vuelve a intentarlo."
            )
            volumes.size == 1 -> importFrom(volumes.first())
            else              -> _state.value = ImportState.SelectingVolume(volumes)
        }
    }

    fun selectVolume(volume: File) = importFrom(volume)

    private fun importFrom(volume: File) {
        viewModelScope.launch {
            try {
                val results = RomImporter.importFrom(volume) { copied, total, system ->
                    _state.value = ImportState.Copying(copied, total, system)
                }
                _state.value = if (results.isEmpty())
                    ImportState.Error(
                        "No se encontraron carpetas reconocidas en el dispositivo.\n" +
                        "Crea carpetas llamadas 'nes' y/o 'megadrive' con las ROMs dentro."
                    )
                else
                    ImportState.Done(results)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Error al copiar archivos")
            }
        }
    }

    fun dismiss() { _state.value = ImportState.Idle }
}
