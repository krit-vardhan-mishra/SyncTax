# SyncTax v4.2.1 Release Information

## Release Details

### Version Information
- **Version Name**: `4.2.1`
- **Version Code**: `7`
- **Release Tag**: `v4.2.1`
- **Release Title**: `SyncTax v4.2.1 - Crash Logger & Update Enhancements`
- **Release Date**: February 10, 2026

---

## Files to Update

### 1. Gradle Configuration
**File**: `app/build.gradle.kts`

Update the following lines (Already applied):
```kotlin
versionCode = 7
versionName = "4.2.1"
```

**Previous values (v4.2.0)**:
```kotlin
versionCode = 6
versionName = "4.2.0"
```

---

## Release Description

### GitHub Release Body

```markdown
## 🎉 SyncTax v4.2.1 - Crash Logger & Update Enhancements

### ✨ Key Features

#### 🐞 Built-in Crash Logger
- **Automatic Logging**: The app now captures crash details automatically.
- **Log Location**: Crash reports are saved to `Android/data/com.just_for_fun.synctax/files/crash_logs/`.
- **Privacy Focused**: Logs are stored locally on your device and can be shared with developers to help fix issues.

#### 🔄 Improved Update Experience
- **Home Screen Update Card**: A dismissible card now appears at the top of the Home screen when a new app update is available.
- **Smart Library Updates**: The Settings screen now intelligently prompts you to "Update App" when a NewPipe Extractor update is available (since it's bundled with the app).
- **Dynamic Versioning**: App now correctly reports the exact version of the active NewPipe Extractor library.

### 🐛 Bug Fixes & Stability

#### ⚡ Crash Fixes
- **Search Screen**: Fixed a crash that occurred when searching online due to background thread UI updates.
- **Shuffle Mode**: Fixed a crash when shuffling online recommendations.

#### 💾 Offline Mode Fixes
- **Song Persistence**: Fixed a critical bug where saved offline songs were not being found/played correctly due to file path mismatches.
- **Filename Sanitization**: Ensured consistent filename handling for all offline operations.

#### 🔧 Other Improvements
- **NewPipe Notification**: Clicking the "Library Update" notification now correctly opens the app update settings instead of the browser.
- **Bottom Navigation**: Verified smooth animation when entering settings.

### 📱 Compatibility
- **Minimum Android Version**: Android 10 (API 29)
- **Target Android Version**: Android 15 (API 36)
- **Supported Architectures**: arm64-v8a, armeabi-v7a

### 🙏 Credits
Thanks for your feedback on the crash reporting and update process!

---

**Full Changelog**: https://github.com/krit-vardhan-mishra/SyncTax/compare/v4.2.0...v4.2.1
```

---

## Release Creation Checklist

### Before Creating Release

- [x] ✅ Update `versionCode` to `7` in `app/build.gradle.kts`
- [x] ✅ Update `versionName` to `"4.2.1"` in `app/build.gradle.kts`
- [ ] ⬜ Build release APK (`./gradlew assembleRelease`)
- [ ] ⬜ Verify Crash Logger works (force a crash in debug build)
- [ ] ⬜ Verify Update Card appears (mock update check)
- [ ] ⬜ Verify Offline Song playback works

### Creating the GitHub Release

1. **Tag Version**: `v4.2.1`
2. **Release Title**: `SyncTax v4.2.1 - Crash Logger & Update Enhancements`
3. **Description**: Use the body text above.
4. **Attach APK**: `SyncTax-v4.2.1.apk`

---
