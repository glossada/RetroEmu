# RetroGameTV — Contexto del Proyecto

## Setup para desarrollo

### Requisitos
- **Android Studio** Hedgehog o posterior (recomendado) — o JDK 17 + Android SDK por CLI
- **Android SDK** con:
  - compileSdk 35 (`SDK Platforms → Android 15`)
  - Build Tools 36.x (`SDK Tools → Android SDK Build-Tools`)
  - Platform Tools (`SDK Tools → Android SDK Platform-Tools`) — incluye `adb`
- **JDK 17** (Android Studio trae el suyo embebido)
- Dispositivo físico: Android TV box con ARM64 o ARM32, Android 8+ (API 26+)

### Clonar y abrir
```bash
git clone https://github.com/glossada/RetroEmu.git
cd RetroEmu
```
Abrí la carpeta en Android Studio → esperá que sincronice Gradle.

### Cores libretro (ya incluidos en el repo)
Los cores `.so` están commiteados en `app/src/main/assets/cores/`:
- `arm64-v8a/` y `armeabi-v7a/` — para release y boxes reales
- `app/src/debug/assets/cores/x86/` y `x86_64/` — solo para emulador Android Studio

No necesitás descargar nada extra.

### Buildear y probar

**Desde Android Studio** (recomendado):
- `Build → Build Bundle(s)/APK(s) → Build APK(s)` → debug APK para emulador
- `Build → Generate Signed Bundle/APK` → release APK para el box

**Desde terminal** (requiere `gradlew` en el repo):
```bash
./gradlew assembleRelease   # APK release ~50MB (arm64 + arm32)
./gradlew assembleDebug     # APK debug ~91MB (todos los ABIs)
./gradlew clean assembleRelease  # Build limpio si hay problemas de caché
```

### Instalar en el box (red local)
```bash
cd app/build/outputs/apk/release
python3 -m http.server 8080
# En el browser del box: http://<IP-de-tu-PC>:8080/app-release.apk
```
IP de la PC: `ip route get 1.1.1.1 | awk '{print $7; exit}'`

> **Si el box da "app not installed"**: ir a Ajustes → Apps → Package Installer → Borrar caché, luego reintentar.

### local.properties (NO commiteado)
Android Studio lo genera automáticamente con tu ruta local del SDK. Si no existe, crealo:
```
sdk.dir=/home/<usuario>/Android/Sdk
```

---


Emulador multi-consola para Android TV boxes (HDMI), construido con Jetpack Compose + libretrodroid. Se controla 100% con gamepad, sin touch.

## Stack

- **Kotlin + Jetpack Compose** (androidx.tv.material3)
- **libretrodroid** — wrapper de libretro para Android
- **Room** (versión 2) — base de datos local de juegos
- **Coil** — carga de thumbnails
- **OkHttp** — descarga de thumbnails desde libretro-thumbnails
- **KSP** — procesador de anotaciones para Room
- **R8/ProGuard** activado en release

## Consolas soportadas

| Sistema | Core libretro | Extensiones |
|---------|---------------|-------------|
| NES | fceumm | .nes, .zip |
| Mega Drive | genesis_plus_gx | .md, .gen, .bin, .smd, .zip |
| GBA | mgba | .gba, .zip |
| NDS | melonds | .nds, .dsi, .zip |

## Arquitectura de carpetas clave

