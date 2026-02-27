package com.burpia.util;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProbadorConexionAI {
    private final ConfiguracionAPI config;
    private final OkHttpClient clienteHttp;

    public ProbadorConexionAI(ConfiguracionAPI config) {
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

            if (respuesta != null && !respuesta.isEmpty()) {
                String mensaje = analizarRespuesta(respuesta, endpointFinal, resultadoHttp.modeloUsado, resultadoHttp.advertencia);
                return new ResultadoPrueba(true, mensaje, respuesta);
            } else {
                return new ResultadoPrueba(false, I18nUI.Conexion.SIN_RESPUESTA(), null);
            }

        } catch (Exception e) {
            return new ResultadoPrueba(false, I18nUI.Conexion.ERROR_CONEXION() + e.getMessage(), null);
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

            String cuerpoError = respuesta.body() != null ? respuesta.body().string() : "sin cuerpo";
            throw new IOException("HTTP " + respuesta.code() + ": " + cuerpoError);
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
        if (advertenciaModelo != null && !advertenciaModelo.isEmpty()) {
            mensaje.append("ℹ️ ").append(advertenciaModelo).append("\n\n");
        }

        if (respuestaValida) {
            mensaje.append(I18nUI.Conexion.MSG_ENVIADO());
            mensaje.append(I18nUI.Conexion.RESPUESTA_MODELO());
            if (contenidoRespuesta.length() > 100) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
            mensaje.append(I18nUI.Conexion.RESPUESTA_CORRECTA());
            mensaje.append(I18nUI.Conexion.RESPUESTA_ACEPTADA());
        } else if (conexionValida) {
            mensaje.append(I18nUI.Conexion.MSG_ENVIADO());
            mensaje.append(I18nUI.Conexion.PROVEEDOR_RESPONDIO());
            mensaje.append(I18nUI.Conexion.CONEXION_VALIDA_SIN_OK());
            mensaje.append(I18nUI.Conexion.MODELO_RESPONDE());
            if (contenidoRespuesta.length() > 150) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 150)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
        } else {
            mensaje.append(I18nUI.Conexion.MSG_ENVIADO());
            if (!contenidoRespuesta.isEmpty()) {
                mensaje.append(I18nUI.Conexion.RESPUESTA_SIN_OK());
                mensaje.append(I18nUI.Conexion.RESPUESTA_RECIBIDA());
                if (contenidoRespuesta.length() > 100) {
                    mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
                } else {
                    mensaje.append("   ").append(contenidoRespuesta);
                }
                mensaje.append(I18nUI.Conexion.RESPUESTA_FORMATO_INCORRECTO());
            } else {
                mensaje.append(I18nUI.Conexion.ERROR_EXTRAER_CONTENIDO());
                mensaje.append(I18nUI.Conexion.EXITO_FORMATO_INCORRECTO());
                mensaje.append(I18nUI.Conexion.RESPUESTA_CRUDA());
                String respuestaNormalizada = respuesta.replaceAll("\\s+", " ");
                if (respuestaNormalizada.length() > 200) {
                    mensaje.append("   ").append(respuestaNormalizada.substring(0, 200)).append("...");
                } else {
                    mensaje.append("   ").append(respuestaNormalizada);
                }
            }
        }

        return mensaje.toString();
    }

    public static class ResultadoPrueba {
        public final boolean exito;
        public final String mensaje;
        public final String respuestaRaw;

        public ResultadoPrueba(boolean exito, String mensaje, String respuestaRaw) {
            this.exito = exito;
            this.mensaje = mensaje;
            this.respuestaRaw = respuestaRaw;
        }
    }

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
