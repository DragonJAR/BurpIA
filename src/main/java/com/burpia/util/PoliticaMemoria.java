package com.burpia.util;

public final class PoliticaMemoria {
    public static final int MAXIMO_BACKLOG_CONSOLA = 5000;
    public static final int MAXIMO_CUERPO_ANALISIS_CARACTERES = 12000;

    public static final int MAXIMO_ENTRADAS_CACHE_EVIDENCIA = 128;
    public static final long MAXIMO_BYTES_CACHE_EVIDENCIA = 32L * 1024L * 1024L;
    public static final int MAXIMO_ARCHIVOS_EVIDENCIA = 5000;
    public static final long TTL_EVIDENCIA_MS = 7L * 24L * 60L * 60L * 1000L;
    public static final int FRECUENCIA_DEPURACION_EVIDENCIA = 100;

    // Límites de caches de renderizadores UI
    public static final int MAX_CACHE_TEXTO_RENDERIZADOR = 100;
    public static final int MAX_CACHE_SEGMENTOS_RENDERIZADOR = 100;

    // Límites de modelos de tabla
    public static final int MAX_HALLAZGOS_MANTENIDOS = 1000;

    private PoliticaMemoria() {
    }
}
