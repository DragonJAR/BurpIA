package com.burpia.i18n;


public final class I18nUI {
    private static volatile IdiomaUI idiomaActual = IdiomaUI.porDefecto();

    private I18nUI() {
    }

    public static void establecerIdioma(IdiomaUI idioma) {
        idiomaActual = idioma != null ? idioma : IdiomaUI.porDefecto();
    }

    public static void establecerIdioma(String codigoIdioma) {
        establecerIdioma(IdiomaUI.desdeCodigo(codigoIdioma));
    }

    public static IdiomaUI obtenerIdioma() {
        return idiomaActual;
    }

    public static String tr(String es, String en) {
        return idiomaActual == IdiomaUI.EN ? en : es;
    }

    public static String trf(String esFormato, String enFormato, Object... args) {
        return String.format(tr(esFormato, enFormato), args);
    }

    public static final class Pestanias {
        private Pestanias() {
        }

        public static String TAREAS() {
            return tr("📋 TAREAS", "📋 TASKS");
        }

        public static String HALLAZGOS() {
            return tr("🔍 HALLAZGOS", "🔍 FINDINGS");
        }

        public static String CONSOLA() {
            return tr("📝 CONSOLA", "📝 CONSOLE");
        }
    }

    public static final class Estadisticas {
        private Estadisticas() {
        }

        public static String TITULO_HALLAZGOS() {
            return tr("🎯 POSIBLES HALLAZGOS Y CRITICIDADES", "🎯 FINDINGS AND SEVERITY OVERVIEW");
        }

        public static String TITULO_OPERATIVO() {
            return tr("📊 DETALLES OPERATIVOS", "📊 OPERATIONAL DETAILS");
        }

        public static String RESUMEN_TOTAL(int total) {
            return trf("🔎 Total: %d", "🔎 Total: %d", total);
        }

        public static String RESUMEN_SEVERIDAD(int critical, int high, int medium, int low, int info) {
            return trf("🟣 %d   🔴 %d   🟠 %d   🟢 %d   🔵 %d", "🟣 %d   🔴 %d   🟠 %d   🟢 %d   🔵 %d",
                critical, high, medium, low, info);
        }

        public static String LIMITE_HALLAZGOS(int limite) {
            return trf("🧮 Limite Hallazgos: %d", "🧮 Findings Limit: %d", limite);
        }

        public static String RESUMEN_OPERATIVO(int solicitudes, int analizados, int omitidos, int errores) {
            return trf("📥 Solicitudes: %d   |   ✅ Analizados: %d   |   ⏭ Omitidos: %d   |   ❌ Errores: %d",
                "📥 Requests: %d   |   ✅ Analyzed: %d   |   ⏭ Skipped: %d   |   ❌ Errors: %d",
                solicitudes, analizados, omitidos, errores);
        }
    }

    public static final class Tareas {
        private Tareas() {
        }

        public static String TRADUCIR_ESTADO(String estadoOriginal) {
            if (estadoOriginal == null) return "";
            switch (estadoOriginal) {
                case com.burpia.model.Tarea.ESTADO_EN_COLA: return tr("En Cola", "Queued");
                case com.burpia.model.Tarea.ESTADO_ANALIZANDO: return tr("Analizando", "Analyzing");
                case com.burpia.model.Tarea.ESTADO_COMPLETADO: return tr("Completado", "Completed");
                case com.burpia.model.Tarea.ESTADO_ERROR: return tr("Error", "Error");
                case com.burpia.model.Tarea.ESTADO_CANCELADO: return tr("Cancelado", "Canceled");
                case com.burpia.model.Tarea.ESTADO_PAUSADO: return tr("Pausado", "Paused");
                default: return tr(estadoOriginal, estadoOriginal);
            }
        }

        public static String TITULO_CONTROLES() {
            return tr("🎮 CONTROLES DE TAREAS", "🎮 TASK CONTROLS");
        }

        public static String TITULO_LISTA() {
            return tr("📋 LISTA DE TAREAS", "📋 TASK LIST");
        }

        public static String BOTON_PAUSAR_TODO() {
            return tr("⏸️ Pausar Todo", "⏸️ Pause All");
        }

        public static String BOTON_REANUDAR_TODO() {
            return tr("▶️ Reanudar Todo", "▶️ Resume All");
        }

        public static String BOTON_CANCELAR_TODO() {
            return tr("🛑 Cancelar Todo", "🛑 Cancel All");
        }

