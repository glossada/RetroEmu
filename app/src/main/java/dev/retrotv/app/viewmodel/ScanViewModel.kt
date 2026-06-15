package dev.retrotv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.retrotv.app.data.RomImporter
import dev.retrotv.app.data.db.AppDatabase
import dev.retrotv.app.data.scanner.DatParser
import dev.retrotv.app.data.scanner.RomScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    sealed class ScanState {
        object Idle : ScanState()
        data class SelectingVolume(val volumes: List<File>) : ScanState()
        data class Scanning(val current: Int, val total: Int, val system: String, val isExternal: Boolean = false) : ScanState()
        data class Done(val count: Int, val skipped: List<String> = emptyList()) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val db by lazy { AppDatabase.getInstance(getApplication()) }
    private val scanner = RomScanner()

    private val folderAliases = mapOf(
        "nes"                     to "nes",
        "famicom"                 to "nes",
        "megadrive"               to "megadrive",
        "mega drive"              to "megadrive",
        "genesis"                 to "megadrive",
        "sega"                    to "megadrive",
        "md"                      to "megadrive",
        "gba"                     to "gba",
        "game boy advance"        to "gba",
        "gameboy advance"         to "gba",
        "nds"                     to "nds",
        "ds"                      to "nds",
        "nintendo ds"             to "nds",
        "nintendo_ds"             to "nds",
        "snes"                    to "snes",
        "super nintendo"          to "snes",
        "super nes"               to "snes",
        "snes9x"                  to "snes",
        "super famicom"           to "snes",
        "super_nintendo"          to "snes",
        "ps1"                     to "ps1",
        "psx"                     to "ps1",
        "playstation"             to "ps1",
        "playstation 1"           to "ps1",
        "sony playstation"        to "ps1",
        "sony - playstation"      to "ps1",
    )

    fun startScan() {
        if (_state.value is ScanState.Scanning) return
        viewModelScope.launch {
            try {
                var total = 0
                val allSkipped = mutableListOf<String>()
                val app = getApplication<Application>()

                for (system in listOf("nes", "snes", "megadrive", "gba", "nds", "ps1")) {
                    _state.value = ScanState.Scanning(0, 0, system)

                    val datResult = withContext(Dispatchers.IO) {
                        app.assets.open("datfiles/$system.dat").use { stream ->
                            DatParser.parse(stream)
                        }
                    }

                    db.gameDao().deleteInternalGamesBySystem(system)

                    val romDir = File("/storage/emulated/0/Roms/$system")
                    val result = scanner.scanSystem(system, datResult, romDir, false) { current, max ->
                        _state.value = ScanState.Scanning(current, max, system)
                    }

                    db.gameDao().upsertGames(result.games)
                    total += result.games.size
                    allSkipped += result.skipped
                }

                _state.value = ScanState.Done(total, allSkipped)
            } catch (e: Throwable) {
                _state.value = ScanState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun startExternalScan() {
        if (_state.value is ScanState.Scanning) return
        viewModelScope.launch {
            val volumes = withContext(Dispatchers.IO) { RomImporter.findRemovableVolumes() }
            when {
                volumes.isEmpty() -> _state.value = ScanState.Error("No se encontró ningún dispositivo USB conectado.")
                volumes.size == 1 -> scanVolume(volumes.first())
                else              -> _state.value = ScanState.SelectingVolume(volumes)
            }
        }
    }

    fun scanVolume(volume: File) {
        if (_state.value is ScanState.Scanning) return
        viewModelScope.launch {
            try {
                var total = 0
                val allSkipped = mutableListOf<String>()
                val app = getApplication<Application>()

                val systemDirs = withContext(Dispatchers.IO) {
                    volume.listFiles()
                        ?.mapNotNull { dir ->
                            val system = folderAliases[dir.name.lowercase()] ?: return@mapNotNull null
                            system to dir
                        }
                        ?: emptyList()
                }

                if (systemDirs.isEmpty()) {
                    _state.value = ScanState.Error(
                        "No se encontraron carpetas de juegos en el USB.\nOrganizá los juegos en carpetas: nes/, gba/, nds/, etc."
                    )
                    return@launch
                }

                for ((system, romDir) in systemDirs) {
                    _state.value = ScanState.Scanning(0, 0, system, isExternal = true)

                    val datResult = withContext(Dispatchers.IO) {
                        app.assets.open("datfiles/$system.dat").use { stream ->
                            DatParser.parse(stream)
                        }
                    }

                    db.gameDao().deleteExternalGamesBySystem(system)

                    val result = scanner.scanSystem(system, datResult, romDir, true) { current, max ->
                        _state.value = ScanState.Scanning(current, max, system, isExternal = true)
                    }

                    db.gameDao().upsertGames(result.games)
                    total += result.games.size
                    allSkipped += result.skipped
                }

                _state.value = ScanState.Done(total, allSkipped)
            } catch (e: Throwable) {
                _state.value = ScanState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun dismissResult() {
        _state.value = ScanState.Idle
    }
}
