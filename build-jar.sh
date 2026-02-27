#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_DIR/.gradle-local}"

resolver_java_home() {
    if [[ -n "${JAVA_HOME:-}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
        echo "${JAVA_HOME}"
        return 0
    fi

    if [[ -x "/usr/libexec/java_home" ]]; then
        local detected=""
        detected=$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)
        if [[ -n "$detected" ]] && [[ -x "${detected}/bin/java" ]]; then
            echo "$detected"
            return 0
        fi
    fi

    local candidates=(
        "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
        "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        "/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    )

    local candidate
    for candidate in "${candidates[@]}"; do
        if [[ -x "${candidate}/bin/java" ]]; then
            echo "$candidate"
            return 0
        fi
    done

    return 1
}

resolver_jar_salida() {
    local jar_dir="$PROJECT_DIR/build/libs"
    if [[ ! -d "$jar_dir" ]]; then
        return 1
    fi

    find "$jar_dir" -maxdepth 1 -type f -name "BurpIA-*.jar" ! -name "*-plain.jar" | sort | tail -n 1
}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  BurpIA v1.0.1 - Constructor de JAR${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

if [[ ! -f "./gradlew" ]]; then
    echo -e "${RED}ERROR: No se encontró ./gradlew${NC}"
    echo "Ejecuta: gradle wrapper"
    exit 1
fi

if [[ ! -x "./gradlew" ]]; then
    echo -e "${YELLOW}Haciendo ./gradlew ejecutable...${NC}"
    chmod +x ./gradlew
fi

if ! java -version >/dev/null 2>&1; then
    if JAVA_HOME_DETECTED="$(resolver_java_home)"; then
        export JAVA_HOME="$JAVA_HOME_DETECTED"
        export PATH="${JAVA_HOME}/bin:${PATH}"
    fi
fi

if ! java -version >/dev/null 2>&1; then
    echo -e "${RED}ERROR: No se encontró un Java Runtime funcional (se requiere Java 17+)${NC}"
    echo "Sugerencia macOS (Homebrew):"
    echo "  brew install openjdk@17"
    echo "  export JAVA_HOME=\"/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home\""
    echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    exit 1
fi

TEST_COUNT=$(find "$PROJECT_DIR/src/test" -name "*.java" -type f 2>/dev/null | wc -l | tr -d ' ')
GRADLE_ARGS=(--no-daemon clean test fatJar)
if [[ "${1:-}" == "--no-test" ]]; then
    echo -e "${YELLOW}Modo: Sin pruebas unitarias (rápido)${NC}"
    GRADLE_ARGS=(--no-daemon clean fatJar -x test)
else
    echo -e "${GREEN}Modo: Con pruebas unitarias${NC}"
    echo -e "  ${CYAN}Archivos de prueba: ${TEST_COUNT}${NC}"
fi

echo ""
echo -e "${BLUE}Construyendo BurpIA JAR...${NC}"
echo -e "  Directorio: ${PROJECT_DIR}"
echo -e "  Gradle cache: ${GRADLE_USER_HOME}"
echo -e "  Java Home: ${JAVA_HOME:-<resuelto por PATH>}"
echo ""

set +e
GRADLE_OUTPUT=$(./gradlew "${GRADLE_ARGS[@]}" 2>&1)
GRADLE_EXIT=$?
echo "$GRADLE_OUTPUT"
set -e

if [[ $GRADLE_EXIT -ne 0 ]]; then
    echo ""
    echo -e "${RED}ERROR: La construcción falló${NC}"
    exit $GRADLE_EXIT
fi

JAR_PATH="$(resolver_jar_salida || true)"
if [[ -z "$JAR_PATH" ]] || [[ ! -f "$JAR_PATH" ]]; then
    echo ""
    echo -e "${RED}ERROR: No se encontró el JAR esperado${NC}"
    echo -e "  Carpeta buscada: $PROJECT_DIR/build/libs"
    echo -e "  Patrón esperado: BurpIA-*.jar"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)

PASSED_COUNT=$(echo "$GRADLE_OUTPUT" | grep -c "PASSED" || echo "0")

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  ✅ JAR construido exitosamente${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "  📦 Archivo: ${JAR_PATH}"
echo -e "  📊 Tamaño:  ${JAR_SIZE}"

if [[ "${1:-}" != "--no-test" ]] && [[ "$PASSED_COUNT" -gt 0 ]]; then
    echo -e "  ✅ Pruebas: ${PASSED_COUNT} pasadas"
fi

echo ""
echo -e "${YELLOW}Instrucciones de instalación:${NC}"
echo -e "  1. Abre Burp Suite Professional"
echo -e "  2. Ve a Extender > Extensions"
echo -e "  3. Click en 'Add'"
echo -e "  4. Selecciona el archivo: ${JAR_PATH}"
echo ""
