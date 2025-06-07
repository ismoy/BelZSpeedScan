# 🚧 BelZSpeedScan Roadmap

This document defines the strategic goals to transform **BelZSpeedScan** into a universal scanning solution for all mobile and web platforms, using a single shared codebase with Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP).

---

## 🟢 Short Term (1–3 months)

### 🔧 Technical Improvements
- [ ] Refactor core logic into `commonMain` to enhance portability
- [ ] Modularize camera renderer and decoder logic
- [ ] Abstract native interfaces for external bindings (e.g., React Native, Flutter)

### 🖼️ UI/UX via Compose Multiplatform
- [ ] Support Compose Multiplatform UI (Desktop and Web - beta)
- [ ] Support automatic dark/light mode switching
- [ ] Customizable overlay via composable UI props

### 📖 Documentation & Community
- [ ] Provide full example projects (KMP/CMP) for Android, iOS, Desktop
- [ ] Improve integration guides step-by-step
- [ ] Provide ready-to-use template with KMP + JetBrains Compose setup

---

## 🚀 Medium Term (3–6 months)

### 🧩 Cross-Framework Integrations
- [ ] NPM compatibility via Kotlin/JS for React Native
- [ ] Create bridge/wrapper for React Native usage with documentation
- [ ] Experimental Flutter support (via platform channels + host app support)

### 🔍 New Features
- [ ] Multi-code simultaneous scanning
- [ ] Continuous scanning mode (loop-based)
- [ ] Per-platform custom sound feedback
- [ ] Auto-detection of code type (QR, EAN, etc.)

### 🧪 Testing & Quality
- [ ] Cross-platform automated testing (Android, iOS, Desktop)
- [ ] CI/CD setup for publishing multiplatform artifacts (Maven, NPM)
- [ ] Visual debug mode with bounding boxes and scan overlays

---

## 🌐 Long Term (6–12 months)

### 🌍 Platform Expansion
- [ ] Web compatibility (via Kotlin/JS + WebAssembly + WebRTC)
- [ ] Wearable support (Wear OS, watchOS)
- [ ] SDK/API for third-party integration (Maven/NPM plugins)

### 📦 Distribution & Tooling
- [ ] Official NPM release as `@belz/scan`
- [ ] Plugin for JetBrains Marketplace
- [ ] Compatibility with other Multiplatform libraries via Gradle modules

### 👥 Community & Ecosystem
- [ ] Simplified contribution workflow (templates, linters, CI helpers)
- [ ] Community events and hackathons
- [ ] Establish a small team of core maintainers

### 🧩 Optional Future Additions
- [ ] External camera support (USB cameras, browser camera APIs)
- [ ] Integration with OCR libraries (text recognition)
- [ ] AI-assisted scan correction (blur/shadow detection)

---

## 🤝 How to Contribute

1. Check [issues labeled `help wanted`](https://github.com/ismoy/BelZSpeedScan/issues)
2. Read the [Contributing Guide](CONTRIBUTING.md)
3. Join the [GitHub Discussions](https://github.com/ismoy/BelZSpeedScan/discussions/)

---

## 📝 Final Notes

- This roadmap is a living document and may evolve based on community feedback and technical progress.
- Stability and ease of integration in modern mobile frameworks are always the top priority.  
