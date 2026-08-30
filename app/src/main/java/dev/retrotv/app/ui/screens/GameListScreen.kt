package dev.retrotv.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.retrotv.app.R
import dev.retrotv.app.data.CoreExtractor
import dev.retrotv.app.data.ThumbnailUrlBuilder
import dev.retrotv.app.data.model.Game
import dev.retrotv.app.ui.CrtOverlay
import dev.retrotv.app.viewmodel.BiosImportViewModel
import dev.retrotv.app.viewmodel.GameListViewModel
import java.io.File

private sealed class PendingDelete {
    data class Selected(val count: Int) : PendingDelete()
    data class All(val count: Int) : PendingDelete()
}

@Composable
fun GameListScreen(
    system: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onLaunchEmulator: (corePath: String, romPath: String, system: String) -> Unit = { _, _, _ -> },
) {
    val viewModel: GameListViewModel = viewModel()
    val biosViewModel: BiosImportViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(system) { viewModel.setSystem(system) }

    val games by viewModel.games.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val biosState by biosViewModel.state.collectAsState()
    val isSelectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    var isSearchEditing by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val systemTitle = when (system) {
        "nes"       -> "NES / Famicom"
        "snes"      -> "Super Nintendo"
        "megadrive" -> "Mega Drive"
        "gba"       -> "Game Boy Advance"
        "nds"       -> "Nintendo DS"
        "ps1"       -> "PlayStation"
        else        -> system.uppercase()
    }
    val cardColor = when (system) {
        "nes"       -> Color(0xFF9B1A1A)
        "snes"      -> Color(0xFF5B3A8B)
        "megadrive" -> Color(0xFF1A3A9B)
        "gba"       -> Color(0xFF4A2080)
        "nds"       -> Color(0xFF1A7A3A)
        "ps1"       -> Color(0xFF1A3A5C)
        else        -> Color(0xFF444444)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080A0D))) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        // Header
        if (isSelectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { viewModel.exitSelectionMode() }) { Text("\u2715 Cancelar") }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (selectedIds.isEmpty()) "Selecci\u00f3n" else "${selectedIds.size} seleccionados",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.weight(1f))
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = { pendingDelete = PendingDelete.Selected(selectedIds.size) },
                    ) { Text("Eliminar (${selectedIds.size})") }
                    Spacer(Modifier.width(12.dp))
                }
                Button(
                    onClick = { pendingDelete = PendingDelete.All(games.size) },
                ) { Text("Eliminar todas (${games.size})") }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onBack) { Text("\u2190 Volver") }
                Spacer(Modifier.width(24.dp))
                Text(text = systemTitle, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                if (games.isNotEmpty()) {
                    Text(text = "${games.size} juegos", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = { viewModel.enterSelectionMode() }) { Text("Gestionar") }
                    Spacer(Modifier.width(12.dp))
                }
                if (system == "ps1") {
                    Button(onClick = { biosViewModel.start() }) { Text("Importar BIOS") }
                    Spacer(Modifier.width(12.dp))
                }
                Button(onClick = onSettingsClick) { Text("⚙ Ajustes") }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Search: browse mode = Card (D-pad passes through), edit mode = TextField
        if (!isSearchEditing) {
            Card(
                onClick = { isSearchEditing = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = if (searchQuery.isEmpty())
                            "\uD83D\uDD0D  Buscar juego\u2026  (OK para escribir)"
                        else
                            "\uD83D\uDD0D  $searchQuery",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (searchQuery.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            BasicTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                            isSearchEditing = false; true
                        } else false
                    },
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Escribe para buscar\u2026",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    }
                },
            )
            LaunchedEffect(Unit) { runCatching { searchFocusRequester.requestFocus() } }
        }

        Spacer(Modifier.height(20.dp))

        if (games.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Sin juegos. Usa \"Escanear ROMs\" desde la pantalla principal.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.id }) { game ->
                    val isSelected = game.id in selectedIds
                    GameCard(
                        game = game,
                        system = system,
                        cardColor = cardColor,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(game.id)
                            } else {
                                if (!java.io.File(game.filePath).exists()) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Archivo no disponible — conectá el USB",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                    return@GameCard
                                }
                                try {
                                    val coreName = CoreExtractor.coreNameForSystem(system)
                                    val corePath = CoreExtractor.extractIfNeeded(context, coreName)
                                    onLaunchEmulator(corePath, game.filePath, system)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error al cargar: ${e.javaClass.simpleName}: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    CrtOverlay()
    if (biosState !is BiosImportViewModel.State.Idle) {
        BiosImportOverlay(
            state = biosState,
            onSelectVolume = { biosViewModel.importFrom(it) },
            onDismiss = { biosViewModel.dismiss() },
        )
    }
    if (pendingDelete != null) {
        DeleteConfirmOverlay(
            pending = pendingDelete!!,
            onConfirm = {
                when (val p = pendingDelete!!) {
                    is PendingDelete.Selected -> viewModel.deleteSelected()
                    is PendingDelete.All      -> viewModel.deleteAllGames(p.count)
                }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
    if (deleteState !is GameListViewModel.DeleteState.Idle) {
        DeleteResultOverlay(
            state = deleteState,
            onDismiss = { viewModel.dismissDelete() },
        )
    }
    } // Box
}

@Composable
private fun BiosImportOverlay(
    state: BiosImportViewModel.State,
    onSelectVolume: (File) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val fr = remember { FocusRequester() }
    val volumes = if (state is BiosImportViewModel.State.SelectingVolume) state.volumes else emptyList()
    // selection: 0..volumes.size-1 = volume, volumes.size = Cancelar
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
                    Key.DirectionDown -> { if (selection <= volumes.size) selection++
                                          if (selection > volumes.size) selection = volumes.size
                                          true }
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
                    is BiosImportViewModel.State.SelectingVolume -> {
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
                    is BiosImportViewModel.State.Copying -> {
                        Text("Copiando BIOS…", style = MaterialTheme.typography.titleMedium)
                        Text(state.fileName, style = MaterialTheme.typography.bodyLarge)
                    }
                    is BiosImportViewModel.State.Done -> {
                        Text("BIOS importada", style = MaterialTheme.typography.titleMedium)
                        state.files.forEach { name -> Text("✓ $name", style = MaterialTheme.typography.bodyLarge) }
                        Text("La BIOS quedará activa para todos los juegos de PS1.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is BiosImportViewModel.State.Error -> {
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
private fun GameCard(
    game: Game,
    system: String,
    cardColor: Color,
    onClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val thumbUrl = remember(game.canonicalName) {
        ThumbnailUrlBuilder.buildUrl(system, game.canonicalName)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.height(160.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Colored background always visible (shows through when image loading/absent)
            Box(modifier = Modifier.fillMaxSize().background(cardColor))
            // Console icon — centered, semi-transparent fallback when no thumbnail
            val iconRes = when (system) {
                "nes"       -> R.drawable.ic_nes
                "snes"      -> R.drawable.ic_snes
                "megadrive" -> R.drawable.ic_megadrive
                "gba"       -> R.drawable.ic_gba
                "nds"       -> R.drawable.ic_nds
                "ps1"       -> R.drawable.ic_ps1
                else        -> null
            }
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center)
                        .alpha(0.35f),
                )
            }
            // Cover image — transparent until loaded; covers icon when successful
            if (thumbUrl != null) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Selection overlay
            if (isSelected) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00E5FF).copy(alpha = 0.22f)))
            }
            // Selection indicator — top-left circle
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(26.dp)
                        .background(
                            if (isSelected) Color(0xFF00C853) else Color.White.copy(alpha = 0.35f),
                            androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Text("✓", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
            // USB badge — top-right corner
            if (game.isExternal) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xFFE65100), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "USB",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
            // Title strip always on top
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = game.canonicalName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmOverlay(
    pending: PendingDelete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 0 = Cancelar (default — require explicit move to confirm for safety)
    var selection by remember { mutableStateOf(0) }
    val fr = remember { FocusRequester() }
    val (title, message) = when (pending) {
        is PendingDelete.Selected -> Pair(
            "Eliminar ${pending.count} juego${if (pending.count != 1) "s" else ""}",
            "Se eliminarán los archivos de ROM del dispositivo y sus registros de la base de datos.\nEsta acción no se puede deshacer.",
        )
        is PendingDelete.All -> Pair(
            "Eliminar todos (${pending.count})",
            "Se eliminarán todos los archivos de ROM de esta consola y sus registros de la base de datos.\nEsta acción no se puede deshacer.",
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .focusRequester(fr)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent true
                when (ev.key) {
                    Key.DirectionLeft  -> { selection = 0; true }
                    Key.DirectionRight -> { selection = 1; true }
                    Key.Enter, Key.DirectionCenter, Key.ButtonA ->
                        { if (selection == 0) onDismiss() else onConfirm(); true }
                    Key.Back, Key.ButtonB -> { onDismiss(); true }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(modifier = Modifier.width(440.dp), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).alpha(if (selection == 0) 1f else 0.4f),
                    ) { Text("◀  Cancelar") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).alpha(if (selection == 1) 1f else 0.4f),
                    ) { Text("Confirmar  ▶") }
                }
                Text(
                    "◀ ▶ para elegir   •   A / OK para confirmar   •   B / Atrás para cancelar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    }
}

@Composable
private fun DeleteResultOverlay(
    state: GameListViewModel.DeleteState,
    onDismiss: () -> Unit,
) {
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .focusRequester(fr)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent true
                when (ev.key) {
                    Key.Enter, Key.DirectionCenter, Key.ButtonA,
                    Key.Back, Key.ButtonB -> { onDismiss(); true }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(modifier = Modifier.width(380.dp), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state) {
                    is GameListViewModel.DeleteState.Deleting -> {
                        Text("Eliminando...", style = MaterialTheme.typography.titleMedium)
                    }
                    is GameListViewModel.DeleteState.Done -> {
                        Text("Listo", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.count} juego${if (state.count != 1) "s" else ""} eliminado${if (state.count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(onClick = onDismiss) { Text("Cerrar") }
                        Text("A / OK / Atrás para cerrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is GameListViewModel.DeleteState.Error -> {
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
