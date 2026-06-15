package dev.retrotv.app.data.scanner

import dev.retrotv.app.data.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipFile

class RomScanner {

    companion object {
        private val EXTENSIONS = mapOf(
            "nes"       to setOf("nes"),
            "megadrive" to setOf("md", "gen", "bin", "smd"),
            "gba"       to setOf("gba"),
            "nds"       to setOf("nds", "dsi"),
        )
    }

    data class ScanResult(val games: List<Game>, val skipped: List<String>)

    suspend fun scanSystem(
        system: String,
        datResult: DatParser.ParseResult,
        romDir: File,
        isExternal: Boolean = false,
        onProgress: suspend (current: Int, total: Int) -> Unit,
    ): ScanResult = withContext(Dispatchers.IO) {
        if (!romDir.exists()) return@withContext ScanResult(emptyList(), emptyList())

        val validExts = EXTENSIONS[system] ?: emptySet()
        val files = romDir.walkTopDown()
            .filter { it.isFile }
            .filter { f -> f.extension.lowercase().let { it in validExts || it == "zip" } }
            .toList()

        val games   = mutableListOf<Game>()
        val skipped = mutableListOf<String>()

        files.forEachIndexed { index, file ->
            onProgress(index + 1, files.size)
            val game = processFile(file, system, datResult, isExternal)
            if (game != null) games.add(game) else skipped.add(file.name)
        }

        ScanResult(games, skipped)
    }

    private fun processFile(file: File, system: String, dat: DatParser.ParseResult, isExternal: Boolean): Game? = try {
        val crcsToTry: List<String> = when {
            file.extension.lowercase() == "zip" -> {
                val bytes = readZipBytes(file) ?: return null
                if (system == "nes" && bytes.size > 16 && bytes.isNesHeader())
                    listOf(bytes.copyOfRange(16, bytes.size).crc32(), bytes.crc32())
                else
                    listOf(bytes.crc32())
            }
            system == "nes" -> {
                // NES ROMs son pequeñas (<1 MB), se puede cargar en memoria
                val bytes = file.readBytes()
                if (bytes.size > 16 && bytes.isNesHeader())
                    listOf(bytes.copyOfRange(16, bytes.size).crc32(), bytes.crc32())
                else
                    listOf(bytes.crc32())
            }
            else -> {
                // GBA (hasta 32 MB) y NDS (hasta 512 MB): CRC32 en streaming para evitar OOM
                listOf(file.streamCrc32())
            }
        }

        // 1. Coincidencia exacta por CRC
        val exactMatch = crcsToTry.firstNotNullOfOrNull { crc -> dat.byCrc[crc]?.let { crc to it } }

        val (crc32, canonicalName) = exactMatch ?: run {
            // 2. Fallback por nombre
            val rawName    = file.nameWithoutExtension
            val normalized = DatParser.normalizeTitle(rawName)
            crcsToTry.first() to (dat.byName[normalized] ?: cleanName(rawName))
        }

        Game(
            id            = "$system:$crc32:${if (isExternal) "e" else "i"}",
            system        = system,
            filePath      = file.absolutePath,
            crc32         = crc32,
            canonicalName = canonicalName,
            thumbnailPath = null,
            lastPlayedAt  = null,
            isExternal    = isExternal,
        )
    } catch (_: Throwable) {
        // OOM, corrupción u otro error: saltar este archivo sin crashear
        null
    }

    // CRC32 en chunks de 64 KB — no carga el archivo completo en RAM
    private fun File.streamCrc32(): String {
        val crc    = CRC32()
        val buffer = ByteArray(65536)
        inputStream().buffered(65536).use { stream ->
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                crc.update(buffer, 0, read)
            }
        }
        return crc.value.toString(16).padStart(8, '0').lowercase()
    }

    private fun readZipBytes(file: File): ByteArray? =
        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .firstOrNull { !it.isDirectory }
                ?.let { zip.getInputStream(it).readBytes() }
        }

    private fun ByteArray.crc32(): String {
        val crc = CRC32()
        crc.update(this)
        return crc.value.toString(16).padStart(8, '0').lowercase()
    }

    private fun ByteArray.isNesHeader(): Boolean =
        this[0] == 'N'.code.toByte() &&
        this[1] == 'E'.code.toByte() &&
        this[2] == 'S'.code.toByte() &&
        this[3] == 0x1A.toByte()

    private fun cleanName(raw: String): String =
        raw.replace(Regex("""[\(\[][^\)\]]*[\)\]]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
