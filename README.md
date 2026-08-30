# RetroGameTV

Emulador multi-consola para **Android TV boxes** (HDMI). Diseñado para controlarse completamente con gamepad — sin touch, sin ratón.

---

## Consolas soportadas

| Consola | Core |
|---------|------|
| NES / Famicom | Nestopia |
| Super Nintendo | Snes9x |
| Mega Drive / Genesis | Genesis Plus GX |
| Game Boy Advance | mGBA |
| Nintendo DS | melonDS |
| PlayStation 1 | PCSX-ReARMed |

---

## Características

- **Interfaz TV-first** — navegación 100% con D-pad y botones, sin touch
- **Escaneo de ROMs** — detecta y nombra juegos automáticamente vía CRC32 + base de datos `.dat`
- **USB directo** — juega desde USB sin necesidad de copiar al dispositivo
- **Importar / Exportar** — copia ROMs entre el dispositivo y un USB
- **Save states** — guardado automático al salir y quick save/load desde el menú in-game
- **Aspect ratio** — pantalla completa, 4:3 clásico o pixel-perfect por consola
- **Remapeo de botones** — configurable por consola
- **Thumbnails** — portadas de juegos cargadas automáticamente desde libretro-thumbnails

---

## Stack

- **Kotlin + Jetpack Compose** (`androidx.tv.material3`)
- **libretrodroid** — wrapper de libretro para Android
- **Room** — base de datos local de juegos
- **Coil** — carga de thumbnails
- **DataStore** — preferencias por consola (botones, aspect ratio)

---

## Requisitos

- Android TV box con **Android 8.0+** (API 26+), ARM64 o ARM32
- Gamepad USB o Bluetooth
- Para PS1: BIOS de PlayStation (importable desde USB dentro de la app)

---

## Build

```bash
# APK release (~50 MB, arm64 + arm32)
./gradlew assembleRelease

# APK debug (~90 MB, todos los ABIs — para emulador Android Studio)
./gradlew assembleDebug
```

### Instalar en el box por red local

```bash
cd app/build/outputs/apk/release
python3 -m http.server 8080
# En el browser del box: http://<IP-de-tu-PC>:8080/app-release.apk
```

---

## Organización de ROMs

Colocá las ROMs en el almacenamiento interno del box o en un USB con esta estructura:

```
Roms/
├── nes/
├── snes/
├── megadrive/
├── gba/
├── nds/
└── ps1/
```

Luego usá **Escanear ROMs** (interno) o **Escanear USB** desde la pantalla principal.
