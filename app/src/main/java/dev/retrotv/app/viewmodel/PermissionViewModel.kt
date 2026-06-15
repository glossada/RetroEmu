package dev.retrotv.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class PermissionState {
    data object Granted : PermissionState()
    data object NotGranted : PermissionState()
}

class PermissionViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<PermissionState>(PermissionState.NotGranted)
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    fun checkPermission() {
        if (hasStoragePermission()) {
            ensureRomFoldersExist()
            _state.value = PermissionState.Granted
        } else {
            _state.value = PermissionState.NotGranted
        }
    }

    private fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun ensureRomFoldersExist() {
        listOf("nes", "megadrive", "gba", "nds").forEach { system ->
            File(Environment.getExternalStorageDirectory(), "Roms/$system").mkdirs()
        }
    }
}
