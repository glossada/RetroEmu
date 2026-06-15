package dev.retrotv.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.Surface
import dev.retrotv.app.ui.AppScreen
import dev.retrotv.app.ui.EmulatorActivity
import dev.retrotv.app.ui.screens.ConsoleSettingsScreen
import dev.retrotv.app.ui.screens.GameListScreen
import dev.retrotv.app.ui.screens.HomeScreen
import dev.retrotv.app.ui.screens.OnboardingScreen
import dev.retrotv.app.ui.theme.RetroGameTVTheme
import dev.retrotv.app.viewmodel.PermissionState
import dev.retrotv.app.viewmodel.PermissionViewModel
import dev.retrotv.app.viewmodel.ImportViewModel
import dev.retrotv.app.viewmodel.ScanViewModel

class MainActivity : ComponentActivity() {

    private val permissionViewModel: PermissionViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val importViewModel: ImportViewModel by viewModels()

    // Set by ConsoleSettingsScreen when the user is remapping a button.
    // Intercepted here because Compose onKeyEvent doesn't reliably receive gamepad events on TV.
    var keyCaptureCallback: ((Int) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        keyCaptureCallback?.let { callback ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    keyCaptureCallback = null
                } else {
                    keyCaptureCallback = null
                    callback(event.keyCode)
                }
                return true
            }
            return true  // also swallow ACTION_UP while capturing
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionViewModel.checkPermission()
        setContent {
            RetroGameTVTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val permState by permissionViewModel.state.collectAsState()
                    val scanState  by scanViewModel.state.collectAsState()
                    val importState by importViewModel.state.collectAsState()
                    when (permState) {
                        PermissionState.NotGranted -> OnboardingScreen(
                            onRequestPermission = ::openStorageSettings
                        )
                        PermissionState.Granted -> {
                            var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

                            BackHandler(enabled = screen != AppScreen.Home) {
                                screen = AppScreen.Home
                            }

                            when (val s = screen) {
                                AppScreen.Home -> HomeScreen(
                                    onConsoleSelected  = { system ->
                                        screen = AppScreen.GameList(system)
                                    },
                                    onScanClick        = { scanViewModel.startScan() },
                                    onScanUsbClick     = { scanViewModel.startExternalScan() },
                                    onSelectScanVolume = { vol -> scanViewModel.scanVolume(vol) },
                                    scanState          = scanState,
                                    onDismissScan      = { scanViewModel.dismissResult() },
                                    onImportClick      = { importViewModel.startImport() },
                                    onSelectVolume     = { vol -> importViewModel.selectVolume(vol) },
                                    importState        = importState,
                                    onDismissImport    = { importViewModel.dismiss() },
                                )
                                is AppScreen.GameList -> GameListScreen(
                                    system = s.system,
                                    onBack = { screen = AppScreen.Home },
                                    onSettingsClick = { screen = AppScreen.ConsoleSettings(s.system) },
                                    onLaunchEmulator = { corePath, romPath, system ->
                                        startActivity(
                                            Intent(this@MainActivity, EmulatorActivity::class.java)
                                                .putExtra(EmulatorActivity.EXTRA_CORE_PATH, corePath)
                                                .putExtra(EmulatorActivity.EXTRA_ROM_PATH, romPath)
                                                .putExtra(EmulatorActivity.EXTRA_SYSTEM, system)
                                        )
                                    }
                                )
                                is AppScreen.ConsoleSettings -> ConsoleSettingsScreen(
                                    system = s.system,
                                    onBack = { screen = AppScreen.GameList(s.system) },
                                    onRegisterCapture = { cb -> keyCaptureCallback = cb },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionViewModel.checkPermission()
    }

    private fun openStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        }
    }
}
