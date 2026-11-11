//go:build windows

// idear 泡泡 Windows 端：用系统自带的 WebView2（Edge 内核）加载三端共用的网页核心。
// 导出备份、从备份恢复、提示与确认框都由 WebView2 自带的下载、文件选择和对话框处理。
package main

import (
	_ "embed"
	"net/url"
	"os"
	"path/filepath"

	webview2 "github.com/jchv/go-webview2"
	"golang.org/x/sys/windows"
)

// 构建时由 build.sh 从 web/idear泡泡.html 复制而来
//
//go:embed assets/index.html
var page []byte

const appTitle = "idear 泡泡"

func main() {
	enableHighDPI()

	// 数据放在 %LOCALAPPDATA%\idear-bubble：网页文件位置固定，本地存储（localStorage）才能每次都读到
	base, err := os.UserCacheDir()
	if err != nil {
		fail("找不到本机的应用数据目录：" + err.Error())
		return
	}
	dir := filepath.Join(base, "idear-bubble")
	pagePath := filepath.Join(dir, "index.html")
	if err := os.MkdirAll(dir, 0o755); err != nil {
		fail("无法创建应用数据目录：" + err.Error())
		return
	}
	if err := os.WriteFile(pagePath, page, 0o644); err != nil {
		fail("无法写入网页文件：" + err.Error())
		return
	}

	w := webview2.NewWithOptions(webview2.WebViewOptions{
		AutoFocus: true,
		DataPath:  filepath.Join(dir, "WebView2"),
		WindowOptions: webview2.WindowOptions{
			Title:  appTitle,
			Width:  1200,
			Height: 780,
			Center: true,
		},
	})
	if w == nil {
		fail("没能启动 Microsoft Edge WebView2。\n\nWindows 11 自带它；Windows 10 请先安装「Microsoft Edge WebView2 运行时」（在微软官网搜索即可），然后重新打开 idear 泡泡。")
		return
	}
	defer w.Destroy()
	w.SetSize(720, 520, webview2.HintMin)
	w.Navigate((&url.URL{Scheme: "file", Path: "/" + filepath.ToSlash(pagePath)}).String())
	w.Run()
}

// 声明支持高分屏，避免在缩放 125%、150% 的屏幕上被系统拉伸发糊（Windows 10 1703 起支持）
func enableHighDPI() {
	proc := windows.NewLazySystemDLL("user32.dll").NewProc("SetProcessDpiAwarenessContext")
	if proc.Find() == nil {
		const perMonitorAwareV2 = ^uintptr(3) // DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2 = (HANDLE)-4
		proc.Call(perMonitorAwareV2)
	}
}

func fail(message string) {
	text, _ := windows.UTF16PtrFromString(message)
	title, _ := windows.UTF16PtrFromString(appTitle)
	windows.MessageBox(0, text, title, windows.MB_OK|windows.MB_ICONERROR)
}
