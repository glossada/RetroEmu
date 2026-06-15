package dev.retrotv.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BiosImporter {

    val SYSTEM_DIR = File("/storage/emulated/0/RetroGameTV/system")

    private val KNOWN_PS1_BIOS = setOf(
        "scph1001.bin", "scph5501.bin", "scph7001.bin", "scph101.bin",  // USA
        "scph1000.bin", "scph5500.bin", "scph5510.bin",                  // Japan
        "scph1002.bin", "scph5502.bin", "scph7502.bin",                  // Europe
    )

    fun findRemovableVolumes(): List<File> =
        File("/storage").listFiles()
            ?.filter { it.isDirectory && it.name != "emulated" && it.canRead() }
            ?: emptyList()

    fun installedBios(): List<String> =
        SYSTEM_DIR.listFiles()
            ?.filter { it.name.lowercase() in KNOWN_PS1_BIOS }
            ?.map { it.name }
            ?: emptyList()

    suspend fun importFrom(
        volume: File,
        onProgress: suspend (fileName: String) -> Unit,
    ): List<String> = withContext(Dispatchers.IO) {
        SYSTEM_DIR.mkdirs()
        val copied = mutableListOf<String>()

        volume.walkTopDown()
            .filter { it.isFile && it.name.lowercase() in KNOWN_PS1_BIOS }
            .forEach { biosFile ->
                onProgress(biosFile.name)
                val dest = File(SYSTEM_DIR, biosFile.name.lowercase())
                if (!dest.exists() || dest.length() != biosFile.length()) {
                    biosFile.copyTo(dest, overwrite = true)
                }
                copied.add(biosFile.name)
            }

        copied
    }
}
