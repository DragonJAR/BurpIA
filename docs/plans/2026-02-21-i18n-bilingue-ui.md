# UI Bilingüe (ES/EN) + Centralización DRY de Etiquetas Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Centralizar todos los textos visibles de la interfaz en un catálogo único DRY y habilitar cambio de idioma Español/Inglés desde Ajustes de Usuario (por defecto: español), con persistencia confiable.

**Architecture:** Se introduce una capa i18n explícita (`IdiomaUI` + `I18nUI`) desacoplada de Swing, y toda UI obtiene textos por clave en lugar de literals. La configuración guarda el idioma seleccionado y la UI se refresca en caliente al guardar ajustes. Se mantiene compatibilidad: severidad/confianza siguen en valores canónicos (`Critical|High|Medium|Low|Info` y `High|Medium|Low`) para no romper filtros, parser ni exportaciones.

**Tech Stack:** Java 21, Swing, Gson, JUnit 5, Gradle.

---

### Task 1: Persistencia de idioma en configuración (default ES)

**Files:**
- Create: `src/main/java/com/burpia/i18n/IdiomaUI.java`
- Modify: `src/main/java/com/burpia/config/ConfiguracionAPI.java`
- Modify: `src/main/java/com/burpia/config/GestorConfiguracion.java`
- Test: `src/test/java/com/burpia/config/ConfiguracionAPITest.java`
- Test: `src/test/java/com/burpia/config/GestorConfiguracionTest.java`

**Step 1: Write failing tests**
- Agregar casos:
  - `ConfiguracionAPI` inicia con idioma `es`.
  - Si se intenta setear idioma inválido, cae en `es`.
  - `GestorConfiguracion` guarda/carga `idiomaUi` correctamente.

**Step 2: Run tests to verify failure**
- Run: `./gradlew test --tests "com.burpia.config.ConfiguracionAPITest" --tests "com.burpia.config.GestorConfiguracionTest"`
- Expected: FAIL por métodos/campo de idioma inexistentes.

**Step 3: Minimal implementation**
```java
public enum IdiomaUI {
    ES("es"), EN("en");
    // desdeCodigo(...), codigo(), porDefecto()
}
```
```java
private String idiomaUi;
public String obtenerIdiomaUi() { return idiomaUi; }
public void establecerIdiomaUi(String idiomaUi) {
    this.idiomaUi = IdiomaUI.desdeCodigo(idiomaUi).codigo();
}
```
- Serializar/deserializar `idiomaUi` en `ArchivoConfiguracion` con fallback seguro a `es`.

**Step 4: Re-run tests**
- Run: `./gradlew test --tests "com.burpia.config.ConfiguracionAPITest" --tests "com.burpia.config.GestorConfiguracionTest"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/i18n/IdiomaUI.java src/main/java/com/burpia/config/ConfiguracionAPI.java src/main/java/com/burpia/config/GestorConfiguracion.java src/test/java/com/burpia/config/ConfiguracionAPITest.java src/test/java/com/burpia/config/GestorConfiguracionTest.java
git commit -m "feat(i18n): persist ui language with safe defaults"
```

### Task 2: Catálogo i18n único para toda la UI (DRY)

**Files:**
- Create: `src/main/java/com/burpia/i18n/I18nUI.java`
- Test: `src/test/java/com/burpia/i18n/I18nUITest.java`

**Step 1: Write failing tests for i18n contract**
- Validar:
  - `I18nUI.texto(key, ES)` y `I18nUI.texto(key, EN)` devuelven textos no vacíos.
  - Fallback a español cuando falta traducción.
  - Formato de mensajes dinámicos (`String.format`) funciona en ambos idiomas.

**Step 2: Run tests to verify failure**
- Run: `./gradlew test --tests "com.burpia.i18n.I18nUITest"`
- Expected: FAIL (clase/keys inexistentes).

