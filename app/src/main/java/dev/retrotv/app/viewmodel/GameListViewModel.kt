package dev.retrotv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.retrotv.app.data.db.AppDatabase
import dev.retrotv.app.data.model.Game
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameListViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { AppDatabase.getInstance(getApplication()) }

    private val systemFlow = MutableStateFlow("")
    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: StateFlow<List<Game>> = systemFlow
        .flatMapLatest { system ->
            if (system.isEmpty()) flowOf(emptyList())
            else db.gameDao().getGamesBySystem(system)
        }
        .combine(searchQuery) { gameList, query ->
            if (query.isBlank()) gameList
            else gameList.filter { it.canonicalName.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selection mode ---
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    // --- Delete state ---
    sealed class DeleteState {
        object Idle : DeleteState()
        object Deleting : DeleteState()
        data class Done(val count: Int) : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun setSystem(system: String) { systemFlow.value = system }

    fun setSearch(query: String) { searchQuery.value = query }

    fun enterSelectionMode() {
        searchQuery.value = ""  // Clear search so all games are visible and countable
        _selectionMode.value = true
        _selectedIds.value = emptySet()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(gameId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (gameId in current) current - gameId else current + gameId
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        val gamesToDelete = games.value.filter { it.id in ids }
        if (gamesToDelete.isEmpty()) return
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            try {
                gamesToDelete.forEach { game -> runCatching { File(game.filePath).delete() } }
                db.gameDao().deleteGamesByIds(gamesToDelete.map { it.id })
                _deleteState.value = DeleteState.Done(gamesToDelete.size)
                exitSelectionMode()
            } catch (e: Throwable) {
                _deleteState.value = DeleteState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun deleteAllGames(totalCount: Int) {
        val system = systemFlow.value
        val gamesToDelete = games.value
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            try {
                gamesToDelete.forEach { game -> runCatching { File(game.filePath).delete() } }
                db.gameDao().deleteGamesBySystem(system)
                _deleteState.value = DeleteState.Done(totalCount)
                exitSelectionMode()
            } catch (e: Throwable) {
                _deleteState.value = DeleteState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun dismissDelete() { _deleteState.value = DeleteState.Idle }
}