```
app/src/main/java/dev/retrotv/app/
├── MainActivity.kt                  # Activity principal, navegación entre pantallas
├── data/
│   ├── model/Game.kt                # Entidad Room: id, system, filePath, crc32, canonicalName, thumbnailPath, lastPlayedAt, isExternal
│   ├── db/AppDatabase.kt            # Room DB versión 2, con MIGRATION_1_2
│   ├── db/GameDao.kt                # CRUD + deleteInternal/ExternalGamesBySystem
│   ├── CoreExtractor.kt             # Mapea system → nombre del .so del core; extrae de assets
│   ├── ConsoleSettings.kt           # Botones habilitados por consola
│   ├── ThumbnailUrlBuilder.kt       # Construye URL de thumbnail desde libretro-thumbnails
│   ├── RomImporter.kt               # Copia ROMs desde USB a /storage/emulated/0/Roms/
│   └── scanner/
│       ├── DatParser.kt             # Parsea archivos .dat (ClrMamePro) para CRC lookup
│       └── RomScanner.kt            # Escanea directorio, calcula CRC32, crea Game objects
├── viewmodel/
│   ├── ScanViewModel.kt             # Estado de escaneo (interno + USB); SelectingVolume, Scanning, Done, Error
│   ├── ImportViewModel.kt           # Estado de importación USB (copia física de archivos)
│   ├── GameListViewModel.kt         # Lista de juegos + búsqueda
│   └── PermissionViewModel.kt       # Permiso MANAGE_EXTERNAL_STORAGE
└── ui/
    ├── EmulatorActivity.kt          # Lanza libretrodroid con el core y ROM seleccionados
    └── screens/
        ├── HomeScreen.kt            # Pantalla principal: sidebar + grilla 2×2 de consolas
        ├── GameListScreen.kt        # Lista de juegos con badge USB, búsqueda, thumbnails
        ├── ConsoleSettingsScreen.kt # Remapeo de botones por consola
        └── OnboardingScreen.kt      # Pide permiso de almacenamiento

app/src/main/assets/
├── cores/arm64-v8a/     # 5 cores (.so) para ARM64
├── cores/armeabi-v7a/   # 5 cores (.so) para ARM32
└── datfiles/            # nes.dat, megadrive.dat, gba.dat, nds.dat

app/src/debug/assets/
└── cores/x86/ y x86_64/ # Solo para emulador Android Studio (no se incluyen en release)
```

## Base de datos

- **Room versión 2** (fue versión 1 antes de agregar `isExternal`)
- Migración `MIGRATION_1_2`: `ALTER TABLE games ADD COLUMN isExternal INTEGER NOT NULL DEFAULT 0`
- ID de juego: `"$system:$crc32:${if (isExternal) "e" else "i"}"`
  - Sufijo `:i` = interno (almacenamiento del dispositivo)
  - Sufijo `:e` = externo (USB)
- Juegos internos y externos coexisten en la misma tabla sin pisarse

## Escaneo de ROMs

### Interno (`startScan()`)
- Lee `/storage/emulated/0/Roms/<system>/`
- Borra solo juegos internos del sistema (`deleteInternalGamesBySystem`)
- Crea Games con `isExternal = false`

### Externo/USB (`startExternalScan()` → `scanVolume()`)
- Detecta volúmenes en `/storage/` (excluye `emulated`)
- Si hay un solo volumen, va directo; si hay varios, muestra selector
- Reconoce subcarpetas por alias: `nds/`, `Nintendo DS/`, `gba/`, `Game Boy Advance/`, etc.
- Borra solo juegos externos del sistema (`deleteExternalGamesBySystem`)
- Crea Games con `isExternal = true`, `filePath` apunta al USB
- Al lanzar un juego USB, verifica que `File(game.filePath).exists()` antes de abrir el emulador

## NDS — configuración especial

```kotlin
// EmulatorActivity.kt — variables melonDS
variables = arrayOf(
    Variable("melonds_screen_layout", "Left/Right", ""),  // Pantallas lado a lado (mejor para TV landscape)
    Variable("melonds_touch_mode",    "Joystick",    ""),  // Stick derecho = cursor, R3 = toque
)
// Aspect ratio: 512×192 (dos pantallas 256×192 side by side)
// R3 (KEYCODE_BUTTON_THUMBR) se intercepta en dispatchKeyEvent para el toque táctil
```

## CRC32 y manejo de archivos grandes

- **ZIP y NES**: `readBytes()` en memoria (archivos chicos)
- **GBA, NDS y resto**: `File.streamCrc32()` en chunks de 64KB → evita OOM en ROMs de hasta 512MB
- Errores de archivo: `catch (_: Throwable)` (no `Exception`) para capturar `OutOfMemoryError`

