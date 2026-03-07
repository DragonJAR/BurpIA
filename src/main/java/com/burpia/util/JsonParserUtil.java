package com.burpia.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilidades para parsing de JSON de respuestas AI.
 * 
 * <p>Clase thread-safe sin estado mutable. Todos los métodos son estáticos.</p>
 * 
 * <p>Centraliza la lógica de extracción de campos de hallazgos desde estructuras
 * JSON variables devueltas por diferentes proveedores AI.</p>
 */
public final class JsonParserUtil {

    private JsonParserUtil() {
    }

    public static final String[] CAMPOS_TITULO = { "titulo", "title", "name", "nombre" };
    public static final String[] CAMPOS_DESCRIPCION = { "descripcion", "description", "hallazgo", "finding", "detalle", "details" };
    public static final String[] CAMPOS_SEVERIDAD = { "severidad", "severity", "risk", "impacto" };
    public static final String[] CAMPOS_CONFIANZA = { "confianza", "confidence", "certainty", "certeza" };
    public static final String[] CAMPOS_EVIDENCIA = { "evidencia", "evidence", "proof", "indicator" };
    public static final String[] CAMPOS_HALLAZGOS = { "hallazgos", "findings", "issues", "vulnerabilidades" };
    public static final String[] CAMPOS_TEXTO = { "text", "texto", "content", "mensaje", "message", "value", "descripcion", "description" };

    private static final int PROFUNDIDAD_MAXIMA_JSON = 5;

    /**
     * Extrae objetos JSON que representan hallazgos desde una estructura JSON arbitraria.
     * 
     * <p>Busca hallazgos en:</p>
     * <ul>
     *   <li>Array raíz: cada elemento JsonObject se considera hallazgo</li>
     *   <li>Campo de hallazgos: busca en campos especificados (hallazgos, findings, etc.)</li>
     *   <li>Objeto raíz: si parece un hallazgo, se incluye directamente</li>
     * </ul>
     * 
     * @param raiz elemento JSON raíz (puede ser null)
     * @param camposHallazgos nombres de campos donde buscar arrays de hallazgos
     * @return lista de JsonObject con hallazgos, nunca null
     */
    public static List<JsonObject> extraerObjetosHallazgos(JsonElement raiz, String[] camposHallazgos) {
        List<JsonObject> objetos = new ArrayList<>();
        if (raiz == null || raiz.isJsonNull()) {
            return objetos;
        }
        if (raiz.isJsonArray()) {
            anexarObjetosDesdeArreglo(raiz.getAsJsonArray(), objetos);
            return objetos;
        }
        if (!raiz.isJsonObject()) {
            return objetos;
        }
        JsonObject objetoRaiz = raiz.getAsJsonObject();
        JsonElement campoHallazgos = extraerPrimerCampoExistente(objetoRaiz, camposHallazgos);
        if (campoHallazgos != null) {
            if (campoHallazgos.isJsonArray()) {
                anexarObjetosDesdeArreglo(campoHallazgos.getAsJsonArray(), objetos);
            } else if (campoHallazgos.isJsonObject()) {
                objetos.add(campoHallazgos.getAsJsonObject());
            }
            return objetos;
        }
        if (pareceObjetoHallazgo(objetoRaiz, CAMPOS_DESCRIPCION, CAMPOS_EVIDENCIA)) {
            objetos.add(objetoRaiz);
        }
        return objetos;
    }

    /**
     * Extrae el primer campo existente de un JsonObject entre varios nombres posibles.
     * 
     * @param objeto objeto JSON fuente
     * @param campos nombres de campos a buscar en orden de prioridad
     * @return valor del primer campo encontrado, o null si ninguno existe
     */
    public static JsonElement extraerPrimerCampoExistente(JsonObject objeto, String... campos) {
        if (objeto == null || campos == null) {
            return null;
        }
        for (String campo : campos) {
            if (Normalizador.esVacio(campo)) {
                continue;
            }
            JsonElement valor = objeto.get(campo);
            if (valor != null && !valor.isJsonNull()) {
                return valor;
            }
        }
        return null;
    }

