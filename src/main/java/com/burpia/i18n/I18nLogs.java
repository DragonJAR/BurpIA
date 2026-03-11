package com.burpia.i18n;

import com.burpia.util.Normalizador;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad de internacionalización para mensajes de logging.
 * <p>
 * Esta clase proporciona traducción automática de mensajes de log de español a inglés
 * basándose en el idioma configurado en {@link I18nUI}.
 * </p>
 * <p>
 * Utiliza un sistema de reemplazos con patrones regex para palabras simples
 * y reemplazo directo para frases complejas.
 * </p>
 *
 * @see I18nUI
 * @see IdiomaUI
 */
public final class I18nLogs {
    private static final String[][] REEMPLAZOS_INGLES = new String[][]{
        {"Inicialización", "Initialization"},
        {"inicialización", "initialization"},
        {"Complemento de Seguridad con IA", "AI Security Plugin"},
        {"Version Burp Suite", "Burp Suite Version"},
        {"Configuracion guardada exitosamente", "Configuration saved successfully"},
        {"Configuracion actualizada", "Configuration updated"},
        {"Configuracion cargada", "Configuration loaded"},
        {"Configuracion cargada:", "Loaded configuration:"},
        {"Ruta de configuracion", "Configuration path"},
        {"Archivo no existe, creando configuracion por defecto", "File does not exist, creating default configuration"},
        {"Archivo no es legible", "File is not readable"},
        {"Archivo vacio, usando configuracion por defecto", "Empty file, using default configuration"},
        {"Error al parsear JSON, usando configuracion por defecto", "Error parsing JSON, using default configuration"},
        {"Error de sintaxis JSON", "JSON syntax error"},
        {"Error de E/S al cargar", "I/O error while loading"},
        {"Error inesperado al cargar", "Unexpected error while loading"},
        {"Directorio padre no existe", "Parent directory does not exist"},
        {"Directorio no es escribible", "Parent directory is not writable"},
        {"Configuracion guardada exitosamente en", "Configuration saved successfully to"},
        {"No se pudieron ajustar permisos privados del archivo", "Could not enforce private file permissions"},
        {"Error de E/S al guardar", "I/O error while saving"},
        {"Error inesperado al guardar", "Unexpected error while saving"},
        {"Error al restaurar estado UI de pestaña principal", "Error restoring main tab UI state"},
        {"Error al persistir estado UI de pestaña principal", "Error persisting main tab UI state"},
        {"Archivo eliminado", "Deleted file"},
        {"Error al eliminar", "Error deleting"},
        {"URL de API", "API URL"},
        {"Modelo", "Model"},
        {"Abriendo dialogo de configuracion", "Opening settings dialog"},
        {"Descargando extensión BurpIA", "Unloading BurpIA extension"},
        {"Extensión BurpIA descargada correctamente", "BurpIA extension unloaded successfully"},
        {"Estado de captura actualizado", "Capture state updated"},
        {"Manejador HTTP registrado exitosamente", "HTTP handler registered successfully"},
        {"Menu contextual de BurpIA registrado exitosamente", "BurpIA context menu registered successfully"},
        {"Pestania de UI registrada exitosamente", "UI tab registered successfully"},
        {"AnalizadorAI iniciado para URL", "AnalizadorAI started for URL"},
        {"Analizando", "Analyzing"},
        {"tomo", "took"},
        {"Analisis completado", "Analysis completed"},
        {"Analisis cancelado", "Analysis canceled"},
        {"Analisis fallido", "Analysis failed"},
        {"Analisis interrumpido", "Analysis interrupted"},
        {"Analisis forzado solicitado", "Forced analysis requested"},
        {"Analisis detenido por cancelacion de usuario", "Analysis stopped by user cancellation"},
        {"Iniciando analisis", "Starting analysis"},
        {"Hilo de analisis iniciado para", "Analysis thread started for"},
        {"Resultado descartado porque la tarea fue cancelada", "Result discarded because task was canceled"},
        {"Error de analisis", "Analysis error"},
        {"severidad maxima", "max severity"},
        {"hallazgos", "findings"},
        {"hallazgo", "finding"},
        {"Solicitud a enviar", "Request to send"},
        {"Respuesta recibida es null", "Received response is null"},
        {"Solicitud iniciadora es null", "Initiating request is null"},
        {"solicitud", "request"},
        {"Solicitud", "Request"},
        {"respuesta", "response"},
        {"Respuesta", "Response"},
        {"DENTRO DE SCOPE - Procesando", "IN SCOPE - Processing"},
        {"FUERA DE SCOPE - Omitiendo", "OUT OF SCOPE - Skipping"},
        {"NOTA: Solo se analizaran solicitudes DENTRO del SCOPE de Burp Suite",
            "NOTE: Only requests INSIDE Burp Suite SCOPE will be analyzed"},
        {"Iniciando analisis para", "Starting analysis for"},
        {"Omitiendo recurso estatico", "Skipping static resource"},
        {"Recurso coincidio con filtro estatico", "Resource matched static filter"},
        {"Solicitud duplicada omitida", "Skipped duplicate request"},
        {"Nueva solicitud registrada", "New request recorded"},
        {"Hash de solicitud", "Request hash"},
        {"Detalles de solicitud", "Request details"},
        {"Metodo", "Method"},
        {"No se pudo capturar la respuesta", "Could not capture response"},
        {"No se pudo analizar solicitud forzada", "Could not analyze forced request"},
        {"No se pudo capturar la respuesta para analisis manual",
            "Could not capture response for manual analysis"},
        {"Analisis manual sin solicitud/respuesta original: se registraran hallazgos, pero no Issue.",
            "Manual analysis without original request/response: findings will be recorded, but no Issue."},
        {"Analisis manual sin response asociada: se registraran hallazgos, pero no Issue.",
            "Manual analysis without associated response: findings will be recorded, but no Issue."},
        {"No se pudo reutilizar la evidencia original del analisis manual",
            "Could not reuse original evidence from manual analysis"},
        {"No se pudo construir evidencia desde analisis manual",
            "Could not build evidence from manual analysis"},
        {"No se pudo encolar AuditIssue por saturacion de cola", "Could not enqueue AuditIssue due to queue saturation"},
        {"No se pudo guardar AuditIssue: SiteMap API no disponible", "Could not save AuditIssue: SiteMap API unavailable"},
        {"Error al crear AuditIssue en Burp Suite", "Error creating AuditIssue in Burp Suite"},
        {"AuditIssue creado en Burp Suite para", "AuditIssue created in Burp Suite for"},
        {"AuditIssue no creado: hallazgo sin datos suficientes", "AuditIssue not created: finding lacks required data"},
        {"Hallazgo sin evidencia HTTP: no se puede crear AuditIssue",
            "Finding without HTTP evidence: AuditIssue cannot be created"},
        {"Hallazgo omitido en Issues", "Finding skipped in Issues"},
        {"Integracion con Issues deshabilitada: solo disponible en Burp Professional",
            "Issues integration disabled: available only in Burp Professional"},
        {"Autoguardado deshabilitado", "Autosave disabled"},
        {"Solicitud null, no se puede verificar scope", "Request is null, scope cannot be verified"},
        {"URL null o vacia, no se puede verificar scope", "URL is null or empty, scope cannot be verified"},
        {"URL fuera de scope", "URL out of scope"},
        {"URL dentro de scope", "URL in scope"},
        {"API de scope no disponible, omitiendo solicitud por seguridad",
            "Scope API unavailable, skipping request for safety"},
        {"Construyendo prompt para URL", "Building prompt for URL"},
        {"Longitud de prompt", "Prompt length"},
        {"Llamando a API", "Calling API"},
        {"con modelo", "with model"},
        {"Codigo de respuesta de API", "API response code"},
        {"Longitud de respuesta de API", "API response length"},
        {"Cuerpo de respuesta de error de API", "API error response body"},
        {"Solicitud HTTP fallida", "HTTP request failed"},
        {"Multi-consulta iniciada con", "Multi-query started with"},
        {"proveedores", "providers"},
        {"Analizando con proveedor", "Analyzing with provider"},
        {"modelo", "model"},
        {"Hallazgos de proveedor", "Findings from provider"},
        {"agregados al resultado", "added to result"},
        {"Multi-consulta completada", "Multi-query completed"},
        {"Total de hallazgos combinados", "Total combined findings"},
        {"Stack trace de la excepcion", "Exception stack trace"},
        {"Tiempo de espera agotado, intenta aumentarlo en los ajustes", "Timeout reached, try increasing it in the settings"},
        {"Sistema de retry", "Retry system"},
        {"Intento inmediato", "Immediate attempt"},
        {"Intento #", "Attempt #"},
        {"Reintento #", "Retry #"},
        {"falló", "failed"},
        {"fallo", "failed"},
        {"Todos los intentos inmediatos fallaron", "All immediate attempts failed"},
        {"Esperando", "Waiting"},
        {"Reintento", "Retry"},
        {"después de esperar", "after waiting"},
        {"despues de esperar", "after waiting"},
        {"Todos los reintentos fallaron", "All retries failed"},
        {"SUGERENCIA: Considera cambiar de proveedor de API.", "SUGGESTION: Consider switching API provider."},
        {"Ultimo error", "Last error"},
        {"Error inesperado", "Unexpected error"},
        {"Error al parsear respuesta de API", "Error parsing API response"},
        {"Parseando respuesta JSON", "Parsing JSON response"},
        {"JSON reparado exitosamente", "JSON repaired successfully"},
        {"Eliminados bloques de código markdown JSON", "Removed JSON markdown code blocks"},
        {"Eliminados bloques de código markdown genéricos", "Removed generic markdown code blocks"},
        {"No se pudo parsear como JSON de hallazgos", "Could not parse findings as JSON"},
        {"Intentando parsing de texto plano como fallback", "Attempting plain-text fallback parsing"},
        {"Total de hallazgos parseados", "Total parsed findings"},
        {"Error al parsear texto plano", "Error parsing plain text"},
        {"Contenido extraido - Longitud", "Extracted content - Length"},
        {"Se encontraron", "Found"},
        {"en JSON", "in JSON"},
        {"JSON no contiene campo 'hallazgos', intentando parsing de texto plano",
            "JSON does not contain 'hallazgos', trying plain-text parsing"},
        {"Hallazgo agregado", "Finding added"},
        {"Severidad extraida", "Extracted severity"},
        {"Deteniendo ExecutorService de ManejadorHttpBurpIA", "Stopping ManejadorHttpBurpIA ExecutorService"},
        {"ExecutorService no termino en 5 segundos, forzando shutdown", "ExecutorService did not finish in 5 seconds, forcing shutdown"},
        {"Error al esperar terminacion de ExecutorService", "Error waiting for ExecutorService termination"},
        {"ExecutorService cerrado", "ExecutorService closed"},
        {"Cola de analisis saturada, solicitud descartada", "Analysis queue saturated, request discarded"},
        {"Maximo de reintentos de inyeccion alcanzado", "Maximum injection retries reached"},
        {"Payload en bufer usando escritura directa", "Payload buffered using direct write"},
        {"Se ha despachado la secuencia VK_ENTER", "VK_ENTER sequence dispatched"},
        {"Payload inicial encolado para inyeccion manual por el usuario", "Initial payload queued for manual injection by user"},
        {"Error escritura raw PTY", "PTY raw write error"},
        {"Error escritura directa PTY", "PTY direct write error"},
        {"Error al cambiar de agente", "Error while switching agent"},
        {"Esperando el delay establecido por el usuario", "Waiting for user-configured delay"},
        {"antes de la inyeccion", "before injection"},
        {"No se pudo encolar tarea en inyector PTY", "Could not enqueue task in PTY injector"},
        {"No se pudo enviar comando de arranque del agente tras reintentos", "Could not send agent startup command after retries"},
        {"Error escribiendo comando crudo PTY", "Error writing raw PTY command"},
        {"Error escribiendo por ttyConnector", "Error writing through ttyConnector"},
        {"Escritura PTY omitida: proceso no disponible", "PTY write skipped: process unavailable"},
        {"Escritura PTY omitida: stream de salida nulo", "PTY write skipped: null output stream"},
        {"Error deteniendo terminalWidget", "Error stopping terminalWidget"},
        {"Error cerrando terminalWidget", "Error closing terminalWidget"},
        {"Error cerrando ttyConnector", "Error closing ttyConnector"},
        {"Error cerrando proceso PTY", "Error closing PTY process"},
        {"Error esperando cierre de proceso PTY", "Error waiting for PTY process shutdown"},
        {"Iniciando secuencia de inyeccion automatica de agente...", "Starting automatic agent injection sequence..."},
        {"Error desconocido PTY", "Unknown PTY error"},
        {"Error de E/S iniciando proceso PTY", "I/O error starting PTY process"},
        {"Error de seguridad iniciando proceso PTY", "Security error starting PTY process"},
        {"Operación interrumpida al iniciar PTY", "Operation interrupted while starting PTY"},
        {"Error inesperado iniciando Consola PTY", "Unexpected error starting PTY console"},
        {"Shell no encontrado", "Shell not found"},
        {"usando fallback a", "using fallback to"},
        {"Captura pausada por usuario", "Capture paused by user"},
        {"Captura reanudada por usuario", "Capture resumed by user"},
        {"ACTIVA", "ACTIVE"},
        {"PAUSADA", "PAUSED"},
        {"Modo Detallado", "Verbose Mode"},
        {"Maximo Concurrente", "Max Concurrent"},
        {"Maximo Hallazgos en Tabla", "Max Findings in Table"},
        {"Retraso", "Delay"},
        {"Tarea creada", "Task created"},
        {"Tareas canceladas", "Tasks canceled"},
        {"Tareas pausadas", "Tasks paused"},
        {"Tareas reanudadas", "Tasks resumed"},
        {"Tareas limpiadas", "Tasks cleaned"},
        {"Tarea atascada detectada", "Stuck task detected"},
        {"Tarea pausada", "Task paused"},
        {"Tarea reanudada", "Task resumed"},
        {"Tarea cancelada", "Task canceled"},
        {"Tarea limpiada", "Task cleaned"},
        {"Error en manejador de cancelacion", "Error in cancellation handler"},
        {"Retencion aplicada en tareas finalizadas", "Retention applied on completed tasks"},
        {"No existe contexto para reintentar tarea", "No context found to retry task"},
        {"No se pudo reencolar tarea", "Could not re-queue task"},
        {"Tarea reencolada", "Task re-queued"},
        {"No se pudo programar analisis", "Could not schedule analysis"},
        {"No se pudo actualizar configuracion: objeto de configuracion nulo",
            "Could not update configuration: null configuration object"},
        {"Cancelacion activa aplicada para tarea", "Active cancellation applied for task"},
        {"No se pudo marcar tarea como analizando (estado no valido)",
            "Could not mark task as analyzing (invalid state)"},
        {"Estado cola analisis", "Analysis queue status"},
        {"activos", "active"},
        {"enCola", "queued"},
        {"completadas", "completed"},
        {"[RASTREO]", "[TRACE]"},
        {"ALERTA: Configuracion de IA no disponible", "ALERT: AI configuration is unavailable"},
        {"ALERTA: Proveedor de AI no configurado o no valido", "ALERT: AI provider is not configured or is invalid"},
        {"ALERTA: URL de API no configurada", "ALERT: API URL is not configured"},
        {"ALERTA: Modelo no configurado para", "ALERT: Model is not configured for"},
        {"ALERTA: Clave de API requerida para", "ALERT: API key is required for"},
        {"Configuracion", "Configuration"},
        {"configuracion", "configuration"},
        {"Complemento de Seguridad con IA", "AI Security Extension"},
        {"[Configuracion]", "[Configuration]"},
        {"[Environment]", "[Environment]"},
        {"[Multi-Provider Configuration]", "[Multi-Provider Configuration]"},
        {"[Performance]", "[Performance]"},
        {"[Agent]", "[Agent]"},
        {"Proveedor Principal: ", "Main Provider: "},
        {"Multi-proveedor: ", "Multi-provider: "},
        {"Concurrencia: ", "Concurrency: "},
        {"Modo detallado: ", "Verbose mode: "},
        {"tareas", "tasks"},
        {"retraso", "delay"},
        {"max hallazgos: ", "max findings: "},
        {"Agente: ", "Agent: "},
        {"Sí", "Yes"},
        {"No", "No"},
        {"Burp Suite: ", "Burp Suite: "},
        {"Java: ", "Java: "},
        {"SO: ", "OS: "},
        {"URL de API: ", "API URL: "},
        {"Clave de API: ", "API Key: "},
        {"Timeout: global ", "Timeout: global "},
        {", timeout ", ", timeout "},
        {"s, modelo ", "s, model "},
        {" (efectivo: ", " (effective: "},
        {"Verificación SSL: ", "SSL Verification: "},
        {"Activado", "ON"},
        {"Desactivado", "OFF"},
        {"Modo solo proxy: ", "Proxy-only mode: "},
        {"Idioma: ", "Language: "},
        {"Tema: ", "Theme: "},
        {"Habilitado: ", "Enabled: "},
        {"Proveedores: ", "Providers: "},
        {"Orden de ejecución: ", "Execution Order: "},
        {"Max concurrente: ", "Max concurrent: "},
        {"Max tareas: ", "Max tasks: "},
        {"Retención: tareas completas ≤", "Retention: Completed tasks ≤"},
        {"Tipo: ", "Type: "},
        {"Binario: ", "Binary: "},
        {"Consola GUI inicializada", "Console GUI initialized"},
        {"Error interno de consola", "Internal console error"},
        {"UI registrada en ", "UI registered in "},
        {"ProviderConfigManager inicializado", "ProviderConfigManager initialized"},
        {"Componentes básicos de UI configurados", "Basic UI components configured"},
        {"Componentes UI inicializados", "UI components initialized"},
        {"Componentes UI no inicializados", "UI components not initialized"},
        {"Configuración inicial cargada", "Initial configuration loaded"},
        {"Proveedor cambiado a", "Provider changed to"},
        {"No se ha seleccionado un modelo", "No model selected"},
        {"Formato de timeout inválido", "Invalid timeout format"},
        {"No se encontró configuración para proveedor", "No configuration found for provider"},
        {"Límite máximo de proveedores alcanzado", "Maximum provider limit reached"},
        {"Proveedor agregado a lista", "Provider added to list"},
        {"No se puede quitar el proveedor principal", "Cannot remove main provider"},
        {"Proveedor quitado de lista", "Provider removed from list"},
        {"No se puede subir el proveedor principal", "Cannot move up main provider"},
        {"Proveedor subido en lista", "Provider moved up in list"},
        {"No se puede bajar el proveedor principal", "Cannot move down main provider"},
        {"Proveedor bajado en lista", "Provider moved down in list"},
        {"movido a la primera posición en lista multi-proveedor", "moved to first position in multi-provider list"},
        {"agregado a la primera posición en lista multi-proveedor", "added to first position in multi-provider list"},
        {"Estado inicial capturado para proveedor", "Initial state captured for provider"},
        {"Estado actual inválido", "Invalid current state"},
        {"UIProvider es nulo", "UIProvider is null"},
        {"Proveedor nuevo es vacío", "New provider is empty"},
        {"Cambiando proveedor", "Changing provider"},
        {"Estado temporal guardado para proveedor", "Temporary state saved for provider"},
        {"Estado temporal eliminado para proveedor", "Temporary state removed for provider"},
        {"Ruta binaria guardada para agente", "Binary path saved for agent"},
        {"Ruta binaria eliminada para agente", "Binary path removed for agent"},
        {"Estado temporal limpiado", "Temporary state cleared"},
        {"ConfigDialogController inicializado", "ConfigDialogController initialized"},
        {"Inicializando manejadores de eventos", "Initializing event handlers"},
        {"Manejadores de eventos inicializados", "Event handlers initialized"},
        {"Cargando configuración inicial", "Loading initial configuration"},
        {"Manejando cambio de proveedor", "Handling provider change"},
        {"Manejando cambio de modelo", "Handling model change"},
        {"Proveedor no soportado para listar modelos", "Unsupported provider for model listing"},
        {"Error al parsear respuesta de modelos", "Error parsing models response"},
        {"SSL inseguro no se pudo configurar", "Insecure SSL could not be configured"},
        {"Error al verificar scope", "Error checking scope"},
        {"No se pudo extraer Content-Type de respuesta", "Could not extract response Content-Type"},
        {"No se pudo construir HttpRequestResponse para evidencia de Issue",
            "Could not build HttpRequestResponse for Issue evidence"},
        {"TaskExecutionManager inicializado con", "TaskExecutionManager initialized with"},
        {"Llamada HTTP cancelada para tarea", "HTTP call canceled for task"},
        {"Hilo de análisis iniciado para", "Analysis thread started for"},
        {"No se pudo persistir evidencia HTTP", "Could not persist HTTP evidence"},
        {"No se pudo eliminar evidencia HTTP", "Could not remove HTTP evidence"},
        {"Deteniendo ExecutorService de TaskExecutionManager", "Stopping TaskExecutionManager ExecutorService"},
        {"No se pudo actualizar configuración: objeto nulo",
            "Could not update configuration: null object"},
        {"Configuración actualizada: maxConcurrente", "Configuration updated: maxConcurrent"},
        {"Estado de filtros guardado", "Filters state saved"},
        {"Estado de filtros restaurado", "Filters state restored"},
        {"Última pestaña guardada", "Last tab saved"},
        {"Pestaña restaurada", "Tab restored"},
        {"Error al guardar última pestaña seleccionada", "Error saving last selected tab"},
        {"Error al restaurar última pestaña seleccionada", "Error restoring last selected tab"},
        {"Anchos de columna guardados para", "Saved column widths for"},
        {"Anchos de columna restaurados para", "Restored column widths for"},
        {"Error al guardar anchos de columna para", "Error saving column widths for"},
        {"Error al restaurar anchos de columna para", "Error restoring column widths for"},
        {"Ancho inválido para columna", "Invalid width for column"},
        {"Error crítico al parsear respuesta de API para", "Critical error parsing API response for"},
        {"Error parseando texto plano", "Error parsing plain text"},
        {"DIAGNOSTICO", "DIAGNOSTIC"},
        {"Multi-consulta: No hay proveedores seleccionados, usando proveedor único",
            "Multi-query: No providers selected, using single provider"},
        {"Multi-consulta: Solo 1 proveedor seleccionado, usando proveedor único",
            "Multi-query: Only 1 provider selected, using single provider"},
        {"PROVEEDOR: Proveedor no existe", "PROVIDER: Provider does not exist"},
        {"no tiene modelo configurado, omitiendo", "has no configured model, skipping"},
        {"PROVEEDOR: Esperando", "PROVIDER: Waiting"},
        {"hallazgo(s) encontrado(s)", "finding(s) found"},
        {"PROVEEDOR: Error con", "PROVIDER: Error with"},
        {"PROVEEDOR: Multi-consulta completada. Total de hallazgos combinados",
            "PROVIDER: Multi-query completed. Total combined findings"},
        {"Orquestador iniciado para URL", "Orchestrator started for URL"},
        {"Timeout configurado para el cliente HTTP", "Configured timeout for HTTP client"},
        {"Adquiriendo permiso del limitador", "Acquiring limiter permit"},
        {"Permiso de limitador adquirido", "Limiter permit acquired"},
        {"Durmiendo por", "Sleeping for"},
        {"segundos antes de llamar a la API", "seconds before calling the API"},
        {"Analisis pausado y liberando hilo", "Analysis paused and releasing thread"},
        {"Permiso de limitador liberado", "Limiter permit released"},
        {"Prompt (preview)", "Prompt (preview)"},
        {"Respuesta de API (preview)", "API response (preview)"},
        {"No se pudo notificar inicio de análisis", "Could not notify analysis start"},
        {"No se pudo notificar inicio de analisis", "Could not notify analysis start"},
        {"Análisis completado", "Analysis completed"},
        {"Análisis fallido", "Analysis failed"},
        {"Análisis cancelado", "Analysis canceled"},
        {"Fallo de retry sin detalle de excepción", "Retry failed without exception details"},
        {"Respuesta de API sin cuerpo (null)", "API response without body (null)"},
        {"ConfigPersistenceManager", "ConfigPersistenceManager"},
        {"Cargando configuración desde GestorConfiguracion", "Loading configuration from GestorConfiguracion"},
        {"La configuración cargada es nula, usando configuración por defecto", "Loaded configuration is null, using default configuration"},
        {"Configuración cargada exitosamente", "Configuration loaded successfully"},
        {"No se puede guardar configuración nula", "Cannot save null configuration"},
        {"Validando configuración antes de guardar", "Validating configuration before saving"},
        {"Errores de validación encontrados", "Validation errors found"},
        {"Guardando configuración en GestorConfiguracion", "Saving configuration to GestorConfiguracion"},
        {"Configuración guardada exitosamente", "Configuration saved successfully"},
        {"Error al guardar configuración", "Error saving configuration"},
        {"Configuración nula no puede ser validada", "Null configuration cannot be validated"},
        {"Se encontraron", "Found"},
        {"errores de validación", "validation errors"},
        {"Configuración validada exitosamente", "Configuration validated successfully"},
        {"Estado de referencia actualizado", "Reference state updated"},
        {"NOTA: Solo analiza tráfico en Scope. Agrega objetivos en Target > Scope",
            "NOTE: Only analyzes traffic in Scope. Add targets in Target > Scope"},
        {"Manejador HTTP inicializado (max concurrente=", "HTTP handler initialized (max concurrent="},
        {"Agente Factory Droid deshabilitado en ajustes.", "Factory Droid agent disabled in settings."},
        {"Inicialización completada exitosamente", "Initialization completed successfully"}
    };

