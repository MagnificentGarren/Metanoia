# 🧠 Metanoia

<p align="center">
  <strong>Build better habits. Master your focus. Become who you aspire to be.</strong>
  <br />
  A gamified productivity and personal development application built for Android.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-24-4285F4?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Room-2.6.1-6DB33F?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Firebase-Authentication-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
</p>

---

## 🌱 Overview

**Metanoia** is a personal productivity and self-improvement application designed to turn ambitious goals into consistent daily progress.

The philosophy behind Metanoia is simple: meaningful personal growth doesn't happen overnight. It happens through small, deliberate actions repeated consistently.

Rather than functioning as just another task manager, Metanoia combines structured project management, focused work sessions, gamification, achievements and personal progress tracking into one cohesive experience.

Whether you're working towards a long-term goal, building better habits or simply trying to become more disciplined, Metanoia provides the tools to help you stay accountable.

### ✨ Core Philosophy

- **Focus:** Eliminate distractions and dedicate time to meaningful work.
- **Consistency:** Turn individual productive sessions into sustainable routines.
- **Progress:** Break larger ambitions into manageable projects and sessions.
- **Achievement:** Make personal development rewarding through milestones and gamification.

---

## 🚀 Features

### ⏱️ Focus & Productivity

- Dedicated focus timer with background execution support.
- Customizable timer sessions.
- Ambient audio playback with relaxing lo-fi music.
- Session tracking to monitor time invested in productive activities.
- Background timer functionality through Android foreground services.

### 📂 Project & Session Management

- Create and manage personal projects.
- Organize productivity sessions around individual goals.
- Track completed work and historical sessions.
- Maintain a structured overview of ongoing and completed projects.

### 🏆 Gamification & Achievements

- Unlockable achievement system with milestone-based progression.
- Custom-designed medals and achievement rewards.
- Collectible avatar system featuring characters such as:
  - 🚀 Astronaut
  - ⚔️ Knight
  - 🏴‍☠️ Pirate
  - 🔬 Scientist
  - 🧙 Wizard
  - 👑 King
- Reward consistency and celebrate personal milestones.

### 🔐 Authentication & Account Management

- Firebase Authentication integration.
- Google Sign-In through Android Credential Manager.
- Account management and user data handling.
- Authentication-aware application experience.

### 💾 Data Persistence & Synchronization

- Local-first architecture powered by Room Database.
- Persistent storage of projects, sessions and application data.
- Dedicated backup and synchronization managers.
- Kotlin Coroutines for asynchronous database operations.

### 🔔 Reminders & Notifications

- Scheduled productivity reminders.
- Background notification handling.
- Notification receivers for time-based accountability.

---

## 🛠️ Technology Stack

Metanoia is developed using native Android technologies, with an emphasis on maintainability, asynchronous operations and reliable local data persistence.

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Android SDK | Native Android development |
| MVVM | Application architecture |
| Room 2.6.1 | Local database and persistence |
| SQLite | Underlying relational storage |
| Kotlin Coroutines | Asynchronous programming |
| Kotlin Flow | Reactive data streams |
| Firebase Authentication | User authentication |
| Android Credential Manager | Google Sign-In integration |
| Material Components | UI components and styling |
| Gradle | Build automation |

### 🏗️ Architecture

The application follows the **Model-View-ViewModel (MVVM)** architectural pattern.

This separation helps organize application responsibilities, keeping interface logic, application state and data management distinct.

**Architectural responsibilities:**

- **Model:** Represents application data and database entities.
- **View:** Activities and Fragments responsible for rendering the user interface.
- **ViewModel:** Manages UI-related state and application logic.
- **Data Layer:** Room Database, DAOs and data management components.
- **Background Services:** Handles long-running operations such as focus timers and notifications.

---

## 📁 Project Structure

The following is a simplified overview of the application's main components.