**Step 3: Minimal implementation**
```java
public final class I18nUI {
    public enum Key { TAB_TAREAS, TAB_HALLAZGOS, AJUSTES_USUARIO, LABEL_IDIOMA, ... }
    public static String texto(Key key, IdiomaUI idioma, Object... args) { ... }
}
```
- Incluir claves para:
  - títulos de panel/tab/dialog,
  - etiquetas (`JLabel`), botones/checkbox,
  - mensajes `JOptionPane`,
  - textos de menú contextual,
  - tooltips (migrando desde `TooltipsUI`).

**Step 4: Re-run tests**
- Run: `./gradlew test --tests "com.burpia.i18n.I18nUITest"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/i18n/I18nUI.java src/test/java/com/burpia/i18n/I18nUITest.java
git commit -m "feat(i18n): add centralized UI text catalog"
```

### Task 3: Estado global de idioma y refresco de interfaz en caliente

**Files:**
- Create: `src/main/java/com/burpia/i18n/ContextoIdiomaUI.java`
- Modify: `src/main/java/com/burpia/ExtensionBurpIA.java`
- Modify: `src/main/java/com/burpia/ui/PestaniaPrincipal.java`

**Step 1: Write failing test for apply-language flow**
- Añadir test liviano (si aplica) en `PanelEstadisticasCapturaTest` o nuevo `PestaniaPrincipalIdiomaTest` para confirmar que al cambiar idioma se actualizan tabs.

**Step 2: Run targeted test**
- Run: `./gradlew test --tests "com.burpia.ui.PestaniaPrincipalIdiomaTest"`
- Expected: FAIL inicial.

**Step 3: Minimal implementation**
```java
public final class ContextoIdiomaUI {
    private static volatile IdiomaUI actual = IdiomaUI.ES;
    public static void establecer(IdiomaUI idioma) { actual = idioma != null ? idioma : IdiomaUI.ES; }
    public static IdiomaUI obtener() { return actual; }
}
```
- En `ExtensionBurpIA.initialize`: cargar idioma desde config y setear contexto.
- En callback de guardado de configuración: si cambió idioma, aplicar `ContextoIdiomaUI.establecer(...)` y `pestaniaPrincipal.aplicarIdioma()`.
- En `PestaniaPrincipal`: añadir `aplicarIdioma()` para tabs + delegación a paneles hijos.

**Step 4: Re-run tests**
- Run: `./gradlew test --tests "com.burpia.ui.PestaniaPrincipalIdiomaTest"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/i18n/ContextoIdiomaUI.java src/main/java/com/burpia/ExtensionBurpIA.java src/main/java/com/burpia/ui/PestaniaPrincipal.java src/test/java/com/burpia/ui/PestaniaPrincipalIdiomaTest.java
git commit -m "feat(i18n): wire runtime language context and tab refresh"
```

### Task 4: DialogoConfiguracion -> “Ajustes de Usuario” + selector Idioma como primera opción

**Files:**
- Modify: `src/main/java/com/burpia/ui/DialogoConfiguracion.java`
- Modify: `src/main/java/com/burpia/ui/TooltipsUI.java` (o retirar uso directo y migrar a `I18nUI`)
- Test: `src/test/java/com/burpia/ui/DialogoConfiguracionIdiomaTest.java`

**Step 1: Write failing UI test**
- Verificar que:
  - El grupo ahora se llama “AJUSTES DE USUARIO”.
  - Primera fila contiene “Idioma” con opciones `Español` y `English`.
  - Selección por defecto: español.

**Step 2: Run test to verify failure**
- Run: `./gradlew test --tests "com.burpia.ui.DialogoConfiguracionIdiomaTest"`
- Expected: FAIL.

