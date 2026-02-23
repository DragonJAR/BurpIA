package com.burpia.ui;

import com.burpia.i18n.I18nUI;

public final class TooltipsUI {
    private TooltipsUI() {
    }

    public static final class Pestanias {
        private Pestanias() {
        }

        public static String TAREAS() {
            return I18nUI.tr("Monitoriza la cola de analisis y controla su ejecucion.",
                "Monitor the analysis queue and control execution.");
        }

        public static String HALLAZGOS() {
            return I18nUI.tr("Revisa, filtra y exporta hallazgos detectados por BurpIA.",
                "Review, filter, and export BurpIA findings.");
        }

        public static String CONSOLA() {
            return I18nUI.tr("Consulta logs operativos y de diagnostico del complemento.",
                "Review extension operational and diagnostic logs.");
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
            return I18nUI.tr("Distribucion de hallazgos por nivel de severidad.",
                "Distribution of findings by severity.");
        }

        public static String LIMITE_HALLAZGOS() {
            return I18nUI.tr("Limite maximo de hallazgos conservados en tabla/memoria.",
                "Maximum findings kept in table/memory.");
        }

        public static String RESUMEN_OPERATIVO() {
            return I18nUI.tr("Solicitudes recibidas, analizadas, omitidas y errores.",
                "Requests received, analyzed, skipped and errored.");
        }

        public static String CONFIGURACION() {
            return I18nUI.tr("Abre configuracion de proveedor, prompt y limites.",
                "Open provider, prompt and limits settings.");
        }

        public static String CAPTURA_PAUSAR() {
            return I18nUI.tr("Pausa el analisis pasivo de nuevas respuestas HTTP.",
                "Pause passive analysis of new HTTP responses.");
        }

        public static String CAPTURA_REANUDAR() {
            return I18nUI.tr("Reanuda el analisis pasivo de respuestas HTTP.",
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
            return I18nUI.tr("Cola de analisis con estado, URL y duracion por tarea.",
                "Analysis queue with status, URL and duration per task.");
        }

        public static String PAUSAR_TODO() {
            return I18nUI.tr("Pausa todas las tareas en cola o analisis.",
                "Pause all queued or running tasks.");
        }

        public static String REANUDAR_TODO() {
            return I18nUI.tr("Reanuda todas las tareas actualmente pausadas.",
                "Resume all currently paused tasks.");
        }

        public static String MENU_VER_DETALLES_ERROR() {
            return I18nUI.tr("Muestra URL, estado y duracion de la tarea con error.",
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
            return I18nUI.tr("Filtra hallazgos por texto en URL o descripcion.",
                "Filter findings by URL or description text.");
        }

        public static String FILTRO_SEVERIDAD() {
            return I18nUI.tr("Muestra solo hallazgos de la severidad seleccionada.",
                "Show only findings with selected severity.");
        }

        public static String LIMPIAR_FILTROS() {
            return I18nUI.tr("Restablece busqueda y filtro de severidad.",
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
            return I18nUI.tr("Guarda automaticamente hallazgos en Issues/Site Map de Burp.",
                "Automatically save findings into Burp Issues/Site Map.");
        }

        public static String GUARDAR_ISSUES_SOLO_PRO() {
            return I18nUI.tr(
                "Disponible solo en Burp Professional. En Community esta opcion no guarda Issues.",
                "Available only in Burp Professional. In Community this option does not save Issues."
            );
        }

        public static String TABLA() {
            return I18nUI.tr("Listado de hallazgos; doble clic para editar y clic derecho para acciones.",
                "Findings list; double click to edit and right click for actions.");
        }

        public static String MENU_REPEATER() {
            return I18nUI.tr("Envia la request original a Repeater para validacion manual.",
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
                "Available only in Burp Professional."
            );
        }

        public static String MENU_IGNORAR() {
            return I18nUI.tr("Marca hallazgos para excluirlos de exportaciones.",
                "Mark findings to exclude them from exports.");
        }

        public static String MENU_BORRAR() {
            return I18nUI.tr("Elimina hallazgos seleccionados de la tabla.",
                "Delete selected findings from table.");
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
            return I18nUI.tr("Conteo acumulado de logs informativos, detallados y de error.",
                "Running totals of info, verbose and error logs.");
        }

        public static String AREA_LOGS() {
            return I18nUI.tr("Salida de ejecucion y diagnostico de BurpIA.",
                "BurpIA execution and diagnostic output.");
        }
    }

    public static final class Configuracion {
        private Configuracion() {
        }

        public static String GUARDAR() {
            return I18nUI.tr("Valida y guarda la configuracion activa de BurpIA.",
                "Validate and save current BurpIA configuration.");
        }

        public static String PROBAR_CONEXION() {
            return I18nUI.tr("Ejecuta una prueba de conexion con el proveedor seleccionado.",
                "Run connectivity test against selected provider.");
        }

        public static String RETRASO() {
            return I18nUI.tr("Segundos de espera antes de cada llamada al proveedor.",
                "Seconds to wait before each provider call.");
        }

        public static String MAXIMO_CONCURRENTE() {
            return I18nUI.tr("Numero maximo de analisis simultaneos.",
                "Maximum simultaneous analyses.");
        }

        public static String MAXIMO_HALLAZGOS() {
            return I18nUI.tr("Limite de hallazgos retenidos en memoria y tabla.",
                "Findings limit kept in memory and table.");
        }

        public static String DETALLADO() {
            return I18nUI.tr("Activa logs de rastreo detallado para diagnostico.",
                "Enable verbose diagnostic tracing logs.");
        }

        public static String PROVEEDOR() {
            return I18nUI.tr("Proveedor LLM usado para analizar trafico HTTP.",
                "LLM provider used to analyze HTTP traffic.");
        }

        public static String URL_API() {
            return I18nUI.tr("URL base del endpoint del proveedor seleccionado.",
                "Base endpoint URL for selected provider.");
        }

        public static String CLAVE_API() {
            return I18nUI.tr("Token/clave de autenticacion del proveedor.",
                "Provider authentication token/key.");
        }

        public static String MODELO() {
            return I18nUI.tr("Modelo especifico utilizado para el analisis.",
                "Specific model used for analysis.");
        }

        public static String CARGAR_MODELOS() {
            return I18nUI.tr("Consulta y actualiza la lista de modelos disponibles.",
                "Fetch and refresh available model list.");
        }

        public static String MAX_TOKENS() {
            return I18nUI.tr("Maximo de tokens solicitados en respuestas del modelo.",
                "Maximum tokens requested from model responses.");
        }

        public static String INFO_TOKENS() {
            return I18nUI.tr("Referencia rapida de ventanas de contexto por proveedor.",
                "Quick reference for provider context windows.");
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
            return I18nUI.tr("Longitud actual del prompt y validacion de tokens.",
                "Current prompt length and token validation.");
        }

        public static String SITIO_AUTOR() {
            return I18nUI.tr("Abre el sitio web oficial del autor del proyecto.",
                "Open the project author's official website.");
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
            return I18nUI.tr("Nivel de certeza de la deteccion.",
                "Detection confidence level.");
        }

        public static String DESCRIPCION() {
            return I18nUI.tr("Descripcion tecnica del hallazgo para analisis manual.",
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
            return I18nUI.tr("Lanza analisis inmediato de la request seleccionada.",
                "Launch immediate analysis for selected request.");
        }
    }
}
