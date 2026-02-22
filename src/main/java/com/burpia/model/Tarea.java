package com.burpia.model;

import java.awt.Color;
import javax.swing.*;

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
    private final Object candado = new Object();

    public Tarea(String id, String tipo, String url, String estado) {
        this.id = id;
        this.tipo = tipo;
        this.url = url;
        this.estado = estado;
        this.tiempoInicio = System.currentTimeMillis();
        this.mensajeInfo = "";
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

    public void establecerEstado(String estado) {
        synchronized (candado) {
            this.estado = estado != null ? estado : ESTADO_ERROR;
            if (ESTADO_COMPLETADO.equals(this.estado) ||
                ESTADO_ERROR.equals(this.estado) ||
                ESTADO_CANCELADO.equals(this.estado)) {
                this.tiempoFin = System.currentTimeMillis();
            }
        }
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
            if (tiempoFin > 0) {
                return tiempoFin - tiempoInicio;
            }
            return System.currentTimeMillis() - tiempoInicio;
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
                return Color.GRAY;
            case ESTADO_ANALIZANDO:
                return new Color(0, 120, 215);
            case ESTADO_COMPLETADO:
                return new Color(0, 153, 0);
            case ESTADO_ERROR:
                return new Color(204, 0, 0);
            case ESTADO_CANCELADO:
                return new Color(153, 76, 0);
            case ESTADO_PAUSADO:
                return new Color(255, 153, 0);
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
        String estado = obtenerEstado();
        return ESTADO_COMPLETADO.equals(estado) ||
               ESTADO_ERROR.equals(estado) ||
               ESTADO_CANCELADO.equals(estado);
    }
}
