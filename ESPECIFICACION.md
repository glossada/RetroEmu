# Especificación — RetroTV (nombre provisional)

Emulador para **Android TV** enfocado y simplificado, solo para **NES/Famicom** y **Sega Mega Drive/Genesis**. Es una alternativa minimalista y amigable a RetroArch: el usuario entra, elige consola, ve la lista de juegos con portadas, busca por nombre, configura su joystick y juega. Toda la emulación se apoya en **LibretroDroid** (no se reimplementan emuladores).

---

## 1. Objetivo y alcance

- Aplicación Android TV navegable 100% con D-pad/control remoto.
- Dos consolas: **NES/Famicom** y **Mega Drive/Genesis**.
- Flujo del usuario:
  1. Coloca las ROMs en carpetas pre-establecidas del almacenamiento interno.
  2. Abre la app, presiona **Escanear**.
  3. La app identifica cada juego, descarga su portada y lo agrega al listado de su consola.
  4. El usuario entra a una consola, ve el grid con título + portada, busca por nombre, y lanza el juego.
  5. Configura (global por consola) el mapeo de botones y el aspect ratio.
  6. Juega, con autosave al salir y quick save / quick load durante la partida.

## 2. Fuera de alcance (MVP)

- Cualquier consola que no sea NES o Mega Drive (nada de SNES, GBA, PS1, etc.).
- Netplay, cloud sync, logros, shaders avanzados, cheats.
- Descarga o gestión de ROMs (las provee el usuario; **la app nunca incluye ni descarga ROMs**).
- Soporte táctil (es una app de TV; el touchscreen no es requerido).

## 3. Stack técnico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose for TV (`androidx.tv:tv-material` 1.x) |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Motor de emulación | LibretroDroid (`com.github.swordfish90:libretrodroid`) vía JitPack |
| Cores | Nestopia UE (NES, default) + fceumm (NES, respaldo) · Genesis Plus GX (Mega Drive) |
| Base de datos local | Room (librería de juegos) |
| Preferencias | DataStore (mapeo de botones, aspect ratio por consola) |
| Carga de imágenes | Coil (con caché en disco) |
| Red | OkHttp (descarga de portadas) |
| Async | Coroutines + Flow |
| DI | Hilt (opcional en MVP; se puede arrancar con DI manual) |
| minSdk | 26 (Android 8) |
| targetSdk | la más reciente disponible (boxes corren Android 13/14) |
| ABI objetivo | arm64-v8a (boxes Android 13/14) |

> **Verificar antes de empezar:** la última versión publicada de LibretroDroid en su repo de GitHub / JitPack, y la versión estable actual de `androidx.tv:tv-material` y del Compose BOM.

## 4. Arquitectura

MVVM en capas:

- **UI (Compose for TV):** pantallas y componentes navegables por foco/D-pad. Sin lógica de negocio.
- **ViewModels:** exponen estado vía `StateFlow`, orquestan casos de uso.
- **Domain/Repository:**
  - `GameLibraryRepository` — escaneo, identificación, persistencia de juegos.
  - `EmulationManager` — wrapper sobre LibretroDroid (carga de core, save states, input).
  - `SettingsRepository` — DataStore (mapeo de botones, aspect ratio).
- **Data:** Room (juegos), DataStore (config), sistema de archivos (ROMs, saves, portadas cacheadas), red (Coil/OkHttp para thumbnails).

Sugerencia de módulos: app único para el MVP (evitar over-engineering con multi-módulo en esta etapa).

## 5. Estructura de almacenamiento y permisos

**Permiso:** `MANAGE_EXTERNAL_STORAGE` (acceso total a almacenamiento). Las boxes se instalan por sideload, así que la restricción de Play Store no aplica. Flujo de solicitud vía `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`; al primer arranque, si no está concedido, mostrar pantalla explicativa + botón que abre el ajuste del sistema.

**Carpetas de ROMs (almacenamiento interno, fijas):**

```
/storage/emulated/0/Roms/nes/
/storage/emulated/0/Roms/megadrive/
```

- Crearlas automáticamente en el primer arranque si no existen.
- Volumen esperado: ~700 MB en total (boxes de 32/64 GB), sin presión de espacio.

**Otras rutas (almacenamiento interno de la app):**

- Saves / save states: `filesDir/saves/{system}/{gameHash}.state` y `.srm`.
- Portadas cacheadas: gestionadas por Coil (caché de disco) + ruta persistida en Room.
- Cores (.so) y DAT de identificación: empaquetados en `app/src/main/assets/cores/` y `app/src/main/assets/datfiles/`, poblados por `setup.sh`. Los `.so` se copian a una ruta legible en tiempo de ejecución si LibretroDroid lo requiere.

**Permiso adicional:** `INTERNET` (descarga de portadas).

## 6. Cores y motor de emulación

