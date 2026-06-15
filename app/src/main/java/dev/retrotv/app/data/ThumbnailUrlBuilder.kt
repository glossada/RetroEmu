package dev.retrotv.app.data

import android.net.Uri

object ThumbnailUrlBuilder {

    private val SYSTEM_SEGMENT = mapOf(
        "nes"       to "Nintendo - Nintendo Entertainment System",
        "megadrive" to "Sega - Mega Drive - Genesis",
        "gba"       to "Nintendo - Game Boy Advance",
        "nds"       to "Nintendo - Nintendo DS",
    )

    // Characters that libretro-thumbnails replaces with underscore in filenames
    private val INVALID_CHARS = Regex("""[&*/:'"<>?\\|]""")

    fun buildUrl(system: String, canonicalName: String): String? {
        val systemName = SYSTEM_SEGMENT[system] ?: return null
        val sanitized  = canonicalName.replace(INVALID_CHARS, "_")
        // Uri.encode percent-encodes everything except letters, digits and -_.~
        val encodedSystem = Uri.encode(systemName)
        val encodedName   = Uri.encode(sanitized)
        return "https://thumbnails.libretro.com/$encodedSystem/Named_Boxarts/$encodedName.png"
    }
}