**Step 3: Minimal implementation**
- Agregar `JComboBox<String> comboIdioma` en panel de ajustes (fila 0).
- Desplazar resto de controles una fila abajo.
- Renombrar borde `AJUSTES DE EJECUCIÓN` -> `AJUSTES DE USUARIO`.
- Guardar `idiomaUi` desde combo al `ConfiguracionAPI`.
- Cargar idioma actual al abrir diálogo.
- Todos los captions/títulos/botones/tooltips del diálogo deben salir de `I18nUI`.

**Step 4: Re-run test**
- Run: `./gradlew test --tests "com.burpia.ui.DialogoConfiguracionIdiomaTest"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/ui/DialogoConfiguracion.java src/main/java/com/burpia/ui/TooltipsUI.java src/test/java/com/burpia/ui/DialogoConfiguracionIdiomaTest.java
git commit -m "feat(ui): add language selector and rename to user settings"
```

### Task 5: Migración de textos de paneles a i18n central (sin duplicación)

**Files:**
- Modify: `src/main/java/com/burpia/ui/PanelTareas.java`
- Modify: `src/main/java/com/burpia/ui/PanelHallazgos.java`
- Modify: `src/main/java/com/burpia/ui/PanelConsola.java`
- Modify: `src/main/java/com/burpia/ui/PanelEstadisticas.java`
- Modify: `src/main/java/com/burpia/ui/DialogoDetalleHallazgo.java`
- Modify: `src/main/java/com/burpia/ui/FabricaMenuContextual.java`

**Step 1: Write/adjust failing tests**
- Extender tests existentes para verificar al menos 1 etiqueta/botón por panel en EN y ES.
- Mantener tests actuales de comportamiento intactos.

**Step 2: Run UI test subset**
- Run: `./gradlew test --tests "com.burpia.ui.PanelTareasActionsTest" --tests "com.burpia.ui.PanelHallazgosFiltrosTest" --tests "com.burpia.ui.PanelEstadisticasCapturaTest"`
- Expected: FAIL parcial por textos esperados.

**Step 3: Minimal implementation**
- Reemplazar literals por `I18nUI.texto(...)`.
- Añadir `aplicarIdioma()` en cada panel para actualizar captions dinámicos y tooltips.
- Reaplicar títulos de `TitledBorder` y textos de menús contextuales con claves i18n.

**Step 4: Re-run tests**
- Run: `./gradlew test --tests "com.burpia.ui.PanelTareasActionsTest" --tests "com.burpia.ui.PanelHallazgosFiltrosTest" --tests "com.burpia.ui.PanelEstadisticasCapturaTest"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/ui/PanelTareas.java src/main/java/com/burpia/ui/PanelHallazgos.java src/main/java/com/burpia/ui/PanelConsola.java src/main/java/com/burpia/ui/PanelEstadisticas.java src/main/java/com/burpia/ui/DialogoDetalleHallazgo.java src/main/java/com/burpia/ui/FabricaMenuContextual.java
git commit -m "refactor(ui): replace hardcoded labels with centralized i18n keys"
```

### Task 6: Encabezados de tablas y textos derivados (coherencia completa)

**Files:**
- Modify: `src/main/java/com/burpia/ui/ModeloTablaHallazgos.java`
- Modify: `src/main/java/com/burpia/ui/ModeloTablaTareas.java`
- Modify: `src/main/java/com/burpia/ui/PanelTareas.java`
- Modify: `src/main/java/com/burpia/ui/PanelHallazgos.java`

**Step 1: Write failing tests for column labels**
- Crear test que verifique nombres de columna en ES/EN.

**Step 2: Run test to verify failure**
- Run: `./gradlew test --tests "com.burpia.ui.ModeloTablaHallazgosTest" --tests "com.burpia.ui.ModeloTablaTareasI18nTest"`
- Expected: FAIL (columnas fijas actuales).

**Step 3: Minimal implementation**
- Sustituir `static final String[] COLUMNAS` por resolución dinámica desde `I18nUI`.
- Exponer `refrescarColumnasIdioma()` para disparar `fireTableStructureChanged()`.
- En paneles, tras refresco de idioma, reconfigurar renderers/anchos de columna.

