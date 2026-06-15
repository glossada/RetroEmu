package dev.retrotv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.retrotv.app.data.ConsoleSettings
import dev.retrotv.app.data.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ConsoleSettingsScreen(
    system: String,
    onBack: () -> Unit,
    onRegisterCapture: (((Int) -> Unit)?) -> Unit = {},
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val systemTitle = when (system) {
        "nes"       -> "NES / Famicom"
        "megadrive" -> "Mega Drive"
        "gba"       -> "Game Boy Advance"
        else        -> system.uppercase()
    }
    val actions     = ConsoleSettings.actionsForSystem(system)

    // Live settings from DataStore
    val aspectRatio by remember(system) {
        context.dataStore.data.map { prefs ->
            ConsoleSettings.AspectRatio.fromKey(
                prefs[ConsoleSettings.aspectKey(system)] ?: ConsoleSettings.AspectRatio.FULL.key
            )
        }
    }.collectAsState(initial = ConsoleSettings.AspectRatio.FULL)

    val physicalKeys by remember(system) {
        context.dataStore.data.map { prefs ->
            actions.associate { action ->
                action to (prefs[ConsoleSettings.mapKey(system, action)]
                    ?: ConsoleSettings.DEFAULTS[action]
                    ?: 0)
            }
        }
    }.collectAsState(initial = actions.associateWith { ConsoleSettings.DEFAULTS[it] ?: 0 })

    // Capture mode: which action is waiting for a key press
    var capturingAction by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack) { Text("← Volver") }
                Spacer(Modifier.width(20.dp))
                Text("Ajustes — $systemTitle", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.height(8.dp))

            // ── Aspect ratio ─────────────────────────────────────────────────
            Text("Relación de aspecto", style = MaterialTheme.typography.titleMedium)

            ConsoleSettings.AspectRatio.entries.forEach { option ->
                val selected = aspectRatio == option
                Card(
                    onClick = {
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[ConsoleSettings.aspectKey(system)] = option.key
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (selected)
                        CardDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else
                        CardDefaults.colors(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (selected) "●" else "○", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(16.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Button mapping ───────────────────────────────────────────────
            Text("Mapeo de botones", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Selecciona una fila y presiona OK para reasignar ese botón.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            actions.forEach { action ->
                val label      = ConsoleSettings.actionLabel(action, system)
                val currentKey = physicalKeys[action] ?: ConsoleSettings.DEFAULTS[action] ?: 0
                ButtonMappingRow(
                    actionLabel = label,
                    keyName     = ConsoleSettings.keyName(currentKey),
                    onActivate  = { capturingAction = action },
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── Capture overlay ──────────────────────────────────────────────────
        capturingAction?.let { action ->
            val saveKey: (Int) -> Unit = { keyCode ->
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[ConsoleSettings.mapKey(system, action)] = keyCode
                    }
                }
                capturingAction = null
            }
            // Register with the Activity so gamepad KeyEvents reach us even without Compose focus.
            LaunchedEffect(action) { onRegisterCapture(saveKey) }

            CaptureOverlay(
                actionLabel = ConsoleSettings.actionLabel(action, system),
                onCapture   = saveKey,
                onCancel    = {
                    onRegisterCapture(null)
                    capturingAction = null
                },
            )
        }

        // Clear Activity-level capture when overlay is dismissed.
        if (capturingAction == null) {
            LaunchedEffect(Unit) { onRegisterCapture(null) }
        }
    }
}

@Composable
private fun ButtonMappingRow(
    actionLabel: String,
    keyName: String,
    onActivate: () -> Unit,
) {
    Card(
        onClick = onActivate,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(actionLabel, style = MaterialTheme.typography.bodyLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(6.dp),
                    ),
                ) {
                    Text(
                        text = keyName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    "Cambiar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CaptureOverlay(
    actionLabel: String,
    onCapture: (keyCode: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (event.key == Key.Back || event.key == Key.Escape) {
                        onCancel()
                    } else {
                        onCapture(event.key.keyCode.toInt())
                    }
                    true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(480.dp),
        ) {
            Column(
                modifier = Modifier.padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Asignar botón", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Presiona el botón físico que quieres usar para: $actionLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Presiona Atrás para cancelar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
}
