#!/usr/bin/env bash
#
# BurpIA - Script de construcción de JAR
# ================================
# Este script construye el archivo JAR fat de BurpIA usando Gradle.
# El JAR resultante incluye todas las dependencias necesarias.
#
# Uso: ./build-jar.sh [--no-test]
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Cache local de Gradle para evitar problemas de permisos
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_DIR/.gradle-local}"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  BurpIA v1.0.0 - Constructor de JAR${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Verificar que gradlew existe y es ejecutable
if [[ ! -f "./gradlew" ]]; then
    echo -e "${RED}ERROR: No se encontró ./gradlew${NC}"
    echo "Ejecuta: gradle wrapper"
    exit 1
fi

if [[ ! -x "./gradlew" ]]; then
    echo -e "${YELLOW}Haciendo ./gradlew ejecutable...${NC}"
    chmod +x ./gradlew
fi

# Contar archivos de prueba
TEST_COUNT=$(find "$PROJECT_DIR/src/test" -name "*.java" -type f 2>/dev/null | wc -l | tr -d ' ')

# Verificar argumento --no-test
GRADLE_TASK="clean test fatJar"
if [[ "${1:-}" == "--no-test" ]]; then
    echo -e "${YELLOW}Modo: Sin pruebas unitarias (rápido)${NC}"
    GRADLE_TASK="clean fatJar -x test"
else
    echo -e "${GREEN}Modo: Con pruebas unitarias${NC}"
    echo -e "  ${CYAN}Archivos de prueba: ${TEST_COUNT}${NC}"
fi

echo ""
echo -e "${BLUE}Construyendo BurpIA JAR...${NC}"
echo -e "  Directorio: ${PROJECT_DIR}"
echo -e "  Gradle cache: ${GRADLE_USER_HOME}"
echo ""

# Ejecutar Gradle y capturar output
set +e
GRADLE_OUTPUT=$(./gradlew --no-daemon $GRADLE_TASK 2>&1)
GRADLE_EXIT=$?
echo "$GRADLE_OUTPUT"
set -e

if [[ $GRADLE_EXIT -ne 0 ]]; then
    echo ""
    echo -e "${RED}ERROR: La construcción falló${NC}"
    exit $GRADLE_EXIT
fi

# Verificar que el JAR se creó
JAR_PATH="$PROJECT_DIR/build/libs/BurpIA-1.0.0.jar"

if [[ ! -f "$JAR_PATH" ]]; then
    echo ""
    echo -e "${RED}ERROR: No se encontró el JAR esperado${NC}"
    echo -e "  Ruta esperada: ${JAR_PATH}"
    exit 1
fi

# Obtener tamaño del JAR
JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)

# Contar pruebas pasadas
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
