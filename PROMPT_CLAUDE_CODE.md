# Prompt para Claude Code

Pegá este texto en Claude Code, en una carpeta vacía donde tengas también el archivo `ESPECIFICACION.md`.

---

Vas a construir una aplicación nativa de **Android TV** desde cero. Toda la definición funcional y técnica está en el archivo **`ESPECIFICACION.md`** de este directorio. **Leelo completo y tratalo como la fuente de verdad** antes de escribir una sola línea.

## Contexto rápido

Es un emulador minimalista para Android TV, solo para **NES/Famicom** y **Sega Mega Drive/Genesis**, tipo "RetroArch simplificado". La emulación se apoya 100% en la librería **LibretroDroid** + cores de libretro. Los cores y las bases de identificación los aporta el script `setup.sh` (incluido); **la app nunca incluye ni descarga ROMs** (las pone el usuario).

Stack: **Kotlin + Jetpack Compose for TV + MVVM**, Room, DataStore, Coil, OkHttp, Coroutines. `minSdk 26`, target la API más reciente, ABI **arm64-v8a**. El detalle completo está en el spec.

## Cómo quiero que trabajes

1. **Antes de empezar, verificá versiones actuales** (no asumas de memoria): última versión de `com.github.swordfish90:libretrodroid` en JitPack/GitHub, y la versión estable de `androidx.tv:tv-material` + el Compose BOM. Decime qué versiones vas a usar.
2. **Construí de forma incremental**, siguiendo el "Orden de implementación sugerido" (sección 15 del spec). No tires todo el código de una.
3. **Después de cada hito, asegurate de que el proyecto compile** y contame qué falta. El **hito crítico** es el paso 4: lanzar una ROM de prueba con LibretroDroid a pantalla completa; no avances a escaneo/UI hasta validar que la emulación funciona.
4. Cuando una decisión no esté en el spec, **proponé la opción más simple y preguntame** en vez de inventar complejidad.
5. Los binarios de terceros (cores `.so` y DAT de No-Intro) **los descarga y verifica el script `setup.sh`** ya incluido en el repo, que los deja en `app/src/main/assets/cores/` y `app/src/main/assets/datfiles/`. No le pidas al usuario que baje nada a mano: asumí que esos archivos existen ahí tras correr `./setup.sh`, y diseñá el código para leerlos desde esos `assets`. Si el scaffold que generás usa otra ruta de assets, ajustá las variables `CORES_DIR`/`DATS_DIR` del script en consecuencia.

## Primer paso

1. Leé `ESPECIFICACION.md`.
2. Verificá las versiones de las dependencias clave y decímelas.
3. Proponeme el **nombre y package** de la app (algo provisional está bien) y la estructura de carpetas del proyecto.
4. Generá el **scaffold inicial**: proyecto Gradle con Compose for TV, estructura MVVM y el `AndroidManifest.xml` configurado para Android TV (leanback launcher, touchscreen no requerido, banner, permisos `MANAGE_EXTERNAL_STORAGE` e `INTERNET`).

Pará ahí y mostrame el resultado antes de seguir con el resto.
