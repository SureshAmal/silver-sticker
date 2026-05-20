# SilverSticker

SilverSticker is an Android sticker-pack manager built with Kotlin and Jetpack Compose. It lets users create local sticker packs, convert selected images into WhatsApp-compatible WebP assets, manage sticker metadata, import and export packs as ZIP files, and add packs to WhatsApp or WhatsApp Business.

## Features

- Create sticker packs with a custom name, publisher, tray icon, and sticker images.
- Convert static images to WebP stickers sized for WhatsApp requirements.
- Convert animated GIFs to animated WebP where supported, with size-aware compression.
- Add, remove, edit, and preview stickers inside an existing pack.
- Assign emojis to individual stickers.
- Import and export sticker packs as ZIP archives.
- Share one or more sticker packs through Android share intents.
- Add packs to WhatsApp or WhatsApp Business through the platform sticker-pack intent.
- Provide sticker metadata and files through an Android `ContentProvider`.
- Browse packs in list or grid view with Material 3 Compose UI.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Navigation Compose
- AndroidX Lifecycle ViewModel
- Coil for image loading, GIF decoding, and animated image support
- Gson for local sticker-pack metadata
- Gradle Kotlin DSL

## Requirements

- Android Studio with Android Gradle Plugin 9.2.1 support
- JDK 11 or newer
- Android SDK with compile SDK 36 installed
- Gradle wrapper included in this repository
- Android device or emulator running Android 7.0 or newer

## Getting Started

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/SureshAmal/silver-sticker.git
cd silver-sticker
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew test
```

Install the debug build on a connected device:

```bash
./gradlew installDebug
```

## Project Structure

```text
app/src/main/java/com/example/silversticker/
+-- MainActivity.kt
+-- models/
|   +-- StickerModels.kt
+-- ui/
|   +-- StickerViewModel.kt
|   +-- components/
|   +-- screens/
|   +-- theme/
+-- utils/
    +-- ImageUtils.kt
    +-- StickerContentProvider.kt
    +-- StickerStorage.kt
    +-- WhatsAppUtil.kt
    +-- ZipUtils.kt
```

Key areas:

- `MainActivity.kt` configures Compose navigation, image loading, and top-level app actions.
- `StickerViewModel.kt` coordinates UI state with storage, import, export, and sticker updates.
- `StickerStorage.kt` stores sticker-pack metadata in app-private storage.
- `ImageUtils.kt` converts selected media into tray icons and sticker WebP files.
- `StickerContentProvider.kt` exposes pack metadata and sticker files to WhatsApp.
- `ZipUtils.kt` handles pack import and export.

## Sticker-Pack Flow

1. The user creates a pack with a name, publisher, tray image, and at least three stickers.
2. Selected images are converted into app-private WebP files.
3. Pack metadata is persisted locally as JSON.
4. The app exposes pack metadata and sticker files through `StickerContentProvider`.
5. When the user adds a pack to WhatsApp, the app launches the appropriate WhatsApp sticker-pack intent.

## Import and Export Format

Exported packs are ZIP files containing:

- `pack.json` with serialized sticker-pack metadata.
- The tray image file.
- Each sticker image file referenced by the pack metadata.

Imported ZIP files must include a valid `pack.json` and the referenced image files.