# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Alkaid is an Android sensor dashboard application written in Kotlin that features a prominent GPS location widget alongside support for multiple sensors (barometer, gyroscope, temperature). The app follows MVVM architecture with a repository pattern and uses modern Android development practices.

## Build and Development Commands

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Clean build artifacts
./gradlew clean

# Full build with all checks
./gradlew build
```

### Testing Commands
```bash
# Run all unit tests
./gradlew test

# Run debug unit tests specifically
./gradlew testDebugUnitTest

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Run all checks (lint + tests)
./gradlew check
```

### Lint and Code Quality
```bash
# Run lint checks
./gradlew lint

# Run lint and apply safe fixes automatically
./gradlew lintFix

# Update lint baseline
./gradlew updateLintBaseline
```

### Install and Run
```bash
# Install debug APK on connected device
./gradlew installDebug

# Uninstall debug APK
./gradlew uninstallDebug
```

## Architecture and Code Structure

### Core Architecture Pattern
- **MVVM (Model-View-ViewModel)**: Clean separation between UI, business logic, and data
- **Repository Pattern**: Centralized data management with `BaseSensorRepository<T>` interface
- **Reactive Programming**: Uses Kotlin Flows and Coroutines for asynchronous data streams
- **Unified State Management**: `SensorResult` sealed interface handles Loading/Data/Error states across all sensors

### Key Components

#### Navigation Structure
- Bottom navigation with three main sections: Home (sensors), Map, Settings
- Navigation configuration in `MainActivity.kt:27-31`

#### Repository Layer (`app/src/main/java/com/example/alkaid/data/repository/`)
- Each sensor has its own repository implementing `BaseSensorRepository<T>`
- Common interface: `getSensorData()`, `startListening()`, `stopListening()`
- Examples: `GpsRepository`, `BarometerRepository`, `GyroscopeRepository`

#### Data Models (`app/src/main/java/com/example/alkaid/data/sensor/`)
- `SensorResult`: Sealed interface for unified state management (Loading/Data/Error)
- `SensorData`: Core sensor data models

#### UI Layer (`app/src/main/java/com/example/alkaid/ui/`)
- `components/`: Custom views including `GpsLocationWidget` 
- `home/`: Main sensor dashboard with `HomeFragment`, `HomeViewModel`, `SensorAdapter`
- `map/`: Map functionality with Google Maps and OpenStreetMap support
- `settings/`: Configuration screen for sensor visibility and preferences

### Dependencies and Libraries
- **Google Play Services**: Location services (`play-services-location`, `play-services-maps`)
- **OpenStreetMap**: osmdroid for alternative map provider
- **Android Architecture Components**: ViewModel, LiveData, Navigation
- **Material Design 3**: UI components and theming
- **ViewBinding**: Type-safe view access
- **Testing**: JUnit, MockK, Turbine, Robolectric, Espresso

### Preferences and Configuration
- Sensor visibility managed via `SensorVisibilityPreferences` and `MapPreferences`
- Settings expose configuration for sensor enable/disable and map provider selection

### Key Development Patterns
- Use ViewBinding for all view access
- Implement repositories using Flow-based reactive patterns
- Follow the existing `SensorResult` pattern for state management
- Test repositories using MockK and Turbine for Flow testing
- Use coroutines for asynchronous operations
- Material Design 3 theming and components throughout

### Testing Approach
- Unit tests with JUnit and MockK
- Flow testing with Turbine library
- Robolectric for Android framework testing
- Instrumentation tests with Espresso
- Test utilities in `TestCoroutineRule.kt`