package dev.retrotv.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RomExporter {

    private val ROM_SOURCE_BASE = File("/storage/emulated/0/Roms")

    private val SYSTEMS = listOf("nes", "snes", "megadrive", "gba", "nds", "ps1")

    fun findRemovableVolumes(context: Context): List<File> =
        RomImporter.findRemovableVolumes(context)

    /**
     * Copies ROM files from the device's internal Roms folder to [volume].
     * Creates one subfolder per system (nes/, snes/, megadrive/, etc.) mirroring
     * the same names used by the importer.
     * Skips files that already exist on the destination with the same size.
     * Returns a map of system → number of files copied.
     */
    suspend fun exportTo(
        volume: File,
        onProgress: suspend (system: String, copied: Int, total: Int) -> Unit,
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Int>()

        for (system in SYSTEMS) {
            val sourceDir = File(ROM_SOURCE_BASE, system)
            if (!sourceDir.exists() || !sourceDir.isDirectory) continue

            val files = sourceDir.walkTopDown()
                .filter { it.isFile }
                .toList()

            if (files.isEmpty()) continue

            val destDir = File(volume, system).also { it.mkdirs() }
            var copied = 0

            for (file in files) {
                onProgress(system, copied, files.size)
                val dest = File(destDir, file.name)
                if (!dest.exists() || dest.length() != file.length()) {
                    file.copyTo(dest, overwrite = true)
                }
                copied++
            }

            onProgress(system, copied, files.size)
            results[system] = copied
        }

        results
    }
}
