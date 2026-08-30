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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.retrotv.app.R
import dev.retrotv.app.viewmodel.ExportViewModel
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
    onExportClick: () -> Unit = {},
    onSelectExportVolume: (java.io.File) -> Unit = {},
    exportState: ExportViewModel.ExportState = ExportViewModel.ExportState.Idle,
    onDismissExport: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080A0D))) {
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
                    text = "RETRO",
                    style = TextStyle(
                        color = Color(0xFF00E5FF),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 5.sp,
                        shadow = Shadow(
                            color = Color(0xFF00E5FF),
                            offset = Offset.Zero,
                            blurRadius = 32f,
                        ),
                    ),
                )
                Text(
                    text = "GAME TV",
                    style = TextStyle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        shadow = Shadow(
                            color = Color(0xFF00E5FF),
                            offset = Offset.Zero,
                            blurRadius = 20f,
                        ),
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(1.dp)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.45f)),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF001824),
                        contentColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0xFF00E5FF),
                        focusedContentColor = Color(0xFF000A10),
                    ),
                ) {
                    Text("Escanear ROMs", letterSpacing = 0.5.sp)
                }
                Button(
                    onClick = onScanUsbClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF001824),
                        contentColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0xFF00E5FF),
                        focusedContentColor = Color(0xFF000A10),
                    ),
                ) {
                    Text("Escanear USB", letterSpacing = 0.5.sp)
                }
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF001824),
                        contentColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0xFF00E5FF),
                        focusedContentColor = Color(0xFF000A10),
                    ),
                ) {
                    Text("Importar USB", letterSpacing = 0.5.sp)
                }
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF001824),
                        contentColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0xFF00E5FF),
                        focusedContentColor = Color(0xFF000A10),
                    ),
                ) {
                    Text("Exportar USB", letterSpacing = 0.5.sp)
                }
            }

            Spacer(modifier = Modifier.width(40.dp))

            // Grilla 3×2 de consolas
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
                        name = "Super Nintendo",
                        iconRes = R.drawable.ic_snes,
                        cardColor = Color(0xFF5B3A8B),
                        onClick = { onConsoleSelected("snes") },
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
                    ConsoleCard(
                        name = "PlayStation",
                        iconRes = R.drawable.ic_ps1,
                        cardColor = Color(0xFF1A3A5C),
                        onClick = { onConsoleSelected("ps1") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // CRT scanlines overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineH = 2.dp.toPx()
            val gap = 2.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.18f),
                    topLeft = Offset(0f, y),
                    size = Size(size.width, lineH),
                )
                y += lineH + gap
            }
        }
        // CRT vignette (darkened edges)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.60f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.55f),
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.72f,
                ),
            )
        }

        if (scanState !is ScanViewModel.ScanState.Idle) {
            ScanOverlay(state = scanState, onSelectVolume = onSelectScanVolume, onDismiss = onDismissScan)
        }
        if (importState !is ImportViewModel.ImportState.Idle) {
            ImportOverlay(state = importState, onSelectVolume = onSelectVolume, onDismiss = onDismissImport)
        }
        if (exportState !is ExportViewModel.ExportState.Idle) {
            ExportOverlay(state = exportState, onSelectVolume = onSelectExportVolume, onDismiss = onDismissExport)
        }
    }
}