- Motor: **LibretroDroid**. Expone una vista `GLRetroView` que en Compose se integra con `AndroidView`. Se inicializa con la ruta del core, la ruta de la ROM, el directorio de saves y demás parámetros (`GLRetroViewData`).
- **Cores como binarios `.so`** del proyecto libretro:
  - `nestopia_libretro_android.so` (NES, default)
  - `fceumm_libretro_android.so` (NES, respaldo)
  - `genesis_plus_gx_libretro_android.so` (Mega Drive)
- Descargar los `.so` para **arm64-v8a** del buildbot de libretro y empaquetarlos en la app. **No se compilan cores en este proyecto.** Esto lo automatiza el script **`setup.sh`** (ver sección "Setup inicial"), que descarga, verifica (ZIP válido + magic ELF) y deja cada `.so` en `app/src/main/assets/cores/`.
- Mapa consola → core:
  - NES → Nestopia (fallback a fceumm si un juego puntual falla).
  - Mega Drive → Genesis Plus GX.
- Licencias: Genesis Plus GX tiene cláusula no comercial; para uso propio en las boxes es indistinto. Documentar la licencia de cada core incluido.

## 7. Escaneo e identificación de juegos

Disparado por el botón **Escanear** en el Home. Para cada consola:

1. Recorrer la carpeta correspondiente y filtrar por extensiones válidas:
   - NES: `.nes`
   - Mega Drive: `.md`, `.gen`, `.bin`, `.smd`
   - Soportar también `.zip` (leer la ROM contenida).
2. Calcular el **CRC32** de cada ROM.
3. Buscar el CRC32 en una **base local hash → nombre canónico** (ver abajo) para obtener el nombre oficial del juego.
4. Si no hay match por hash, usar como fallback el nombre del archivo limpiado (quitar tags `(USA)`, `[!]`, etc.).
5. Persistir el juego en Room y disparar la descarga de portada (sección 8).
6. Mostrar progreso "X de Y" durante el escaneo (coroutine en el ViewModel; WorkManager opcional si se quiere en background).

**Base hash → nombre:** se empaquetan los **DAT de No-Intro** de cada sistema (los baja `setup.sh`, ver sección 6) y se parsean a una tabla de Room en el primer arranque. Estos DAT vienen en **formato clrmamepro** (texto, no XML): cada entrada es `game ( name "..." rom ( name "..." size N crc XXXXXXXX md5 ... sha1 ... ) )`. Un parser simple por bloques alcanza. Se prefiere esto antes que parsear los `.rdb` binarios de libretro (formato propio más complejo).

Fuente de los DAT (descargables directo, sin formularios):
```
https://raw.githubusercontent.com/libretro/libretro-database/master/metadat/no-intro/Nintendo - Nintendo Entertainment System.dat
https://raw.githubusercontent.com/libretro/libretro-database/master/metadat/no-intro/Sega - Mega Drive - Genesis.dat
```

> **Gotcha NES:** algunos hashes de No-Intro para NES se calculan sobre la ROM **sin** la cabecera iNES de 16 bytes. Manejar ambos casos (con y sin cabecera) al buscar el match.

## 8. Portadas / thumbnails

- Fuente: repositorio **libretro-thumbnails**.
- Patrón de URL:
  ```
  https://thumbnails.libretro.com/{Sistema}/Named_Boxarts/{Nombre del juego}.png
  ```
- Nombres de sistema:
  - NES → `Nintendo - Nintendo Entertainment System`
  - Mega Drive → `Sega - Mega Drive - Genesis`
- El `{Nombre del juego}` debe coincidir con el nombre canónico de No-Intro, **sanitizado**: reemplazar los caracteres `&*/:` `"<>?\|` por `_`. URL-encode del resultado.
- Descargar con OkHttp / dejar que Coil cachee en disco. Si la portada no existe (404), usar un placeholder genérico por consola.

## 9. Modelo de datos (Room)

```kotlin
@Entity
data class Game(
    @PrimaryKey val id: String,        // p.ej. "{system}:{crc32}"
    val system: String,                // "nes" | "megadrive"
    val filePath: String,
    val crc32: String,
    val canonicalName: String,         // nombre mostrado
    val thumbnailPath: String?,        // ruta local cacheada (nullable)
    val lastPlayedAt: Long?
)
```

- Tabla opcional `HashEntry(system, crc32, name)` para la base de identificación, si no se usa SQLite pre-construida.
- Config de cada consola (mapeo de botones, aspect ratio) va en **DataStore**, no en Room.

## 10. Pantallas y flujos (Compose for TV)

1. **Home / selección de consola**
   - Dos cards grandes: **NES** y **Mega Drive**.
   - Acciones: **Escanear**, **Ajustes**.
   - Foco inicial claro; todo navegable con D-pad.
2. **Listado de juegos (por consola)**
   - Grid de cards con **portada + título** (lazy grid de Compose).
   - **Búsqueda por nombre** (filtra el grid en vivo).
   - Acción al seleccionar: lanzar el juego.
3. **Escaneo**
   - Overlay/pantalla con progreso "X de Y juegos".