        public static String BOTON_LIMPIAR() {
            return tr("🧹 Limpiar", "🧹 Clear");
        }

        public static String ESTADISTICAS(int activas, int completadas, int errores) {
            return trf("📊 Tareas Activas: %d | ✅ Completadas: %d | ❌ Con Errores: %d",
                "📊 Active Tasks: %d | ✅ Completed: %d | ❌ Failed: %d",
                activas, completadas, errores);
        }

        public static String INFO_SIN_TAREAS_GESTIONAR() {
            return tr("No hay tareas activas o pausadas para gestionar.",
                "There are no active or paused tasks to manage.");
        }

        public static String INFO_SIN_TAREAS_CANCELAR() {
            return tr("No hay tareas activas para cancelar.",
                "There are no active tasks to cancel.");
        }

        public static String INFO_SIN_TAREAS_LIMPIAR() {
            return tr("No hay tareas completadas para limpiar.",
                "There are no completed tasks to clear.");
        }

        public static String TITULO_INFORMACION() {
            return tr("Informacion", "Information");
        }

        public static String TITULO_CONFIRMAR_CANCELACION() {
            return tr("Confirmar cancelacion", "Confirm cancellation");
        }

        public static String TITULO_CONFIRMAR_LIMPIEZA() {
            return tr("Confirmar limpieza", "Confirm cleanup");
        }

        public static String MSG_CONFIRMAR_CANCELAR_TAREAS(int total) {
            return trf("¿Cancelar %d tarea(s) activa(s)?",
                "Cancel %d active task(s)?",
                total);
        }

        public static String MSG_CONFIRMAR_CANCELAR_UNA_TAREA() {
            return tr("¿Cancelar esta tarea?", "Cancel this task?");
        }

        public static String MSG_CONFIRMAR_LIMPIAR_COMPLETADAS(int total) {
            return trf("¿Eliminar %d tarea(s) completada(s)?",
                "Remove %d completed task(s)?",
                total);
        }

        public static String MENU_VER_DETALLES_ERROR() {
            return tr("📋 Ver Detalles del Error", "📋 View Error Details");
        }

        public static String MENU_REINTENTAR() {
            return tr("🔄 Reintentar", "🔄 Retry");
        }

        public static String MENU_PAUSAR() {
            return tr("⏸️ Pausar", "⏸️ Pause");
        }

        public static String MENU_REANUDAR() {
            return tr("▶️ Reanudar", "▶️ Resume");
        }

        public static String MENU_CANCELAR() {
            return tr("🛑 Cancelar", "🛑 Cancel");
        }

        public static String MENU_ELIMINAR_LISTA() {
            return tr("🧹 Eliminar de la Lista", "🧹 Remove from List");
        }

        public static String MENU_REINTENTAR_MULTIPLES(int total) {
            return trf("🔄 Reintentar %d tarea(s) con error/cancelada(s)",
                "🔄 Retry %d failed/canceled task(s)",
                total);
        }

        public static String MENU_PAUSAR_MULTIPLES(int total) {
            return trf("⏸️ Pausar %d tarea(s) activa(s)",
                "⏸️ Pause %d active task(s)",
                total);
        }

        public static String MENU_REANUDAR_MULTIPLES(int total) {
            return trf("▶️ Reanudar %d tarea(s) pausada(s)",
                "▶️ Resume %d paused task(s)",
                total);
        }

        public static String MENU_CANCELAR_MULTIPLES(int total) {
            return trf("🛑 Cancelar %d tarea(s) activa(s)",
                "🛑 Cancel %d active task(s)",
                total);
        }

        public static String MENU_ELIMINAR_MULTIPLES(int total) {
            return trf("🧹 Eliminar %d tarea(s) finalizada(s)",
                "🧹 Remove %d completed task(s)",
                total);
        }

        public static String TITULO_DETALLES_ERROR() {
            return tr("Detalles del Error", "Error Details");
        }

        public static String MSG_DETALLES_ERROR(String url, String duracion, String estado) {
            return trf("URL: %s%nDuracion: %s%nEstado: %s",
                "URL: %s%nDuration: %s%nStatus: %s",
                url, duracion, estado);
        }

        public static String MSG_REINTENTOS(int total) {
            return trf("Tareas puestas en cola para reintentar: %d",
                "Tasks re-queued for retry: %d",
                total);
        }

