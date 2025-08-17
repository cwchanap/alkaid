# Alkaid - Sensor Display App

A modern Android application that displays real-time sensor data through customizable widgets. Users can view data from various device sensors including barometer, gyroscope, temperature, and GPS location in an intuitive card-based interface.

## Features

### 🎯 Core Functionality
- **Real-time Sensor Data**: Live updates from device sensors including:
  - **Barometer**: Atmospheric pressure in hPa
  - **Gyroscope**: Rotation rates around X, Y, Z axes in rad/s
  - **Temperature**: Ambient temperature in °C
  - **GPS Location**: Latitude, longitude, altitude, and accuracy

### ⚙️ Customizable Interface
- **Widget Visibility Control**: Show/hide individual sensor widgets through settings
- **Responsive Grid Layout**: Adaptive layout that adjusts to screen orientation and size
- **Material Design**: Modern UI following Material Design principles
- **Error Handling**: Graceful handling of unavailable sensors and permission issues

### 🔒 Smart Permissions
- **Runtime Permission Requests**: Intelligent permission handling for location services
- **Permission Rationale**: Clear explanations when permissions are required
- **Graceful Degradation**: App functions properly even when some sensors are unavailable

## Architecture

### 📱 Modern Android Architecture
- **MVVM Pattern**: Clean separation between UI and business logic
- **ViewBinding**: Type-safe view references without findViewById
- **Repository Pattern**: Centralized data access through repository classes
- **Flow-based Updates**: Reactive programming with Kotlin Coroutines and Flow

### 🏗️ Project Structure
```
app/src/main/java/com/example/alkaid/
├── data/
│   ├── preferences/         # SharedPreferences management
│   ├── repository/          # Sensor data repositories
│   └── sensor/             # Data models and sensor types
├── ui/
│   ├── components/         # Reusable UI components
│   ├── home/              # Sensor display screen
│   └── settings/          # Settings management screen
└── MainActivity.kt        # Main navigation controller
```

## Sensor Implementation Details

### 🌡️ Supported Sensors
1. **Barometer (TYPE_PRESSURE)**
   - Measures atmospheric pressure
   - Useful for altitude estimation and weather monitoring
   
2. **Gyroscope (TYPE_GYROSCOPE)**
   - Measures rotation rates around device axes
   - Displays magnitude of combined rotation
   
3. **Temperature (TYPE_AMBIENT_TEMPERATURE)**
   - Measures ambient air temperature
   - Note: Not all devices have dedicated temperature sensors
   
4. **GPS Location**
   - Uses FusedLocationProviderClient for efficient location updates
   - Displays coordinates, altitude, and accuracy information

### 🔄 Data Flow
1. **Repository Layer**: Each sensor has its own repository implementing `BaseSensorRepository<T>`
2. **Flow-based Updates**: Repositories emit `SensorResult` states (Loading, Data, Error)
3. **UI Updates**: ViewHolders subscribe to repository flows using coroutines
4. **Lifecycle Awareness**: Subscriptions are properly managed to prevent memory leaks

## Technical Specifications

### 📋 Requirements
- **Minimum SDK**: Android 8.0 (API level 26)
- **Target SDK**: Android 14 (API level 36)
- **Kotlin Version**: 2.0.21
- **Gradle Version**: 8.12.0

### 📦 Key Dependencies
- **AndroidX Lifecycle**: For lifecycle-aware components
- **AndroidX Navigation**: For fragment navigation
- **Material Design Components**: For modern UI components  
- **Google Play Services Location**: For GPS functionality
- **Kotlin Coroutines**: For asynchronous programming

### 🧪 Testing
- **Unit Tests**: MockK for mocking dependencies
- **Repository Tests**: Test sensor data handling logic
- **Preferences Tests**: Verify settings persistence

## Setup and Installation

### 📥 Getting Started
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on device or emulator

### ⚠️ Important Notes
- **Real Device Testing**: Some sensors (especially temperature and barometer) may not be available on emulators
- **Location Permission**: GPS functionality requires location permissions
- **Sensor Availability**: App handles gracefully when sensors are not present on device

## Usage

### 🚀 Quick Start
1. **Launch App**: Open Alkaid from your app drawer
2. **View Sensors**: See available sensor widgets on the main screen
3. **Customize Display**: Go to Settings to show/hide specific sensors
4. **Grant Permissions**: Allow location access when prompted for GPS sensor

### 🎛️ Settings Configuration
- Navigate to the Settings tab
- Toggle switches to show/hide individual sensor widgets
- Location permission is automatically requested when enabling GPS sensor
- Changes are immediately reflected on the main sensor screen

## Contributing

This project demonstrates modern Android development practices including:
- Clean Architecture principles
- Reactive programming with Coroutines and Flow
- Material Design implementation
- Runtime permission handling
- Sensor data management
- Unit testing with MockK

Feel free to explore the codebase to learn about these implementation patterns!
