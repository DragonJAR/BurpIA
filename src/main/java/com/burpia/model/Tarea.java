package com.burpia.model;

import com.burpia.i18n.I18nUI;
import com.burpia.ui.EstilosUI;
import com.burpia.util.Normalizador;

import java.awt.Color;

/**
 * Modelo que representa una tarea de análisis en la cola de procesamiento.
 * <p>
 * Esta clase es thread-safe y utiliza sincronización para proteger el acceso
 * a estado mutable.
 * </p>
 */
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

    /**
     * Crea una nueva tarea con los parámetros especificados.
     *
     * @param id     identificador único de la tarea (no puede ser null o vacío)
     * @param tipo   tipo de análisis (no puede ser null o vacío)
     * @param url    URL a analizar (no puede ser null o vacío)
     * @param estado estado inicial de la tarea (debe ser un estado válido)
     * @throws IllegalArgumentException si id, tipo o url están vacíos
     */
    public Tarea(String id, String tipo, String url, String estado) {
        if (Normalizador.esVacio(id)) {
            throw new IllegalArgumentException(I18nUI.Tareas.MSG_ID_VACIO());
        }
        if (Normalizador.esVacio(tipo)) {
            throw new IllegalArgumentException(I18nUI.Tareas.MSG_TIPO_VACIO());
        }
        if (Normalizador.esVacio(url)) {
            throw new IllegalArgumentException(I18nUI.Tareas.MSG_URL_VACIA());
        }

        this.id = id;
        this.tipo = tipo;
        this.url = url;
        this.estado = esEstadoValido(estado) ? estado : ESTADO_ERROR;
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

    /**
     * Establece un nuevo estado para la tarea.
     * <p>
     * Si el estado nuevo es inválido o vacío, se establece como ERROR.
     * </p>
     *
     * @param estadoNuevo el nuevo estado de la tarea
     */
    public void establecerEstado(String estadoNuevo) {
        synchronized (candado) {
            String estadoAnterior = this.estado;

            this.estado = (Normalizador.noEsVacio(estadoNuevo) && esEstadoValido(estadoNuevo)) ? estadoNuevo : ESTADO_ERROR;

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

    /**
     * Verifica si un estado es final (no puede cambiar a otro estado).
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es COMPLETADO, ERROR o CANCELADO
     */
    public static boolean esEstadoFinal(String estado) {
        return ESTADO_COMPLETADO.equals(estado) ||
               ESTADO_ERROR.equals(estado) ||
               ESTADO_CANCELADO.equals(estado);
    }

    /**
     * Verifica si un estado es reintentable.
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es ERROR o CANCELADO
     */
    public static boolean esEstadoReintentable(String estado) {
        return ESTADO_ERROR.equals(estado) || ESTADO_CANCELADO.equals(estado);
    }

    /**
     * Verifica si un estado puede ser pausado.
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es EN_COLA o ANALIZANDO
     */
    public static boolean esEstadoPausable(String estado) {
        return ESTADO_EN_COLA.equals(estado) || ESTADO_ANALIZANDO.equals(estado);
    }

    /**
     * Verifica si un estado puede ser reanudado.
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es PAUSADO
     */
    public static boolean esEstadoReanudable(String estado) {
        return ESTADO_PAUSADO.equals(estado);
    }

    /**
     * Verifica si un estado puede ser cancelado.
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es pausable o reanudable
     */
    public static boolean esEstadoCancelable(String estado) {
        return esEstadoPausable(estado) || esEstadoReanudable(estado);
    }

    /**
     * Verifica si un estado puede ser eliminado de la cola.
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es final
     */
    public static boolean esEstadoEliminable(String estado) {
        return esEstadoFinal(estado);
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

    /**
     * Obtiene el color asociado a un estado de tarea.
     *
     * @param estado el estado de la tarea
     * @return el color correspondiente al estado, o negro si el estado es inválido
     */
    public static Color obtenerColorEstado(String estado) {
        if (Normalizador.esVacio(estado) || !esEstadoValido(estado)) {
            return Color.BLACK;
        }

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

    /**
     * Verifica si un estado es válido.
     *
     * @param estado el estado a verificar
     * @return {@code true} si el estado es uno de los estados definidos
     */
    public static boolean esEstadoValido(String estado) {
        return ESTADO_EN_COLA.equals(estado) ||
               ESTADO_ANALIZANDO.equals(estado) ||
               ESTADO_COMPLETADO.equals(estado) ||
               ESTADO_ERROR.equals(estado) ||
               ESTADO_CANCELADO.equals(estado) ||
               ESTADO_PAUSADO.equals(estado);
    }

    public boolean esActiva() {
        return esEstadoCancelable(obtenerEstado());
    }

    public boolean esFinalizada() {
        return esEstadoFinal(obtenerEstado());
    }
}
