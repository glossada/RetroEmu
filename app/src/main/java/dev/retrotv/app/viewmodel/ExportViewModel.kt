package dev.retrotv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.retrotv.app.data.RomExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    sealed class ExportState {
        object Idle : ExportState()
        data class SelectingVolume(val volumes: List<File>) : ExportState()
        data class Copying(val system: String, val copied: Int, val total: Int) : ExportState()
        data class Done(val results: Map<String, Int>) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun startExport() {
        if (_state.value !is ExportState.Idle) return
        val volumes = RomExporter.findRemovableVolumes(getApplication())
        when {
            volumes.isEmpty() -> _state.value = ExportState.Error(
                "No se encontró ningún USB ni tarjeta SD conectada.\n" +
                "Conecta el dispositivo y vuelve a intentarlo."
            )
            volumes.size == 1 -> exportTo(volumes.first())
            else              -> _state.value = ExportState.SelectingVolume(volumes)
        }
    }

    fun selectVolume(volume: File) = exportTo(volume)

    private fun exportTo(volume: File) {
        viewModelScope.launch {
            try {
                val results = RomExporter.exportTo(volume) { system, copied, total ->
                    _state.value = ExportState.Copying(system, copied, total)
                }
                _state.value = if (results.isEmpty())
                    ExportState.Error(
                        "No hay ROMs en el dispositivo para exportar.\n" +
                        "Importa juegos primero usando \"Importar USB\"."
                    )
                else
                    ExportState.Done(results)
            } catch (e: Exception) {
                _state.value = ExportState.Error(e.message ?: "Error al exportar archivos")
            }
        }
    }

    fun dismiss() { _state.value = ExportState.Idle }
}
