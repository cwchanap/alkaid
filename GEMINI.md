# Gemini Project Context: Alkaid - Android Sensor Dashboard

## Project Overview

Alkaid is a modern Android sensor dashboard application built with Kotlin. Its primary feature is a prominent GPS location widget that displays real-time altitude, latitude, and longitude. The app also supports other sensors like the barometer, gyroscope, and thermometer.

The project follows the MVVM (Model-View-ViewModel) architecture pattern, utilizing a repository pattern for centralized data management. It leverages modern Android development technologies, including:

*   **Kotlin:** The primary programming language.
*   **Coroutines and Flows:** For asynchronous and reactive data streams.
*   **Material Design 3:** For modern UI components and theming.
*   **Android Jetpack:** A suite of libraries for robust architecture, including ViewModel, LiveData, and Navigation.
*   **Google Play Services:** For location services (FusedLocationProviderClient).

## Building and Running

### Prerequisites

*   Android Studio Arctic Fox or newer
*   Android SDK 24+ (Android 7.0)
*   A device or emulator with sensor support (GPS, Barometer, etc.)

### Build and Run Commands

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/cwchanap/alkaid.git
    cd alkaid
    ```

2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open an existing project."
    *   Navigate to the cloned directory.

3.  **Build and Run:**
    *   The project can be built and run using the standard Android Studio "Run" button (Shift + F10) or via the command line using Gradle:
    ```bash
    # Build the debug APK
    ./gradlew assembleDebug

    # Install the debug APK on a connected device
    ./gradlew installDebug

    # Run the app
    # (Best launched from Android Studio or the device itself)
    ```

4.  **Run Tests:**
    ```bash
    # Run unit tests
    ./gradlew testDebugUnitTest

    # Run instrumentation tests
    ./gradlew connectedDebugAndroidTest
    ```

## Development Conventions

*   **MVVM Architecture:** Code is separated into UI (Views/Fragments), ViewModel, and Repository layers.
*   **Repository Pattern:** Each sensor has its own repository for data handling.
*   **Kotlin Coroutines and Flows:** Asynchronous operations and data streams are managed using coroutines and StateFlow/SharedFlow.
*   **Dependency Management:** Dependencies are managed using the `libs.versions.toml` file for centralized version control.
*   **ViewBinding:** Used to access views in a type-safe manner.
*   **Testing:** Unit tests are written with JUnit and MockK. Instrumentation tests use Espresso.
*   **Permissions:** The app requests necessary permissions for location and high-sampling-rate sensors.
*   **UI:** The UI is built with Material Design 3 components, including a custom GPS widget.
