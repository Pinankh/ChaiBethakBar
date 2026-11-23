# Chai Bathak Bar — Paperless Android Tablet POS

A lightweight, paperless 100% digital Point-of-Sale (POS) application built for Android Tablets in landscape orientation (10-inch+). Built specifically for café and restaurant co-owners managing Dine-In tables and Takeaway parcels with Cash and Online payment support.

---

## ☕ Key Features

- **Dine-In Management & Interactive Floor Map**:
  - Zone-filtered table view (*Indoor*, *Patio*, *Counter*).
  - Real-time color-coded table statuses: **Available** (Green), **Occupied** (Amber), **Billed** (Blue).
  - Running order tracking, elapsed dining time display, and atomic table-shifting.

- **Fast Takeaway & Parcel Counter Workflow**:
  - Daily resetting token numbers (`Parcel #01`, `Parcel #02`, etc.).
  - Active takeaway parcel strip on top of the home screen for quick order tracking.
  - 1-tap "Done ✓" handover status update once packed and delivered.

- **Unified 3-Pane Order & Billing Workspace**:
  - **Left Pane (Categories)**: Quick category filters (*Chai*, *Coffee*, *Snacks*, *Mocktails*, *Combos*).
  - **Centre Pane (Menu Grid)**: Search bar, Veg/Non-Veg filter, and touch-friendly 3-column menu tiles.
  - **Right Pane (Live Cart)**: Cooking notes (e.g., *Less Sugar*, *Extra Adrak*), custom items, packaging charges, discount calculations, CGST/SGST/VAT splits, and nearest-rupee round-off.

- **Paperless Digital Receipts & Simple Payments**:
  - On-screen digital bill dialog with optional Android Share Sheet (`Intent.ACTION_SEND`) to share text receipts via WhatsApp or SMS.
  - Payment settlement strictly via **ONLINE** (UPI/Bank) or **CASH** (with quick-tender chips and change-to-return helper).

- **Offline-First Firestore Architecture**:
  - Powered by Firebase Cloud Firestore with persistent local disk cache enabled.
  - All transactions and order edits queue cleanly offline during Wi-Fi outages.
  - Immutable historical order records (cancelled items logged with reasons).

- **Daily Sales Reports & Tally**:
  - Closing tally comparing exact Cash vs. Online revenue.
  - Key performance metrics: Total Sales, Order Count, Dine-In vs. Takeaway split, Discounts, and Tax summaries.
  - Top-selling items list ranked by quantity and revenue.

---

## 🛠️ Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose with Material 3 (Optimized for landscape tablets)
- **Architecture**: MVVM + Unidirectional Data Flow (StateFlow) + Repository Pattern
- **Backend & Storage**: Firebase Cloud Firestore (Offline Cache enabled) & Firebase Authentication
- **Concurrency**: Kotlin Coroutines & Flows
- **SDK Targets**: `minSdk = 26` (Android 8.0) | `compileSdk` / `targetSdk = 35` (Android 15)
- **Monetary Logic**: All money calculations stored in paise (`Long`) to guarantee accurate precision.

---

## 📁 Project Structure

```
ChaiBethakBar/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/com/pinankh/chaibethakbar/
│               ├── MainActivity.kt
│               ├── data/
│               │   └── PosRepository.kt     # Firestore real-time listeners & offline WriteBatch operations
│               ├── model/
│               │   └── PosModels.kt         # Data models (OutletConfig, TableModel, MenuItem, OrderItem, PosOrder)
│               ├── ui/
│               │   └── theme/               # Material 3 Café Theme (Color, Type, Theme)
│               └── util/
│                   ├── CurrencyAndTax.kt    # Money in Paise (Long) and Tax Calculator logic
│                   └── NetworkMonitor.kt    # Real-time network sync monitor
├── build.gradle.kts
├── gradle/libs.versions.toml
└── PRD.md                                  # Product Requirements Document
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Ladybug 2024.2.1 or newer)
- Android SDK 35
- Android Tablet or Emulator (10-inch+ recommended) in landscape mode

### Setup Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/ChaiBethakBar.git
   cd ChaiBethakBar
   ```

2. **Configure Firebase**:
   - Go to the [Firebase Console](https://console.firebase.google.com/) and create a project.
   - Add an Android app with the package name `com.pinankh.chaibethakbar`.
   - Enable **Authentication** (Email/Password method).
   - Enable **Cloud Firestore** and apply the security rules found in `PRD.md`.
   - Download `google-services.json` and place it in the `app/` folder.

3. **Build & Run**:
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Select an Android Tablet emulator or physical tablet device in landscape orientation and click **Run**.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
