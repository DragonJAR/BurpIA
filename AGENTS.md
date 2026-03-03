# AGENTS.md - Guía para Agentes de IA

> **Este documento es la guía principal para que cualquier agente de IA entienda el proyecto BurpIA y trabaje en él siguiendo sus estándares y arquitectura.**

**Última actualización:** 2026-03-02
**Versión actual:** 1.0.2
**Idioma:** Español (con soporte para inglés)

---

## 📋 Tabla de Contenidos

1. [Resumen del Proyecto](#resumen-del-proyecto)
2. [Principios Fundamentales](#principios-fundamentales)
3. [Arquitectura del Proyecto](#arquitectura-del-proyecto)
4. [Estructura de Directorios](#estructura-de-directorios)
5. [Documentación Existente](#documentación-existente)
6. [Patrones Arquitecturales](#patrones-arquitecturales)
7. [Componentes Principales](#componentes-principales)
8. [Sistema de Configuración](#sistema-de-configuración)
9. [Sistema de Logging Unificado](#sistema-de-logging-unificado)
10. [Sistema de Internacionalización](#sistema-de-internacionalización)
11. [Integración con Agentes](#integración-con-agentes)
12. [Convenciones de Código](#convenciones-de-código)
13. [Flujo de Trabajo para Agentes](#flujo-de-trabajo-para-agentes)
14. [Mejores Prácticas](#mejores-prácticas)
15. [Referencias Rápidas](#referencias-rápidas)

---

## Resumen del Proyecto

**BurpIA** es una extensión de Burp Suite que integra Modelos de Lenguaje Grande (LLMs) para el análisis automatizado de seguridad del tráfico HTTP. Permite a los profesionales de seguridad detectar vulnerabilidades potenciales utilizando análisis potenciado por IA con flujos de trabajo pasivos y manuales.

### Características Principales

- **Análisis AI Automatizado:** Análisis pasivo de tráfico HTTP con múltiples proveedores (OpenAI, Claude, Gemini, Ollama, etc.)
- **Integración de Agentes:** Soporte para Claude Code, Factory Droid, Gemini CLI vía MCP
- **Terminal Integrada:** Interfaz terminal JediTerm para interacción con agentes
- **Gestión de Tareas:** Cola de tareas con pausa/reanudar/cancelación
- **Deduplicación:** Hash SHA-256 para evitar análisis redundantes
- **Persistencia Completa:** Configuración, filtros UI, y estado entre sesiones
- **Limitación de Tasa:** Control de concurrencia configurable
- **Internacionalización:** Soporte completo para español e inglés

---

## Principios Fundamentales

Antes de trabajar en este proyecto, **DEBES** seguir estos tres principios sin excepción:

### 1. 🛡️ CONFIABILIDAD (Reliability)

- **Validación de Entrada:** Siempre valida inputs usando `Normalizador.esVacio()` y `Normalizador.noEsVacio()`
- **Manejo de Errores:** Usa `GestorLoggingUnificado` para logging con try-catch en operaciones críticas
- **Degradación Elegante:** La UI debe permanecer funcional cuando características no están disponibles
- **Tests Incrementales:** Compila después de cada cambio significativo: `./gradlew jar --quiet`
- **Retrocompatibilidad:** Mantén compatibilidad con configuraciones existentes

### 2. ⚡ EFICIENCIA (Efficiency)

- **Lazy Loading:** Carga datos bajo demanda (ej: evidencias HTTP en hallazgos)
- **Rate Limiting:** Respeta los límites configurados via `LimitadorTasa`
- **Deduplicación:** Usa hash SHA-256 antes de procesar contenido duplicado
- **Gestión de Recursos:** Limpia recursos en métodos shutdown (ExecutorService, streams)
- **Optimización UI:** Actualizaciones UI en EDT via `UIUtils.ejecutarEnEdt()`

### 3. 🔄 DRY (Don't Repeat Yourself)

**ESTE ES EL PRINCIPIO MÁS IMPORTANTE.** El proyecto ya tiene infraestructura consolidada:

- ✅ **Usa `GestorLoggingUnificado`** para TODO el logging (nunca uses System.out/err directo)
- ✅ **Usa `Normalizador`** para validación de strings (nunca hagas `str == null || str.isEmpty()`)
- ✅ **Usa `I18nLogs`/`I18nUI`** para todo el texto (nunca strings hardcoded)
- ✅ **Usa `ConfiguracionAPI`** para acceder a configuración (nunca variables sueltas)
- ✅ **Usa `GestorTareas`** para gestión de tareas (no crees colas propias)
- ✅ **Usa `UIUtils`** para operaciones UI comunes (no duplices código UI)

> **⚠️ REGLA DE ORO:** Si estás a punto de escribir código que ya existe en utilidades, DETENTE y usa la utilidad existente.

---

## Arquitectura del Proyecto

### Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        Burp Suite API                            │
│                    (MontoyaApi 2026.2)                          │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                 ┌───────────────┴───────────────┐
                 │                               │
         ┌───────▼────────┐           ┌──────────▼──────────┐
         │  ExtensionBurpIA │           │  ManejadorHttpBurpIA│
         │  (Main Controller)          │  (HTTP Handler)      │
         └───────┬────────┘           └──────────┬──────────┘
                 │                               │
    ┌────────────┼────────────┬─────────────────┤
    │            │            │                 │
┌───▼────┐  ┌───▼──────┐  ┌──▼─────┐  ┌───────▼────────┐
│ UI     │  │ Config  │  │ Analizer│  │ Agent          │
│System  │  │ Manager │  │         │  │Integration     │
└────────┘  └──────────┘  └────────┘  └────────────────┘
    │            │            │                 │
    └────────────┴────────────┴─────────────────┘
                 │
         ┌───────▼────────┐
         │ Shared Utils   │
         │ - Logging      │
         │ - I18n         │
         │ - Validation   │
         └────────────────┘
```

### Flujo de Datos Principal

```
HTTP Traffic
    │
    ▼
[ManejadorHttpBurpIA] - Intercepta request/response
    │
    ▼
[Deduplicación SHA-256] - Evita procesamiento duplicado
    │
    ▼
[GestorTareas] - Encola tarea con límite de concurrencia
    │
    ▼
[AnalizadorAI] - Analiza con LLM configurado
    │
    ├──► [PanelTareas] - Actualiza cola de tareas
    ├──► [ModeloTablaHallazgos] - Agrega hallazgo si es relevante
    ├──► [PanelHallazgos] - Actualiza tabla de hallazgos
    └──► [Estadisticas] - Actualiza contadores
```

---

## Estructura de Directorios

```
BurpIA/
├── src/main/java/com/burpia/
│   ├── ExtensionBurpIA.java          # ⭐ Punto de entrada principal
│   ├── CLAUDE.md                     # 📄 Contexto para agentes Claude
│   │
│   ├── config/                       # ⚙️ Gestión de configuración
│   │   ├── ConfiguracionAPI.java    # Clase central de configuración
│   │   ├── GestorConfiguracion.java # Persistencia JSON
│   │   ├── AgenteTipo.java          # Enum de tipos de agentes
│   │   └── CLAUDE.md                # Documentación de config
│   │
│   ├── model/                        # 📦 Modelos de datos
│   │   ├── Hallazgo.java            # Modelo de hallazgos/vulnerabilidades
│   │   ├── Tarea.java               # Modelo de tareas de análisis
│   │   ├── Estadisticas.java        # Contadores de estadísticas
│   │   └── ResultadoAnalisisMultiple.java # Resultados multi-análisis
│   │
│   ├── ui/                           # 🖼️ Componentes de interfaz
│   │   ├── PestaniaPrincipal.java   # Pestaña principal (4 tabs)
│   │   ├── PanelEstadisticas.java   # Panel de estadísticas
│   │   ├── PanelHallazgos.java      # Panel de hallazgos con filtros
│   │   ├── PanelTareas.java         # Panel de cola de tareas
│   │   ├── PanelAgente.java         # Terminal de agente
│   │   ├── PanelConsola.java        # Consola de logs
│   │   ├── DialogoConfiguracion.java # Diálogo de configuración
│   │   ├── ModeloTablaHallazgos.java # TableModel para hallazgos
│   │   ├── ModeloTablaTareas.java    # TableModel para tareas
│   │   ├── [15+ componentes UI más]  # Botones, menús, diálogos
│   │   └── CLAUDE.md                # Documentación UI
│   │
│   ├── util/                         # 🔧 Utilidades compartidas (DRY!)
│   │   ├── GestorLoggingUnificado.java  # ⭐ Logging centralizado
│   │   ├── GestorTareas.java           # Gestión de tareas
│   │   ├── GestorConsolaGUI.java       # Gestor de consola GUI
│   │   ├── Normalizador.java           # ⭐ Validación de strings
│   │   ├── OSUtils.java               # Utilidades de SO
│   │   ├── HttpUtils.java             # Utilidades HTTP
│   │   ├── UIUtils.java               # ⭐ Utilidades UI
│   │   ├── LimitadorTasa.java         # Rate limiting
│   │   ├── ReparadorJson.java         # Reparación de JSON
│   │   ├── VersionBurpIA.java         # Gestión de versión
│   │   └── CLAUDE.md                  # Documentación de utilidades
│   │
│   ├── i18n/                         # 🌍 Internacionalización
│   │   ├── I18nUI.java               # Traducciones UI (ES/EN)
│   │   ├── I18nLogs.java             # Traducciones de logs
│   │   └── IdiomaUI.java             # Enum de idiomas
│   │
│   └── analyzer/                     # 🔬 Motor de análisis
│       └── AnalizadorAI.java         # Analizador AI principal
│
├── docs/                             # 📚 Documentación autogenerada
│   ├── COMPONENTES_UI.md            # Componentes UI documentados
│   ├── FUNCIONES.md                 # Funciones/API documentadas
│   ├── I18N.md                      # Claves de internacionalización
│   └── PLUGINS-BURP.md              # Referencia Burp API
│
├── src/assets/                       # 🎨 Assets estáticos
│   ├── logo.png                     # Logo de BurpIA
│   ├── [screenshots]                # Capturas de pantalla
│   └── CLAUDE.md                    # Documentación de assets
│
├── build.gradle                      # 🔨 Configuración de build
├── AGENT-*.md                        # 📖 Guías de agentes (Claude, Droid, Gemini)
├── README.md / README.en.md          # 📖 Guías de usuario
└── AGENTS.md                         # ⭐ ESTE DOCUMENTO
```

---

## Documentación Existente

### 📚 Documentación de Usuario

| Archivo | Propósito | Cuándo consultarlo |
|---------|-----------|-------------------|
| `README.md` | Guía de usuario en español | Contexto general del proyecto |
| `README.en.md` | User guide in English | English context |
| `AGENTE-CLAUDE-ES.md` | Guía agente Claude (ES) | Configurar agente Claude |
| `AGENT-CLAUDE-EN.md` | Claude agent guide (EN) | Configure Claude agent |
| `AGENTE-DROID-ES.md` | Guía agente Droid (ES) | Configurar agente Droid |
| `AGENT-DROID-EN.md` | Droid agent guide (EN) | Configure Droid agent |
| `AGENTE-GEMINI-ES.md` | Guía agente Gemini (ES) | Configurar agente Gemini |
| `AGENT-GEMINI-EN.md` | Gemini agent guide (EN) | Configure Gemini agent |

### 📚 Documentación Técnica Autogenerada

| Archivo | Propósito | Generado por |
|---------|-----------|-------------|
| `docs/FUNCIONES.md` | Referencia de funciones/API | `gradle generarDocsFunciones` |
| `docs/COMPONENTES_UI.md` | Componentes UI documentados | `gradle generarDocsUI` |
| `docs/I18N.md` | Claves de internacionalización | `gradle generarDocsI18n` |
| `docs/PLUGINS-BURP.md` | Referencia Burp API | Documentación oficial |

> **💡 PRO TIP:** La documentación se genera automáticamente desde el código fuente. Si modificas código, regenera la documentación con:
> ```bash
> ./gradlew generarDocsFunciones generarDocsUI generarDocsI18n
> ```

---

## Patrones Arquitecturales

### 1. DRY (Don't Repeat Yourself) - Ya Implementado ✅

El proyecto **YA TIENE** infraestructura consolidada. **NO** la dupliques.

#### Logging Unificado

```java
// ❌ NUNCA hagas esto:
System.out.println("Mensaje");
stderr.println("Error");
LOGGER.log(Level.INFO, "Mensaje");

// ✅ SIEMPRE haz esto:
gestorLogging.info("MiComponente", "Mensaje");
gestorLogging.error("MiComponente", "Error");
```

**Implementación:** `src/main/java/com/burpia/util/GestorLoggingUnificado.java:102-295`

**Soporta 4 canales:**
1. Consola GUI (vía `GestorConsolaGUI`)
2. stdout/stderr (vía `PrintWriter`)
3. java.util.logging.Logger
4. Burp Suite API (vía `MontoyaApi`)

**Factory methods:**
```java
// En ExtensionBurpIA (todos los canales)
gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, api, null);

// En componentes UI (sin Burp API)
gestorLogging = GestorLoggingUnificado.crearMinimal(stdout, stderr);

// En clases con Logger java.util.logging
gestorLogging = GestorLoggingUnificado.crearConLogger(LOGGER);
```

#### Validación de Strings

```java
// ❌ NUNCA hagas esto:
if (str == null || str.isEmpty()) { }
if (str != null && !str.trim().isEmpty()) { }

// ✅ SIEMPRE haz esto:
if (Normalizador.esVacio(str)) { }
if (Normalizador.noEsVacio(str)) { }
```

**Implementación:** `src/main/java/com/burpia/util/Normalizador.java`

#### Internacionalización

```java
// ❌ NUNCA hagas esto:
JLabel label = new JLabel("Hola Mundo");

// ✅ SIEMPRE haz esto:
JLabel label = new JLabel(I18nUI.General.HOLA_MUNDO());

// Para logs:
gestorLogging.info("MiComponente", I18nLogs.MENSAJE_LOG());
```

**Archivos:** `src/main/java/com/burpia/i18n/I18nUI.java`, `I18nLogs.java`

### 2. Modularidad con Responsabilidad Única

Cada clase tiene una responsabilidad clara:

- **ExtensionBurpIA:** Coordinación principal y registro en Burp
- **ConfiguracionAPI:** Almacén de configuración centralizado
- **GestorConfiguracion:** Persistencia (lectura/escritura JSON)
- **GestorTareas:** Gestión de cola de tareas con estado
- **AnalizadorAI:** Lógica de análisis con LLMs
- **ModeloTablaHallazgos/ModeloTablaTareas:** TableModels para Swing

### 3. Event-Driven Design

```java
// Patrón observador para cambios de configuración
AtomicReference<Runnable> manejadorCambioConfiguracion = new AtomicReference<>();
manejadorCambioConfiguracion.set(() -> {
    // Recargar configuración
});

// Disparar evento
Runnable handler = manejadorCambioConfiguracion.get();
if (handler != null) {
    handler.run();
}
```

### 4. Inyección de Dependencias (Constructor)

```java
public PanelHallazgos(ConfiguracionAPI config,
                     ModeloTablaHallazgos modelo,
                     GestorTareas gestorTareas) {
    this.config = config;
    this.modelo = modelo;
    this.gestorTareas = gestorTareas;
    // ...
}
```

### 5. Lazy Loading para Optimización

```java
// Evidencias HTTP se cargan bajo demanda
private HttpRequestResponse httpRequestResponse;

public HttpRequestResponse obtenerHttpRequestResponse() {
    if (httpRequestResponse == null && httpRequestResponseSaved != null) {
        httpRequestResponse = httpRequestResponseSaved;
    }
    return httpRequestResponse;
}
```

---

## Componentes Principales

### 1. ExtensionBurpIA (Controlador Principal)

**Archivo:** `src/main/java/com/burpia/ExtensionBurpIA.java`

**Responsabilidades:**
- Inicializar todos los componentes
- Registrar HTTP handlers y menús contextuales
- Gestionar el ciclo de vida de la extensión
- Coordinar entre UI, análisis y configuración

**Campos importantes:**
```java
private MontoyaApi api;                    // Burp Suite API
private ConfiguracionAPI config;           // Configuración central
private ManejadorHttpBurpIA manejadorHttp; // HTTP handler
private PestaniaPrincipal pestaniaPrincipal; // UI principal
private GestorTareas gestorTareas;         // Gestor de tareas
private Estadisticas estadisticas;         // Estadísticas
private GestorLoggingUnificado gestorLogging; // Logging unificado
```

**Método clave - initialize():**
```java
@Override
public void initialize(MontoyaApi api) {
    this.api = api;

    // 1. Configurar logging
    setupLogging();

    // 2. Cargar configuración
    gestorConfig = new GestorConfiguracion(stdout, stderr);
    config = gestorConfig.cargarConfiguracion();

    // 3. Inicializar gestores
    gestorTareas = new GestorTareas(modeloTablaTareas, ...);
    gestorConsola = new GestorConsolaGUI();
    gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, api, null);

    // 4. Crear UI
    pestaniaPrincipal = new PestaniaPrincipal(...);
    api.userInterface().registerMainTab(pestaniaPrincipal);

    // 5. Registrar handlers
    manejadorHttp = new ManejadorHttpBurpIA(...);
    api.http().registerHttpHandler(manejadorHttp);

    // 6. Registrar menú contextual
    registrarMenuContextual();
}
```

### 2. Sistema de Configuración

#### ConfiguracionAPI (Almacén Central)

**Archivo:** `src/main/java/com/burpia/config/ConfiguracionAPI.java`

**Almacena 32+ elementos de configuración:**

- **AI Provider:** Proveedor, API keys, URLs base, modelos, max tokens
- **Rendimiento:** Delay, max concurrente, max hallazgos, timeouts
- **UI/Tema:** Tema, idioma, fuentes estándar y monoespaciadas
- **Flags:** Detallado, escaneo pasivo, auto-guardado, auto-scroll, alertas
- **Prompts:** Prompt configurable y modificado
- **Seguridad:** Ignorar errores SSL, solo proxy
- **Agente:** Tipo, rutas de binarios, prompts, delays

**Métodos de ejemplo:**
```java
// AI Provider
public String obtenerProveedorAI();
public String obtenerApiKey(String proveedor);
public String obtenerModelo(String proveedor);

// Rendimiento
public int obtenerRetrasoSegundos();
public int obtenerMaximoConcurrente();

// UI
public boolean esTemaClaro();
public String obtenerIdiomaUi();
```

#### GestorConfiguracion (Persistencia)

**Archivo:** `src/main/java/com/burpia/config/GestorConfiguracion.java`

**Responsabilidades:**
- Cargar configuración desde `~/.burpia/config.json`
- Guardar configuración automáticamente
- Crear configuración por defecto si no existe
- Asegurar permisos privados en archivo (Unix: 0600)

**Uso:**
```java
// Cargar configuración
GestorConfiguracion gestor = new GestorConfiguracion(stdout, stderr);
ConfiguracionAPI config = gestor.cargarConfiguracion();

// Guardar configuración (automático con listeners)
config.setProveedorAI("openai");
// GestorConfiguracion detecta cambio y guarda automáticamente
```

### 3. Componentes UI

#### PestaniaPrincipal (Pestaña Principal)

**Archivo:** `src/main/java/com/burpia/ui/PestaniaPrincipal.java`

**Contiene 4 pestañas:**
1. **Consola** (PanelConsola): Logs del sistema
2. **Hallazgos** (PanelHallazgos): Tabla de hallazgos con filtros
3. **Tareas** (PanelTareas): Cola de tareas con controles
4. **Agente** (PanelAgente): Terminal del agente

#### PanelHallazgos (Panel de Hallazgos)

**Archivo:** `src/main/java/com/burpia/ui/PanelHallazgos.java`

**Características:**
- Tabla con hallazgos (vulnerabilidades detectadas)
- Filtro de búsqueda por texto
- Filtro por severidad (Crítica, Alta, Media, Baja, Info)
- Botones: Enviar a agente, Copiar prompt, Eliminar
- Persistencia de filtros entre sesiones

**Modelo de datos:**
```java
public class Hallazgo {
    private String severidad;        // CRITICAL, HIGH, MEDIUM, LOW, INFO
    String url;                      // URL afectada
    private String vulnerabilidad;   // Tipo de vulnerabilidad
    private String evidencia;        // Evidencia del hallazgo
    private String recomendacion;    // Recomendación de fix
    private HttpRequestResponse httpRequestResponse; // Evidencia HTTP
    // ...
}
```

#### PanelTareas (Panel de Tareas)

**Archivo:** `src/main/java/com/burpia/ui/PanelTareas.java`

**Características:**
- Tabla con cola de tareas
- Botones: Pausar, Reanudar, Cancelar todo
- Barra de progreso
- Contador de tareas pendientes/completadas

**Estados de tarea:**
```java
public enum EstadoTarea {
    PENDIENTE,     // En cola
    PROCESANDO,    // Analizando
    COMPLETADO,    // Finalizado
    ERROR          // Falló
}
```

#### PanelAgente (Terminal del Agente)

**Archivo:** `src/main/java/com/burpia/ui/PanelAgente.java`

**Características:**
- Terminal JediTerm para interacción con agente
- Soporte para PTY (pseudo-terminal)
- Inyección automática de prompts
- Botones: Copiar, Pegar, Limpiar, Ctrl+C, Cambiar agente

**Integración con agentes:**
```java
// Iniciar terminal con agente
public void iniciarConsola() {
    PtyProcess process = new PtyProcessBuilder()
        .setCommand(binaryPath)
        .setEnvironment(envVars)
        .start();

    ttyConnector = new PtyProcessTtyConnector(process);
    terminalWidget = new JediTermWidget(ttyConnector);
}
```

#### DialogoConfiguracion (Diálogo de Configuración)

**Archivo:** `src/main/java/com/burpia/ui/DialogoConfiguracion.java`

**Pestañas:**
1. **AI Provider:** Configurar proveedores, API keys, modelos
2. **Rendimiento:** Delays, concurrencia, límites
3. **UI/Tema:** Tema, idioma, fuentes
4. **Prompts:** Prompt configurable
5. **Agente:** Tipo de agente, rutas, delays
6. **Acerca de:** Información de versión y enlaces

### 4. Motor de Análisis

#### AnalizadorAI

**Archivo:** `src/main/java/com/burpia/analyzer/AnalizadorAI.java`

**Responsabilidades:**
- Construir prompt con variables de sustitución
- Llamar a API del proveedor AI
- Parsear respuesta JSON
- Extraer hallazgos detectados

**Flujo:**
```java
public List<Hallazgo> analizar(String requestStr, String responseStr, ConfiguracionAPI config) {
    // 1. Construir prompt con sustitución
    String prompt = construirPrompt(requestStr, responseStr);

    // 2. Llamar API
    String respuesta = llamarApi(prompt, config);

    // 3. Reparar JSON si necesario
    respuesta = ReparadorJson.repararJsonMalformado(respuesta);

    // 4. Parsear
    List<Hallazgo> hallazgos = parsearHallazgos(respuesta);

    return hallazgos;
}
```

**Variables de sustitución en prompt:**
```
{{REQUEST}}          - Request HTTP completo
{{REQUEST_HEADERS}}   - Headers del request
{{REQUEST_BODY}}      - Body del request
{{RESPONSE}}          - Response HTTP completo
{{RESPONSE_HEADERS}}  - Headers del response
{{RESPONSE_BODY}}     - Body del response
{{URL}}               - URL del request
{{METHOD}}            - Método HTTP
```

### 5. Gestor de Tareas

#### GestorTareas

**Archivo:** `src/main/java/com/burpia/util/GestorTareas.java`

**Responsabilidades:**
- Gestionar cola de tareas con límite de concurrencia
- Pausar/reanudar procesamiento
- Cancelar tareas pendientes
- Notificar cambios a UI

**Uso:**
```java
// Crear gestor con límite de 5 tareas concurrentes
GestorTareas gestor = new GestorTareas(modeloTablaTareas, config, 5);

// Encolar tarea
gestor.encolarTarea(() -> {
    // Lógica de análisis
    List<Hallazgo> hallazgos = analizador.analizar(...);
    return hallazgos;
});

// Pausar
gestor.pausar();

// Reanudar
gestor.reanudar();

// Cancelar todo
gestor.cancelarTodo();
```

---

## Sistema de Configuración

### Ubicación del Archivo

**Unix/Linux/macOS:** `~/.burpia/config.json`
**Windows:** `%USERPROFILE%\.burpia\config.json`

### Estructura del Archivo JSON

```json
{
  "proveedorAI": "openai",
  "apiKeysPorProveedor": {
    "openai": "sk-...",
    "claude": "sk-ant-...",
    "gemini": "AIza..."
  },
  "urlsBasePorProveedor": {
    "openai": "https://api.openai.com/v1",
    "claude": "https://api.anthropic.com/v1"
  },
  "modelosPorProveedor": {
    "openai": "gpt-4",
    "claude": "claude-3-5-sonnet-20241022"
  },
  "maxTokensPorProveedor": {
    "openai": 4096,
    "claude": 8192
  },
  "retrasoSegundos": 1,
  "maximoConcurrente": 5,
  "maximoHallazgosTabla": 1000,
  "tema": "oscuro",
  "idiomaUi": "es",
  "agenteHabilitado": true,
  "tipoAgente": "CLAUDE_CODE",
  "rutasBinarioPorAgente": {
    "CLAUDE_CODE": "/usr/local/bin/claude",
    "FACTORY_DROID": "/usr/local/bin/droid"
  },
  "promptConfigurable": "Analiza este tráfico HTTP...",
  "escaneoPasivoHabilitado": true,
  "autoGuardadoIssuesHabilitado": true
}
```

### Valores por Defecto

Ver constructor de `ConfiguracionAPI` (líneas 70-107) para todos los valores por defecto.

---

## Sistema de Logging Unificado

### GestorLoggingUnificado

**Archivo:** `src/main/java/com/burpia/util/GestorLoggingUnificado.java`

**Principio:** Centralizar TODO el logging en un solo lugar, soportando múltiples canales de salida.

### Factory Methods

```java
// 1. FULL - Todos los canales (en ExtensionBurpIA)
GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, api, null);

// 2. MINIMAL - Solo stdout/stderr (en componentes sin Burp API)
GestorLoggingUnificado.crearMinimal(stdout, stderr);

// 3. LOGGER - Solo java.util.logging (en clases con Logger)
GestorLoggingUnificado.crearConLogger(LOGGER);
```

### Métodos de Logging

```java
// Info con origen
gestorLogging.info("MiComponente", "Mensaje informativo");

// Error con origen
gestorLogging.error("MiComponente", "Mensaje de error");

// Error con excepción
gestorLogging.error("MiComponente", "Mensaje", throwable);

// Info sin origen (usa "BurpIA" por defecto)
gestorLogging.info("Mensaje rápido");

// Log con nivel específico
gestorLogging.log(Level.WARNING, "Advertencia");
gestorLogging.log(Level.SEVERE, "Error grave", throwable);

// Separador visual
gestorLogging.separador();
```

### Canales Soportados

1. **Consola GUI** (GestorConsolaGUI):
   ```java
   gestorConsola.registrarInfo("Origen", "Mensaje");
   gestorConsola.registrarError("Origen", "Error");
   ```

2. **stdout/stderr** (PrintWriter):
   ```java
   stdout.println("[BurpIA] Mensaje");
   stderr.println("[BurpIA] [ERROR] Error");
   ```

3. **Burp Suite API** (MontoyaApi):
   ```java
   api.logging().logToOutput("BurpIA: Mensaje");
   api.logging().logToError("BurpIA: [ERROR] Error");
   ```

4. **java.util.logging.Logger**:
   ```java
   logger.log(Level.INFO, "Mensaje");
   logger.log(Level.SEVERE, "Error", throwable);
   ```

### Ejemplo de Uso Real

```java
public class MiComponente {
    private final GestorLoggingUnificado gestorLogging;

    public MiComponente(PrintWriter stdout, PrintWriter stderr) {
        this.gestorLogging = GestorLoggingUnificado.crearMinimal(stdout, stderr);
    }

    public void hacerAlgo() {
        try {
            gestorLogging.info("MiComponente", "Iniciando operación...");

            // Lógica...

            gestorLogging.info("MiComponente", "Operación completada");
        } catch (Exception e) {
            gestorLogging.error("MiComponente", "Error en operación", e);
        }
    }
}
```

---

## Sistema de Internacionalización

### I18nUI (Traducciones de UI)

**Archivo:** `src/main/java/com/burpia/i18n/I18nUI.java`

**Estructura:**
```java
public interface I18nUI {
    interface General {
        String COMPLEMENTO_SEGURIDAD_IA();
        String ENTORNO();
        String CONFIGURACION_IA();
        String ACTIVADO();
        String DESACTIVADO();
        // ...
    }

    interface PanelHallazgos {
        String TITULO();
        String FILTRO_BUSQUEDA();
        String FILTRO_SEVERIDAD();
        // ...
    }

    // Más interfaces para cada componente
}
```

**Uso:**
```java
// ❌ NUNCA hagas esto:
JLabel label = new JLabel("Hallazgos");

// ✅ SIEMPRE haz esto:
JLabel label = new JLabel(I18nUI.PanelHallazgos.TITULO());
```

### I18nLogs (Traducciones de Logs)

**Archivo:** `src/main/java/com/burpia/i18n/I18nLogs.java`

**Estructura similar a I18nUI pero para mensajes de log.**

**Uso:**
```java
// En logging
gestorLogging.info("MiComponente", I18nLogs.MI_MENSAJE_LOG());

// I18nLogs.tr() aplica traducción automáticamente
// GestorLoggingUnificado ya llama a I18nLogs.tr() internamente
```

### Idiomas Soportados

- **Español (ES):** Por defecto
- **English (EN):** Soporte completo

**Configuración:**
```java
// En config.json
"idiomaUi": "es"  // o "en"

// Cambiar idioma en runtime
I18nUI.establecerIdioma("es");
```

---

## Integración con Agentes

### Tipos de Agentes Soportados

**Archivo:** `src/main/java/com/burpia/config/AgenteTipo.java`

```java
public enum AgenteTipo {
    CLAUDE_CODE(
        "Claude Code",
        "~/.local/bin/claude --dangerously-skip-permissions",
        "%USERPROFILE%\\.local\\bin\\claude.exe --dangerously-skip-permissions",
        "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-CLAUDE-ES.md",
        "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-CLAUDE-EN.md"
    ),
    FACTORY_DROID(
        "Factory Droid",
        "~/.local/bin/droid",
        "%USERPROFILE%\\bin\\droid.exe",
        "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-DROID-ES.md",
        "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-DROID-EN.md"
    ),
    GEMINI_CLI(
        "Gemini CLI",
        "~/.local/bin/gemini --yolo",
        "%USERPROFILE%\\bin\\gemini.exe --yolo",
        "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-GEMINI-ES.md",
        "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-GEMINI-EN.md"
    );
}
```

### Configuración de Agente

**En config.json:**
```json
{
  "agenteHabilitado": true,
  "tipoAgente": "CLAUDE_CODE",
  "rutasBinarioPorAgente": {
    "CLAUDE_CODE": "/home/user/.local/bin/claude"
  },
  "agentePreflightPrompt": "You are a security assistant...",
  "agentePrompt": "Analyze this finding...",
  "agenteDelay": 800
}
```

### Inyección de Prompts

**Automática:**
- Al iniciar el agente, se inyecta el `preflightPrompt`
- Al enviar un hallazgo, se inyecta el `prompt` con variables

**Manual:**
- Botón "Inyectar payload" en PanelAgente
- Campo de texto para inyección manual

### Comunicación vía MCP

BurpIA expone un **MCP Server** que los agentes pueden usar para acceder a herramientas:

- **burpia://send-to-agent** - Enviar hallazgo al agente
- **burpia://get-findings** - Obtener lista de hallazgos
- **burpia://get-stats** - Obtener estadísticas

---

## Convenciones de Código

### 1. Nomenclatura

**Paquetes:** `com.burpia.{modulo}`
- `config` - Configuración
- `model` - Modelos de datos
- `ui` - Componentes UI
- `util` - Utilidades
- `i18n` - Internacionalización
- `analyzer` - Análisis

**Clases:** `PascalCase` (Inglés para clases técnicas)
```java
public class GestorConfiguracion { }
public class PanelHallazgos extends JPanel { }
```

**Métodos públicos:** `camelCase` (Español para métodos expuestos)
```java
public void registrarHallazgo(Hallazgo hallazgo) { }
public String obtenerProveedorAI() { }
```

**Variables privadas:** `camelCase`
```java
private ConfiguracionAPI config;
private PrintWriter stdout;
```

**Constantes:** `UPPER_SNAKE_CASE`
```java
private static final int MAX_HALLAZGOS = 1000;
private static final String PREFIJO_LOG = "[BurpIA] ";
```

### 2. Orden de Declaración en Clases

```java
public class MiClase {
    // 1. Constantes estáticas
    private static final Logger LOGGER = Logger.getLogger(...);
    private static final int MAX_ITEMS = 100;

    // 2. Campos estáticos
    private static int contadorGlobal = 0;

    // 3. Campos de instancia (final primero)
    private final ConfiguracionAPI config;
    private GestorTareas gestorTareas;

    // 4. Constructores
    public MiClase(ConfiguracionAPI config) { }

    // 5. Métodos públicos
    public void hacerAlgo() { }

    // 6. Métodos protegidos
    protected void hacerAlgoInterno() { }

    // 7. Métodos privados
    private void validar() { }

    // 8. Clases internas
    private static class MiClaseInterna { }
}
```

### 3. Comentarios y Javadoc

**Métodos públicos deben tener Javadoc:**
```java
/**
 * Registra un nuevo hallazgo de seguridad.
 *
 * @param hallazgo El hallazgo a registrar (no puede ser null)
 * @throws IllegalArgumentException si hallazgo es null
 */
public void registrarHallazgo(Hallazgo hallazgo) {
    if (hallazgo == null) {
        throw new IllegalArgumentException("hallazgo no puede ser null");
    }
    // ...
}
```

**Comentarios inline para lógica compleja:**
```java
// Calcular hash SHA-256 para deduplicación
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));

// Convertir a hex string
StringBuilder hexString = new StringBuilder();
for (byte b : hash) {
    hexString.append(String.format("%02x", b));
}
```

### 4. Manejo de Errores

**Siempre usa GestorLoggingUnificado:**
```java
try {
    // Operación que puede fallar
    procesarDatos();
} catch (IOException e) {
    gestorLogging.error("MiComponente", "Error procesando datos", e);
    // Degradación elegante
    return Collections.emptyList();
}
```

**Valida argumentos:**
```java
public void setConfig(ConfiguracionAPI config) {
    if (config == null) {
        throw new IllegalArgumentException("config no puede ser null");
    }
    this.config = config;
}
```

### 5. Swing/UI Threading

**SIEMPRE ejecuta actualizaciones UI en EDT:**
```java
// ❌ NUNCA actualices UI fuera de EDT
tabla.setModel(nuevoModelo);

// ✅ SIEMPRE usa UIUtils.ejecutarEnEdt
UIUtils.ejecutarEnEdt(() -> {
    tabla.setModel(nuevoModelo);
    tabla.repaint();
});
```

### 6. Recursos y Cleanup

**Cierra recursos en shutdown:**
```java
public void shutdown() {
    // 1. Cancelar tareas
    if (gestorTareas != null) {
        gestorTareas.cancelarTodo();
    }

    // 2. Cerrar ExecutorService
    if (executorService != null) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // 3. Cerrar streams
    if (stdout != null) {
        stdout.close();
    }
}
```

---

## Flujo de Trabajo para Agentes

Cuando trabajes en este proyecto, sigue este flujo:

### 1. 📖 LEER Contexto

**PRIMERO** lee estos archivos en orden:

1. **AGENTS.md** (este documento) - Vista general
2. **CLAUDE.md** en directorios clave - Contexto específico:
   - `src/main/java/com/burpia/CLAUDE.md`
   - `src/main/java/com/burpia/config/CLAUDE.md`
   - `src/main/java/com/burpia/ui/CLAUDE.md`
   - `src/main/java/com/burpia/util/CLAUDE.md`
3. **Documentación relevante** en `docs/`:
   - `docs/FUNCIONES.md` - API de funciones
   - `docs/COMPONENTES_UI.md` - Componentes UI
   - `docs/I18N.md` - Claves de i18n

### 2. 🔍 ENTENDER la Tarea

**Identifica:**
- ¿Qué componente necesitas modificar?
- ¿Qué nuevas características necesitas agregar?
- ¿Qué partes del código existente puedes reutilizar?

**Busca código existente:**
- Usa `Grep` para buscar patrones similares
- Revisa clases relacionadas en el mismo paquete
- Lee CLAUDE.md en directorios relevantes

### 3. ✅ APLICAR Principios

**Antes de escribir código, pregúntate:**

- ¿Puedo reutilizar `GestorLoggingUnificado` en lugar de crear mi propio logger?
- ¿Puedo usar `Normalizador` en lugar de validar strings manualmente?
- ¿Puedo usar `I18nLogs`/`I18nUI` en lugar de strings hardcoded?
- ¿Puedo usar `UIUtils` en lugar de duplicar código UI?
- ¿Hay una clase de utilidad que ya hace lo que necesito?

### 4. 💻 ESCRIBIR Código

**Sigue las convenciones:**
- Nomenclatura correcta
- Javadoc para métodos públicos
- Manejo de errores con `GestorLoggingUnificado`
- Validaciones con `Normalizador`
- Internacionalización con `I18n*`
- Actualizaciones UI en EDT con `UIUtils`

### 5. 🧪 COMPILAR y Probar

**⚠️ OBLIGATORIO: Después de CADA implementación, SIEMPRE ejecuta:**

```bash
./build-jar.sh
```

Este script:
1. Limpia el build anterior
2. Compila todo el proyecto
3. Genera el JAR final
4. Valida que no haya errores de compilación
5. Muestra el resultado del build

**Si el build es exitoso:**
- ✅ Verás "BUILD SUCCESSFUL"
- ✅ El JAR estará en `build/libs/BurpIA-1.0-SNAPSHOT.jar`
- ✅ Puedes proceder a commit

**Si hay errores de compilación:**
- ❌ Verás "BUILD FAILED"
- ❌ Revisa los errores mostrados
- ❌ Corrige:
  - Imports faltantes
  - Tipos de datos incorrectos
  - Métodos que no existen
  - Errores de sintaxis
- ❌ Vuelve a ejecutar `./build-jar.sh`

**📌 REGLA DE ORO:**
> **NO hagas commit si `./build-jar.sh` no muestra "BUILD SUCCESSFUL"**

El build debe pasar **siempre** antes de commit. Esto asegura que:
- El código compila correctamente
- No hay errores obvios
- El JAR es funcional
- Los cambios son listos para producción

### 6. 🧹 LIMPIAR Código Huérfano

**⚠️ OBLIGATORIO: Antes de commit, elimina TODO el código no utilizado:**

```bash
# Usa tu IDE para detectar código no usado
# - IntelliJ: Analyze → Inspect Code → "Unused declaration"
# - VS Code: Ejecuta "SonarLint" o herramienta similar
```

**Elimina antes de commit:**
- ❌ Métodos privados que nunca se llaman
- ❌ Campos que se declaran pero nunca se usan
- ❌ Imports que no se utilizan
- ❌ Bloques de código comentados
- ❌ `// TODO` o `// FIXME` sin issue

**Regla:**
> **No debe haber NI UNA línea de código muerto en el repositorio.**
> **Si el código no se usa, se ELIMINA.**

### 7. 📝 COMMIT con Mensaje Claro

```bash
git commit -m "$(cat <<'EOF'
[TIPO] Descripción breve del cambio

Detalles adicionales sobre el cambio y su propósito.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

**Tipos de commit:**
- `feat` - Nueva característica
- `fix` - Corrección de bug
- `refactor` - Refactorización (sin cambio funcional)
- `docs` - Cambios en documentación
- `style` - Formato/código style (sin lógica)
- `test` - Adición de tests
- `chore` - Mantenimiento (deps, build, etc.)

### 8. 📚 ACTUALIZAR Documentación (si aplica)

**Si agregaste nuevas funciones:**
```bash
./gradlew generarDocsFunciones generarDocsUI generarDocsI18n
```

**Si agregaste nuevos componentes UI:**
- Actualiza `docs/COMPONENTES_UI.md` si es necesario
- Agrega CLAUDE.md en el directorio del nuevo componente

---

## Mejores Prácticas

### 1. Seguridad

- **Valida SIEMPRE** inputs del usuario
- **Nunca** expongas API keys en logs
- **Usa permisos restrictivos** en config.json (0600 en Unix)
- **Sanitiza** datos antes de mostrar en UI

### 2. Performance

- **Lazy loading:** Carga datos solo cuando se necesiten
- **Rate limiting:** Respeta los límites configurados
- **Deduplicación:** Usa hash SHA-256 antes de procesar
- **Batching:** Agrupa operaciones cuando sea posible

### 3. Mantenibilidad

- **DRY:** Reutiliza código existente
- **Single Responsibility:** Una clase, una responsabilidad
- **SOLID:** Sigue principios SOLID
- **KISS:** Keep It Simple, Stupid

### 4. Testing (Manual)

- **Prueba en ambos idiomas:** ES y EN
- **Prueba en ambos temas:** Claro y oscuro
- **Prueba con diferentes proveedores:** OpenAI, Claude, Gemini
- **Prueba con datos edge cases:** Strings vacíos, null, valores extremos

### 5. Compatibilidad

- **Backward compat:** Mantén compatibilidad con configs existentes
- **Version checks:** Verifica versión de Burp Suite si usas APIs específicas
- **Graceful degradation:** La app debe funcionar aunque falten componentes opcionales

### 6. Calidad de Código

> **⚠️ REGLAS ESTRICTAS:**

#### Código Huérfano (Dead Code)

**🚫 PROHIBIDO:**
- Métodos privados que nunca se llaman
- Campos que se declaran pero nunca se usan
- Imports que no se utilizan
- Clases completas que no se referencian
- Bloques de código comentados
- `// TODO` o `// FIXME` sin issue de seguimiento

**✅ ACCIÓN REQUERIDA:**
- Antes de commit, elimina TODO el código no utilizado
- Usa tu IDE para detectar métodos/imports no usados
- Si eliminas funcionalidad, elimina TAMBIÉN:
  - Los métodos que la implementaban
  - Los campos relacionados
  - Los imports ya no necesarios
  - Los comentarios que la describían

**🔍 VALIDACIÓN:**
```bash
# Ejecutar antes de commit (si está disponible)
./gradlew checkDeadCode  # o tu herramienta equivalente

# O usar tu IDE:
# - IntelliJ: Analyze → Inspect Code → "Unused declaration"
# - VS Code: Extensión "SonarLint" o similar
```

#### Comentarios en el Código

**🚫 EVITAR:**
- Comentarios que explican "qué hace" el código (el código debe hablar por sí solo)
- Comentarios obsoletos o desactualizados
- Bloques grandes de comentario que podrían ser documentación separada

**✅ USAR COMENTARIOS PARA:**
- **Javadoc** en métodos públicos (obligatorio)
- Explicar **POR QUÉ** se hace algo no obvio
- Explicar **trade-offs** o decisiones arquitectónicas
- Referencias a documentación externa o issues

**📝 EJEMPLOS: CORRECTO ✅**
```java
/**
 * Envía un hallazgo al agente AI para análisis adicional.
 *
 * @param hallazgo El hallazgo a analizar (no puede ser null)
 * @throws IllegalStateException si el agente no está configurado
 */
public void enviarAlAgente(Hallazgo hallazgo) {
    if (config.obtenerTipoAgente() == null) {
        throw new IllegalStateException("Agente no configurado");
    }
    // ...
}
```

```java
// Usar SHA-256 en lugar de MD5 por seguridad (colisiones)
String hash = calcularHashSHA256(contenido);
```

**❌ EJEMPLOS: INCORRECTO**
```java
// Este método envía el hallazgo al agente
public void enviarAlAgente(Hallazgo h) { }  // ❌ Javadoc faltante, nombre no descriptivo

// // TODO: implementar esto luego
// public void metodoFuturo() { }  // ❌ Código comentado

// String nombre = "Juan";  // Código antiguo
// nombre = "Pedro";  // ❌ Comentar en lugar de eliminar
```

**🎯 PRINCIPIO:**
> **El código debe ser auto-documentado. Si necesitas muchos comentarios para explicarlo, probablemente necesitas refactorizar.**

**Excepciones:**
- Javadoc es **OBLIGATORIO** en APIs públicas
- Comentarios breves para explicar **POR QUÉ** (no qué) son aceptables
- Referencias a issues/tickets son bienvenidas: `// #ISSUE-123`

---

## Referencias Rápidas

### Comandos de Build

```bash
# ⭐ COMPILACIÓN PRINCIPAL (USA ESTE SIEMPRE)
./build-jar.sh              # Limpia + compila + genera JAR + valida

# Comandos individuales (solo si los necesitas específicamente)
./gradlew jar               # Solo compilar JAR
./gradlew jar --quiet       # Compilar sin output
./gradlew clean             # Limpiar build
./gradlew clean build       # Limpiar y compilar

# Generar documentación
./gradlew generarDocsFunciones  # Documentación de funciones
./gradlew generarDocsUI         # Documentación de UI
./gradlew generarDocsI18n       # Documentación de i18n
```

> **⚠️ IMPORTANTE:** Usa `./build-jar.sh` después de cada implementación. Es la forma más confiable de validar que todo compila correctamente.

### Rutas Importantes

```
Configuración:               ~/.burpia/config.json
Logs:                        En PanelConsola (via GestorConsolaGUI)
JAR output (ruta fija):      /Users/jaimearestrepo/Proyectos/BurpIA/build/libs/BurpIA-{VERSION}.jar
                            Donde {VERSION} es la versión actual (ej: 1.0.2)
Versión actual del código:   src/main/java/com/burpia/util/VersionBurpIA.java
```

> **📌 REGLA:** El binario SIEMPRE debe generarse en:
> `/Users/jaimearestrepo/Proyectos/BurpIA/build/libs/BurpIA-{VERSION}.jar`
>
> Donde `{VERSION}` se obtiene dinámicamente de `VersionBurpIA.java`.
> El script `build-jar.sh` se encarga de esto automáticamente.

### Claves de Configuración Comunes

```java
// AI Provider
config.obtenerProveedorAI()           // "openai", "claude", etc.
config.obtenerApiKey("openai")
config.obtenerModelo("openai")

// Rendimiento
config.obtenerRetrasoSegundos()       // Delay entre peticiones
config.obtenerMaximoConcurrente()     // Max operaciones concurrentes
config.obtenerMaximoHallazgosTabla()  // Max hallazgos en tabla

// UI
config.esTemaClaro()                  // true = claro, false = oscuro
config.obtenerIdiomaUi()              // "es" o "en"

// Flags
config.esDetallado()                  // Logging detallado
config.escaneoPasivoHabilitado()      // Escaneo pasivo activo
config.autoGuardadoIssuesHabilitado() // Auto-guardar a Issues
config.autoScrollConsolaHabilitado()  // Auto-scroll en consola

// Agente
config.agenteHabilitado()             // Agente habilitado
config.obtenerTipoAgente()            // "CLAUDE_CODE", etc.
```

### Niveles de Severidad

```
CRITICAL - Vulnerabilidad crítica
HIGH     - Vulnerabilidad alta
MEDIUM   - Vulnerabilidad media
LOW      - Vulnerabilidad baja
INFO     - Informativo
```

### Estados de Tarea

```
PENDIENTE   - En cola
PROCESANDO  - Analizando actualmente
COMPLETADO  - Análisis finalizado
ERROR       - Falló el análisis
```

### Proveedores AI Soportados

```
openai   - OpenAI (GPT-4, GPT-3.5)
claude   - Anthropic (Claude 3.5 Sonnet)
gemini   - Google (Gemini Pro)
ollama   - Ollama (local)
moonshot - Moonshot AI
zai      - Z.ai
minimax  - MiniMax
custom   - Custom endpoint
```

### Agentes Soportados

```
CLAUDE_CODE  - Claude Code CLI
FACTORY_DROID - Factory Droid CLI
GEMINI_CLI   - Gemini CLI
```

---

## Conclusión

Este proyecto sigue **principios SOLID**, **DRY**, y patrones de **diseño limpio**. La infraestructura está consolidada y lista para usarse.

**Antes de escribir código nuevo:**
1. ✅ Revisa si ya existe una utilidad que haga lo que necesitas
2. ✅ Lee la documentación relevante
3. ✅ Sigue las convenciones establecidas
4. ✅ Usa `GestorLoggingUnificado`, `Normalizador`, `I18n*`, `UIUtils`

**Recuerda los tres principios:**
- 🛡️ **CONFIABLE** - Validación, manejo de errores, degradación elegante
- ⚡ **EFICIENTE** - Lazy loading, rate limiting, deduplicación
- 🔄 **DRY** - Reutiliza código existente, no lo dupliques

**¡El proyecto está listo para que trabajes en él de manera efectiva!**

---

**¿Preguntas? Consulta:**
- `docs/` - Documentación técnica
- `AGENT-*.md` - Guías de agentes específicos
- CLAUDE.md en directorios específicos - Contexto detallado

**Happy coding! 🚀**