**Step 4: Re-run tests**
- Run: `./gradlew test --tests "com.burpia.ui.ModeloTablaHallazgosTest" --tests "com.burpia.ui.ModeloTablaTareasI18nTest"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/ui/ModeloTablaHallazgos.java src/main/java/com/burpia/ui/ModeloTablaTareas.java src/main/java/com/burpia/ui/PanelTareas.java src/main/java/com/burpia/ui/PanelHallazgos.java src/test/java/com/burpia/ui/ModeloTablaTareasI18nTest.java
git commit -m "feat(i18n): localize table headers and refresh table structures"
```

### Task 7: Revisión de mensajes emergentes y textos operativos visibles

**Files:**
- Modify: `src/main/java/com/burpia/ui/DialogoConfiguracion.java`
- Modify: `src/main/java/com/burpia/ui/PanelTareas.java`
- Modify: `src/main/java/com/burpia/ui/PanelHallazgos.java`
- Modify: `src/main/java/com/burpia/ui/PanelConsola.java`
- Modify: `src/main/java/com/burpia/ui/DialogoDetalleHallazgo.java`
- Modify: `src/main/java/com/burpia/ui/FabricaMenuContextual.java`

**Step 1: Add a checklist test/assertions (or snapshot assertions) for dialogs**
- Verificar títulos y mensajes críticos de `JOptionPane` en ambos idiomas donde sea testeable.

**Step 2: Run targeted tests**
- Run: `./gradlew test --tests "com.burpia.ui.*Test"`
- Expected: FAIL en mensajes hardcodeados restantes.

**Step 3: Minimal implementation**
- Mover a `I18nUI` todos los `showMessageDialog/showConfirmDialog` de UI.
- Evitar concatenaciones repetidas: helpers DRY para mensajes con contador.

**Step 4: Re-run tests**
- Run: `./gradlew test --tests "com.burpia.ui.*Test"`
- Expected: PASS.

**Step 5: Commit**
```bash
git add src/main/java/com/burpia/ui/DialogoConfiguracion.java src/main/java/com/burpia/ui/PanelTareas.java src/main/java/com/burpia/ui/PanelHallazgos.java src/main/java/com/burpia/ui/PanelConsola.java src/main/java/com/burpia/ui/DialogoDetalleHallazgo.java src/main/java/com/burpia/ui/FabricaMenuContextual.java
git commit -m "refactor(i18n): centralize dialog messages and confirmations"
```

### Task 8: Hardening final + documentación

**Files:**
- Modify: `README.md`
- Modify: `docs/README.md` (si aplica)

**Step 1: Full regression**
- Run: `./gradlew test`
- Expected: PASS total.

**Step 2: Build artifact**
- Run: `./gradlew clean jar`
- Validate: `build/libs/BurpIA-1.0.0.jar`

**Step 3: Manual smoke (obligatorio)**
- Validar flujo real:
  - Abrir Ajustes, verificar “Ajustes de Usuario”.
  - Idioma por defecto Español.
  - Cambiar a English, guardar, comprobar actualización visual inmediata de tabs/paneles/dialog.
  - Reabrir BurpIA y confirmar persistencia de idioma.

**Step 4: Docs update**
- Documentar en README:
  - soporte ES/EN,
  - selector de idioma,
  - alcance (UI + tooltips + mensajes).

**Step 5: Commit**
```bash
git add README.md docs/README.md
git commit -m "docs: document bilingual UI support and language settings"
```

---

## Assumptions de diseño (para evitar ambigüedad)
- El cambio de idioma se aplica **en caliente** tras guardar ajustes, sin reiniciar Burp.
- Los valores de severidad/confianza se mantienen en inglés (canónicos) para no romper lógica.
- Se centralizan en i18n **todos los textos visibles al usuario** (etiquetas, botones, tabs, títulos de panel, tooltips y diálogos de confirmación/mensaje).
