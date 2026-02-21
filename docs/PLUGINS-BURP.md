# Burp Suite Montoya API - Documentación Completa

> **Versión API:** 2026.2  
> **Fuente:** https://portswigger.github.io/burp-extensions-montoya-api/javadoc/  
> **Generado:** Febrero 2026

---

## 📋 Tabla de Contenido

### 🔧 **API Core**
- [MontoyaApi](#montoyaapi) - Punto de entrada principal
- [BurpExtension](#burpextension) - Interface base para extensiones
- [MontoyaApiExtension](#montoyaapiextension) - Extensiones con capacidades mejoradas

### 🤖 **AI Funcionalidad** (Professional)
- [Ai](#ai) - Funcionalidad AI
- [Ai.Chat](#aichat) - Chat AI

### ⚡ **Core Components**
- [Core](#core) - Componentes básicos (ByteArray, ToolType, etc.)
- [Utilities](#utilities) - Utilidades varias
- [Utilities.Json](#utilitiesjson) - JSON utils
- [Utilities.Rank](#utilitiesrank) - Ranking utils
- [Utilities.Shell](#utilitiesshell) - Shell utils

### 🌐 **HTTP Handling**
- [Http](#http) - HTTP principal
- [Http.Message](#httpmessage) - Mensajes HTTP
- [Http.Handler](#httphandler) - Handlers HTTP
- [Http.Message.Params](#httpmessageparams) - Parámetros HTTP
- [Http.Message.Requests](#httpmessagerequests) - Requests
- [Http.Message.Responses](#httpmessageresponses) - Responses
- [Http.Message.Responses.Analysis](#httpmessageresponsesanalysis) - Análisis
- [Http.Sessions](#httpsessions) - Manejo de sesiones

### 🔍 **Scanner** (Professional)
- [Scanner](#scanner) - Scanner principal
- [Scanner.Audit](#scanneraudit) - Auditoría
- [Scanner.Audit.InsertionPoint](#scannerauditinsertionpoint) - Puntos de inserción
- [Scanner.Audit.Issues](#scannerauditissues) - Issues de auditoría
- [Scanner.BChecks](#scannerbchecks) - BChecks
- [Scanner.ScanCheck](#scannerscancheck) - Scan checks

### 🛡️ **Proxy**
- [Proxy](#proxy) - Proxy principal
- [Proxy.Http](#proxyhttp) - Proxy HTTP
- [Proxy.WebSocket](#proxywebsocket) - Proxy WebSocket

### 🎯 **Herramientas de Burp**
- [Comparer](#comparer) - Comparer tool
- [Decoder](#decoder) - Decoder tool
- [Intruder](#intruder) - Intruder tool
- [Repeater](#repeater) - Repeater tool
- [SiteMap](#sitemap) - Site map
- [Scope](#scope) - Target scope
- [Collaborator](#collaborator) - OOB testing (Professional)
- [Organizer](#organizer) - Organizer tool

### 🎨 **User Interface**
- [UI](#ui) - UI principal
- [UI.ContextMenu](#uicontextmenu) - Menú contextual
- [UI.Editor](#uieditor) - Editores
- [UI.Editor.Extension](#uieditorextension) - Editores de extensión
- [UI.HotKey](#uihotkey) - Hotkeys
- [UI.Menu](#uimenu) - Menús
- [UI.Settings](#uisettings) - Configuraciones
- [UI.Swing](#uiswing) - Componentes Swing

### 💾 **Persistence**
- [Persistence](#persistence) - Persistencia principal
- [Project](#project) - Proyecto
- [Extension](#extension) - Gestión de extensión
- [Logging](#logging) - Logging
- [Bambda](#bambda) - Bambdas

### 🔌 **Extensiones**
- [BurpSuite](#burpsuite) - Aplicación Burp Suite
- [Extension](#extension) - Extensión

### 🔄 **WebSocket**
- [WebSocket](#websocket) - WebSocket principal
- [WebSocket.Extension](#websocketextension) - Extensiones WebSocket

---

## 🔧 API Core

### MontoyaApi

**Package:** `burp.api.montoya`

**Interface:** `MontoyaApi`

Punto de entrada principal para la Montoya API. Proporciona acceso a toda la funcionalidad de Burp Suite.

**Métodos Principales:**
```java
// Acceso a herramientas principales
Api ai()                    // Funcionalidad AI (Professional)
Bambda bambda()           // Bambdas
BurpSuite burpSuite()     // Aplicación Burp Suite
Comparer comparer()       // Herramienta Comparer
Decoder decoder()         // Herramienta Decoder
Extension extension()     // Gestión de extensión
Http http()               // HTTP requests/responses
Intruder intruder()       // Herramienta Intruder
Logging logging()         // Logging y eventos
Organizer organizer()     // Herramienta Organizer
Persistence persistence() // Persistencia
Project project()         // Proyecto
Proxy proxy()             // Herramienta Proxy
Repeater repeater()       // Herramienta Repeater
Scanner scanner()         // Scanner (Professional)
Scope scope()             // Target scope
SiteMap siteMap()         // Site map
Utilities utilities()     // Utilidades
UserInterface userInterface() // User interface
WebSocket websockets()    // WebSocket
```

**Ejemplo de uso:**
```java
public class MyExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        // Configurar extensión
        api.extension().setName("Mi Extensión");
        
        // Acceder a múltiples herramientas
        api.logging().logToOutput("Iniciando extensión");
        api.http().registerHttpHandler(myHttpHandler);
        api.scanner().registerPassiveScanCheck(myPassiveCheck);
    }
}
```

---

### BurpExtension

**Package:** `burp.api.montoya`

**Interface:** `BurpExtension`

Todas las extensiones deben implementar esta interface.

**Método requerido:**
```java
void initialize(MontoyaApi api);
```

**Implementación básica:**
```java
public class MyExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        // Lógica de inicialización aquí
        api.extension().setName("Mi Extensión");
    }
}
```

---

### MontoyaApiExtension

**Package:** `burp.api.montoya`

**Interface:** `MontoyaApiExtension`

Extensiones con capacidades mejoradas que necesitan ser solicitadas explícitamente.

---

## 🤖 AI Funcionalidad

### Ai

**Package:** `burp.api.montoya.ai`

**Interfaces:**
- **Ai** - Funcionalidad AI principal
- **Prompt** - Prompts de chat AI

**Métodos de Ai:**
```java
boolean isEnabled()           // ¿Tiene acceso a funcionalidad AI?
Prompt prompt()              // Acceso a prompts de chat AI
```

**Uso:**
```java
if (api.ai().isEnabled()) {
    Prompt prompt = api.ai().prompt();
    // Usar funcionalidad AI
}
```

---

### Ai.Chat

**Package:** `burp.api.montoya.ai.chat**

Contiene clases relacionadas con el chat AI y la interacción con modelos de lenguaje.

---

## ⚡ Core Components

### Core

**Package:** `burp.api.montoya.core`

**Clases e Interfaces Principales:**

| Clase/Interface | Descripción |
|----------------|-------------|
| **Annotations** | Anotaciones almacenadas con requests/responses |
| **ByteArray** | Arreglo de bytes con métodos de manipulación |
| **Color** | Colores para highlights en Burp Suite |
| **Marker** | Rango que representa datos interesantes |
| **Range** | Rango de valores (inicio inclusivo, fin exclusivo) |
| **Registration** | Retornado cuando un objeto es registrado |
| **Task** | Tarea en el Dashboard |
| **ToolSource** | Herramienta fuente de un objeto |
| **ToolType** | Tipos de herramientas en Burp Suite |
| **ProductVersion** | Versión del producto |

**Ejemplo ByteArray:**
```java
ByteArray data = ByteArray.byteArray("Hello World");
String hex = data.toHex();
String base64 = data.toBase64();
```

---

## 🌐 HTTP Handling

### Http

**Package:** `burp.api.montoya.http`

**Interface:** `Http`

Acceso a funcionalidad HTTP requests/responses.

**Métodos principales:**
```java
CookieJar cookieJar()                           // Cookie Jar
ResponseKeywordsAnalyzer createResponseKeywordsAnalyzer()  // Analizador de keywords
ResponseVariationsAnalyzer createResponseVariationsAnalyzer() // Analizador de variaciones
void registerHttpHandler(HttpHandler handler)   // Registrar handler HTTP
void registerSessionHandlingAction(SessionHandlingAction action) // Handler de sesión
HttpRequestResponse sendRequest(HttpRequest request)     // Enviar request
HttpRequestResponse sendRequest(HttpRequest request, RequestOptions options)
List<HttpRequestResponse> sendRequests(List<HttpRequest> requests) // Enviar múltiples
```

---

### Http.Message

**Package:** `burp.api.montoya.http.message`

**Clases principales:**

| Clase | Descripción |
|-------|-------------|
| **ContentType** | Tipos de contenido reconocidos por Burp |
| **Cookie** | Cookie de Burp |
| **HttpHeader** | Header HTTP de Burp |
| **HttpMessage** | Información común a HttpRequest/HttpResponse |
| **HttpRequestResponse** | Acople entre request/response |
| **MimeType** | MIME types reconocidos por Burp |
| **StatusCodeClass** | Clases de status codes HTTP |

---

### Http.Handler

**Package:** `burp.api.montoya.http.handler`

**Interfaces principales:**

| Interface | Descripción |
|-----------|-------------|
| **HttpHandler** | Handler HTTP principal |
| **HttpRequestToBeSent** | Request a ser enviado |
| **HttpResponseReceived** | Response recibida |
| **RequestAction** | Acción para requests |
| **RequestToBeSentAction** | Acción para request a enviar |
| **ResponseAction** | Acción para responses |
| **ResponseReceivedAction** | Acción para response recibida |
| **TimingData** | Datos de timing |

**Ejemplo HttpHandler:**
```java
api.http().registerHttpHandler(new HttpHandler() {
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        api.logging().logToOutput("Enviando: " + request.url());
        return RequestToBeSentAction.continueWith(request);
    }
    
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        return ResponseReceivedAction.continueWith(response);
    }
});
```

---

### Http.Message.Params

**Package:** `burp.api.montoya.http.message.params`

**Clases principales:**

| Clase | Descripción |
|-------|-------------|
| **HttpParameter** | Parámetro HTTP de Burp |
| **HttpParameterType** | Tipos de parámetros HTTP |
| **ParsedHttpParameter** | Parámetro HTTP parseado por Burp |

**Tipos de HttpParameter:**
```java
HttpParameterType.URL           // Parámetros de URL
HttpParameterType.BODY          // Parámetros de body
HttpParameterType.COOKIE        // Parámetros de cookie
HttpParameterType.XML           // Parámetros XML
HttpParameterType.MULTIPART     // Parámetros multipart
HttpParameterType.JSON          // Parámetros JSON
HttpParameterType.XML_ATTRIBUTE // Atributos XML
HttpParameterType.HEADER        // Headers HTTP
HttpParameterType.UNKNOWN       // Tipo desconocido
```

---

### Http.Message.Requests

**Package:** `burp.api.montoya.http.message.requests`

**Clases:**
- **HttpRequest** - Request HTTP de Burp
- **Transformation** - Transformaciones aplicables a requests

**Ejemplo HttpRequest:**
```java
// Crear request
HttpService service = HttpService.httpService("example.com", 443, true);
HttpRequest request = HttpRequest.httpRequest(service, 
    "GET /path HTTP/1.1\r\nHost: example.com\r\n\r\n");

// Modificar request
request = request.withMethod("POST")
               .withHeader("Content-Type", "application/json")
               .withBody("{\"test\": \"data\"}");
```

---

### Http.Message.Responses

**Package:** `burp.api.montoya.http.message.responses`

**Clase principal:**
- **HttpResponse** - Response HTTP de Burp

---

### Http.Message.Responses.Analysis

**Package:** `burp.api.montoya.http.message.responses.analysis`

**Clases:**
- **ResponseKeywordsAnalyzer** - Analizador de keywords en responses
- **ResponseVariationsAnalyzer** - Analizador de variaciones en responses

---

### Http.Sessions

**Package:** `burp.api.montoya.http.sessions`

**Interface principal:**
- **SessionHandlingAction** - Acciones de manejo de sesión

---

## 🔍 Scanner (Professional)

### Scanner

**Package:** `burp.api.montoya.scanner`

**Interface:** `Scanner`

Acceso a funcionalidad del Scanner tool (Professional only).

**Métodos principales:**
```java
BChecks bChecks()                                       // Acceso a BChecks
void generateReport(List<AuditIssue> issues, ReportFormat format, Path path)  // Generar reporte
void registerActiveScanCheck(ActiveScanCheck check)     // Registrar check activo
void registerAuditIssueHandler(AuditIssueHandler handler) // Handler de issues
void registerInsertionPointProvider(InsertionPointProvider provider)  // Provider de insertion points
void registerPassiveScanCheck(PassiveScanCheck check)   // Registrar check pasivo
Audit startAudit(StartAuditConfiguration config)       // Iniciar auditoría
Crawl startCrawl(StartCrawlConfiguration config)       // Iniciar crawl
```

**Ejemplo de Passive Scan Check:**
```java
api.scanner().registerPassiveScanCheck(new PassiveScanCheck() {
    @Override
    public ConsolidationAction consolidateIssues(AuditIssue newIssue, AuditIssue existingIssue) {
        return ConsolidationAction.KEEP_BOTH;
    }
    
    @Override
    public AuditResult passivelyAudit(HttpRequestResponse requestResponse) {
        // Lógica de scanning pasivo
        return AuditResult.auditIssues();
    }
});
```

---

### Scanner.Audit

**Package:** `burp.api.montoya.scanner.audit`

**Interfaces principales:**
- **Audit** - Auditoría en Scanner tool
- **AuditIssueHandler** - Handler para audit issues

---

### Scanner.Audit.InsertionPoint

**Package:** `burp.api.montoya.scanner.audit.insertionpoint**

**Interface principal:**
- **InsertionPoint** - Puntos de inserción para scanning

---

### Scanner.Audit.Issues

**Package:** `burp.api.montoya.scanner.audit.issues`

**Clases principales:**

| Clase | Descripción |
|-------|-------------|
| **AuditIssue** | Detalles de issues de auditoría |
| **AuditIssueConfidence** | Niveles de confianza |
| **AuditIssueDefinition** | Información de fondo de issues |
| **AuditIssueSeverity** | Niveles de severidad |

**Niveles de Severidad:**
```java
AuditIssueSeverity.HIGH      // Alta severidad
AuditIssueSeverity.MEDIUM    // Severidad media
AuditIssueSeverity.LOW       // Baja severidad
AuditIssueSeverity.INFO      // Informativo
```

**Niveles de Confianza:**
```java
AuditIssueConfidence.CERTAIN  // Cierto
AuditIssueConfidence.FIRM     // Firme
AuditIssueConfidence.TENTATIVE // Tentativo
```

---

### Scanner.BChecks

**Package:** `burp.api.montoya.scanner.bchecks`

Contiene clases relacionadas con BChecks (lenguaje de scripting del Scanner).

---

### Scanner.ScanCheck

**Package:** `burp.api.montoya.scanner.scancheck**

Contiene clases para los scan checks del Scanner.

---

## 🛡️ Proxy

### Proxy

**Package:** `burp.api.montoya.proxy`

**Interface:** `Proxy`

Acceso a funcionalidad del Proxy tool.

**Métodos principales:**
```java
void enableIntercept()                                    // Habilitar interceptación
void disableIntercept()                                   // Deshabilitar interceptación
boolean isInterceptEnabled()                              // ¿Está habilitada la interceptación?
List<ProxyHttpRequestResponse> history()                  // Historial HTTP
List<ProxyHttpRequestResponse> history(ProxyHistoryFilter filter)  // Historial filtrado
void registerRequestHandler(ProxyRequestHandler handler) // Handler de requests
void registerResponseHandler(ProxyResponseHandler handler) // Handler de responses
void registerWebSocketCreationHandler(WebSocketCreationHandler handler)  // Handler de WebSocket
```

---

### Proxy.Http

**Package:** `burp.api.montoya.proxy.http`

**Interfaces principales:**
- **ProxyRequestHandler** - Handler para requests del proxy
- **ProxyResponseHandler** - Handler para responses del proxy

**Ejemplo ProxyRequestHandler:**
```java
api.proxy().registerRequestHandler(new ProxyRequestHandler() {
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest request) {
        api.logging().logToOutput("Interceptado: " + request.url());
        return ProxyRequestReceivedAction.followUserRules(request);
    }
    
    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest request) {
        return ProxyRequestToBeSentAction.followUserRules(request);
    }
});
```

---

### Proxy.WebSocket

**Package:** `burp.api.montoya.proxy.websocket`

Contiene clases relacionadas con el manejo de WebSocket en el proxy.

---

## 🎯 Herramientas de Burp

### Comparer

**Package:** `burp.api.montoya.comparer`

**Interface:** `Comparer`

Acceso a la funcionalidad de la herramienta Comparer.

```java
// Obtener acceso al Comparer
api.comparer();
```

---

### Decoder

**Package:** `burp.api.montoya.decoder`

**Interface:** `Decoder`

Acceso a la funcionalidad de la herramienta Decoder.

```java
// Obtener acceso al Decoder
api.decoder();
```

---

### Intruder

**Package:** `burp.api.montoya.intruder`

**Interface:** `Intruder`

Acceso a la funcionalidad de la herramienta Intruder.

**Métodos principales:**
```java
void registerPayloadGeneratorProvider(PayloadGeneratorProvider provider)  // Provider de payloads
void registerPayloadProcessor(PayloadProcessor processor)              // Procesador de payloads
void sendToIntruder(HttpService service, HttpRequestTemplate template)  // Enviar a Intruder
void sendToIntruder(HttpRequest request)                              // Enviar request a Intruder
```

---

### Repeater

**Package:** `burp.api.montoya.repeater`

**Interface:** `Repeater`

Acceso a la funcionalidad de la herramienta Repeater.

**Métodos:**
```java
void sendToRepeater(HttpRequest request)                    // Enviar a Repeater (nombre default)
void sendToRepeater(HttpRequest request, String name)       // Enviar a Repeater con nombre
```

---

### SiteMap

**Package:** `burp.api.montoya.sitemap`

**Interface:** `SiteMap`

Acceso a la funcionalidad del Site Map.

**Métodos:**
```java
void add(HttpRequestResponse requestResponse)              // Agregar al site map
void add(AuditIssue issue)                                 // Agregar issue
List<AuditIssue> issues()                                 // Obtener todos los issues
List<HttpRequestResponse> requestResponses()              // Obtener todos los request/response
```

---

### Scope

**Package:** `burp.api.montoya.scope`

**Interface:** `Scope`

Acceso a la funcionalidad del target scope.

**Métodos:**
```java
void includeInScope(String url)                           // Incluir URL en scope
void excludeFromScope(String url)                          // Excluir URL del scope
boolean isInScope(String url)                              // Verificar si URL está en scope
void registerScopeChangeHandler(ScopeChangeHandler handler) // Handler de cambios de scope
```

---

### Collaborator

**Package:** `burp.api.montoya.collaborator`

**Interface:** `Collaborator`

Acceso a la funcionalidad de Burp Collaborator (Professional only).

**Métodos:**
```java
CollaboratorClient createClient()                         // Crear cliente Collaborator
CollaboratorClient restoreClient(SecretKey secretKey)      // Restaurar cliente existente
PayloadGenerator defaultPayloadGenerator()               // Generador de payloads default
```

---

### Organizer

**Package:** `burp.api.montoya.organizer`

**Interface:** `Organizer`

Acceso a la funcionalidad de la herramienta Organizer.

**Métodos:**
```java
List<OrganizerItem> items()                              // Obtener todos los items
List<OrganizerItem> items(OrganizerItemFilter filter)     // Items filtrados
void sendToOrganizer(HttpRequest request)                // Enviar request a Organizer
void sendToOrganizer(HttpRequestResponse requestResponse) // Enviar request/response
```

---

## 🎨 User Interface

### UI

**Package:** `burp.api.montoya.ui`

**Clases principales:**
- **UserInterface** - Acceso a features de UI
- **UserInterface.Selection** - Información de selección del usuario
- **Theme** - Temas disponibles en Burp Suite

---

### UI.ContextMenu

**Package:** `burp.api.montoya.ui.contextmenu`

**Clases principales:**
- **ContextMenuEvent** - Evento de menú contextual
- **ContextMenuItemsProvider** - Proveedor de items de menú contextual

---

### UI.Editor

**Package:** `burp.api.montoya.ui.editor`

**Interfaces principales:**
- **HttpRequestEditor** - Editor de requests HTTP
- **HttpResponseEditor** - Editor de responses HTTP

---

### UI.Editor.Extension

**Package:** `burp.api.montoya.ui.editor.extension`

**Interface principal:**
- **HttpRequestEditorProvider** - Proveedor de editores de request

---

### UI.HotKey

**Package:** `burp.api.montoya.ui.hotkey`

**Interface principal:**
- **HotKeyHandler** - Handler de hotkeys

---

### UI.Menu

**Package:** `burp.api.montoya.ui.menu`

**Interface principal:**
- **MenuBar** - Menú de la aplicación

---

### UI.Settings

**Package:** `burp.api.montoya.ui.settings`

**Interface principal:**
- **Settings** - Configuraciones de UI

---

### UI.Swing

**Package:** `burp.api.montoya.ui.swing`

**Interface principal:**
- **SuiteTab** - Pestaña de la suite

---

## 💾 Persistence

### Persistence

**Package:** `burp.api.montoya.persistence`

**Interface:** `Persistence`

Acceso a funcionalidad de persistencia.

**Métodos:**
```java
PersistedObject extensionData()        // Almacenamiento de datos en proyecto
Preferences preferences()              // Almacenamiento de preferencias
```

---

### Project

**Package:** `burp.api.montoya.project`

**Interface:** `Project`

Acceso a funcionalidad del proyecto.

---

### Extension

**Package:** `burp.api.montoya.extension`

**Interface:** `Extension`

Acceso a funcionalidad de gestión de extensión.

**Métodos:**
```java
Path filename()                                           // Ruta del archivo de la extensión
boolean isBapp()                                          // ¿Es un BApp?
void registerUnloadingHandler(ExtensionUnloadingHandler handler)  // Handler de descarga
void setName(String name)                                 // Establecer nombre
void unload()                                             // Descargar extensión
```

---

### Logging

**Package:** `burp.api.montoya.logging`

**Interface:** `Logging`

Acceso a funcionalidad de logging.

**Métodos:**
```java
void logToOutput(String message)                          // Log a stdout
void logToError(String message)                          // Log a stderr
void logToError(String message, Throwable throwable)     // Log con stack trace
void raiseCriticalEvent(String message)                  // Evento crítico
void raiseDebugEvent(String message)                     // Evento debug
void raiseErrorEvent(String message)                     // Evento error
void raiseInfoEvent(String message)                      // Evento info
```

---

### Bambda

**Package:** `burp.api.montoya.bambda`

**Interface:** `Bambda`

Acceso a funcionalidad de Bambdas.

**Métodos:**
```java
BambdaImportResult importBambda(String script)           // Importar script Bambda
```

---

## 🔌 Extensiones

### BurpSuite

**Package:** `burp.api.montoya.burpsuite`

**Clases principales:**
- **BurpSuite** - Acceso a aplicación Burp Suite
- **ShutdownOptions** - Opciones de shutdown
- **TaskExecutionEngine** - Motor de ejecución de tareas
- **TaskExecutionEngine.TaskExecutionEngineState** - Estado del motor

---

## 🔄 WebSocket

### WebSocket

**Package:** `burp.api.montoya.websocket`

**Interface principal:** `WebSockets` - Acceso a funcionalidad WebSocket

---

### WebSocket.Extension

**Package:** `burp.api.montoya.websocket.extension`

Contiene clases relacionadas con extensiones WebSocket.

---

## 📚 Ejemplos Prácticos

### Extensión Completa

```java
package com.example;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.HttpRequestResponse;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.scanner.audit.AuditIssue;
import burp.api.montoya.scanner.audit.AuditIssueSeverity;
import burp.api.montoya.scanner.audit.AuditIssueConfidence;
import burp.api.montoya.scanner.PassiveScanCheck;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.scanner.ConsolidationAction;

public class ComprehensiveExtension implements BurpExtension {
    
    private MontoyaApi api;
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        
        // Configurar extensión
        api.extension().setName("Extensión Completa");
        
        // Registrar handlers
        registerHttpHandlers();
        registerScannerChecks();
        registerProxyHandlers();
        
        // Log inicial
        api.logging().logToOutput("Extensión completada inicializada");
    }
    
    private void registerHttpHandlers() {
        api.http().registerHttpHandler(new HttpHandler() {
            @Override
            public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
                // Loggear requests
                if (api.scope().isInScope(request.url())) {
                    api.logging().logToOutput("Request in-scope: " + request.url());
                }
                return RequestToBeSentAction.continueWith(request);
            }
            
            @Override
            public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
                // Analizar responses
                analyzeResponse(response);
                return ResponseReceivedAction.continueWith(response);
            }
        });
    }
    
    private void registerScannerChecks() {
        api.scanner().registerPassiveScanCheck(new PassiveScanCheck() {
            @Override
            public ConsolidationAction consolidateIssues(AuditIssue newIssue, AuditIssue existingIssue) {
                return ConsolidationAction.KEEP_BOTH;
            }
            
            @Override
            public AuditResult passivelyAudit(HttpRequestResponse requestResponse) {
                // Buscar información sensible
                String responseBody = requestResponse.response().bodyToString();
                
                if (responseBody.contains("password") || responseBody.contains("secret")) {
                    return AuditResult.auditIssue(
                        AuditIssue.auditIssue(
                            "Sensitive Information Exposure",
                            "Response contains potentially sensitive information",
                            "Review the response for sensitive data",
                            requestResponse.request().url(),
                            AuditIssueSeverity.MEDIUM,
                            AuditIssueConfidence.TENTATIVE,
                            null, null, null,
                            requestResponse
                        )
                    );
                }
                
                return AuditResult.auditIssues();
            }
        });
    }
    
    private void registerProxyHandlers() {
        api.proxy().registerRequestHandler(new ProxyRequestHandler() {
            @Override
            public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest request) {
                // Podemos modificar requests aquí si es necesario
                return ProxyRequestReceivedAction.continueWith(request);
            }
            
            @Override
            public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest request) {
                return ProxyRequestToBeSentAction.continueWith(request);
            }
        });
    }
    
    private void analyzeResponse(HttpResponseReceived response) {
        // Lógica de análisis de response
        String body = response.bodyToString();
        
        if (body.contains("<!DOCTYPE html>")) {
            // Es HTML
            api.logging().logToOutput("HTML response detected: " + response.url());
        }
    }
}
```

### Uso de Utilities

```java
// Base64
ByteArray data = ByteArray.byteArray("Hello World");
String encoded = api.utilities().base64Utils().encodeToString(data);
ByteArray decoded = api.utilities().base64Utils().decode(encoded);

// URL
String urlEncoded = api.utilities().urlUtils().encode("hello world");
String urlDecoded = api.utilities().urlUtils().decode(urlEncoded);

// JSON
String json = "{\"name\":\"test\",\"value\":123}";
JsonNode node = api.utilities().jsonUtils().parseJson(json);
String name = node.get("name").asText();

// Random
String randomString = api.utilities().randomUtils().randomString(10, RandomUtils.CharacterSet.ALPHANUMERIC);
```

### Manejo de Persistencia

```java
// Guardar datos en proyecto
PersistedObject data = api.persistence().extensionData();
data.setString("api_key", "secret123");
data.setInt("max_requests", 100);

// Leer datos
String apiKey = data.getString("api_key");
int maxRequests = data.getInt("max_requests");

// Preferencias
Preferences prefs = api.persistence().preferences();
prefs.setString("theme", "dark");
```

---

## 🎯 Mejores Prácticas

### 1. Inicialización
- Siempre establecer el nombre de la extensión
- Registrar handlers en el método `initialize()`
- Manejar excepciones apropiadamente

### 2. Manejo de HTTP
- Usar `RequestToBeSentAction.continueWith()` y `ResponseReceivedAction.continueWith()` por defecto
- Verificar scope antes de procesar requests
- Utilizar filtros para reducir carga

### 3. Scanner
- Implementar `consolidateIssues()` para evitar duplicados
- Usar niveles apropiados de severidad y confianza
- Proveer descripciones claras y remedios

### 4. Persistencia
- Usar `extensionData()` para datos del proyecto
- Usar `preferences()` para configuraciones de extensión
- Considerar que los datos pueden ser nulos

### 5. UI
- Registrar componentes UI en el hilo de UI (SwingUtilities.invokeLater)
- Liberar recursos cuando la extensión se descarga
- Usar temas consistentes con Burp Suite

---

## 🔗 Recursos Adicionales

- **Documentación oficial:** https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating
- **Ejemplos GitHub:** https://github.com/PortSwigger/burp-extensions-montoya-api-examples
- **Javadoc completo:** https://portswigger.github.io/burp-extensions-montoya-api/javadoc/
- **BApp Store:** https://portswigger.net/bappstore

---

*Documentación generada a partir de la API Javadoc de Burp Suite Montoya (Versión 2026.2)*