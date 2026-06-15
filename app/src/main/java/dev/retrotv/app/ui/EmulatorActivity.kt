package dev.retrotv.app.ui

import android.os.Bundle
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import dev.retrotv.app.data.ConsoleSettings
import dev.retrotv.app.data.dataStore
import dev.retrotv.app.data.db.AppDatabase
import dev.retrotv.app.ui.screens.InGameMenu
import dev.retrotv.app.ui.theme.RetroGameTVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class EmulatorActivity : ComponentActivity() {

    private lateinit var retroView: GLRetroView
    private lateinit var stateFile: File
    private lateinit var srmFile: File
    private lateinit var system: String
    private lateinit var romPath: String

    private var keyMap: Map<Int, Int> = ConsoleSettings.defaultKeyMap()
    private var coreReady = false

    private var showMenu by mutableStateOf(false)
    private var menuSelection by mutableIntStateOf(0)
    private var currentAspectRatio by mutableStateOf(AspectRatio.FULL)
    private var showResumeDialog by mutableStateOf(false)
    private var resumeSelection by mutableIntStateOf(0)  // 0=Continuar, 1=Nueva partida

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val corePath = intent.getStringExtra(EXTRA_CORE_PATH) ?: run { finish(); return }
        romPath      = intent.getStringExtra(EXTRA_ROM_PATH)  ?: run { finish(); return }
        system       = intent.getStringExtra(EXTRA_SYSTEM) ?: "nes"

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val saveKey = File(romPath).nameWithoutExtension
        val saveDir = File(filesDir, "saves/$system").also { it.mkdirs() }
        stateFile = File(saveDir, "$saveKey.state")
        srmFile   = File(saveDir, "$saveKey.srm")

        val data = GLRetroViewData(this).apply {
            coreFilePath    = corePath
            gameFilePath    = romPath
            systemDirectory = filesDir.absolutePath
            savesDirectory  = saveDir.absolutePath
            saveRAMState    = if (srmFile.exists()) srmFile.readBytes() else null
            if (system == "nds") {
                variables = arrayOf(
                    Variable("melonds_screen_layout", "Left/Right", ""),
                    Variable("melonds_touch_mode",    "Joystick",    ""),
                )
            }
        }

        val root = FrameLayout(this)

        GLRetroView(this, data).also { view ->
            retroView = view
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            lifecycle.addObserver(view)
            root.addView(view, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ))
            view.requestFocus()
        }

        // Show resume dialog if save state exists; otherwise start fresh
        if (stateFile.exists()) {
            showResumeDialog = true
            resumeSelection = 0
        }

        // Allow autosave after 3s — by then the core has either loaded or failed.
        // This prevents SIGSEGV from calling serializeState() on a broken GLRetroView.
        window.decorView.postDelayed({ if (!isFinishing && !isDestroyed) coreReady = true }, 3000)

        // Load per-console settings (aspect ratio + button mapping) from DataStore
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            keyMap = ConsoleSettings.buildKeyMap(system, prefs)
            currentAspectRatio = AspectRatio.fromKey(
                prefs[ConsoleSettings.aspectKey(system)] ?: AspectRatio.FULL.key
            )
            applyAspectRatio()
        }

        ComposeView(this).also { cv ->
            cv.setContent {
                RetroGameTVTheme {
                    if (showResumeDialog) {
                        ResumeDialog(
                            selection  = resumeSelection,
                            onContinue = ::continueFromSave,
                            onNewGame  = ::onNewGame,
                        )
                    }
                    if (showMenu) {
                        InGameMenu(
                            items = buildMenuItems(),
                            selectedIndex = menuSelection,
                        )
                    }
                }
            }
            root.addView(cv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }

        setContentView(root)
    }

    override fun onPause() {
        autosaveSync()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        // Update lastPlayedAt in the database
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                AppDatabase.getInstance(this@EmulatorActivity)
                    .gameDao()
                    .updateLastPlayedByPath(romPath, System.currentTimeMillis())
            }
        }
    }

    // ----- Resume dialog actions -----

    private fun continueFromSave() {
        coreReady = true
        showResumeDialog = false
        lifecycleScope.launch {
            kotlinx.coroutines.delay(300)
            runCatching { retroView.unserializeState(stateFile.readBytes()) }
        }
    }

    private fun onNewGame() {
        coreReady = true
        showResumeDialog = false
        stateFile.delete()
        srmFile.delete()
    }

    // ----- Key dispatch -----

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action

        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (showResumeDialog && action == KeyEvent.ACTION_DOWN) return true
            if (showMenu && action == KeyEvent.ACTION_DOWN) { showMenu = false; return true }
            return super.dispatchKeyEvent(event)
        }

        if (showResumeDialog) {
            if (action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT  -> resumeSelection = 0
                    KeyEvent.KEYCODE_DPAD_RIGHT -> resumeSelection = 1
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_BUTTON_A   -> if (resumeSelection == 0) continueFromSave() else onNewGame()
                }
            }
            return true
        }

        if (event.keyCode == KeyEvent.KEYCODE_MENU && action == KeyEvent.ACTION_DOWN) {
            showMenu = !showMenu
            return true
        }

        if (showMenu) {
            if (action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP ->
                        menuSelection = (menuSelection - 1 + MENU_SIZE) % MENU_SIZE
                    KeyEvent.KEYCODE_DPAD_DOWN ->
                        menuSelection = (menuSelection + 1) % MENU_SIZE
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_BUTTON_A -> invokeMenuAction()
                }
            }
            return true
        }

        val nativeCode = keyMap[event.keyCode]
        if (nativeCode != null && ::retroView.isInitialized) {
            retroView.sendKeyEvent(action, nativeCode)
            return true
        }

        // NDS: right stick click (R3) = touch press in melonDS joystick mode
        if (system == "nds" && event.keyCode == KeyEvent.KEYCODE_BUTTON_THUMBR && ::retroView.isInitialized) {
            retroView.sendKeyEvent(action, KeyEvent.KEYCODE_BUTTON_THUMBR)
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!::retroView.isInitialized || showMenu || showResumeDialog) {
            return super.dispatchGenericMotionEvent(event)
        }
        // libretrodroid.onGenericMotionEvent routes input to the port derived from
        // event.device.controllerNumber (controller 2 → port 1 = player 2).
        // Force everything to port 0 (player 1) so single-player games work with any USB pad.
        if (event.source and InputDevice.SOURCE_JOYSTICK != 0) {
            retroView.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_DPAD,
                event.getAxisValue(MotionEvent.AXIS_HAT_X),
                event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                0,
            )
            retroView.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                event.getAxisValue(MotionEvent.AXIS_X),
                event.getAxisValue(MotionEvent.AXIS_Y),
                0,
            )
            retroView.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                event.getAxisValue(MotionEvent.AXIS_Z),
                event.getAxisValue(MotionEvent.AXIS_RZ),
                0,
            )
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    // ----- Menu actions -----

    private fun invokeMenuAction() {
        when (menuSelection) {
            0 -> quickSave()
            1 -> quickLoad()
            2 -> cycleAspectRatio()
            3 -> autosaveAndExit()
        }
    }

    private fun quickSave() {
        runCatching {
            stateFile.writeBytes(retroView.serializeState())
            srmFile.writeBytes(retroView.serializeSRAM())
        }
        showMenu = false
    }

    private fun quickLoad() {
        if (stateFile.exists()) {
            runCatching { retroView.unserializeState(stateFile.readBytes()) }
        }
        showMenu = false
    }

    private fun cycleAspectRatio() {
        val next = when (currentAspectRatio) {
            AspectRatio.FULL          -> AspectRatio.FOUR_THREE
            AspectRatio.FOUR_THREE    -> AspectRatio.INTEGER_SCALE
            AspectRatio.INTEGER_SCALE -> AspectRatio.FULL
        }
        currentAspectRatio = next
        applyAspectRatio()
        lifecycleScope.launch {
            dataStore.edit { prefs ->
                prefs[ConsoleSettings.aspectKey(system)] = next.key
            }
        }
    }

    private fun autosaveAndExit() {
        showMenu = false
        finish()
    }

    // ----- Helpers -----

    private fun autosaveSync() {
        if (!::retroView.isInitialized || !coreReady) return
        runCatching {
            stateFile.writeBytes(retroView.serializeState())
            srmFile.writeBytes(retroView.serializeSRAM())
        }
    }

    private fun applyAspectRatio() {
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val params = FrameLayout.LayoutParams(0, 0).apply {
            gravity = Gravity.CENTER
        }
        when (currentAspectRatio) {
            AspectRatio.FULL -> {
                params.width  = FrameLayout.LayoutParams.MATCH_PARENT
                params.height = FrameLayout.LayoutParams.MATCH_PARENT
            }
            AspectRatio.FOUR_THREE -> {
                params.height = screenH
                params.width  = (screenH * 4f / 3f).toInt()
            }
            AspectRatio.INTEGER_SCALE -> {
                val (nativeW, nativeH) = when (system) {
                    "megadrive" -> 320 to 224
                    "gba"       -> 240 to 160
                    "nds"       -> 512 to 192   // Left/Right layout: two 256×192 screens side by side
                    else        -> 256 to 240
                }
                val scale = minOf(screenW / nativeW, screenH / nativeH).coerceAtLeast(1)
                params.width  = nativeW * scale
                params.height = nativeH * scale
            }
        }
        retroView.layoutParams = params
    }

    private fun buildMenuItems(): List<String> {
        val nextAr = when (currentAspectRatio) {
            AspectRatio.FULL          -> AspectRatio.FOUR_THREE
            AspectRatio.FOUR_THREE    -> AspectRatio.INTEGER_SCALE
            AspectRatio.INTEGER_SCALE -> AspectRatio.FULL
        }
        return listOf(
            "Quick Save",
            "Quick Load${if (stateFile.exists()) "" else "  (sin guardado)"}",
            "Aspecto: ${currentAspectRatio.label} \u2192 ${nextAr.label}",
            "Salir (con autosave)",
        )
    }



    // ----- Enums & constants -----

    enum class AspectRatio(val key: String, val label: String) {
        FULL("full", "Completo"),
        FOUR_THREE("4_3", "4:3"),
        INTEGER_SCALE("integer", "Pixel-perfect");

        companion object {
            fun fromKey(k: String) = entries.find { it.key == k } ?: FULL
        }
    }

    companion object {
        const val EXTRA_CORE_PATH = "extra_core_path"
        const val EXTRA_ROM_PATH  = "extra_rom_path"
        const val EXTRA_SYSTEM    = "extra_system"

        private const val MENU_SIZE = 4


    }
}

// Shown as overlay when a save state exists for this game
@androidx.compose.runtime.Composable
private fun ResumeDialog(selection: Int, onContinue: () -> Unit, onNewGame: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .onKeyEvent { true },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("¿Continuar partida?", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "← → para elegir   •   OK para confirmar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectableDialogButton(
                        label     = "▶  Continuar",
                        selected  = selection == 0,
                        onClick   = onContinue,
                    )
                    SelectableDialogButton(
                        label     = "↺  Nueva partida",
                        selected  = selection == 1,
                        onClick   = onNewGame,
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SelectableDialogButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = if (selected)
            Modifier
                .border(3.dp, Color.White, RoundedCornerShape(50.dp))
                .padding(4.dp)
        else
            Modifier.alpha(0.4f),
    ) {
        androidx.tv.material3.Button(
            onClick = onClick,
            scale = if (selected)
                androidx.tv.material3.ButtonDefaults.scale(scale = 1.08f)
            else
                androidx.tv.material3.ButtonDefaults.scale(scale = 1f),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
