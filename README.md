# idear 泡泡 v1.0

把脑子里暂时不想处理、又不想忘记的想法，变成一颗颗泡泡。想得越多的事，泡泡越大；双击泡泡可以走进去，看里面的小想法。

数据只保存在当前设备本地，没有服务器和账号。换设备时，用「备份 ⇅」导出 `.json`，再到另一台设备上恢复。

## 功能

- 新建泡泡，编辑名字、内容、颜色、大小方式、所在位置
- 泡泡大小跟着里面小想法的数量长大；屏幕拥挤时一起缩小
- 拖动泡泡，松手后重叠的泡泡慢慢散开
- 双击走进泡泡，用面包屑返回上一层
- 搜索：高亮命中的泡泡（包括里面的小想法命中）
- 吹掉泡泡（标记完成）、删除泡泡、查看已完成的小泡泡
- 备份导出 / 从备份恢复

功能来源与逐项分析见 [analysis.md](analysis.md)。

## 目录结构

```
.
├── web/
│   ├── idear泡泡.html   # 网页端，也是各端共用的核心（单文件，零依赖）
│   └── build.sh         # 网页端构建：检查脚本语法，输出 web/dist/index.html
├── android/             # Android 端：Gradle 工程，WebView 外壳（Java，无第三方库）
│   └── app/src/main/java/com/idear/bubble/MainActivity.java
├── macos/               # Mac 端：Swift Package，AppKit + WKWebView 外壳（无第三方库）
│   ├── Sources/IdearBubble/
│   ├── Info.plist
│   └── build.sh         # 编译并组装 macos/build/idear泡泡.app
├── analysis.md          # 需求视频的逐帧分析
└── README.md
```

## 网页端

需要：任意现代浏览器；构建时需要 Node.js（只用来检查脚本语法）。

**直接运行**：源文件就是成品，双击或用命令打开：

```bash
open 'web/idear泡泡.html'
```

**构建**（输出可部署到任意静态托管的 `web/dist/index.html`）：

```bash
./web/build.sh
```

**本地预览构建结果**：

```bash
python3 -m http.server 8765 --directory web/dist
```

然后访问 http://localhost:8765/ 。注意：`file://` 和 `http://localhost` 在浏览器里是不同的来源，各自保存各自的数据。

## Android 端

一个原生 WebView 外壳：构建时把 `web/idear泡泡.html` 打包进 APK，并补上导出备份（系统「保存到」界面）、从备份恢复（系统文件选择器）和确认框。

需要：

- JDK 17 或更高版本
- Android SDK，含 `platforms;android-36`；用环境变量 `ANDROID_HOME` 指向 SDK 目录，或在 `android/local.properties` 里写 `sdk.dir=/你的/sdk/路径`
- 首次构建会通过 Gradle Wrapper 自动下载 Gradle 9.9.1 和 Android Gradle Plugin 9.5.1，需要联网

**构建调试版 APK**：

```bash
cd android
./gradlew assembleDebug
```

产物在 `android/app/build/outputs/apk/debug/app-debug.apk`（约 60 KB），最低支持 Android 7.0（API 24），需要系统 WebView 87 及以上。

**安装到已连接的设备或模拟器并启动**：

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb shell am start -n com.idear.bubble/.MainActivity
```

也可以用 Android Studio 打开 `android/` 目录直接运行。正式发布需要自行配置签名，仓库里不包含任何签名密钥。

## Mac 端

一个原生 AppKit 窗口，里面的 WKWebView 加载 `web/idear泡泡.html`，并补上提示与确认框、导出备份（系统保存面板）、从备份恢复（系统打开面板），以及「编辑」菜单（⌘C / ⌘V / ⌘Z 等）。

需要：macOS 13 或更高版本，Xcode 或 Command Line Tools（Swift 6）。

**构建**（输出 `macos/build/idear泡泡.app`，使用本机临时签名）：

```bash
./macos/build.sh
```

**运行**：

```bash
open 'macos/build/idear泡泡.app'
```

数据保存在 `~/Library/WebKit/com.idear.bubble/`。要分发给其他电脑，需要换成自己的 Apple 开发者证书签名并公证。