## Build

```bash
# Requiere gradlew (copiado de otro proyecto + gradle-wrapper.jar)
./gradlew assembleRelease      # APK release ~50MB
./gradlew assembleDebug        # APK debug ~91MB (incluye cores x86/x86_64)
./gradlew clean assembleRelease # Build limpio (usar cuando hay problemas)
```

- **Release**: arm64-v8a + armeabi-v7a, minificado con R8, firmado con debug keystore
- **Debug**: todos los ABIs, para emulador Android Studio
- Lint desactivado en release (`abortOnError = false`)

### Firma
- Usa `~/.android/debug.keystore` (contraseña: `android`, alias: `androiddebugkey`)
- Solo V2 signing (AGP 8.x ignora V1 cuando minSdk ≥ 24)
- El box acepta V2-only

### Servir APK al box (red local)
```bash
cd app/build/outputs/apk/release
python3 -m http.server 8080
# Box descarga desde: http://192.168.1.2:8080/app-release.apk
```

**Importante**: si el box da "app not installed", borrar caché del Package Installer en el box antes de intentar de nuevo (`Ajustes → Apps → Package Installer → Borrar caché`).

## Thumbnails

- URL: `https://raw.githubusercontent.com/libretro-thumbnails/<sistema>/master/Named_Boxarts/<nombre>.png`
- Mapeo de sistema a nombre de carpeta en `ThumbnailUrlBuilder.kt`
- Carga lazy con Coil + fondo de color de consola mientras carga
- **Pendiente**: los thumbnails no se muestran actualmente (bug a resolver)

## Pantallas y navegación

```
HomeScreen
├── [Escanear ROMs]   → ScanViewModel.startScan()
├── [Escanear USB]    → ScanViewModel.startExternalScan()
├── [Importar USB]    → ImportViewModel.startImport() (copia archivos al dispositivo)
├── ConsoleCard NES   → GameListScreen("nes")
├── ConsoleCard MD    → GameListScreen("megadrive")
├── ConsoleCard GBA   → GameListScreen("gba")
└── ConsoleCard NDS   → GameListScreen("nds")

GameListScreen
├── Búsqueda D-pad friendly (Card → TextField con BACK para salir)
├── Grid de GameCards (minSize 200dp)
│   ├── Badge naranja "USB" si game.isExternal
│   └── onClick → verifica existencia del archivo → lanza EmulatorActivity
└── [⚙ Ajustes] → ConsoleSettingsScreen (remapeo de botones)
```

## Errores conocidos y soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| IDE: "Unresolved reference" después de Edit | Falso positivo del analizador incremental | Ignorar — Gradle siempre compila bien |
| OOM escaneando ROMs NDS | `readBytes()` en archivos de 512MB | Usar `streamCrc32()` (ya implementado) |
| "App not installed" en box | Caché del Package Installer | Borrar caché en Ajustes → Apps → Package Installer |
| "Parsing error" al descargar APK | Corrupción por intermediarios (ej: MediaFire) | Usar solo servidor HTTP local (`python3 -m http.server`) |
| Thumbnails no cargan | Bug pendiente | No investigado aún |

## Permisos necesarios

- `MANAGE_EXTERNAL_STORAGE` — leer ROMs de `/storage/emulated/0/Roms/` y USB
- `INTERNET` — descargar thumbnails
- `WRITE_EXTERNAL_STORAGE` — fallback para Android < 11

## Estado actual (junio 2025)

- [x] NES, Mega Drive, GBA, NDS funcionando
- [x] Escaneo de ROMs internos con CRC32
- [x] Importar ROMs desde USB (copia al dispositivo)
- [x] Escanear ROMs directamente desde USB (sin copiar)
- [x] Badge "USB" en cards de juegos externos
- [x] Verificación de archivo antes de lanzar juego USB
- [x] HomeScreen con sidebar + grilla 2×2
- [x] Búsqueda de juegos con D-pad
- [x] Remapeo de botones por consola
- [ ] Thumbnails (pendiente)
