# EcoLens

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material--Design-3-blue.svg)](https://m3.material.io/)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini-orange.svg)](https://deepmind.google/technologies/gemini/)
[![Firebase](https://img.shields.io/badge/Firebase-33.1.0-yellow.svg)](https://firebase.google.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

EcoLens is an Android application that bridges AI and conservation science. Point your camera at any plant or animal and receive instant species identification powered by Google Gemini — complete with scientific taxonomy, ecological context, and a full research history that syncs across your devices.

---

## Features

### AI-Powered Identification

EcoLens uses Gemini's multimodal capabilities to identify species from live camera feeds or gallery images. Each identification surfaces the full biological taxonomy from Kingdom down to Species, along with conservation status and ecological notes. A built-in "Nature Expert" chat lets you ask follow-up questions with streaming responses, so the AI feels like a live conversation rather than a lookup.

### Research & Data Management

Every identification is saved to a searchable, filterable history. You can search by name, filter by biological kingdom or class, narrow results to a date range, and bookmark important entries. When it's time to share or archive your findings, EcoLens exports to DOCX, XLSX, PDF, or raw JSON — each format properly structured for its intended audience, from a field report to a data pipeline.

### Security

User data is protected through biometric authentication (fingerprint and face unlock via BiometricPrompt) and hardware-level encrypted storage for API configurations. Cloud sync runs over Firebase with Google Authentication, so your history is available on any device without compromising local security.

### UI & Accessibility

The interface follows Material You (MD3), adapting its color scheme dynamically to the user's wallpaper. Lottie animations, Shimmer loading states, and Robinhood Ticker transitions give the app a polished, responsive feel. Full Markdown and HTML rendering is supported for rich content display. The app also ships a Text-to-Speech engine for hands-free, accessible descriptions, and supports English and Vietnamese with hot-swap localization — no restart required.

---

## Tech Stack

| Layer | Technology |
|---|---|
| AI | Google Gemini SDK 0.9.0 |
| Architecture | MVVM + Repository Pattern |
| Database | Room 2.6.1 with KSP |
| Networking | Retrofit 2 + OkHttp 4 (HMAC interceptors) |
| Export | Apache POI 5.2.3 |
| Reactive | Kotlin Coroutines & Flow |
| Images | Glide 4 + uCrop + CameraX |
| Animations | Lottie + Robinhood Ticker |
| Markdown | Markwon 4.6.2 |
| Backend | Firebase (Auth + Firestore) |
| Native | C++ via CMake (security modules) |

**Minimum SDK:** API 31 (Android 12) · **Target SDK:** API 34 (Android 14)

---

## Getting Started

**Prerequisites:** Android Studio Hedgehog (2023.1.1+) or IntelliJ IDEA, JDK 17, and a Gemini API key from [Google AI Studio](https://aistudio.google.com/).

```bash
git clone https://github.com/NguyenDevs/EcoLens.git
```

1. Place your `google-services.json` in the `/app` directory.
2. Add `WORKER_URL` and `APP_SECRET` to `gradle.properties`.
3. Sync Gradle and build.

---

## Contributing

Bug reports and feature requests are welcome via [GitHub Issues](https://github.com/NguyenDevs/EcoLens/issues). Pull requests are appreciated — please open an issue first to discuss significant changes.

---

## Contact

**NguyenDevs** · [tainguyen.devs@gmail.com](mailto:tainguyen.devs@gmail.com) · `com.nguyendevs.ecolens`
