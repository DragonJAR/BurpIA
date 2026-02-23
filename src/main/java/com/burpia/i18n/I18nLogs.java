package com.burpia.i18n;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



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
        {"No se pudo encolar AuditIssue por saturación de cola", "Could not enqueue AuditIssue due to queue saturation"},
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
        {"Stack trace de la excepción", "Exception stack trace"},
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
        {"segundos", "seconds"},
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
        {"Contenido extraído - Longitud", "Extracted content - Length"},
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
        {"Cola de análisis saturada, solicitud descartada", "Analysis queue saturated, request discarded"},
        {"Cola de analisis saturada, solicitud descartada", "Analysis queue saturated, request discarded"},
        {"Captura pausada por usuario", "Capture paused by user"},
        {"Captura reanudada por usuario", "Capture resumed by user"},
        {"ACTIVADO", "ENABLED"},
        {"desactivado", "disabled"},
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
        {"configuracion", "configuration"}
    };
    private static final String[][] REEMPLAZOS_ES_A_EN = crearReemplazosOrdenados(0);
    private static final String[][] REEMPLAZOS_EN_A_ES = crearReemplazosOrdenados(1);

    private I18nLogs() {
    }

    public static String tr(String mensaje) {
        if (mensaje == null) {
            return "";
        }
        if (I18nUI.obtenerIdioma() == IdiomaUI.ES) {
            return aplicarReemplazos(mensaje, REEMPLAZOS_EN_A_ES, 1, 0);
        }
        if (I18nUI.obtenerIdioma() != IdiomaUI.EN) {
            return mensaje;
        }

        return aplicarReemplazos(mensaje, REEMPLAZOS_ES_A_EN, 0, 1);
    }

    public static String trTecnico(String mensaje) {
        return mensaje != null ? mensaje : "";
    }

    private static String aplicarReemplazos(String texto,
                                            String[][] reemplazosOrdenados,
                                            int indiceOrigen,
                                            int indiceDestino) {
        String resultado = texto;
        for (String[] reemplazo : reemplazosOrdenados) {
            resultado = reemplazarLiteralSeguro(
                resultado,
                reemplazo[indiceOrigen],
                reemplazo[indiceDestino]
            );
        }
        return resultado;
    }

    private static String reemplazarLiteralSeguro(String texto, String origen, String destino) {
        if (texto == null || texto.isEmpty() || origen == null || origen.isEmpty()) {
            return texto;
        }
        if (esPalabraSimple(origen) && esPalabraSimple(destino)) {
            String patron = "(?<!\\p{L})" + Pattern.quote(origen) + "(?!\\p{L})";
            return Pattern.compile(patron).matcher(texto).replaceAll(Matcher.quoteReplacement(destino));
        }
        return texto.replace(origen, destino);
    }

    private static boolean esPalabraSimple(String texto) {
        return texto != null && texto.matches("\\p{L}+");
    }

    private static String[][] crearReemplazosOrdenados(int indiceOrigen) {
        String[][] copia = new String[REEMPLAZOS_INGLES.length][2];
        for (int i = 0; i < REEMPLAZOS_INGLES.length; i++) {
            copia[i][0] = REEMPLAZOS_INGLES[i][0];
            copia[i][1] = REEMPLAZOS_INGLES[i][1];
        }
        Arrays.sort(copia, (a, b) -> Integer.compare(b[indiceOrigen].length(), a[indiceOrigen].length()));
        return copia;
    }
}
