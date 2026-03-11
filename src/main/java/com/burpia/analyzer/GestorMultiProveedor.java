package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ConstantesJsonAI;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GsonProvider;
import com.burpia.util.JsonParserUtil;
import com.burpia.util.Normalizador;
import com.burpia.util.ParserRespuestasAI;
import com.burpia.util.ReparadorJson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public class GestorMultiProveedor {
    private static final String ORIGEN_LOG = "GestorMultiProveedor";
    private static final long DELAY_ENTRE_PROVEEDORES_MS = 2000L;
    private static final String LINEA_SEPARADORA_PROVEEDOR = "========================================";
    
    private final SolicitudAnalisis solicitud;
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final GestorLoggingUnificado gestorLogging;
    private final Object logLock;
    private final Gson gson;
    private final ConstructorPrompts constructorPrompt;

    public GestorMultiProveedor(SolicitudAnalisis solicitud,
                               ConfiguracionAPI config,
                               PrintWriter stdout,
                               PrintWriter stderr,
                               GestorConsolaGUI gestorConsola,
                               BooleanSupplier tareaCancelada,
                               BooleanSupplier tareaPausada,
                               GestorLoggingUnificado gestorLogging) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.stdout = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.stderr = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.gestorConsola = gestorConsola;
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.gestorLogging = gestorLogging;
        this.logLock = new Object();
        this.gson = GsonProvider.get();
        this.constructorPrompt = new ConstructorPrompts(this.config);
    }

    public ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedor() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        
        if (Normalizador.esVacia(proveedores)) {
            registrar("Multi-consulta: No hay proveedores seleccionados, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            registrar("Multi-consulta: Solo 1 proveedor seleccionado, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        return ejecutarAnalisisSecuencialProveedores(proveedores);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisSecuencialProveedores(List<String> proveedores)
            throws IOException, InterruptedException {
        
        List<Hallazgo> todosHallazgos = new ArrayList<>();
        List<String> proveedoresFallidos = new ArrayList<>();

        try {
            for (String proveedor : proveedores) {
                verificarCancelacion();
                esperarSiPausada();

                if (Normalizador.esVacio(proveedor)) {
                    continue;
                }
                
                if (!ProveedorAI.existeProveedor(proveedor)) {
                    registrar("PROVEEDOR: Proveedor no existe: " + proveedor + ", omitiendo");
                    continue;
                }

                String modelo = config.obtenerModeloParaProveedor(proveedor);
                if (Normalizador.esVacio(modelo)) {
                    registrar("PROVEEDOR: Proveedor " + proveedor + " no tiene modelo configurado, omitiendo");
                    continue;
                }

                if (!todosHallazgos.isEmpty()) {
                    long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                    registrar("PROVEEDOR: Esperando " + delaySegundos + " segundos antes del siguiente proveedor");
                    esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
                }

                registrar(LINEA_SEPARADORA_PROVEEDOR);
                registrar("PROVEEDOR: " + proveedor + " (" + modelo + ")");

                try {
                    ResultadoAnalisisMultiple resultado = ejecutarAnalisisProveedor(proveedor, modelo);
                    List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();
                    
                    registrar("PROVEEDOR: " + proveedor + " completado - " + hallazgosProveedor.size()
                            + " hallazgo(s) encontrado(s)");
                    todosHallazgos.addAll(hallazgosProveedor);

                } catch (Exception e) {
                    registrar("PROVEEDOR: Error con " + proveedor + ": " + e.getMessage());
                    proveedoresFallidos.add(proveedor);
                }
            }

            if (!proveedoresFallidos.isEmpty()) {
                registrarError("PROVEEDOR: " + proveedoresFallidos.size() +
                        " proveedor(es) fallaron: " + String.join(", ", proveedoresFallidos));
            }

            registrar(LINEA_SEPARADORA_PROVEEDOR);
            registrar("PROVEEDOR: Multi-consulta completada. Total de hallazgos combinados: " + todosHallazgos.size());

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), todosHallazgos,
                    solicitud.obtenerSolicitudHttp(), proveedoresFallidos);

        } finally {
        }
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedorUnico() throws IOException, InterruptedException {
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(config, tareaCancelada, tareaPausada, gestorLogging);
        String respuesta = llamarAPIAIConRetries(analizadorHTTP, config);
        return parsearRespuesta(respuesta);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedor(String proveedor, String modelo)
            throws IOException, InterruptedException {
        
        ConfiguracionAPI configProveedor = crearConfiguracionParaProveedor(proveedor);
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(configProveedor, tareaCancelada, tareaPausada, gestorLogging);
        String respuesta = llamarAPIAIConRetries(analizadorHTTP, configProveedor);
        ResultadoAnalisisMultiple resultado = parsearRespuesta(respuesta);
        
        return etiquetarResultado(resultado, proveedor, modelo);
    }

    private ConfiguracionAPI crearConfiguracionParaProveedor(String proveedor) {
        ConfiguracionAPI configProveedor = new ConfiguracionAPI();
        configProveedor.aplicarDesde(config);
        configProveedor.establecerProveedorAI(proveedor);
        return configProveedor;
    }

    private String llamarAPIAIConRetries(AnalizadorHTTP analizadorHTTP, ConfiguracionAPI configActual)
            throws IOException, InterruptedException {
        
        verificarCancelacion();
        esperarSiPausada();
        
        String prompt = constructorPrompt.construirPromptAnalisis(solicitud);
        
        try {
            String respuesta = analizadorHTTP.llamarAPI(prompt);
            registrar("Longitud de respuesta de API: " + respuesta.length() + " caracteres");
            return respuesta;
        } catch (ContextExceededException e) {
            // Para multi-proveedor, propagar como IOException
            throw new IOException("Contexto excedido en proveedor " + configActual.obtenerProveedorAI() + ": " + e.getMessage(), e);
        }
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        String respuestaOriginal = respuestaJson != null ? respuestaJson : "";

        try {
            // Intentar reparar JSON malformado primero
            String jsonReparado = ReparadorJson.repararJson(respuestaOriginal);
            String respuestaProcesada = respuestaOriginal;
            if (jsonReparado != null && !jsonReparado.equals(respuestaOriginal)) {
                respuestaProcesada = jsonReparado;
            }

            String proveedor = config.obtenerProveedorAI() != null ? config.obtenerProveedorAI() : "";
            String contenido = ParserRespuestasAI.extraerContenido(respuestaProcesada, proveedor);
            if (Normalizador.esVacio(contenido)) {
                contenido = respuestaProcesada;
            }

            // Usar GsonProvider para parseo consistente
            JsonElement raiz = gson.fromJson(contenido, JsonElement.class);
            List<JsonObject> objetosHallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, ConstantesJsonAI.CAMPOS_HALLAZGOS);

            if (!objetosHallazgos.isEmpty()) {
                for (JsonObject obj : objetosHallazgos) {
                    String titulo = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO);
                    String descripcion = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION);
                    String severidad = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD);
                    String confianza = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA);
                    String evidencia = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA);

                    agregarHallazgoNormalizado(hallazgos, titulo, descripcion, severidad, confianza, evidencia);
                }
            } else {
                // Fallback: usar ParserRespuestasAI para extracción extrema
                hallazgos.addAll(parsearRespuestaExtrema(contenido, respuestaOriginal));
            }
        } catch (Exception e) {
            registrarError("Error al parsear respuesta: " + e.getMessage());
            // Último recurso: usar extracción extrema
            hallazgos.addAll(parsearRespuestaExtrema(respuestaOriginal, respuestaOriginal));
        }

        return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgos,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList());
    }

    /**
     * Extracción extrema usando ParserRespuestasAI cuando todo lo demás falla.
     * Aplica las estrategias de fallback ya existentes en ParserRespuestasAI.
     */
    private List<Hallazgo> parsearRespuestaExtrema(String contenido, String respuestaOriginal) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (Normalizador.esVacio(contenido)) {
            return hallazgos;
        }

        // Intentar extracción de array JSON inteligente (método existente en ParserRespuestasAI)
        try {
            com.google.gson.JsonArray arrayHallazgos = ParserRespuestasAI.extraerArrayJsonInteligente(contenido, gson);
            if (arrayHallazgos != null && arrayHallazgos.size() > 0) {
                for (com.google.gson.JsonElement item : arrayHallazgos) {
                    if (item.isJsonObject()) {
                        JsonObject obj = item.getAsJsonObject();
                        String titulo = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO);
                        String descripcion = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION);
                        String severidad = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD);
                        String confianza = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA);
                        String evidencia = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA);
                        agregarHallazgoNormalizado(hallazgos, titulo, descripcion, severidad, confianza, evidencia);
                    }
                }
                return hallazgos;
            }
        } catch (Exception e) {
            // Continuar con siguiente fallback
        }

        // Intentar extracción por delimitadores (método existente en ParserRespuestasAI)
        try {
            com.google.gson.JsonArray arrayDelimitadores = ParserRespuestasAI.extraerHallazgosPorDelimitadores(contenido, gson);
            if (arrayDelimitadores != null && arrayDelimitadores.size() > 0) {
                for (com.google.gson.JsonElement item : arrayDelimitadores) {
                    if (item.isJsonObject()) {
                        JsonObject obj = item.getAsJsonObject();
                        String titulo = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO);
                        String descripcion = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION);
                        String severidad = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD);
                        String confianza = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA);
                        String evidencia = JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA);
                        agregarHallazgoNormalizado(hallazgos, titulo, descripcion, severidad, confianza, evidencia);
                    }
                }
                return hallazgos;
            }
        } catch (Exception e) {
            // Continuar con texto plano
        }

        // Último recurso: extracción de texto plano usando ParserRespuestasAI
        return extraerHallazgosTextoPlano(contenido);
    }

    /**
     * Extracción de hallazgos de texto plano cuando JSON falla completamente.
     */
    private List<Hallazgo> extraerHallazgosTextoPlano(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (Normalizador.esVacio(contenido)) {
            return hallazgos;
        }

        // Buscar patrones de campos conocidos en texto plano
        String[] patronesTitulo = {"titulo:", "title:", "vulnerabilidad:", "vulnerability:"};
        String[] patronesDescripcion = {"descripcion:", "description:", "detalle:", "details:", "hallazgo:", "finding:"};

        // Intentar extraer al menos un título si existe
        String titulo = extraerPrimerCoincidencia(contenido, patronesTitulo);
        String descripcion = extraerPrimerCoincidencia(contenido, patronesDescripcion);

        if (Normalizador.noEsVacio(titulo)) {
            hallazgos.add(new Hallazgo(
                    solicitud.obtenerUrl(),
                    titulo,
                    Normalizador.noEsVacio(descripcion) ? descripcion : "Hallazgo detectado en análisis",
                    Hallazgo.SEVERIDAD_INFO,
                    Hallazgo.CONFIANZA_BAJA,
                    solicitud.obtenerSolicitudHttp()
            ));
        }

        return hallazgos;
    }

    /**
     * Extrae el valor después del primer patrón encontrado.
     */
    private String extraerPrimerCoincidencia(String texto, String[] patrones) {
        if (Normalizador.esVacio(texto) || patrones == null) {
            return "";
        }
        String textoLower = texto.toLowerCase();
        for (String patron : patrones) {
            int indice = textoLower.indexOf(patron);
            if (indice >= 0) {
                int inicio = indice + patron.length();
                // Buscar hasta el final de línea o próximo campo
                int fin = texto.indexOf('\n', inicio);
                if (fin < 0) {
                    fin = texto.length();
                }
                String valor = texto.substring(inicio, fin).trim();
                // Limpiar comillas si existen
                if (valor.startsWith("\"") && valor.endsWith("\"")) {
                    valor = valor.substring(1, valor.length() - 1);
                }
                return Normalizador.normalizarTexto(valor);
            }
        }
        return "";
    }

    private void agregarHallazgoNormalizado(List<Hallazgo> destino,
            String tituloRaw,
            String descripcionRaw,
            String severidadRaw,
            String confianzaRaw,
            String evidenciaRaw) {
        if (destino == null) {
            return;
        }
        String titulo = normalizarTextoSimple(tituloRaw, "Sin título");
        String descripcion = normalizarTextoSimple(descripcionRaw, "");
        String evidencia = normalizarTextoSimple(evidenciaRaw, "");
        
        if (Normalizador.esVacio(descripcion)) {
            descripcion = evidencia;
        } else if (Normalizador.noEsVacio(evidencia) && !descripcion.contains(evidencia)) {
            descripcion = descripcion + "\nEvidencia: " + evidencia;
        }
        
        if (Normalizador.esVacio(descripcion)) {
            descripcion = "Sin descripción";
        }
        
        String severidad = Hallazgo.normalizarSeveridad(normalizarTextoSimple(severidadRaw, Hallazgo.SEVERIDAD_INFO));
        String confianza = Hallazgo.normalizarConfianza(normalizarTextoSimple(confianzaRaw, Hallazgo.CONFIANZA_BAJA));
        
        destino.add(new Hallazgo(
                solicitud.obtenerUrl(),
                titulo,
                descripcion,
                severidad,
                confianza,
                solicitud.obtenerSolicitudHttp()));
    }

    private String normalizarTextoSimple(String valor, String porDefecto) {
        if (valor == null) {
            return porDefecto != null ? porDefecto : "";
        }
        String normalizado = Normalizador.normalizarTexto(valor);
        if (normalizado.isEmpty()) {
            return porDefecto != null ? porDefecto : "";
        }
        return normalizado;
    }

    private ResultadoAnalisisMultiple etiquetarResultado(ResultadoAnalisisMultiple resultado,
                                                         String proveedor,
                                                         String modelo) {
        List<Hallazgo> hallazgos = resultado.obtenerHallazgos();
        List<Hallazgo> hallazgosConEtiqueta = new ArrayList<>();

        for (Hallazgo hallazgo : hallazgos) {
            String descripcionOriginal = hallazgo.obtenerHallazgo();
            String etiqueta = I18nUI.Configuracion.TXT_DESCUBIERTO_CON(proveedor, modelo);
            String descripcionConEtiqueta = descripcionOriginal + etiqueta;

            Hallazgo hallazgoEtiquetado = new Hallazgo(
                    hallazgo.obtenerUrl(),
                    hallazgo.obtenerTitulo(),
                    descripcionConEtiqueta,
                    hallazgo.obtenerSeveridad(),
                    hallazgo.obtenerConfianza(),
                    hallazgo.obtenerSolicitudHttp(),
                    hallazgo.obtenerEvidenciaHttp());

            hallazgosConEtiqueta.add(hallazgoEtiquetado);
        }

        return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgosConEtiqueta,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList());
    }

    private void verificarCancelacion() throws InterruptedException {
        if (tareaCancelada.getAsBoolean()) {
            throw new InterruptedException("Tarea cancelada por usuario");
        }
    }

    private void esperarSiPausada() throws InterruptedException {
        while (tareaPausada.getAsBoolean() && !tareaCancelada.getAsBoolean()) {
            Thread.sleep(250);
        }
        verificarCancelacion();
    }

    private void esperarConControl(long milisegundos) throws InterruptedException {
        long restante = milisegundos;
        while (restante > 0) {
            verificarCancelacion();
            esperarSiPausada();
            long espera = Math.min(restante, 250);
            Thread.sleep(espera);
            restante -= espera;
        }
    }

    private void registrar(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.info(ORIGEN_LOG, mensaje);
        } else {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.INFO, false, "[BurpIA] ", false);
        }
    }

    private void registrarError(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.error(ORIGEN_LOG, mensaje);
        } else {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.ERROR, true, "[BurpIA] [ERROR] ", false);
        }
    }

    private void registrarInterno(String mensaje, GestorConsolaGUI.TipoLog tipo, boolean esError, 
                                 String prefijoSalida, boolean mensajeTecnico) {
        String mensajeSeguro = mensaje != null ? mensaje : "";
        GestorConsolaGUI consolaActual = this.gestorConsola;

        if (consolaActual != null) {
            if (mensajeTecnico) {
                consolaActual.registrarTecnico(ORIGEN_LOG, mensajeSeguro, tipo);
            } else {
                consolaActual.registrar(ORIGEN_LOG, mensajeSeguro, tipo);
            }
            return;
        }

        PrintWriter destinoStr;
        synchronized (logLock) {
            destinoStr = esError ? stderr : stdout;
            if (destinoStr != null) {
                destinoStr.println(prefijoSalida + mensajeSeguro);
                destinoStr.flush();
            }
        }
    }
}