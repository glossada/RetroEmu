package dev.retrotv.app.ui

sealed class AppScreen {
    data object Home : AppScreen()
    data class GameList(val system: String) : AppScreen()
    data class ConsoleSettings(val system: String) : AppScreen()
}
