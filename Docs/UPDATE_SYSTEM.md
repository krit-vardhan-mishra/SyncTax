# SyncTax Update System Documentation

## Overview
SyncTax v4.1.0 is a major update introducing a comprehensive automatic update checking system inspired by ytdlnis, along with significant improvements to recommendation logic, search functionality, song management, and user experience features like pull-to-refresh.

## Features

### 1. App Update Checking
- **Automatic Check on App Start**: Optionally check for updates when the app launches
- **Manual Check**: Check for updates anytime from Settings
- **Background Updates**: Periodic background checks with notifications
- **Beta Channel Support**: Option to receive beta/pre-release versions
- **Direct Download & Install**: Download APKs directly from GitHub releases

### 2. Library Update Checking
- **NewPipe Extractor Version Tracking**: Monitor the NewPipe Extractor library version
- **Update Notifications**: Get notified when library updates are bundled with new app versions
- **Release Notes**: View detailed release notes for library updates

### 3. Update Settings
- **Check on App Start**: Toggle automatic update checks when the app opens
- **Include Beta Versions**: Opt-in to beta/pre-release notifications
- **Skip Version**: Skip specific versions you don't want to install

### 4. Changelog Viewer
- **Full Release History**: View all releases with descriptions
- **Release Notes**: Read detailed changelogs for each version
- **Direct Download Links**: Download any previous version

## Architecture

### Components

#### 1. UpdateUtil (`core/utils/UpdateUtil.kt`)
Central utility class handling all update operations:
- GitHub API integration
- Version comparison logic
- APK architecture matching (arm64-v8a, armeabi-v7a, x86_64, universal)
- Download management with DownloadManager
- Skip version functionality

#### 2. AppUpdateViewModel (`presentation/viewmodels/AppUpdateViewModel.kt`)
ViewModel managing update UI state:
- `AppUpdateState`: Tracks app update status (Idle, Checking, UpdateAvailable, Downloading, etc.)
- `LibraryUpdateState`: Tracks library update status
- `allReleases`: Full release history for changelog
- Progress tracking for downloads

#### 3. UpdateSettingsSection (`core/ui/UpdateSettingsSection.kt`)
Composable UI component providing:
- App version display
- Update check buttons
- Progress indicators
- Update dialogs
- Changelog viewer
- Auto-update settings toggles

#### 4. UpdateCheckWorker (`core/worker/UpdateCheckWorker.kt`)
WorkManager background worker:
- Periodic update checks (every 12 hours)
- Notification when updates available
- Respects user preferences (skip version, beta channel)

#### 5. GithubRelease (`data/model/GithubRelease.kt`)
Data model for GitHub API responses:
- Release metadata (tag, name, body, assets)
- Gson serialization annotations

### Update Flow

```
User Action (Check for Updates)
        ↓
AppUpdateViewModel.checkForAppUpdate()
        ↓
UpdateUtil.checkForAppUpdate()
        ↓
GitHub API Call
        ↓
Version Comparison
        ↓
UpdateAvailable State
        ↓
User Taps "Update"
        ↓
UpdateUtil.downloadAndInstallAppUpdate()
        ↓
DownloadManager
        ↓
APK Downloaded
        ↓
Android Installer Opens
```

### Version Comparison Logic

The system uses a smart version comparison algorithm:

1. **Tag Format**: Handles `v3.5.0`, `v-3.5.0`, `v3.5.0-beta` formats
2. **Numeric Conversion**: Converts versions to comparable integers
3. **Beta Handling**: 
   - If user enables beta, shows pre-release versions
   - If stable is newer than beta, prioritizes stable
   - If current version is beta and user disables beta updates, allows "downgrade" to stable
4. **Architecture Matching**: Automatically selects correct APK for device architecture

## Usage

### For Users

#### Checking for Updates
1. Open **Settings**
2. Scroll to **Updates** section
3. Tap **Check for App Updates**
4. If update available, tap **Update** button
5. APK downloads automatically
6. Install prompt appears when download completes

#### Checking Library Updates
1. Open **Settings**
2. Scroll to **Updates** section
3. Tap **Check Library Updates**
4. View NewPipe Extractor version information
5. If update available, details shown with GitHub link

#### Enabling Auto-Check
1. Open **Settings**
2. Scroll to **Update Settings** card
3. Toggle **Check on app start**
4. Optionally toggle **Include beta versions**

