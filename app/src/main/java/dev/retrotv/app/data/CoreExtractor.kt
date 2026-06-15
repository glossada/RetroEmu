package dev.retrotv.app.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.IOException

object CoreExtractor {

    private val KNOWN_ABIS = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    // Prefer nativeLibraryDir to detect the RUNNING ABI: on x86_64 emulators where the APK
    // only ships x86 natives the process is 32-bit, so Build.SUPPORTED_ABIS reports "x86_64"
    // first even though the app actually runs as x86.
    // Fall back to Build.SUPPORTED_ABIS when nativeLibraryDir has an unexpected format
    // (some custom TV-box ROMs omit the ABI segment from the path).
    private fun runningAbi(context: Context): String {
        val fromLibDir = context.applicationInfo.nativeLibraryDir.substringAfterLast('/')
        if (fromLibDir in KNOWN_ABIS) return fromLibDir
        return Build.SUPPORTED_ABIS.firstOrNull { it in KNOWN_ABIS } ?: "arm64-v8a"
    }

    fun coreNameForSystem(system: String): String = when (system) {
        "megadrive" -> "genesis_plus_gx_libretro_android.so"
        "gba"       -> "mgba_libretro_android.so"
        "nds"       -> "melonds_libretro_android.so"
        "snes"      -> "snes9x_libretro_android.so"
        "ps1"       -> "pcsx_rearmed_libretro_android.so"
        else        -> "nestopia_libretro_android.so"
    }

    fun extractIfNeeded(context: Context, coreName: String): String {
        val abi       = runningAbi(context)
        val assetPath = "cores/$abi/$coreName"
        val dest      = File(context.filesDir, assetPath)

        if (!dest.exists()) {
            dest.parentFile?.mkdirs()
            try {
                context.assets.open(assetPath).use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: IOException) {
                dest.delete()
                throw e
            }
        }
        // Android 10+ W^X policy: dlopen() refuses to load files writable by the
        // calling process. Remove write permission so the linker accepts the file.
        if (dest.canWrite()) dest.setWritable(false, false)
        return dest.absolutePath
    }
}
