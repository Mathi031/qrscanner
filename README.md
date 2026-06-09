# QR Scanner

A QR code scanner for Android: fast, private, and with smart actions based on the scanned content.

## Features

- Live camera scanning, with automatic on-screen code detection.
- Scanning codes from images stored in the gallery.
- Contextual actions based on the content type: open a link, write an email, call or send an SMS to a number, view a location on the map, or connect to a Wi-Fi network.
- Safety warning before opening potentially deceptive links (shorteners, domains with hidden or non-ASCII characters, etc.).
- Persistent scan history, with search and favorites.
- Privacy control: you can turn off history saving and exclude Wi-Fi passwords so they are never stored.
- Quick Settings tile to open the scanner from any screen with a single tap.
- Dark mode support, consistent with the device theme.
- Accessible interface: it adapts to the system's large text without clipping or overlap.

## Tech stack

- **Kotlin** + **Jetpack Compose** and **Material 3** for the entire UI.
- **CameraX** for the preview and frame analysis.
- **ML Kit Barcode Scanning** (bundled model, embedded in the APK, no runtime download).
- **Room** for the persistent history.
- **DataStore Preferences** for the privacy settings.
- Minimum **Android 7.0 (API 24)**.

## Architecture

The project is organized in layers: the UI layer (`ui/`) groups each screen with its `ViewModel`, and the data layer (`data/`) holds the Room entities and DAO, the repositories, and the DataStore preferences. Shared dependencies (database, repositories) are exposed from the `Application` class, which acts as a simple service locator, without any dependency-injection framework. Navigation between screens is handled with **Navigation Compose**. UI state is modeled with `StateFlow`, while one-shot events —such as a code detection— are delivered through a `Channel`, so they are not replayed after a recomposition or a configuration change.

## Local build

```bash
git clone https://github.com/mathi031-ja/qrscanner.git
cd qrscanner
./gradlew assembleDebug
./gradlew installDebug  # with a device connected over USB
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Release build (Play Store)

The release build is minified (R8 + resource shrinking) and signed with an upload
keystore that is never committed.

1. Create `keystore.properties` in the repository root (it is in `.gitignore`):

   ```properties
   storeFile=/Users/mathi03/keystores/qrscanner-upload.jks
   storePassword=********
   keyAlias=upload
   keyPassword=********
   ```

   If this file is missing (CI, or a fresh clone) the debug build still works; only
   the signed release build requires it.

2. Generate the signed App Bundle:

   ```bash
   ./gradlew bundleRelease
   ```

   The bundle is produced at `app/build/outputs/bundle/release/app-release.aab` —
   that is the file you upload to Play Console.

## Permissions

- **CAMERA**: the only runtime permission. The app requests it the first time you open the live scanner; if you deny it, you can still scan from the gallery.

No other permission is requested from the user. The CameraX and ML Kit libraries declare *normal*-level permissions (`INTERNET`, `ACCESS_NETWORK_STATE`) that the system grants automatically and that require no approval; scanning works offline.

## Screenshots

<!-- TODO: add screenshots under docs/screenshots/ -->

## License

Released under the MIT License — see the [LICENSE](LICENSE) file. Copyright (c) 2026 George.
