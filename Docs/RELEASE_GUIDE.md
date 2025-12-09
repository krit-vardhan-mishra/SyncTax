# SyncTax Release Guide

This guide explains how to create GitHub releases for SyncTax so that the app can correctly check for updates and download the latest APK.

## 📋 Prerequisites

Before creating a release, ensure you have:
- Built the APK in release mode
- Tested the APK thoroughly
- Updated the version in `app/build.gradle.kts` (versionCode and versionName)

## 🚀 Release Creation Steps

### Step 1: Go to GitHub Releases
1. Open your GitHub repository: https://github.com/krit-vardhan-mishra/SyncTax
2. Click on **"Releases"** in the right sidebar
3. Click **"Create a new release"** button

### Step 2: Tag Version (CRITICAL - App Checks This)
```
Tag version: v3.5.0
```
**Important Rules:**
- Must start with `v` followed by the version number
- Version number must match exactly what's in `app/build.gradle.kts` `versionName`
- Format: `v{major}.{minor}.{patch}` (e.g., `v3.5.0`, `v1.2.3`)
- The app compares this tag to determine if an update is available

### Step 3: Release Title
```
Release title: SyncTax v3.5.0
```
**Format:** `SyncTax v{version}` (e.g., `SyncTax v3.5.0`)

### Step 4: Release Description
Write a clear description of what's new in this version. Example:

```
## What's New in SyncTax v3.5.0

### ✨ New Features
- Added automatic update checking
- Improved playlist management
- Enhanced audio quality

### 🐛 Bug Fixes
- Fixed crash when loading large playlists
- Resolved album art display issues

### 🔧 Technical Improvements
- Optimized memory usage
- Better error handling

### 📱 Compatibility
- Minimum Android version: API 29
- Target Android version: API 36
```

**Guidelines:**
- Use markdown formatting
- Categorize changes (Features, Bug Fixes, Improvements)
- Mention compatibility requirements
- Keep it user-friendly and informative

### Step 5: Attach APK File (CRITICAL - App Downloads This)
**File Requirements:**
- File must be a valid Android APK
- File extension must be `.apk` (case-sensitive)
- File name can be anything, but recommended format: `SyncTax-{version}.apk`

**Example file names that work:**
- ✅ `SyncTax-3.5.0.apk`
- ✅ `SyncTax-v3.5.0.apk`
- ✅ `app-release.apk`
- ✅ `SyncTax.apk`

**Important:** The app automatically finds and downloads the first `.apk` file in the release assets.

### Step 6: Pre-release Checkbox
- **Uncheck** "Set as a pre-release" unless this is a beta/test version
- Pre-releases are ignored by the update checker

### Step 7: Publish Release
Click **"Publish release"**

## 🔍 How the App Checks for Updates

The app uses this GitHub API endpoint:
```
GET https://api.github.com/repos/krit-vardhan-mishra/SyncTax/releases/latest
```

It looks for:
1. **`tag_name`**: Must be `v{version}` format (e.g., `v3.5.0`)
2. **Assets**: Finds the first file ending with `.apk`
3. **Comparison**: Compares tag version with app's current version

## ⚠️ Critical Mistakes to Avoid

### ❌ Wrong Tag Format
- `3.5.0` (missing `v` prefix) → App won't recognize version
- `version-3.5.0` → App won't parse correctly
- `v3.5` → Missing patch version

### ❌ Wrong APK Extension
- `SyncTax-3.5.0.apk.zip` → Not `.apk`
- `SyncTax-3.5.0.APK` → Wrong case

### ❌ Pre-release Set
- Pre-releases are ignored by the update checker

### ❌ Version Mismatch
- Tag: `v3.5.0` but app version is `3.4.0` → Won't trigger update
- Tag: `v3.5.0` but app version is `3.5.1` → Won't show update

## 📝 Version Numbering Convention

Follow semantic versioning:
- **Major** (3.x.x): Breaking changes
- **Minor** (x.5.x): New features
- **Patch** (x.x.0): Bug fixes

Examples:
- `3.5.0` → First release of version 3.5
- `3.5.1` → Bug fix for 3.5.0
- `4.0.0` → Major version with breaking changes

## 🧪 Testing the Release

After publishing:

1. **Wait 5-10 minutes** for GitHub to process the release
2. **Test the API**: Visit `https://api.github.com/repos/krit-vardhan-mishra/SyncTax/releases/latest`
3. **Verify JSON** contains correct `tag_name` and `assets` with `.apk` file
4. **Test in app**: Use "Check for Updates" in Settings

## 🔄 Update Flow

1. User taps "Check for Updates" in Settings
2. App fetches latest release data
3. Compares versions
4. If newer version available:
   - Shows snackbar: "Update available: v3.5.0"
   - User taps "Download"
   - Downloads APK to Downloads folder
   - Opens Android installer
5. User installs update

## 📞 Support

If updates aren't working:
1. Check the GitHub API response
2. Verify tag format and APK extension
3. Ensure release is not marked as pre-release
4. Check app logs for error messages

---

**Remember:** The tag version and APK file extension are the two most critical elements that must be exactly right for updates to work!</content>
<parameter name="filePath">e:\Git-Hub\SnycTax\docs\RELEASE_GUIDE.md