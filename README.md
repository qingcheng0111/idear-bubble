# idear 泡泡 v1.0

把脑子里暂时不想处理、又不想忘记的想法，变成一颗颗泡泡。想得越多的事，泡泡越大；双击泡泡可以走进去，看里面的小想法。

数据只保存在当前设备本地，没有服务器和账号。换设备时，用「备份 ⇅」导出 `.json`，再到另一台设备上恢复。

## 下载安装

到 [Releases](https://github.com/qingcheng0111/idear-bubble/releases/latest) 下载对应的安装包：

| 平台 | 文件 | 安装 |
|---|---|---|
| Android 7.0 及以上 | `idear-bubble-1.0-android.apk` | 在手机上打开 APK，按提示允许「安装未知来源应用」 |
| macOS 13 及以上（Apple 芯片 / Intel） | `idear-bubble-1.0-macos.dmg` | 打开 dmg，把「idear泡泡」拖进「应用程序」 |
| 浏览器 | `idear-bubble-1.0-web.html` | 下载后双击，用浏览器打开 |

Mac 版没有经过 Apple 公证，第一次打开会被系统拦下。先双击打开一次，然后到「系统设置 → 隐私与安全性」，点「仍要打开」。也可以在终端执行：

```bash
xattr -dr com.apple.quarantine '/Applications/idear泡泡.app'
```

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
│   └── build.sh         # 编译通用版，组装 idear泡泡.app 并打包 .dmg
├── analysis.md          # 需求视频的逐帧分析
└── README.md
```

三端共用同一份网页核心 `web/idear泡泡.html`：改功能只改这一个文件，再按下面的命令重新构建各端即可。Android 和 Mac 外壳只负责加载它，并补上网页在内嵌浏览器里缺少的原生能力。

下面的命令都在仓库根目录执行。

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
./android/gradlew -p android assembleDebug
```

产物在 `android/app/build/outputs/apk/debug/app-debug.apk`（约 60 KB），最低支持 Android 7.0（API 24），需要系统 WebView 87 及以上。

**安装到已连接的设备或模拟器并启动**：

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb shell am start -n com.idear.bubble/.MainActivity
```

也可以用 Android Studio 打开 `android/` 目录直接运行。

**构建签名的正式版**：仓库里不保存签名文件和密码，构建时通过环境变量传入。`IDEAR_KEY_ALIAS` 可省略，默认是 `idear-bubble`；不设置这些变量时，产出的是未签名的正式包。下面的示例里，签名文件放在 `~/.android/`，密码存在 macOS 钥匙串的 `idear-bubble-release` 条目中：

```bash
IDEAR_KEYSTORE="$HOME/.android/idear-bubble-release.jks" \
IDEAR_KEYSTORE_PASSWORD="$(security find-generic-password -s idear-bubble-release -w)" \
./android/gradlew -p android assembleRelease
```

产物在 `android/app/build/outputs/apk/release/app-release.apk`。之后发布的每个版本都必须用同一个签名文件，用户才能直接覆盖升级，请妥善备份它。

## Mac 端

一个原生 AppKit 窗口，里面的 WKWebView 加载 `web/idear泡泡.html`，并补上提示与确认框、导出备份（系统保存面板）、从备份恢复（系统打开面板），以及「编辑」菜单（⌘C / ⌘V / ⌘Z 等）。

需要：macOS 13 或更高版本，Xcode 或 Command Line Tools（Swift 6）。

**构建**（Apple 芯片 + Intel 通用版，使用本机临时签名；同时输出应用 `macos/build/idear泡泡.app` 和安装镜像 `macos/build/idear-bubble-1.0-macos.dmg`）：

```bash
./macos/build.sh
```

**运行**：

```bash
open 'macos/build/idear泡泡.app'
```

数据保存在 `~/Library/WebKit/com.idear.bubble/`。要分发给其他电脑，需要换成自己的 Apple 开发者证书签名并公证。
