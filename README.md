# 视频解析助手（Android 原生版）

基于 **Kotlin + Jetpack Compose** 的短视频解析下载工具，可粘贴短视频分享链接，解析出无水印视频、封面、标题与作者信息，并将视频/封面保存到系统相册。

## 功能总览

| 功能 | 说明 |
|------|------|
| 粘贴并解析 | 读取剪贴板链接，一键解析视频信息 |
| 视频信息展示 | 封面、标题、作者 |
| 视频文件大小 | 解析后自动获取，下载前即可查看 |
| 下载视频 | 保存到系统相册（DCIM），显示实时进度 |
| 下载进度 | 百分比 + 已下载/总大小 + 动画进度条 |
| 下载封面 | 将封面图片保存到相册（Pictures） |
| 复制标题 / 作者 | 点击标题或作者名一键复制 |
| 自定义服务器 | 支持修改解析服务器地址，可恢复默认 |

## 界面

- iOS 风格毛玻璃卡片设计
- 浅色主题，状态栏透明
- 响应式布局，适配手机 / 平板 / 大屏设备

## 环境要求

- JDK 17+
- Android Studio Hedgehog 或更新版本
- Android SDK 34
- 设备 / 模拟器 Android 7.0+（minSdk 24）

## 快速开始

```bash
# 1. 用 Android Studio 打开项目根目录
# 2. 等待 Gradle 同步完成
# 3. 连接设备或启动模拟器，点击 Run 直接运行
```

> **注意**
> 本仓库未提交 `gradle-wrapper.jar`，直接运行 `gradlew` 会失败。
> - 推荐使用 Android Studio 自动处理 Gradle 环境
> - CI 中已通过 `gradle/actions/setup-gradle` 自动安装 Gradle 8.9
> - 如需本地命令行构建，请先自行补齐 `gradle-wrapper.jar` 或安装 Gradle 8.9

## 项目结构

```
app/src/main/java/com/videoparser/app/
├── MainActivity.kt      # Compose UI：主界面、卡片、按钮、进度条、设置弹窗
├── MainViewModel.kt     # 状态管理：解析、下载、文件大小、复制事件
├── VideoApi.kt          # 网络层：解析接口、OkHttp 客户端
├── VideoDownloader.kt   # 下载器：视频/封面保存、进度与文件大小获取
└── SettingsStore.kt     # SharedPreferences：服务器地址持久化
```

## 使用说明

1. 复制任意短视频分享链接（如抖音、快手等）
2. 打开 App，点击 **粘贴并解析**
3. 解析成功后展示封面、标题、作者、视频大小
4. 点击 **下载视频**，查看实时进度
5. 点击封面，可保存封面图；点击标题 / 作者名可复制对应文字

## 服务器设置

默认解析服务器地址硬编码于 `VideoApi.kt`：

```
http://122.51.115.245:8888
```

如需更换服务器：

1. 点击首页右上角 **设置**
2. 输入新的服务器地址（需以 `http://` 或 `https://` 开头）
3. 点击 **保存**，地址将持久化到本地
4. 点击 **恢复默认** 可重置为内置默认地址

## 权限说明

| 权限 | 用途 |
|------|------|
| `INTERNET` | 访问解析服务器、下载视频与封面 |
| `ACCESS_NETWORK_STATE` | 网络可用性判断（系统常规声明） |
| `ACCESS_WIFI_STATE` | 网络状态（系统常规声明） |
| `WRITE_EXTERNAL_STORAGE` | Android 9 及以下直写 DCIM / Pictures（`maxSdkVersion=28`） |

> Android 10+ 使用 `MediaStore` 写入，无需存储权限。

## 构建

### 本地构建

```bash
gradle :app:assembleDebug
```

需要本机已安装 Gradle 8.9 或补齐 `gradle-wrapper.jar` 后使用：

```bash
./gradlew :app:assembleDebug
```

### CI 构建

项目内置 GitHub Actions 工作流 `.github/workflows/build-release.yml`：

- **普通 push / PR**：构建 Debug APK 并上传 artifact
- **tag（v1.2.3）**：解码 Keystore，构建签名 Release APK，自动创建 GitHub Release

## 已知限制

- 下载暂不支持取消
- 下载暂不支持后台 / 断点续传
- 无下载历史记录
- 暂不支持深色模式
- 暂无 App 内视频预览播放
- 所有 UI 文案为硬编码中文，暂未做 i18n

## 版本

- 当前默认版本：`1.0.2`（versionCode `102`）
- tag 构建时由 CI 自动解析 `v1.2.3` → versionName `1.2.3`，versionCode `10203`
