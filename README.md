# BurpIA - AI-Powered Security Analysis for Burp Suite

<div align="center">

![BurpIA Logo](assets/logo.png)

**Análisis de Seguridad con Inteligencia Artificial para Burp Suite**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Burp Suite](https://img.shields.io/badge/Burp%20Suite-Montoya%20API%202026.2-orange.svg)](https://portswigger.net/burp)
[![Java](https://img.shields.io/badge/Java-17+-red.svg)](https://www.oracle.com/java/)
[![Version](https://img.shields.io/badge/version-1.0.0-green.svg)](https://github.com/jaimearestrepo/BurpIA/releases)

[Features](#-features) • [Installation](#-installation) • [Usage](#-usage) • [Configuration](#-configuration) • [Contributing](#-contributing)

</div>

---

## 🎯 Overview

**BurpIA** is a professional extension for Burp Suite Professional that leverages multiple AI models to automatically analyze HTTP traffic and identify security vulnerabilities. It performs passive security analysis during your normal browsing or testing workflow, highlighting potential issues in real-time.

### Key Capabilities

- 🔍 **Passive Analysis** - Analyzes all HTTP traffic without interfering with your workflow
- 🤖 **Multi-Provider Support** - Works with OpenAI, Claude, Gemini, Z.ai, Minimax, and Ollama
- 📊 **Real-time Detection** - Identifies vulnerabilities from the OWASP Top 10 and beyond
- 🎨 **Professional UI** - Beautiful, intuitive interface with real-time statistics
- 🔄 **Smart Retry** - Automatic retry system with exponential backoff
- 📝 **Custom Prompts** - Configure your own analysis prompts for specialized testing
- 🌍 **Multilingual** - Available in Spanish and English

---

## ✨ Features

### 🎯 Intelligent Vulnerability Detection

BurpIA uses state-of-the-art AI models to detect:

- **Injection Flaws**: SQLi, XSS, XXE, SSTI, Command Injection, LDAP, XPath
- **Authentication Issues**: Weak authentication, session management problems
- **Access Control Weaknesses**: Broken authorization, privilege escalation
- **Cryptographic Failures**: Cleartext transmission, weak ciphers, missing HSTS
- **Sensitive Data Exposure**: Data leaks in headers, URLs, or response bodies
- **CSRF, SSRF, Open Redirect**: Cross-site request forgery and server-side request forgery
- **Business Logic Flaws**: Application-specific vulnerabilities
- **Information Disclosure**: Server versions, stack traces, debug parameters
- **And Many More**: Covers OWASP Top 10 plus additional vulnerability classes

### 🤖 Supported AI Providers

| Provider | Models | Context Window |
|----------|--------|----------------|
| **OpenAI** | GPT-5.x, GPT-4o, o1 | 128k tokens |
| **Claude** | Claude Sonnet 4-6, Opus, Haiku | 200k tokens |
| **Gemini** | Gemini 3 Pro, Flash, Deep Think | 1M tokens |
| **Z.ai** | GLM-5, GLM-4.x | 128k tokens |
| **Minimax** | MiniMax M2.5 | 32k tokens |
| **Ollama** | Local models (Llama, Mistral, etc.) | Variable |

### 🎨 Professional Interface

- **📊 Real-time Statistics Dashboard** - Live metrics on requests analyzed, findings discovered, and performance
- **📋 Findings Table** - Sortable, filterable list of discovered vulnerabilities with severity indicators
- **🔄 Task Manager** - Monitor and control analysis tasks with pause/resume/cancel capabilities
- **📝 Console Log** - Detailed logging with color-coded severity levels
- **⚙️ Configuration Dialog** - Easy-to-use interface for managing AI providers and settings
- **📤 Burp Suite Integration** - Send findings directly to Repeater or Intruder for manual validation and fuzzing

### 🔧 Advanced Features

- **Smart Deduplication** - SHA-256 hash-based request deduplication to avoid redundant analysis
- **Rate Limiting** - Configurable concurrent analysis limits with automatic throttling
- **Exponential Backoff Retry** - 3 immediate retries + 30s/60s/90s backoff for transient failures
- **Export Capabilities** - Export findings to CSV or JSON for reporting (excludes ignored findings)
- **Ignore System** - Mark findings as ignored to exclude from exports while keeping them visible
- **Thread-Safe Architecture** - Built with proper concurrency controls for production use
- **Montoya API 2026.2 Compliant** - Uses the latest Burp Suite extension API
- **Multi-Provider Config Persistence** - Saves API keys and settings per provider in `.burpia.json`

---

## 📦 Installation

### Prerequisites

- **Burp Suite Professional** (2024.10 or later with Montoya API)
- **Java 17** or higher
- **API Key** from your preferred AI provider

### Quick Install

1. **Download the latest release** from [Releases](https://github.com/jaimearestrepo/BurpIA/releases)
   ```
   BurpIA-1.0.0.jar
   ```

2. **Open Burp Suite Professional**

3. **Navigate to**: `Extender` → `Extensions` → `Add`

4. **Select**: `BurpIA-1.0.0.jar`

5. **Click**: `Next` → `Next` → `Finish`

6. **Go to**: `BurpIA` tab

7. **Configure**:
   - Select your AI provider
   - Enter your API key
   - Select a model
   - Adjust settings (max concurrent, delay, etc.)

8. **Start analyzing** - Navigate to websites and BurpIA will automatically analyze traffic!

### Build from Source

```bash
# Clone the repository
git clone https://github.com/jaimearestrepo/BurpIA.git
cd BurpIA

# Build with Gradle
./gradlew clean fatJar

# JAR will be generated at:
# build/libs/BurpIA-1.0.0.jar
```

---

## 🚀 Usage

### Basic Workflow

1. **Configure Extension** (first time only)
   - Open BurpIA configuration
   - Select AI provider and enter API key
   - Test connection
   - Save settings

2. **Start Analysis**
   - Browse to your target application
   - BurpIA automatically intercepts and analyzes HTTP traffic
   - Findings appear in real-time in the "Hallazgos" tab

3. **Review Findings**
   - Go to "🔍 Hallazgos" tab
   - Sort by severity, filter by keywords
   - Click on findings to view details
   - Right-click to delete or export

4. **Monitor Performance**
   - Check "📊 Estadísticas" panel for real-time metrics
   - View "📝 Consola" for detailed logs
   - Adjust settings if needed

### Advanced Features

#### Custom Analysis Prompts

Create your own vulnerability detection rules:

1. Go to configuration dialog
2. Select "📝 Prompt" tab
3. Edit the analysis prompt
4. Must include `{REQUEST}` token where HTTP request should be inserted
5. Expected response format: `{"hallazgos": [{"descripcion": "...", "severidad": "...", "confianza": "..."}]}`

#### Task Management

From the "📋 Tareas" tab you can:
- **Pause/Resume** all tasks or individual tasks
- **Cancel** running tasks
- **Retry** failed tasks
- **View error details** for failed analyses
- **Clean** completed tasks

Right-click on tasks for dynamic context menus based on task state.

#### Export Findings

Right-click on findings table:
- **📤 Send to Repeater** - Send HTTP request to Burp's Repeater for manual testing
- **🔍 Send to Intruder** - Send HTTP request to Burp's Intruder for fuzzing and validation
- **🚫 Ignore** - Mark as ignored (excluded from exports but visible in table)
- **🗑️ Delete** - Remove from table completely
- **Export to CSV** - For spreadsheets (excludes ignored findings)
- **Export to JSON** - For integration with other tools (excludes ignored findings)

---

## ⚙️ Configuration

### Provider-Specific Settings

#### OpenAI
- **API URL**: `https://api.openai.com/v1`
- **Models**: GPT-5.3-codex, GPT-5.2-pro, GPT-4o, o1
- **Max Tokens**: 4096 (default)
- **API Key**: Required

#### Claude (Anthropic)
- **API URL**: `https://api.anthropic.com/v1`
- **Models**: Claude Sonnet 4-6, Claude 3 Opus, Claude 3 Haiku
- **Max Tokens**: 8192 (default)
- **API Key**: Required
- **Headers**: `x-api-key`, `anthropic-version: 2023-06-01`

#### Gemini (Google)
- **API URL**: `https://generativelanguage.googleapis.com/v1beta`
- **Models**: Gemini 3 Pro, Flash, Deep Think
- **Max Tokens**: 8192 (default)
- **API Key**: Required
- **Headers**: `x-goog-api-key`

#### Ollama (Local)
- **API URL**: `http://localhost:11434`
- **Models**: User-provided (Llama, Mistral, etc.)
- **Max Tokens**: 4096 (default)
- **API Key**: Not required
- **Setup**: Run `ollama serve` before using

### Global Settings

| Setting | Default | Range | Description |
|---------|---------|-------|-------------|
| **Max Concurrent** | 3 | 1-10 | Maximum simultaneous API requests |
| **Delay (seconds)** | 5 | 0-60 | Delay between requests per thread |
| **AI Timeout (seconds)** | 60 | 10-300 | Maximum wait time for AI response |
| **Verbose Mode** | Off | On/Off | Enable detailed logging |

### Performance Tuning

- **High-throughput scanning**: Increase Max Concurrent (5-10), decrease Delay (1-2s)
- **Rate-limited APIs**: Decrease Max Concurrent (1-2), increase Delay (10-30s)
- **Local Ollama**: Max Concurrent (3-5), Delay (0-1s) - very fast!

---

## 🏗️ Architecture

### Technology Stack

- **Language**: Java 17
- **Build Tool**: Gradle 8.x
- **HTTP Client**: OkHttp 4.x
- **JSON Parsing**: Gson 2.x
- **UI Framework**: Java Swing
- **Burp API**: Montoya API 2026.2

### Design Patterns

- **Observer Pattern**: Callbacks for asynchronous analysis
- **Singleton**: Shared configuration (GestorConfiguracion)
- **Strategy**: Interchangeable AI providers
- **Template Method**: Analysis flow with retry
- **Factory**: Creation of prompts and JSON objects
- **Lock**: ReentrantLock for thread synchronization

### Thread Safety

All shared state is protected with:
- `ReentrantLock` for complex synchronization
- `ConcurrentHashMap` for lock-free maps
- `AtomicInteger` for counters
- `SwingUtilities.invokeLater()` for UI updates

### Package Structure

```
com.burpia/
├── ExtensionBurpIA.java          # Extension entry point
├── ManejadorHttpBurpIA.java      # HTTP handler
├── analyzer/
│   ├── AnalizadorAI.java          # AI analysis engine
│   ├── ConstructorPrompts.java    # Prompt builder
├── config/
│   ├── ConfiguracionAPI.java       # Configuration POJO
│   ├── GestorConfiguracion.java    # JSON persistence
│   └── ProveedorAI.java            # Provider definitions
├── model/
│   ├── Estadisticas.java           # Thread-safe statistics
│   ├── Hallazgo.java               # Finding model
│   ├── ResultadoAnalisisMultiple.java # Analysis result
│   ├── SolicitudAnalisis.java      # Analysis request
│   └── Tarea.java                   # Task model
├── ui/
│   ├── PestaniaPrincipal.java      # Main UI tab
│   ├── PanelHallazgos.java         # Findings panel
│   ├── PanelTareas.java            # Task panel
│   ├── PanelConsola.java           # Console panel
│   ├── PanelEstadisticas.java      # Statistics panel
│   └── DialogoConfiguracion.java    # Config dialog
└── util/
    ├── GestorConsolaGUI.java        # Console manager
    ├── GestorTareas.java            # Task manager
    ├── LimitadorTasa.java           # Rate limiter
    ├── DeduplicadorSolicitudes.java  # Request deduplication
    ├── ReparadorJson.java           # JSON repair utility
    ├── ProbadorConexionAI.java      # Connection tester
    └── ParserRespuestasAI.java      # Response parser
```

---

## 🧪 Testing

### Manual Testing Checklist

- [ ] Connection test works for each provider
- [ ] Analysis generates findings in table
- [ ] Statistics update in real-time
- [ ] Context menus work (delete, retry, etc.)
- [ ] Export to CSV/JSON works
- [ ] Pause/resume/cancel tasks works
- [ ] Verbose logging shows details
- [ ] Custom prompts work correctly
- [ ] Rate limiting respects max concurrent
- [ ] Deduplication prevents duplicate analysis

### Test Sites

- **OWASP Juice Shop**: `https://juice-shop.herokuapp.com`
- **WebGoat**: `https://webgoat.github.io/WebGoat/`
- **Testphp.vulnweb.com**: `http://testphp.vulnweb.com`

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup

```bash
# Clone repository
git clone https://github.com/jaimearestrepo/BurpIA.git
cd BurpIA

# Build
./gradlew clean build

# Run tests (when implemented)
./gradlew test

# Generate fat JAR
./gradlew fatJar
```

### Code Style

- Follow Java naming conventions
- Use SwingUtilities.invokeLater() for all UI updates
- Never call Burp API from EDT (use executor service)
- Add Javadoc to all public methods
- Test with multiple AI providers before submitting PR

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 Jaime Andrés Restrepo (DragonJAR.org)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 👨‍💻 Author

**Jaime Andrés Restrepo**

- 🌐 Website: [DragonJAR.org](https://www.dragonjar.org)
- 📧 Contact: [DragonJAR Contact](https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica)
- 🔗 GitHub: [@jaimearestrepo](https://github.com/jaimearestrepo)

---

## 🙏 Acknowledgments

- **PortSwigger** - For the excellent Burp Suite and Montoya API
- **OpenAI** - For GPT models
- **Anthropic** - For Claude models
- **Google** - For Gemini models
- **Z.ai (Zhipu)** - For GLM models
- **Ollama** - For local model inference

---

## 📚 Resources

- [Burp Suite Extensions](https://portswigger.net/burp/extender/)
- [Montoya API Documentation](https://portswigger.github.io/burp-extensions-montoya-api/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [BurpIA GitHub](https://github.com/jaimearestrepo/BurpIA)

---

<div align="center">

**Made with ❤️ by DragonJAR.org**

![Burp Suite](https://img.shields.io/badge/Burp%20Suite-Professional-orange.svg)
![Security](https://img.shields.io/badge/Security-OWASP%20Top%2010-red.svg)
![AI](https://img.shields.io/badge/Powered%20by-AI-purple.svg)

</div>
