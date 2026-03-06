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
        {"Analisis manual sin request/response original: se registraran hallazgos, pero no Issue.",
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
        {"UI registrada en ", "UI registered in "},
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
    private static String aplicarReemplazos(String texto, EntradaReemplazo[] reemplazos) {
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
