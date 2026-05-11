# LiuYin - Bilibili Audio Player

An Android app that parses Bilibili video share links and plays the audio stream.

## Features

- Parse Bilibili BV links to extract audio streams
- Play audio via ExoPlayer with CDN failover support
- Receive Bilibili share intents from other apps
- Background playback via MediaSessionService
- Clean Material3 UI with Jetpack Compose

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **DI:** Hilt + KSP
- **Player:** Media3 ExoPlayer
- **Network:** Retrofit + OkHttp + Gson
- **Image Loading:** Coil 3

## Architecture

The app follows a clean architecture with a parser plugin system:

```
Share Intent → ParserPlugin → AudioInfo → LiuYinPlayer → ExoPlayer
```

- **Parser Plugin Interface:** Extensible design for different video platform parsers
- **WBI Authentication:** Automatic request signing via OkHttp interceptor
- **CDN Failover:** Automatic fallback to backup audio URLs on playback error

## License

MIT
