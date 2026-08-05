# Plex

Android client for [music.plexs.me](https://music.plexs.me).

Kotlin + Jetpack Compose. Streams directly from YouTube, no ads, background
playback. Sign in with email + OTP or Google.

## Build

Open the project in Android Studio and run, or from the terminal:

    ./gradlew assembleRelease

No keys are needed at build time. Runtime config is fetched from the API on
first launch.

## Releases

A signed APK is attached to every tag. CI builds and signs it automatically.

## License

TBD.