    /**
     * Mensajes de logging relacionados con el agente Factory Droid.
     */
    public static final class Agente {
        private Agente() {
        }

        public static String ERROR_DESHABILITADO() {
            return tr("Agente Factory Droid deshabilitado en ajustes.");
        }
    }

    /**
     * Mensajes de logging para la fase de inicialización de la extensión.
     */
    public static final class Inicializacion {
        private Inicializacion() {
        }

        public static String SEPARADOR() {
            return "==================================================";
        }

        public static String SECCION_CONFIGURACION() {
            return tr("[Configuration]");
        }

        public static String SECCION_ENTORNO() {
            return tr("[Environment]");
        }

        public static String SECCION_MULTI_PROVEEDOR() {
            return tr("[Multi-Provider Configuration]");
        }

        public static String SECCION_RENDIMIENTO() {
            return tr("[Performance]");
        }

        public static String SECCION_AGENTE() {
            return tr("[Agent]");
        }

        public static String PROVEEDOR_PRINCIPAL() {
            return tr("Proveedor Principal: ");
        }

        public static String MULTI_PROVEEDOR() {
            return tr("Multi-proveedor: ");
        }

        public static String CONCURRENCIA() {
            return tr("Concurrencia: ");
        }

        public static String MODO_DETALLADO() {
            return tr("Modo detallado: ");
        }

