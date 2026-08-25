# Moondicine 🌙💊

**Medical Residency Exam Practice App**

An Android application for medical students preparing for residency entrance exams (ENAM, ANCAR, VINCI, Revalida, etc.). Upload exam PDFs, and let AI parse, classify, and store questions for interactive study with intelligent explanations.

## Features

- 📄 **PDF Upload** — Import exams with questions and answer keys (single or dual PDF mode)
- 🤖 **AI-Powered Parsing** — Cohere AI extracts and classifies questions by medical specialty
- 📚 **Interactive Quizzes** — Quick, specialty-focused, weak areas, and exam simulation modes
- 💡 **AI Explanations** — Detailed reasoning for correct and incorrect answers
- 📊 **Performance Analytics** — Track accuracy by specialty, progress over time
- 🚩 **Flag & Review** — Mark questions for later review
- 📝 **Notes** — Add personal notes to any question
- 🎯 **Weak Areas Focus** — Automatically identifies topics you struggle with
- 🌙 **GitHub-Based Updates** — Check for updates directly from GitHub releases
- 📚 **Exam Browser** — Browse and select specific exams (Uninove, ENAMED 2026, Revalida 2026, etc.)
- 🎯 **Specialty Browser** — Select specific medical specialties to study
- 📱 **Offline-First** — Works completely offline after initial sync
- 🔄 **Auto-Sync** — Sync question bank from Supabase on app start

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Database | Room |
| DI | Hilt |
| PDF Extraction | Apache PDFBox for Android |
| AI | Cohere API (command-r-plus) |
| Networking | Retrofit + OkHttp |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Project Structure

```
moondicine/
├── app/
│   ├── src/main/
│   │   ├── java/com/moondicine/app/
│   │   │   ├── data/
│   │   │   │   ├── database/          # Room database
│   │   │   │   ├── repository/        # Data repositories
│   │   │   │   ├── remote/            # Supabase/Network
│   │   │   │   └── update/            # Update checking
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── home/          # Home screen
│   │   │   │   │   ├── exams/         # Exam browser
│   │   │   │   │   ├── specialties/   # Specialty browser
│   │   │   │   │   ├── quiz/          # Quiz screens
│   │   │   │   │   ├── review/        # Review screen
│   │   │   │   │   ├── stats/         # Statistics
│   │   │   │   │   ├── upload/        # PDF upload
│   │   │   │   │   ├── onboarding/    # Onboarding
│   │   │   │   │   └── settings/      # Settings + updates
│   │   │   │   └── navigation/        # Navigation
│   │   │   ├── ai/                    # Cohere AI integration
│   │   │   └── di/                    # Hilt modules
│   │   └── res/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── .github/workflows/
│   └── build.yml                      # CI/CD workflow
├── supabase/
│   └── migrations/                    # Database schema
├── gradle/
├── gradlew.bat
├── gradlew
├── settings.gradle.kts
├── build.gradle.kts
└── README.md
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Cohere API key

### Setup

1. Clone the repository:
```bash
git clone https://github.com/igor/moondicine.git
cd moondicine
```

2. Add your Cohere API key to `local.properties`:
```properties
COHERE_API_KEY=your_api_key_here
```

3. Open in Android Studio and sync Gradle

4. Run on device/emulator

### Building APK

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

## GitHub Actions CI/CD

The project includes a GitHub Actions workflow (`.github/workflows/build.yml`) that:

1. Builds debug and release APKs on every push
2. Creates GitHub Releases when tags are pushed (e.g., `v1.0.0`)
3. Attaches APKs to releases automatically

### Creating a Release

```bash
# Create and push a tag
git tag v1.0.1
git push origin v1.0.1
```

This triggers the workflow to build and create a GitHub Release with APKs attached.

## In-App Updates

The app checks for updates from GitHub releases:

- **Automatic check**: On app start and when opening Settings
- **Manual check**: Settings → Atualizações → "Verificar atualizações"
- **Update dialog**: Shows version info, release notes, and link to GitHub release
- **User-controlled**: Users choose when to update

### Update Flow

1. App queries GitHub API for latest release
2. Compares version with current app version
3. If newer version exists, shows update dialog/banner
4. User can view release notes and open GitHub release page
5. User downloads and installs APK manually

### Update Flow

1. App queries GitHub API for latest release
2. Compares version with current app version
3. If newer version exists, shows update dialog/banner
4. User can view release notes and open GitHub release page
5. User downloads and installs APK manually

## Configuration

### Cohere API Key

Add to `local.properties`:
```properties
COHERE_API_KEY=your_cohere_api_key
```

### GitHub Repository

Update `UpdateRepository.kt` with your GitHub repository:
```kotlin
private val githubApiUrl = "https://api.github.com/repos/YOUR_USERNAME/moondicine/releases/latest"
```

### GitHub Actions

The workflow uses `GITHUB_TOKEN` automatically. No additional secrets needed for basic builds.

For release signing, add these secrets:
- `SIGNING_KEY`: Base64 encoded keystore
- `SIGNING_KEY_ALIAS`: Key alias
- `SIGNING_KEY_PASSWORD`: Key password
- `SIGNING_STORE_PASSWORD`: Keystore password

## Medical Specialties Covered

The app classifies questions into these 5 major medical specialties:
- Clínica Médica (Internal Medicine)
- Cirurgia Geral (General Surgery)
- Pediatria (Pediatrics)
- Ginecologia e Obstetrícia (OB/GYN)
- Medicina Preventiva (Preventive Medicine)

## License

MIT License - see LICENSE file for details.

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## Support

For issues and feature requests, please use GitHub Issues.
