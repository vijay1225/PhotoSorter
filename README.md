# PhotoSorter - Minimalist Offline Gallery Cleaner & Media Triage

An ultra-fast, minimalist, privacy-first Android application built with **Kotlin** and **Jetpack Compose**. Clean, sort, and organize thousands of device photos and videos with intuitive 4-way gesture triage, filmstrip physics, full multi-year calendar navigation, and safe Android MediaStore batch deletion.

**Developer:** **Thanelanaka Vijaya Kumar**  
**Repository:** [https://github.com/vijay1225/PhotoSorter](https://github.com/vijay1225/PhotoSorter)

---

## 🔒 Privacy & Security First

PhotoSorter is built with a zero-trust, strictly offline privacy architecture:
- **Zero Logins & Accounts**: No sign-ups, no passwords, no email collection, and no cloud accounts.
- **100% Offline (Zero Network Access)**: The application **does NOT declare `android.permission.INTERNET`** in its manifest. The Android OS physically prevents the app from creating network sockets, communicating with remote servers, or transmitting data/telemetry.
- **Media Permissions Only**: Only standard media access permissions are requested to read media on device:
  - `READ_MEDIA_IMAGES` & `READ_MEDIA_VIDEO` (Android 13+)
  - `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+)
  - `READ_EXTERNAL_STORAGE` (Android 12 and below)
- **Safe OS-Level Deletion**: Deletions are mediated through the official Android `MediaStore.createDeleteRequest` API (API 30+). Files are never deleted without explicit system confirmation sheets.
- **Local Private SQLite Database**: All triage states, categorization labels, and session skip positions are stored strictly inside the app's internal database (`photosort.db`).
- **Backup Disabled**: `android:allowBackup="false"` ensures local database states cannot be extracted via unauthorized adb backups.

---

## 🌟 Key Features

### 1. ⚡ Ergonomic 4-Way Gesture Triage
Sort thousands of photos effortlessly with natural swipes:
- ⬆️ **Swipe UP -> KEEP**: Save and label as Keep (Emerald Green).
- ⬇️ **Swipe DOWN -> DELETE**: Queue for trash deletion (Crimson Red).
- ⬅️ **Swipe LEFT -> LATER**: Skip forward to categorize later (Blue). Remembers your position across sessions.
- ➡️ **Swipe RIGHT -> PREVIOUS**: Return to the previous media to re-categorize or undo.
- **Filmstrip Physics**: Smooth side-to-side transitions simulating moving film.
- **Aspect Ratio Control**: Toggle between unclipped `Fit` and border-to-border `Fill` viewing modes.
- **Maximum Screen Real Estate**: Over 85% of viewport dedicated purely to your media.

### 2. 🎬 Dedicated Video Triage & Preview
- Separate Photos and Videos home modes with a single-tap pill (`[ 📸 Photos | 🎬 Videos ]`).
- Dynamic video duration chips and thumbnail extraction.
- Prominent glassmorphic **Play Button** to review video playback and audio before making keep/delete decisions.

### 3. 📅 Multi-Year Calendar DatePicker & All-Time Timeline
- **Interactive Calendar DatePicker**: Jump to any specific year, month, or day with Material 3 calendar pagination and year selection.
- **All-Time Timeline**: Aggregated directly across your entire device storage, grouped by year (2026, 2025, 2024...) with live item counts for every month.
- **Quick Preset Chips**: One-tap jumping to **Recent**, **3 Months Ago**, **6 Months Ago**, and **1 Year Ago**.
- **Session-Only Reset**: Temporary date jumps automatically reset when the app restarts, resuming sorting from top incoming images.

### 4. 🔄 Flexible Sorting Order & Folder Filtering
- **Sort Order Toggle**: Switch between **Newest First** and **Oldest First** directions.
- **Album / Folder Selector**: Filter media by folder (Camera, WhatsApp, Screenshots, Downloads, DCIM) with real-time item counters.

### 5. 📤 Quick Share Integration
- Easily share any current photo or video directly via the Android system share sheet before deciding to keep or delete it.

### 6. 📚 Sections Tab (Review & Re-label)
- Browse categorized sections: **Uncategorized**, **Keep**, and **Later**.
- **Single-Item Triage Mode**: Tap any item in a section to open it directly in the swipe triage view, categorize it, and immediately return back to your grid.
- **Multi-Select**: Batch re-label or restore multiple photos in one tap.

### 7. 🗑️ Trash & Permanent Deletion
- Review all queued deletions with total calculated reclaimable storage space.
- Execute batch permanent deletions via Android's secure system confirmation dialog.
- Instant Restore option to revert decisions before deletion.

---

## 📱 Screenshots & Navigation Flow

```
+-----------------------------------------------------------+
| [ Photos | Videos ]  [ Folder v ]  [ Sort ]  [ 📅 Date ]  |  <- Compact Bar (~46dp)
+-----------------------------------------------------------+
|                                                           |
|                                                           |
|                    FULL-BLEED MEDIA CARD                  |  <- 85%+ Screen Area
|                         (Filmstrip)                       |
|                                                           |
|                                                           |
+-----------------------------------------------------------+
| [ < Prev ]   [ X Delete ]   [ ↺ Undo ]   [ Share ]   [ > Later ] |  <- Ergonomic Thumb Bar
+-----------------------------------------------------------+
|               [ Triage ]     [ Sections ]     [ Trash ]          |  <- Navigation Bar
+-----------------------------------------------------------+
```

---

## 📦 Download & Installation

Signed **Release APKs** are available in this repository:
- **Location**: [`release-apk/PhotoSorter-release.apk`](release-apk/PhotoSorter-release.apk)

### Installation via Device:
1. Download `PhotoSorter-release.apk` to your Android device.
2. Open your device **Files** or **Downloads** app.
3. Tap the APK file and select **Install** (allow installation from this source if prompted).

### Installation via ADB:
```bash
adb install -r release-apk/PhotoSorter-release.apk
```

---

## 🛠️ Building from Source

### Prerequisites:
- Android Studio Iguana / Ladybug or newer
- JDK 17 (`JAVA_HOME` pointing to Java 17)
- Android SDK 35 with Build Tools 35.0.0

### Build Instructions:
```bash
# Clone the repository
git clone https://github.com/vijay1225/PhotoSorter.git
cd PhotoSorter

# Run Unit Tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Signed Release APK
./gradlew assembleRelease
```
The generated APK will be at `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License & Attribution

Developed by **Thanelanaka Vijaya Kumar**.  
Released under the MIT License. Feel free to use, modify, and distribute with attribution.
