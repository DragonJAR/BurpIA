package com.burpia.i18n;

public final class I18nUI {
    private static volatile IdiomaUI idiomaActual = IdiomaUI.porDefecto();
    private static final String URL_DRAGONJAR_CONTACTO = "https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica";

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

    private static String trfBloqueUrl(String esEtiqueta, String enEtiqueta, String url) {
        return trf(esEtiqueta + "%n%s", enEtiqueta + "%n%s", url);
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

        public static String AGENTE() {
            return tr("🤖 AGENTE", "🤖 AGENT");
        }
    }

    public static final class General {
        private General() {
        }

        public static String CONFIGURACION_GUARDADA() {
            return tr("Configuración guardada", "Configuration saved");
        }

        public static String AGENTE_GENERICO() {
            return tr("Agente", "Agent");
        }

        public static String HALLAZGO_GENERICO() {
            return tr("Hallazgo", "Finding");
        }

        public static String SEGUNDOS() {
            return tr("segundos", "seconds");
        }

        public static String ACTIVADO() {
            return tr("ACTIVADO", "ENABLED");
        }

        public static String DESACTIVADO() {
            return tr("desactivado", "disabled");
        }

        public static String COMPLEMENTO_SEGURIDAD_IA() {
            return tr("Complemento de Seguridad con IA", "AI Security Plugin");
        }

        public static String ENTORNO() {
            return tr("Entorno", "Environment");
        }

        public static String CONFIGURACION_IA() {
            return tr("Configuración IA", "AI Configuration");
        }

        public static String CHECK_NO_VOLVER_MOSTRAR_ALERTA() {
            return tr("No volver a mostrar este mensaje", "Do not show this message again");
        }

        public static String TOOLTIP_NO_VOLVER_MOSTRAR_ALERTA() {
            return tr("Desactiva futuros avisos de esta acción.", "Disable future notices for this action.");
        }
    }

    public static final class Estadisticas {
        private Estadisticas() {
        }

