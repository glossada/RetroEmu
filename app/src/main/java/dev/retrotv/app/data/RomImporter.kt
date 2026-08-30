package dev.retrotv.app.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RomImporter {

    private val ROM_DEST_BASE = File("/storage/emulated/0/Roms")

    // Folder names accepted on the USB drive → internal system name
    private val FOLDER_ALIASES = mapOf(
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

    /** Returns removable volumes (USB drives, SD cards) currently mounted. */
    fun findRemovableVolumes(context: Context): List<File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sm = context.getSystemService(StorageManager::class.java)
            sm.storageVolumes
                .filter { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
                .mapNotNull { it.directory }
        } else {
            File("/storage").listFiles()
                ?.filter { it.isDirectory && it.name != "emulated" }
                ?: emptyList()
        }
    }

    /**
     * Copies ROM files from [volume] subfolders into the app's ROM directories.
     * Recognises folder names listed in [FOLDER_ALIASES].
     * Returns a map of system → number of files copied.
     */
    suspend fun importFrom(
        volume: File,
        onProgress: suspend (copied: Int, total: Int, system: String) -> Unit,
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Int>()

        val sourceFolders = volume.listFiles()
            ?.mapNotNull { dir ->
                val system = FOLDER_ALIASES[dir.name.lowercase()] ?: return@mapNotNull null
                system to dir
            }
            ?: emptyList()

        for ((system, sourceDir) in sourceFolders) {
            val files = sourceDir.walkTopDown()
                .filter { it.isFile }
                .toList()

            if (files.isEmpty()) continue

            val destDir = File(ROM_DEST_BASE, system).also { it.mkdirs() }
            var copied = 0

            for (file in files) {
                onProgress(copied, files.size, system)
                val dest = File(destDir, file.name)
                if (!dest.exists() || dest.length() != file.length()) {
                    file.copyTo(dest, overwrite = true)
                }
                copied++
            }

            onProgress(copied, files.size, system)
            results[system] = copied
        }

        results
    }
}