        public static String AGENTE() {
            return tr("Agente: ");
        }

        public static String TAREAS(String cantidad) {
            return tr(cantidad) + " " + tr("tareas");
        }

        public static String RETRASO_SEGUNDOS(String segundos) {
            return tr("retraso") + " " + segundos + "s";
        }

        public static String MAX_HALLAZGOS(String cantidad) {
            return tr("max hallazgos: ") + cantidad;
        }

        public static String TIMEOUT_SEGUNDOS(String segundos) {
            return tr("timeout") + " " + segundos + "s";
        }

        public static String SI() {
            return tr("Sí");
        }

        public static String NO() {
            return tr("No");
        }

        public static String ENTORNO_BURP_SUITE(String tipo, String version) {
            return tr("Burp Suite: ") + tipo + " " + version;
        }

        public static String ENTORNO_JAVA(String version, String vendor) {
            return tr("Java: ") + version + " (" + vendor + ")";
        }

        public static String ENTORNO_OS(String nombre, String version, String arch) {
            return tr("SO: ") + nombre + " " + version + " (" + arch + ")";
        }

        public static String URL_API(String url) {
            return tr("URL de API: ") + url;
        }

        public static String API_KEY(String keySanitizada) {
            return tr("Clave de API: ") + keySanitizada;
        }

