# Changelog

All notable changes to BurpIA will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-19

### Added
- Initial release of BurpIA for Burp Suite Professional
- Multi-provider AI support (OpenAI, Claude, Gemini, Z.ai, Minimax, Ollama)
- Passive HTTP traffic analysis
- Real-time vulnerability detection
- Professional UI with statistics dashboard
- Custom prompt configuration
- Export findings to CSV/JSON (excludes ignored findings)
- Task management (pause/resume/cancel)
- Smart deduplication with SHA-256 hashing
- Rate limiting with configurable concurrent requests
- Exponential backoff retry system (3 immediate + 30s/60s/90s)
- Thread-safe architecture with proper concurrency controls
- Color-coded console logging
- Context menus for findings and tasks
- Multi-language support (Spanish/English)
- **Ignore system**: Mark findings as ignored (excluded from exports but visible)
- **Delete findings**: Remove findings from table completely
- **Burp Suite integration**: Send findings to Repeater for manual validation
- **Burp Suite integration**: Send findings to Scanner for automated verification
- **Multi-provider config persistence**: Saves API keys per provider in `.burpia.json`
- Dynamic context menus based on task/finding state
- Utility methods: `esSeveridadValida()`, `esEstadoValido()`, `esActiva()`, `esFinalizada()`
- **Unit test suite** with JUnit 5 and Mockito (13+ test classes)
- **Responsive layout** for PanelEstadisticas:
  - Horizontal layout for wide windows (≥900px)
  - Vertical layout for narrow windows
  - Dynamic button positioning based on available space

### Security
- API keys are never logged (completely hidden from output)
- Secure credential storage per provider
- No sensitive data in logs

### Performance
- Optimized static resource filtering (O(1) with Set cache)
- FixedThreadPool for bounded thread creation
- Request deduplication to prevent redundant analysis
- Efficient JSON parsing with Gson

### Bug Fixes
- Fixed race condition in GestorTareas.limpiarTarea()
- Fixed NullPointerException in ConfiguracionAPI.promptConfigurable
- Fixed memory leak in ExecutorService (proper shutdown)
- Fixed API SiteMap calls from EDT (Montoya API compliance)
- Fixed Repeater/Scanner calls from EDT (Montoya API compliance)
- Fixed HashMap synchronization issues
- Improved error handling in all API calls
- Fixed provider config loading when switching providers

### Documentation
- Comprehensive README with installation and usage guides
- Contributing guidelines with code style rules
- AGENTS.md with complete project status
- PLUGINS-BURP.md with Burp API documentation

### Architecture
- Montoya API 2026.2 compliant
- Proper EDT separation with SwingUtilities.invokeLater()
- ReentrantLock for complex synchronization
- ConcurrentHashMap for lock-free operations
- AtomicInteger for thread-safe counters
- Resource cleanup in shutdown() methods
- All Burp API calls run in separate threads (never from EDT)

## [Unreleased]

### Planned
- [ ] Integration tests
- [ ] Additional AI providers
- [ ] Custom vulnerability signatures
- [ ] Reporting templates
- [ ] Dark mode theme
- [ ] Plugin system for custom analyzers