        public static String MSG_PAUSADAS(int total) {
            return trf("Tareas pausadas: %d", "Tasks paused: %d", total);
        }

        public static String MSG_REANUDADAS(int total) {
            return trf("Tareas reanudadas: %d", "Tasks resumed: %d", total);
        }

        public static String MSG_CANCELADAS(int total) {
            return trf("Tareas canceladas: %d", "Tasks canceled: %d", total);
        }

        public static String MSG_ELIMINADAS(int total) {
            return trf("Tareas eliminadas: %d", "Tasks removed: %d", total);
        }
    }

    public static final class Hallazgos {
        private Hallazgos() {
        }

        public static String OPCION_TODAS_CRITICIDADES() {
            return tr("Todas las Criticidades", "All Severities");
        }

        public static String TITULO_FILTROS() {
            return tr("🔭 FILTROS Y BUSQUEDA", "🔭 FILTERS AND SEARCH");
        }

        public static String TITULO_GUARDAR_PROYECTO() {
            return tr("💾 GUARDAR EN PROYECTO", "💾 SAVE TO PROJECT");
        }

        public static String TITULO_TABLA() {
            return tr("💎 HALLAZGOS DE SEGURIDAD", "💎 SECURITY FINDINGS");
        }

        public static String ETIQUETA_BUSCAR() {
            return tr("🔎 Buscar:", "🔎 Search:");
        }

        public static String BOTON_LIMPIAR() {
            return tr("❌ Limpiar", "❌ Clear");
        }

        public static String BOTON_EXPORTAR_CSV() {
            return tr("📊 Exportar CSV", "📊 Export CSV");
        }

        public static String BOTON_EXPORTAR_JSON() {
            return tr("📄 Exportar JSON", "📄 Export JSON");
        }

        public static String BOTON_LIMPIAR_TODO() {
            return tr("🗑️ Eliminar Todo", "🗑️ Erase All");
        }

        public static String CHECK_GUARDAR_ISSUES() {
            return tr("Guardar automaticamente en Issues", "Automatically save to Issues");
        }

        public static String CHECK_GUARDAR_ISSUES_SOLO_PRO() {
            return tr(
                "Guardar automaticamente en Issues (solo Burp Professional)",
                "Automatically save to Issues (Burp Professional only)"
            );
        }

        public static String MSG_SIN_HALLAZGOS_LIMPIAR() {
            return tr("No hay hallazgos para limpiar.", "There are no findings to clear.");
        }

        public static String TITULO_INFORMACION() {
            return tr("Informacion", "Information");
        }

        public static String MSG_CONFIRMAR_LIMPIEZA(int total) {
            return trf("¿Estas seguro de que deseas limpiar todos los hallazgos (%d)?",
                "Are you sure you want to clear all findings (%d)?",
                total);
        }

        public static String TITULO_CONFIRMAR_LIMPIEZA() {
            return tr("Confirmar limpieza", "Confirm cleanup");
        }

        public static String DIALOGO_EXPORTAR_CSV() {
            return tr("Exportar hallazgos a CSV", "Export findings to CSV");
        }

        public static String DIALOGO_EXPORTAR_JSON() {
            return tr("Exportar hallazgos a JSON", "Export findings to JSON");
        }

        public static String CSV_HEADER() {
            return tr("Hora,URL,Hallazgo,Severidad,Confianza",
                "Time,URL,Finding,Severity,Confidence");
        }

        public static String MSG_EXPORTACION_EXITOSA(int total, String archivo, int ignorados) {
            String base = trf("Se exportaron %d hallazgos a %s",
                "Exported %d findings to %s",
                total, archivo);
            if (ignorados > 0) {
                return base + trf("%n(Ignorados y no exportados: %d)",
                    "%n(Ignored and not exported: %d)",
                    ignorados);
            }
            return base;
        }

        public static String TITULO_EXPORTACION_EXITOSA() {
            return tr("Exportacion exitosa", "Export successful");
        }

        public static String MSG_ERROR_EXPORTAR(String error) {
            return trf("Error al exportar: %s", "Export error: %s", error);
        }

        public static String TITULO_ERROR_EXPORTACION() {
            return tr("Error de exportacion", "Export error");
        }

        public static String MENU_ENVIAR_REPEATER() {
            return tr("📤 Enviar al Repeater", "📤 Send to Repeater");
        }

