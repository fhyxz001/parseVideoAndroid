# 视频解析助手 - Android 原生版

将 demo（uni-app）项目 1:1 移植为 Android 原生 App，使用 **Kotlin + Jetpack Compose**。

## 功能对照

| demo 功能 | Android 实现 |
|-----------|-------------|
| 粘贴链接 | ClipboardManager 读取剪贴板 |
| 解析视频 | OkHttp GET `/video/share/url/parse` |
| 封面 + 标题 + 作者 | Coil AsyncImage + Compose UI |
| 下载到 DCIM | MediaStore (Android 10+) / File 直写 |
| 下载进度条 | 协程流式读取 + 动画进度条 |
| 复制长链 | ClipboardManager |
| 获取短链 | OkHttp POST `/api/new` |
| 系统分享 | Intent.ACTION_SEND |

## 构建要求

- JDK 17+
- Android Studio Hedgehog 或更新版本
- Android SDK 34
- 设备/模拟器 Android 7.0+（minSdk 24）

## 快速开始

```bash
# 用 Android Studio 打开根目录，同步 Gradle 后直接运行即可
```

## 项目结构

```
app/src/main/java/com/videoparser/app/
├── MainActivity.kt      # Compose UI 全部页面
├── MainViewModel.kt     # 状态管理
├── VideoApi.kt          # 网络层（解析 + 短链）
└── VideoDownloader.kt   # 下载到相册
```

## 接口地址

与 demo 完全一致，无需修改：

- 解析：`http://39.101.129.76:9999/video/share/url/parse?url=<encoded>`
- 短链：`http://39.101.129.76:4567/api/new`
