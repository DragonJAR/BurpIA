package com.burpia.i18n;

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
        {"URL de API", "API URL"},
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
        {"solicitud", "request"},
        {"Solicitud", "Request"},
        {"respuesta", "response"},
        {"Respuesta", "Response"},
        {"DENTRO DE SCOPE - Procesando", "IN SCOPE - Processing"},
        {"FUERA DE SCOPE - Omitiendo", "OUT OF SCOPE - Skipping"},
        {"NOTA: Solo se analizaran solicitudes DENTRO del SCOPE de Burp Suite",
            "NOTE: Only requests INSIDE Burp Suite SCOPE will be analyzed"},
        {"Omitiendo recurso estatico", "Skipping static resource"},
        {"Solicitud duplicada omitida", "Skipped duplicate request"},
        {"Nueva solicitud registrada", "New request recorded"},
        {"Hash de solicitud", "Request hash"},
        {"Detalles de solicitud", "Request details"},
        {"No se pudo capturar la respuesta", "Could not capture response"},
        {"No se pudo analizar solicitud forzada", "Could not analyze forced request"},
        {"No se pudo encolar AuditIssue por saturación de cola", "Could not enqueue AuditIssue due to queue saturation"},
        {"No se pudo encolar AuditIssue por saturacion de cola", "Could not enqueue AuditIssue due to queue saturation"},
        {"No se pudo guardar AuditIssue: SiteMap API no disponible", "Could not save AuditIssue: SiteMap API unavailable"},
        {"Error al crear AuditIssue en Burp Suite", "Error creating AuditIssue in Burp Suite"},
        {"AuditIssue creado en Burp Suite para", "AuditIssue created in Burp Suite for"},
        {"AuditIssue no creado: hallazgo sin datos suficientes", "AuditIssue not created: finding lacks required data"},
        {"Hallazgo sin evidencia HTTP: no se puede crear AuditIssue",
            "Finding without HTTP evidence: AuditIssue cannot be created"},
        {"Hallazgo omitido en Issues", "Finding skipped in Issues"},
        {"Autoguardado deshabilitado", "Autosave disabled"},
        {"Construyendo prompt para URL", "Building prompt for URL"},
        {"Longitud de prompt", "Prompt length"},
        {"Llamando a API", "Calling API"},
        {"con modelo", "with model"},
        {"Codigo de respuesta de API", "API response code"},
        {"Longitud de respuesta de API", "API response length"},
        {"Cuerpo de respuesta de error de API", "API error response body"},
        {"Solicitud HTTP fallida", "HTTP request failed"},
        {"Sistema de retry", "Retry system"},
        {"Intento inmediato", "Immediate attempt"},
        {"Todos los intentos inmediatos fallaron", "All immediate attempts failed"},
        {"Esperando", "Waiting"},
        {"segundos", "seconds"},
        {"Reintento", "Retry"},
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
        {"Configuracion", "Configuration"},
        {"configuracion", "configuration"}
    };

    private I18nLogs() {
    }

    public static String tr(String mensaje) {
        if (mensaje == null) {
            return "";
        }
        if (I18nUI.obtenerIdioma() != IdiomaUI.EN) {
            return mensaje;
        }

        String traducido = mensaje;
        for (String[] reemplazo : REEMPLAZOS_INGLES) {
            traducido = traducido.replace(reemplazo[0], reemplazo[1]);
        }
        return traducido;
    }
}