        public static String MENU_ENVIAR_INTRUDER() {
            return tr("🔍 Enviar a Intruder", "🔍 Send to Intruder");
        }

        public static String MENU_ENVIAR_SCANNER() {
            return tr("🛰️ Enviar a Scanner Pro", "🛰️ Send to Scanner Pro");
        }

        public static String MENU_ENVIAR_ISSUES() {
            return tr("📌 Enviar a Issues de Burp", "📌 Send to Burp Issues");
        }

        public static String MENU_ENVIAR_ISSUES_SOLO_PRO() {
            return tr(
                "📌 Enviar a Issues de Burp (solo Pro)",
                "📌 Send to Burp Issues (Pro only)"
            );
        }

        public static String MENU_IGNORAR() {
            return tr("🚫 Ignorar", "🚫 Ignore");
        }

        public static String MENU_BORRAR() {
            return tr("🗑️ Borrar", "🗑️ Delete");
        }

        public static String MSG_CONFIRMAR_IGNORAR(int total) {
            if (total == 1) {
                return tr("¿Ignorar este hallazgo?", "Ignore this finding?");
            }
            return trf("¿Ignorar %d hallazgos?", "Ignore %d findings?", total);
        }

        public static String TITULO_CONFIRMAR_IGNORAR() {
            return tr("Confirmar ignorar", "Confirm ignore");
        }

        public static String MSG_CONFIRMAR_BORRAR(int total) {
            if (total == 1) {
                return tr("¿Borrar este hallazgo de la tabla?", "Delete this finding from the table?");
            }
            return trf("¿Borrar %d hallazgos de la tabla?", "Delete %d findings from the table?", total);
        }

        public static String TITULO_CONFIRMAR_BORRAR() {
            return tr("Confirmar borrado", "Confirm deletion");
        }

        public static String TITULO_ACCION_REPEATER() {
            return tr("Enviar al Repeater", "Send to Repeater");
        }

        public static String RESUMEN_ACCION_REPEATER() {
            return tr("Enviados al Repeater", "Sent to Repeater");
        }

        public static String TITULO_ACCION_INTRUDER() {
            return tr("Enviar a Intruder", "Send to Intruder");
        }

        public static String RESUMEN_ACCION_INTRUDER() {
            return tr("Enviados a Intruder", "Sent to Intruder");
        }

        public static String TITULO_ACCION_SCANNER() {
            return tr("Enviar a Scanner Pro", "Send to Scanner Pro");
        }

        public static String RESUMEN_ACCION_SCANNER() {
            return tr("Enviados a Scanner Pro", "Sent to Scanner Pro");
        }

        public static String TITULO_ACCION_ISSUES() {
            return tr("Enviar a Issues de Burp", "Send to Burp Issues");
        }

        public static String RESUMEN_ACCION_ISSUES() {
            return tr("Guardados en Issues de Burp", "Saved to Burp Issues");
        }

        public static String SUFIJO_ISSUE_GUARDADO() {
            return tr("(guardado en Issues)", "(saved to Issues)");
        }

        public static String ERROR_HALLAZGO_NO_DISPONIBLE() {
            return tr("Hallazgo no disponible", "Finding unavailable");
        }

        public static String ERROR_SITEMAP_NO_DISPONIBLE() {
            return tr("Site Map de Burp no disponible", "Burp Site Map unavailable");
        }

        public static String ERROR_GUARDAR_ISSUE() {
            return tr("No se pudo crear o guardar el Issue", "Could not create or save the Issue");
        }

        public static String MSG_ISSUES_SOLO_PRO() {
            return tr(
                "Esta accion solo esta disponible en Burp Suite Professional.",
                "This action is only available in Burp Suite Professional."
            );
        }

        public static String MSG_SIN_REQUEST() {
            return tr("(sin request)", "(missing request)");
        }

        public static String MSG_SIN_REQUEST_ORIGINAL(int total) {
            return trf("%nSin request original: %d", "%nMissing original request: %d", total);
        }

        public static String MSG_IGNORADOS_OMITIDOS(int total) {
            return trf("%nIgnorados omitidos: %d", "%nIgnored skipped: %d", total);
        }

