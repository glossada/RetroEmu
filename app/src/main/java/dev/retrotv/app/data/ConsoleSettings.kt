package dev.retrotv.app.data

import android.view.KeyEvent
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object ConsoleSettings {

    // ── Aspect ratio ──────────────────────────────────────────────────────────

    enum class AspectRatio(val key: String, val label: String) {
        FULL("full", "Pantalla completa"),
        FOUR_THREE("4_3", "4:3 (clásico)"),
        INTEGER_SCALE("integer", "Pixel-perfect");

        companion object {
            fun fromKey(k: String) = entries.find { it.key == k } ?: FULL
        }
    }

    fun aspectKey(system: String): Preferences.Key<String> =
        stringPreferencesKey("aspect_ratio_$system")

    // ── Button mapping ────────────────────────────────────────────────────────

    val ALL_ACTIONS = listOf(
        "a", "b", "x", "y", "l", "r",
        "start", "select",
        "up", "down", "left", "right",
    )

    fun actionsForSystem(system: String): List<String> = when (system) {
        "nes" -> listOf("a", "b", "select", "start", "up", "down", "left", "right")
        "gba" -> listOf("a", "b", "l", "r", "select", "start", "up", "down", "left", "right")
        "nds" -> listOf("a", "b", "x", "y", "l", "r", "select", "start", "up", "down", "left", "right")
        else  -> ALL_ACTIONS
    }

    fun actionLabel(action: String, system: String): String = when (action) {
        "a"      -> if (system == "megadrive") "C" else "A"
        "b"      -> "B"
        "x"      -> if (system == "megadrive") "X" else "L"
        "y"      -> if (system == "megadrive") "A" else "R"
        "l"      -> if (system == "megadrive") "Y" else "L"
        "r"      -> if (system == "megadrive") "Z" else "R"
        "start"  -> "Start"
        "select" -> if (system == "megadrive") "Mode" else "Select"
        "up"     -> "D-Pad ↑"
        "down"   -> "D-Pad ↓"
        "left"   -> "D-Pad ←"
        "right"  -> "D-Pad →"
        else     -> action
    }

    // Physical key that triggers each joypad action by default.
    // A/B and X/Y swapped to match standard Android gamepad conventions.
    val DEFAULTS: Map<String, Int> = mapOf(
        "a"      to KeyEvent.KEYCODE_BUTTON_B,
        "b"      to KeyEvent.KEYCODE_BUTTON_A,
        "x"      to KeyEvent.KEYCODE_BUTTON_Y,
        "y"      to KeyEvent.KEYCODE_BUTTON_X,
        "l"      to KeyEvent.KEYCODE_BUTTON_L1,
        "r"      to KeyEvent.KEYCODE_BUTTON_R1,
        "start"  to KeyEvent.KEYCODE_BUTTON_START,
        "select" to KeyEvent.KEYCODE_BUTTON_SELECT,
        "up"     to KeyEvent.KEYCODE_DPAD_UP,
        "down"   to KeyEvent.KEYCODE_DPAD_DOWN,
        "left"   to KeyEvent.KEYCODE_DPAD_LEFT,
        "right"  to KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    // Key code sent to GLRetroView.sendKeyEvent for each action.
    private val NATIVE_CODES: Map<String, Int> = mapOf(
        "a"      to KeyEvent.KEYCODE_BUTTON_A,
        "b"      to KeyEvent.KEYCODE_BUTTON_B,
        "x"      to KeyEvent.KEYCODE_BUTTON_X,
        "y"      to KeyEvent.KEYCODE_BUTTON_Y,
        "l"      to KeyEvent.KEYCODE_BUTTON_L1,
        "r"      to KeyEvent.KEYCODE_BUTTON_R1,
        "start"  to KeyEvent.KEYCODE_BUTTON_START,
        "select" to KeyEvent.KEYCODE_BUTTON_SELECT,
        "up"     to KeyEvent.KEYCODE_DPAD_UP,
        "down"   to KeyEvent.KEYCODE_DPAD_DOWN,
        "left"   to KeyEvent.KEYCODE_DPAD_LEFT,
        "right"  to KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    fun mapKey(system: String, action: String): Preferences.Key<Int> =
        intPreferencesKey("${system}_map_$action")

    /** Builds physical→native key map from a DataStore snapshot. */
    fun buildKeyMap(system: String, prefs: Preferences): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (action in ALL_ACTIONS) {
            val physical = prefs[mapKey(system, action)] ?: DEFAULTS[action] ?: continue
            val native   = NATIVE_CODES[action] ?: continue
            result[physical] = native
        }
        return result
    }

    /** Returns the key map using only defaults (no DataStore lookup). */
    fun defaultKeyMap(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (action in ALL_ACTIONS) {
            val physical = DEFAULTS[action] ?: continue
            val native   = NATIVE_CODES[action] ?: continue
            result[physical] = native
        }
        return result
    }

    fun keyName(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A      -> "A"
        KeyEvent.KEYCODE_BUTTON_B      -> "B"
        KeyEvent.KEYCODE_BUTTON_X      -> "X"
        KeyEvent.KEYCODE_BUTTON_Y      -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1     -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1     -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2     -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2     -> "R2"
        KeyEvent.KEYCODE_BUTTON_START  -> "Start"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
        KeyEvent.KEYCODE_DPAD_UP       -> "↑"
        KeyEvent.KEYCODE_DPAD_DOWN     -> "↓"
        KeyEvent.KEYCODE_DPAD_LEFT     -> "←"
        KeyEvent.KEYCODE_DPAD_RIGHT    -> "→"
        else                           -> "[$keyCode]"
    }
}
