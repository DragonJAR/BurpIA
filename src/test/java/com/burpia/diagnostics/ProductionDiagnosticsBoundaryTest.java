package com.burpia.diagnostics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Production Diagnostics Boundary Tests")
class ProductionDiagnosticsBoundaryTest {

    private static final Path DIRECTORIO_FUENTES = Path.of("src/main/java");

    @Test
    @DisplayName("Hooks de diagnostico runtime permanecen fuera de src/main")
    void hooksRuntimeFueraDeMain() throws IOException {
        Map<String, String> patronesBloqueados = new LinkedHashMap<>();
        patronesBloqueados.put("burpia_pty_error.log", "archivo ad-hoc de diagnostico en HOME");
        patronesBloqueados.put("[ENTER-DEBUG]", "log sensible de payload de transporte");

        List<Path> fuentes = listarFuentesMain();

        validarDirectorioFuentesExiste(fuentes);

        List<String> hallazgos = new ArrayList<>();

        for (Path archivo : fuentes) {
            String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
            for (Map.Entry<String, String> patron : patronesBloqueados.entrySet()) {
                if (contenido.contains(patron.getKey())) {
                    hallazgos.add(
                        archivo + " contiene '" + patron.getKey() + "' (" + patron.getValue() + ")"
                    );
                }
            }
        }

        assertTrue(
            hallazgos.isEmpty(),
            "Se detectaron hooks de diagnostico en produccion en " + fuentes.size() + " archivos:\n" + String.join("\n", hallazgos)
        );
    }

    private List<Path> listarFuentesMain() throws IOException {
        if (!Files.exists(DIRECTORIO_FUENTES)) {
            fail("Directorio de fuentes no existe: " + DIRECTORIO_FUENTES.toAbsolutePath() +
                 ". Ejecute el test desde la raiz del proyecto.");
        }

        try (Stream<Path> stream = Files.walk(DIRECTORIO_FUENTES)) {
            return stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
    }

    private void validarDirectorioFuentesExiste(List<Path> fuentes) {
        if (fuentes.isEmpty()) {
            fail("No se encontraron archivos Java en " + DIRECTORIO_FUENTES.toAbsolutePath() +
                 ". El directorio existe pero esta vacio - posible error de configuracion.");
        }
    }
}
