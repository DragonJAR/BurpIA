package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utilidad para probar la conexión con proveedores de IA.
 * <p>
 * Verifica que la configuración de API key, modelo y endpoint sea correcta
 * enviando una solicitud de prueba y analizando la respuesta.
 * </p>
 *
 * @see ConfiguracionAPI
 * @see ConstructorSolicitudesProveedor
 */
public class ProbadorConexionAI {
    private final ConfiguracionAPI config;
    private final OkHttpClient clienteHttp;

    /**
     * Crea un nuevo probador de conexión con la configuración especificada.
     *
     * @param config la configuración de API a probar, no puede ser {@code null}
     * @throws IllegalArgumentException si la configuración es {@code null}
     */
    public ProbadorConexionAI(ConfiguracionAPI config) {
        if (config == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_CONFIGURACION_NULA_ARGUMENTO());
        }
        this.config = config;
        int timeoutLectura = config.obtenerTiempoEsperaParaModelo(
            config.obtenerProveedorAI(),
            config.obtenerModelo()
        );
        this.clienteHttp = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeoutLectura, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Ejecuta la prueba de conexión con el proveedor de IA configurado.
     * <p>
     * Realiza las siguientes verificaciones:
     * </p>
     * <ol>
     *   <li>Valida la configuración (API key, modelo, etc.)</li>
     *   <li>Envía una solicitud de prueba simple ("Responde exactamente con OK")</li>
     *   <li>Analiza la respuesta para determinar si la conexión es exitosa</li>
     * </ol>
     *
     * @return un {@link ResultadoPrueba} con el resultado de la prueba
     */
    public ResultadoPrueba probarConexion() {
        try {
            java.util.Map<String, String> errores = config.validar();
            if (!errores.isEmpty()) {
                StringBuilder sb = new StringBuilder(I18nUI.Conexion.ERRORES_CONFIGURACION());
                for (java.util.Map.Entry<String, String> entry : errores.entrySet()) {
                    sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                return new ResultadoPrueba(false, sb.toString(), null);
            }

            ResultadoHttpPrueba resultadoHttp = realizarSolicitudPrueba();
            String respuesta = resultadoHttp.respuesta;
            String endpointFinal = resultadoHttp.endpoint;

            if (Normalizador.noEsVacio(respuesta)) {
                String mensaje = analizarRespuesta(respuesta, endpointFinal, resultadoHttp.modeloUsado, resultadoHttp.advertencia);
                return new ResultadoPrueba(true, mensaje, respuesta);
            } else {
                return new ResultadoPrueba(false, I18nUI.Conexion.SIN_RESPUESTA(), null);
            }

        } catch (Exception e) {
            return new ResultadoPrueba(false,
                    I18nUI.Conexion.ERROR_CONEXION() + describirErrorVisible(e),
                    null);
        }
    }

    private ResultadoHttpPrueba realizarSolicitudPrueba() throws IOException {
        ConstructorSolicitudesProveedor.SolicitudPreparada preparada =
            ConstructorSolicitudesProveedor.construirSolicitud(config, "Responde exactamente con OK", clienteHttp);

        Request solicitud = preparada.request;
        try (Response respuesta = clienteHttp.newCall(solicitud).execute()) {
            if (respuesta.isSuccessful()) {
                String respuestaBody = respuesta.body() != null ? respuesta.body().string() : "";
                return new ResultadoHttpPrueba(preparada.endpoint, respuestaBody, preparada.modeloUsado, preparada.advertencia);
            }

            String cuerpoError = respuesta.body() != null
                    ? respuesta.body().string()
                    : I18nUI.Conexion.DETALLE_SIN_CUERPO();
            throw new IOException(I18nUI.Conexion.DETALLE_HTTP(respuesta.code(), cuerpoError));
        }
    }

    private String analizarRespuesta(String respuesta, String endpointProbado, String modeloUsado, String advertenciaModelo) {
        String proveedor = config.obtenerProveedorAI();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append(I18nUI.Conexion.EXITO_CONEXION()).append(proveedor).append("\n\n");

        String contenidoRespuesta = ParserRespuestasAI.extraerContenido(respuesta, proveedor);
        boolean respuestaValida = ParserRespuestasAI.validarRespuestaPrueba(contenidoRespuesta);
        boolean conexionValida = ParserRespuestasAI.validarRespuestaConexion(contenidoRespuesta);

        mensaje.append(I18nUI.Conexion.INFO_CONFIGURACION());
        mensaje.append(I18nUI.Conexion.INFO_MODELO()).append(modeloUsado).append("\n");
        mensaje.append(I18nUI.Conexion.INFO_URL_BASE()).append(ConfiguracionAPI.extraerUrlBase(endpointProbado)).append("\n");
        mensaje.append(I18nUI.Conexion.INFO_ENDPOINT()).append(endpointProbado).append("\n\n");
        if (Normalizador.noEsVacio(advertenciaModelo)) {
            mensaje.append("ℹ️ ").append(advertenciaModelo).append("\n\n");
        }

        if (respuestaValida) {
            mensaje.append(I18nUI.Conexion.MSG_ENVIADO());
            mensaje.append(I18nUI.Conexion.RESPUESTA_MODELO());
            mensaje.append("   ").append(truncarTexto(contenidoRespuesta, 100));
            mensaje.append(I18nUI.Conexion.RESPUESTA_CORRECTA());
            mensaje.append(I18nUI.Conexion.RESPUESTA_ACEPTADA());
        } else if (conexionValida) {
            mensaje.append(I18nUI.Conexion.MSG_ENVIADO());
            mensaje.append(I18nUI.Conexion.PROVEEDOR_RESPONDIO());
            mensaje.append(I18nUI.Conexion.CONEXION_VALIDA_SIN_OK());
            mensaje.append(I18nUI.Conexion.MODELO_RESPONDE());
            mensaje.append("   ").append(truncarTexto(contenidoRespuesta, 150));
        } else {
            mensaje.append(I18nUI.Conexion.MSG_ENVIADO());
            if (Normalizador.noEsVacio(contenidoRespuesta)) {
                mensaje.append(I18nUI.Conexion.RESPUESTA_SIN_OK());
                mensaje.append(I18nUI.Conexion.RESPUESTA_RECIBIDA());
                mensaje.append("   ").append(truncarTexto(contenidoRespuesta, 100));
                mensaje.append(I18nUI.Conexion.RESPUESTA_FORMATO_INCORRECTO());
            } else {
                mensaje.append(I18nUI.Conexion.ERROR_EXTRAER_CONTENIDO());
                mensaje.append(I18nUI.Conexion.EXITO_FORMATO_INCORRECTO());
                mensaje.append(I18nUI.Conexion.RESPUESTA_CRUDA());
                String respuestaNormalizada = respuesta.replaceAll("\\s+", " ");
                mensaje.append("   ").append(truncarTexto(respuestaNormalizada, 200));
            }
        }

        return mensaje.toString();
    }

    /**
     * Trunca un texto a una longitud máxima, agregando "..." si se truncó.
     *
     * @param texto el texto a truncar
     * @param longitudMaxima la longitud máxima permitida
     * @return el texto truncado con "..." si era más largo, o el texto original
     */
    private static String truncarTexto(String texto, int longitudMaxima) {
        if (texto == null) {
            return "";
        }
        if (texto.length() > longitudMaxima) {
            return texto.substring(0, longitudMaxima) + "...";
        }
        return texto;
    }

    private static String describirErrorVisible(Throwable error) {
        if (error == null) {
            return I18nUI.Conexion.ERROR_DESCONOCIDO();
        }

        String mensaje = error.getMessage();
        if (Normalizador.noEsVacio(mensaje) && mensaje.startsWith("HTTP ")) {
            return mensaje;
        }
        if (Normalizador.noEsVacio(mensaje)
                && (mensaje.startsWith(I18nUI.Conexion.ERROR_CONEXION())
                || mensaje.startsWith(I18nUI.Conexion.ERROR_PRUEBA_CONEXION(""))
                || mensaje.equals(I18nUI.Conexion.SIN_RESPUESTA()))) {
            return mensaje;
        }

        Throwable causa = error.getCause();
        if (causa != null && causa != error) {
            String detalleCausa = describirErrorVisible(causa);
            if (Normalizador.noEsVacio(detalleCausa)) {
                return detalleCausa;
            }
        }

        return I18nUI.Conexion.ERROR_RED_INESPERADO(error.getClass().getSimpleName());
    }

    /**
     * Resultado de una prueba de conexión.
     * <p>
     * Contiene información sobre si la prueba fue exitosa, un mensaje descriptivo
     * y la respuesta cruda del servidor (si la hubo).
     * </p>
     */
    public static class ResultadoPrueba {
        /** Indica si la prueba de conexión fue exitosa */
        public final boolean exito;
        /** Mensaje descriptivo del resultado de la prueba */
        public final String mensaje;
        /** Respuesta cruda del servidor, puede ser {@code null} */
        public final String respuestaRaw;

        /**
         * Crea un nuevo resultado de prueba.
         *
         * @param exito si la prueba fue exitosa
         * @param mensaje mensaje descriptivo del resultado
         * @param respuestaRaw respuesta cruda del servidor, puede ser {@code null}
         */
        public ResultadoPrueba(boolean exito, String mensaje, String respuestaRaw) {
            this.exito = exito;
            this.mensaje = mensaje;
            this.respuestaRaw = respuestaRaw;
        }
    }

    /**
     * Resultado interno de una solicitud HTTP de prueba.
     */
    private static class ResultadoHttpPrueba {
        private final String endpoint;
        private final String respuesta;
        private final String modeloUsado;
        private final String advertencia;

        private ResultadoHttpPrueba(String endpoint, String respuesta, String modeloUsado, String advertencia) {
            this.endpoint = endpoint;
            this.respuesta = respuesta;
            this.modeloUsado = modeloUsado;
            this.advertencia = advertencia;
        }
    }
}
