package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;

import javax.swing.*;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProbadorConexionAI {
    private final ConfiguracionAPI config;
    private final OkHttpClient clienteHttp;

    public ProbadorConexionAI(ConfiguracionAPI config) {
        this.config = config;
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

            String endpointProbado = ConfiguracionAPI.construirUrlApiProveedor(
                config.obtenerProveedorAI(),
                config.obtenerUrlApi(),
                config.obtenerModelo()
            );
            String respuesta = realizarSolicitudPrueba(endpointProbado);

            if (respuesta != null && !respuesta.isEmpty()) {
                String mensaje = analizarRespuesta(respuesta, endpointProbado);
                return new ResultadoPrueba(true, mensaje, respuesta);
            } else {
                return new ResultadoPrueba(false, "No se recibio respuesta del servidor", null);
            }

        } catch (Exception e) {
            return new ResultadoPrueba(false, "Error de conexion: " + e.getMessage(), null);
        }
    }

    private String realizarSolicitudPrueba(String endpointProbado) throws IOException {
        String proveedor = config.obtenerProveedorAI();
        String claveApi = config.obtenerClaveApi();
        String cuerpo = construirCuerpoPrueba(proveedor);

        Request.Builder solicitudBuilder = new Request.Builder()
                .url(endpointProbado)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(
                        cuerpo,
                        MediaType.parse("application/json")
                ));

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
        String mensajePrueba = "Responde exactamente con OK";

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

    private String analizarRespuesta(String respuesta, String endpointProbado) {
        String proveedor = config.obtenerProveedorAI();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("✅ Conexión exitosa a ").append(proveedor).append("\n\n");

        String contenidoRespuesta = ParserRespuestasAI.extraerContenido(respuesta, proveedor);
        boolean respuestaValida = ParserRespuestasAI.validarRespuestaPrueba(contenidoRespuesta);
        boolean conexionValida = ParserRespuestasAI.validarRespuestaConexion(contenidoRespuesta);

        mensaje.append("📋 Configuración:\n");
        mensaje.append("   Modelo: ").append(config.obtenerModelo()).append("\n");
        mensaje.append("   URL base: ").append(ConfiguracionAPI.extraerUrlBase(endpointProbado)).append("\n");
        mensaje.append("   Endpoint probado: ").append(endpointProbado).append("\n\n");

        if (respuestaValida) {
            mensaje.append("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n");
            mensaje.append("✅ Respuesta del modelo:\n");
            if (contenidoRespuesta.length() > 100) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
            mensaje.append("\n\n✅ ¡El modelo respondió correctamente!");
            mensaje.append("\n(Respuesta aceptada: contiene \"OK\" o \"Hola\")");
        } else if (conexionValida) {
            mensaje.append("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n");
            mensaje.append("✅ El proveedor respondió y el contenido fue extraído correctamente.\n");
            mensaje.append("ℹ️ La respuesta no incluyó literalmente \"OK\", pero la conexión es válida.\n\n");
            mensaje.append("Respuesta del modelo:\n");
            if (contenidoRespuesta.length() > 150) {
                mensaje.append("   ").append(contenidoRespuesta.substring(0, 150)).append("...");
            } else {
                mensaje.append("   ").append(contenidoRespuesta);
            }
        } else {
            mensaje.append("💬 Mensaje enviado: \"Responde exactamente con OK\"\n\n");
            if (!contenidoRespuesta.isEmpty()) {
                mensaje.append("⚠️ La respuesta NO contiene \"OK\" ni \"Hola\"\n\n");
                mensaje.append("❌ Respuesta recibida:\n");
                if (contenidoRespuesta.length() > 100) {
                    mensaje.append("   ").append(contenidoRespuesta.substring(0, 100)).append("...");
                } else {
                    mensaje.append("   ").append(contenidoRespuesta);
                }
                mensaje.append("\n\n⚠️ El modelo respondió pero no cumple el formato esperado.");
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
