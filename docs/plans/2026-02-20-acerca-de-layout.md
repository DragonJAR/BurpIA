# Acerca De Layout Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mejorar la disposición de la pestaña "Acerca de" para reducir espacios muertos, mejorar jerarquía visual y mantener una experiencia limpia en resoluciones grandes y pequeñas.

**Architecture:** Se ajustará únicamente la composición Swing de `DialogoConfiguracion.crearPanelAcercaDe()`, separando bloques visuales en un layout vertical compacto con espaciado controlado y un "bottom spacer" flexible que absorba crecimiento de alto. No se tocará lógica de negocio.

**Tech Stack:** Java 21, Swing, Gradle, JUnit 5 (si se agrega prueba de construcción de panel).

---

### Task 1: Reestructurar layout base de la pestaña Acerca de

**Files:**
- Modify: `src/main/java/com/burpia/ui/DialogoConfiguracion.java`

**Step 1: Escribir prueba mínima de construcción de panel (opcional pero recomendada)**
```java
// Verifica que el panel no lance excepciones y contenga título
```

**Step 2: Ejecutar prueba para validar baseline**
Run: `./gradlew test --tests "com.burpia.ui.*Dialogo*"`
Expected: PASS o "no tests found" (si aún no existe test dedicado).

**Step 3: Implementar layout compacto**
- Reducir márgenes verticales superiores e intermedios.
- Mantener título + subtítulo centrados, pero con menor separación.
- Cambiar contenedor principal a flujo vertical con `BoxLayout` y separadores más pequeños.
- Eliminar altura fija excesiva en paneles (`setMaximumSize` demasiado grandes).

**Step 4: Verificar compilación**
Run: `./gradlew -q compileJava`
Expected: PASS.

### Task 2: Optimizar bloques RESUMEN y DESARROLLADO POR

**Files:**
- Modify: `src/main/java/com/burpia/ui/DialogoConfiguracion.java`

**Step 1: Ajustar bloque RESUMEN**
- Reducir padding interno.
- Usar `JTextArea` no editable con line-wrap y altura preferida más ajustada.
- Evitar que este bloque empuje innecesariamente el contenido inferior.

**Step 2: Ajustar bloque DESARROLLADO POR**
- Reducir altura máxima y espacio en blanco interno.
- Alinear nombre + botón de sitio en una composición más compacta.

**Step 3: Verificar en compilación**
Run: `./gradlew -q compileJava`
Expected: PASS.

### Task 3: Mejorar comportamiento responsive (alto/ventana pequeña)

**Files:**
- Modify: `src/main/java/com/burpia/ui/DialogoConfiguracion.java`

**Step 1: Hacer que el espacio sobrante se vaya al final**
- Mantener un `Glue` o filler en la parte baja para que el contenido útil quede arriba.
- Evitar hueco grande entre versión y botones inferiores del diálogo.

**Step 2: Confirmar scroll correcto**
- Verificar que al reducir altura el scroll sea natural y no corte bloques.

**Step 3: Validar manualmente**
- Probar visualmente en ventana alta y ventana compacta.
Expected: jerarquía limpia, menos aire vertical, lectura más rápida.

### Task 4: Pulido visual consistente con el resto del diálogo

**Files:**
- Modify: `src/main/java/com/burpia/ui/DialogoConfiguracion.java`

**Step 1: Normalizar espaciados y tipografías**
- Alinear separación con pestañas `Proveedor LLM` y `Prompt`.
- Mantener mismas fuentes de `EstilosUI`.

**Step 2: Revisión de accesibilidad visual básica**
- Contraste de texto descriptivo.
- Botón del sitio visible sin dominar la pantalla.

**Step 3: Verificación final de regresión**
Run: `./gradlew test`
Expected: PASS.

### Task 5: Build final

**Files:**
- No additional changes

**Step 1: Construir artefacto final**
Run: `./gradlew clean jar`

**Step 2: Validar JAR**
Check: `/Users/jaimearestrepo/Proyectos/BurpIA/build/libs/BurpIA-1.0.0.jar`

