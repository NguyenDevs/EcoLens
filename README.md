# EcoLens: AI-Powered Biodiversity Identification 🌿

EcoLens is a sophisticated Android application designed to bridge the gap between technology and nature. By leveraging state-of-the-art Artificial Intelligence, EcoLens empowers users to identify and learn about various species of plants and animals instantly through their camera lens.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material--Design-3-blue.svg)](https://m3.material.io/)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini-orange.svg)](https://deepmind.google/technologies/gemini/)
[![Firebase](https://img.shields.io/badge/Firebase-Auth-yellow.svg)](https://firebase.google.com/)

---

## ✨ Key Features

- **🚀 Real-time AI Identification**: Instantly identify species using the camera or gallery, powered by Google's Gemini AI.
- **💬 AI-Powered Conservation Chat**: Engage in deep conversations with an AI expert about your discoveries, conservation efforts, or ecological facts.
- **📖 Comprehensive Encyclopedia**: Access detailed information including:
    - Scientific and common names.
    - Full biological taxonomy.
    - Habitat, distribution, and characteristics.
    - **Conservation Status** integrated with IUCN Red List data.
- **🎙️ Interactive Voice Assistant**: Built-in Text-to-Speech (TTS) capabilities to read species details aloud, providing an accessible learning experience.
- **🔍 Smart Search & Explore**: Discover new species or search for specific ones with an intuitive, modern interface.
- **📂 History & Personal Library**: Save your discoveries to a local database (Room) and sync them across devices using Firebase.
- **🌎 Multilingual Support**: Fully localized in multiple languages (English, Vietnamese, etc.) with dynamic language switching.
- **🌓 Adaptive Theme**: Stunning Material You implementation with full support for Light and Dark modes.
- **📍 Location Awareness**: Capture and store discovery locations to build your own personal biodiversity map.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack (Material Design 3) |
| **AI Backend** | Google Gemini AI |
| **Networking** | Retrofit 2 & OkHttp |
| **Local Database** | Room Persistence Library |
| **Authentication** | Firebase Auth (Google Sign-In) |
| **Image Handling** | Glide & uCrop |
| **Architecture** | MVVM (Model-View-ViewModel) with Clean Architecture principles |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (API Level 34)

### Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/NguyenDevs/EcoLens.git
   ```
2. **Setup Firebase:**
   - Create a new project on the [Firebase Console](https://console.firebase.google.com/).
   - Add your Android app and download `google-services.json`.
   - Place `google-services.json` in the `app/` directory.
3. **Configure API Keys:**
   - Obtain a Gemini API key from [Google AI Studio](https://aistudio.google.com/).
   - (Optional) Obtain iNaturalist/GBIF API keys if needed for extended data.
4. **Build & Run:**
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Run the app on an emulator or physical device.

---

## 🏗️ Architecture

EcoLens follows the **MVVM (Model-View-ViewModel)** architectural pattern to ensure a highly maintainable and testable codebase:

- **View**: Activity/Fragment-based UI using ViewBinding.
- **ViewModel**: Manages UI state using Kotlin Flows and handles business logic coordination.
- **Repository**: Acts as a single source of truth for data, coordinating between remote AI/APIs and the local Room database.
- **Managers**: Specialized handlers for AI identification, location services, and playback.

---

## 📸 Screenshots

| Home | Identification | History |
| :---: | :---: | :---: |
| ![Home](https://raw.githubusercontent.com/NguyenDevs/EcoLens/main/screenshots/home.png) | ![Identify](https://raw.githubusercontent.com/NguyenDevs/EcoLens/main/screenshots/identify.png) | ![History](https://raw.githubusercontent.com/NguyenDevs/EcoLens/main/screenshots/history.png) |

*(Note: Replace with actual screenshot links)*

---

## 🤝 Contribution

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License & Contact

Distributed under the MIT License. See `LICENSE` for more information.

**Project Lead**: NguyenDevs  
**Email**: contact@nguyendevs.com  
**Website**: [nguyendevs.com](https://nguyendevs.com)

---
*Developed with ❤️ for Nature and Technology.*
