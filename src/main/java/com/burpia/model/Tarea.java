package com.burpia.model;
import com.burpia.ui.EstilosUI;
import java.awt.Color;

public class Tarea {
    public static final String ESTADO_EN_COLA = "En Cola";
    public static final String ESTADO_ANALIZANDO = "Analizando";
    public static final String ESTADO_COMPLETADO = "Completado";
    public static final String ESTADO_ERROR = "Error";
    public static final String ESTADO_CANCELADO = "Cancelado";
    public static final String ESTADO_PAUSADO = "Pausado";

    private final String id;
    private final String tipo;
    private final String url;
    private final long tiempoInicio;
    private String estado;
    private String mensajeInfo;
    private long tiempoFin;
    private long tiempoAcumulado;
    private long tiempoUltimoInicioAnalisis;
    private final Object candado = new Object();

    public Tarea(String id, String tipo, String url, String estado) {
        this.id = id;
        this.tipo = tipo;
        this.url = url;
        this.estado = estado;
        this.tiempoInicio = System.currentTimeMillis();
        this.mensajeInfo = "";
        this.tiempoAcumulado = 0;
        this.tiempoUltimoInicioAnalisis = 0;

        if (ESTADO_ANALIZANDO.equals(this.estado)) {
            this.tiempoUltimoInicioAnalisis = System.currentTimeMillis();
        }
    }

    public String obtenerId() {
        return id;
    }

    public String obtenerTipo() {
        return tipo;
    }

    public String obtenerUrl() {
        return url;
    }

    public String obtenerEstado() {
        synchronized (candado) {
            return estado;
        }
    }

    public void establecerEstado(String estadoNuevo) {
        synchronized (candado) {
            String estadoAnterior = this.estado;

            this.estado = (estadoNuevo != null && esEstadoValido(estadoNuevo)) ? estadoNuevo : ESTADO_ERROR;

            if (ESTADO_ANALIZANDO.equals(estadoAnterior) && !ESTADO_ANALIZANDO.equals(this.estado)) {
                if (tiempoUltimoInicioAnalisis > 0) {
                    tiempoAcumulado += (System.currentTimeMillis() - tiempoUltimoInicioAnalisis);
                    tiempoUltimoInicioAnalisis = 0;
                }
            }

            if (!ESTADO_ANALIZANDO.equals(estadoAnterior) && ESTADO_ANALIZANDO.equals(this.estado)) {
                tiempoUltimoInicioAnalisis = System.currentTimeMillis();
            }

            if (esEstadoFinal(this.estado)) {
                this.tiempoFin = System.currentTimeMillis();
            }
        }
    }

    public static boolean esEstadoFinal(String estado) {
        return ESTADO_COMPLETADO.equals(estado) ||
               ESTADO_ERROR.equals(estado) ||
               ESTADO_CANCELADO.equals(estado);
    }

    public String obtenerMensajeInfo() {
        synchronized (candado) {
            return mensajeInfo;
        }
    }

    public void establecerMensajeInfo(String mensaje) {
        synchronized (candado) {
            this.mensajeInfo = mensaje;
        }
    }

    public long obtenerTiempoInicio() {
        return tiempoInicio;
    }

    public long obtenerTiempoFin() {
        synchronized (candado) {
            return tiempoFin;
        }
    }

    public long obtenerDuracionMilisegundos() {
        synchronized (candado) {
            long total = tiempoAcumulado;
            if (ESTADO_ANALIZANDO.equals(estado) && tiempoUltimoInicioAnalisis > 0) {
                total += (System.currentTimeMillis() - tiempoUltimoInicioAnalisis);
            }
            return total;
        }
    }

    public String formatearDuracion() {
        return formatearDuracion(obtenerDuracionMilisegundos());
    }

    public static String formatearDuracion(long milisegundos) {
        long segundos = milisegundos / 1000;
        long minutos = segundos / 60;
        segundos = segundos % 60;

        if (minutos > 0) {
            return String.format("%dm %ds", minutos, segundos);
        } else {
            return String.format("%ds", segundos);
        }
    }

    public Object[] aFilaTabla() {
        return new Object[]{
            obtenerTipo(),
            obtenerUrl(),
            obtenerEstado(),
            formatearDuracion()
        };
    }

    public static Color obtenerColorEstado(String estado) {
        if (estado == null) return Color.BLACK;
        switch (estado) {
            case ESTADO_EN_COLA:
                return EstilosUI.COLOR_TASK_EN_COLA;
            case ESTADO_ANALIZANDO:
                return EstilosUI.COLOR_TASK_ANALIZANDO;
            case ESTADO_COMPLETADO:
                return EstilosUI.COLOR_TASK_COMPLETADO;
            case ESTADO_ERROR:
                return EstilosUI.COLOR_TASK_ERROR;
            case ESTADO_CANCELADO:
                return EstilosUI.COLOR_TASK_CANCELADO;
            case ESTADO_PAUSADO:
                return EstilosUI.COLOR_TASK_PAUSADO;
            default:
                return Color.BLACK;
        }
    }

    public static boolean esEstadoValido(String estado) {
        return ESTADO_EN_COLA.equals(estado) ||
               ESTADO_ANALIZANDO.equals(estado) ||
               ESTADO_COMPLETADO.equals(estado) ||
               ESTADO_ERROR.equals(estado) ||
               ESTADO_CANCELADO.equals(estado) ||
               ESTADO_PAUSADO.equals(estado);
    }

    public boolean esActiva() {
        String estado = obtenerEstado();
        return ESTADO_EN_COLA.equals(estado) ||
               ESTADO_ANALIZANDO.equals(estado) ||
               ESTADO_PAUSADO.equals(estado);
    }

    public boolean esFinalizada() {
        return esEstadoFinal(obtenerEstado());
    }
}
