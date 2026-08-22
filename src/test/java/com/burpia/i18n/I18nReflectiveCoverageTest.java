package com.burpia.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Cobertura reflexiva del sistema i18n: enumera TODOS los métodos zero-arg
 * {@code public static String *()} en las clases internas de {@link I18nUI}
 * y verifica que ni el slot ES ni el slot EN devuelvan vacío.
 * <p>
 * Sin este guardrail, un método nuevo con {@code tr("texto", "")} (slot EN
 * vacío) pasaría todos los tests existentes y produciría strings vacías en
 * UI cuando el usuario seleccione inglés.
 * </p>
 */
@DisplayName("I18n Reflective Coverage")
class I18nReflectiveCoverageTest {

    private IdiomaUI idiomaPrevio;

    @BeforeEach
    void capturarIdioma() {
        idiomaPrevio = I18nUI.obtenerIdioma();
    }

    @AfterEach
    void restaurarIdioma() {
        if (idiomaPrevio != null) {
            I18nUI.establecerIdioma(idiomaPrevio.codigo());
        }
    }

    @Test
    @DisplayName("Todos los métodos zero-arg public static String de I18nUI devuelven no-vacío en ES y EN")
    void todosLosMetodosZeroArgDevuelvenNoVacioEnAmbosIdiomas() {
        List<String> fallas = new ArrayList<>();

        for (Class<?> clase : I18nUI.class.getDeclaredClasses()) {
            verificarClase(clase, fallas);
            // Sub-clases anidadas (Tooltips.Configuracion, etc.)
            for (Class<?> subclase : clase.getDeclaredClasses()) {
                verificarClase(subclase, fallas);
            }
        }

        if (!fallas.isEmpty()) {
            fail("Métodos i18n con slot vacío detectados:\n  - " + String.join("\n  - ", fallas));
        }
    }

    private void verificarClase(Class<?> clase, List<String> fallas) {
        for (Method metodo : clase.getDeclaredMethods()) {
            if (!esMetodoElegible(metodo)) {
                continue;
            }
            verificarMetodoEnAmbosIdiomas(clase, metodo, fallas);
        }
    }

    private boolean esMetodoElegible(Method metodo) {
        int mods = metodo.getModifiers();
        return Modifier.isPublic(mods)
            && Modifier.isStatic(mods)
            && metodo.getReturnType() == String.class
            && metodo.getParameterCount() == 0
            // Excluir métodos sintéticos generados por el compilador
            && !metodo.isSynthetic()
            // Excluir métodos heredados (toString, etc.)
            && metodo.getDeclaringClass() == metodo.getDeclaringClass();
    }

    private void verificarMetodoEnAmbosIdiomas(Class<?> clase, Method metodo, List<String> fallas) {
        String ruta = clase.getSimpleName() + "." + metodo.getName() + "()";

        // ES
        I18nUI.establecerIdioma("es");
        String valorEs = invocar(metodo, ruta, fallas);
        if (valorEs != null && valorEs.isEmpty()) {
            fallas.add(ruta + " — slot ES vacío");
        }

        // EN
        I18nUI.establecerIdioma("en");
        String valorEn = invocar(metodo, ruta, fallas);
        if (valorEn != null && valorEn.isEmpty()) {
            fallas.add(ruta + " — slot EN vacío");
        }
    }

    private String invocar(Method metodo, String ruta, List<String> fallas) {
        try {
            Object resultado = metodo.invoke(null);
            return resultado == null ? "" : resultado.toString();
        } catch (IllegalAccessException | InvocationTargetException e) {
            fallas.add(ruta + " — no se pudo invocar: " + e.getMessage());
            return null;
        }
    }
}
