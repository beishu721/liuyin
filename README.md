# LiuYin - Bilibili 音频播放器

一个解析 Bilibili 视频分享链接并播放音频流的 Android 应用。

## 功能特点

- 解析 Bilibili BV 链接，提取音频流
- 使用 ExoPlayer 播放音频，支持 CDN 故障切换
- 接收来自其他应用的 Bilibili 分享意图
- 通过 MediaSessionService 支持后台播放
- 使用 Jetpack Compose + Material3 构建的简洁界面

## 技术栈

- **语言:** Kotlin
- **UI:** Jetpack Compose + Material3
- **依赖注入:** Hilt + KSP
- **播放器:** Media3 ExoPlayer
- **网络:** Retrofit + OkHttp + Gson
- **图片加载:** Coil 3

## 架构

应用采用清晰的架构设计，并实现了解析器插件系统：

```
分享意图 → 解析器插件 → AudioInfo → LiuYinPlayer → ExoPlayer
```

- **解析器插件接口：** 可扩展的设计，支持为不同视频平台添加解析器
- **WBI 认证：** 通过 OkHttp 拦截器自动对请求进行签名
- **CDN 故障切换：** 当播放出错时自动回退到备用音频 URL

## 开源协议

MIT
