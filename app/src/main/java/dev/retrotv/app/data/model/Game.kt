package dev.retrotv.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey val id: String,         // "{system}:{crc32hex}:{i|e}"  (i=internal, e=external/USB)
    val system: String,
    val filePath: String,
    val crc32: String,
    val canonicalName: String,
    val thumbnailPath: String?,
    val lastPlayedAt: Long?,
    val isExternal: Boolean = false,
)
