# Alkaid — Product Requirements Document

## Overview

Alkaid is an Android sensor dashboard app built with Kotlin, MVVM architecture, and Material Design 3. It provides real-time sensor readings (GPS, barometer, gyroscope, temperature, accelerometer, magnetometer, light, humidity), a dual-provider map, weather data, and a constellation sky map.

This PRD covers 10 features organized into three phases: architectural foundations, infrastructure improvements, and major feature additions.

---

## Phase 1: Foundations

### F1. HomeFragment / ViewModel Refactor

**Problem**: `HomeFragment` owns 8 repository instances, manages the adapter, handles preferences, and observes GPS data. `HomeViewModel` contains only a placeholder string. This violates MVVM and prevents unit testing of home screen logic.

**Requirements**:
- Move all repository management, sensor data observation, and preference handling from `HomeFragment` into `HomeViewModel`
- Expose a single `StateFlow<List<SensorUiItem>>` combining sensor type, visibility, and latest reading
- `HomeFragment` becomes a pure view layer — observes state, binds to adapter, handles navigation
- Sensor repositories survive configuration changes (no recreation on rotation)

**Acceptance Criteria**:
- [ ] `HomeFragment` contains zero repository references
- [ ] `HomeViewModel` owns all sensor repositories and exposes combined UI state
- [ ] Existing behavior is unchanged from the user's perspective
- [ ] Unit tests cover `HomeViewModel` state emission

---

### F2. Sensor-Specific Icons and Visual Polish

**Problem**: 5 of 8 sensor cards use `ic_home_black_24dp`. Settings toggles also use generic icons. Cards are visually indistinguishable.

**Requirements**:
- Add dedicated Material icons for each sensor type (barometer, thermometer, gyroscope, accelerometer, magnetometer, light bulb, humidity drop, GPS pin)
- Update `SensorCardView.setupSensor()` icon mapping
- Update Settings toggle icons to match
- Add value transition animations in `SensorCardView.showData()`

**Acceptance Criteria**:
- [ ] Each sensor card displays a unique, semantically correct icon
- [ ] Settings screen shows matching icons next to each sensor toggle
- [ ] Value updates animate smoothly instead of snapping

---

## Phase 2: Infrastructure

### F3. Dependency Injection with Hilt

**Problem**: Repositories are manually constructed in Fragments/ViewModels. Four separate `GpsRepository` instances independently request location updates, wasting battery.

**Requirements**:
- Add Hilt as the DI framework
- Define `@Singleton`-scoped modules for all sensor repositories and `FusedLocationProviderClient`
- Annotate ViewModels with `@HiltViewModel` and inject dependencies via constructor
- Remove all manual repository construction from Fragments and ViewModels

**Acceptance Criteria**:
- [ ] Single shared `GpsRepository` instance across the app
- [ ] All ViewModels use constructor injection
- [ ] No repository instantiation in Fragment code
- [ ] Existing tests pass (update with Hilt test modules where needed)

---

### F4. Digital Compass

**Problem**: Accelerometer and magnetometer repositories exist but display only raw numeric values with limited practical use.

**Requirements**:
- Create `CompassRepository` that combines accelerometer and magnetometer flows using `SensorManager.getRotationMatrix()` and `SensorManager.getOrientation()`
- Emit heading (degrees), cardinal direction (N/NE/E/SE/S/SW/W/NW), and magnetic declination
- Build a custom `CompassView` with an animated needle
- Display as a prominent widget on the home screen (similar to `GpsLocationWidget`)

**Acceptance Criteria**:
- [ ] Compass heading updates in real time
- [ ] Needle animates smoothly on rotation
- [ ] Cardinal direction label shown alongside degrees
- [ ] Compass toggleable via sensor visibility settings

---

### F5. Sensor Update Rate Configuration

**Problem**: All sensors hardcode `SENSOR_DELAY_NORMAL`. GPS uses a fixed 10-second interval. Users cannot trade battery life for precision or vice versa.

**Requirements**:
- Create `SensorUpdateRatePreferences` (following the `SensorVisibilityPreferences` pattern)
- Support four modes per sensor: Normal, UI, Game, Fastest
- Support configurable GPS interval (5s / 10s / 30s / 60s)
- Add a global "Power Saving" toggle that sets all sensors to lowest rate
- Add rate selection UI in Settings under each sensor

**Acceptance Criteria**:
- [ ] Changing rate takes effect immediately (re-registers sensor listener)
- [ ] GPS interval change triggers a new `LocationRequest`
- [ ] Power Saving mode overrides individual settings
- [ ] Preference persists across app restarts

---

## Phase 3: Features

### F6. Weather Forecast

**Problem**: Weather tab shows only current conditions. The free OpenWeatherMap tier includes a 5-day/3-hour forecast endpoint that is unused.

