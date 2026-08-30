package dev.retrotv.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.retrotv.app.data.model.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // NULLs last: (lastPlayedAt IS NULL)=1 sorts after 0 (non-null)
    @Query("""
        SELECT * FROM games
        WHERE system = :system
        ORDER BY (lastPlayedAt IS NULL), lastPlayedAt DESC, canonicalName ASC
    """)
    fun getGamesBySystem(system: String): Flow<List<Game>>

    @Upsert
    suspend fun upsertGame(game: Game)

    @Upsert
    suspend fun upsertGames(games: List<Game>)

    @Query("DELETE FROM games WHERE system = :system")
    suspend fun deleteGamesBySystem(system: String)

    @Query("DELETE FROM games WHERE system = :system AND isExternal = 0")
    suspend fun deleteInternalGamesBySystem(system: String)

    @Query("DELETE FROM games WHERE system = :system AND isExternal = 1")
    suspend fun deleteExternalGamesBySystem(system: String)

    @Query("DELETE FROM games WHERE id IN (:ids)")
    suspend fun deleteGamesByIds(ids: List<String>)

    @Query("UPDATE games SET lastPlayedAt = :ts WHERE filePath = :path")
    suspend fun updateLastPlayedByPath(path: String, ts: Long)
}
