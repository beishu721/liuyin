# 留音 (LiuYin) — Bilibili 音频播放器

留音是一款开源的 Android 应用，输入 B站视频的 BV 号或分享链接，即可提取音频流进行播放。支持后台播放、播放历史记录、封面和音频自动下载。

---

## 目录

- [功能特点](#功能特点)
- [应用截图](#应用截图)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [数据流](#数据流)
- [构建与运行](#构建与运行)
- [使用指南](#使用指南)
- [设计决策](#设计决策)
- [常见问题](#常见问题)
- [开源协议](#开源协议)

---

## 功能特点

- **BV 号解析**：输入 BV 号或完整的 B站视频链接，自动提取最高质量的 DASH 音频流
- **音频播放**：基于 Media3 ExoPlayer，支持播放/暂停、进度拖动、时间显示
- **CDN 故障切换**：播放出错时自动切换备用 CDN 地址，保证播放稳定性
- **后台播放**：通过 MediaSessionService 支持锁屏控制和通知栏控制
- **迷你播放器**：底部常驻迷你播放条，显示封面和标题，可快速控制播放/暂停
- **全屏播放器**：点击迷你播放条展开全屏界面，显示大封面和进度条
- **播放历史**：自动记录播放历史，支持回放和删除，数据持久化存储（最多 50 条）
- **封面与音频下载**：播放成功后自动将封面图片和音频文件保存到本地
- **分享意图接收**：支持从浏览器或其他 App 分享 B站链接，自动提取 BV号并播放
- **WBI 签名**：自动获取并缓存 WBI 密钥，对 API 请求进行签名，无需用户登录

---

## 应用截图

> 替换为实际截图

| 首页 | 播放器 | 播放列表 | 设置 |
|------|--------|----------|------|
| ![首页](screenshots/home.png) | ![播放器](screenshots/player.png) | ![列表](screenshots/playlist.png) | ![设置](screenshots/settings.png) |

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material3 | BOM 2026.02.01 |
| 导航 | Navigation Compose | 2.8.9 |
| 依赖注入 | Hilt + KSP | 2.59.2 |
| 播放器 | Media3 ExoPlayer + MediaSession | 1.6.1 |
| 网络 | Retrofit + OkHttp + Gson | 2.11 / 4.12 / 2.11 |
| 图片加载 | Coil 3 (Compose + OkHttp) | 3.1.0 |
| 持久化 | DataStore Preferences | 1.1.3 |
| 日志 | Timber | 5.0.1 |
| 构建 | AGP + Gradle | 9.1.1 / 9.3.1 |

---

## 项目结构

```
app/src/main/java/com/liuyin/app/
├── LiuYinApp.kt                  # Application：WBI 密钥初始化、通知渠道
├── MainActivity.kt               # 主 Activity：Scaffold + NavHost + 播放器覆盖层
│
├── di/
│   └── AppModule.kt              # Hilt 模块：DataStore 提供
│
├── data/
│   ├── model/Models.kt           # API 响应 DTO + 领域模型（AudioInfo, VideoInfo）
│   ├── repository/
│   │   ├── AudioRepository.kt    # 音频仓库：封装 BiliParserV1
│   │   └── PlaylistRepository.kt # 播放列表仓库：DataStore + Gson 持久化
│   └── DownloadUtil.kt           # 下载工具：封面图片 + 音频文件
│
├── network/
│   ├── api/BilibiliApi.kt        # Retrofit 接口（view, playurl, nav）
│   ├── BilibiliAuth.kt           # WBI 签名：MD5 加密、mix_key 管理
│   ├── BilibiliInterceptor.kt    # OkHttp 拦截器：UA/Referer/Cookie + WBI 签名
│   ├── NetworkModule.kt          # Hilt 模块：Gson, OkHttpClient, Retrofit
│   └── parser/
│       ├── ParserPlugin.kt       # 解析器接口（可扩展）
│       ├── BiliParserV1.kt       # B站解析器：view → cid → playurl → 最佳音频
│       └── ParserConfig.kt       # 远程配置模型（未来热更新用）
│
├── player/
│   ├── LiuYinPlayer.kt           # ExoPlayer 单例：StateFlow 状态、CDN 备份切换
│   └── PlaybackService.kt        # MediaSessionService：后台播放支持
│
└── ui/
    ├── theme/
    │   ├── Color.kt              # 调色板
    │   ├── Type.kt               # 字体排版
    │   └── Theme.kt              # 主题（支持动态取色）
    ├── navigation/
    │   └── Navigation.kt         # BottomNavItem：首页/播放列表/设置
    ├── components/
    │   └── MiniPlayer.kt         # 迷你播放器组件
    ├── main/
    │   ├── MainScreen.kt         # 首页：BV 输入 + 播放按钮 + 错误/下载提示
    │   └── MainViewModel.kt      # 主 ViewModel：输入/解析/下载状态管理
    ├── player/
    │   └── PlayerScreen.kt       # 全屏播放器：封面 + 进度条 + 播放控制
    ├── playlist/
    │   ├── PlaylistScreen.kt     # 播放列表页：历史记录展示
    │   └── PlaylistViewModel.kt  # 列表 ViewModel：增删改查
    └── settings/
        └── SettingsScreen.kt     # 设置页：关于信息
```

---

## 数据流

### 播放流程

```
用户输入 BV号
    │
    ▼
MainViewModel.parseAndPlay()
    │
    ├─→ BiliParserV1.ensureKeys()
    │       └─→ GET /x/web-interface/nav  ← 获取 WBI 密钥
    │
    ├─→ BiliParserV1.parse(bvid)
    │       │
    │       ├─→ GET /x/web-interface/view?bvid=...        ← 获取视频信息（标题、cid、封面）
    │       │
    │       ├─→ GET /x/player/playurl?bvid=...&cid=...    ← 获取 DASH 音频流
    │       │
    │       └─→ maxBy { bandwidth }  ← 选取最高码率音频
    │
    ├─→ LiuYinPlayer.play(audioInfo)  ← 开始播放
    │
    ├─→ PlaylistRepository.addItem()  ← 加入播放历史
    │
    └─→ DownloadUtil.downloadCover() + downloadAudio()  ← 后台下载
```

### WBI 签名流程

```
App 启动
    │
    └─→ LiuYinApp.onCreate()
            └─→ GET /x/web-interface/nav
                    └─→ 提取 img_key + sub_key
                            └─→ mix_key = sub_key[:4] + img_key[:4]

每次 API 请求
    │
    └─→ BilibiliInterceptor.intercept()
            ├─→ 添加 User-Agent, Referer, Cookie 等请求头
            ├─→ 排除 nav 和 wbi/index（这两个接口不需要签名）
            └─→ 对其余接口：
                    参数排序 → 拼接 → 追加 mix_key → MD5 → w_rid
                    追加 wts（时间戳）+ w_rid 到请求参数
```

### CDN 故障切换

```
ExoPlayer 播放出错
    │
    └─→ LiuYinPlayer.onPlayerError()
            ├─→ 切换至 backupUrls[currentBackupIndex]
            ├─→ player.stop() → setMediaItem() → prepare() → play()
            └─→ 所有备份 URL 均失败 → 播放状态 = ERROR
```

---

## 构建与运行

### 环境要求

- **Android Studio** 最新版（推荐 Meerkat 或更高）
- **JDK 21**（AGP 9 要求）
- **Gradle 9.3.1**（使用项目自带的 Gradle Wrapper，无需手动安装）
- **Android SDK** API 36 及以上
- **最低支持** Android 7.0（API 24）
- **目标版本** Android 16（API 36）

### 克隆项目

```bash
git clone https://github.com/beishu721/liuyin.git
cd liuyin
```

### 构建 Debug APK

**Windows（PowerShell）：**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew assembleDebug
```

**macOS / Linux：**

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到设备

确保手机已开启 USB 调试并连接电脑：

```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew installDebug

# macOS / Linux
JAVA_HOME=/path/to/jdk-21 ./gradlew installDebug
```

### JDK 路径配置

如果不想每次都设置 `JAVA_HOME`，可以在项目根目录的 `gradle.properties` 中修改：

```properties
org.gradle.java.home=C\:\\Program Files\\Eclipse Adoptium\\jdk-21.0.11.10-hotspot
```

---

## 使用指南

### 基本使用

1. 打开「留音」App，进入**首页**
2. 在输入框中粘贴 B站视频链接或 BV 号，例如：
   - `BV1xx411c7mD`
   - `https://www.bilibili.com/video/BV1xx411c7mD`
3. 点击「**播放**」按钮
4. 等待解析完成，音频自动开始播放

### 播放控制

- **迷你播放器**：播放成功后底部出现横条，显示封面和标题
  - 点击右侧按钮 → 播放/暂停
  - 点击横条其他区域 → 展开全屏播放器
- **全屏播放器**：
  - 拖动进度条 → 快进/快退
  - 点击中央按钮 → 播放/暂停
  - 点击左上角箭头 → 收起播放器

### 播放历史

- 点击底部「**播放列表**」Tab 查看历史记录
- 点击任意条目 → 重新播放
- 点击垃圾桶图标 → 从历史中删除

### 分享链接播放

在浏览器或其他 App 中分享 B站视频链接到「留音」，App 会自动提取 BV 号并开始播放。

### 本地文件

下载的封面和音频保存在：

| 内容 | 路径 |
|------|------|
| 封面图片 | `手机存储/Android/data/com.liuyin.app/files/Pictures/LiuYin/` |
| 音频文件 | `手机存储/Android/data/com.liuyin.app/files/Music/LiuYin/` |

---

## 设计决策

### 解析器插件系统

定义了 `ParserPlugin` 接口，内置 `BiliParserV1` 作为 B站解析器实现。未来可通过 `ParserConfig` 加载远程配置，实现解析规则的热更新，或为其他平台（如 YouTube、Niconico）添加解析器。

```kotlin
interface ParserPlugin {
    suspend fun parse(bvid: String): AudioInfo
    suspend fun getVideoInfo(bvid: String): VideoInfo
}
```

### WBI 签名

B站从 2023 年开始要求所有 API 请求携带 WBI 签名（`w_rid` + `wts` 参数）。本项目通过以下方式实现：

1. App 启动时从 `nav` 接口获取 `img_key` 和 `sub_key`
2. 拼接为 `mix_key`，缓存在内存中
3. `BilibiliInterceptor` 拦截所有请求（`nav` 除外），自动计算签名并附加到 URL
4. 若获取密钥失败，会在首次播放时重试

### CDN 故障切换

B站的 DASH 音频流提供多个 CDN 备份 URL。`LiuYinPlayer` 在播放出错时自动遍历 `backupUrls` 列表，切换至下一个可用地址，无需用户手动操作。

### 状态管理

- `LiuYinPlayer` 通过 `StateFlow` 暴露播放状态（播放中/暂停/加载/错误）、当前音频信息、播放进度和总时长
- Compose 组件通过 `collectAsStateWithLifecycle()` 观察这些 Flow，实现响应式 UI 更新
- `MainViewModel` 管理输入/加载/错误/下载提示状态
- `PlaylistViewModel` 管理播放列表的增删改查

### 持久化

播放历史使用 DataStore Preferences 存储，以 JSON 格式序列化。最多保留 50 条记录，按 `audioUrl` 去重，最新播放的排在最前。

---

## 常见问题

### Q: 显示"请输入有效的 BV 号"？

请确保输入的是完整的 B站视频链接或 BV 号。支持的格式：
- `BV1xx411c7mD`（标准 BV 号）
- `https://www.bilibili.com/video/BV1xx411c7mD`（完整链接）
- `https://b23.tv/xxxxx`（短链接暂不支持，请使用完整链接）

### Q: 显示"该视频没有可用的音频流"？

部分 B站视频（如转载视频、番剧）可能不提供 DASH 音频流，或需要登录才能获取。请尝试其他视频。

### Q: 播放卡顿或失败？

App 会自动切换 CDN 备份地址。如果所有地址均失败，请检查网络连接，或尝试切换 WiFi/移动数据。

### Q: 下载的音频在哪里？

在文件管理器中进入：`Android/data/com.liuyin.app/files/Music/LiuYin/`

### Q: 是否需要登录 B站账号？

不需要。App 通过 WBI 签名访问公开 API，无需登录。

### Q: 支持哪些 Android 版本？

Android 7.0（API 24）及以上。

---

## 开源协议

MIT License

Copyright (c) 2025 beishu721

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
