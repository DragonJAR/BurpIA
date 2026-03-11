package com.burpia.analyzer;

import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ConstantesJsonAI;
import com.burpia.util.ExtractorCamposRobusto;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GsonProvider;
import com.burpia.util.JsonParserUtil;
import com.burpia.util.Normalizador;
import com.burpia.util.ParserRespuestasAI;
import com.burpia.util.ReparadorJson;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParseadorRespuestasAI {
    private static final String ORIGEN_LOG = "ParseadorRespuestasAI";

    private final GestorLoggingUnificado gestorLogging;
    private final Gson gson;
    private final String idiomaUi;

    public ParseadorRespuestasAI(GestorLoggingUnificado gestorLogging, String idiomaUi) {
        this.gestorLogging = gestorLogging != null ? gestorLogging : GestorLoggingUnificado.crear(null, null, null, null, null);
        this.gson = GsonProvider.get();
        this.idiomaUi = idiomaUi != null ? idiomaUi : "es";
    }

    public ParseadorRespuestasAI(GestorLoggingUnificado gestorLogging) {
        this(gestorLogging, "es");
    }

    public ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson, SolicitudAnalisis solicitud, String proveedor) {
        rastrear("Parseando respuesta JSON");
        List<Hallazgo> hallazgos = new ArrayList<>();
        String respuestaOriginal = respuestaJson != null ? respuestaJson : "";

        try {
            String respuestaNormalizada = ParserRespuestasAI.limpiarContenidoModelo(respuestaOriginal);
            String jsonReparado = ReparadorJson.repararJson(respuestaNormalizada);
            String respuestaProcesada = respuestaOriginal;
            if (jsonReparado != null && !jsonReparado.equals(respuestaNormalizada)) {
                rastrear("JSON reparado exitosamente");
                respuestaProcesada = jsonReparado;
            } else {
                respuestaProcesada = respuestaNormalizada;
            }

            String proveedorNormalizado = proveedor != null ? proveedor : "";
            String contenido = ParserRespuestasAI.extraerContenido(respuestaProcesada, proveedorNormalizado);
            if (Normalizador.esVacio(contenido)) {
                contenido = ParserRespuestasAI.limpiarContenidoModelo(respuestaProcesada);
            }

            rastrear("Contenido extraído - Longitud: " + contenido.length() + " caracteres");

            try {
                // Sanitizar comillas escapadas antes de parsear
                String contenidoSanitizado = sanitizarComillasEscapadas(contenido);
                String contenidoLimpio = limpiarBloquesMarkdownJson(contenidoSanitizado);
                JsonElement raiz = gson.fromJson(contenidoLimpio, JsonElement.class);
                List<JsonObject> objetosHallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, ConstantesJsonAI.CAMPOS_HALLAZGOS);

                if (!objetosHallazgos.isEmpty()) {
                    rastrear("Se encontraron " + objetosHallazgos.size() + " hallazgos en JSON");
                    for (JsonObject obj : objetosHallazgos) {
                        agregarHallazgoNormalizado(
                                hallazgos,
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA),
                                solicitud);
                    }
                } else {
                    rastrear("JSON sin objetos de hallazgo, intentando parsing de texto plano");
                    hallazgos.addAll(parsearTextoPlano(contenido, solicitud));
                }
            } catch (Exception e) {
                rastrear("No se pudo parsear como JSON de hallazgos: " + e.getMessage());
                List<Hallazgo> hallazgosNoEstrictos = parsearHallazgosJsonNoEstricto(contenido, solicitud);
                if (!hallazgosNoEstrictos.isEmpty()) {
                    rastrear("Fallback JSON no estricto recuperó " + hallazgosNoEstrictos.size() + " hallazgos");
                    hallazgos.addAll(hallazgosNoEstrictos);
                } else {
                    rastrear("Intentando parsing de texto plano como fallback");
                    hallazgos.addAll(parsearTextoPlano(contenido, solicitud));
                }
            }

            if (!respuestaProcesada.equals(respuestaOriginal)) {
                String contenidoOriginal = ParserRespuestasAI.extraerContenido(respuestaOriginal, proveedorNormalizado);
                if (Normalizador.esVacio(contenidoOriginal)) {
                    contenidoOriginal = ParserRespuestasAI.limpiarContenidoModelo(respuestaOriginal);
                }

                List<Hallazgo> hallazgosOriginalesNoEstrictos = parsearHallazgosJsonNoEstricto(contenidoOriginal, solicitud);
                if (debePreferirHallazgosOriginales(hallazgosOriginalesNoEstrictos, hallazgos)) {
                    rastrear(
                            "Se detectó pérdida de contenido tras reparación JSON; " +
                                    "se conserva parseo no estricto del payload original");
                    hallazgos.clear();
                    hallazgos.addAll(hallazgosOriginalesNoEstrictos);
                }
            }

            rastrear("Total de hallazgos parseados: " + hallazgos.size());

            return new ResultadoAnalisisMultiple(
                    solicitud.obtenerUrl(),
                    hallazgos,
                    solicitud.obtenerSolicitudHttp(),
                    Collections.emptyList());

        } catch (Exception e) {
            String errorMsg = Normalizador.noEsVacio(e.getMessage())
                ? e.getMessage()
                : I18nUI.Tareas.MSG_ERROR_DESCONOCIDO();
            String errorDesc = I18nUI.trf("Error al parsear respuesta: %s", "Error parsing response: %s", errorMsg);
            gestorLogging.error(ORIGEN_LOG,
                I18nLogs.tr("Error crítico al parsear respuesta de API para " + solicitud.obtenerUrl()),
                e);
            throw new ParseExceptionAI(errorDesc, e);
        }
    }

    private List<Hallazgo> parsearHallazgosJsonNoEstricto(String contenido, SolicitudAnalisis solicitud) {
        if (Normalizador.esVacio(contenido)) {
            return new ArrayList<>();
        }

        JsonArray arrayHallazgos = ParserRespuestasAI.extraerArrayJsonInteligente(contenido, gson);

        if (arrayHallazgos != null && arrayHallazgos.size() > 0) {
            return convertirArrayAHallazgos(arrayHallazgos, solicitud);
        }

        JsonArray arrayRecuperado = ParserRespuestasAI.extraerHallazgosPorDelimitadores(contenido, gson);

        if (arrayRecuperado != null && arrayRecuperado.size() > 0) {
            rastrear("Recuperación extrema: " + arrayRecuperado.size() + " hallazgos");
            return convertirArrayAHallazgos(arrayRecuperado, solicitud);
        }

        return parsearHallazgosCampoPorCampo(contenido, solicitud);
    }

    private List<Hallazgo> convertirArrayAHallazgos(JsonArray array, SolicitudAnalisis solicitud) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (JsonElement item : array) {
            if (item != null && item.isJsonObject()) {
                JsonObject obj = item.getAsJsonObject();
                agregarHallazgoNormalizado(
                        hallazgos,
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA),
                        solicitud);
            }
        }
        rastrear("Array JSON convertido a " + hallazgos.size() + " hallazgos");
        return hallazgos;
    }

    private List<Hallazgo> parsearHallazgosCampoPorCampo(String contenido, SolicitudAnalisis solicitud) {
        List<Hallazgo> hallazgos = new ArrayList<>();

        String bloqueHallazgos = extraerBloqueArrayHallazgos(contenido);
        if (Normalizador.esVacio(bloqueHallazgos)) {
            return hallazgos;
        }

        List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                bloqueHallazgos,
                ExtractorCamposRobusto.CamposHallazgo.TITULO);
        if (bloques.isEmpty()) {
            bloques = extraerObjetosNoEstrictos(bloqueHallazgos);
        }

        for (String objeto : bloques) {
            String bloqueHallazgo = normalizarObjetoNoEstricto(objeto);
            if (Normalizador.esVacio(bloqueHallazgo)) {
                continue;
            }

            String titulo = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_TITULO, bloqueHallazgo);
            String descripcion = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_DESCRIPCION, bloqueHallazgo);
            String severidad = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_SEVERIDAD, bloqueHallazgo);
            String confianza = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_CONFIANZA, bloqueHallazgo);
            String evidencia = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_EVIDENCIA, bloqueHallazgo);

            agregarHallazgoNormalizado(hallazgos, titulo, descripcion, severidad, confianza, evidencia, solicitud);
        }

        if (!hallazgos.isEmpty()) {
            rastrear("Parseo campo por campo recuperó " + hallazgos.size() + " hallazgos");
        }

        return hallazgos;
    }

    private String extraerCampoConFallback(String[] campos, String bloque) {
        for (String campo : campos) {
            String valor = ParserRespuestasAI.extraerCampoNoEstricto(campo, bloque);
            if (Normalizador.noEsVacio(valor)) {
                return valor;
            }
        }
        return "";
    }

    private boolean debePreferirHallazgosOriginales(List<Hallazgo> originales, List<Hallazgo> actuales) {
        if (originales == null || originales.isEmpty()) {
            return false;
        }
        if (actuales == null || actuales.isEmpty()) {
            return true;
        }
        if (originales.size() > actuales.size()) {
            return true;
        }
        if (originales.size() < actuales.size()) {
            return false;
        }
        return calcularHuellaContenido(originales) > calcularHuellaContenido(actuales);
    }

    private int calcularHuellaContenido(List<Hallazgo> hallazgos) {
        int huella = 0;
        for (Hallazgo hallazgo : hallazgos) {
            if (hallazgo == null) {
                continue;
            }
            huella += longitudSegura(hallazgo.obtenerTitulo());
            huella += longitudSegura(hallazgo.obtenerHallazgo());
        }
        return huella;
    }

    private int longitudSegura(String valor) {
        return valor != null ? valor.length() : 0;
    }

    private String extraerBloqueArrayHallazgos(String contenido) {
        int indiceHallazgos = -1;
        String[] claves = { "\"hallazgos\"", "\"findings\"", "\"issues\"", "\"vulnerabilidades\"" };
        for (String clave : claves) {
            int indice = contenido.indexOf(clave);
            if (indice >= 0 && (indiceHallazgos < 0 || indice < indiceHallazgos)) {
                indiceHallazgos = indice;
            }
        }
        if (indiceHallazgos < 0) {
            return "";
        }
        int inicioArray = contenido.indexOf('[', indiceHallazgos);
        if (inicioArray < 0) {
            return "";
        }

        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = inicioArray; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (escapado) {
                escapado = false;
                continue;
            }
            if (c == '\\') {
                escapado = true;
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
                continue;
            }
            if (!enComillas) {
                if (c == '[') {
                    profundidad++;
                } else if (c == ']') {
                    profundidad--;
                    if (profundidad == 0) {
                        return contenido.substring(inicioArray + 1, i);
                    }
                }
            }
        }
        return "";
    }

    private String normalizarObjetoNoEstricto(String objeto) {
        if (objeto == null) {
            return "";
        }
        String bloque = objeto.trim();
        if (Normalizador.esVacio(bloque)) {
            return "";
        }
        if (!bloque.startsWith("{")) {
            bloque = "{" + bloque;
        }
        if (!bloque.endsWith("}")) {
            bloque = bloque + "}";
        }
        return bloque;
    }

    private List<String> extraerObjetosNoEstrictos(String bloqueHallazgos) {
        List<String> objetos = new ArrayList<>();
        if (Normalizador.esVacio(bloqueHallazgos)) {
            return objetos;
        }
        int inicioObjeto = -1;
        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = 0; i < bloqueHallazgos.length(); i++) {
            char c = bloqueHallazgos.charAt(i);
            if (escapado) {
                escapado = false;
                continue;
            }
            if (c == '\\') {
                escapado = true;
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
                continue;
            }
            if (enComillas) {
                continue;
            }
            if (c == '{') {
                if (profundidad == 0) {
                    inicioObjeto = i;
                }
                profundidad++;
            } else if (c == '}') {
                profundidad--;
                if (profundidad == 0 && inicioObjeto >= 0) {
                    objetos.add(bloqueHallazgos.substring(inicioObjeto, i + 1));
                    inicioObjeto = -1;
                }
            }
        }
        if (objetos.isEmpty()) {
            String[] partes = bloqueHallazgos.split("\\}\\s*,\\s*\\{");
            for (String parte : partes) {
                if (Normalizador.noEsVacio(parte)) {
                    objetos.add(parte);
                }
            }
        }
        return objetos;
    }

    private String etiquetaEvidencia() {
        return "en".equalsIgnoreCase(idiomaUi) ? "Evidence" : "Evidencia";
    }

    private String limpiarBloquesMarkdownJson(String contenido) {
        String limpio = contenido != null ? contenido.trim() : "";
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(?:json)?\\s*", "");
            limpio = limpio.replaceFirst("\\s*```\\s*$", "");
        }
        return limpio.trim();
    }

    /**
     * Sanitiza comillas escapadas incorrectamente por el LLM.
     * El LLM genera: "evidencia": "valor con "texto" dentro"
     * sin escapar las comillas internas. Esto rompe el JSON.
     * 
     * El LLM también genera: "evidencia": "valor con \"texto\" dentro"
     * donde el backslash se interpreta incorrectamente como literal.
     */
    private String sanitizarComillasEscapadas(String contenido) {
        if (Normalizador.esVacio(contenido)) {
            return contenido;
        }

        // Solo procesar si contiene estructura JSON
        if (!contenido.contains("{\"") && !contenido.contains("[{")) {
            return contenido;
        }

        // Usar ReparadorJson para reparar comillas en campos evidencia
        String resultado = ReparadorJson.repararJson(contenido);
        
        // Si ReparadorJson no pudo reparar, intentamos nuestro parser manual
        if (resultado == null) {
            resultado = reparadorManualComillas(contenido);
        }
        
        return resultado != null ? resultado : contenido;
    }
    
    /**
     * Parser manual para reparar comillas sin escapar en valores JSON.
     * Repara el caso específico donde el LLM genera: "campo": "valor con "texto" dentro"
     */
    private String reparadorManualComillas(String texto) {
        if (Normalizador.esVacio(texto)) {
            return texto;
        }
        
        StringBuilder resultado = new StringBuilder(texto.length() + 64);
        int i = 0;
        
        while (i < texto.length()) {
            char c = texto.charAt(i);
            
            if (c == '"') {
                // Inicio de string o final - verificar contexto
                // Buscar si hay un : antes (indicador de campo)
                boolean esInicioCampo = false;
                for (int j = i - 1; j >= 0; j--) {
                    char p = texto.charAt(j);
                    if (p == ':') {
                        esInicioCampo = true;
                        break;
                    } else if (p != ' ' && p != '\t') {
                        break;
                    }
                }
                
                if (esInicioCampo) {
                    // Encontrar el final de este valor
                    int fin = encontrarFinString(texto, i + 1);
                    if (fin == -1) {
                        resultado.append(c);
                        i++;
                        continue;
                    }
                    
                    // Extraer el valor y repararlo
                    String valor = texto.substring(i + 1, fin);
                    String valorReparado = reparadorComillasEnValor(valor);
                    
                    resultado.append('"').append(valorReparado).append('"');
                    i = fin + 1;
                } else {
                    resultado.append(c);
                    i++;
                }
            } else {
                resultado.append(c);
                i++;
            }
        }
        
        return resultado.toString();
    }
    
    /**
     * Encuentra el final de un string JSON (la comilla de cierre no escapada)
     */
    private int encontrarFinString(String texto, int inicio) {
        boolean escapado = false;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (escapado) {
                escapado = false;
                continue;
            }
            if (c == '\\') {
                escapado = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Repara comillas sin escapar dentro de un valor string.
     * El LLM genera: valor con "texto" dentro -> valor con \"texto\" dentro
     */
    private String reparadorComillasEnValor(String valor) {
        if (Normalizador.esVacio(valor)) {
            return valor;
        }
        
        StringBuilder resultado = new StringBuilder(valor.length() + 32);
        boolean dentroDeTag = false;
        
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            
            if (c == '<') {
                dentroDeTag = true;
                resultado.append(c);
            } else if (c == '>') {
                dentroDeTag = false;
                resultado.append(c);
            } else if (c == '"' && dentroDeTag) {
                // Comilla sin escapar dentro de tag HTML - escapar
                resultado.append("\\\"");
            } else {
                resultado.append(c);
            }
        }
        
        return resultado.toString();
    }

    private void agregarHallazgoNormalizado(List<Hallazgo> destino,
                                            String tituloRaw,
                                            String descripcionRaw,
                                            String severidadRaw,
                                            String confianzaRaw,
                                            String evidenciaRaw,
                                            SolicitudAnalisis solicitud) {
        if (destino == null || solicitud == null) {
            return;
        }
        
        final int CAPACIDAD_EXTRA_BUILDER = 32;
        String titulo = normalizarTextoSimple(tituloRaw, tituloPorDefecto());
        String descripcion = normalizarTextoSimple(descripcionRaw, "");
        String evidencia = normalizarTextoSimple(evidenciaRaw, "");
        if (Normalizador.esVacio(descripcion)) {
            descripcion = evidencia;
        } else if (Normalizador.noEsVacio(evidencia) && !descripcion.contains(evidencia)) {
            StringBuilder sb = new StringBuilder(
                    descripcion.length() + evidencia.length() + CAPACIDAD_EXTRA_BUILDER);
            sb.append(descripcion).append("\n").append(etiquetaEvidencia()).append(": ").append(evidencia);
            descripcion = sb.toString();
        }
        if (Normalizador.esVacio(descripcion)) {
            descripcion = descripcionPorDefecto();
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

    private String tituloPorDefecto() {
        return I18nUI.tr("Sin título", "Untitled");
    }

    private String descripcionPorDefecto() {
        return I18nUI.tr("Sin descripción", "No description");
    }

    private List<Hallazgo> parsearTextoPlano(String contenido, SolicitudAnalisis solicitud) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (Normalizador.esVacio(contenido)) {
            return hallazgos;
        }

        try {
            String[] lineas = contenido.split("\n");
            StringBuilder descripcion = new StringBuilder();
            String severidad = Hallazgo.SEVERIDAD_INFO;
            String confianza = Hallazgo.CONFIANZA_BAJA;
            final int MAX_LONGITUD_TITULO_RESUMIDO = 30;
            final java.util.regex.Pattern PATRON_ETIQUETA_TITULO = java.util.regex.Pattern.compile("(?i)(título:|title:)");
            final java.util.regex.Pattern PATRON_ETIQUETA_SEVERIDAD = java.util.regex.Pattern.compile("(?i)(severidad:|severity:)");
            final java.util.regex.Pattern PATRON_ETIQUETA_DESCRIPCION = java.util.regex.Pattern
                    .compile("(?i)(vulnerabilidad|descripcion:|description:)");

            for (String linea : lineas) {
                String lineaNormalizada = linea.trim();
                String lineaLower = lineaNormalizada.toLowerCase();

                if (contieneAlguno(lineaLower, "título:", "title:")) {
                    if (descripcion.length() > 0) {
                        agregarHallazgoDesdeDescripcion(hallazgos, descripcion.toString(), severidad, confianza, solicitud);
                        descripcion.setLength(0);
                    }

                    descripcion
                            .append(PATRON_ETIQUETA_TITULO.matcher(lineaNormalizada).replaceAll("").trim())
                            .append(" - ");
                } else if (contieneAlguno(lineaLower, "severidad:", "severity:")) {
                    severidad = extraerSeveridadTexto(lineaNormalizada);
                } else if (contieneAlguno(lineaLower, "vulnerabilidad", "descripcion:", "description:")) {
                    if (descripcion.length() > 0) {
                        descripcion.append("\n");
                    }
                    descripcion.append(PATRON_ETIQUETA_DESCRIPCION.matcher(lineaNormalizada).replaceAll("").trim());
                } else if (lineaNormalizada.length() > 10) {
                    if (descripcion.length() > 0) {
                        descripcion.append("\n");
                    }
                    descripcion.append(lineaNormalizada);
                }
            }

            if (descripcion.length() > 0) {
                agregarHallazgoDesdeDescripcion(hallazgos, descripcion.toString(), severidad, confianza, solicitud);
            }

            if (hallazgos.isEmpty() && contenido.trim().length() > 20) {
                String tituloContenido = contenido.trim();
                if (tituloContenido.length() > MAX_LONGITUD_TITULO_RESUMIDO) {
                    tituloContenido = tituloContenido.substring(0, MAX_LONGITUD_TITULO_RESUMIDO) + "...";
                }
                hallazgos.add(new Hallazgo(
                        solicitud.obtenerUrl(),
                        tituloContenido,
                        contenido.trim(),
                        Hallazgo.SEVERIDAD_INFO,
                        Hallazgo.CONFIANZA_BAJA,
                        solicitud.obtenerSolicitudHttp()));
            }

        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error parseando texto plano"), e);
        }

        return hallazgos;
    }

    private boolean contieneAlguno(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    private String extraerSeveridadTexto(String linea) {
        String texto = linea.toLowerCase();
        if (texto.contains("critical") || texto.contains("crítica")) {
            return Hallazgo.SEVERIDAD_CRITICAL;
        }
        if (texto.contains("high") || texto.contains("alta")) {
            return Hallazgo.SEVERIDAD_HIGH;
        }
        if (texto.contains("medium") || texto.contains("media")) {
            return Hallazgo.SEVERIDAD_MEDIUM;
        }
        if (texto.contains("low") || texto.contains("baja")) {
            return Hallazgo.SEVERIDAD_LOW;
        }
        return Hallazgo.SEVERIDAD_INFO;
    }

    private void agregarHallazgoDesdeDescripcion(List<Hallazgo> destino,
                                                 String descripcion,
                                                 String severidad,
                                                 String confianza,
                                                 SolicitudAnalisis solicitud) {
        String titulo = I18nUI.tr("Hallazgo Plano", "Plain Finding");
        String desc = descripcion.trim();
        if (desc.startsWith("- ")) {
            desc = desc.substring(2);
        }
        if (Normalizador.noEsVacio(desc)) {
            destino.add(new Hallazgo(
                    solicitud.obtenerUrl(),
                    titulo,
                    desc,
                    severidad,
                    confianza,
                    solicitud.obtenerSolicitudHttp()));
        }
    }

    private void rastrear(String mensaje) {
        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("[RASTREO] " + mensaje));
    }

    public static class ParseExceptionAI extends RuntimeException {
        public ParseExceptionAI(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
