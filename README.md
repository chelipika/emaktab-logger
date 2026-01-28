# 📱 Emaktab Login Logger (Android)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

A powerful, multi-threaded Android utility tool designed to automate bulk authorization checks for the **Emaktab** platform. Originally ported from Python, this app features a modern **Jetpack Compose** UI with a "Hacker/Terminal" aesthetic, real-time logging, and Excel file integration.

## ✨ Features

*   **🚀 High Performance**: Rewritten from Python to Kotlin using **Coroutines** & **Flow** for non-blocking I/O.
*   **📊 Excel Integration**: Upload `.xls` or `.xlsx` files directly to import hundreds of credentials at once.
*   **🔐 Deep Verification**:
    1.  Authenticates User.
    2.  Retrieves Context (Group/Person ID).
    3.  Verifies Diary/Journal Access (detects `systemForbidden`).
*   **💻 Terminal UI**: Real-time scrolling logcat-style interface with color-coded feedback.
*   **🌍 Multi-Language Support**: Auto-detects system language:
    *   🇺🇸 English
    *   🇷🇺 Russian (Русский)
    *   🇺🇿 Uzbek (O'zbekcha)
*   **🔄 Self-Updater**: Built-in OTA mechanism to check for, download, and install app updates.

## 📸 Screenshots

| Upload Screen | Real-time Logs |
|:---:|:---:|
YOUTUBE VIDEO SPACE

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose (Material3)
*   **Networking**: OkHttp 4 + Gson (REST API)
*   **Concurrency**: Coroutines + Kotlin Flow
*   **File Parsing**: Apache POI (Excel .xls/.xlsx support)
*   **Architecture**: MVVM-like pattern with State Hoisting.

## 📂 Excel File Format

To use the logger, prepare an Excel file with the following structure:

*   **Format**: `.xls` or `.xlsx`
*   **Columns**:
    *   **Column A**: Username (Login)
    *   **Column B**: Password

| | A | B |
|:---:|:---|:---|
| **1** | student_login_01 | password123 |
| **2** | teacher_test | securePass! |
| **3** | admin_user | 111111 |

> **Note**: The app automatically trims whitespace from cells.

## 🚀 Installation & Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/YourUsername/Emaktab-Logger.git
    ```
2.  **Open in Android Studio**:
    *   File -> Open -> Select the project folder.
3.  **Sync Gradle**:
    *   Ensure all dependencies (Apache POI, OkHttp) are downloaded.
4.  **Build & Run**:
    *   Connect your Android device or use an Emulator.
    *   Click **Run**.

## ⚠️ Permissions

The app requires the following permissions (handled in `AndroidManifest.xml`):
*   `INTERNET`: To communicate with the API.
*   `REQUEST_INSTALL_PACKAGES`: To perform self-updates (Android 8.0+).

## 📝 Usage Guide

1.  **Upload**: Click **"1. Upload Excel File"** and select your credential list.
2.  **Verify**: The app will parse the file and show how many users were loaded (e.g., "✅ Loaded 50 users").
3.  **Start**: Click **"2. START LOGGER"**.
4.  **Monitor**: Watch the black terminal window for real-time progress.
    *   **Green**: Success.
    *   **White**: Processing.
    *   **Red**: Failed/Forbidden.

## ⚖️ Disclaimer

This application is for **educational purposes and authorized testing only**. The developer is not responsible for any misuse of this tool against systems for which you do not have explicit permission to access.

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

**Created by chelipika**
