# Plex

Native Android client for [music.plexs.me](https://music.plexs.me).

Built in Kotlin with Jetpack Compose. Streams directly from YouTube's own
streams, no ads, background playback, dark by design.

## Features

- Email / OTP and Google sign-in
- Full search and catalog browsing
- Background playback with a foreground media service
- Update checks on launch
- Dark theme only, because it is a music app

## Building

Open the project in Android Studio, or from the terminal:

```sh
./gradlew assembleRelease
```

The app talks to a private API. It needs no keys of its own to build — all
runtime config is fetched on first launch. A signed APK is published with each
[release](https://github.com/Plexsq/plex-mobile/releases).

## License

TBD.
