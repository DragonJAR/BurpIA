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

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Production Diagnostics Boundary Tests")
class ProductionDiagnosticsBoundaryTest {

    @Test
    @DisplayName("Hooks de diagnostico runtime permanecen fuera de src/main")
    void hooksRuntimeFueraDeMain() throws Exception {
        Map<String, String> patronesBloqueados = new LinkedHashMap<>();
        patronesBloqueados.put("burpia.agent.enterDebug", "override runtime de debug");
        patronesBloqueados.put("burpia.agent.submit.strategy", "override runtime de estrategia submit");
        patronesBloqueados.put("burpia.agent.submit.delayMs", "override runtime de delay submit");
        patronesBloqueados.put("burpia_pty_error.log", "archivo ad-hoc de diagnostico en HOME");
        patronesBloqueados.put("[ENTER-DEBUG]", "log sensible de payload de transporte");

        List<Path> fuentes = listarFuentesMain();
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

        assertTrue(hallazgos.isEmpty(), "Se detectaron hooks de diagnostico en produccion:\n" + String.join("\n", hallazgos));
    }

    private List<Path> listarFuentesMain() throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java"))) {
            return stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
    }
}
