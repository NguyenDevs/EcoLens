# EcoLens: Sophisticated AI-Powered Biodiversity Ecosystem 🌿

EcoLens is an industry-grade Android application that serves as a bridge between high-performance artificial intelligence and conservation science. Unlike basic identification tools, EcoLens provides a complete ecosystem for biodiversity research, documentation, and ecological education.

Developed and refined using **IntelliJ IDEA**, the project adheres to strict clean architecture principles and leverages the full power of Google's Gemini AI.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material--Design-3-blue.svg)](https://m3.material.io/)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini-orange.svg)](https://deepmind.google/technologies/gemini/)
[![Firebase](https://img.shields.io/badge/Firebase-33.1.0-yellow.svg)](https://firebase.google.com/)
[![Built with IntelliJ IDEA](https://img.shields.io/badge/Built%20with-IntelliJ%20IDEA-blue.svg)](https://www.jetbrains.com/idea/)

---

## 🔥 Exhaustive Feature Set

### 🧠 Intelligence & Recognition
*   **Visual Recognition Engine**: Leveraging Gemini AI with multimodal capabilities to identify species from both live camera feeds and high-resolution gallery images.
*   **Contextual AI Chat**: A dedicated "Nature Expert" chat interface where users can ask complex questions about identified species, conservation status, or ecological impacts. Supports streaming AI responses for a "thinking" experience.
*   **Scientific Encyclopedia**: Automatically retrieves scientific names, full biological taxonomy (Kingdom to Species), and detailed descriptions.

### 📊 Data Management & Export
*   **Multi-Format Export Engine**: Export your findings into professional documents. Supported formats include:
    *   **DOCX**: Fully formatted Word documents with embedded images and rich text segments.
    *   **XLSX**: Structured Excel spreadsheets for scientific data analysis.
    *   **PDF**: Print-ready reports with professional styling.
    *   **JSON**: Raw data export for integration with other research tools.
*   **Advanced History Tracking**: A robust management system featuring:
    *   **Full-Text Search**: Quickly locate past discoveries.
    *   **Categorical Filtering**: Group by biological kingdom or class.
    *   **Date Range Selection**: Precise history retrieval based on discovery time.
    *   **Favorites System**: One-tap bookmarking for important research entries.

### 🛡️ Security & Integrity
*   **Biometric Authentication**: Secure your research and account data using Fingerprint or Facial recognition (BiometricPrompt integration).
*   **Encrypted Storage**: Sensitive data and API configurations are protected using hardware-level security managers.
*   **Firebase Integration**: Secure cloud synchronization of history and preferences across devices using Google Authentication.

### 🎨 Premium UI/UX
*   **Material You (MD3)**: Dynamic color themes that adapt to the user's wallpaper and environment.
*   **Smooth Motion System**: Integrated **Lottie** animations, **Robinhood Ticker** for number transitions, and **Shimmer** effects for loading states.
*   **Rich Text Support**: Full Markdown and HTML rendering within the app for professional documentation presentation.
*   **Smart Media Toolkit**: Built-in **uCrop** for precise image framing and **CameraX** for high-performance camera control.

---

## 🛠️ Detailed Technical Specification

### Development Environment & Tooling
*   **IDE**: IntelliJ IDEA / Android Studio
*   **Build System**: Gradle (Kotlin DSL)
*   **Native Support**: C++ (CMake) integration for specialized security modules.
*   **Minimum SDK**: API 31 (Android 12)
*   **Target SDK**: API 34 (Android 14)

### Core Technology Stack
| Module | Technology | Rationale |
|---|---|---|
| **AI Backend** | Google Gemini SDK (0.9.0) | State-of-the-art vision & generative models. |
| **Database** | Room Persistence Library (2.6.1) | Managed local relational data with KSP support. |
| **Networking** | Retrofit 2 + OkHttp 4 | Industry-standard REST client with custom HMAC interceptors. |
| **Architecture** | MVVM + Repository Pattern | Decoupled UI and business logic for maximum testability. |
| **Export Engine** | Apache POI (5.2.3) | Professional-grade DOCX/XLSX generation. |
| **Markdown** | Markwon (4.6.2) | Flexible HTML/Markdown rendering for content. |
| **Reactive UI** | Kotlin Coroutines & Flow | High-performance asynchronous state management. |
| **Images** | Glide 4 + uCrop | Optimized image loading and native cropping. |
| **Animations** | Lottie + Robinhood Ticker | Fluid, meaningful micro-animations. |

---

## 🌍 Localization & Accessibility
*   **Multilingual Core**: Dynamically switch between English, Vietnamese, and other supported languages without app restarts.
*   **Voice Assistant**: Integrated Text-to-Speech (TTS) engine provides audio descriptions for an accessible and hands-free experience.
*   **Adaptive Layouts**: Full support for varying screen sizes and orientations using ConstraintLayout and CoordinatorLayout.

---

## 🚀 Getting Started

### Prerequisites
1.  **IntelliJ IDEA** or **Android Studio Hedgehog** (2023.1.1+)
2.  **JDK 17**
3.  **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/)

### Installation Logic
1.  Clone the repository:
    ```bash
    git clone https://github.com/NguyenDevs/EcoLens.git
    ```
2.  Place your `google-services.json` in the `/app` folder.
3.  Configure your environment variables for `WORKER_URL` and `APP_SECRET` in `gradle.properties`.
4.  Build the project and sync Gradle files via IntelliJ/Android Studio.

---

## 🤝 Contribution & License

Contributions are welcome! If you find a bug or have a feature request, please open an issue.

**License**: Distributed under the MIT License.

---

## 📬 Contact & Support

**Lead Developer**: NguyenDevs  
**Email**: [tainguyen.devs@gmail.com](mailto:tainguyen.devs@gmail.com)  
**Project ID**: `com.nguyendevs.ecolens`

*Developed with precision using IntelliJ IDEA for the preservation of Nature.*
