# BurpIA

## 🔍 AI-Powered Security Analysis for Burp Suite

**Professional extension that leverages multiple AI models to automatically analyze HTTP traffic and identify security vulnerabilities.**

### Features

- **Passive Analysis** - Analyzes all HTTP traffic without interfering
- **Multi-Provider Support** - OpenAI, Claude, Gemini, Z.ai, Minimax, Ollama
- **Real-time Detection** - OWASP Top 10 and beyond
- **Professional UI** - Beautiful, intuitive interface with responsive layout
- **Smart Retry** - Automatic retry with exponential backoff
- **Custom Prompts** - Configure your own analysis
- **Comprehensive Tests** - Unit test suite with 13+ test classes

### Quick Links

- 📖 [README](../README.md)
- 📦 [Installation](../README.md#-installation)
- 🚀 [Usage](../README.md#-usage)
- 🤝 [Contributing](../CONTRIBUTING.md)
- 📜 [License](../LICENSE)
- 📋 [Changelog](../CHANGELOG.md)

### Quick Install

```bash
# Download latest release
wget https://github.com/jaimearestrepo/BurpIA/releases/download/v1.0.0/BurpIA-1.0.0.jar

# Or build from source
git clone https://github.com/jaimearestrepo/BurpIA.git
cd BurpIA
./gradlew fatJar

# Load in Burp Suite Professional
# Extender → Extensions → Add → Select BurpIA-1.0.0.jar
```

### Requirements

- Burp Suite Professional 2024.10+
- Java 17+
- API Key from preferred AI provider

### Author

**Jaime Andrés Restrepo** - [DragonJAR.org](https://www.dragonjar.org)

---

<div align="center">

**Made with ❤️ by DragonJAR.org**

[![GitHub stars](https://img.shields.io/github/stars/jaimearestrepo/BurpIA?style=social)](https://github.com/jaimearestrepo/BurpIA/stargazers)
[![GitHub forks](https://img.shields.io/github/stars/jaimearestrepo/BurpIA?style=social)](https://github.com/jaimearestrepo/BurpIA/network/members)

</div>
