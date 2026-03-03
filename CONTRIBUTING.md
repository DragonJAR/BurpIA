# Contributing to BurpIA

¡Gracias por tu interés en contribuir a BurpIA! Las contribuciones son bienvenidas y apreciadas.

## 🎯 Tipos de Contribuciones

Aceptamos varios tipos de contribuciones:

- 🐛 **Bug fixes** - Resolver problemas reportados
- ✨ **New features** - Agregar funcionalidades útiles
- 📚 **Documentation** - Mejorar README, javadoc, guías
- 🧪 **Tests** - Escribir pruebas unitarias
- 🎨 **UI/UX** - Mejorar la interfaz de usuario
- ⚡ **Performance** - Optimizar código existente
- 🌍 **Translations** - Agregar nuevos idiomas
- 🔧 **Refactoring** - Mejorar estructura del código

## 🚀 Cómo Empezar

### 1. Configura tu entorno de desarrollo

**Requisitos:**
- Java 17 o superior
- Burp Suite Professional 2026.1+
- Gradle 8.x
- Montoya API 2026.2 o superior
- IDE recomendado: IntelliJ IDEA o Eclipse

**Instalación:**
```bash
# Clona el repositorio
git clone https://github.com/DragonJAR/BurpIA.git
cd BurpIA

# Construye el proyecto
./gradlew clean build

# Genera el JAR
./gradlew fatJar
```

### 2. Elige una issue

Busca issues etiquetadas con:
- `good first issue` - Para principiantes
- `help wanted` - Necesita contribuidores
- `enhancement` - Nuevas características
- `bug` - Corrección de errores

### 3. Crea una branch

```bash
# Actualiza tu repositorio local
git checkout main
git pull origin main

# Crea una nueva rama para tu contribución
git checkout -b feature/tu-nombre-descriptivo
# O para bug fixes:
git checkout -b fix/tu-nombre-descriptivo
```

### 4. Haz tus cambios

**Lineamientos de código:**
- Sigue las convenciones de nombres del proyecto (ver sección Estilo de Código)
- Usa `UIUtils.ejecutarEnEdt()` para todas las actualizaciones de UI
- NUNCA llames a la API de Burp desde el EDT (usa executor service)
- Agrega Javadoc a todos los métodos públicos
- Escribe código thread-safe cuando sea accesible desde múltiples hilos

**Ejemplo de código thread-safe:**
```java
private final ReentrantLock lock = new ReentrantLock();

public void metodoThreadSafe() {
    lock.lock();
    try {
        // Operación segura
    } finally {
        lock.unlock();
    }

    UIUtils.ejecutarEnEdt(() -> {
        // Actualización de UI
    });
}
```

### 5. Prueba tus cambios

**Prueba manual:**
1. Compila el JAR: `./gradlew fatJar`
2. Carga en Burp Suite Professional
3. Configura con una API key de prueba
4. Navega a un sitio de prueba (ej: http://testphp.vulnweb.com)
5. Verifica que tus cambios funcionan correctamente

### 6. Commit tus cambios

Usa mensajes de commit claros:

```
tipo(alcance): descripción breve

Cuerpo del commit con más detalles si es necesario

Closes #numero-de-issue
```

**Tipos de commit:**
- `feat` - Nueva característica
- `fix` - Corrección de bug
- `docs` - Cambios en documentación
- `style` - Cambios de formato (espacios, etc.)
- `refactor` - Refactorización de código
- `test` - Agregar o actualizar pruebas
- `chore` - Mantenimiento (dependencias, etc.)

### 7. Push y crea Pull Request

```bash
# Push tus cambios a tu fork
git push origin feature/tu-nombre-descriptivo
```

## 📋 Revisión de Pull Requests

Todos los PRs son revisados antes de mergear. Nos enfocamos en:

- ✅ Funcionalidad correcta
- ✅ Thread-safety apropiado (ReentrantLock, Atomic, Concurrent)
- ✅ Cumplimiento de API Montoya
- ✅ Código limpio y sin violaciones de PMD
- ✅ Cobertura de tests razonable
- ✅ Manejo de errores apropiado

## 📜 Estilo de Código

### Idioma y Nomenclatura

El proyecto usa **español** para casi todo, siguiendo estas reglas:
- **Clases**: `PascalCase` en español - `GestorTareas`, `PanelAgente`
- **Métodos**: `camelCase` en español - `crearTarea()`, `actualizarEstado()`
- **Variables**: `camelCase` en español - `listaHallazgos`, `esValido`
- **Constantes**: `UPPER_SNAKE_CASE` - `MAX_REINTENTOS`
- **Términos Técnicos**: Se mantienen en inglés (API, JSON, HTTP, Request, Response, etc.)

### Internacionalización (i18n)

BurpIA es multi-idioma. **NUNCA** uses strings "hardcoded" para mensajes de usuario o logs:
- Usa `I18nUI.obtenerMensaje("clave")` para la interfaz.
- Usa `I18nLogs.obtenerLog("clave")` para la consola y logs.

## 🧪 Calidad y Tests

El proyecto utiliza JUnit 5 y Mockito. Ubica tus tests en `src/test/java/com/burpia/`.

### Ejecutar validaciones

```bash
# Ejecutar tests y generar reporte Jacoco
./gradlew test jacocoTestReport

# Ejecutar análisis estático con PMD
./gradlew pmdMain
```

### Reglas de Oro
1. **No bloquees el EDT**: Burp Suite se congelará si haces red o procesamiento pesado en el Event Dispatch Thread.
2. **Resource Cleanup**: Asegúrate de cerrar flujos y apagar ExecutorServices en el handler de `unload`.
3. **Logging**: Usa el sistema de logging centralizado del proyecto para facilitar la depuración.

## 📧 Contacto

Para preguntas sobre contribuciones:
- Abre un issue con la etiqueta `question`
- Contacta a [@DragonJAR](https://github.com/DragonJAR) en GitHub

---

**¡Gracias por contribuir a BurpIA!** 🎉