        public static String MSG_RESULTADO_ACCION(String resumen, int exitosos, int total, int sinRequest, int ignorados, String detalle) {
            String texto = resumen + ": " + exitosos + "/" + total;
            if (sinRequest > 0) {
                texto += MSG_SIN_REQUEST_ORIGINAL(sinRequest);
            }
            if (ignorados > 0) {
                texto += MSG_IGNORADOS_OMITIDOS(ignorados);
            }
            return texto + "\n\n" + detalle;
        }

        public static String MSG_ACCION_SOLO_IGNORADOS(int total) {
            return trf(
                "No hay hallazgos operables: los %d seleccionados estan marcados como ignorados.",
                "No actionable findings: the %d selected are marked as ignored.",
                total
            );
        }

        public static String MSG_HALLAZGOS_IGNORADOS(int total) {
            return trf("Hallazgos ignorados: %d%n%nNo se incluiran en exportaciones CSV/JSON.",
                "Ignored findings: %d%n%nThey will not be included in CSV/JSON exports.",
                total);
        }

        public static String TITULO_HALLAZGOS_IGNORADOS() {
            return tr("Hallazgos ignorados", "Ignored findings");
        }

        public static String MSG_HALLAZGOS_BORRADOS(int total) {
            return trf("Hallazgos eliminados: %d", "Deleted findings: %d", total);
        }

        public static String TITULO_HALLAZGOS_BORRADOS() {
            return tr("Hallazgos borrados", "Deleted findings");
        }
    }

    public static final class Consola {
        private Consola() {
        }

        public static String TITULO_CONTROLES() {
            return tr("⚙️ CONTROLES DE CONSOLA", "⚙️ CONSOLE CONTROLS");
        }

        public static String TITULO_LOGS() {
            return tr("📝 LOGS DEL SISTEMA", "📝 SYSTEM LOGS");
        }

        public static String CHECK_AUTO_SCROLL() {
            return tr("📜 Auto-scroll", "📜 Auto-scroll");
        }

        public static String BOTON_LIMPIAR() {
            return tr("🧹 Limpiar Consola", "🧹 Clear Console");
        }

        public static String RESUMEN(int total, int info, int detallados, int errores) {
            return trf("📊 Total LOGs: %d | ℹ️ Informativos: %d | 🔍 Detallados: %d | ❌ Errores: %d",
                "📊 Total Logs: %d | ℹ️ Info: %d | 🔍 Verbose: %d | ❌ Errors: %d",
                total, info, detallados, errores);
        }

        public static String MSG_CONSOLA_VACIA() {
            return tr("La consola ya esta vacia.", "The console is already empty.");
        }

        public static String TITULO_INFORMACION() {
            return tr("Informacion", "Information");
        }

        public static String MSG_CONFIRMAR_LIMPIEZA(int total) {
            return trf("¿Limpiar %d log(s) de la consola?",
                "Clear %d console log(s)?",
                total);
        }

        public static String TITULO_CONFIRMAR_LIMPIEZA() {
            return tr("Confirmar limpieza", "Confirm cleanup");
        }

        public static String LOG_INICIALIZADA() {
            return tr("Consola GUI inicializada", "GUI console initialized");
        }
    }

    public static final class DetalleHallazgo {
        private DetalleHallazgo() {
        }

        public static String TITULO_DIALOGO() {
            return tr("💎 Detalle de Hallazgo", "💎 Finding Details");
        }

        public static String TITULO_PANEL() {
            return tr("✏️ EDITAR HALLAZGO", "✏️ EDIT FINDING");
        }

        public static String LABEL_URL() {
            return tr("URL:", "URL:");
        }

        public static String LABEL_TITULO() {
            return tr("Título:", "Title:");
        }

        public static String LABEL_SEVERIDAD() {
            return tr("Severidad:", "Severity:");
        }

        public static String LABEL_CONFIANZA() {
            return tr("       Confianza: ", "       Confidence: ");
        }

        public static String LABEL_DESCRIPCION() {
            return tr("Descripcion:", "Description:");
        }

        public static String BOTON_GUARDAR() {
            return tr("💾 Guardar Cambios", "💾 Save Changes");
        }

        public static String BOTON_CANCELAR() {
            return tr("❌ Cancelar", "❌ Cancel");
        }

        public static String MSG_VALIDACION() {
            return tr("La URL y la descripcion no pueden estar vacias.",
                "URL and description cannot be empty.");
        }

        public static String TITULO_ERROR_VALIDACION() {
            return tr("Error de validacion", "Validation error");
        }
    }

    public static final class Configuracion {
        private Configuracion() {
        }

