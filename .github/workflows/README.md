# yt-dlp Downloader (Android)

Compose app wrapping yt-dlp via the youtubedl-android library (bundles Python, yt-dlp and ffmpeg).

## Build
1. Open this folder in Android Studio (Koala or newer). It will sync and generate the Gradle wrapper.
2. Run on a device/emulator (Android 10+), or Build > Build APK(s).

## Notes
- Files are saved to Downloads/YtDlpApp.
- Use "Update yt-dlp" regularly; sites change often.
- Share a link from your browser/YouTube to this app to prefill the URL.
- Downloads run while the app is open (no background service yet).
- If the dependency version fails to resolve, check the latest tag at
  https://github.com/yausername/youtubedl-android and edit `ytdlp` in app/build.gradle.kts.

## Build the APK without installing anything (GitHub Actions)
1. Create a new GitHub repo and upload everything in this folder (keep the .github folder).
2. Open the Actions tab > "Build APK" > Run workflow.
3. When it finishes, download the "YtDlpApp-debug-apk" artifact, unzip it, and install the .apk on your phone
   (allow "Install unknown apps" when prompted).