        public static String TITULO_HALLAZGOS() {
            return tr("🎯 POSIBLES HALLAZGOS Y SEVERIDADES", "🎯 FINDINGS AND SEVERITY OVERVIEW");
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
            return trf("🧮 Límite Hallazgos: %d", "🧮 Findings Limit: %d", limite);
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

        public static String ESTADO_TAREA_ATASCADA() {
            return tr("Tarea atascada - timeout", "Stuck task - timeout");
        }

        public static String LOG_TAREA_ATASCADA_DETECTADA() {
            return tr("Tarea atascada detectada: ", "Stuck task detected: ");
        }

        public static String TRADUCIR_ESTADO(String estadoOriginal) {
            if (estadoOriginal == null)
                return "";
            switch (estadoOriginal) {
                case com.burpia.model.Tarea.ESTADO_EN_COLA:
                    return tr("En Cola", "Queued");
                case com.burpia.model.Tarea.ESTADO_ANALIZANDO:
                    return tr("Analizando", "Analyzing");
                case com.burpia.model.Tarea.ESTADO_COMPLETADO:
                    return tr("Completado", "Completed");
                case com.burpia.model.Tarea.ESTADO_ERROR:
                    return tr("Error", "Error");
                case com.burpia.model.Tarea.ESTADO_CANCELADO:
                    return tr("Cancelado", "Canceled");
                case com.burpia.model.Tarea.ESTADO_PAUSADO:
                    return tr("Pausado", "Paused");
                default:
                    return tr(estadoOriginal, estadoOriginal);
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
            return tr("Información", "Information");
        }

        public static String TITULO_CONFIRMAR_CANCELACION() {
            return tr("Confirmar cancelación", "Confirm cancellation");
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

        public static String SEVERIDAD_CRITICAL() {
            return tr("Crítica", "Critical");
        }

        public static String SEVERIDAD_HIGH() {
            return tr("Alta", "High");
        }

        public static String SEVERIDAD_MEDIUM() {
            return tr("Media", "Medium");
        }

        public static String SEVERIDAD_LOW() {
            return tr("Baja", "Low");
        }

        public static String SEVERIDAD_INFO() {
            return tr("Informativa", "Info");
        }

        public static String TRADUCIR_SEVERIDAD(String severidad) {
            if (severidad == null)
                return "";
            switch (severidad) {
                case com.burpia.model.Hallazgo.SEVERIDAD_CRITICAL:
                    return SEVERIDAD_CRITICAL();
                case com.burpia.model.Hallazgo.SEVERIDAD_HIGH:
                    return SEVERIDAD_HIGH();
                case com.burpia.model.Hallazgo.SEVERIDAD_MEDIUM:
                    return SEVERIDAD_MEDIUM();
                case com.burpia.model.Hallazgo.SEVERIDAD_LOW:
                    return SEVERIDAD_LOW();
                case com.burpia.model.Hallazgo.SEVERIDAD_INFO:
                    return SEVERIDAD_INFO();
                default:
                    return severidad;
            }
        }

        public static String CONFIANZA_ALTA() {
            return tr("Alta", "High");
        }

        public static String CONFIANZA_MEDIA() {
            return tr("Media", "Medium");
        }

        public static String CONFIANZA_BAJA() {
            return tr("Baja", "Low");
        }

        public static String[] OPCIONES_SEVERIDAD() {
            return new String[] {
                    SEVERIDAD_CRITICAL(),
                    SEVERIDAD_HIGH(),
                    SEVERIDAD_MEDIUM(),
                    SEVERIDAD_LOW(),
                    SEVERIDAD_INFO()
            };
        }

        public static String TRADUCIR_CONFIANZA(String confianza) {
            if (confianza == null)
                return "";
            switch (confianza) {
                case com.burpia.model.Hallazgo.CONFIANZA_ALTA:
                    return CONFIANZA_ALTA();
                case com.burpia.model.Hallazgo.CONFIANZA_MEDIA:
                    return CONFIANZA_MEDIA();
                case com.burpia.model.Hallazgo.CONFIANZA_BAJA:
                    return CONFIANZA_BAJA();
                default:
                    return confianza;
            }
        }

        public static String OPCION_TODAS_CRITICIDADES() {
            return tr("Todas las Severidades", "All Severities");
        }

        public static String[] OPCIONES_FILTRO_SEVERIDAD() {
            String[] severidades = OPCIONES_SEVERIDAD();
            String[] opciones = new String[severidades.length + 1];
            opciones[0] = OPCION_TODAS_CRITICIDADES();
            System.arraycopy(severidades, 0, opciones, 1, severidades.length);
            return opciones;
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
            return tr("Guardar automáticamente en Issues", "Automatically save to Issues");
        }

        public static String CHECK_GUARDAR_ISSUES_SOLO_PRO() {
            return tr(
                    "Guardar automáticamente en Issues (solo Burp Professional)",
                    "Automatically save to Issues (Burp Professional only)");
        }

        public static String MSG_SIN_HALLAZGOS_LIMPIAR() {
            return tr("No hay hallazgos para limpiar.", "There are no findings to clear.");
        }

        public static String TITULO_INFORMACION() {
            return tr("Información", "Information");
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
            return tr("Hora,URL,Hallazgo,Descripción,Severidad,Confianza",
                    "Time,URL,Finding,Description,Severity,Confidence");
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
            return tr("Exportación exitosa", "Export successful");
        }

        public static String MSG_ERROR_EXPORTAR(String error) {
            return trf("Error al exportar: %s", "Export error: %s", error);
        }

        public static String TITULO_ERROR_EXPORTACION() {
            return tr("Error de exportación", "Export error");
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
                    "📌 Send to Burp Issues (Pro only)");
        }

        public static String MENU_IGNORAR() {
            return tr("🚫 Ignorar", "🚫 Ignore");
        }

        public static String MENU_BORRAR() {
            return tr("🗑️ Borrar", "🗑️ Delete");
        }

        public static String MENU_AGREGAR_HALLAZGO() {
            return tr("➕ Agregar hallazgo", "➕ Add finding");
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
                    "Esta acción solo está disponible en Burp Suite Professional.",
                    "This action is only available in Burp Suite Professional.");
        }

        public static String MSG_SIN_REQUEST() {
            return tr("(sin request original)", "(missing original request)");
        }

        public static String MSG_SIN_REQUEST_ORIGINAL(int total) {
            return trf("%nSin request original: %d", "%nMissing original request: %d", total);
        }

        public static String MSG_IGNORADOS_OMITIDOS(int total) {
            return trf("%nIgnorados omitidos: %d", "%nIgnored skipped: %d", total);
        }

        public static String MSG_RESULTADO_ACCION(String resumen, int exitosos, int total, int sinRequest,
                int ignorados, String detalle) {
            String texto = resumen + ": " + exitosos + "/" + total;
            if (sinRequest > 0) {
                texto += MSG_SIN_REQUEST_ORIGINAL(sinRequest);
            }
            if (ignorados > 0) {
                texto += MSG_IGNORADOS_OMITIDOS(ignorados);
            }
            return texto + "\n\n" + detalle;
        }

        public static String ETIQUETA_ESTADO_EXITO_ALERTA() {
            return "[OK]";
        }

        public static String ETIQUETA_ESTADO_ADVERTENCIA_ALERTA() {
            return tr("[AVISO]", "[WARN]");
        }

        public static String ETIQUETA_ESTADO_ERROR_ALERTA() {
            return tr("[ERROR]", "[ERROR]");
        }

        public static String LINEA_ESTADO_EXITO_ALERTA(String texto) {
            return construirLineaEstadoAlerta(ETIQUETA_ESTADO_EXITO_ALERTA(), texto);
        }

        public static String LINEA_ESTADO_ADVERTENCIA_ALERTA(String texto) {
            return construirLineaEstadoAlerta(ETIQUETA_ESTADO_ADVERTENCIA_ALERTA(), texto);
        }

        public static String LINEA_ESTADO_ERROR_ALERTA(String texto) {
            return construirLineaEstadoAlerta(ETIQUETA_ESTADO_ERROR_ALERTA(), texto);
        }

        private static String construirLineaEstadoAlerta(String etiqueta, String texto) {
            String contenido = texto != null ? texto : "";
            return contenido.isBlank() ? etiqueta : etiqueta + " " + contenido;
        }

        public static String MSG_ACCION_SOLO_IGNORADOS(int total) {
            return trf(
                    "No hay hallazgos operables: los %d seleccionados están marcados como ignorados.",
                    "No actionable findings: the %d selected are marked as ignored.",
                    total);
        }

        public static String MSG_HALLAZGOS_IGNORADOS(int total) {
            return trf("Hallazgos ignorados: %d%n%nNo se incluirán en exportaciones CSV/JSON.",
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

        public static String DETALLE_ISSUE() {
            return tr(
                    "Revisa la solicitud para confirmar la vulnerabilidad. Este hallazgo se guarda automáticamente para que no se pierda al cerrar Burp, pero requiere validación manual. Haz clic derecho en la pestaña de hallazgos para enviar la petición al Repeater o al Intruder. Nunca confíes ciegamente en los resultados de una IA.",
                    "Review the request to confirm the vulnerability. This finding is saved automatically so it is not lost when Burp closes, but it requires manual validation. Right-click on the findings tab to send the request to Repeater or Intruder. Never blindly trust AI results.");
        }

        public static String REMEDIACION_ISSUE() {
            return tr(
                    "Revisa los encabezados y el cuerpo de la solicitud HTTP para confirmar la vulnerabilidad. Este hallazgo se guarda automáticamente para que no se pierda al cerrar Burp, pero requiere validación manual. Haz clic derecho en la pestaña de hallazgos para enviar la petición al Repeater o al Intruder, y nunca confíes ciegamente en los resultados de una IA.",
                    "Review HTTP request headers and body to confirm the vulnerability. This finding is saved automatically so it is not lost when Burp closes, but it requires manual validation. Right-click on the findings tab to send the request to Repeater or Intruder, and never blindly trust AI results.");
        }

        public static String MENU_ENVIAR_AGENTE_ROCKET() {
            return tr("🚀 Enviar al Agente", "🚀 Send to Agent");
        }

        public static String MENU_ENVIAR_AGENTE_ROCKET(String agente) {
            return trf("🚀 Enviar a %s", "🚀 Send to %s", agente);
        }

        public static String TITULO_ACCION_AGENTE() {
            return tr("Enviar al Agente", "Send to Agent");
        }

        public static String RESUMEN_ACCION_AGENTE(String agente) {
            return trf("Enviados a %s", "Sent to %s", agente);
        }

        public static String MSG_ENVIADOS_AGENTE(int total, String agente) {
            return trf("Se enviaron %d hallazgo(s) a %s.",
                    "Sent %d finding(s) to %s.",
                    total, agente);
        }

        public static String SUFIJO_ENVIADO_INTRUDER() {
            return tr("(enviado a Intruder)", "(sent to Intruder)");
        }

        public static String SUFIJO_ENVIADO_SCANNER() {
            return tr("(enviado a Scanner Pro)", "(sent to Scanner Pro)");
        }

        public static String SUFIJO_ENVIADO_AGENTE(String agente) {
            return trf("(enviado a %s)", "(sent to %s)", agente);
        }

        public static String ERROR_ENVIO_AGENTE(String agente) {
            return trf("No se pudo enviar a %s", "Could not send to %s", agente);
        }

        public static String ERROR_SCANNER_SOLO_PRO() {
            return tr("Scanner solo disponible en Burp Professional", "Scanner only available in Burp Professional");
        }

        public static String URL_DESCONOCIDA() {
            return tr("URL desconocida", "Unknown URL");
        }

        public static String SUFIJO_ERROR_INLINE() {
            return tr(" (error: ", " (error: ");
        }

        public static String LOG_ERROR_ACCION_SOLICITUD() {
            return tr("Error al ejecutar acción sobre la solicitud: ", "Error executing action on request: ");
        }

        public static String LOG_ERROR_ENVIAR_SCANNER() {
            return tr("[BurpIA] Error al enviar a Scanner Pro: ", "[BurpIA] Error sending to Scanner Pro: ");
        }

        public static String ERROR_PANEL_CERRANDO() {
            return tr("No se pudo ejecutar la acción: el panel se está cerrando.",
                    "Action could not be executed: the panel is shutting down.");
        }

        public static String TITULO_OPERACION_LOTE() {
            return tr("Selección de Operación en Lote", "Batch Operation Selection");
        }

        public static String LOG_PETICION_ENVIADA_SCANNER() {
            return tr("[BurpIA] Petición enviada a Scanner Pro: ", "[BurpIA] Request sent to Scanner Pro: ");
        }
    }

    public static final class Consola {
        private Consola() {
        }

        public static String TITULO_ERROR_PTY() {
            return tr("Error de PTY", "PTY Error");
        }

        public static String NOTA_SCOPE_ANALISIS() {
            return tr(
                    "NOTA: BurpIA solo analiza tráfico DENTRO del Scope de Burp Suite.",
                    "NOTE: BurpIA only analyzes traffic INSIDE Burp Suite Scope.");
        }

        public static String NOTA_SCOPE_ANALISIS_ACCION() {
            return tr(
                    "ACCIÓN: Si no ves análisis, agrega el objetivo en Target > Scope.",
                    "ACTION: If you do not see analysis, add the target in Target > Scope.");
        }

        public static String ANALISIS_BLOQUEADO_CONFIG(String razon, String origen, String url) {
            return trf(
                    "ANÁLISIS BLOQUEADO: %s. ACCIÓN: abre BurpIA > Configuración, completa Proveedor/URL/Modelo/API Key, guarda y ejecuta Probar Conexión. Origen=%s, URL=%s",
                    "ANALYSIS BLOCKED: %s. ACTION: open BurpIA > Settings, complete Provider/URL/Model/API Key, save, and run Test Connection. Origin=%s, URL=%s",
                    razon, origen, url);
        }

        public static String TAREA_BLOQUEADA_CONFIG_LLM() {
            return tr("Bloqueada por configuración LLM", "Blocked by LLM configuration");
        }

        public static String ESTADO_INICIAL_LLM_LISTO(String proveedor, String modelo) {
            return trf(
                    "Estado LLM al inicio: listo para analizar (Proveedor=%s, Modelo=%s)",
                    "LLM startup status: ready to analyze (Provider=%s, Model=%s)",
                    proveedor, modelo);
        }

        public static String ESTADO_INICIAL_LLM_BLOQUEADO(String razon) {
            return trf(
                    "Estado LLM al inicio: no listo (%s). ACCIÓN: abre BurpIA > Configuración, completa Proveedor/URL/Modelo/API Key, guarda y ejecuta Probar Conexión.",
                    "LLM startup status: not ready (%s). ACTION: open BurpIA > Settings, complete Provider/URL/Model/API Key, save, and run Test Connection.",
                    razon);
        }

        public static String ESTADO_INICIAL_LLM_BLOQUEADO_CABECERA(String razon) {
            return trf(
                    "Estado LLM al inicio: no listo (%s).",
                    "LLM startup status: not ready (%s).",
                    razon);
        }

        public static String ESTADO_INICIAL_LLM_BLOQUEADO_ACCION() {
            return tr(
                    "ACCIÓN: abre BurpIA > Configuración, completa Proveedor/URL/Modelo/API Key, guarda y ejecuta Probar Conexión.",
                    "ACTION: open BurpIA > Settings, complete Provider/URL/Model/API Key, save, and run Test Connection.");
        }

        public static String TITULO_CONTROLES() {
            return tr("⚙️ CONTROLES DE CONSOLA", "⚙️ CONSOLE CONTROLS");
        }

        public static String LOG_MANEJADOR_INICIALIZADO(int concurrente, int retraso, boolean detallado) {
            return trf("ManejadorHttpBurpIA inicializado (maximoConcurrente=%d, retraso=%ds, detallado=%s)",
                    "ManejadorHttpBurpIA initialized (maxConcurrent=%d, delay=%ds, verbose=%s)",
                    concurrente, retraso, detallado);
        }

        public static String LOG_CONFIGURACION_ACTUALIZADA(int concurrente, int retraso, String modelo, int timeout) {
            return trf("Configuración actualizada: maximoConcurrente=%d, retraso=%ds, timeout(%s)=%ds",
                    "Configuration updated: maxConcurrent=%d, delay=%ds, timeout(%s)=%ds",
                    concurrente, retraso, modelo, timeout);
        }

        public static String RESUMEN_LOGS(int total, int info, int verbose, int err) {
            return RESUMEN_LOGS_BASE(total, info, verbose, err);
        }

        private static String RESUMEN_LOGS_BASE(int total, int info, int verbose, int err) {
            return trf("Total: %d | Info: %d | Verbose: %d | Errores: %d",
                    "Total: %d | Info: %d | Verbose: %d | Errors: %d",
                    total, info, verbose, err);
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
            return "📊 " + RESUMEN_LOGS_BASE(total, info, detallados, errores);
        }

        public static String MSG_CONSOLA_VACIA() {
            return tr("La consola ya está vacía.", "The console is already empty.");
        }

        public static String TITULO_INFORMACION() {
            return tr("Información", "Information");
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

        public static String BOTON_REINICIAR() {
            return tr("Reiniciar", "Restart");
        }

        public static String BOTON_CTRL_C() {
            return tr("Ctrl+C", "Ctrl+C");
        }

        public static String ETIQUETA_DELAY() {
            return tr("⏱️ Espera MCP (ms):", "⏱️ MCP Wait (ms):");
        }

        public static String BOTON_INYECTAR_PAYLOAD() {
            return tr("Inyectar Payload", "Inject Payload");
        }

        public static String BOTON_CAMBIAR_AGENTE(String nombreAgente) {
            return trf("Activar %s", "Enable %s", nombreAgente);
        }

        public static String BOTON_CAMBIAR_AGENTE_GENERICO() {
            return tr("Cambiar Agente", "Switch Agent");
        }

        public static String MSG_URL_GUIA_AGENTE(String url) {
            return trfBloqueUrl(
                    "No se pudo abrir el navegador.\nURL de la guía:",
                    "Could not open browser.\nGuide URL:",
                    url);
        }

        public static String TITULO_PANEL_AGENTE_GENERICO() {
            return tr("🖥️ CONSOLA DEL AGENTE", "🖥️ AGENT CONSOLE");
        }

        public static String TAG_RASTREO() {
            return tr(" [RASTREO]", " [TRACE]");
        }
    }

    public static final class Conexion {
        private Conexion() {
        }

        public static String ERRORES_CONFIGURACION() {
            return tr("Errores de configuración:\n", "Configuration errors:\n");
        }

        public static String SIN_RESPUESTA() {
            return tr("No se recibió respuesta del servidor", "No response was received from server");
        }

        public static String ERROR_CONEXION() {
            return tr("Error de conexión: ", "Connection error: ");
        }

        public static String ERROR_PRUEBA_CONEXION(String errorMsg) {
            return trf("Error durante la prueba de conexión:%n%n%s", "Error during connection test:%n%n%s", errorMsg);
        }

        public static String EXITO_CONEXION() {
            return tr("✅ Conexión exitosa a ", "✅ Successful connection to ");
        }

        public static String INFO_CONFIGURACION() {
            return tr("📋 Configuración:\n", "📋 Configuration:\n");
        }

        public static String INFO_MODELO() {
            return tr("   Modelo: ", "   Model: ");
        }

        public static String INFO_URL_BASE() {
            return tr("   URL base: ", "   Base URL: ");
        }

        public static String INFO_ENDPOINT() {
            return tr("   Endpoint probado: ", "   Tested endpoint: ");
        }

        public static String MSG_ENVIADO() {
            return tr("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n",
                    "💬 Sent message: \"Reply exactly with OK\"\n\n");
        }

        public static String RESPUESTA_MODELO() {
            return tr("✅ Respuesta del modelo:\n", "✅ Model response:\n");
        }

        public static String RESPUESTA_CORRECTA() {
            return tr("\n\n✅ ¡El modelo respondió correctamente!", "\n\n✅ Model responded correctly!");
        }

        public static String RESPUESTA_ACEPTADA() {
            return tr("\n(Respuesta aceptada: contiene \"OK\" o \"Hola\")",
                    "\n(Accepted response: contains \"OK\" or \"Hello\")");
        }

        public static String PROVEEDOR_RESPONDIO() {
            return tr("✅ El proveedor respondió y el contenido fue extraído correctamente.\n",
                    "✅ Provider responded and content was extracted successfully.\n");
        }

        public static String CONEXION_VALIDA_SIN_OK() {
            return tr("ℹ️ La respuesta no incluyó literalmente \"OK\", pero la conexión es valida.\n\n",
                    "ℹ️ Response did not include literal \"OK\", but connection is valid.\n\n");
        }

        public static String MODELO_RESPONDE() {
            return tr("Respuesta del modelo:\n", "Model response:\n");
        }

        public static String RESPUESTA_SIN_OK() {
            return tr("⚠️ La respuesta NO contiene \"OK\" ni \"Hola\"\n\n",
                    "⚠️ Response does NOT contain \"OK\" or \"Hello\"\n\n");
        }

        public static String RESPUESTA_RECIBIDA() {
            return tr("❌ Respuesta recibida:\n", "❌ Received response:\n");
        }

        public static String RESPUESTA_FORMATO_INCORRECTO() {
            return tr("\n\n⚠️ El modelo respondió pero no cumple el formato esperado.",
                    "\n\n⚠️ Model responded but did not match the expected format.");
        }

        public static String ERROR_EXTRAER_CONTENIDO() {
            return tr("⚠️ No se pudo extraer el contenido de la respuesta\n",
                    "⚠️ Could not extract response content\n");
        }

        public static String EXITO_FORMATO_INCORRECTO() {
            return tr("   La conexión fue exitosa pero el formato de respuesta no es el esperado.\n",
                    "   Connection succeeded but response format is not as expected.\n");
        }

        public static String RESPUESTA_CRUDA() {
            return tr("   Respuesta cruda (primeros 200 caracteres):\n", "   Raw response (first 200 characters):\n");
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
            return tr("Descripción:", "Description:");
        }

        public static String BOTON_GUARDAR() {
            return tr("💾 Guardar Cambios", "💾 Save Changes");
        }

        public static String BOTON_CANCELAR() {
            return tr("❌ Cancelar", "❌ Cancel");
        }

        public static String MSG_VALIDACION() {
            return tr("La URL y la descripción no pueden estar vacías.",
                    "URL and description cannot be empty.");
        }

        public static String TITULO_ERROR_VALIDACION() {
            return tr("Error de validación", "Validation error");
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

        public static String TAB_AJUSTES_USUARIO() {
            return tr("⚙️ Ajustes de Usuario", "⚙️ User Settings");
        }

        public static String TAB_PROMPT() {
            return tr("📝 Prompt", "📝 Prompt");
        }

        public static String TAB_ACERCA() {
            return tr("ℹ️ Acerca de", "ℹ️ About");
        }

        public static String TAB_AGENTES() {
            return tr("🤖 Agentes", "🤖 Agents");
        }

        public static String TITULO_MULTI_PROVEEDOR() {
            return tr("🔗 Consultas Multi-Proveedor", "🔗 Multi-Provider Queries");
        }

        public static String BOTON_HABILITAR() {
            return tr("Habilitar", "Enable");
        }

        public static String LABEL_PRONTO() {
            return tr("Pronto", "Soon");
        }

        public static String LABEL_SELECCIONAR_AGENTE() {
            return tr("Seleccionar Agente:", "Select Agent:");
        }

        public static String LABEL_TIMEOUT_MODELO() {
            return tr("Timeout de Modelo (s):", "Model Timeout (s):");
        }

        public static String BOTON_GUARDAR() {
            return tr("💾 Guardar Ajustes", "💾 Save Settings");
        }

        public static String BOTON_GUARDANDO() {
            return tr("💾 Guardando...", "💾 Saving...");
        }

        public static String BOTON_PROBAR_CONEXION() {
            return tr("🔍 Probar Conexión", "🔍 Test Connection");
        }

        public static String BOTON_PROBANDO() {
            return tr("🔍 Probando...", "🔍 Testing...");
        }

        public static String LABEL_IGNORAR_SSL() {
            return tr("Ignorar errores de certificado SSL (No recomendado)",
                    "Ignore SSL certificate errors (Not recommended)");
        }

        public static String LABEL_SEGURIDAD_SSL() {
            return tr("Seguridad SSL:", "SSL Security:");
        }

        public static String LABEL_FILTRO_HERRAMIENTAS() {
            return tr("Filtro de Herramientas:", "Tool Filtering:");
        }

        public static String LABEL_SOLO_PROXY() {
            return tr("Capturar solo peticiones del Proxy (Navegador)", "Capture only Proxy requests (Browser)");
        }

        public static String LABEL_HABILITAR_ALERTAS() {
            return tr("Mostrar alertas y notificaciones:", "Show alerts and notifications:");
        }

        public static String LABEL_ALERTAS() {
            return tr("Alertas:", "Alerts:");
        }

        public static String TITULO_PERSISTENCIA_UI() {
            return tr("💾 PERSISTENCIA DE ESTADO UI", "💾 UI STATE PERSISTENCE");
        }

        public static String CHECK_PERSISTIR_FILTRO_BUSQUEDA() {
            return tr("Persistir filtro de búsqueda", "Persist search filter");
        }

        public static String CHECK_PERSISTIR_FILTRO_SEVERIDAD() {
            return tr("Persistir filtro de severidad", "Persist severity filter");
        }

        public static String MSG_CORRIGE_CAMPOS() {
            return tr("Corrige los siguientes campos:\n", "Please fix the following fields:\n");
        }

        public static String ERROR_API_SIN_MODELOS(String proveedorSeleccionado) {
            return tr("La API no devolvió modelos para " + proveedorSeleccionado,
                    "The API returned no models for " + proveedorSeleccionado);
        }

        public static String ERROR_OBTENER_GEMINI() {
            return tr("No se pudieron obtener modelos Gemini: ", "Could not retrieve Gemini models: ");
        }

        public static String ERROR_OBTENER_OLLAMA() {
            return tr("No se pudieron obtener modelos Ollama: ", "Could not retrieve Ollama models: ");
        }

        public static String ERROR_OBTENER_API() {
            return tr("No se pudieron obtener modelos de la API: ", "Could not retrieve models from API: ");
        }

        public static String ERROR_PROVEEDOR_SIN_LISTA() {
            return tr(
                    "Este proveedor no expone una lista automatica de modelos. Escribe el modelo manualmente en el campo Modelo.",
                    "This provider does not expose an automatic model list. Enter the model manually in the Model field.");
        }

        public static String ERROR_PROVEEDOR_REQUERIDO() {
            return tr("Proveedor de AI es requerido", "AI provider is required");
        }

        public static String ERROR_MODELO_REQUERIDO() {
            return tr("Modelo es requerido", "Model is required");
        }

        public static String ERROR_PROMPT_REQUERIDO() {
            return tr("Prompt configurable es requerido", "Configurable prompt is required");
        }

        public static String ERROR_CONCURRENCIA() {
            return tr("Concurrencia debe ser entre 1 y 10", "Concurrency must be between 1 and 10");
        }

        public static String ERROR_TEMA_INVALIDO() {
            return tr("Tema debe ser 'Light' o 'Dark'", "Theme must be 'Light' or 'Dark'");
        }

        public static String SIN_DETALLE_ERROR() {
            return tr("Sin detalle de error", "No error details available");
        }

        public static String ALERTA_PROVEEDOR_INVALIDO() {
            return tr("ALERTA: Proveedor de AI no configurado o no valido",
                    "ALERT: AI provider is not configured or is invalid");
        }

        public static String ALERTA_URL_VACIA() {
            return tr("ALERTA: URL de API no configurada", "ALERT: API URL is not configured");
        }

        public static String ERROR_PROVEEDOR_NO_RECONOCIDO(String proveedor) {
            return trf("Proveedor de AI no reconocido: %s", "Unknown AI provider: %s", proveedor);
        }

        public static String ERROR_RETRASO_RANGO(int min, int max) {
            return trf("Retraso debe estar entre %d y %d segundos", "Delay must be between %d and %d seconds", min,
                    max);
        }

        public static String ERROR_MAXIMO_CONCURRENTE_RANGO(int min, int max) {
            return trf("Máximo concurrente debe estar entre %d y %d", "Max concurrent must be between %d and %d", min,
                    max);
        }

        public static String ERROR_MAXIMO_HALLAZGOS_TABLA_RANGO(int min, int max) {
            return trf("Máximo de hallazgos en tabla debe estar entre %d y %d",
                    "Max findings in table must be between %d and %d", min, max);
        }

        public static String ALERTA_MODELO_NO_CONFIGURADO(String proveedor) {
            return trf("ALERTA: Modelo no configurado para %s", "ALERT: Model is not configured for %s", proveedor);
        }

        public static String ALERTA_CLAVE_REQUERIDA(String proveedor) {
            return trf("ALERTA: Clave de API requerida para %s", "ALERT: API key is required for %s", proveedor);
        }

        public static String PROMPT_POR_DEFECTO() {
            return tr(
                    "Analiza la siguiente petición y respuesta HTTP:\n\nSOLICITUD:\n{REQUEST}\n\nRESPUESTA:\n{RESPONSE}",
                    "Analyze the following HTTP request and response:\n\nREQUEST:\n{REQUEST}\n\nRESPONSE:\n{RESPONSE}");
        }

        public static String ALERTA_CLAVE_VACIA() {
            return tr("ALERTA: Clave API no configurada", "ALERT: API key is not configured");
        }

        public static String ERROR_TIMEOUT_RANGO() {
            return tr("Tiempo de espera debe estar entre 10 y 300 segundos",
                    "Timeout must be between 10 and 300 seconds");
        }

        public static String MSG_CONFIGURACION_NULA() {
            return tr("Configuración nula", "Null configuration");
        }

        public static String MSG_DIRECTORIO_NO_EXISTE() {
            return tr("Directorio no existe: ", "Directory does not exist: ");
        }

        public static String MSG_DIRECTORIO_NO_ESCRIBIBLE() {
            return tr("Directorio no escribible: ", "Directory is not writable: ");
        }

        public static String MSG_ERROR_IO() {
            return tr("Error de E/S: ", "I/O error: ");
        }

        public static String MSG_ERROR_INESPERADO() {
            return tr("Error inesperado: ", "Unexpected error: ");
        }

        public static String TITULO_AJUSTES_USUARIO() {
            return tr("⚙️ AJUSTES DE USUARIO", "⚙️ USER SETTINGS");
        }

        public static String LABEL_IDIOMA() {
            return tr("Idioma:", "Language:");
        }

        public static String TITULO_FUENTES() {
            return tr("🔡 AJUSTES DE FUENTES", "🔡 FONT SETTINGS");
        }

        public static String BOTON_RESTAURAR_FUENTES() {
            return tr("🔄 Restaurar por Defecto", "🔄 Restore Defaults");
        }

        public static String MSG_CONFIRMAR_RESTAURAR_FUENTES() {
            return tr("¿Estás seguro de que deseas restaurar las fuentes a sus valores por defecto?",
                    "Are you sure you want to restore fonts to their default values?");
        }

        public static String LABEL_FUENTE_ESTANDAR() {
            return tr("Fuente Interfaz:", "Interface Font:");
        }

        public static String LABEL_FUENTE_MONO() {
            return tr("Fuente Monospace:", "Monospace Font:");
        }

        public static String LABEL_TAMANIO_FUENTE() {
            return tr("Tamaño:", "Size:");
        }

        public static String LABEL_RETRASO() {
            return tr("Retraso (seg):", "Delay (sec):");
        }

        public static String LABEL_MAXIMO_CONCURRENTE() {
            return tr("Máximo Concurrente:", "Max Concurrent:");
        }

        public static String LABEL_MAX_HALLAZGOS_TABLA() {
            return tr("Max Hallazgos Tabla:", "Max Findings Table:");
        }

        public static String LABEL_MODO_DETALLADO() {
            return tr("Modo Detallado:", "Verbose Mode:");
        }

        public static String CHECK_DETALLADO() {
            return tr("Activar registro detallado (recomendado para depuración)",
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

        public static String OPCION_MODELO_CUSTOM() {
            return tr("-- Personalizado --", "-- Custom --");
        }

        public static String LABEL_MAX_TOKENS() {
            return tr("Máximo de Tokens:", "Max Tokens:");
        }

        public static String TITULO_INSTRUCCIONES() {
            return tr("📝 INSTRUCCIONES", "📝 INSTRUCTIONS");
        }

        public static String TEXTO_INSTRUCCIONES() {
            return tr(
                    "• El token {REQUEST} se reemplaza automáticamente con la solicitud HTTP analizada.\n" +
                            "• El token {RESPONSE} se reemplaza automáticamente con la respuesta HTTP capturada.\n" +
                            "• El token {OUTPUT_LANGUAGE} se reemplaza con el idioma configurado por el usuario.\n" +
                            "• Incluye {REQUEST}, {RESPONSE} y {OUTPUT_LANGUAGE} donde quieras insertar la evidencia.\n"
                            +
                            "• El prompt debe pedir respuesta JSON estricta con este formato:",
                    "• The {REQUEST} token is automatically replaced with the analyzed HTTP request.\n" +
                            "• The {RESPONSE} token is automatically replaced with the captured HTTP response.\n" +
                            "• The {OUTPUT_LANGUAGE} token is replaced with the user-configured language.\n" +
                            "• Include {REQUEST}, {RESPONSE}, and {OUTPUT_LANGUAGE} exactly where you want the evidence inserted.\n"
                            +
                            "• The prompt must request strict JSON output in this format:");
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

        public static String ICONO_APP() {
            return "🧠";
        }

        public static String SUBTITULO_APP() {
            return tr("Análisis de Seguridad con Inteligencia Artificial",
                    "Security Analysis with Artificial Intelligence");
        }

        public static String TITULO_RESUMEN(String version) {
            return "BurpIA (" + version + ")";
        }

        private static final String INTRO_DESCRIPCION_ES = "BurpIA es una extensión profesional para Burp Suite que aprovecha diferentes "
                +
                "modelos de Inteligencia Artificial para analizar tráfico HTTP e identificar " +
                "vulnerabilidades de seguridad de forma automatizada con validación manual asistida por agentes.";

        private static final String INTRO_DESCRIPCION_EN = "BurpIA is a professional Burp Suite extension that leverages different "
                +
                "Artificial Intelligence models to analyze HTTP traffic and identify " +
                "security vulnerabilities automatically with agentic manual validation.";

        private static final String[] CARACTERISTICAS_APP_ES = {
                "Detección de forma pasiva de problemas de seguridad con IA Generativa",
                "IA basada en agentes integrada en la interfaz del plugin",
                "Compatibilidad con OpenAI, Claude, Gemini, Z.ai, Minimax y Ollama",
                "De-duplicación inteligente de peticiones para optimizar la cuota de API",
                "Gestión asíncrona mediante colas de tareas paralelizables",
                "Integración con site map (Issues), Repeater, Intruder y Scanner Pro",
                "Prompt totalmente configurable para análisis a medida",
                "Exportación nativa de reportes de hallazgos a CSV y JSON",
                "Multiidioma en inglés y español."
        };

        private static final String[] CARACTERISTICAS_APP_EN = {
                "Passive detection of security issues with Generative AI",
                "Agentic AI integrated into the plugin interface",
                "Compatibility with OpenAI, Claude, Gemini, Z.ai, Minimax, and Ollama",
                "Smart request deduplication to optimize API quota",
                "Asynchronous task management through parallel queues",
                "Integration with site map (Issues), Repeater, Intruder, and Scanner Pro",
                "Fully configurable prompt for tailored analysis",
                "Native findings export to CSV and JSON",
                "Bilingual support in English and Spanish."
        };

        public static String DESCRIPCION_APP() {
            return tr(
                    construirDescripcionApp(INTRO_DESCRIPCION_ES, "Características principales",
                            CARACTERISTICAS_APP_ES),
                    construirDescripcionApp(INTRO_DESCRIPCION_EN, "Key features", CARACTERISTICAS_APP_EN));
        }

        private static String construirDescripcionApp(String introduccion, String titulo, String[] caracteristicas) {
            StringBuilder descripcion = new StringBuilder();
            descripcion.append(introduccion).append("\n\n").append(titulo).append(":\n");
            if (caracteristicas != null) {
                for (String caracteristica : caracteristicas) {
                    if (caracteristica == null || caracteristica.trim().isEmpty()) {
                        continue;
                    }
                    descripcion.append("• ").append(caracteristica).append('\n');
                }
            }
            if (descripcion.length() > 0 && descripcion.charAt(descripcion.length() - 1) == '\n') {
                descripcion.setLength(descripcion.length() - 1);
            }
            return descripcion.toString();
        }

        public static final class Agentes {
            private Agentes() {
            }

            public static String TITULO_AGENTE() {
                return tr("🤖 CONFIGURACION AGENTE", "🤖 AGENT SETTINGS");
            }

            public static String TITULO_EJECUCION_AGENTE() {
                return tr("⚙️ EJECUCION DEL AGENTE", "⚙️ AGENT EXECUTION");
            }

            public static String TITULO_PROMPTS_AGENTE() {
                return tr("🧾 PROMPTS DEL AGENTE", "🧾 AGENT PROMPTS");
            }

            public static String CHECK_HABILITAR_AGENTE() {
                return tr("Habilitar Agente", "Enable Agent");
            }

            public static String CHECK_HABILITAR_AGENTE(String nombreAgente) {
                return trf("Habilitar Agente: %s", "Enable Agent: %s", nombreAgente);
            }

            public static String LABEL_RUTA_BINARIO() {
                return tr("Ruta del Binario:", "Binary Path:");
            }

            public static String TITULO_PROMPT_AGENTE() {
                return tr("✍️ PROMPT DEL AGENTE", "✍️ AGENT PROMPT");
            }

            public static String TITULO_PROMPT_INICIAL_AGENTE() {
                return tr("🚀 PROMPT INICIAL (PRE-FLIGHT)", "🚀 INITIAL PROMPT (PRE-FLIGHT)");
            }

            public static String DESCRIPCION_PROMPT_INICIAL_AGENTE() {
                return tr(
                        "Se inyecta al iniciar/reiniciar/cambiar agente o al pulsar Inyectar Payload.",
                        "Injected on start/restart/agent switch or when clicking Inject Payload.");
            }

            public static String DESCRIPCION_PROMPT_VALIDACION_AGENTE() {
                return tr(
                        "Plantilla usada para validar hallazgos enviados al agente.",
                        "Template used to validate findings sent to the agent.");
            }

            public static String MSG_CONFIGURACION_REQUERIDA() {
                return tr("Configura la ruta del binario en los ajustes de agentes.",
                        "Configure the binary path in the agents settings.");
            }

            public static String TITULO_VALIDACION_AGENTE() {
                return tr("Validación de Agente", "Agent Validation");
            }

            public static String MSG_BINARIO_NO_EXISTE(String nombreAgente, String rutaBinario) {
                return trf("El binario de %s no existe en la ruta actual: %s",
                        "The %s binary does not exist at the current path: %s",
                        nombreAgente, rutaBinario);
            }

            public static String MSG_COMANDO_CONFIGURADO(String comando) {
                return trf("Comando configurado: %s",
                        "Configured command: %s",
                        comando);
            }

            public static String MSG_BINARIO_NO_EXISTE_SIMPLE(String rutaBinario) {
                return trf("El binario del agente no existe en la ruta: %s",
                        "The agent binary does not exist at path: %s",
                        rutaBinario);
            }

            public static String ENLACE_INSTALAR_AGENTE(String nombreAgente) {
                return trf("¿Como instalar y configurar %s?", "How to install and configure %s?", nombreAgente);
            }
        }

        public static String TITULO_DESARROLLADO_POR() {
            return tr("DESARROLLADO POR", "DEVELOPED BY");
        }

        public static String NOMBRE_DESARROLLADOR() {
            return "Jaime Restrepo (@DragonJAR)";
        }

        public static String BOTON_SITIO_WEB() {
            return NOMBRE_DESARROLLADOR();
        }

        public static String TITULO_ACTUALIZACIONES() {
            return tr("ACTUALIZACIONES", "UPDATES");
        }

        public static String DESCRIPCION_ACTUALIZACIONES() {
            return tr(
                    "Comprueba si hay una nueva version disponible comparando con el repositorio oficial.",
                    "Check whether a newer version is available from the official repository.");
        }

        public static String BOTON_BUSCAR_ACTUALIZACIONES() {
            return tr("Buscar actualizaciones", "Check for Updates");
        }

        public static String BOTON_BUSCANDO_ACTUALIZACIONES() {
            return tr("Buscando...", "Checking...");
        }

        public static String VERSION_ACTUAL(String version) {
            return trf("Version actual: %s", "Current version: %s", version);
        }

        public static String VERSION() {
            return VERSION_ACTUAL(com.burpia.util.VersionBurpIA.obtenerVersionActual());
        }

        public static String MSG_ACTUALIZACION_DISPONIBLE(String versionActual, String versionRemota,
                String urlDescarga) {
            return trf(
                    "Hay una nueva version disponible.\nVersion actual: %s\nVersion remota: %s\n\nVisita %s para descargar la ultima version.",
                    "A new version is available.\nCurrent version: %s\nRemote version: %s\n\nVisit %s to download the latest version.",
                    versionActual,
                    versionRemota,
                    urlDescarga);
        }

        public static String MSG_VERSION_AL_DIA(String versionActual) {
            return trf("Ya tienes la ultima version instalada (%s).",
                    "You already have the latest version installed (%s).",
                    versionActual);
        }

        public static String MSG_ERROR_VERIFICAR_ACTUALIZACIONES(String detalle) {
            return trf("No se pudieron verificar actualizaciones: %s",
                    "Could not check for updates: %s",
                    detalle);
        }

        public static String MSG_ERROR_HTTP_VERSION_REMOTA(int codigoHttp) {
            return trf("Error HTTP consultando version remota: %d",
                    "HTTP error while fetching remote version: %d",
                    codigoHttp);
        }

        public static String MSG_VERSION_REMOTA_VACIA() {
            return tr("El archivo remoto VERSION.txt está vacío o no es válido.",
                    "The remote VERSION.txt file is empty or invalid.");
        }

        public static String TITULO_ENLACE() {
            return tr("Enlace", "Link");
        }

        public static String URL_SITIO_WEB() {
            return URL_DRAGONJAR_CONTACTO;
        }

        public static String MSG_URL(String url) {
            return trfBloqueUrl("URL:", "URL:", url);
        }

        public static String MSG_URL() {
            return MSG_URL(URL_SITIO_WEB());
        }

        public static String MSG_ERROR_ABRIR_NAVEGADOR(String detalle) {
            return trf("Error al abrir el navegador: %s",
                    "Error opening browser: %s",
                    detalle);
        }

        public static String TITULO_ERROR() {
            return tr("Error", "Error");
        }

        public static String MSG_CONFIRMAR_RESTAURAR_PROMPT() {
            return tr(
                    "¿Estas seguro de que deseas restaurar el prompt por defecto? Se perderán tus cambios personalizados.",
                    "Are you sure you want to restore the default prompt? Your custom changes will be lost.");
        }

        public static String TITULO_CONFIRMAR_RESTAURACION() {
            return tr("Confirmar Restauración", "Confirm Restore");
        }

        public static String MSG_ERROR_PROMPT_SIN_REQUEST() {
            return tr(
                    "El prompt debe contener el token {REQUEST} para indicar donde se insertará la solicitud HTTP.\n" +
                            "El token {RESPONSE} es opcional pero recomendado para analizar la respuesta del servidor.",
                    "The prompt must include the {REQUEST} token to indicate where the HTTP request will be inserted.\n"
                            +
                            "The {RESPONSE} token is optional but recommended to analyze the server response.");
        }

        public static String TITULO_ERROR_VALIDACION() {
            return tr("Error de Validación", "Validation Error");
        }

        public static String MSG_ERROR_GUARDAR_CONFIG(String detalle, String ruta) {
            return trf("No se pudo guardar la configuración:\n%s\n\nRuta: %s",
                    "Configuration could not be saved:\n%s\n\nPath: %s",
                    detalle, ruta);
        }

        public static String TITULO_ERROR_GUARDAR() {
            return tr("Error al Guardar", "Save Error");
        }

        public static String MSG_ERROR_FORMATO_NUMERO() {
            return tr("Formato de número inválido en ajustes de usuario",
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
            return tr("Validación", "Validation");
        }

        public static String MSG_URL_VACIA() {
            return tr("La URL de API no puede estar vacía.\nPor favor ingresa una URL valida.",
                    "The API URL cannot be empty.\nPlease provide a valid URL.");
        }

        public static String MSG_API_KEY_VACIA(String proveedor) {
            return trf("La clave de API no puede estar vacía para %s.\nPor favor ingresa tu clave de API.",
                    "API key cannot be empty for %s.\nPlease provide your API key.",
                    proveedor);
        }

        public static String MSG_SELECCIONA_MODELO() {
            return tr("Por favor selecciona un modelo",
                    "Please select a model");
        }

        public static String TITULO_CONEXION_EXITOSA() {
            return tr("✅ Conexión Exitosa", "✅ Connection Successful");
        }

        public static String TITULO_ERROR_CONEXION() {
            return tr("❌ Error de Conexión", "❌ Connection Error");
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
            return tr("⚡️ Analizar Solicitud con BurpIA", "⚡️ Analyze Request with BurpIA");
        }

        public static String MENU_ENVIAR_AGENTE() {
            return tr("🤖 Enviar al Agente", "🤖 Send to Agent");
        }

        public static String MENU_ENVIAR_AGENTE(String agente) {
            return trf("🤖 Enviar a %s", "🤖 Send to %s", agente);
        }

        public static String LOG_DEBOUNCE_IGNORADO() {
            return tr("[BurpIA] Debounce: ignorando clic duplicado", "[BurpIA] Debounce: duplicate click ignored");
        }

        public static String LOG_ANALISIS_FORZADO() {
            return tr("[BurpIA] Analizando solicitud desde menu contextual (forzado)",
                    "[BurpIA] Analyzing request from context menu (forced)");
        }

        public static String MSG_ANALISIS_INICIADO() {
            return tr("Solicitud enviada para análisis forzado.\n" +
                    "Esto puede tomar unos segundos dependiendo de la respuesta de la AI.",
                    "Request sent for forced analysis.\n" +
                            "This may take a few seconds depending on AI response time.");
        }

        public static String MSG_ANALISIS_INICIADO_RESULTADO(int iniciadas, int total, int omitidas) {
            if (total <= 1 && iniciadas > 0 && omitidas == 0) {
                return MSG_ANALISIS_INICIADO();
            }
            if (iniciadas <= 0) {
                return trf("No se pudo enviar ninguna solicitud para análisis forzado. Seleccionadas: %d.",
                        "Could not send any requests for forced analysis. Selected: %d.",
                        total);
            }
            if (omitidas > 0) {
                return trf("Solicitudes enviadas para análisis forzado: %d/%d (omitidas: %d).",
                        "Requests sent for forced analysis: %d/%d (skipped: %d).",
                        iniciadas, total, omitidas);
            }
            return trf("Solicitudes enviadas para análisis forzado: %d/%d.",
                    "Requests sent for forced analysis: %d/%d.",
                    iniciadas, total);
        }

        public static String TITULO_ANALISIS_INICIADO() {
            return tr("BurpIA - Análisis Iniciado", "BurpIA - Analysis Started");
        }

        public static String TITULO_ENVIO_AGENTE() {
            return tr("BurpIA - Envío al Agente", "BurpIA - Sent to Agent");
        }

        public static String MSG_ENVIO_AGENTE(String agente) {
            return trf("Solicitud enviada a %s.",
                    "Request sent to %s.",
                    agente);
        }

        public static String MSG_ENVIO_AGENTE_RESULTADO(String agente, int exitosas, int total, int fallidas) {
            if (total <= 1 && exitosas > 0 && fallidas == 0) {
                return MSG_ENVIO_AGENTE(agente);
            }
            if (exitosas <= 0) {
                return trf("No se pudo enviar ninguna solicitud a %s. Seleccionadas: %d.",
                        "Could not send any requests to %s. Selected: %d.",
                        agente, total);
            }
            if (fallidas > 0) {
                return trf("Solicitudes enviadas a %s: %d/%d (fallidas: %d).",
                        "Requests sent to %s: %d/%d (failed: %d).",
                        agente, exitosas, total, fallidas);
            }
            return trf("Solicitudes enviadas a %s: %d/%d.",
                    "Requests sent to %s: %d/%d.",
                    agente, exitosas, total);
        }
    }

    public static final class Tablas {
        private Tablas() {
        }

        public static String[] COLUMNAS_TAREAS() {
            return new String[] {
                    tr("Tipo", "Type"),
                    tr("URL", "URL"),
                    tr("Estado", "Status"),
                    tr("Duración", "Duration")
            };
        }

        public static String[] COLUMNAS_HALLAZGOS() {
            return new String[] {
                    tr("Hora", "Time"),
                    tr("URL", "URL"),
                    tr("Hallazgo", "Finding"),
                    tr("Severidad", "Severity"),
                    tr("Confianza", "Confidence")
            };
        }
    }

    public static final class Tooltips {
        private Tooltips() {
        }

        public static final class Pestanias {
            private Pestanias() {
            }

            public static String TAREAS() {
                return I18nUI.tr("Monitoriza la cola de análisis y controla su ejecución.",
                        "Monitor the analysis queue and control execution.");
            }

            public static String HALLAZGOS() {
                return I18nUI.tr("Revisa, filtra y exporta hallazgos detectados por BurpIA.",
                        "Review, filter, and export BurpIA findings.");
            }

            public static String CONSOLA() {
                return I18nUI.tr("Consulta logs operativos y de diagnóstico del complemento.",
                        "Review extension operational and diagnostic logs.");
            }

            public static String AGENTE() {
                return I18nUI.tr("Terminal interactiva del agente (pentesting asistido por IA).",
                        "Interactive terminal for agent (AI-assisted pentesting).");
            }
        }

        public static final class Estadisticas {
            private Estadisticas() {
            }

            public static String RESUMEN_TOTAL() {
                return I18nUI.tr("Total acumulado de hallazgos registrados.",
                        "Total number of recorded findings.");
            }

            public static String RESUMEN_SEVERIDAD() {
                return I18nUI.tr("Distribución de hallazgos por nivel de severidad.",
                        "Distribution of findings by severity.");
            }

            public static String LIMITE_HALLAZGOS() {
                return I18nUI.tr("Límite máximo de hallazgos conservados en tabla/memoria.",
                        "Maximum findings kept in table/memory.");
            }

            public static String RESUMEN_OPERATIVO() {
                return I18nUI.tr("Solicitudes recibidas, analizadas, omitidas y errores.",
                        "Requests received, analyzed, skipped and errored.");
            }

            public static String CONFIGURACION() {
                return I18nUI.tr("Abre configuración de proveedor, prompt y límites.",
                        "Open provider, prompt and limits settings.");
            }

            public static String CAPTURA_PAUSAR() {
                return I18nUI.tr("Pausa el análisis pasivo de nuevas respuestas HTTP.",
                        "Pause passive analysis of new HTTP responses.");
            }

            public static String CAPTURA_REANUDAR() {
                return I18nUI.tr("Reanuda el análisis pasivo de respuestas HTTP.",
                        "Resume passive analysis of HTTP responses.");
            }
        }

        public static final class Tareas {
            private Tareas() {
            }

            public static String PAUSAR_REANUDAR() {
                return I18nUI.tr("Pausa o reanuda todas las tareas activas.",
                        "Pause or resume all active tasks.");
            }

            public static String CANCELAR() {
                return I18nUI.tr("Cancela tareas activas y evita nuevos reintentos automaticos.",
                        "Cancel active tasks and prevent automatic retries.");
            }

            public static String LIMPIAR() {
                return I18nUI.tr("Elimina tareas finalizadas (completadas, error o canceladas).",
                        "Remove finished tasks (completed, failed, or canceled).");
            }

            public static String ESTADISTICAS() {
                return I18nUI.tr("Resumen en tiempo real del estado de la cola de tareas.",
                        "Real-time queue status summary.");
            }

            public static String TABLA() {
                return I18nUI.tr("Cola de análisis con estado, URL y duración por tarea.",
                        "Analysis queue with status, URL and duration per task.");
            }

            public static String PAUSAR_TODO() {
                return I18nUI.tr("Pausa todas las tareas en cola o análisis.",
                        "Pause all queued or running tasks.");
            }

            public static String REANUDAR_TODO() {
                return I18nUI.tr("Reanuda todas las tareas actualmente pausadas.",
                        "Resume all currently paused tasks.");
            }

            public static String MENU_VER_DETALLES_ERROR() {
                return I18nUI.tr("Muestra URL, estado y duración de la tarea con error.",
                        "Show URL, status and duration of the failed task.");
            }

            public static String MENU_REINTENTAR_UNA() {
                return I18nUI.tr("Reencola la tarea para un nuevo intento.",
                        "Requeue task for another attempt.");
            }

            public static String MENU_PAUSAR_UNA() {
                return I18nUI.tr("Pausa la tarea para detener su procesamiento.",
                        "Pause task processing.");
            }

            public static String MENU_REANUDAR_UNA() {
                return I18nUI.tr("Reanuda la tarea pausada y la devuelve a cola.",
                        "Resume paused task and move it back to queue.");
            }

            public static String MENU_CANCELAR_UNA() {
                return I18nUI.tr("Cancela la tarea activa seleccionada.",
                        "Cancel selected active task.");
            }

            public static String MENU_ELIMINAR_UNA() {
                return I18nUI.tr("Elimina la tarea finalizada de la tabla.",
                        "Remove finished task from table.");
            }

            public static String MENU_REINTENTAR_MULTIPLES() {
                return I18nUI.tr("Reencola tareas con estado error o cancelado.",
                        "Requeue failed or canceled tasks.");
            }

            public static String MENU_PAUSAR_MULTIPLES() {
                return I18nUI.tr("Pausa tareas en cola o analizando.",
                        "Pause queued or running tasks.");
            }

            public static String MENU_REANUDAR_MULTIPLES() {
                return I18nUI.tr("Reanuda tareas pausadas y las devuelve a cola.",
                        "Resume paused tasks and move them back to queue.");
            }

            public static String MENU_CANCELAR_MULTIPLES() {
                return I18nUI.tr("Cancela tareas activas seleccionadas.",
                        "Cancel selected active tasks.");
            }

            public static String MENU_ELIMINAR_MULTIPLES() {
                return I18nUI.tr("Elimina tareas finalizadas seleccionadas.",
                        "Remove selected finished tasks.");
            }
        }

        public static final class Hallazgos {
            private Hallazgos() {
            }

            public static String BUSQUEDA() {
                return I18nUI.tr("Filtra hallazgos por texto en URL o descripción.",
                        "Filter findings by URL or description text.");
            }

            public static String FILTRO_SEVERIDAD() {
                return I18nUI.tr("Muestra solo hallazgos de la severidad seleccionada.",
                        "Show only findings with selected severity.");
            }

            public static String LIMPIAR_FILTROS() {
                return I18nUI.tr("Restablece búsqueda y filtro de severidad.",
                        "Reset search and severity filter.");
            }

            public static String EXPORTAR_CSV() {
                return I18nUI.tr("Exporta hallazgos no ignorados en formato CSV.",
                        "Export non-ignored findings as CSV.");
            }

            public static String EXPORTAR_JSON() {
                return I18nUI.tr("Exporta hallazgos no ignorados en formato JSON.",
                        "Export non-ignored findings as JSON.");
            }

            public static String LIMPIAR_TODO() {
                return I18nUI.tr("Elimina todos los hallazgos visibles en la tabla.",
                        "Remove all visible findings from table.");
            }

            public static String GUARDAR_ISSUES() {
                return I18nUI.tr("Guarda automáticamente hallazgos en Issues/Site Map de Burp.",
                        "Automatically save findings into Burp Issues/Site Map.");
            }

            public static String GUARDAR_ISSUES_SOLO_PRO() {
                return I18nUI.tr(
                        "Disponible solo en Burp Professional. En Community esta opción no guarda Issues.",
                        "Available only in Burp Professional. In Community this option does not save Issues.");
            }

            public static String TABLA() {
                return I18nUI.tr("Listado de hallazgos; doble clic para editar y clic derecho para acciones.",
                        "Findings list; double click to edit and right click for actions.");
            }

            public static String MENU_REPEATER() {
                return I18nUI.tr("Envia la request original a Repeater para validación manual.",
                        "Send original request to Repeater for manual validation.");
            }

            public static String MENU_INTRUDER() {
                return I18nUI.tr("Envia la request original a Intruder para pruebas activas.",
                        "Send original request to Intruder for active testing.");
            }

            public static String MENU_SCANNER() {
                return I18nUI.tr("Envia la request original al Scanner Pro de Burp.",
                        "Send original request to Burp Scanner Pro.");
            }

            public static String MENU_ISSUES() {
                return I18nUI.tr("Guarda manualmente hallazgos seleccionados en Issues/Site Map de Burp.",
                        "Manually save selected findings into Burp Issues/Site Map.");
            }

            public static String MENU_ISSUES_SOLO_PRO() {
                return I18nUI.tr(
                        "Disponible solo en Burp Professional.",
                        "Available only in Burp Professional.");
            }

            public static String MENU_IGNORAR() {
                return I18nUI.tr("Marca hallazgos para excluirlos de exportaciones.",
                        "Mark findings to exclude them from exports.");
            }

            public static String MENU_BORRAR() {
                return I18nUI.tr("Elimina hallazgos seleccionados de la tabla.",
                        "Delete selected findings from table.");
            }

            public static String MENU_AGREGAR_HALLAZGO() {
                return I18nUI.tr("Crea un hallazgo manual con URL, severidad y descripción personalizados.",
                        "Create a manual finding with custom URL, severity and description.");
            }

            public static String ENVIAR_AGENTE() {
                return I18nUI.tr("Enviar contexto del hallazgo al agente interactivo.",
                        "Send finding context to interactive agent.");
            }

            public static String ENVIAR_AGENTE(String agente) {
                return I18nUI.trf("Enviar contexto del hallazgo a %s.", "Send finding context to %s.", agente);
            }
        }

        public static final class Consola {
            private Consola() {
            }

            public static String AUTOSCROLL() {
                return I18nUI.tr("Desplaza la consola al final cuando llegan nuevos logs.",
                        "Scroll console to bottom when new logs arrive.");
            }

            public static String LIMPIAR() {
                return I18nUI.tr("Borra todo el historial de logs de la consola.",
                        "Clear full console log history.");
            }

            public static String RESUMEN() {
                return I18nUI.tr("Conteo acumulado de logs desde la última limpieza de consola.",
                        "Running totals of logs since the last console cleanup.");
            }

            public static String AREA_LOGS() {
                return I18nUI.tr("Salida de ejecución y diagnóstico de BurpIA.",
                        "BurpIA execution and diagnostic output.");
            }
        }

        public static final class Configuracion {
            private Configuracion() {
            }

            public static String GUARDAR() {
                return I18nUI.tr("Valida y guarda la configuración activa de BurpIA.",
                        "Validate and save current BurpIA configuration.");
            }

            public static String PROBAR_CONEXION() {
                return I18nUI.tr("Ejecuta una prueba de conexión con el proveedor seleccionado.",
                        "Run connectivity test against selected provider.");
            }

            public static String RETRASO() {
                return I18nUI.tr("Segundos de espera antes de cada llamada al proveedor.",
                        "Seconds to wait before each provider call.");
            }

            public static String MAXIMO_CONCURRENTE() {
                return I18nUI.tr("Número máximo de análisis simultáneos.",
                        "Maximum simultaneous analyses.");
            }

            public static String MAXIMO_HALLAZGOS() {
                return I18nUI.tr("Límite de hallazgos retenidos en memoria y tabla.",
                        "Findings limit kept in memory and table.");
            }

            public static String DETALLADO() {
                return I18nUI.tr("Activa logs de rastreo detallado para diagnóstico.",
                        "Enable verbose diagnostic tracing logs.");
            }

            public static String PROVEEDOR() {
                return I18nUI.tr("Proveedor LLM usado para analizar tráfico HTTP.",
                        "LLM provider used to analyze HTTP traffic.");
            }

            public static String URL_API() {
                return I18nUI.tr("URL base del endpoint del proveedor seleccionado.",
                        "Base endpoint URL for selected provider.");
            }

            public static String CLAVE_API() {
                return I18nUI.tr("Token/clave de autenticación del proveedor.",
                        "Provider authentication token/key.");
            }

            public static String MODELO() {
                return I18nUI.tr("Modelo específico utilizado para el análisis.",
                        "Specific model used for analysis.");
            }

            public static String CARGAR_MODELOS() {
                return I18nUI.tr("Consulta y actualiza la lista de modelos disponibles.",
                        "Fetch and refresh available model list.");
            }

            public static String MAX_TOKENS() {
                return I18nUI.tr("Máximo de tokens solicitados en respuestas del modelo.",
                        "Maximum tokens requested from model responses.");
            }

            public static String INFO_TOKENS() {
                return I18nUI.tr("Referencia rápida de ventanas de contexto por proveedor.",
                        "Quick reference for provider context windows.");
            }

            public static String TIMEOUT_MODELO() {
                return I18nUI.tr("Tiempo máximo de espera de respuesta para el proveedor+modelo seleccionados.",
                        "Maximum response wait time for the selected provider+model.");
            }

            public static String PROMPT_EDITOR() {
                return I18nUI.tr("Define las instrucciones que recibe el modelo.",
                        "Define instructions sent to the model.");
            }

            public static String RESTAURAR_PROMPT() {
                return I18nUI.tr("Reemplaza el prompt actual por el prompt por defecto.",
                        "Replace current prompt with default prompt.");
            }

            public static String CONTADOR_PROMPT() {
                return I18nUI.tr("Longitud actual del prompt y validación de tokens.",
                        "Current prompt length and token validation.");
            }

            public static String SITIO_AUTOR() {
                return I18nUI.tr("Abre el sitio web oficial del autor del proyecto.",
                        "Open the project author's official website.");
            }

            public static String CHECK_ACTUALIZACIONES() {
                return I18nUI.tr("Consulta la version remota y avisa si hay una nueva disponible.",
                        "Check remote version and notify when a newer one is available.");
            }

            public static String IGNORAR_SSL() {
                return I18nUI.tr("Habilita esto si obtienes errores de PKIX o certificados al conectar con la API.",
                        "Enable this if you get PKIX or certificate errors when connecting to the API.");
            }

            public static String SOLO_PROXY() {
                return I18nUI.tr("Ignora peticiones del Intruder, Scanner, Repeater u otras herramientas de Burp.",
                        "Ignore requests from Intruder, Scanner, Repeater, or other Burp tools.");
            }

            public static String HABILITAR_ALERTAS() {
                return I18nUI.tr("Activa o desactiva las ventanas emergentes de confirmación y advertencia.",
                        "Enable or disable confirmation and warning pop-ups.");
            }

            public static String PERSISTIR_FILTRO_BUSQUEDA() {
                return I18nUI.tr("Si está marcado, el texto de búsqueda en Hallazgos se restaura al reiniciar Burp Suite.",
                        "When checked, the search text in Findings is restored when Burp Suite restarts.");
            }

            public static String PERSISTIR_FILTRO_SEVERIDAD() {
                return I18nUI.tr("Si está marcado, el filtro de severidad en Hallazgos se restaura al reiniciar Burp Suite.",
                        "When checked, the severity filter in Findings is restored when Burp Suite restarts.");
            }

            public static String RESTAURAR_PROMPT_AGENTE() {
                return I18nUI.tr("Restaura el prompt de validación del agente al valor por defecto.",
                        "Restores the agent validation prompt to its default value.");
            }

            public static String RESTAURAR_PROMPT_INICIAL_AGENTE() {
                return I18nUI.tr("Restaura el prompt inicial pre-flight al valor por defecto.",
                        "Restores the initial pre-flight prompt to its default value.");
            }

            public static String DELAY_PROMPT_AGENTE() {
                return I18nUI.tr(
                        "Espera (ms) para inyectar el prompt inicial del agente. Varía según la máquina.",
                        "Wait (ms) to inject the agent initial prompt. Varies by machine.");
            }

            public static String HABILITAR_AGENTE() {
                return I18nUI.tr("Activa o desactiva el agente para pentesting asistido por IA.",
                        "Enable or disable the agent for AI-assisted pentesting.");
            }

            public static String SELECCIONAR_AGENTE() {
                return I18nUI.tr("Selecciona el perfil de agente (ej. Droid, Claude Code) a configurar.",
                        "Select the agent profile (e.g. Droid, Claude Code) to configure.");
            }

            public static String BINARIO_AGENTE() {
                return I18nUI.tr(
                        "Ruta o comando del agente (ej. droid o claude --dangerously-skip-permissions).",
                        "Agent path or command (e.g. droid or claude --dangerously-skip-permissions).");
            }

            public static String PROMPT_AGENTE() {
                return I18nUI.tr("Plantilla de validación enviada al agente cuando recibe un hallazgo.",
                        "Validation template sent to the agent when it receives a finding.");
            }

            public static String PROMPT_INICIAL_AGENTE() {
                return I18nUI.tr("Prompt inicial pre-flight que prepara herramientas y deja el agente en READY.",
                        "Initial pre-flight prompt that prepares tools and leaves the agent in READY state.");
            }

            public static String IDIOMA() {
                return I18nUI.tr("Cambia el idioma de toda la interfaz de BurpIA.",
                        "Switch the language for the entire BurpIA interface.");
            }

            public static String FUENTE_ESTANDAR() {
                return I18nUI.tr("Cambia la familia de fuente usada en la interfaz general de BurpIA.",
                        "Change the font family used in the general BurpIA interface.");
            }

            public static String TAMANIO_ESTANDAR() {
                return I18nUI.tr("Ajusta el tamaño de la letra para el texto normal del complemento.",
                        "Adjust the font size for the normal text of the extension.");
            }

            public static String FUENTE_MONO() {
                return I18nUI.tr("Tipografía de ancho fijo para bloques de código y terminales.",
                        "Fixed-width typography for code blocks and terminals.");
            }

            public static String TAMANIO_MONO() {
                return I18nUI.tr("Tamaño de letra para el texto de estilo monospace.",
                        "Font size for monospace style text.");
            }

            public static String RESTAURAR_FUENTES() {
                return I18nUI.tr("Restablece los estilos de fuente a los valores originales del sistema.",
                        "Reset font styles to the original system values.");
            }
        }

        public static final class Agente {
            private Agente() {
            }

            public static String REINICIAR() {
                return I18nUI.tr("Reinicia el proceso de terminal del agente.",
                        "Restart the agent terminal process.");
            }

            public static String CTRL_C() {
                return I18nUI.tr("Envía señal de interrupción (Ctrl+C) al proceso activo.",
                        "Send interrupt signal (Ctrl+C) to active process.");
            }

            public static String DELAY() {
                return I18nUI.tr(
                        "Espera (ms) para que los MCPs estén en verde e inyectar el prompt inicial. Varía según la máquina.",
                        "Wait (ms) for MCPs to turn green and inject the initial prompt. Varies by machine.");
            }

            public static String INYECTAR_PAYLOAD() {
                return I18nUI.tr("Inyecta manualmente el prompt inicial en la terminal.",
                        "Manually inject original prompt into terminal.");
            }

            public static String CAMBIAR_AGENTE_RAPIDO() {
                return I18nUI.tr("Cambia rápidamente entre agentes disponibles.",
                        "Quickly switch between available agents.");
            }

            public static String GUIA_AGENTE(String nombreAgente) {
                return I18nUI.trf(
                        "Abrir guía de instalación/configuración de %s.",
                        "Open %s installation/setup guide.",
                        nombreAgente);
            }
        }

        public static final class DetalleHallazgo {
            private DetalleHallazgo() {
            }

            public static String URL() {
                return I18nUI.tr("URL asociada al hallazgo.",
                        "URL associated with the finding.");
            }

            public static String SEVERIDAD() {
                return I18nUI.tr("Nivel de impacto estimado del hallazgo.",
                        "Estimated impact level of the finding.");
            }

            public static String CONFIANZA() {
                return I18nUI.tr("Nivel de certeza de la detección.",
                        "Detection confidence level.");
            }

            public static String DESCRIPCION() {
                return I18nUI.tr("Descripción técnica del hallazgo para análisis manual.",
                        "Technical finding description for manual analysis.");
            }

            public static String GUARDAR() {
                return I18nUI.tr("Guarda cambios y actualiza el hallazgo en la tabla.",
                        "Save changes and update finding in table.");
            }

            public static String CANCELAR() {
                return I18nUI.tr("Cierra el dialogo sin aplicar cambios.",
                        "Close dialog without applying changes.");
            }
        }

        public static final class Contexto {
            private Contexto() {
            }

            public static String ANALIZAR_SOLICITUD() {
                return I18nUI.tr("Lanza análisis inmediato de la request seleccionada.",
                        "Launch immediate analysis for selected request.");
            }

            public static String ENVIAR_A_AGENTE(String agente) {
                return I18nUI.trf("Envía la solicitud seleccionada a %s.",
                        "Send selected request to %s.",
                        agente);
            }
        }
    }
}
