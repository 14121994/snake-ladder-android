# Snake & Ladder Android App

A simple 2-player Snake & Ladder game built with Kotlin and Jetpack Compose.

## Features
- 10x10 board (1..100) with serpentine numbering
- 2 to 4 players with alternating turns
- Random dice roll (1..6)
- Snake and ladder jumps
- Exact-roll rule to reach 100
- Move history and restart game
- Unit tests for core movement rules

## Open in Android Studio
1. Open Android Studio.
2. Click **Open** and select `snake-ladder-android`.
3. Let Gradle sync complete.
4. Run the `app` configuration on an emulator or Android device.

## Build from command line
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

If Android SDK is not configured yet, create `local.properties` with:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

## Main files
- `app/src/main/java/com/example/snakeladder/MainActivity.kt`
- `app/src/main/java/com/example/snakeladder/ui/theme/Theme.kt`
- `app/src/main/AndroidManifest.xml`
