# AI ASISSTANT

**AI ASISSTANT** is a native Kotlin Android AI assistant built with **Jetpack Compose**, **Material 3**, **MVVM**, **Room**, **DataStore**, **Android Keystore AES-256-GCM encryption**, and **Gemini AI** (`Firebase AI Logic` + optional Keystore-encrypted custom API key override). It supports natural bilingual conversations in **Sinhala (සිංහල)** and **English**.

---

## Core Features

1. **AI Chat System**
   - Streaming Gemini responses (`gemini-3.8-flash` default, switchable to `gemini-3.5-flash-lite` or `gemini-4-argon`).
   - Automatic and manual **Sinhala (සිංහල)** and **English** support.
   - Markdown rendering with syntax-highlighted code blocks, copy message/code, regenerate last response, and retry failed requests.
   - Automatic conversation persistence in **Room Database** with full-text history search, individual deletion, and Clear All confirmation.

2. **AI Voice Assistant**
   - Native Android `SpeechRecognizer` with English (`en-US`), Sinhala (`si-LK`), and Auto locale selection.
   - Real-time neon purple RMS waveform visualizer and runtime `RECORD_AUDIO` permission handling.
   - Native Android `TextToSpeech` playback with configurable speech rate, pitch, and mute toggle.

3. **AI Image Generator**
   - Supports **Firebase AI Logic (`gemini-nano-banana-2.1`)** or custom **OpenAI-compatible Image Generation APIs** configured in Settings.
   - Live shimmer loading state, full image preview, save to Gallery (`Pictures/AI_ASISSTANT`), and share via `FileProvider`.
   - Transparent error and configuration messaging when billing or credentials are missing.

4. **AI Coding Assistant**
   - Supports **HTML, CSS, JavaScript, Python, Java, Kotlin, C++, PHP, and JSON**.
   - Modes: **Generate, Explain, Debug, Fix Errors, and Format**.
   - Multi-language syntax highlighter and one-tap code copy.

5. **AI PDF Reader**
   - Zero-permission Storage Access Framework (`OpenDocument`) PDF picker.
   - On-device text extraction via **PDFBox-Android** and native page rendering via **Android `PdfRenderer`** with page navigation.
   - Explicit on-screen privacy consent gate before sending extracted text to Gemini for **Summarize**, **Translate (Sinhala / English)**, or **Q&A**.

6. **AI Web Search**
   - Real HTTP retrieval via **Wikipedia Live API** (no key required), **Google Custom Search JSON API**, **Serper.dev**, or **Tavily AI**.
   - Displays clickable source cards with titles and URLs alongside grounded Gemini summaries citing retrieved sources.

7. **Security, Profile & Settings**
   - All user-entered API credentials (Gemini, Image Generation, Web Search) are encrypted locally using **AndroidKeyStore AES-256-GCM** (`KeystoreCryptoManager`) and stored in **DataStore**.
   - Customizable Obsidian Black & Neon Purple themes, glow intensity, language preference, voice tuning, and user profile/persona.

---

## Project Structure

```text
AI-ASISSTANT/
├── .github/
│   └── workflows/
│       └── android.yml            # CI workflow: JDK 17, tests, debug APK, Releases
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── ai/            # AppCheckInitializer, AiService, CodeSyntaxHighlighter
│   │   │   │   ├── data/          # Room DB, DataStore, PDFBox processor, WebSearch API
│   │   │   │   ├── security/      # AndroidKeyStore AES-256-GCM KeystoreCryptoManager
│   │   │   │   ├── ui/            # Theme, GlassComponents, MarkdownRenderer, Screens, Navigation
│   │   │   │   ├── voice/         # SpeechRecognizer & TextToSpeech manager
│   │   │   │   ├── AppContainer.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/               # Adaptive icons, custom fonts, drawables, strings, XML
│   │   │   └── AndroidManifest.xml
│   │   └── test/                  # Robolectric & unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

---

## Setup & API Key Configuration

### Option A: Inside AI Studio / Firebase AI Logic (Recommended)
- Firebase AI Logic (`Firebase.ai`) and App Check are configured out of the box via `app/google-services.json`.
- No client-side Gemini API key is required or committed to source control.

### Option B: Standalone GitHub Build / Custom API Keys (BYOK)
1. **Never commit API keys or `google-services.json` to a public repository** (`.env`, `debug.keystore`, and `app/google-services.json` are excluded via `.gitignore`).
2. Install or build the APK, then open **Settings** inside the app:
   - **Gemini API Configuration**: Toggle **Use Custom Gemini API Key**, enter your Gemini key, and tap **Encrypt & Save Key**.
   - **Image Generation API Configuration**: Choose **OpenAI-Compatible Image API**, enter endpoint URL, model ID, and API key.
   - **Web Search API Configuration**: Select **Wikipedia Live API** (no key needed) or configure **Google Custom Search** (API key + `cx`), **Serper.dev**, or **Tavily AI**.
3. *(Optional for Firebase CI builds)* Encode your `app/google-services.json` to Base64 and add it as a GitHub Repository Secret named `GOOGLE_SERVICES_JSON_BASE64`:
   ```bash
   base64 -w 0 app/google-services.json
   ```

---

## Building Locally

Requirements: **JDK 17+**, **Android SDK (Compile SDK 36, Min SDK 26)**.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Downloading the APK from GitHub Actions

1. Push this project to your GitHub repository on the `main` branch (or trigger **Run workflow** manually from the **Actions** tab).
2. Open the **Actions** tab in your GitHub repository and select the latest **Build AI ASISSTANT Android APK** run.
3. Scroll down to **Artifacts** and click **`AI-ASISSTANT-APK`** to download the compiled debug APK ZIP.
4. To publish an automated **GitHub Release** with the APK attached, push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   The APK will appear automatically under **Releases** on GitHub.
