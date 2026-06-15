#!/usr/bin/env bash
#
# setup.sh — Descarga y verifica los binarios de terceros que el proyecto
# necesita pero NO genera: los cores de libretro y los DAT de No-Intro.
#
# Descarga cores para arm64-v8a (boxes reales) y x86 (emulador Android Studio).
# Los .so quedan en assets/cores/{abi}/ para que CoreExtractor elija el correcto.
#
# Requisitos: bash, curl, unzip, od  (Linux / macOS / WSL / Git Bash)
# Uso:
#   ./setup.sh           # descarga lo que falte
#   ./setup.sh --force   # vuelve a descargar todo aunque ya exista
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------
BUILDBOT_BASE="https://buildbot.libretro.com/nightly/android/latest"
DB_BASE="https://raw.githubusercontent.com/libretro/libretro-database/master/metadat/no-intro"

CORES_BASE="${CORES_BASE:-app/src/main/assets/cores}"
DATS_DIR="${DATS_DIR:-app/src/main/assets/datfiles}"

ABIS=("arm64-v8a" "x86")
CORES=( "nestopia" "fceumm" "genesis_plus_gx" )

FORCE=0
[ "${1:-}" = "--force" ] && FORCE=1

# ---------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------
log() { printf "\033[1;34m›\033[0m %s\n" "$*"; }
ok()  { printf "\033[1;32m✓\033[0m %s\n" "$*"; }
warn(){ printf "\033[1;33m!\033[0m %s\n" "$*"; }
die() { printf "\033[1;31m✗ %s\033[0m\n" "$*" >&2; exit 1; }

need() { command -v "$1" >/dev/null 2>&1 || die "Falta el comando requerido: $1"; }

fetch() {
  curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 -o "$2" "$1" \
    || die "No se pudo descargar (HTTP error o sin conexión): $1"
}

# ---------------------------------------------------------------------------
# Cores
# ---------------------------------------------------------------------------
fetch_core() {
  local abi="$1"
  local core="$2"
  local so="${core}_libretro_android.so"
  local dest="${CORES_BASE}/${abi}/${so}"

  if [ -f "$dest" ] && [ "$FORCE" -eq 0 ]; then
    ok "Core ya presente: ${dest}"
    return
  fi

  local url="${BUILDBOT_BASE}/${abi}/${core}_libretro_android.so.zip"
  local zip="${TMP}/${abi}_${core}.so.zip"

  log "Descargando core [${abi}]: ${core}"
  fetch "$url" "$zip"

  unzip -tq "$zip" >/dev/null 2>&1 \
    || die "El archivo de '${core}' (${abi}) no es un ZIP válido. URL: $url"

  unzip -o -q "$zip" -d "$TMP"
  [ -f "${TMP}/${so}" ] \
    || die "El ZIP de '${core}' (${abi}) no contiene '${so}'. Contenido: $(unzip -Z1 "$zip" | tr '\n' ' ')"

  local magic
  magic=$(head -c 4 "${TMP}/${so}" | od -An -tx1 | tr -d ' \n')
  [ "$magic" = "7f454c46" ] \
    || die "'${so}' (${abi}) no es un ELF válido (magic=${magic})."

  mkdir -p "$(dirname "$dest")"
  mv -f "${TMP}/${so}" "$dest"
  ok "Core listo: ${dest}"
}

# ---------------------------------------------------------------------------
# DAT de No-Intro
# ---------------------------------------------------------------------------
fetch_dat() {
  local remote="$1"
  local out="$2"
  local dest="${DATS_DIR}/${out}"

  if [ -f "$dest" ] && [ "$FORCE" -eq 0 ]; then
    ok "DAT ya presente: ${dest}"
    return
  fi

  log "Descargando DAT: ${remote}"
  local enc; enc=$(printf '%s' "$remote" | sed 's/ /%20/g')
  fetch "${DB_BASE}/${enc}" "$dest"

  head -n1 "$dest" | grep -q "clrmamepro" \
    || die "'${out}' no parece un DAT válido."
  grep -q "game (" "$dest" \
    || die "'${out}' no contiene entradas de juegos."

  local n; n=$(grep -c "game (" "$dest")
  ok "DAT listo: ${dest} (${n} juegos)"
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
need curl; need unzip; need od

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$DATS_DIR"

log "== Cores (libretro buildbot) =="
for abi in "${ABIS[@]}"; do
  log "-- ABI: ${abi} --"
  for c in "${CORES[@]}"; do
    fetch_core "$abi" "$c"
  done
done
echo

log "== Bases de identificación (No-Intro vía libretro-database) =="
fetch_dat "Nintendo - Nintendo Entertainment System.dat" "nes.dat"
fetch_dat "Sega - Mega Drive - Genesis.dat"              "megadrive.dat"
echo

ok "Setup completo. Cores en assets/cores/{abi}/ y DATs en assets/datfiles/."
echo
warn "Recordatorio: las ROMs NO se descargan acá. Van en el almacenamiento interno:"
echo "    /storage/emulated/0/Roms/nes/"
echo "    /storage/emulated/0/Roms/megadrive/"
