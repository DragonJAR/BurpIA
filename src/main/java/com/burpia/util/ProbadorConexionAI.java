package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;

import javax.swing.*;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Clase para probar la conexión con APIs de IA.
 * OPTIMIZACIÓN v1.0: Usa OkHttp en lugar de HttpURLConnection nativo para consistencia.
 * OPTIMIZACIÓN v1.0: Usa ParserRespuestasAI para parsing robusto de JSON.
 */
public class ProbadorConexionAI {
    private final ConfiguracionAPI config;
    private final OkHttpClient clienteHttp;

    public ProbadorConexionAI(ConfiguracionAPI config) {
        this.config = config;
        // Usar OkHttp para consistencia con AnalizadorAI
        this.clienteHttp = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(config.obtenerTiempoEsperaAI(), TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public ResultadoPrueba probarConexion() {
        try {
            java.util.Map<String, String> errores = config.validar();
            if (!errores.isEmpty()) {
                StringBuilder sb = new StringBuilder("Errores de configuracion:\n");
                for (java.util.Map.Entry<String, String> entry : errores.entrySet()) {
                    sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                return new ResultadoPrueba(false, sb.toString(), null);
            }

            // Realizar solicitud de prueba
            String respuesta = realizarSolicitudPrueba();

            if (respuesta != null && !respuesta.isEmpty()) {
                // Analizar respuesta segun proveedor
                String mensaje = analizarRespuesta(respuesta);
                return new ResultadoPrueba(true, mensaje, respuesta);
            } else {
                return new ResultadoPrueba(false, "No se recibio respuesta del servidor", null);
            }

        } catch (Exception e) {
            return new ResultadoPrueba(false, "Error de conexion: " + e.getMessage(), null);
        }
    }

    private String realizarSolicitudPrueba() throws IOException {
        String urlApi = config.obtenerUrlApi();
        String proveedor = config.obtenerProveedorAI();
        String claveApi = config.obtenerClaveApi();

        // Construir cuerpo de solicitud simple
        String cuerpo = construirCuerpoPrueba(proveedor);

        // Construir solicitud HTTP con OkHttp
        Request.Builder solicitudBuilder = new Request.Builder()
                .url(urlApi)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        cuerpo
                ));

        // Agregar headers de autenticación según proveedor
        switch (proveedor) {
            case "OpenAI":
                solicitudBuilder.addHeader("Authorization", "Bearer " + claveApi);
                break;
            case "Claude":
                solicitudBuilder.addHeader("x-api-key", claveApi);
                solicitudBuilder.addHeader("anthropic-version", "2023-06-01");
                break;
            case "Gemini":
                solicitudBuilder.addHeader("x-goog-api-key", claveApi);
                break;
            case "Z.ai":
            case "minimax":
                solicitudBuilder.addHeader("Authorization", "Bearer " + claveApi);
                break;
            case "Ollama":
                // Ollama no requiere API key
                break;
        }

        Request solicitud = solicitudBuilder.build();

        // Ejecutar solicitud
        try (Response respuesta = clienteHttp.newCall(solicitud).execute()) {
            if (!respuesta.isSuccessful()) {
                String cuerpoError = respuesta.body() != null ? respuesta.body().string() : "sin cuerpo";
                throw new IOException("HTTP " + respuesta.code() + ": " + cuerpoError);
            }

            return respuesta.body().string();
        }
    }

    private String construirCuerpoPrueba(String proveedor) {
        StringBuilder cuerpo = new StringBuilder();
        String mensajePrueba = "Hola, escribe OK";

        switch (proveedor) {
            case "OpenAI":
            case "Z.ai":
            case "minimax":
                cuerpo.append("{");
                cuerpo.append("\"model\": \"").append(config.obtenerModelo()).append("\",");
                cuerpo.append("\"messages\": [{\"role\": \"user\", \"content\": \"").append(mensajePrueba).append("\"}],");
                cuerpo.append("\"max_tokens\": 50");
                cuerpo.append("}");
                break;

            case "Claude":
                cuerpo.append("{");
                cuerpo.append("\"model\": \"").append(config.obtenerModelo()).append("\",");
                cuerpo.append("\"max_tokens\": 50,");
                cuerpo.append("\"messages\": [{\"role\": \"user\", \"content\": \"").append(mensajePrueba).append("\"}]");
                cuerpo.append("}");
                break;

            case "Gemini":
                cuerpo.append("{");
                cuerpo.append("\"contents\": [{\"parts\": [{\"text\": \"").append(mensajePrueba).append("\"}]}]");
                cuerpo.append("}");
                break;

            case "Ollama":
                cuerpo.append("{");
                cuerpo.append("\"model\": \"").append(config.obtenerModelo()).append("\",");
                cuerpo.append("\"stream\": false,");
                cuerpo.append("\"messages\": [{\"role\": \"user\", \"content\": \"").append(mensajePrueba).append("\"}]");
                cuerpo.append("}");
                break;
        }

        return cuerpo.toString();
    }

    /**
     * Analiza la respuesta de la API de IA.
     * OPTIMIZACIÓN v1.0: Usa ParserRespuestasAI para parsing robusto de JSON.
     *
     * @param respuesta JSON crudo de la respuesta
     * @return Mensaje formateado para mostrar al usuario
     */
    private String analizarRespuesta(String respuesta) {
        String proveedor = config.obtenerProveedorAI();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("✅ Conexión exitosa a ").append(proveedor).append("\n\n");

        // OPTIMIZACIÓN v1.0: Usar ParserRespuestasAI en lugar de parsing manual
        String contenidoRespuesta = ParserRespuestasAI.extraerContenido(respuesta, proveedor);
        boolean respuestaValida = ParserRespuestasAI.validarRespuestaPrueba(contenidoRespuesta);

        mensaje.append("📋 Configuración:\n");
        mensaje.append("   Modelo: ").append(config.obtenerModelo()).append("\n");
        mensaje.append("   URL base: ").append(ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi())).append("\n");
        mensaje.append("   Endpoint probado: ").append(config.obtenerUrlApi()).append("\n\n");

        if (respuestaValida) {
            mensaje.append("💬 Mensaje enviado: \"Hola, escribe OK\"\n\n");
            mensaje.append("✅ Respuesta del modelo:\n");
            if (contenidoRespuesta.length() > 100) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
            mensaje.append("\n\n✅ ¡El modelo respondió correctamente!");
            mensaje.append("\n(Respuesta aceptada: contiene \"OK\" o \"Hola\")");
        } else {
            mensaje.append("💬 Mensaje enviado: \"Hola, escribe OK\"\n\n");
            if (!contenidoRespuesta.isEmpty()) {
                mensaje.append("⚠️ La respuesta NO contiene \"OK\" ni \"Hola\"\n\n");
                mensaje.append("❌ Respuesta recibida:\n");
                if (contenidoRespuesta.length() > 100) {
                    mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
                } else {
                    mensaje.append("   ").append(contenidoRespuesta);
                }
                mensaje.append("\n\n⚠️ El modelo respondió pero NO escribió \"OK\" ni \"Hola\" como se esperaba.\n");
                mensaje.append("   Esto puede indicar un problema con el modelo o la configuración.");
            } else {
                mensaje.append("⚠️ No se pudo extraer el contenido de la respuesta\n");
                mensaje.append("   La conexión fue exitosa pero el formato de respuesta no es el esperado.\n");
                mensaje.append("   Respuesta cruda (primeros 200 caracteres):\n");
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

    public void mostrarDialogoPrueba(JFrame padre) {
        // Mostrar dialogo de carga
        JDialog dialogoCarga = new JDialog(padre, "Probando Conexion", true);
        JLabel etiqueta = new JLabel("Conectando a " + config.obtenerProveedorAI() + "...", SwingConstants.CENTER);
        dialogoCarga.add(etiqueta);
        dialogoCarga.setSize(300, 100);
        dialogoCarga.setLocationRelativeTo(padre);

        // Ejecutar prueba en background
        SwingWorker<ResultadoPrueba, Void> worker = new SwingWorker<ResultadoPrueba, Void>() {
            @Override
            protected ResultadoPrueba doInBackground() throws Exception {
                return probarConexion();
            }

            @Override
            protected void done() {
                dialogoCarga.dispose();
                try {
                    ResultadoPrueba resultado = get();
                    int tipoMensaje = resultado.exito ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
                    JOptionPane.showMessageDialog(
                        padre,
                        resultado.mensaje,
                        resultado.exito ? "Conexion Exitosa" : "Error de Conexion",
                        tipoMensaje
                    );
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        padre,
                        "Error durante la prueba: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
        dialogoCarga.setVisible(true);
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
}
