# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build debug APK
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot" ./gradlew assembleDebug
```

- Java 21 required (AGP 9 constraint). JDK path is set in `gradle.properties`.
- AGP 9.1.1, Kotlin 2.2.10, Gradle 9.3.1.
- Hilt uses KSP (not kapt). `android.disallowKotlinSourceSets=false` is set for AGP 9 compatibility.

## Architecture

```
app/src/main/java/com/liuyin/app/
├── LiuYinApp.kt           # Hilt Application, WBI key init, notification channel
├── MainActivity.kt         # @AndroidEntryPoint, Compose host
├── di/
│   └── AppModule.kt        # DataStore provider (IO-scoped)
├── data/
│   ├── model/Models.kt     # API response DTOs + domain models (AudioInfo, VideoInfo)
│   └── repository/
│       └── AudioRepository.kt  # Facade over BiliParserV1
├── network/
│   ├── api/BilibiliApi.kt       # Retrofit interface (view, playurl, wbi/index)
│   ├── BilibiliAuth.kt          # WBI signature (MD5, mix_key)
│   ├── BilibiliInterceptor.kt   # OkHttp interceptor: UA/Referer + WBI signing
│   ├── NetworkModule.kt         # Hilt module: Gson, OkHttpClient, Retrofit
│   └── parser/
│       ├── ParserPlugin.kt      # Interface (parse, getVideoInfo)
│       ├── BiliParserV1.kt      # Built-in impl: view → cid → playurl → best audio
│       └── ParserConfig.kt      # Remote config model (future hot-update)
├── player/
│   ├── LiuYinPlayer.kt     # ExoPlayer singleton, CDN backup failover, StateFlow state
│   └── PlaybackService.kt  # MediaSessionService, @AndroidEntryPoint
└── ui/                     # Compose screens (skeletons)
    ├── main/MainScreen.kt
    ├── player/PlayerScreen.kt
    ├── playlist/PlaylistScreen.kt
    └── settings/SettingsScreen.kt
```

## Data Flow (Playback)

```
BV input → BiliParserV1.parse(bvid)
  → api.getVideoInfo(bvid)          # title, cid, cover
  → api.getPlayUrl(bvid, cid)       # DASH audio streams
  → maxBy { bandwidth }             # pick highest quality
  → AudioInfo(baseUrl, backupUrls)
  → LiuYinPlayer.play(audioInfo)
  → ExoPlayer + MediaSessionService
```

## Key Design Decisions

- **WBI signing**: `BilibiliAuth` caches `mix_key = subKey[:4] + imgKey[:4]`; `BilibiliInterceptor` auto-signs all requests except `wbi/index` endpoint. Keys fetched on app startup in `LiuYinApp.onCreate()`, with fallback in `BiliParserV1.ensureKeys()`.
- **CDN failover**: `LiuYinPlayer` listens for `Player.Listener.onPlayerError`, iterates through `backupUrls` on failure.
- **Parser plugin**: `ParserPlugin` interface with `BiliParserV1` as built-in impl; `ParserConfig` models remote JSON for future URL/header overrides.
- **Hilt**: All singletons use `@Inject @Singleton`. Module classes for external libs (Retrofit, OkHttp). No `@Module` needed for project classes.
- **State exposure**: `LiuYinPlayer` exposes `playbackState`, `position`, `duration`, `currentAudio` as `StateFlow` for Compose observation.

## Dependencies (libs.versions.toml)

| Category | Lib |
|----------|-----|
| DI | Hilt 2.59.2 + KSP |
| Player | Media3 ExoPlayer 1.6.1 + Session |
| Network | Retrofit 2.11 + OkHttp 4.12 + Gson |
| Image | Coil 3.1 (compose + okhttp) |
| DataStore | Preferences 1.1.3 |
| Logging | Timber 5.0.1 |

## Convenient SharedPreferences / DataStore

Settings are stored via DataStore Preferences in `di/AppModule.kt`. The instance is created with `Dispatchers.IO` scope. Usage:

```kotlin
@Inject lateinit var dataStore: DataStore<Preferences>
```

## Share Intent

`MainActivity` handles `ACTION_SEND` with `text/plain` MIME type (B站链接分享). Intent filter is in `AndroidManifest.xml`.