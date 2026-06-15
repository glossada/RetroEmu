package dev.retrotv.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retrotv.app.data.BiosImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BiosImportViewModel : ViewModel() {

    sealed class State {
        object Idle : State()
        data class SelectingVolume(val volumes: List<File>) : State()
        data class Copying(val fileName: String) : State()
        data class Done(val files: List<String>) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun start() {
        if (_state.value is State.Copying) return
        viewModelScope.launch {
            val volumes = withContext(Dispatchers.IO) { BiosImporter.findRemovableVolumes() }
            when {
                volumes.isEmpty() -> _state.value = State.Error("No se encontró ningún dispositivo USB conectado.")
                volumes.size == 1 -> importFrom(volumes.first())
                else              -> _state.value = State.SelectingVolume(volumes)
            }
        }
    }

    fun importFrom(volume: File) {
        if (_state.value is State.Copying) return
        viewModelScope.launch {
            try {
                val copied = BiosImporter.importFrom(volume) { fileName ->
                    _state.value = State.Copying(fileName)
                }
                _state.value = if (copied.isEmpty()) {
                    State.Error(
                        "No se encontraron archivos BIOS de PS1 en el USB.\n" +
                        "Archivos buscados: scph1001.bin, scph5501.bin, etc."
                    )
                } else {
                    State.Done(copied)
                }
            } catch (e: Throwable) {
                _state.value = State.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun dismiss() { _state.value = State.Idle }
}
