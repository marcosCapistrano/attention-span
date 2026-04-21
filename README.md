# AttentionSpan

AttentionSpan is a modern Android application designed to help users manage their study time and improve focus. Built with Jetpack Compose, it provides a clean and intuitive interface for tracking study sessions across different subjects.

## Features

- **Subject Management**: Organize your studies by creating and managing different subjects.
- **Study Timer**: A dedicated chronometer to track your focus sessions in real-time.
- **Foreground Service**: Keep your timer running even when the app is in the background with a persistent notification.
- **Statistics & Insights**: Visualize your study habits with detailed charts and stats (powered by Vico).
- **Home Screen Widget**: Track your current progress directly from your home screen using Jetpack Glance.
- **Persistent Storage**: All your subjects and study sessions are safely stored locally using Room Database.
- **Settings**: Customize your experience with user preferences stored via DataStore.

## Tech Stack

- **UI**: Jetpack Compose
- **Architecture**: MVVM (ViewModel, StateFlow)
- **Database**: Room
- **Navigation**: Compose Navigation
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Background Work**: Android Services (Foreground Service)
- **Widgets**: Jetpack Glance
- **Charts**: Vico
- **Preferences**: DataStore

## Getting Started

### Prerequisites

- Android Studio Ladybug | 2024.2.1 or newer
- Android SDK 24+ (Android 7.0)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AttentionSpan.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on an emulator or a physical device.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
