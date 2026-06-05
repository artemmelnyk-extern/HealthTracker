#!/usr/bin/env bash
set -euo pipefail

# ─── Config ───────────────────────────────────────────────────────────────────
export JAVA_HOME="/usr/lib/jvm/java-1.17.0-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
PORT=8080
BUILD=true   # set to false to skip build and use existing APK

# ─── Colors ───────────────────────────────────────────────────────────────────
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
RESET="\033[0m"

# ─── Parse args ───────────────────────────────────────────────────────────────
for arg in "$@"; do
  case $arg in
    --no-build) BUILD=false ;;
    --port=*)   PORT="${arg#*=}" ;;
    --release)  APK_PATH="app/build/outputs/apk/release/app-release.apk" ;;
  esac
done

# ─── Build ────────────────────────────────────────────────────────────────────
if [ "$BUILD" = true ]; then
  echo -e "${YELLOW}Building debug APK...${RESET}"
  ./gradlew assembleDebug
fi

# ─── Verify APK exists ────────────────────────────────────────────────────────
if [ ! -f "$APK_PATH" ]; then
  echo "APK not found at: $APK_PATH"
  echo "Run without --no-build, or check the path."
  exit 1
fi

APK_SIZE=$(du -sh "$APK_PATH" | cut -f1)
APK_DIR=$(dirname "$APK_PATH")
APK_FILE=$(basename "$APK_PATH")

# ─── Get local IP ─────────────────────────────────────────────────────────────
LOCAL_IP=$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7; exit}')
if [ -z "$LOCAL_IP" ]; then
  LOCAL_IP=$(hostname -I | awk '{print $1}')
fi

DOWNLOAD_URL="http://${LOCAL_IP}:${PORT}/${APK_FILE}"

# ─── Print info ───────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}APK ready:${RESET} $APK_PATH (${APK_SIZE})"
echo ""
echo -e "${CYAN}On your Android device:${RESET}"
echo "  1. Open browser and go to:"
echo ""
echo -e "     ${YELLOW}${DOWNLOAD_URL}${RESET}"
echo ""
echo "  2. Download and tap to install"
echo "  3. Allow 'Install from unknown sources' if prompted"
echo ""

# ─── Optional QR code ─────────────────────────────────────────────────────────
if command -v qrencode &>/dev/null; then
  echo -e "${CYAN}Scan to open on device:${RESET}"
  qrencode -t ansiutf8 "$DOWNLOAD_URL"
  echo ""
else
  echo -e "${YELLOW}Tip:${RESET} install 'qrencode' to get a scannable QR code"
  echo "     sudo apt install qrencode"
  echo ""
fi

# ─── Free port if already in use ──────────────────────────────────────────────
EXISTING_PID=$(lsof -ti tcp:"$PORT" 2>/dev/null || true)
if [ -n "$EXISTING_PID" ]; then
  echo -e "${YELLOW}Port ${PORT} in use (PID ${EXISTING_PID}), releasing...${RESET}"
  kill "$EXISTING_PID" 2>/dev/null || true
  sleep 1
fi

# ─── Serve ────────────────────────────────────────────────────────────────────
echo -e "${GREEN}Serving on port ${PORT}... (Ctrl+C to stop)${RESET}"
echo ""
cd "$APK_DIR"
python3 -m http.server "$PORT"
