# EcoLens

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material--Design-3-blue.svg)](https://m3.material.io/)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini-orange.svg)](https://deepmind.google/technologies/gemini/)
[![Cloudflare](https://img.shields.io/badge/Gateway-Cloudflare-f38020.svg)](https://workers.cloudflare.com/)
[![Firebase](https://img.shields.io/badge/Firebase-33.1.0-yellow.svg)](https://firebase.google.com/)

EcoLens is an advanced biodiversity integration platform that bridges the gap between field research and digital data orchestration. Instead of relying on a single model, it coordinates a network of global biodiversity databases and Large Language Models (LLMs) to provide researchers with standardized, multi-sourced ecological insights.

---

## System Architecture and Data Orchestration

EcoLens implements a multi-layered data pipeline to ensure scientific accuracy and taxonomic integrity:

1.  **Species Identification:** Leverages the **iNaturalist API** for visual pattern matching and geolocation-based suggestions.
2.  **Taxonomic Standardization:** Directly queries the **GBIF (Global Biodiversity Information Facility)** backbone to build standardized taxonomic hierarchies (from Kingdom to Species).
3.  **Conservation Assessment:** Cross-references discoveries with the **IUCN Red List** and performs real-time web scraping of the **Vietnam Red Data Book** for localized conservation status.
4.  **Generative Insights:** Orchestrates **Google Gemini AI** to synthesize biological descriptions and provide an interactive "Nature Expert" streaming chat experience.

## EcoLens Gateway (Middleware)

The platform features a dedicated middleware layer built on **Cloudflare Workers** to manage scalability and secure external resources:
*   **Resource Management:** A dynamic pooling system manages API throughput, ensuring consistent uptime for AI-powered features by intelligently handling rate limits and quotas.
*   **Request Security:** Implements **HMAC-SHA256 signature verification** for all client-server communications, preventing replay attacks and unauthorized API access.
*   **Credential Isolation:** Sensitive API tokens (Gemini, IUCN, iNaturalist) are maintained exclusively within the server-side environment, never exposed within the mobile application binary.

## Research Management and Exporting

Designed for practical field use, EcoLens provides robust tools for data preservation and reporting:
*   **Local Vault:** Powered by **Room Database**, supporting full-text search, categorical filtering, and offline research management.
*   **Cloud Synchronization:** Integrates **Firebase Realtime Database** for seamless data consistency across authorized devices.
*   **Export Engine:** A custom implementation using **Apache POI** that transforms digital records into professional DOCX reports, structured XLSX spreadsheets, or print-ready PDFs.

## User Experience and Engineering

*   **Material You:** Fully implements **Material Design 3**, featuring dynamic color adaptation and a premium aesthetic with **Lottie** micro-animations.
*   **Performance:** Built on **Kotlin Coroutines and Flow** for efficient, non-blocking data streams and high-responsiveness during complex network operations.
*   **Native Integration:** Utilizes **C++ Native Modules (JNI)** for core logic obfuscation and biometric security integration.

---

## Interface Gallery

<details>
<summary><b>Click to expand the Application Interface Gallery</b></summary>
<br>

The EcoLens interface is optimized for scientific clarity while maintaining a fluid, modern mobile experience.

### Home and Identification
<p align="center">
  <a href="screenshots/home.jpg"><img src="screenshots/home.jpg" height="600" alt="Home"></a>
  <a href="screenshots/identify.jpg"><img src="screenshots/identify.jpg" height="600" alt="Identity"></a>
</p>

### Research History
<p align="center">
  <a href="screenshots/history.jpg"><img src="screenshots/history.jpg" height="600" alt="History"></a>
  <a href="screenshots/history_detail.jpg"><img src="screenshots/history_detail.jpg" height="600" alt="History Detail"></a>
</p>

### AI Nature Assistant
<p align="center">
  <a href="screenshots/assistant.jpg"><img src="screenshots/assistant.jpg" height="600" alt="Assistant"></a>
  <a href="screenshots/chat.jpg"><img src="screenshots/chat.jpg" height="600" alt="Chat"></a>
</p>

### System Settings
<p align="center">
  <a href="screenshots/setting.jpg"><img src="screenshots/setting.jpg" height="600" alt="Setting"></a>
</p>

</details>

---

## Tech Stack

| Layer | Technology |
|---|---|
| **AI Integration** | Google Gemini SDK 0.9.0 (Dynamic Resource Pooling) |
| **EcoLens Gateway** | Cloudflare Workers · HMAC-SHA256 Auth · Rate Limiting |
| **Biodiversity Data** | iNaturalist (ID), GBIF (Taxonomy), IUCN Red List (Status) |
| **Research Storage** | Room 2.6.1 with KSP · Firebase Realtime Database |
| **Export Engine** | Apache POI 5.2.3 (DOCX, XLSX, PDF) |
| **Networking** | Retrofit 2 + OkHttp 4 (HMAC Interceptors) |
| **Security** | BiometricPrompt · C++ Native Obfuscation · Replay Detection |
| **UI Framework** | Material Design 3 · Shimmer · Lottie |
| **Multimedia** | CameraX · Glide 4 · uCrop |

---

## Getting Started

**Prerequisites:** Android Studio Hedgehog (2023.1.1+) or IntelliJ IDEA, JDK 17, and access to the EcoLens Gateway.

1. **Infrastructure Setup**: The **EcoLens Gateway** (Cloudflare Worker) source code is private to ensure API key security. Contact the Lead Developer for a deployment template.
2. **Firebase**: Place your `google-services.json` in the `/app` directory.
3. **Environment**: Configure `WORKER_BASE_URL` and `APP_SECRET` in your `gradle.properties`.
4. **Build**: Sync and build the project via Android Studio.

---

## Contact

**NguyenDevs** · [tainguyen.devs@gmail.com](mailto:tainguyen.devs@gmail.com)

*Developed for the preservation of Nature through Engineering Excellence.*