        public static String TITULO_DIALOGO() {
            return tr("🧠 Ajustes de BurpIA", "🧠 BurpIA Settings");
        }

        public static String TAB_PROVEEDOR() {
            return tr("🏢 Proveedor LLM", "🏢 LLM Provider");
        }

        public static String TAB_PROMPT() {
            return tr("📝 Prompt", "📝 Prompt");
        }

        public static String TAB_ACERCA() {
            return tr("ℹ️ Acerca de", "ℹ️ About");
        }

        public static String LABEL_TIMEOUT_MODELO() {
            return tr("Timeout de Modelo (s):", "Model Timeout (s):");
        }

        public static String BOTON_GUARDAR() {
            return tr("💾 Guardar Ajustes", "💾 Save Settings");
        }

        public static String BOTON_PROBAR_CONEXION() {
            return tr("🔍 Probar Conexion", "🔍 Test Connection");
        }

        public static String BOTON_PROBANDO() {
            return tr("🔍 Probando...", "🔍 Testing...");
        }

        public static String TITULO_AJUSTES_USUARIO() {
            return tr("⚙️ AJUSTES DE USUARIO", "⚙️ USER SETTINGS");
        }

        public static String LABEL_IDIOMA() {
            return tr("Idioma:", "Language:");
        }

        public static String LABEL_RETRASO() {
            return tr("Retraso (seg):", "Delay (sec):");
        }

        public static String LABEL_MAXIMO_CONCURRENTE() {
            return tr("Maximo Concurrente:", "Max Concurrent:");
        }

        public static String LABEL_MAX_HALLAZGOS_TABLA() {
            return tr("Max Hallazgos Tabla:", "Max Findings Table:");
        }

        public static String LABEL_MODO_DETALLADO() {
            return tr("Modo Detallado:", "Verbose Mode:");
        }

        public static String CHECK_DETALLADO() {
            return tr("Activar registro detallado (recomendado para depuracion)",
                "Enable verbose logging (recommended for troubleshooting)");
        }

        public static String DESCRIPCION_DETALLADO() {
            return tr("El modo detallado registra solicitudes HTTP, llamadas API y operaciones internas.",
                "Verbose mode records HTTP requests, API calls, and internal operations.");
        }

        public static String TITULO_PROVEEDOR() {
            return tr("🤖 CONFIGURACION DE PROVEEDOR AI", "🤖 AI PROVIDER SETTINGS");
        }

        public static String LABEL_PROVEEDOR_AI() {
            return tr("Proveedor AI:", "AI Provider:");
        }

        public static String LABEL_URL_API() {
            return tr("URL de API:", "API URL:");
        }

        public static String LABEL_CLAVE_API() {
            return tr("Clave de API:", "API Key:");
        }

        public static String LABEL_MODELO() {
            return tr("Modelo:", "Model:");
        }

        public static String BOTON_CARGAR_MODELOS() {
            return tr("🔄 Cargar Modelos", "🔄 Load Models");
        }

        public static String BOTON_ACTUALIZANDO_MODELOS() {
            return tr("🔄 Actualizando...", "🔄 Refreshing...");
        }

        public static String LABEL_MAX_TOKENS() {
            return tr("Maximo de Tokens:", "Max Tokens:");
        }

        public static String TITULO_INSTRUCCIONES() {
            return tr("📝 INSTRUCCIONES", "📝 INSTRUCTIONS");
        }

        public static String TEXTO_INSTRUCCIONES() {
            return tr(
                "• El token {REQUEST} se reemplaza automaticamente con la solicitud HTTP analizada.\n" +
                    "• El token {RESPONSE} se reemplaza automaticamente con la respuesta HTTP capturada.\n" +
                    "• El token {OUTPUT_LANGUAGE} se reemplaza con el idioma configurado por el usuario.\n" +
                    "• Incluye {REQUEST}, {RESPONSE} y {OUTPUT_LANGUAGE} donde quieras insertar la evidencia.\n" +
                    "• El prompt debe pedir respuesta JSON estricta con este formato:",
                "• The {REQUEST} token is automatically replaced with the analyzed HTTP request.\n" +
                    "• The {RESPONSE} token is automatically replaced with the captured HTTP response.\n" +
                    "• The {OUTPUT_LANGUAGE} token is replaced with the user-configured language.\n" +
                    "• Include {REQUEST}, {RESPONSE}, and {OUTPUT_LANGUAGE} exactly where you want the evidence inserted.\n" +
                    "• The prompt must request strict JSON output in this format:"
            );
        }