### For Developers

#### Creating a Release
See [RELEASE_GUIDE.md](RELEASE_GUIDE.md) for detailed release creation instructions.

**Quick Checklist**:
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Build release APK
3. Create GitHub release with tag `v{version}` (e.g., `v4.1.0`)
4. Attach APK file (must end with `.apk`)
5. Write release notes
6. Publish release (uncheck "pre-release" unless it's a beta)

#### Testing Update System
```kotlin
// Check for updates programmatically
val updateUtil = UpdateUtil(context)
val result = updateUtil.checkForAppUpdate()

result.onSuccess { release ->
    Log.d("Update", "New version available: ${release.tagName}")
}

result.onFailure { error ->
    Log.e("Update", "Update check failed: ${error.message}")
}
```

## Technical Details

### APK Architecture Detection
The system automatically detects device architecture and downloads the appropriate APK:

```kotlin
val supportedAbis = Build.SUPPORTED_ABIS
val deviceArch = when {
    supportedAbis.contains("arm64-v8a") -> "arm64-v8a"
    supportedAbis.contains("armeabi-v7a") -> "armeabi-v7a"
    supportedAbis.contains("x86_64") -> "x86_64"
    supportedAbis.contains("x86") -> "x86"
    else -> "universal"
}
```

Priority order for APK selection:
1. Exact architecture match (e.g., `SyncTax-v4.1.0-arm64-v8a.apk`)
2. Universal APK (e.g., `SyncTax-v4.1.0.apk`)
3. First `.apk` file found

### Background Update Checks

WorkManager configuration:
- **Interval**: Every 12 hours
- **Constraints**: Network connected
- **Retry Policy**: Linear backoff, 30 minutes
- **Initial Delay**: 1 hour after scheduling

Notification shown when:
- New version available
- Version not skipped by user
- Version not already notified

### Preferences

Stored in SharedPreferences `{packageName}_preferences`:
- `check_update_on_start`: Boolean - Auto-check on app start
- `include_beta_updates`: Boolean - Include pre-release versions
- `skipped_version`: String - Version user chose to skip

Worker preferences in separate SharedPreferences `update_worker_prefs`:
- `last_notified_version`: String - Last version notified about

## API Integration

### GitHub API Endpoint
```
GET https://api.github.com/repos/krit-vardhan-mishra/SyncTax/releases/latest
```

Response structure:
```json
{
  "tag_name": "v4.1.0",
  "name": "SyncTax v4.1.0",
  "body": "Release notes markdown...",
  "prerelease": false,
  "assets": [
    {
      "name": "SyncTax-v4.1.0-arm64-v8a.apk",
      "browser_download_url": "https://github.com/.../SyncTax-v4.1.0-arm64-v8a.apk",
      "size": 145000000
    }
  ]
}
```

### NewPipe Extractor API
```
GET https://api.github.com/repos/TeamNewPipe/NewPipeExtractor/releases/latest
```

Used to check library version and provide update information.

## Security Considerations

1. **HTTPS Only**: All downloads use HTTPS
2. **Official Repository**: Only downloads from official GitHub repository
3. **User Confirmation**: User must explicitly tap "Update" button
4. **Android Installer**: Uses system installer, which verifies APK signature
5. **Permissions**: Uses DownloadManager (no special permissions required)

## Troubleshooting

### Update Not Showing
- Verify internet connection
- Check GitHub releases page manually
- Ensure release tag format is `v{version}`
- Confirm APK file has `.apk` extension
- Check app logs for error messages

### Download Fails
- Check storage space
- Verify network connection
- Try downloading from GitHub directly
- Check DownloadManager app settings

### Version Comparison Issues
- Ensure `versionName` in gradle matches release tag
- Check for extra characters in version string
- Verify semantic versioning format (X.Y.Z)

## Future Enhancements

Potential improvements for future versions:
1. Delta updates (only download changed files)
2. In-app update prompts (Google Play-style)
3. Automatic installation (with user permission)
4. Update scheduling (install at specific time)
5. Rollback functionality
6. Multi-language support in release notes
7. Update size and changelog preview before download

## Credits

Inspired by the excellent update system in [ytdlnis](https://github.com/deniscerri/ytdlnis).

---

**Version**: 4.1.0  
**Last Updated**: January 30, 2026  
**Author**: SyncTax Team
