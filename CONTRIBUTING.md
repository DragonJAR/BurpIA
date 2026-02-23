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
- Burp Suite Professional 2024.10+
- Gradle 8.x
- IDE recomendado: IntelliJ IDEA o Eclipse

**Instalación:**
```bash
# Clona el repositorio
git clone https://github.com/jaimearestrepo/BurpIA.git
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
- Sigue las convenciones de nombres de Java
- Usa `SwingUtilities.invokeLater()` para todas las actualizaciones de UI
- NUNCA llames a la API de Burp desde el EDT (usa executor service)
- Agrega Javadoc a todos los métodos públicos
- Escribe código thread-safe cuando sea accesible desde múltiples hilos

**Ejemplo de código thread-safe:**
```java
private final ReentrantLock lock = new ReentrantLock();

public void metodoThreadSafe() {
    lock.lock();
    try {
    } finally {
        lock.unlock();
    }

    SwingUtilities.invokeLater(() -> {
    });
}

public void metodoNoThreadSafe() {
    SwingUtilities.invokeLater(() -> {
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

**Sitios de prueba:**
- http://testphp.vulnweb.com
- https://juice-shop.herokuapp.com
- https://webgoat.github.io/WebGoat/

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

**Ejemplo:**
```
feat(ai): add support for new provider

Implementa soporte para el nuevo proveedor de IA X,
incluyendo configuración de headers y parsing de respuestas.

Closes #123
```

### 7. Push y crea Pull Request

```bash
# Push tus cambios a tu fork
git push origin feature/tu-nombre-descriptivo
```

Luego:
1. Ve a GitHub
2. Crea un Pull Request
3. Describe tus cambios claramente
4. Referencia las issues relacionadas
5. Espera la revisión

## 📋 Revisión de Pull Requests

Todos los PRs son revisados antes de mergear. Nos enfocamos en:

- ✅ Funcionalidad correcta
- ✅ Thread-safety apropiado
- ✅ Cumplimiento de API Montoya
- ✅ Código limpio y bien documentado
- ✅ Sin memory leaks
- ✅ Manejo de errores apropiado
- ✅ Tests cuando sea aplicable

## 🐛 Reportando Bugs

Usa el template de bug report:

```markdown
**Descripción**
Breve descripción del problema

**Pasos para reproducir**
1. Ir a '...'
2. Click en '....'
3. Scroll a '....'
4. Ver error

**Comportamiento esperado**
Debería pasar esto

**Screenshots**
Si aplica, agrega screenshots

**Entorno:**
- Burp Suite version: [ej. 2024.10]
- OS: [ej. Windows 11, macOS 15]
- Java version: [ej. 17.0.9]
- BurpIA version: [ej. 1.0.0]
```

## 💡 Sugerencias de Features

Para sugerir nuevas características:

1. Verifica issues existentes primero
2. Describe el caso de uso claramente
3. Explica por qué es útil
4. Considera la complejidad de implementación
5. Si es posible, ofrece ayuda en la implementación

## 📜 Estilo de Código

### Convenciones de Java

- **Clases**: `PascalCase` - `MiClase`
- **Métodos**: `camelCase` - `miMetodo`
- **Constantes**: `UPPER_SNAKE_CASE` - `MI_CONSTANTE`
- **Paquetes**: `lowercase` - `com.burpia.util`

### Javadoc

Agrega Javadoc a clases y métodos públicos:

```java
 * Breve descripción de la clase.
 *
 * <p>Descripción más detallada si es necesario.
 *
 * @author Nombre del autor
 * @since 1.0.0
public class MiClase {

     * Breve descripción del método.
     *
     * @param parametro1 Descripción del parámetro
     * @return Descripción del retorno
     * @throws MiException Cuando ocurre este error
    public String miMetodo(String parametro1) throws MiException {
    }
}
```

### Naming en Español

El proyecto usa español en:
- Nombres de variables
- Mensajes de UI
- Comentarios en código
- Documentación

Excepción:
- Nombres de clases y métodos (camelCase/PascalCase)
- Palabras técnicas en inglés (API, JSON, HTTP, etc.)

## ⚠️ Reglas Importantes

### Thread-Safety

TODOS los componentes deben ser thread-safe:

```java
private final AtomicInteger contador = new AtomicInteger(0);

public void incrementar() {
    contador.incrementAndGet();
}

private int contador = 0;

public void incrementar() {
    contador++; // Race condition!
}
```

### Montoya API Compliance

**REGLA DE ORO:** NUNCA llamar a métodos de la API de Burp desde el EDT.

```java
new Thread(() -> {
    api.repeater().sendToRepeater(request, "BurpIA");
    api.scanner().startAudit(config);
    api.siteMap().add(auditIssue);
}, "BurpIA-API").start();

SwingUtilities.invokeLater(() -> {
    api.repeater().sendToRepeater(request, "BurpIA"); // Deadlock!
    api.siteMap().add(auditIssue);
});
```

### Resource Cleanup

Siempre liberar recursos:

```java
public void shutdown() {
    if (executorService != null) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

public void shutdown() {
}
```

## 🧪 Escribiendo Tests

El proyecto tiene una suite de tests con JUnit 5 y Mockito. Ubica tus tests en `src/test/java/com/burpia/`:

```java
@DisplayName("ProbadorConexionAI debería validar endpoints correctamente")
@Test
public void testGestorTareas_crearTarea() {
    GestorTareas gestor = new GestorTareas(modelo, logger);
    Tarea tarea = gestor.crearTarea("Test", "http://example.com",
        Tarea.ESTADO_EN_COLA, "Mensaje");

    assertNotNull(tarea);
    assertEquals("Test", tarea.obtenerTipo());
    assertEquals("http://example.com", tarea.obtenerUrl());
}
```

### Ejecutar Tests

```bash
# Todos los tests
./gradlew test

# Test específico
./gradlew test --tests "com.burpia.util.ProbadorConexionAITest"
```

## 📚 Recursos Útiles

- [Burp Montoya API Javadoc](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
- [Effective Java](https://www.oracle.com/java/technologies/javase/effectivejava.html)

## 🤝 Código de Conducta

Se espera que todos los contribuyentes:
- Sean respetuosos y constructivos
- Acepten críticas constructivas
- Se enfoquen en lo que es mejor para la comunidad
- Muestren empatía hacia otros miembros de la comunidad

## 📧 Contacto

Para preguntas sobre contribuciones:
- Abre un issue con la etiqueta `question`
- Contacta a [@jaimearestrepo](https://github.com/jaimearestrepo) en GitHub

---

**¡Gracias por contribuir a BurpIA!** 🎉
