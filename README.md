# EcoLens

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material--Design-3-blue.svg)](https://m3.material.io/)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini-orange.svg)](https://deepmind.google/technologies/gemini/)
[![Cloudflare](https://img.shields.io/badge/Worker-Cloudflare-f38020.svg)](https://workers.cloudflare.com/)
[![Firebase](https://img.shields.io/badge/Firebase-33.1.0-yellow.svg)](https://firebase.google.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

EcoLens is an industry-grade Android ecosystem that bridges the gap between high-performance artificial intelligence and conservation science. By orchestrating a network of global biodiversity databases and generative models, the platform empowers users to identify, document, and study species with professional accuracy.

---

## Intelligence and Data Ecosystem

EcoLens orchestrates a sophisticated biodiversity intelligence network that begins with the **iNaturalist API**, processing visual patterns and geographical coordinates to provide high-confidence species matches. This initial identification is instantly validated against the **GBIF (Global Biodiversity Information Facility)** backbone to ensure a standardized taxonomic hierarchy from Kingdom down to Species. To provide critical scientific depth, the application cross-references every discovery with the **IUCN Red List of Threatened Species** to retrieve conservation status and environmental assessment metrics.

The entire generative experience, including detailed biological descriptions and the interactive "Nature Expert" streaming chat, is powered by **Google Gemini AI**. These multi-stage API interactions are managed and secured by **Cloudflare Workers**, which handle backend orchestration and HMAC-signed verification to protect the integrity of the research metadata and API security.

## Research Management and Exporting

Every discovery is preserved within a high-performance local vault powered by **Room**, supporting advanced full-text search, categorical filtering, and date-range selection. When research findings need to move beyond the device, a custom-built export engine leveraging **Apache POI** transforms digital records into professional DOCX field reports, structured XLSX spreadsheets, or print-ready PDFs. Continuous data integrity is maintained through **Firebase Realtime Database** synchronization, ensuring that your ecological library remains consistent across all your devices.

## Security and Authentication

The platform implements multiple layers of hardware-level protection, including **BiometricPrompt** integration for biometric authentication via facial or fingerprint recognition. Sensitive configurations and API keys are shielded by specialized security modules and a C++ native bridge, while all network traffic is verified through a secure **Cloudflare** edge layer. User authentication is seamlessly integrated with **Google Sign-In**, providing a secure and frictionless gateway to the global biodiversity ecosystem.

## Design and Accessibility

The interface embodies **Material Design 3 (Material You)**, dynamically adapting its color scheme to the user's environment while maintaining a premium aesthetic through **Lottie** micro-animations and **Robinhood Ticker** transitions. **Markwon** ensures that complex scientific descriptions are rendered with full Markdown and HTML fidelity for professional presentation. For accessibility, EcoLens includes a localized **Text-to-Speech** engine and supports hot-swappable language configurations for English and Vietnamese, providing an inclusive experience for researchers and nature enthusiasts worldwide.

---

## Tech Stack

| Layer | Technology |
|---|---|
| AI Engine | Google Gemini SDK 0.9.0 |
| Cloud Infrastructure | Cloudflare Workers (Backend Orchestration) |
| Biodiversity Data | iNaturalist (ID), GBIF (Taxonomy), IUCN (Conservation) |
| Database | Room 2.6.1 with KSP |
| Export Engine | Apache POI 5.2.3 (DOCX, XLSX, PDF, JSON) |
| Networking | Retrofit 2 + OkHttp 4 (HMAC Interceptors) |
| Architecture | MVVM + Repository Pattern |
| Security | BiometricPrompt + C++ Native Security Modules |
| UI Framework | Material Design 3 · Shimmer · ExpandableLayout |
| Animations | Lottie · Robinhood Ticker |
| Multimedia | CameraX · Glide 4 · uCrop |
| Backend | Firebase (Auth + Database + Storage) |

**Minimum SDK:** API 31 (Android 12) · **Target SDK:** API 34 (Android 14)

---

## Getting Started

**Prerequisites:** Android Studio Hedgehog (2023.1.1+) or IntelliJ IDEA, JDK 17, and a Gemini API key from [Google AI Studio](https://aistudio.google.com/).

```bash
git clone https://github.com/NguyenDevs/EcoLens.git
```

1. Place your `google-services.json` in the `/app` directory.
2. Add `WORKER_BASE_URL`, `FIREBASE_DATABASE_URL`, and `APP_SECRET` to your environment configuration or `gradle.properties`.
3. Sync Gradle files and build the project via IntelliJ IDEA or Android Studio.

---

## Contributing

Bug reports and feature requests are welcome via [GitHub Issues](https://github.com/NguyenDevs/EcoLens/issues). Pull requests are appreciated — please open an issue first to discuss significant changes.

---

## Contact

**NguyenDevs** · [tainguyen.devs@gmail.com](mailto:tainguyen.devs@gmail.com) · `com.nguyendevs.ecolens`

*Developed with precision using IntelliJ IDEA for the preservation of Nature.*