        public static String TIMEOUT_GLOBAL(String globalSegundos, String modeloSegundos, String efectivoSegundos) {
            return tr("Timeout: global ") + globalSegundos + "s, modelo " + modeloSegundos + "s (efectivo: " + efectivoSegundos + "s)";
        }

        public static String SSL_VERIFICACION(boolean activado) {
            return tr("Verificación SSL: ") + (activado ? tr("Activado") : tr("Desactivado"));
        }

        public static String MODO_SOLO_PROXY(boolean activado) {
            return tr("Modo solo proxy: ") + (activado ? tr("Activado") : tr("Desactivado"));
        }

        public static String IDIOMA(String idioma, String codigo) {
            return tr("Idioma: ") + idioma + " (" + codigo + ")";
        }

        public static String TEMA(String tema) {
            return tr("Tema: ") + tema;
        }

        public static String MULTI_HABILITADO(boolean habilitado) {
            return tr("Habilitado: ") + (habilitado ? SI() : NO());
        }

        public static String AGENTE_HABILITADO(boolean habilitado) {
            return tr("Habilitado: ") + (habilitado ? SI() : NO());
        }

        public static String PROVEEDORES() {
            return tr("Proveedores: ");
        }

        public static String ORDEN_EJECUCION() {
            return tr("Orden de ejecución: ");
        }