        public static String TITULO_PROMPT_ANALISIS() {
            return tr("✍️ PROMPT DE ANALISIS", "✍️ ANALYSIS PROMPT");
        }

        public static String BOTON_RESTAURAR_PROMPT() {
            return tr("🔄 Restaurar Prompt por Defecto", "🔄 Restore Default Prompt");
        }

        public static String CONTADOR_CARACTERES(int longitud) {
            return trf("Caracteres: %d", "Characters: %d", longitud);
        }

        public static String CONTADOR_FALTA_REQUEST(int longitud) {
            return trf("Caracteres: %d ⚠️ Falta {REQUEST}",
                "Characters: %d ⚠️ Missing {REQUEST}",
                longitud);
        }

        public static String ADVERTENCIA_PROMPT() {
            return tr("⚠️ Importante: {REQUEST} es obligatorio. Se recomienda incluir {RESPONSE} para mejor contexto.",
                "⚠️ Important: {REQUEST} is required. {RESPONSE} is recommended for better context.");
        }

        public static String TITULO_APP() {
            return "BurpIA";
        }

        public static String SUBTITULO_APP() {
            return tr("Analisis de Seguridad con Inteligencia Artificial",
                "Security Analysis with Artificial Intelligence");
        }

        public static String TITULO_RESUMEN() {
            return tr("RESUMEN", "SUMMARY");
        }

        public static String DESCRIPCION_APP() {
            return tr(
                "BurpIA es una extension profesional para Burp Suite que aprovecha multiples " +
                    "modelos de Inteligencia Artificial para analizar trafico HTTP e identificar " +
                    "vulnerabilidades de seguridad de forma automatizada.\n\n" +
                    "Caracteristicas principales:\n" +
                    "• Compatibilidad con OpenAI, Claude, Gemini, Z.ai, Minimax y Ollama\n" +
                    "• De-duplicacion inteligente de peticiones para optimizar la cuota de API\n" +
                    "• Gestion asincrona mediante colas de tareas paralelizables\n" +
                    "• Integracion con site map (Issues), Repeater, Intruder y Scanner Pro\n" +
                    "• Prompt totalmente configurable para analisis a medida\n" +
                    "• Exportacion nativa de reportes de hallazgos a CSV y JSON",
                "BurpIA is a professional Burp Suite extension that uses multiple " +
                    "Artificial Intelligence models to analyze HTTP traffic and identify " +
                    "security vulnerabilities automatically.\n\n" +
                    "Key features:\n" +
                    "• Compatibility with OpenAI, Claude, Gemini, Z.ai, Minimax, and Ollama\n" +
                    "• Smart request deduplication to optimize API usage\n" +
                    "• Asynchronous task queue with parallel processing\n" +
                    "• Integration with site map (Issues), Repeater, Intruder, and Scanner Pro\n" +
                    "• Fully configurable prompt for custom analysis\n" +
                    "• Native findings export to CSV and JSON"
            );
        }

        public static String TITULO_DESARROLLADO_POR() {
            return tr("DESARROLLADO POR", "DEVELOPED BY");
        }

        public static String BOTON_SITIO_WEB() {
            return tr("Visitar DragonJAR.org", "Visit DragonJAR.org");
        }

        public static String VERSION() {
            return tr("Version 1.0.0 - Febrero 2026", "Version 1.0.0 - February 2026");
        }

        public static String TITULO_ENLACE() {
            return tr("Enlace", "Link");
        }

        public static String MSG_URL() {
            return "URL: https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica";
        }

        public static String MSG_ERROR_ABRIR_NAVEGADOR(String detalle) {
            return trf("Error al abrir el navegador: %s",
                "Error opening browser: %s",
                detalle);
        }

        public static String TITULO_ERROR() {
            return "Error";
        }

        public static String MSG_CONFIRMAR_RESTAURAR_PROMPT() {
            return tr("¿Estas seguro de que deseas restaurar el prompt por defecto? Se perderan tus cambios personalizados.",
                "Are you sure you want to restore the default prompt? Your custom changes will be lost.");
        }

        public static String TITULO_CONFIRMAR_RESTAURACION() {
            return tr("Confirmar Restauracion", "Confirm Restore");
        }

