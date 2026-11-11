import AppKit
import UniformTypeIdentifiers
import WebKit

/// 加载应用包里的 index.html（各端共用的网页核心），
/// 并补上网页在 WKWebView 里缺少的原生能力：提示与确认框、打开文件、保存文件。
@MainActor
final class WebController: NSObject, WKUIDelegate, WKNavigationDelegate, WKScriptMessageHandler {
    // 用固定的 https 来源加载本地网页，localStorage 才能稳定保存
    private static let origin = URL(string: "https://idear-bubble.local/")!

    let webView: WKWebView

    override init() {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        webView = WKWebView(frame: .zero, configuration: configuration)
        super.init()

        configuration.userContentController.add(self, name: "saveTextFile")
        webView.uiDelegate = self
        webView.navigationDelegate = self
        loadPage()
    }

    private func loadPage() {
        guard let url = Bundle.main.url(forResource: "index", withExtension: "html"),
              let html = try? String(contentsOf: url, encoding: .utf8) else {
            webView.loadHTMLString("<p style=\"font: 15px -apple-system; padding: 24px\">应用包里缺少 index.html，请用 macos/build.sh 重新构建。</p>", baseURL: nil)
            return
        }
        webView.loadHTMLString(html, baseURL: Self.origin)
    }

    // 只显示应用自己的页面；比如把文件拖进窗口时，不跳转去打开那个文件
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction) async -> WKNavigationActionPolicy {
        navigationAction.request.url?.host == Self.origin.host ? .allow : .cancel
    }

    // MARK: alert / confirm

    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo) async {
        _ = await showAlert(message, buttons: ["好"])
    }

    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo) async -> Bool {
        await showAlert(message, buttons: ["确定", "取消"]) == .alertFirstButtonReturn
    }

    private func showAlert(_ message: String, buttons: [String]) async -> NSApplication.ModalResponse {
        let alert = NSAlert()
        alert.messageText = message
        buttons.forEach { alert.addButton(withTitle: $0) }
        guard let window = webView.window else { return alert.runModal() }
        return await alert.beginSheetModal(for: window)
    }

    // MARK: 从备份恢复：网页里的 <input type="file">

    func webView(_ webView: WKWebView, runOpenPanelWith parameters: WKOpenPanelParameters,
                 initiatedByFrame frame: WKFrameInfo) async -> [URL]? {
        let panel = NSOpenPanel()
        panel.allowsMultipleSelection = parameters.allowsMultipleSelection
        panel.canChooseDirectories = false
        panel.allowedContentTypes = [.json]
        let response: NSApplication.ModalResponse
        if let window = webView.window {
            response = await panel.beginSheetModal(for: window)
        } else {
            response = panel.runModal()
        }
        return response == .OK ? panel.urls : nil
    }

    // MARK: 导出备份：网页调用 window.webkit.messageHandlers.saveTextFile.postMessage({ name, text })

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "saveTextFile",
              let body = message.body as? [String: Any],
              let name = body["name"] as? String,
              let text = body["text"] as? String else { return }

        let panel = NSSavePanel()
        panel.nameFieldStringValue = name
        panel.allowedContentTypes = [.json]
        panel.canCreateDirectories = true
        Task {
            let response: NSApplication.ModalResponse
            if let window = webView.window {
                response = await panel.beginSheetModal(for: window)
            } else {
                response = panel.runModal()
            }
            guard response == .OK, let url = panel.url else { return }
            do {
                try text.write(to: url, atomically: true, encoding: .utf8)
            } catch {
                NSAlert(error: error).runModal()
            }
        }
    }
}