        public static String CONCURRENCIA_MAX(String max) {
            return tr("Max concurrente: ") + max;
        }

        public static String MAX_TAREAS(String max) {
            return tr("Max tareas: ") + max;
        }

        public static String RETENCION(String max) {
            return tr("Retención: tareas completas ≤") + max;
        }

        public static String AGENTE_TIPO(String tipo) {
            return tr("Tipo: ") + tipo;
        }

        public static String AGENTE_BINARIO(String ruta) {
            return tr("Binario: ") + ruta;
        }

        public static String UI_REGISTRADA_EN(String tipo, String version) {
            return tr("UI registrada en ") + tipo + " " + version;
        }

        public static String INICIALIZACION_COMPLETA() {
            return tr("Inicialización completada exitosamente");
        }
    }

    /**
     * Mensajes de logging relacionados con errores de contexto excedido.
     */
    public static final class ContextoExcedido {
        private ContextoExcedido() {
        }

        public static String DETECTADO() {
            return tr("Contexto excedido detectado");
        }

        public static String TRUNCANDO(int intento) {
            return String.format(tr("Truncando prompt (intento %d)"), intento);
        }

        public static String TRUNCADO(int original, int nuevo) {
            return String.format(tr("Prompt truncado de %d a %d caracteres"), original, nuevo);
        }

