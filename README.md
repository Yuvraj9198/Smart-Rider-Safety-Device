# 🏍️ Smart Rider Safety Device Solutions
### *Redefining Two-Wheeler Rider Safety with Smart IoT & Minimalist Navigation*

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Hardware](https://img.shields.io/badge/Hardware-ESP32%20%7C%20TFT%20Display%20%7C%20Sensors-E7352C?logo=espressif&logoColor=white)](https://www.espressif.com/)
[![Bluetooth](https://img.shields.io/badge/Protocol-Bluetooth%20SPP%20RFCOMM-0082FC?logo=bluetooth&logoColor=white)](https://en.wikipedia.org/wiki/Bluetooth)
[![Google Maps](https://img.shields.io/badge/API-Google%20Maps%20%26%20Places-4285F4?logo=googlemaps&logoColor=white)](https://developers.google.com/maps)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📌 Executive Summary

Road safety for two-wheeler riders is a critical global challenge. In 2022 alone, India witnessed **1.55 Lakh road fatalities**, with two-wheeler riders comprising **over 45%** of all road deaths. A primary contributing hazard is **rider distraction caused by smartphones mounted on handlebars**, compelling riders to decipher cluttered map screens, notifications, and complex UIs while operating at speed.

The **Smart Rider Safety Device** is an end-to-end IoT and mobile ecosystem engineered to transition rider safety from *reactive* (helmets that only absorb impacts post-crash) to **proactive and preventive**. By pairing an Android mobile navigation engine with a dedicated handlebar-mounted **ESP32 microcontroller with a minimalist TFT display and smart helmet HUD**, the system delivers glanceable, distraction-free navigation combined with automated crash detection and sobriety verification.

---

## 🚀 Key Hardware & System Features

### 1. 🧭 Minimalist Handlebar Screen for Navigation
- **Distraction-Free Visual Interface:** Replaces complex smartphone map screens with bold, high-contrast directional glyphs (turn arrows, roundabouts, distance-to-turn).
- **Sunlight-Readable TFT Display:** Mounted directly onto the bike handlebars for instant glanceability without diverting focus from the road.
- **Micro-Controller Driven:** Eliminates screen timeouts, notification overlays, and glare issues inherent to mobile devices.

### 2. 🪖 Smart Helmet HUD (Head-Up Display)
- **Line-of-Sight Projection:** Project turn directions, speed alerts, and critical indicators inside the rider's visor.
- **Eyes on the Road:** Keeps navigation cues within the rider's primary peripheral vision, drastically reducing reaction delay.

### 3. 🚨 Advanced Accident & Fall Detection Sensors
- **6-Axis Inertial Measurement Unit (IMU):** Integrates high-sensitivity accelerometer and gyroscope telemetry.
- **Real-Time Crash Classification:** Algorithmically detects severe impacts, sudden tilt angles, and high-G deceleration typical of bike spills.
- **Automated Emergency SOS:** Immediately dispatches emergency SOS alerts with real-time GPS coordinates to emergency contacts and emergency services during the vital "Golden Hour".

### 4. 🛑 Alcohol Detection & Pre-Ride Sobriety Verification
- **Integrated MQ-3 Gas Sensor:** Sniffs breath inside the helmet enclosure before starting a journey.
- **Impaired Riding Prevention:** Alerts the rider and logs sobriety compliance before departure, preventing drunk-riding incidents.

### 5. ⚡ Ultra-Low Latency Bluetooth SPP Bridge
- **Standard RFCOMM SPP Profile (`00001101-0000-1000-8000-00805F9B34FB`):** Seamless plug-and-play pairing between the Android app and ESP32 without requiring cloud reliance.
- **Fault-Tolerant Reconnection:** Automatically resumes directional telemetry streams upon signal recovery.

---

## 🏗️ System Architecture

```
 ┌───────────────────────────────────────────────────────────────┐
 │                   Android Application (Client)                │
 │                                                               │
 │  ┌───────────────────────┐         ┌───────────────────────┐  │
 │  │   Google Places API   │         │  Google Directions    │  │
 │  │   (Search Destination)│         │  (Polyline & Steps)   │  │
 │  └───────────┬───────────┘         └───────────┬───────────┘  │
 │              │                                 │              │
 │              ▼                                 ▼              │
 │  ┌─────────────────────────────────────────────────────────┐  │
 │  │          NavigationParser & Geo-fence Tracking          │  │
 │  │     - Step-by-step waypoint detection (< 20m radius)    │  │
 │  │     - Dynamic Route Recalculation if off-path           │  │
 │  │     - Text-to-Speech (TTS) voice announcements          │  │
 │  └─────────────────────────────┬───────────────────────────┘  │
 └────────────────────────────────┼──────────────────────────────┘
                                  │ Bluetooth Serial (SPP)
                                  ▼
 ┌───────────────────────────────────────────────────────────────┐
 │                  ESP32 Hardware Module (Bike/Helmet)          │
 │                                                               │
 │  ┌─────────────────────────┐     ┌─────────────────────────┐  │
 │  │   Minimalist TFT Screen │     │  Helmet LED HUD         │  │
 │  │  (Turn Arrows, Distance)│     │  (Line-of-Sight Display)│  │
 │  └─────────────────────────┘     └─────────────────────────┘  │
 │                                                               │
 │  ┌─────────────────────────┐     ┌─────────────────────────┐  │
 │  │    Accident Detection   │     │ Alcohol Detection MQ-3  │  │
 │  │    (6-Axis MPU6050 IMU) │     │ (Pre-Ride Sobriety Test)│  │
 │  └─────────────────────────┘     └─────────────────────────┘  │
 └───────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Hardware Bill of Materials (BOM) & Wiring

| Component | Specification / Model | Interface | Function |
| :--- | :--- | :--- | :--- |
| **Microcontroller** | ESP32-WROOM-32 (Dual Core 240MHz, BLE & BT Classic) | UART / SPI / I2C | Central processing unit & Bluetooth communication hub |
| **Display** | 1.8" or 2.4" SPI TFT (ST7735 / ILI9341) | SPI (MOSI, CLK, CS, DC, RST) | Minimalist navigation turn arrows and distance indicator |
| **HUD Optics** | Optical combiner lens + High-lumen LED projection | Digital GPIO | Head-Up Display projection into helmet visor |
| **Crash Sensor** | MPU-6050 6-Axis Accelerometer & Gyroscope | I2C (SDA = GPIO 21, SCL = GPIO 22) | Tilt angle, sudden deceleration, and rollover detection |
| **Alcohol Sensor** | MQ-3 Alcohol Gas Sensor Module | Analog (ADC GPIO 34) / Digital (GPIO 35) | Breath alcohol level measurement prior to ignition |
| **Buzzer / Audio** | 5V Active Piezo Buzzer | Digital GPIO | Audio turn alerts and emergency collision warnings |
| **Power Supply** | 3.7V 18650 Li-ion battery + TP4056 + 5V Step-Up Boost | VCC / GND | Portable, rechargeable power system for bike/helmet mount |

---

## 💻 ESP32 Firmware Reference Snippet

The ESP32 runs a Bluetooth Serial listener that parses navigation tokens dispatched by the Android app:

```cpp
#include "BluetoothSerial.h"
#include <TFT_eSPI.h> // Hardware SPI TFT library

BluetoothSerial SerialBT;
TFT_eSPI tft = TFT_eSPI();

void setup() {
  Serial.begin(115200);
  tft.init();
  tft.setRotation(1);
  tft.fillScreen(TFT_BLACK);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.drawString("SMART RIDER READY", 20, 40, 4);

  // Initialize Bluetooth Classic SPP
  SerialBT.begin("SmartRider-ESP32");
  Serial.println("Bluetooth device active. Pair with Android App.");
}

void loop() {
  if (SerialBT.available()) {
    String instruction = SerialBT.readStringUntil('\n');
    instruction.trim();
    
    tft.fillScreen(TFT_BLACK);
    
    if (instruction.indexOf("LEFT") >= 0 || instruction.indexOf("left") >= 0) {
      drawLeftArrow();
      tft.drawString(instruction, 10, 100, 2);
    } else if (instruction.indexOf("RIGHT") >= 0 || instruction.indexOf("right") >= 0) {
      drawRightArrow();
      tft.drawString(instruction, 10, 100, 2);
    } else {
      drawForwardArrow();
      tft.drawString(instruction, 10, 100, 2);
    }
  }
}
```

---

## 📱 Android Application Overview

- **Package:** `com.developerfour.lastoption`
- **Target SDK:** Android 34 (compileSdk 35)
- **Key Modules:**
  - `MainActivity.kt`: Bluetooth device discovery, SPP connection management, and manual hardware diagnostics (`SEND LEFT`, `SEND RIGHT`, `SEND FORWARD`).
  - `NavigationActivity.kt`: Destination query with Google Places Autocomplete, origin/destination marker rendering, and location permissions handling.
  - `NavigationActivity2.kt`: Real-time navigation tracking loop, Google Directions JSON polyline decoding, automated proximity detection (<20m threshold) with automatic next-step advancement, Text-to-Speech (TTS), and streaming commands to the ESP32 via `sendToESP32()`.
  - `NavigationParser.kt`: Strips HTML direction instructions into clean, legible strings and decodes overview polyline geometry.

---

## 📊 Market Opportunity & Commercial Scope

- **Massive Two-Wheeler Market:** Over **22+ Crore registered two-wheelers in India** alone, representing immense scale for aftermarket and OEM safety installations.
- **Delivery Fleets & Gig Workers:** Essential for food delivery & quick-commerce giants (*Zomato, Swiggy, Zepto, Blinkit*) to reduce delivery executive accidents and improve delivery turnaround times safely.
- **Bike-Taxi Integration:** Elevated passenger confidence and compliance for mobility providers (*Rapido, Uber Moto*).
- **InsurTech Partnerships:** Telematics and crash logs provide insurance carriers with quantifiable rider risk profiles and lower loss ratios.

---

## ⚡ Getting Started

### Prerequisites
1. **Android Studio Ladybug (or newer)** with Android SDK 34+.
2. **ESP32 Development Board** (ESP32-WROOM or ESP32-S3) flashed with Bluetooth SPP receiver firmware.
3. **Google Maps & Places API Key** with Directions API enabled.

### Setup Instructions
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Yuvraj9198/Smart-Rider-Safety-Device.git
   cd Smart-Rider-Safety-Device
   ```

2. **Configure API Keys:**
   Open `app/src/main/res/values/strings.xml` and insert your Google Maps API Key:
   ```xml
   <string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
   ```

3. **Build & Install:**
   Connect your Android device with USB debugging enabled, then run:
   ```bash
   ./gradlew installDebug
   ```

4. **Pair with Hardware:**
   - Power on the ESP32 module.
   - Open the **Smart Rider** app, grant Location and Bluetooth permissions.
   - Tap **Scan for Devices**, select your ESP32 module (e.g. `SmartRider-ESP32`), and establish the connection.
   - Select your destination and tap **Start Navigation Realtime** to begin live streaming directions to your hardware.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
