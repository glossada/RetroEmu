package dev.retrotv.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.retrotv.app.R
import dev.retrotv.app.viewmodel.ImportViewModel
import java.io.File
import dev.retrotv.app.viewmodel.ScanViewModel

@Composable
fun HomeScreen(
    onConsoleSelected: (system: String) -> Unit = {},
    onScanClick: () -> Unit = {},
    onScanUsbClick: () -> Unit = {},
    onSelectScanVolume: (java.io.File) -> Unit = {},
    scanState: ScanViewModel.ScanState = ScanViewModel.ScanState.Idle,
    onDismissScan: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onSelectVolume: (java.io.File) -> Unit = {},
    importState: ImportViewModel.ImportState = ImportViewModel.ImportState.Idle,
    onDismissImport: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sidebar izquierda: título + botones de acción
            Column(
                modifier = Modifier.width(180.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "RetroGameTV",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Escanear ROMs")
                }
                Button(onClick = onScanUsbClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Escanear USB")
                }
                Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Importar USB")
                }
            }

            Spacer(modifier = Modifier.width(40.dp))

            // Grilla 2×2 de consolas
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ConsoleCard(
                        name = "NES / Famicom",
                        iconRes = R.drawable.ic_nes,
                        cardColor = Color(0xFF9B1A1A),
                        onClick = { onConsoleSelected("nes") },
                        modifier = Modifier.weight(1f),
                    )
                    ConsoleCard(
                        name = "Mega Drive",
                        iconRes = R.drawable.ic_megadrive,
                        cardColor = Color(0xFF1A3A9B),
                        onClick = { onConsoleSelected("megadrive") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ConsoleCard(
                        name = "Game Boy Advance",
                        iconRes = R.drawable.ic_gba,
                        cardColor = Color(0xFF4A2080),
                        onClick = { onConsoleSelected("gba") },
                        modifier = Modifier.weight(1f),
                    )
                    ConsoleCard(
                        name = "Nintendo DS",
                        iconRes = R.drawable.ic_nds,
                        cardColor = Color(0xFF1A7A3A),
                        onClick = { onConsoleSelected("nds") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (scanState !is ScanViewModel.ScanState.Idle) {
            ScanOverlay(state = scanState, onSelectVolume = onSelectScanVolume, onDismiss = onDismissScan)
        }
        if (importState !is ImportViewModel.ImportState.Idle) {
            ImportOverlay(state = importState, onSelectVolume = onSelectVolume, onDismiss = onDismissImport)
        }
    }
}

@Composable
private fun ScanOverlay(
    state: ScanViewModel.ScanState,
    onSelectVolume: (File) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Intercept ALL key events so nothing behind the overlay reacts
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .onKeyEvent { true },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(420.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state) {
                    is ScanViewModel.ScanState.SelectingVolume -> {
                        Text("Selecciona el USB", style = MaterialTheme.typography.titleMedium)
                        state.volumes.forEachIndexed { i, vol ->
                            Button(
                                onClick = { onSelectVolume(vol) },
                                modifier = if (i == 0) Modifier.fillMaxWidth().focusRequester(focusRequester) else Modifier.fillMaxWidth(),
                            ) { Text(vol.name) }
                        }
                        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }
                    is ScanViewModel.ScanState.Scanning -> {
                        val systemLabel = when (state.system) {
                            "nes" -> "NES"; "megadrive" -> "Mega Drive"; "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; else -> state.system
                        }
                        val source = if (state.isExternal) " (USB)" else ""
                        Text(
                            text = "Escaneando $systemLabel$source\u2026",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.total > 0) {
                            Text(
                                text = "${state.current} de ${state.total}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            ProgressBar(fraction = state.current.toFloat() / state.total)
                        } else {
                            Text(
                                text = "Leyendo cat\u00e1logo\u2026",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        // Absorb focus during scan so nothing behind is reachable
                        Box(modifier = Modifier.focusRequester(focusRequester).focusable())
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }
                    is ScanViewModel.ScanState.Done -> {
                        Text(
                            text = "Escaneo completado",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${state.count} juegos encontrados",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (state.skipped.isNotEmpty()) {
                            Text(
                                text = "${state.skipped.size} archivo(s) omitido(s):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            state.skipped.take(5).forEach { name ->
                                Text(
                                    text = "• $name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.skipped.size > 5) {
                                Text(
                                    text = "… y ${state.skipped.size - 5} más",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(focusRequester),
                        ) { Text("Cerrar") }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }
                    is ScanViewModel.ScanState.Error -> {
                        Text(
                            text = "Error en el escaneo",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(focusRequester),
                        ) { Text("Cerrar") }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }
                    ScanViewModel.ScanState.Idle -> {}
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color.DarkGray, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun ConsoleCard(
    name: String,
    iconRes: Int,
    cardColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(130.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ImportOverlay(
    state: ImportViewModel.ImportState,
    onSelectVolume: (File) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .onKeyEvent { true },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(460.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state) {
                    is ImportViewModel.ImportState.SelectingVolume -> {
                        Text("Selecciona el dispositivo", style = MaterialTheme.typography.titleMedium)
                        state.volumes.forEachIndexed { i, vol ->
                            Button(
                                onClick = { onSelectVolume(vol) },
                                modifier = if (i == 0) Modifier.focusRequester(focusRequester) else Modifier,
                            ) { Text(vol.name) }
                        }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }

                    is ImportViewModel.ImportState.Copying -> {
                        val systemLabel = when (state.system) {
                            "nes" -> "NES"; "megadrive" -> "Mega Drive"; "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; else -> state.system
                        }
                        Text("Copiando $systemLabel…", style = MaterialTheme.typography.titleMedium)
                        if (state.total > 0) {
                            Text("${state.copied} de ${state.total} archivos")
                            ProgressBar(fraction = state.copied.toFloat() / state.total)
                        }
                        Box(modifier = Modifier.focusRequester(focusRequester).focusable())
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }

                    is ImportViewModel.ImportState.Done -> {
                        Text("Importación completada", style = MaterialTheme.typography.titleMedium)
                        state.results.forEach { (system, count) ->
                            val label = when (system) {
                                "nes" -> "NES"; "megadrive" -> "Mega Drive"; "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; else -> system
                            }
                            Text("$label: $count archivos copiados", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "Ahora presiona Escanear ROMs para actualizar la lista.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(focusRequester),
                        ) { Text("Cerrar") }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }

                    is ImportViewModel.ImportState.Error -> {
                        Text("Error", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(focusRequester),
                        ) { Text("Cerrar") }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                    }

                    else -> {}
                }
            }
        }
    }
}
