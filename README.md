# 🛰️ Alkaid - Android Sensor Dashboard

A modern Android sensor app featuring a prominent GPS location widget with real-time tracking capabilities.

## ✨ Features

### 🎯 **Prominent GPS Display**
- **Large altitude display** prominently shown at the top
- **Precise coordinates** (latitude/longitude) with 6 decimal precision
- **Real-time updates** with accuracy indicators
- **Material Design** card layout with elevation

### 📊 **Multi-Sensor Support**
- **GPS Location** - Dedicated prominent widget + coordinate tracking
- **Barometer** - Atmospheric pressure monitoring
- **Gyroscope** - Device rotation and movement detection  
- **Temperature** - Ambient temperature sensing

### 🔧 **Smart UI Design**
- **Clean Navigation** - Sensors and Settings only (Dashboard removed)
- **Adaptive Grid** - GPS excluded from regular grid to avoid duplication
- **Configurable Sensors** - Enable/disable sensors in Settings
- **Responsive Layout** - Works in portrait and landscape modes

## 📱 Screenshots

### Main Interface
```
┌─────────────────────────────┐
│         SENSORS             │
├─────────────────────────────┤
│    🛰️ GPS LOCATION          │
│       Altitude              │
│       ***150.2m***          │ ← Large, prominent
│   37.7749°    -122.4194°    │ ← Smaller coordinates
│   Accuracy: ±5.2m           │
└─────────────────────────────┘
┌───────────┬─────────────────┐
│ 🌡️ TEMP    │ 📊 GYROSCOPE    │
│  22.5°C    │  0.025 rad/s    │
├───────────┼─────────────────┤
│ 🌪️ BARO   │                 │
│ 1013.2hPa │                 │
└───────────┴─────────────────┘
```

## 🏗️ Architecture

- **MVVM Pattern** - Clean separation of concerns
- **Repository Pattern** - Centralized data management
- **Coroutines & Flows** - Reactive, asynchronous data streams
- **Material Design 3** - Modern UI components and theming
- **Custom Views** - Specialized GPS location widget
- **Preference Management** - Sensor visibility configuration

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0)
- Device with sensors (GPS, Barometer, etc.)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/cwchanap/alkaid.git
   cd alkaid
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Choose "Open an existing project"
   - Navigate to the cloned directory

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press `Shift + F10`

### Permissions

The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - For precise GPS coordinates
- `ACCESS_COARSE_LOCATION` - For network-based location
- `HIGH_SAMPLING_RATE_SENSORS` - For enhanced sensor data

## 🔧 Configuration

### Sensor Settings
Navigate to **Settings** tab to:
- ✅ Enable/disable individual sensors
- 🔄 Refresh sensor availability
- ⚙️ Configure update intervals

### GPS Widget
The GPS location widget:
- Updates every **5-10 seconds**
- Shows **altitude prominently** in the center
- Displays **lat/lng coordinates** below
- Includes **accuracy indicators**
- Handles **error states** gracefully

## 📂 Project Structure

```
app/src/main/java/com/example/alkaid/
├── data/
│   ├── preferences/     # Sensor visibility settings
│   ├── repository/      # Data layer (GPS, Barometer, etc.)
│   └── sensor/         # Sensor data models
├── ui/
│   ├── components/     # Custom views (GPS widget)
│   ├── home/          # Main sensor dashboard
│   └── settings/      # Configuration screen
└── MainActivity.kt    # Entry point
```

## 🛠️ Technical Details

### GPS Implementation
- **FusedLocationProviderClient** for optimal battery usage
- **High accuracy mode** for precise positioning
- **Reactive updates** using Kotlin Flows
- **Permission handling** with graceful degradation

### Sensor Management
- **Repository pattern** for each sensor type
- **Unified SensorResult** sealed class for state management
- **Coroutine-based** data streaming
- **Preference-driven** sensor visibility

### UI Components
- **Custom GPS widget** with prominent altitude display
- **Material CardView** layouts with elevation
- **Adaptive grid** that excludes GPS to prevent duplication
- **Clean navigation** structure (Dashboard removed)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 🌟 Acknowledgments

- Material Design 3 for the beautiful UI components
- Android Jetpack libraries for robust architecture
- Google Play Services for location services

---

**Built with ❤️ for precise location tracking and sensor monitoring**