        public static String MAX_INTENTOS(int max) {
            return String.format(tr("Prompt excede límite después de %d truncados"), max);
        }

        public static String LIMITE_EXTRAIDO(int tokens) {
            return String.format(tr("Límite de tokens extraído del error: %d"), tokens);
        }

        public static String RETRY_CON_TRUNCADO() {
            return tr("Reintentando con prompt truncado");
        }

        public static String NO_RETRYABLE() {
            return tr("Error de contexto no recuperable después de truncados");
        }
    }

    /**
     * Mensajes de logging relacionados con la gestión de tareas.
     */
    public static final class Tareas {
        private Tareas() {
        }

        public static String ESTADO_ATASCADA() {
            return tr("Tarea atascada - timeout");
        }

        public static String LOG_ATASCADA_DETECTADA() {
            return tr("Tarea atascada detectada: ");
        }

        public static String LOG_ERROR_MANEJADOR(String tipoOperacion) {
            return tr("Error en manejador de " + tipoOperacion + ": ");
        }
    }

    /**
     * Mensajes de logging relacionados con la persistencia y estado de configuración.
     */
    public static final class Configuracion {
        private Configuracion() {
        }

        public static String CARGANDO_DESDE_GESTOR() {
            return tr("Cargando configuración desde GestorConfiguracion");
        }