@Composable
private fun ScanOverlay(
    state: ScanViewModel.ScanState,
    onSelectVolume: (File) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val fr = remember { FocusRequester() }
    val volumes = if (state is ScanViewModel.ScanState.SelectingVolume) state.volumes else emptyList()
    var selection by remember(state) { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .focusRequester(fr)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent true
                when (ev.key) {
                    Key.DirectionUp   -> { if (selection > 0) selection--; true }
                    Key.DirectionDown -> { if (selection < volumes.size) selection++; true }
                    Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                        if (volumes.isNotEmpty() && selection < volumes.size) onSelectVolume(volumes[selection])
                        else onDismiss()
                        true
                    }
                    Key.Back, Key.ButtonB -> { onDismiss(); true }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(modifier = Modifier.width(420.dp), shape = RoundedCornerShape(16.dp)) {
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
                                modifier = Modifier.fillMaxWidth().alpha(if (selection == i) 1f else 0.45f),
                            ) { Text(vol.name) }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().alpha(if (selection == volumes.size) 1f else 0.45f),
                        ) { Text("Cancelar") }
                        Text("▲ ▼ para elegir   •   A / OK para confirmar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ScanViewModel.ScanState.Scanning -> {
                        val systemLabel = when (state.system) {
                            "nes" -> "NES"; "snes" -> "Super Nintendo"; "megadrive" -> "Mega Drive"
                            "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; "ps1" -> "PlayStation"; else -> state.system
                        }
                        val source = if (state.isExternal) " (USB)" else ""
                        Text("Escaneando $systemLabel$source…", style = MaterialTheme.typography.titleMedium)
                        if (state.total > 0) {
                            Text("${state.current} de ${state.total}", style = MaterialTheme.typography.bodyLarge)
                            ProgressBar(fraction = state.current.toFloat() / state.total)
                        } else {
                            Text("Leyendo catálogo…", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    is ScanViewModel.ScanState.Done -> {
                        Text("Escaneo completado", style = MaterialTheme.typography.titleMedium)
                        Text("${state.count} juegos encontrados", style = MaterialTheme.typography.bodyLarge)
                        if (state.skipped.isNotEmpty()) {
                            Text("${state.skipped.size} archivo(s) omitido(s):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            state.skipped.take(5).forEach { name ->
                                Text("• $name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (state.skipped.size > 5) {
                                Text("… y ${state.skipped.size - 5} más", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ScanViewModel.ScanState.Error -> {
                        Text("Error en el escaneo", style = MaterialTheme.typography.titleMedium)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ScanViewModel.ScanState.Idle -> {}
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
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
            // Top highlight bar (retro screen-glare effect)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.35f)),
            )
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
private fun ExportOverlay(
    state: ExportViewModel.ExportState,
    onSelectVolume: (File) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val fr = remember { FocusRequester() }
    val volumes = if (state is ExportViewModel.ExportState.SelectingVolume) state.volumes else emptyList()
    var selection by remember(state) { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .focusRequester(fr)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent true
                when (ev.key) {
                    Key.DirectionUp   -> { if (selection > 0) selection--; true }
                    Key.DirectionDown -> { if (selection < volumes.size) selection++; true }
                    Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                        if (volumes.isNotEmpty() && selection < volumes.size) onSelectVolume(volumes[selection])
                        else onDismiss()
                        true
                    }
                    Key.Back, Key.ButtonB -> { onDismiss(); true }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(modifier = Modifier.width(460.dp), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state) {
                    is ExportViewModel.ExportState.SelectingVolume -> {
                        Text("Selecciona el destino", style = MaterialTheme.typography.titleMedium)
                        state.volumes.forEachIndexed { i, vol ->
                            Button(
                                onClick = { onSelectVolume(vol) },
                                modifier = Modifier.fillMaxWidth().alpha(if (selection == i) 1f else 0.45f),
                            ) { Text(vol.name) }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().alpha(if (selection == volumes.size) 1f else 0.45f),
                        ) { Text("Cancelar") }
                        Text("▲ ▼ para elegir   •   A / OK para confirmar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ExportViewModel.ExportState.Copying -> {
                        val systemLabel = when (state.system) {
                            "nes" -> "NES"; "snes" -> "Super Nintendo"; "megadrive" -> "Mega Drive"
                            "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; "ps1" -> "PlayStation"; else -> state.system
                        }
                        Text("Exportando $systemLabel…", style = MaterialTheme.typography.titleMedium)
                        if (state.total > 0) {
                            Text("${state.copied} de ${state.total} archivos")
                            ProgressBar(fraction = state.copied.toFloat() / state.total)
                        }
                    }
                    is ExportViewModel.ExportState.Done -> {
                        Text("Exportación completada", style = MaterialTheme.typography.titleMedium)
                        state.results.forEach { (system, count) ->
                            val label = when (system) {
                                "nes" -> "NES"; "snes" -> "Super Nintendo"; "megadrive" -> "Mega Drive"
                                "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; "ps1" -> "PlayStation"; else -> system
                            }
                            Text("$label: $count archivos", style = MaterialTheme.typography.bodyLarge)
                        }
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ExportViewModel.ExportState.Error -> {
                        Text("Error", style = MaterialTheme.typography.titleMedium)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {}
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    }
}

@Composable
private fun ImportOverlay(
    state: ImportViewModel.ImportState,
    onSelectVolume: (File) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val fr = remember { FocusRequester() }
    val volumes = if (state is ImportViewModel.ImportState.SelectingVolume) state.volumes else emptyList()
    var selection by remember(state) { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .focusRequester(fr)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent true
                when (ev.key) {
                    Key.DirectionUp   -> { if (selection > 0) selection--; true }
                    Key.DirectionDown -> { if (selection < volumes.size) selection++; true }
                    Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                        if (volumes.isNotEmpty() && selection < volumes.size) onSelectVolume(volumes[selection])
                        else onDismiss()
                        true
                    }
                    Key.Back, Key.ButtonB -> { onDismiss(); true }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(modifier = Modifier.width(460.dp), shape = RoundedCornerShape(16.dp)) {
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
                                modifier = Modifier.fillMaxWidth().alpha(if (selection == i) 1f else 0.45f),
                            ) { Text(vol.name) }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().alpha(if (selection == volumes.size) 1f else 0.45f),
                        ) { Text("Cancelar") }
                        Text("▲ ▼ para elegir   •   A / OK para confirmar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ImportViewModel.ImportState.Copying -> {
                        val systemLabel = when (state.system) {
                            "nes" -> "NES"; "snes" -> "Super Nintendo"; "megadrive" -> "Mega Drive"
                            "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; "ps1" -> "PlayStation"; else -> state.system
                        }
                        Text("Copiando $systemLabel…", style = MaterialTheme.typography.titleMedium)
                        if (state.total > 0) {
                            Text("${state.copied} de ${state.total} archivos")
                            ProgressBar(fraction = state.copied.toFloat() / state.total)
                        }
                    }
                    is ImportViewModel.ImportState.Done -> {
                        Text("Importación completada", style = MaterialTheme.typography.titleMedium)
                        state.results.forEach { (system, count) ->
                            val label = when (system) {
                                "nes" -> "NES"; "snes" -> "Super Nintendo"; "megadrive" -> "Mega Drive"
                                "gba" -> "Game Boy Advance"; "nds" -> "Nintendo DS"; "ps1" -> "PlayStation"; else -> system
                            }
                            Text("$label: $count archivos copiados", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Ahora presiona Escanear ROMs para actualizar la lista.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ImportViewModel.ImportState.Error -> {
                        Text("Error", style = MaterialTheme.typography.titleMedium)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {}
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    }
}

