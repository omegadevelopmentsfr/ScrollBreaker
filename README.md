# ScrollBreak

ScrollBreak is an Android application designed to mitigate compulsive mobile usage by substituting social media "doomscrolling" with productive learning intervals. 

The application monitors device usage and intervenes when pre-defined screen time limits are exceeded on distracting platforms, presenting users with educational content from Wikipedia or Dev.to to foster mindful digital habits.

## Key Features

- **Usage Monitoring**: Automatically detects excessive session duration on a configurable list of social media and entertainment applications.
- **Educational Integration**: Delivers curated article summaries from Wikipedia, Wikinews, Wikivoyage, Wikibooks, and Dev.to.
- **Thematic Customization**: Allows users to specify preferred knowledge domains, such as Science, Technology, History, and Art.
- **Intervention Mechanisms**: Supports multiple interruption styles, including non-intrusive overlays and system notifications.
- **Accessibility Features**: Includes integrated Text-to-Speech (TTS) functionality for eyes-free content consumption.
- **Content Management**: Features a bookmarking system for saving significant articles for future reference.
- **Analytics Dashboard**: Tracks performance metrics, including articles consumed and estimated time diverted from unproductive scrolling.

## Getting Started

### Prerequisites

- Android Studio Koala or newer
- Android device or emulator running API level 26 (Android 8.0) or higher

### Build and Setup

1. **Repository Acquisition**:
   Clone the repository using the following command:
   ```bash
   git clone https://github.com/yourusername/scrollbreak.git
   ```

2. **Project Import**:
   Launch Android Studio, select **File > Open**, and navigate to the `scrollbreak` directory.

3. **Permission Configuration**:
   To function effectively, the application requires the following system permissions:
   - **Usage Access**: Necessary for monitoring active application session times.
   - **Display over other apps**: Required for the overlay intervention feature.
   The application will facilitate the configuration of these permissions during the initial launch.

4. **Deployment**:
   Execute the build process and run the application on your target device using the standard Android Studio run configuration.

 ### Or install the app

   You can find the .apk file in the release section.

## Technical Architecture

The application is built using modern Android development standards:

- **UI Framework**: Jetpack Compose using Material 3 design principles.
- **Concurrency**: Kotlin Coroutines and Flow for reactive data streams.
- **Persistence**: Room Database for structured data (bookmarks) and DataStore for user preferences.
- **Networking**: Retrofit and Moshi for interface with the Wikipedia REST API and Dev.to API.
- **Service Layer**: Background services for continuous usage monitoring and UI overlays.

## License

This project is licensed under the MIT License. Refer to the LICENSE file for further details.

---

*Focusing digital consumption on knowledge and mindfulness.*
