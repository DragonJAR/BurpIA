# AGENTS.md - Guía Completa para Agentes de IA

**BurpIA - Extensión de Burp Suite con Análisis AI**
**Versión:** 1.0.2  
**Última actualización:** 3 de marzo, 2026

---

## Tabla de Contenidos

1. [Resumen del Proyecto](#resumen-del-proyecto)
2. [Arquitectura General](#arquitectura-general)
3. [Componentes Principales](#componentes-principales)
4. [Sistema de Configuración](#sistema-de-configuración)
5. [Proveedores AI Soportados](#proveedores-ai-soportados)
6. [Sistema de Tareas](#sistema-de-tareas)
7. [Componentes UI](#componentes-ui)
8. [Utilidades y Helpers](#utilidades-y-helpers)
9. [Sistema de Internacionalización](#sistema-de-internacionalización)
10. [Testing](#testing)
11. [Build y Deploy](#build-y-deploy)
12. [Estado Actual y Cambios Recientes](#estado-actual-y-cambios-recientes)
13. [Conventions y Standards](#conventions-y-standards)
14. [Documentación Relacionada](#documentación-relacionada)

---

## 📚 Documentación Relacionada

Para información más detallada sobre temas específicos, consultar:

### Documentación de UI
- **[COMPONENTES_UI.md](docs/COMPONENTES_UI.md)** - Documentación detallada de componentes de interfaz
  - Descripción completa de cada panel UI
  - Métodos y propiedades expuestas
  - Ejemplos de uso

### Documentación de Funciones API
- **[FUNCIONES.md](docs/FUNCIONES.md)** - API completa de funciones y métodos
  - Firmas de métodos generadas desde código fuente
  - Parámetros y retornos
  - Organización por módulos (Configuración, Modelos, Utilidades, UI, etc.)

### Documentación de Internacionalización
- **[I18N.md](docs/I18N.md)** - Claves de internacionalización
  - Traducciones de interfaz de usuario
  - Traducciones de mensajes de log
  - Idiomas soportados (español, inglés)

### Documentación de Plugins Burp
- **[PLUGINS-BURP.md](docs/PLUGINS-BURP.md)** - Integración con Burp Suite
  - Extensión Burp Suite
  - Montoya API
  - Handlers HTTP

---

---

## Resumen del Proyecto

**BurpIA** es una extensión profesional de Burp Suite que utiliza Inteligencia Artificial para analizar automáticamente el tráfico HTTP y identificar vulnerabilidades de seguridad.

### Características Principales

- ✅ **Análisis AI automático** de tráfico HTTP/HTTPS
- ✅ **Soporte multi-proveedor** (OpenAI, Claude, Gemini, Ollama)
- ✅ **Sistema de tareas** con cola, pausa, reanudación y cancelación
- ✅ **Interfaz bilingüe** (español/inglés)
- ✅ **Agentes CLI** integrados (Gemini CLI)
- ✅ **Gestión de hallazgos** con exportación a Burp Issues
- ✅ **Sistema de estadísticas** en tiempo real
- ✅ **Configuración persistente** en JSON
- ✅ **Logging unificado** y depuración

### Stack Tecnológico

- **Lenguaje:** Java 17+ (probado con Java 21)
- **Build:** Gradle 8.5
- **UI:** Swing (integrado con Burp Suite Montoya API)
- **HTTP:** OkHttp4
- **JSON:** Gson
- **Testing:** JUnit 5

---

## Arquitectura General

```
BurpIA/
├── src/main/java/com/burpia/
│   ├── ExtensionBurpIA.java          # Punto de entrada principal
│   ├── ManejadorHttpBurpIA.java      # Maneja captura y análisis HTTP
│   ├── analyzer/                     # Motor de análisis AI
│   │   ├── AnalizadorAI.java         # Analizador principal con reintentos
│   │   ├── ConstructorPrompts.java   # Construye prompts según idioma
│   │   └── PoliticaReintentos.java   # Lógica de reintentos y backoff
│   ├── model/                        # Modelos de datos
│   │   ├── Hallazgo.java             # Hallazgo de seguridad
│   │   ├── Tarea.java                # Tarea en cola de análisis
│   │   ├── Estadisticas.java         # Estadísticas globales
│   │   ├── SolicitudAnalisis.java    # Solicitud HTTP a analizar
│   │   └── ResultadoAnalisisMultiple.java
│   ├── ui/                           # Componentes de interfaz
│   │   ├── PestaniaPrincipal.java    # Panel principal con pestañas
│   │   ├── PanelConsola.java         # Consola de logging
│   │   ├── PanelHallazgos.java       # Tabla de hallazgos
│   │   ├── PanelTareas.java          # Cola de tareas
│   │   ├── PanelEstadisticas.java   # Dashboard de estadísticas
│   │   ├── PanelAgente.java          # Terminal de agentes CLI
│   │   └── DialogoConfiguracion.java # Diálogo de preferencias
│   ├── util/                         # Utilidades reutilizables
│   │   ├── GestorTareas.java         # Gestor de cola de tareas
│   │   ├── GestorConfiguracion.java  # Persistencia de configuración
│   │   ├── GestorLoggingUnificado.java # Logging centralizado
│   │   ├── Normalizador.java         # Validación de strings
│   │   ├── ParserRespuestasAI.java   # Parseo de respuestas AI
│   │   └── LimitadorTasa.java        # Rate limiting
│   ├── config/                       # Configuración
│   │   ├── ConfiguracionAPI.java     # Modelo de configuración
│   │   ├── ProveedorAI.java          # Enums de proveedores
│   │   └── AgenteTipo.java           # Tipos de agentes
│   └── i18n/                         # Internacionalización
│       ├── I18nUI.java               # UI localizada
│       └── I18nLogs.java             # Logs localizados
└── build.gradle                      # Build Gradle

---

## Componentes Principales

### 1. ExtensionBurpIA

**Ubicación:** `src/main/java/com/burpia/ExtensionBurpIA.java`

**Responsabilidades:**
- Punto de entrada de la extensión (implementa `MontoyaExtension`)
- Inicialización de todos los componentes
- Registro de handlers HTTP
- Configuración de UI (pestañas, menús contextuales)
- Gestión del ciclo de vida

**Inicialización:**
```java
@Override
public void initialize(MontoyaApi api) {
    // 1. Crear gestor de configuración
    // 2. Inicializar logging
    // 3. Crear manejador HTTP
    // 4. Crear gestor de tareas
    // 5. Crear UI principal
    // 6. Registrar menús contextuales
    // 7. Inicializar agente si está habilitado
}
```

### 2. ManejadorHttpBurpIA

**Ubicación:** `src/main/java/com/burpia/ManejadorHttpBurpIA.java`

**Responsabilidades:**
- Capturar tráfico HTTP/HTTPS (si escaneo pasivo habilitado)
- Crear tareas de análisis desde solicitudes capturadas
- Gestionar la cola de ejecución
- Controlar concurrencia y rate limiting
- Manejar reintentos y errores

**Campos Clave:**
```java
private final ExecutorService executorService;        // Pool de threads
private final Map<String, Future<?>> ejecucionesActivas;  // Futures en ejecución
private final Map<String, AnalizadorAI> analizadoresActivos;  // Analizadores activos
private final LimitadorTasa limitador;                // Rate limiting
private final Map<String, ContextoReintento> contextosReintento;  // Contextos de reintento
```

**Métodos Importantes:**
- `procesarSolicitud()`: Crea tarea desde solicitud HTTP
- `cancelarEjecucionActiva()`: Cancela tarea inmediatamente (HTTP + thread)
- `reencolarTarea()`: Reintenta análisis de tarea

### 3. AnalizadorAI

**Ubicación:** `src/main/java/com/burpia/analyzer/AnalizadorAI.java`

**Responsabilidades:**
- Ejecutar análisis AI de una solicitud HTTP
- Construir prompts según idioma y proveedor
- Realizar llamada HTTP al proveedor AI
- Parsear respuesta JSON y extraer hallazgos
- Implementar reintentos con backoff exponencial
- Detectar y responder a cancelación/pausa

**Características Clave:**
- **Cancelación inmediata**: Usa `Call.cancel()` de OkHttp para detener llamadas HTTP
- **Verificación de estado**: Chequea `estaTareaCancelada()` y `estaTareaPausada()` periódicamente
- **Sistema de reintentos**: Hasta 5 intentos con backoff exponencial
- **Timeout por modelo**: Configurable según proveedor y modelo

**Flujo de Ejecución:**
```java
1. verificarCancelacion()
2. esperarSiPausada()
3. Adquirir permiso de limitador
4. Esperar delay (retrasoSegundos)
5. llamarAPIAIConRetries()
   - Para cada intento:
     - verificarCancelacion()
     - esperarSiPausada()
     - llamarAPIAI()
     - Si error reintentable → backoff y reintentar
6. Parsear respuesta
7. Notificar callback (alCompletarAnalisis / alErrorAnalisis / alCanceladoAnalisis)
8. Liberar permiso de limitador (finally)
```

### 4. GestorTareas

**Ubicación:** `src/main/java/com/burpia/util/GestorTareas.java`

**Responsabilidades:**
- Gestionar cola de tareas (CRUD)
- Actualizar estado de tareas
- Sincronizar con modelo de tabla UI
- Verificar tareas atascadas (timeout > 5 minutos)
- Aplicar retención de tareas finalizadas (max 2000 por defecto)
- Notificar manejadores de cancelación/pausa/reanudar

**Estados de Tarea:**
```java
ESTADO_EN_COLA        // Esperando ser procesada
ESTADO_ANALIZANDO     // Actualmente en análisis
ESTADO_PAUSADO        // Pausada por usuario
ESTADO_COMPLETADO     // Análisis exitoso
ESTADO_ERROR          // Error en análisis
ESTADO_CANCELADO      // Cancelada por usuario
```

**Métodos de Control de Flujo:**
- `pausarTarea(id)`: Pausa tarea individual
- `reanudarTarea(id)`: Reanuda tarea pausada
- `cancelarTarea(id)`: Cancela tarea
- `pausarTodasActivas()`: Pausa en masa
- `reanudarTodasPausadas()`: Reanuda en masa
- `cancelarTodas()`: Cancela todo

**Características de Fiabilidad:**
- `ReentrantLock` para thread-safety
- `ConcurrentHashMap` para almacenamiento
- Verificación periódica de tareas atascadas
- Limpieza automática de tareas finalizadas
- Separación de bloqueos de datos y UI (evita congelación)

### 5. ModeloTablaHallazgos

**Ubicación:** `src/main/java/com/burpia/ui/ModeloTablaHallazgos.java`

**Responsabilidades:**
- Modelo de tabla para hallazgos (extends `AbstractTableModel`)
- Agregar/eliminar/actualizar hallazgos
- Filtrado y búsqueda
- Gestión de colas visibles
- Purging automático cuando excede límite

**Características:**
- Thread-safe para actualizaciones desde background
- Soporta filtros de severidad
- Búsqueda en tiempo real
- Máximo de hallazgos configurable (default: 1000)

---

## Sistema de Configuración

### ConfiguracionAPI

**Ubicación:** `src/main/java/com/burpia/config/ConfiguracionAPI.java`

**Propiedades Principales:**

#### Proveedores AI
```java
private String proveedorAI;                    // openai, claude, gemini, ollama
private Map<String, String> apiKeysPorProveedor;
private Map<String, String> urlsBasePorProveedor;
private Map<String, String> modelosPorProveedor;
private Map<String, Integer> maxTokensPorProveedor;
private Map<String, Long> tiemposEsperaPorModelo;
```

#### Rendimiento
```java
private int retrasoSegundos;                   // Delay entre peticiones (default: 0)
private int maximoConcurrente;                 // Máximo operaciones paralelas (default: 10)
private int maximoHallazgosTabla;              // Límite de hallazgos en UI (default: 1000)
private long tiempoEsperaAI;                   // Timeout global (default: 120s)
```

#### UI/Tema
```java
private String tema;                           // claro, oscuro, auto
private String idiomaUi;                       // es, en
private String nombreFuenteEstandar;
private int tamanioFuenteEstandar;
private String nombreFuenteMono;
private int tamanioFuenteMono;
```

#### Flags
```java
private boolean detallado;                     // Logging verbose
private boolean escaneoPasivoHabilitado;       // Capturar tráfico automáticamente
private boolean autoGuardadoIssuesHabilitado;  // Exportar a Burp Issues
private boolean autoScrollConsolaHabilitado;   // Auto-scroll en consola
private boolean alertasHabilitadas;            // Mostrar alertas
private boolean ignorarErroresSSL;             // Ignorar certificados SSL inválidos
private boolean soloProxy;                     // Solo tráfico proxy
```

#### Prompts
```java
private String promptConfigurable;             // Prompt personalizado
private boolean promptModificado;              // Flag de prompt modificado
```

#### Agente
```java
private boolean agenteHabilitado;              // Agente CLI activo
private String tipoAgente;                     // Tipo de agente
private Map<String, String> rutasBinarioPorAgente;
private String agentePreflightPrompt;
private String agentePrompt;
private long agenteDelay;                      // Delay del agente (ms)
```

### GestorConfiguracion

**Ubicación:** `src/main/java/com/burpia/config/GestorConfiguracion.java`

**Responsabilidades:**
- Cargar/guardar `~/.burpia/config.json`
- Validar configuración
- Proporcionar valores por defecto
- Migración de configuraciones legacy

**Formato JSON:**
```json
{
  "proveedorAI": "openai",
  "apiKeysPorProveedor": {
    "openai": "sk-..."
  },
  "modelosPorProveedor": {
    "openai": "gpt-4o"
  },
  "maximoConcurrente": 5,
  "idiomaUi": "es",
  "tema": "oscuro"
}
```

---

## Proveedores AI Soportados

### OpenAI
- **Modelos:** GPT-4o, GPT-4o-mini, GPT-3.5-turbo
- **Endpoint:** `https://api.openai.com/v1/chat/completions`
- **Headers:** `Authorization: Bearer {apiKey}`
- **Timeout por defecto:** 120s

### Claude (Anthropic)
- **Modelos:** Claude 3.5 Sonnet, Claude 3 Opus, Claude 3 Haiku
- **Endpoint:** `https://api.anthropic.com/v1/messages`
- **Headers:** `x-api-key: {apiKey}`, `anthropic-version: 2023-06-01`
- **Timeout por defecto:** 120s

### Gemini (Google)
- **Modelos:** Gemini 1.5 Pro, Gemini 1.5 Flash
- **Endpoint:** `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Headers:** `x-goog-api-key: {apiKey}`
- **Timeout por defecto:** 120s

### Ollama (Local)
- **Modelos:** llama2, mistral, codellama, etc.
- **Endpoint:** `http://localhost:11434/api/generate`
- **Headers:** Ninguno (local)
- **Timeout por defecto:** 300s

### Características Comunes
- **Sistema de reintentos:** Hasta 5 intentos
- **Backoff exponencial:** 1s → 8s máximo
- **Soporte de streaming:** Sí (configurable)
- **Verificación SSL:** Opcional (ignorarErroresSSL)

---

## Sistema de Tareas

### Flujo de Tarea Completo

```
1. CAPTURA
   ManejadorHttpBurpIA.procesarSolicitud()
   ↓
2. CREACIÓN
   GestorTareas.crearTarea()
   ↓
3. EN COLA
   ModeloTablaTareas.agregarTarea()
   ↓
4. EJECUCIÓN
   ExecutorService.submit(AnalizadorAI)
   ↓
   a) marcarTareaAnalizando()
   b) Analizar con AI
   c) Actualizar tarea (COMPLETADO/ERROR/CANCELADO)
   ↓
5. LIMPIEZA
   finalizarEjecucionActiva()
   analizadoresActivos.remove(id)
```

### Control de Concurrency

**LimitadorTasa:**
- Basado en `Semaphore`
- Máximo de permisos configurable (default: 10)
- `adquirir()`: Bloquea hasta que hay permiso disponible
- `liberar()`: Libera permiso después de análisis

**ExecutorService:**
```java
ThreadPoolExecutor(
    corePoolSize: maxThreads,
    maximumPoolSize: maxThreads,
    keepAliveTime: 60s,
    workQueue: ArrayBlockingQueue<>(capacidadCola),
    threadFactory: "BurpIA-Thread-N"
    handler: AbortPolicy (rechaza si cola llena)
)
```

### Cancelación y Pausa

**Mecanismo de Cancelación:**
1. Usuario hace clic en "Cancelar"
2. `GestorTareas.cancelarTarea(id)` → marca como CANCELADO
3. `manejadorCancelacion.accept(id)` → `cancelarEjecucionActiva(id)`
4. `AnalizadorAI.cancelarLlamadaHttpActiva()` → cancela HTTP
5. `future.cancel(true)` → interrumpe thread
6. Lanzamiento de `IOException("Canceled")`
7. Catch → `callback.alCanceladoAnalisis()`

**Cancelación Inmediata (Reciente):**
- Usa `Call.cancel()` de OkHttp
- Libera socket TCP inmediatamente
- No espera timeout (60-120s)
- Siguiente tarea comienza en <1 segundo

---

## Componentes UI

### PestaniaPrincipal

**Ubicación:** `src/main/java/com/burpia/ui/PestaniaPrincipal.java`

**Pestañas:**
1. **Consola** - Logging de operaciones
2. **Hallazgos** - Tabla de hallazgos con filtros
3. **Tareas** - Cola de tareas con controles
4. **Estadísticas** - Dashboard con métricas
5. **Agente** - Terminal de agentes CLI

**Características:**
- Soporte de temas (claro/oscuro)
- Internacionalización (español/inglés)
- Actualización en tiempo real
- Persistencia de última pestaña seleccionada

### PanelHallazgos

**Componentes:**
- Tabla con columnas: Severidad, Título, URL, Confianza
- Filtro de búsqueda por texto
- Filtro de severidad (Critical, High, Medium, Low, Info)
- Menú contextual (click derecho)
- Exportar a CSV
- Enviar a Burp Issues

**Métodos Importantes:**
```java
obtenerHallazgoSeleccionado()   // Hallazgo actualmente seleccionado
agregarHallazgos(List<Hallazgo>)  // Agregar en lote
limpiarCompletados()              // Eliminar completados
aplicarFiltrosGuardados()        // Restaurar filtros
```

### PanelTareas

**Componentes:**
- Tabla con columnas: Estado, Tipo, URL, Mensaje
- Botones: Pausar/Reanudar todo, Cancelar todo, Limpiar completados
- Estadísticas en tiempo real
- Controles por tarea individual

**Estados Visuales:**
- 🟢 EN_COLA
- 🔵 ANALIZANDO
- ⏸️ PAUSADO
- ✅ COMPLETADO
- ❌ ERROR
- 🚫 CANCELADO

### PanelConsola

**Características:**
- Logging con niveles (VERBOSE, INFO, WARNING, ERROR)
- Auto-scroll configurable
- Filtrado por nivel
- Limpiar consola
- Copiar al portapapeles

### PanelEstadisticas

**Métricas:**
- Total de hallazgos creados
- Hallazgos visibles (filtrados)
- Distribución por severidad
- Solicitudes procesadas
- Tasa de errores
- Límite de hallazgos

### PanelAgente

**Características:**
- Terminal integrado para agentes CLI
- Soporte para Gemini CLI
- Input/output en tiempo real
- Configuración de delay (ms)
- Indicador de actividad

---

**📖 Ver también:** [Documentación completa de UI](docs/COMPONENTES_UI.md) para detalles exhaustivos de todos los componentes visuales.

---

## Utilidades y Helpers

### Normalizador

**Ubicación:** `src/main/java/com/burpia/util/Normalizador.java`

**Métodos:**
```java
static boolean esVacio(String)      // String null o vacío
static boolean noEsVacio(String)    // String con contenido
static String normalizarTexto(String) // Minúsculas, trim, sin espacios múltiples
static String sanitizarApiKey(String) // Oculta API key
```

**Principio DRY:** Centraliza validación de strings (91 usos en el código)

### GestorLoggingUnificado

**Ubicación:** `src/main/java/com/burpia/util/GestorLoggingUnificado.java`

**Responsabilidades:**
- Logging centralizado con múltiples canales:
  - GestorConsolaGUI (UI)
  - stdout (console)
  - stderr (errors)
  - MontoyaApi logging (Burp)
  - java.util.logging.Logger (fallback)

**Métodos:**
```java
info(String origen, String mensaje)
error(String origen, String mensaje)
error(String origen, String mensaje, Throwable)
separador()
```

### ParserRespuestasAI

**Ubicación:** `src/main/java/com/burpia/util/ParserRespuestasAI.java`

**Responsabilidades:**
- Parsear JSON de respuesta AI
- Extraer hallazgos de varios formatos:
  - Bloque `findings` estándar
  - JSON de texto libre (models "thinking")
  - Bloques markdown con JSON
- Manejo de JSON dañado o incompleto

### LimitadorTasa

**Ubicación:** `src/main/java/com/burpia/util/LimitadorTasa.java`

**Implementación:**
- Basado en `Semaphore`
- Controla concurrencia máxima
- `adquirir()`: Bloquea si no hay permisos
- `liberar()`: Libera permiso

### ProbadorConexionAI

**Ubicación:** `src/main/java/com/burpia/util/ProbadorConexionAI.java`

**Responsabilidades:**
- Probar conexión con proveedor AI
- Validar API key
- Verificar modelo disponible
- Timeout corto (5s)

### ReparadorJson

**Ubicación:** `src/main/java/com/burpia/util/ReparadorJson.java`

**Responsabilidades:**
- Reparar JSON dañado de IA
- Extraer JSON de texto libre
- Manejar bloques markdown
- Limpiar caracteres inválidos

---

**📖 Ver también:** [Documentación completa de funciones API](docs/FUNCIONES.md) para firmas detalladas de todos los métodos y utilidades.

---

## Sistema de Internacionalización

### I18nUI

**Ubicación:** `src/main/java/com/burpia/i18n/I18nUI.java`

**Soporte:**
- Español (es) - Idioma principal
- Inglés (en) - Fallback

**Categorías:**
```java
interface Consola { ... }
interface Tareas { ... }
interface Hallazgos { ... }
interface Estadisticas { ... }
interface Configuracion { ... }
```

**Uso:**
```java
I18nUI.Tareas.TITULO_LISTA()
I18nUI.Hallazgos.FILTRO_SEVERIDAD()
I18nUI.Estadisticas.RESUMEN_TOTAL(total, visibles)
```

### I18nLogs

**Ubicación:** `src/main/java/com/burpia/i18n/I18nLogs.java`

**Mensajes de logging localizados:**
- Errores
- Advertencias
- Información
- Rastreo (debug)

---

**📖 Ver también:** [Documentación completa de internacionalización](docs/I18N.md) para todas las claves de traducción disponibles.

---

## Testing

### Framework
- **JUnit 5** (Jupiter)
- **Jacoco** (cobertura de código)
- **PMD** (análisis estático)

### Cobertura Actual
- **394 tests** passing
- **Cobertura:** ~70-80% en componentes principales
- **Tests integrados:** ManejadorHttpBurpIA, GestorTareas, etc.

### Test Categories
1. **Unit Tests:** Clases individuales
2. **Integration Tests:** Componentes integrados
3. **UI Tests:** Componentes Swing
4. **Performance Tests:** Benchmarks

### Ejecutar Tests
```bash
./build-jar.sh              # Build con tests
./gradlew test              # Solo tests
./gradlew test --tests "*Test"  # Tests específicos
```

---

## Build y Deploy

### Build
```bash
./build-jar.sh              # Build completo con tests
./build-jar.sh --no-test     # Build rápido sin tests
```

**Output:** `build/libs/BurpIA-1.0.2.jar` (~8.9MB)

### Instalación en Burp Suite
1. Abrir Burp Suite Professional
2. **Extender** → **Extensions**
3. Click **Add**
4. Seleccionar `build/libs/BurpIA-1.0.2.jar`
5. Configurar API keys en **Settings**

### Dependencias Principales
```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'net.portswigger.burp.extensions:montoya-api:2024.4'
```

---

## Estado Actual y Cambios Recientes

### Versión Actual: 1.0.2

### Cambios Recientes (Últimos 10 Commits)

#### 1. Cancelación Inmediata de Llamadas HTTP ⭐ **MÁS RECIENTE**
**Commit:** `f5a587b`

**Problema:** Cuando se pausaba/cancelaba una tarea, la siguiente esperaba hasta 2 minutos.

**Solución:**
- Implementado `Call.cancel()` de OkHttp
- Campo `volatile Call llamadaHttpActiva` en AnalizadorAI
- Mapa `analizadoresActivos` en ManejadorHttpBurpIA
- Cancelación en 2 pasos: HTTP → Thread

**Resultado:**
- Tiempo de respuesta: 60-120s → <1s
- UX mejorada significativamente

**Archivos modificados:**
- `AnalizadorAI.java`
- `ManejadorHttpBurpIA.java`

#### 2. Optimización DRY - Parser
**Commits:** `b4ba2ad`, `2ab50c2`

**Mejoras:**
- Extracción de JSON genérica reutilizable
- Eliminación de duplicación en parsers
- Soporte para modelos "thinking" (Claude, Gemini)

#### 3. Corrección de Bug - alErrorAnalisis
**Commit:** `63643c1`

**Problema:** `debeDescartarResultado()` verificaba cancelación Y finalización, causando que errores no se marcaran en tareas finalizadas.

**Solución:**
- `estaTareaCancelada()`: Solo verifica cancelación
- `debeDescartarResultado()`: Verifica cancelación Y finalización
- `alErrorAnalisis` usa `estaTareaCancelada()`
- `alCompletarAnalisis` usa `debeDescartarResultado()`

#### 4. Refactorización DRY - Callbacks
**Commit:** `8537995`

**Métodos auxiliares creados:**
- `limpiarRecursosTarea()`: Centraliza limpieza de finally blocks
- `debeDescartarResultado()`: Combina verificaciones
- `actualizarEstadisticasEnEdt()`: Centraliza actualización de UI

#### 5. Mejora de Fiabilidad - Sistema de Tareas
**Commit:** `0ebe61e`

**Problemas resueltos:**
- Future.cancel() no interrumpe I/O
- COMPLETADO sobrescribe CANCELADO
- Futures pueden fugarse
- UI se congela con operaciones masivas

**Soluciones:**
- Verificación de estado antes de procesar resultados
- Try-finally en todos los callbacks
- Separación de lock y actualización UI
- Limpieza de recursos garantizada

#### 6. Internacionalización
**Commits:** `2fa89e1`, `d0ed556`, `f7ea0a3`

**Progreso:**
- Reemplazo de strings hardcoded
- Logging simplificado
- Soporte bilingüe completo

---

## Conventions y Standards

### Principios Fundamentales

#### 1. CONFIABILIDAD
- Validación de inputs (Normalizador)
- Manejo robusto de errores
- Limpieza de recursos garantizada (try-finally)
- Verificación de estado antes de operaciones

#### 2. EFICIENCIA
- Rate limiting configurable
- Concurrencia controlada
- Cancelación inmediata (no esperar timeouts)
- Reutilización de objetos (clientes HTTP cacheados)

#### 3. DRY (Don't Repeat Yourself)
- Utilidades centralizadas:
  - `Normalizador.esVacio()` (91 usos)
  - `GestorLoggingUnificado`
  - `ParserRespuestasAI`
- Sin código duplicado
- Sin comentarios huérfanos

### Código de Calidad

#### Validaciones
```java
// SIEMPRE validar antes de usar
if (gestorTareas == null || Normalizador.esVacio(id)) {
    return;
}
```

#### Logging
```java
// Usar gestorLogging (no LOGGER directo)
gestorLogging.info("Origen", "Mensaje");
gestorLogging.error("Origen", "Mensaje", exception);
```

#### Limpieza de Recursos
```java
try {
    // Usar recurso
} finally {
    // SIEMPRE limpiar, sin importar resultado
    recurso.close();
    referencia = null;  // Permitir GC
}
```

### Estándares de Nombres

#### Paquetes
```
com.burpia.{paquete}.{Clase}
```

#### Métodos
- Gestión: `crearX()`, `obtenerX()`, `actualizarX()`, `eliminarX()`
- Booleanos: `esX()`, `tieneX()`, `estaX()`
- Callbacks: `alX()`

#### Constantes
```java
private static final int MAX_INTENTOS = 5;
private static final String ESTADO_COMPLETADO = "Completado";
```

### Comentarios

**✅ Permitidos:**
- Javadoc en métodos públicos
- Explicaciones de algoritmos complejos
- NOTAS DE OPTIMIZACIÓN

**❌ Prohibidos:**
- Comentarios obvios: `// Sumar a + b`
- Comentarios de código deshabilitado
- Comentarios huérfanos sin código

---

## Problemas Conocidos y Limitaciones

### Limitaciones Actuales

1. **Persistencia de Estado UI**
   - Filtros de búsqueda no se guardan
   - Última pestaña no se recuerda
   - Anchos de columnas no se guardan
   - **Prioridad:** MEDIA (mejora UX)

2. **Tests Flaky**
   - Algunos tests fallan intermitentemente
   - Posible condición de carrera en tests UI
   - **Prioridad:** BAJA

### Mejoras Futuras Planeadas

1. **Persistencia de Estado UI** (Fase 1)
   - Guardar filtros de PanelHallazgos
   - Guardar última pestaña seleccionada

2. **Soporte de Streaming AI**
   - Respuestas progresivas
   - Cancelación mid-stream

3. **Más Proveedores**
   - Azure OpenAI
   - HuggingFace
   - Cohere

---

## Debugging y Troubleshooting

### Logs Detallados

Activar modo verbose:
```java
config.establecerDetallado(true);
```

### Logs de Rastreo

Ver rastreo de operaciones:
```bash
# Buscar "[RASTREO]" en consola
```

### Problemas Comunes

#### 1. "Tarea atascada"
**Causa:** Tarea en estado ANALIZANDO por > 5 minutos
**Solución:** Se marca automáticamente como ERROR
**Log:** `"Tarea atascada detectada"`

#### 2. "Cola saturada"
**Causa:** ExecutorService rechaza tarea
**Solución:** Aumentar `maximoConcurrente`
**Log:** `"Cola de análisis saturada"`

#### 3. "LLM no está listo"
**Causa:** Configuración inválida
**Solución:** Configurar API key y modelo
**Log:** Mensaje en UI

---

## Referencias Rápidas

### Key Classes

- **Entry Point:** `ExtensionBurpIA`
- **HTTP Handler:** `ManejadorHttpBurpIA`
- **AI Engine:** `AnalizadorAI`
- **Task Manager:** `GestorTareas`
- **Main UI:** `PestaniaPrincipal`
- **Config:** `GestorConfiguracion`

### Key Methods

- **Procesar solicitud:** `ManejadorHttpBurpIA.procesarSolicitud()`
- **Crear tarea:** `GestorTareas.crearTarea()`
- **Cancelar inmediato:** `AnalizadorAI.cancelarLlamadaHttpActiva()`
- **Guardar config:** `GestorConfiguracion.guardarConfiguracion()`

### Archivos de Configuración

- **Usuario:** `~/.burpia/config.json`
- **Formato:** JSON
- **Validación:** Automática al cargar

---

## Notas para Agentes de IA

### Reglas de Oro

1. **NO crear código muerto**
   - Eliminar código comentado
   - Eliminar métodos no usados

2. **SIEMPRE validar**
   - Usar `Normalizador.esVacio()`
   - Verificar null antes de usar

3. **SEGUIR principios DRY**
   - Reutilizar utilidades existentes
   - No duplicar lógica

4. **Usar gestorLogging**
   - NO usar `java.util.logging.Logger` directamente
   - Usar `GestorLoggingUnificado`

5. **Validar después de cambios**
   - `./build-jar.sh` después de cada implementación
   - Verificar que todos los tests pasan

6. **Leer AGENTS.md primero**
   - Antes de modificar código
   - Antes de agregar características

### Anti-Patterns a Evitar

❌ **NO HACER:**
```java
// Validación manual
if (str == null || str.isEmpty()) { }

// Logging directo
LOGGER.info("Mensaje");

// Código comentado
// TODO: fix later

// Comentarios obvios
// Sumar a y b
int c = a + b;  // Suma
```

✅ **HACER:**
```java
// Validación DRY
if (Normalizador.esVacio(str)) { }

// Logging unificado
gestorLogging.info("Origen", "Mensaje");

// Sin código muerto
// Eliminar completamente

// Sin comentarios obvios
int c = a + b;
```

---

## Métricas del Proyecto

### Líneas de Código
- **Total:** ~15,000+ LOC
- **Java:** ~12,000 LOC
- **Tests:** ~3,000 LOC

### Cobertura de Tests
- **Tests:** 394 passing
- **Cobertura:** ~70-80%
- **Categories:** Unit, Integration, UI, Performance

### Dependencias
- **Runtime:** 3 dependencias principales
- **Test:** JUnit 5, Mockito, etc.
- **Build:** Gradle 8.5

### Performance
- **Startup:** <1 segundo
- **Análisis:** 2-10 segundos (según modelo)
- **Memoria:** ~100-200MB

---

**Última actualización:** 3 de marzo, 2026  
**Versión del documento:** 1.0  
**Mantenedor:** @DragonJAR

---

## Apéndice

### A. Arquitectura de Callbacks

```java
public interface Callback {
    void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);
    void alErrorAnalisis(String error);
    default void alCanceladoAnalisis() {}
}
```

**Implementación:** `ManejadorHttpBurpIA.ManejadorResultadoAI`

### B. Estados de Tarea - Máquina de Estados

```
EN_COLA → ANALIZANDO → COMPLETADO
   ↓                      ↓
PAUSADO              ERROR
   ↓                      ↓
EN_COLA ←----------- CANCELADO
```

### C. Configuración de Timeout

```
Timeout global: config.obtenerTiempoEsperaAI()
Timeout por modelo: config.obtenerTiempoEsperaParaModelo(proveedor, modelo)
Priority: Modelo > Global
Default: 120 segundos
```

### D. Rate Limiting

```
maximoConcurrente = N
Semaphore permisos(N)

adquirir():
  while (permisos.available == 0) {
    wait()
  }
  permisos.acquire()

liberar():
  permisos.release()
  notify()
```

---

**FIN DE AGENTS.md**