4. **Mapeo de botones (por consola)** — ver sección 11.
5. **Aspect ratio (por consola)** — ver sección 13.
6. **Juego (pantalla completa)**
   - `GLRetroView` a pantalla completa.
   - **Menú in-game** (invocado con un botón/combo dedicado): Quick Save, Quick Load, Aspect ratio, **Salir** (con autosave).

## 11. Mapeo de controles

- Concepto: mapear los botones del **RetroPad** de libretro a los keycodes del gamepad físico.
- **Global por consola** (un mapeo para NES, otro para Mega Drive).
- Botones por consola:
  - NES: D-pad, A, B, Start, Select.
  - Mega Drive: D-pad, A, B, C, X, Y, Z, Start, Mode (soportar control de 3 y 6 botones).
- Pantalla de configuración: listar cada botón del RetroPad; al seleccionarlo, capturar el siguiente `KeyEvent`/`MotionEvent` del gamepad y guardar la asociación en DataStore.
- En runtime: interceptar input, traducir keycode físico → botón RetroPad → enviarlo a `GLRetroView` (`sendKeyEvent` / `sendMotionEvent`).
- Proveer un mapeo por defecto razonable para gamepads estándar.

## 12. Save states

- **Autosave al salir:** al cerrar el juego, serializar el estado (`GLRetroView.serializeState()`) y la SRAM, y guardarlos por juego (`{gameHash}.state` / `.srm`).
- **Quick save / Quick load:** un slot rápido por juego, accesible desde el menú in-game.
- Al lanzar un juego con autosave existente, ofrecer **Continuar** o **Empezar de nuevo**.

## 13. Aspect ratio

Tres opciones, configurables **por consola**:

- **Original** — 4:3 con corrección de aspecto.
- **Integer scaling** — escalado entero, pixel perfect.
- **Completa** — estirar para llenar la pantalla.

Aplicado mediante el dimensionado/escala del contenedor de `GLRetroView` (y opciones de core donde aplique).

## 14. Manifest y configuración Android TV

- `uses-feature android.software.leanback` required **true**.
- `uses-feature android.hardware.touchscreen` required **false**.
- Activity principal con intent-filter `android.intent.category.LEANBACK_LAUNCHER`.
- Atributo `android:banner` en `<application>` (banner 320x180 para el launcher de TV).
- Permisos: `MANAGE_EXTERNAL_STORAGE`, `INTERNET`.
- Tema Material para TV; sin barra de acción táctil.

## 15. Setup inicial (`setup.sh`)

Antes de compilar, se corre una sola vez el script **`setup.sh`** incluido en el repo. Descarga y **verifica** los binarios de terceros que el proyecto necesita pero no genera, y los deja dentro del proyecto (se empaquetan en el APK):

- **Cores** desde el buildbot de libretro (`.../nightly/android/latest/arm64-v8a/{core}_libretro_android.so.zip`): Nestopia, fceumm, Genesis Plus GX → `app/src/main/assets/cores/`. Verifica que cada descarga sea un ZIP válido y que el `.so` interno tenga magic ELF.
- **DAT de No-Intro** (NES y Mega Drive) desde `libretro-database` → `app/src/main/assets/datfiles/`. Verifica que sean DAT clrmamepro con entradas de juegos.

Si algo cambia de nombre o baja corrupto, el script **aborta con un error claro** en lugar de dejar un archivo equivocado. Las ROMs nunca se tocan acá (las pone el usuario en el almacenamiento interno).

## 16. Orden de implementación sugerido

1. Scaffold del proyecto (Gradle, Compose for TV, estructura MVVM, manifest de TV).
2. Permiso `MANAGE_EXTERNAL_STORAGE` + creación de carpetas + pantalla de onboarding.
3. Home con selección de consola (UI navegable por D-pad).
4. Integración de **LibretroDroid** + un core: lanzar una ROM de prueba a pantalla completa. **Hito clave** — validar que emula bien antes de seguir.
5. Mapeo de controles + input en runtime.
6. Save states (autosave + quick save/load) + aspect ratio.
7. Escaneo: recorrido de carpetas, CRC32, base hash→nombre, persistencia en Room.
8. Listado de juegos con grid de portadas + búsqueda.
9. Descarga y caché de portadas (libretro-thumbnails + Coil).
10. Segundo core / segunda consola y pulido general.

## 17. Puntos a verificar / TODO

- [ ] Última versión de LibretroDroid (GitHub/JitPack) y de `androidx.tv:tv-material` + Compose BOM.
- [ ] Confirmar API de input de la versión de LibretroDroid usada (`sendKeyEvent` / `sendMotionEvent` / `GLRetroViewData`).
- [ ] Correr `./setup.sh` para poblar cores y DATs (reemplaza el paso manual).
- [ ] Manejar el caso de cabecera iNES en el hashing de NES.
- [ ] Banner de TV (320x180) e ícono.
- [ ] Nombre y package definitivos de la app.