    /**
     * Extrae el valor de texto del primer campo existente entre varios nombres posibles.
     * 
     * <p>Convierte automáticamente tipos complejos (arrays, objetos) a texto
     * usando {@link #extraerCampoComoTexto(JsonElement, int)}.</p>
     * 
     * @param objeto objeto JSON fuente
     * @param campos nombres de campos a buscar en orden de prioridad
     * @return valor de texto del primer campo encontrado con contenido, o cadena vacía
     */
    public static String extraerCampoFlexible(JsonObject objeto, String... campos) {
        if (objeto == null || campos == null) {
            return "";
        }
        for (String campo : campos) {
            if (Normalizador.esVacio(campo)) {
                continue;
            }
            String valor = extraerCampoComoTexto(objeto.get(campo), 0);
            if (Normalizador.noEsVacio(valor)) {
                return valor;
            }
        }
        return "";
    }

    /**
     * Determina si un objeto JSON parece representar un hallazgo de seguridad.
     * 
     * <p>Un objeto parece hallazgo si tiene contenido en campos de descripción
     * o evidencia.</p>
     * 
     * @param objeto objeto JSON a evaluar
     * @param camposDescripcion nombres de campos de descripción
     * @param camposEvidencia nombres de campos de evidencia
     * @return true si el objeto parece un hallazgo
     */
    public static boolean pareceObjetoHallazgo(JsonObject objeto, String[] camposDescripcion, String[] camposEvidencia) {
        if (objeto == null) {
            return false;
        }
        return Normalizador.noEsVacio(extraerCampoFlexible(objeto, camposDescripcion))
                || Normalizador.noEsVacio(extraerCampoFlexible(objeto, camposEvidencia));
    }

    /**
     * Convierte un elemento JSON a texto de forma recursiva.
     * 
     * <p>Maneja:</p>
     * <ul>
     *   <li>Primitivos: retorna valor como string</li>
     *   <li>Arrays: concatena elementos con espacio</li>
     *   <li>Objetos: busca campos de texto conocidos, luego itera propiedades</li>
     * </ul>
     * 
     * @param elemento elemento JSON a convertir
     * @param profundidad nivel actual de recursión (iniciar con 0)
     * @return texto extraído, o cadena vacía si no aplica
     */
    public static String extraerCampoComoTexto(JsonElement elemento, int profundidad) {
        if (elemento == null || elemento.isJsonNull() || profundidad > PROFUNDIDAD_MAXIMA_JSON) {
            return "";
        }
        if (elemento.isJsonPrimitive()) {
            try {
                String valor = elemento.getAsString();
                return valor != null ? valor.trim() : "";
            } catch (Exception e) {
                return "";
            }
        }
        if (elemento.isJsonArray()) {
            StringBuilder acumulado = new StringBuilder();
            for (JsonElement item : elemento.getAsJsonArray()) {
                anexarTexto(acumulado, extraerCampoComoTexto(item, profundidad + 1));
            }
            return acumulado.toString().trim();
        }
        if (elemento.isJsonObject()) {
            JsonObject objeto = elemento.getAsJsonObject();
            for (String campoTexto : CAMPOS_TEXTO) {
                String texto = extraerCampoComoTexto(objeto.get(campoTexto), profundidad + 1);
                if (Normalizador.noEsVacio(texto)) {
                    return texto;
                }
            }
            for (Map.Entry<String, JsonElement> entry : objeto.entrySet()) {
                String texto = extraerCampoComoTexto(entry.getValue(), profundidad + 1);
                if (Normalizador.noEsVacio(texto)) {
                    return texto;
                }
            }
            return "";
        }
        return "";
    }

    private static void anexarObjetosDesdeArreglo(JsonArray arreglo, List<JsonObject> destino) {
        if (arreglo == null || destino == null) {
            return;
        }
        for (JsonElement elemento : arreglo) {
            if (elemento != null && elemento.isJsonObject()) {
                destino.add(elemento.getAsJsonObject());
            }
        }
    }

    private static void anexarTexto(StringBuilder destino, String texto) {
        if (destino == null || Normalizador.esVacio(texto)) {
            return;
        }
        if (destino.length() > 0) {
            destino.append(' ');
        }
        destino.append(texto.trim());
    }
}
