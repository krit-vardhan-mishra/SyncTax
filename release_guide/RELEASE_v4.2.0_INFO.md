# SyncTax v4.2.0 Release Information

## Release Details

### Version Information
- **Version Name**: `4.2.0`
- **Version Code**: `6`
- **Release Tag**: `v4.2.0`
- **Release Title**: `SyncTax v4.2.0 - Offline & Update System Stability`
- **Release Date**: February 1, 2026

---

## Files to Update

### 1. Gradle Configuration
**File**: `app/build.gradle.kts`

Update the following lines:
```kotlin
versionCode = 6
versionName = "4.2.0"
```

**Current values (v4.1.0)**:
```kotlin
versionCode = 5
versionName = "4.1.0"
```

---

## Release Description

### GitHub Release Body

```markdown
## 🎉 SyncTax v4.2.0 - Offline & Update System Stability

### ✨ Key Improvements & Fixes

#### 💾 Robust Offline Mode
- **Properly Implemented Saved Songs**: The offline song saving functionality has been significantly improved and is now fully stable.
- **Metadata Preservation**: Saved songs correctly retain all metadata including album art and artist info.
- **Offline Playback**: Seamless playback of saved songs when no internet connection is available.

#### 🔄 Fully Functional Auto-Update
- **Update System Fixes**: The automatic update system introduced in v4.1.0 has been patched and now functions correctly.
- **Reliable Checks**: Accurate background checks for both App and NewPipe Extractor library updates.
- **Seamless Installs**: Direct download and installation of updates from GitHub Releases works smoothly.

#### 🎨 UI/UX Enhancements
- **Favorite Status Consistency**: Fixed issue where "Add to Favorites" / "Remove from Favorites" text was not updating correctly across different screens (Songs tab vs Favorites tab). Now, the status is synchronized globally.
- **Refined Song Menu**: The song options menu now reliably shows the correct favorite action regardless of where it is opened.

### 🔗 Changes vs v4.1.0
While v4.1.0 introduced the Update System and Offline Mode, this release (v4.2.0) addresses critical stability issues that prevented them from working as intended.
- **Fixed**: Auto-update check failing or not triggering.
- **Fixed**: Saved songs not always appearing in offline mode.
- **Fixed**: Inconsistent "Remove from Favorites" text in the library.

### 📱 Compatibility
- **Minimum Android Version**: Android 10 (API 29)
- **Target Android Version**: Android 15 (API 36)
- **Supported Architectures**: arm64-v8a, armeabi-v7a, x86_64, universal

### 🙏 Credits
Thanks to the user community for reporting the favorite status inconsistency and update issues.

---

**Full Changelog**: https://github.com/krit-vardhan-mishra/SyncTax/compare/v4.1.0...v4.2.0
```

---

## Release Creation Checklist

### Before Creating Release

- [ ] ✅ Update `versionCode` to `6` in `app/build.gradle.kts`
- [ ] ✅ Update `versionName` to `"4.2.0"` in `app/build.gradle.kts`
- [ ] ⬜ Build release APK (`./gradlew assembleRelease`)
- [ ] ⬜ Verify Favorite text changes dynamically (Add <-> Remove)
- [ ] ⬜ Verify Offline Song saving works
- [ ] ⬜ Verify Update Check finds this new version (mock test)

### Creating the GitHub Release

1. **Tag Version**: `v4.2.0`
2. **Release Title**: `SyncTax v4.2.0 - Offline & Update System Stability`
3. **Description**: Use the body text above.
4. **Attach APK**: `SyncTax-v4.2.0.apk` (or architecture specific)

---