        public static String MSG_ERROR_PROMPT_SIN_REQUEST() {
            return tr("El prompt debe contener el token {REQUEST} para indicar donde se insertara la solicitud HTTP.\n" +
                    "El token {RESPONSE} es opcional pero recomendado para analizar la respuesta del servidor.",
                "The prompt must include the {REQUEST} token to indicate where the HTTP request will be inserted.\n" +
                    "The {RESPONSE} token is optional but recommended to analyze the server response.");
        }

        public static String TITULO_ERROR_VALIDACION() {
            return tr("Error de Validacion", "Validation Error");
        }

        public static String MSG_ERROR_GUARDAR_CONFIG(String detalle, String ruta) {
            return trf("No se pudo guardar la configuracion:\n%s\n\nRuta: %s",
                "Configuration could not be saved:\n%s\n\nPath: %s",
                detalle, ruta);
        }

        public static String TITULO_ERROR_GUARDAR() {
            return tr("Error al Guardar", "Save Error");
        }

        public static String MSG_ERROR_FORMATO_NUMERO() {
            return tr("Formato de numero invalido en ajustes de usuario",
                "Invalid number format in user settings");
        }

        public static String MSG_MODELOS_ACTUALIZADOS(int cantidad, String proveedor) {
            return trf("Se cargaron %d modelos para %s",
                "Loaded %d models for %s",
                cantidad, proveedor);
        }

        public static String TITULO_MODELOS_ACTUALIZADOS() {
            return tr("Modelos Actualizados", "Models Updated");
        }

        public static String MSG_ERROR_PROCESAR_MODELOS(String detalle) {
            return trf("Error al procesar modelos: %s",
                "Error processing models: %s",
                detalle);
        }

        public static String MSG_SELECCIONA_PROVEEDOR() {
            return tr("Por favor selecciona un proveedor de AI",
                "Please select an AI provider");
        }

        public static String TITULO_VALIDACION() {
            return tr("Validacion", "Validation");
        }

        public static String MSG_URL_VACIA() {
            return tr("La URL de API no puede estar vacia.\nPor favor ingresa una URL valida.",
                "The API URL cannot be empty.\nPlease provide a valid URL.");
        }

        public static String MSG_API_KEY_VACIA(String proveedor) {
            return trf("La clave de API no puede estar vacia para %s.\nPor favor ingresa tu clave de API.",
                "API key cannot be empty for %s.\nPlease provide your API key.",
                proveedor);
        }

        public static String MSG_SELECCIONA_MODELO() {
            return tr("Por favor selecciona un modelo",
                "Please select a model");
        }

        public static String TITULO_CONEXION_EXITOSA() {
            return tr("✅ Conexion Exitosa", "✅ Connection Successful");
        }

        public static String TITULO_ERROR_CONEXION() {
            return tr("❌ Error de Conexion", "❌ Connection Error");
        }

        public static String MSG_ERROR_PRUEBA_INESPERADO(String detalle) {
            return trf("Error inesperado durante la prueba:%n%n%s",
                "Unexpected error during test:%n%n%s",
                detalle);
        }
    }

    public static final class Contexto {
        private Contexto() {
        }

        public static String ITEM_ANALIZAR_SOLICITUD() {
            return tr("Analizar Solicitud con BurpIA", "Analyze Request with BurpIA");
        }

        public static String MSG_ANALISIS_INICIADO() {
            return tr("Solicitud enviada para analisis forzado.\n" +
                    "Esto puede tomar unos segundos dependiendo de la respuesta de la AI.",
                "Request sent for forced analysis.\n" +
                    "This may take a few seconds depending on AI response time.");
        }

        public static String TITULO_ANALISIS_INICIADO() {
            return tr("BurpIA - Analisis Iniciado", "BurpIA - Analysis Started");
        }
    }

    public static final class Tablas {
        private Tablas() {
        }

        public static String[] COLUMNAS_TAREAS() {
            return new String[]{
                tr("Tipo", "Type"),
                tr("URL", "URL"),
                tr("Estado", "Status"),
                tr("Duracion", "Duration")
            };
        }

        public static String[] COLUMNAS_HALLAZGOS() {
            return new String[]{
                tr("Hora", "Time"),
                tr("URL", "URL"),
                tr("Hallazgo", "Finding"),
                tr("Severidad", "Severity"),
                tr("Confianza", "Confidence")
            };
        }
    }
}
