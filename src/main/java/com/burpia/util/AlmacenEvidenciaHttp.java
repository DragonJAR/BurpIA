package com.burpia.util;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AlmacenEvidenciaHttp {
    private static final Logger LOGGER = Logger.getLogger(AlmacenEvidenciaHttp.class.getName());
    private static final String EXTENSION_ARCHIVO = ".ev.gz";

    private final Path directorioEvidencia;
    private final ReentrantLock lock;
    private final LinkedHashMap<String, EntradaCache> cache;
    private final AtomicInteger escrituras;
    private long bytesCache;

    public AlmacenEvidenciaHttp() {
        this(RutasBurpIA.obtenerDirectorioEvidencia());
    }

    AlmacenEvidenciaHttp(Path directorioEvidencia) {
        this.directorioEvidencia = directorioEvidencia;
        this.lock = new ReentrantLock();
        this.cache = new LinkedHashMap<>(64, 0.75f, true);
        this.escrituras = new AtomicInteger(0);
        this.bytesCache = 0L;
        inicializarDirectorio();
        depurarArchivosDisco();
    }

    public String guardar(HttpRequestResponse evidencia) {
        if (evidencia == null) {
            return null;
        }
        byte[] requestBytes = extraerRequestBytes(evidencia);
        byte[] responseBytes = extraerResponseBytes(evidencia);

        if (requestBytes.length == 0 && responseBytes.length == 0) {
            return null;
        }

        String evidenciaId = UUID.randomUUID().toString();
        Path rutaArchivo = rutaArchivo(evidenciaId);

        try {
            escribirArchivo(rutaArchivo, requestBytes, responseBytes);
            agregarACache(evidenciaId, evidencia, requestBytes.length + responseBytes.length);
            int totalEscrituras = escrituras.incrementAndGet();
            if (totalEscrituras % PoliticaMemoria.FRECUENCIA_DEPURACION_EVIDENCIA == 0) {
                depurarArchivosDisco();
            }
            return evidenciaId;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No se pudo guardar evidencia HTTP en disco", e);
            }
            eliminarArchivoSilencioso(rutaArchivo);
            return null;
        }
    }

    public HttpRequestResponse obtener(String evidenciaId) {
        if (evidenciaId == null || evidenciaId.trim().isEmpty()) {
            return null;
        }

        HttpRequestResponse enCache = obtenerDesdeCache(evidenciaId);
        if (enCache != null) {
            return enCache;
        }

        Path ruta = rutaArchivo(evidenciaId);
        if (!Files.exists(ruta)) {
            return null;
        }

        try {
            RegistroEvidencia registro = leerArchivo(ruta);
            if (registro == null || registro.requestBytes.length == 0) {
                return null;
            }
            HttpRequestResponse evidencia = reconstruir(registro.requestBytes, registro.responseBytes);
            if (evidencia == null) {
                return null;
            }
            agregarACache(evidenciaId, evidencia, registro.requestBytes.length + registro.responseBytes.length);
            return evidencia;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No se pudo reconstruir evidencia HTTP", e);
            }
            return null;
        }
    }

    public void eliminar(String evidenciaId) {
        if (evidenciaId == null || evidenciaId.trim().isEmpty()) {
            return;
        }
        lock.lock();
        try {
            EntradaCache removida = cache.remove(evidenciaId);
            if (removida != null) {
                bytesCache = Math.max(0L, bytesCache - removida.pesoBytes);
            }
        } finally {
            lock.unlock();
        }
        eliminarArchivoSilencioso(rutaArchivo(evidenciaId));
    }

    public void limpiarCacheMemoria() {
        lock.lock();
        try {
            cache.clear();
            bytesCache = 0L;
        } finally {
            lock.unlock();
        }
    }

    private void inicializarDirectorio() {
        try {
            Files.createDirectories(directorioEvidencia);
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No se pudo crear directorio de evidencias", e);
            }
        }
    }

    private HttpRequestResponse obtenerDesdeCache(String evidenciaId) {
        lock.lock();
        try {
            EntradaCache entrada = cache.get(evidenciaId);
            return entrada != null ? entrada.evidencia : null;
        } finally {
            lock.unlock();
        }
    }

    private void agregarACache(String evidenciaId, HttpRequestResponse evidencia, long pesoBytes) {
        if (evidenciaId == null || evidencia == null) {
            return;
        }
        lock.lock();
        try {
            EntradaCache previa = cache.put(evidenciaId, new EntradaCache(evidencia, Math.max(0L, pesoBytes)));
            if (previa != null) {
                bytesCache = Math.max(0L, bytesCache - previa.pesoBytes);
            }
            bytesCache += Math.max(0L, pesoBytes);
            aplicarLimitesCache();
        } finally {
            lock.unlock();
        }
    }

    private void aplicarLimitesCache() {
        while (!cache.isEmpty()
            && (cache.size() > PoliticaMemoria.MAXIMO_ENTRADAS_CACHE_EVIDENCIA
            || bytesCache > PoliticaMemoria.MAXIMO_BYTES_CACHE_EVIDENCIA)) {
            Map.Entry<String, EntradaCache> eldest = cache.entrySet().iterator().next();
            cache.remove(eldest.getKey());
            bytesCache = Math.max(0L, bytesCache - eldest.getValue().pesoBytes);
        }
    }

    private void escribirArchivo(Path rutaArchivo, byte[] requestBytes, byte[] responseBytes) throws IOException {
        Files.createDirectories(directorioEvidencia);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(
            Files.newOutputStream(rutaArchivo, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        )))) {
            out.writeInt(requestBytes.length);
            out.write(requestBytes);
            out.writeInt(responseBytes.length);
            out.write(responseBytes);
        }
    }

    private RegistroEvidencia leerArchivo(Path rutaArchivo) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(
            Files.newInputStream(rutaArchivo, StandardOpenOption.READ)
        )))) {
            int reqLen = Math.max(0, in.readInt());
            byte[] requestBytes = new byte[reqLen];
            in.readFully(requestBytes);

            int resLen = Math.max(0, in.readInt());
            byte[] responseBytes = new byte[resLen];
            if (resLen > 0) {
                in.readFully(responseBytes);
            }
            return new RegistroEvidencia(requestBytes, responseBytes);
        }
    }

    private HttpRequestResponse reconstruir(byte[] requestBytes, byte[] responseBytes) {
        try {
            HttpRequest request = HttpRequest.httpRequest(ByteArray.byteArray(requestBytes));
            if (responseBytes == null || responseBytes.length == 0) {
                return null;
            }
            HttpResponse response = HttpResponse.httpResponse(ByteArray.byteArray(responseBytes));
            return HttpRequestResponse.httpRequestResponse(request, response);
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No se pudo reconstruir request/response desde bytes", e);
            }
            return null;
        }
    }

    private byte[] extraerRequestBytes(HttpRequestResponse evidencia) {
        try {
            if (evidencia.request() == null || evidencia.request().toByteArray() == null) {
                return new byte[0];
            }
            return evidencia.request().toByteArray().getBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private byte[] extraerResponseBytes(HttpRequestResponse evidencia) {
        try {
            if (!evidencia.hasResponse() || evidencia.response() == null || evidencia.response().toByteArray() == null) {
                return new byte[0];
            }
            return evidencia.response().toByteArray().getBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private Path rutaArchivo(String evidenciaId) {
        return directorioEvidencia.resolve(evidenciaId + EXTENSION_ARCHIVO);
    }

    private void depurarArchivosDisco() {
        try {
            Files.createDirectories(directorioEvidencia);
        } catch (Exception e) {
            return;
        }

        List<Path> archivos = listarArchivosEvidencia();
        if (archivos.isEmpty()) {
            return;
        }

        long ahora = System.currentTimeMillis();
        List<Path> candidatos = new ArrayList<>(archivos.size());
        for (Path archivo : archivos) {
            try {
                long modificado = Files.getLastModifiedTime(archivo).toMillis();
                if ((ahora - modificado) > PoliticaMemoria.TTL_EVIDENCIA_MS) {
                    eliminarArchivoSilencioso(archivo);
                    continue;
                }
                candidatos.add(archivo);
            } catch (Exception e) {
                eliminarArchivoSilencioso(archivo);
            }
        }

        int excedente = candidatos.size() - PoliticaMemoria.MAXIMO_ARCHIVOS_EVIDENCIA;
        if (excedente <= 0) {
            return;
        }

        candidatos.sort(Comparator.comparingLong(this::obtenerUltimaModificacionSegura));
        for (int i = 0; i < excedente; i++) {
            eliminarArchivoSilencioso(candidatos.get(i));
        }
    }

    private List<Path> listarArchivosEvidencia() {
        List<Path> archivos = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(directorioEvidencia)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName() != null && path.getFileName().toString().endsWith(EXTENSION_ARCHIVO))
                .forEach(archivos::add);
        } catch (Exception ignored) {
        }
        return archivos;
    }

    private long obtenerUltimaModificacionSegura(Path archivo) {
        try {
            return Files.getLastModifiedTime(archivo).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    private void eliminarArchivoSilencioso(Path archivo) {
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (Exception ignored) {
        }
    }

    private static final class EntradaCache {
        private final HttpRequestResponse evidencia;
        private final long pesoBytes;

        private EntradaCache(HttpRequestResponse evidencia, long pesoBytes) {
            this.evidencia = evidencia;
            this.pesoBytes = pesoBytes;
        }
    }

    private static final class RegistroEvidencia {
        private final byte[] requestBytes;
        private final byte[] responseBytes;

        private RegistroEvidencia(byte[] requestBytes, byte[] responseBytes) {
            this.requestBytes = requestBytes;
            this.responseBytes = responseBytes;
        }
    }
}