**Requirements**:
- Add `@GET("forecast")` endpoint to `WeatherApiService`
- Define forecast data models (hourly items with temp, icon, description, wind, precipitation probability)
- Display hourly forecast as a horizontal scrollable list with temperature and weather icons
- Display 5-day daily summary (high/low temp, dominant condition) as a vertical list below current weather
- Load weather icons using Coil (resolves existing TODO in `WeatherFragment`)

**Acceptance Criteria**:
- [ ] Hourly forecast scrollable for next 24 hours
- [ ] 5-day daily forecast visible below hourly
- [ ] Weather condition icons load from OpenWeatherMap CDN
- [ ] Forecast refreshes alongside current weather

---

### F7. Sensor Data History and Charts

**Problem**: All sensor readings are ephemeral. Previous values are lost on each update.

**Requirements**:
- Add Room database with a `SensorReading` entity (sensorType, value, unit, timestamp)
- Background recording: persist readings at a configurable interval when enabled
- Tapping a sensor card opens a detail screen with an interactive time-series chart (MPAndroidChart or Vico)
- Support time range selection: 1h, 6h, 24h, 7d
- Export data as CSV via Android ShareSheet

**Acceptance Criteria**:
- [ ] Sensor readings persist to Room database
- [ ] Chart renders historical data with zoom/pan
- [ ] Time range selector filters chart data
- [ ] CSV export produces valid file and opens share dialog
- [ ] Auto-cleanup: readings older than 30 days are pruned

---

### F8. Expanded Constellation Sky Map

**Problem**: Only Orion is implemented. The Julian date calculation has a bug. No user interaction. The app is named after a star but barely features astronomy.

**Requirements**:
- Populate a star catalog covering all 88 IAU constellations (JSON asset or embedded database)
- Render stars with size proportional to apparent magnitude
- Fix Julian date calculation bug at `ConstellationMapView.kt:132-134`
- Add touch gestures: pinch-to-zoom, drag-to-pan
- Add tap-to-select: tapping a constellation shows its name, star names, and mythology snippet
- Add time scrubber: slider to view the sky at a different date/time
- Highlight Alkaid (Eta Ursae Majoris) with a special indicator

**Acceptance Criteria**:
- [ ] All 88 constellations render with correct star positions
- [ ] Star sizes vary by magnitude
- [ ] Pinch-zoom works smoothly (min 1x to max 5x)
- [ ] Tapping a constellation shows an info card
- [ ] Time slider updates the sky projection in real time
- [ ] Julian date calculation is correct across all months

---

### F9. Offline Map Tile Caching

**Problem**: The OSM map requires an internet connection. Users in outdoor/remote areas lose map functionality.

**Requirements**:
- Enable osmdroid's built-in `SqlTileWriter` for automatic tile caching
- Add a "Download Region" feature: user selects a bounding box on the map, app downloads tiles for zoom levels 10–16
- Show download progress and estimated storage size
- Add cache management in Settings: view cache size, clear cache
- Google Maps provider is out of scope (has its own offline mechanism)

**Acceptance Criteria**:
- [ ] Previously viewed OSM tiles load without network
- [ ] Region download captures tiles for the selected area
- [ ] Download progress shown with cancel option
- [ ] Cache size visible in Settings with a clear button

---

### F10. GPS Track Recording and GPX Export

**Problem**: The map shows current location but cannot record movement over time. No activity tracking capability.

**Requirements**:
- Start/stop recording controls on the map screen (floating action button)
- Foreground service for background recording with persistent notification
- Persist track points (timestamp, lat, lng, altitude, accuracy, speed) to Room database
- Render recorded track as a polyline on both Google Maps and OSM
- Compute and display: total distance, duration, average speed, elevation gain/loss
- Export track as GPX file via ShareSheet
- Track list screen to view and manage saved tracks

**Acceptance Criteria**:
- [ ] Recording continues when app is backgrounded
- [ ] Polyline renders on whichever map provider is active
- [ ] Stats (distance, duration, speed, elevation) update live during recording
- [ ] GPX export produces a valid file that opens in other mapping apps
- [ ] Saved tracks can be renamed, viewed, and deleted

---

## Implementation Order

```
Phase 1 (Foundations)          Phase 2 (Infrastructure)       Phase 3 (Features)
──────────────────────         ────────────────────────        ──────────────────
F1. ViewModel Refactor    →    F3. Hilt DI              →     F6. Weather Forecast
F2. Sensor Icons          →    F4. Digital Compass       →     F7. Data History/Charts
                               F5. Update Rate Config    →     F8. Constellation Sky Map
                                                               F9. Offline Map Caching
                                                               F10. GPS Track Recording
```

Phase 1 has no dependencies and should be completed first. Within Phase 2, F3 (Hilt) should precede F4 and F5 since they benefit from DI. Phase 3 features are independent of each other and can be built in any order.

---

## Out of Scope

- iOS or cross-platform support
- User accounts or cloud sync
- Social features or sharing beyond file export
- Monetization or in-app purchases
- Google Maps offline (handled by Google's own SDK)
- Wear OS companion app