        public static String CONFIGURACION_NULA() {
            return tr("La configuración cargada es nula, usando configuración por defecto");
        }

        public static String CARGADA_OK() {
            return tr("Configuración cargada exitosamente");
        }

        public static String NO_GUARDAR_NULA() {
            return tr("No se puede guardar configuración nula");
        }

        public static String VALIDANDO_ANTES_GUARDAR() {
            return tr("Validando configuración antes de guardar");
        }

        public static String ERRORES_VALIDACION(int cantidad) {
            return tr("Errores de validación encontrados: " + cantidad);
        }

        public static String GUARDANDO_EN_GESTOR() {
            return tr("Guardando configuración en GestorConfiguracion");
        }

        public static String GUARDADA_OK() {
            return tr("Configuración guardada exitosamente");
        }

        public static String ERROR_GUARDAR() {
            return tr("Error al guardar configuración");
        }

        public static String NULA_NO_VALIDA() {
            return tr("Configuración nula no puede ser validada");
        }

        public static String VALIDACION_FALLIDA(int cantidad) {
            return tr("Se encontraron " + cantidad + " errores de validación");
        }

        public static String VALIDADA_OK() {
            return tr("Configuración validada exitosamente");
        }

        public static String ESTADO_REFERENCIA_ACTUALIZADO() {
            return tr("Estado de referencia actualizado");
        }
    }

    /**
     * Mensajes de logging relacionados con la gestión de evidencias.
     */
    public static final class Evidence {
        private Evidence() {
        }

        public static String EVIDENCIA_NULA() {
            return tr("Intentando almacenar evidencia nula");
        }

        public static String EVIDENCIA_ALMACENADA() {
            return tr("Evidencia almacenada: ");
        }

        public static String ERROR_ALMACENAR() {
            return tr("Error al almacenar evidencia");
        }

        public static String ERROR_OBTENER() {
            return tr("Error al obtener evidencia: ");
        }

        public static String EVIDENCIA_ELIMINADA() {
            return tr("Evidencia eliminada: ");
        }

        public static String ERROR_ELIMINAR() {
            return tr("Error al eliminar evidencia: ");
        }

        public static String HALLAZGO_NULO_ISSUE() {
            return tr("Intentando guardar hallazgo nulo como Issue");
        }

        public static String ISSUES_SOLO_PRO() {
            return tr("Integración con Issues solo disponible en Burp Professional");
        }

        public static String HALLAZGO_SIN_EVIDENCIA() {
            return tr("Hallazgo sin evidencia HTTP: no se puede crear AuditIssue");
        }

        public static String AUDIT_ISSUE_CREADO() {
            return tr("AuditIssue creado para hallazgo: ");
        }

        public static String AUDIT_ISSUE_NO_CREADO() {
            return tr("AuditIssue no creado: hallazgo sin datos suficientes");
        }

        public static String ERROR_GUARDAR_ISSUE() {
            return tr("Error al guardar hallazgo como Issue");
        }

        public static String AUDIT_ISSUES_CREADOS(int creados, int total) {
            return tr("Se crearon " + creados + " AuditIssues de " + total + " hallazgos");
        }

        public static String CACHE_LIMPIADO() {
            return tr("Cache de memoria de evidencias limpiado");
        }

        public static String ERROR_LIMPIAR() {
            return tr("Error al limpiar evidencias antiguas");
        }
    }

    /**
     * Mensajes de logging relacionados con el almacenamiento de evidencias en disco.
     */
    public static final class AlmacenEvidencia {
        private AlmacenEvidencia() {
        }

        public static String ERROR_GUARDAR_LIMPIAR() {
            return tr("Error al guardar evidencia, se limpia archivo: ");
        }

        public static String ERROR_RECONSTRUIR() {
            return tr("Error al reconstruir evidencia: ");
        }

        public static String ERROR_CREAR_DIRECTORIO() {
            return tr("Error al crear directorio de evidencia: ");
        }

        public static String TAMANIO_REQUEST_INVALIDO() {
            return tr("Tamaño de request inválido en archivo: ");
        }

        public static String TAMANIO_RESPONSE_INVALIDO() {
            return tr("Tamaño de response inválido en archivo: ");
        }

        public static String ERROR_RECONSTRUIR_BYTES() {
            return tr("Error al reconstruir request/response desde bytes");
        }

        public static String ERROR_EXTRAER_REQUEST() {
            return tr("Error al extraer bytes de request: ");
        }

        public static String ERROR_EXTRAER_RESPONSE() {
            return tr("Error al extraer bytes de response: ");
        }

        public static String ERROR_DIRECTORIO_DEPURACION() {
            return tr("Error al crear directorio para depuración: ");
        }

        public static String ERROR_MODIFICACION_ARCHIVO() {
            return tr("Error al obtener modificación de archivo: ");
        }

        public static String ERROR_LISTAR_ARCHIVOS() {
            return tr("Error al listar archivos de evidencia: ");
        }

        public static String ERROR_ULTIMA_MODIFICACION() {
            return tr("Error al obtener última modificación: ");
        }