```text
Metanoia/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/myapplicationtoday/
│           │       │
│           │       ├── AccountDataManager.kt
│           │       ├── AchievementsEngine.kt
│           │       ├── AppDatabase.kt
│           │       ├── AuthActivity.kt
│           │       ├── BackupManager.kt
│           │       │
│           │       ├── DashboardFragment.kt
│           │       ├── ProjectsFragment.kt
│           │       ├── SessionsFragment.kt
│           │       ├── ProfileFragment.kt
│           │       ├── AchievementsFragment.kt
│           │       │
│           │       ├── Session.kt
│           │       ├── TimerService.kt
│           │       │
│           │       └── ui/
│           │           └── Custom UI components
│           │
│           ├── res/
│           │   ├── drawable/
│           │   ├── layout/
│           │   ├── mipmap/
│           │   └── values/
│           │
│           └── AndroidManifest.xml
│
├── build.gradle
├── settings.gradle
├── gradle/
└── README.md
```

### 🔍 Important Components

| Component | Responsibility |
|---|---|
| `AccountDataManager.kt` | Account-related data management |
| `AchievementsEngine.kt` | Achievement evaluation and gamification logic |
| `AppDatabase.kt` | Room database configuration |
| `AuthActivity.kt` | Authentication and sign-in workflows |
| `BackupManager.kt` | Application data backup management |
| `DashboardFragment.kt` | Main productivity dashboard |
| `ProjectsFragment.kt` | Project creation and management |
| `SessionsFragment.kt` | Focus sessions and timer interface |
| `Session.kt` | Session data model |
| `TimerService.kt` | Background focus timer service |

---

## ⚙️ Getting Started

Follow these instructions to set up Metanoia locally.

### Prerequisites

Ensure you have the following installed:

- [Android Studio](https://developer.android.com/studio)
- Android SDK with Compile SDK 37
- Android SDK Platform 24 or higher
- Gradle 8.x or the version specified by the project wrapper
- A physical Android device or Android Emulator

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/your-username/metanoia.git
```

**2. Open the project**

Open the cloned repository in Android Studio.

**3. Configure Firebase**

To enable authentication functionality:

1. Create a Firebase project through the [Firebase Console](https://console.firebase.google.com/).
2. Register your Android application using its application ID.
3. Enable the required authentication providers.
4. Download the `google-services.json` configuration file.
5. Place the file inside the `app/` directory.

```text
Metanoia/
└── app/
    └── google-services.json
```

**4. Synchronize Gradle**

Allow Android Studio to download the required dependencies and synchronize the project.

**5. Run the application**

Select an Android Emulator or connect a physical Android device and run the application.

> **Note:** Firebase authentication requires valid Firebase configuration. Some functionality may be unavailable until the project is configured correctly.

---

## 🔒 Security & Configuration

Metanoia integrates Firebase Authentication and local database persistence.

When configuring the application:

- Keep Firebase configuration and project credentials out of public repositories where applicable.
- Never commit private signing keys or sensitive service credentials.
- Configure Firebase authentication providers before testing sign-in functionality.
- Validate user input before persisting application data.
- Ensure backup and synchronization operations handle failures appropriately.

---

## 🗺️ Future Development

Potential areas for further development include:

- [ ] Advanced productivity analytics and visual statistics.
- [ ] Habit streak tracking and consistency scoring.
- [ ] Expanded achievement and collectible systems.
- [ ] Cloud-based data synchronization across devices.
- [ ] Customizable productivity goals.
- [ ] Enhanced notification scheduling.
- [ ] Additional ambient sound environments.
- [ ] Improved historical productivity insights.

---

## 🤝 Contributing

Contributions, suggestions and constructive feedback are welcome.

If you would like to contribute:

1. Fork the repository.
2. Create a feature branch.
3. Implement your changes.
4. Test your implementation.
5. Submit a pull request describing your contribution.

---

## 📄 License

This project is distributed under the MIT License.

See the [LICENSE](LICENSE) file for additional information.

---

<p align="center">
  <strong>Metanoia — Progress is built one session at a time. 🧠</strong>
  <br />
  <sub>Stop waiting for motivation. Start building momentum.</sub>
</p>
