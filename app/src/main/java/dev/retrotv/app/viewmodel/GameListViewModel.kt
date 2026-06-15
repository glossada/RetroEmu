package dev.retrotv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.retrotv.app.data.db.AppDatabase
import dev.retrotv.app.data.model.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

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

    fun setSystem(system: String) {
        systemFlow.value = system
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }
}