        public static String ERROR_ELIMINAR_ARCHIVO() {
            return tr("Error al eliminar archivo: ");
        }
    }

    /**
     * Mensajes de logging relacionados con la tabla de hallazgos.
     */
    public static final class Hallazgos {
        private Hallazgos() {
        }

        public static String ERROR_ESCUCHA_CAMBIOS() {
            return tr("Error en escucha de cambios: ");
        }
    }

    /**
     * Representa una entrada de reemplazo para la traducción de mensajes.
     * <p>
     * Contiene el texto origen, el texto destino, y opcionalmente un patrón
     * regex compilado para reemplazos de palabras completas.
     * </p>
     */
    private static final class EntradaReemplazo {
        final String origen;
        final String destino;
        final Pattern patronCompilado;
        final String destinoQuoted;

        EntradaReemplazo(String origen, String destino) {
            this.origen = origen;
            this.destino = destino;
            boolean usaRegex = ES_PALABRA_SIMPLE.matcher(origen).matches()
                            && ES_PALABRA_SIMPLE.matcher(destino).matches();
            if (usaRegex) {
                this.patronCompilado = Pattern.compile(
                    "(?<!\\p{L})" + Pattern.quote(origen) + "(?!\\p{L})");
                this.destinoQuoted = Matcher.quoteReplacement(destino);
            } else {
                this.patronCompilado = null;
                this.destinoQuoted = null;
            }
        }
    }

    private static final Pattern ES_PALABRA_SIMPLE = Pattern.compile("\\p{L}+");

    private static final EntradaReemplazo[] REEMPLAZOS_ES_A_EN = crearReemplazos(0);
    private static final EntradaReemplazo[] REEMPLAZOS_EN_A_ES = crearReemplazos(1);

    private I18nLogs() {
    }

    /**
     * Traduce un mensaje de logging según el idioma configurado.
     * <p>
     * Si el idioma es español, devuelve el mensaje sin cambios.
     * Si el idioma es inglés, aplica reemplazos de español a inglés.
     * Para otros idiomas, devuelve el mensaje original.
     * </p>
     *
     * @param mensaje el mensaje a traducir, puede ser {@code null}
     * @return el mensaje traducido, o cadena vacía si el input es {@code null}
     */
    public static String tr(String mensaje) {
        if (mensaje == null) {
            return "";
        }
        if (I18nUI.obtenerIdioma() == IdiomaUI.ES) {
            return aplicarReemplazos(mensaje, REEMPLAZOS_EN_A_ES);
        }
        if (I18nUI.obtenerIdioma() != IdiomaUI.EN) {
            return mensaje;
        }

        return aplicarReemplazos(mensaje, REEMPLAZOS_ES_A_EN);
    }

    /**
     * Devuelve un mensaje técnico sin traducción, sanitizando valores null.
     * <p>
     * Este método se utiliza para mensajes que no deben ser traducidos,
     * como rutas de archivo, URLs, o contenido técnico.
     * </p>
     *
     * @param mensaje el mensaje técnico, puede ser {@code null}
     * @return el mensaje original, o cadena vacía si el input es {@code null}
     */
    public static String trTecnico(String mensaje) {
        return mensaje != null ? mensaje : "";
    }

    /**
     * Aplica una serie de reemplazos a un texto.
     * <p>
     * Para palabras simples, usa patrones regex para coincidir palabras completas.
     * Para frases complejas, usa reemplazo directo de strings.
     * </p>
     *
     * @param texto el texto al que aplicar los reemplazos
     * @param reemplazos array de entradas de reemplazo a aplicar
     * @return el texto con todos los reemplazos aplicados
     */
    private static String aplicarReemplazos(String texto, EntradaReemplazo... reemplazos) {
        String resultado = texto;
        for (EntradaReemplazo r : reemplazos) {
            if (r.patronCompilado != null) {
                resultado = r.patronCompilado.matcher(resultado).replaceAll(r.destinoQuoted);
            } else {
                resultado = resultado.replace(r.origen, r.destino);
            }
        }
        return resultado;
    }

    /**
     * Crea un array de entradas de reemplazo ordenadas por longitud descendente.
     * <p>
     * El ordenamiento por longitud asegura que las cadenas más largas se
     * reemplacen primero, evitando que substrings cortos interfieran con
     * reemplazos de frases más largas.
     * </p>
     *
     * @param indiceOrigen 0 para ES→EN, 1 para EN→ES
     * @return array de entradas de reemplazo ordenadas
     */
    private static EntradaReemplazo[] crearReemplazos(int indiceOrigen) {
        String[][] ordenados = new String[REEMPLAZOS_INGLES.length][2];
        for (int i = 0; i < REEMPLAZOS_INGLES.length; i++) {
            ordenados[i][0] = REEMPLAZOS_INGLES[i][0];
            ordenados[i][1] = REEMPLAZOS_INGLES[i][1];
        }
        Arrays.sort(ordenados, (a, b) -> Integer.compare(
            b[indiceOrigen].length(), a[indiceOrigen].length()));

        EntradaReemplazo[] resultado = new EntradaReemplazo[ordenados.length];
        int idxOrigen = indiceOrigen;
        int idxDestino = indiceOrigen == 0 ? 1 : 0;
        for (int i = 0; i < ordenados.length; i++) {
            resultado[i] = new EntradaReemplazo(ordenados[i][idxOrigen], ordenados[i][idxDestino]);
        }
        return resultado;
    }
}
