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
                StringBuilder sb = new StringBuilder(I18nUI.tr("Errores de configuracion:\n", "Configuration errors:\n"));
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
                return new ResultadoPrueba(false, I18nUI.tr("No se recibio respuesta del servidor", "No response was received from server"), null);
            }

        } catch (Exception e) {
            return new ResultadoPrueba(false, I18nUI.tr("Error de conexion: ", "Connection error: ") + e.getMessage(), null);
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
        mensaje.append(I18nUI.tr("✅ Conexion exitosa a ", "✅ Successful connection to ")).append(proveedor).append("\n\n");

        String contenidoRespuesta = ParserRespuestasAI.extraerContenido(respuesta, proveedor);
        boolean respuestaValida = ParserRespuestasAI.validarRespuestaPrueba(contenidoRespuesta);
        boolean conexionValida = ParserRespuestasAI.validarRespuestaConexion(contenidoRespuesta);

        mensaje.append(I18nUI.tr("📋 Configuracion:\n", "📋 Configuration:\n"));
        mensaje.append(I18nUI.tr("   Modelo: ", "   Model: ")).append(modeloUsado).append("\n");
        mensaje.append(I18nUI.tr("   URL base: ", "   Base URL: ")).append(ConfiguracionAPI.extraerUrlBase(endpointProbado)).append("\n");
        mensaje.append(I18nUI.tr("   Endpoint probado: ", "   Tested endpoint: ")).append(endpointProbado).append("\n\n");
        if (advertenciaModelo != null && !advertenciaModelo.isEmpty()) {
            mensaje.append("ℹ️ ").append(advertenciaModelo).append("\n\n");
        }

        if (respuestaValida) {
            mensaje.append(I18nUI.tr("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n",
                "💬 Sent message: \"Reply exactly with OK\"\n\n"));
            mensaje.append(I18nUI.tr("✅ Respuesta del modelo:\n", "✅ Model response:\n"));
            if (contenidoRespuesta.length() > 100) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
            mensaje.append(I18nUI.tr("\n\n✅ ¡El modelo respondio correctamente!",
                "\n\n✅ Model responded correctly!"));
            mensaje.append(I18nUI.tr("\n(Respuesta aceptada: contiene \"OK\" o \"Hola\")",
                "\n(Accepted response: contains \"OK\" or \"Hello\")"));
        } else if (conexionValida) {
            mensaje.append(I18nUI.tr("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n",
                "💬 Sent message: \"Reply exactly with OK\"\n\n"));
            mensaje.append(I18nUI.tr("✅ El proveedor respondio y el contenido fue extraido correctamente.\n",
                "✅ Provider responded and content was extracted successfully.\n"));
            mensaje.append(I18nUI.tr("ℹ️ La respuesta no incluyo literalmente \"OK\", pero la conexion es valida.\n\n",
                "ℹ️ Response did not include literal \"OK\", but connection is valid.\n\n"));
            mensaje.append(I18nUI.tr("Respuesta del modelo:\n", "Model response:\n"));
            if (contenidoRespuesta.length() > 150) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 150)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
        } else {
            mensaje.append(I18nUI.tr("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n",
                "💬 Sent message: \"Reply exactly with OK\"\n\n"));
            if (!contenidoRespuesta.isEmpty()) {
                mensaje.append(I18nUI.tr("⚠️ La respuesta NO contiene \"OK\" ni \"Hola\"\n\n",
                    "⚠️ Response does NOT contain \"OK\" or \"Hello\"\n\n"));
                mensaje.append(I18nUI.tr("❌ Respuesta recibida:\n", "❌ Received response:\n"));
                if (contenidoRespuesta.length() > 100) {
                    mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
                } else {
                    mensaje.append("   ").append(contenidoRespuesta);
                }
                mensaje.append(I18nUI.tr("\n\n⚠️ El modelo respondio pero no cumple el formato esperado.",
                    "\n\n⚠️ Model responded but did not match the expected format."));
            } else {
                mensaje.append(I18nUI.tr("⚠️ No se pudo extraer el contenido de la respuesta\n",
                    "⚠️ Could not extract response content\n"));
                mensaje.append(I18nUI.tr("   La conexion fue exitosa pero el formato de respuesta no es el esperado.\n",
                    "   Connection succeeded but response format is not expected.\n"));
                mensaje.append(I18nUI.tr("   Respuesta cruda (primeros 200 caracteres):\n",
                    "   Raw response (first 200 characters):\n"));
                respuesta = respuesta.replaceAll("\\s+", " ");
                if (respuesta.length() > 200) {
                    mensaje.append("   ").append(respuesta.substring(0, 200)).append("...");
                } else {
                    mensaje.append("   ").append(respuesta);
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
