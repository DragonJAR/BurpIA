package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.PromptTruncador;

import java.io.IOException;

/**
 * Ejecuta una llamada a la API de IA aplicando truncado-y-reintento ante
 * errores de contexto excedido.
 *
 * <p>Centraliza (DRY) la lógica que antes vivía duplicada/incompleta entre
 * {@link OrquestadorAnalisis} (proveedor único, con truncado) y
 * {@link GestorMultiProveedor} (multi-consulta, que <b>no</b> truncaba y
 * marcaba el proveedor como fallido). Ahora ambos caminos comparten este
 * ejecutor, de modo que la recuperación ante prompts demasiado largos
 * funciona igual en modo único y en multi-proveedor.</p>
 *
 * @since 1.6.0
 */
public final class EjecutorLlamadaConTruncado {

    /** Número máximo de truncados sucesivos antes de rendirse. */
    public static final int MAX_TRUNCADOS = 3;

    private static final String ORIGEN_LOG = "EjecutorLlamadaConTruncado";

    private final ConfiguracionAPI config;
    private final AnalizadorHTTP analizadorHTTP;
    private final PromptTruncador promptTruncador;
    private final ControlCancelacionPausa control;
    private final GestorLoggingUnificado gestorLogging;

    public EjecutorLlamadaConTruncado(ConfiguracionAPI config,
                                      AnalizadorHTTP analizadorHTTP,
                                      PromptTruncador promptTruncador,
                                      ControlCancelacionPausa control,
                                      GestorLoggingUnificado gestorLogging) {
        this.config = config != null ? config : new ConfiguracionAPI();
        this.analizadorHTTP = analizadorHTTP;
        this.promptTruncador = promptTruncador != null ? promptTruncador : new PromptTruncador();
        this.control = control != null ? control : new ControlCancelacionPausa(null, null);
        this.gestorLogging = gestorLogging != null
                ? gestorLogging
                : GestorLoggingUnificado.crearMinimal(null, null);
    }

    /**
     * Llama a la API con el prompt indicado; si el proveedor responde con un
     * error de contexto excedido, trunca el prompt y reintenta hasta
     * {@link #MAX_TRUNCADOS} veces.
     *
     * @param promptInicial prompt ya construido
     * @return respuesta cruda de la API
     * @throws IOException          si la llamada falla de forma definitiva
     * @throws InterruptedException si la tarea se cancela/pausa
     */
    public String ejecutar(String promptInicial) throws IOException, InterruptedException {
        String promptActual = promptInicial;
        int intentosTruncado = 0;

        while (intentosTruncado <= MAX_TRUNCADOS) {
            control.verificarCancelacion();
            control.esperarSiPausada();

            try {
                String respuesta = analizadorHTTP.llamarAPI(promptActual);
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Longitud de respuesta de API: %d caracteres",
                        respuesta.length()));
                return respuesta;
            } catch (ContextExceededException e) {
                if (intentosTruncado >= MAX_TRUNCADOS) {
                    gestorLogging.error(ORIGEN_LOG, I18nLogs.ContextoExcedido.MAX_INTENTOS(MAX_TRUNCADOS));
                    throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO(), e);
                }
                intentosTruncado++;

                int tokensObjetivo = obtenerTokensObjetivo(e);
                int longitudPreTruncado = promptActual.length();
                String promptTruncado = promptTruncador.truncarPrompt(promptActual, tokensObjetivo);

                // Guarda de no-progreso (M1): si el truncador no pudo reducir el
                // prompt (ya estaba bajo el objetivo estimado), reenviarlo sería
                // gastar llamadas inútiles re-tringereando el mismo error. Rendir.
                if (promptTruncado.length() >= longitudPreTruncado) {
                    gestorLogging.error(ORIGEN_LOG, I18nLogs.ContextoExcedido.NO_RETRYABLE());
                    throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO(), e);
                }

                promptActual = promptTruncado;
                gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.TRUNCANDO(intentosTruncado));
                gestorLogging.info(ORIGEN_LOG,
                        I18nLogs.ContextoExcedido.TRUNCADO(longitudPreTruncado, promptActual.length()));
                gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.RETRY_CON_TRUNCADO());
            }
        }

        // No debería alcanzarse: el bucle siempre retorna o lanza en cada rama.
        throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO());
    }

    /**
     * Umbral mínimo para aceptar un límite extraído del error como context
     * window real. Números menores (quota residual, IDs, "3 tokens remaining")
     * se descartan y caen al fallback del proveedor, evitando objetivos de
     * truncado absurdos que sobre-truncarían el prompt.
     */
    private static final int LIMITE_MINIMO_CREIBLE = 1000;

    /**
     * Calcula el número objetivo de tokens basado en el error y la
     * configuración del proveedor activo en {@link #config}.
     *
     * <p>Cascada DRY: límite extraído de la excepción (ya parseado) → maxTokens
     * del proveedor → estimación por modelo. El límite extraído solo se acepta
     * si es creíble (≥ {@link #LIMITE_MINIMO_CREIBLE}).</p>
     */
    private int obtenerTokensObjetivo(ContextExceededException error) {
        int limiteExtraido = error.obtenerLimiteTokens();
        if (limiteExtraido >= LIMITE_MINIMO_CREIBLE) {
            return promptTruncador.calcularTokensDisponibles(limiteExtraido, limiteExtraido / 4);
        }

        int maxTokens = config.obtenerMaxTokensParaProveedor(config.obtenerProveedorAI());
        if (maxTokens > 0) {
            return promptTruncador.calcularTokensDisponibles(maxTokens, maxTokens / 4);
        }

        int contextWindow = ConfiguracionAPI.estimarContextWindow(config.obtenerModelo());
        return promptTruncador.calcularTokensDisponibles(contextWindow, contextWindow / 4);
    }
}
