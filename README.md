# 视频解析助手（Android 原生版）

基于 **Kotlin + Jetpack Compose** 的短视频解析下载工具，可粘贴短视频分享链接，解析出无水印视频、封面、标题与作者信息，并将视频/封面保存到系统相册。支持**视频剪辑下载**：只下载视频中你想要的那一段，省流量省时间。

## 功能总览

| 功能 | 说明 |
|------|------|
| 粘贴并解析 | 读取剪贴板链接，一键解析视频信息 |
| 视频信息展示 | 封面、标题、作者 |
| 视频文件大小 / 时长 | 解析后自动获取，下载前即可查看 |
| 下载视频 | 保存到系统相册（DCIM），显示实时进度 |
| **剪辑下载** | 双滑块时间轴选择起止时间，**只下载所选片段**（无损流复制，不转码） |
| 下载进度 | 百分比 + 已下载/总大小 + 动画进度条 |
| 下载封面 | 将封面图片保存到相册（Pictures） |
| 复制标题 / 作者 | 点击标题或作者名一键复制 |
| 自定义服务器 | 支持修改解析服务器地址，可恢复默认 |

## 剪辑下载

解析成功后点击 **剪辑下载**，在底部弹窗中像剪辑 App 一样操作：

- **时间轴缩略图**：自动抽取 8 个关键帧缩略图拼成时间轴，直观定位
- **双滑块选区**：拖动把手选起止时间，点击时间轴微调，支持 ±1s 步进
- **关键帧对齐预览**：起点自动吸附到最近关键帧，选好后会提示实际剪出的区间
- **预计大小**：按所选时长估算下载体积，确认前心里有数
- **省流量**：利用 HTTP Range 只下载所选时间段的字节 + 视频元数据（1 小时视频只要其中 10 分钟 ≈ 只下 1/6 的流量）
- **无损输出**：流复制不转码，画质无损失，速度快
- **可取消**：剪辑过程中可随时取消；关闭弹窗后仍在后台进行，完成会提示

技术细节（面向开发者）：

- 探测阶段仅拉取文件头部/尾部少量字节，解析 `moov` 中的采样表（stts/stss/stsc/stsz/stco/ctts），建立“时间 → 字节偏移”索引
- 起点吸附关键帧保证可独立解码；重建 `ftyp + moov + mdat`（faststart），裁剪采样表并重定位 chunk 偏移
- 若服务器不支持 Range、或遇到分片 MP4（moof）等无法流剪辑的情况，自动降级为“全量下载 + 系统 `MediaExtractor`/`MediaMuxer` 剪切”
- 剪辑核心为纯 Kotlin 实现且带单元测试（合成 MP4 数学校验 + 真实 ffmpeg 生成文件的 ffprobe/全量解码端到端验证）

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
├── MainActivity.kt      # Compose UI：主界面、卡片、按钮、进度条、剪辑弹窗、设置弹窗
├── MainViewModel.kt     # 状态管理：解析、下载、剪辑、缩略图、复制事件
├── VideoApi.kt          # 网络层：解析接口、OkHttp 客户端
├── VideoDownloader.kt   # 下载器：视频/封面保存、进度与文件大小获取
├── VideoClipper.kt      # 剪辑编排：moov 探测、Range 片段下载、降级剪切
├── Mp4Boxes.kt          # MP4 元数据解析（纯 Kotlin）：box 遍历、采样表、moov 定位
├── Mp4Clipper.kt        # 剪辑核心（纯 Kotlin）：时间规划、容器重建、box 写入
└── SettingsStore.kt     # SharedPreferences：服务器地址持久化
```

单元测试位于 `app/src/test/`，覆盖剪辑核心的解析/规划/重建数学正确性；本机装有 ffmpeg 时会额外做真实文件的 ffprobe + 全量解码端到端验证（无 ffmpeg 自动跳过）。

## 使用说明

1. 复制任意短视频分享链接（如抖音、快手等）
2. 打开 App，点击 **粘贴并解析**
3. 解析成功后展示封面、标题、作者、视频大小与时长
4. 点击 **下载视频**，查看实时进度
5. 点击 **剪辑下载**，在时间轴上选好起止时间后点击 **剪辑并下载**，只下载所选片段
6. 点击封面，可保存封面图；点击标题 / 作者名可复制对应文字

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

- 普通下载暂不支持取消（剪辑下载已支持取消）
- 下载暂不支持后台 / 断点续传
- 剪切点按关键帧对齐，起点可能有几秒偏差（无损流复制的固有特性）
- 无下载历史记录
- 暂不支持深色模式
- 暂无 App 内视频预览播放
- 所有 UI 文案为硬编码中文，暂未做 i18n

## 版本

- 当前默认版本：`1.0.2`（versionCode `102`）
- tag 构建时由 CI 自动解析 `v1.2.3` → versionName `1.2.3`，versionCode `10203`
