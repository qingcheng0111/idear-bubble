package com.idear.bubble;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 加载三端共用的网页核心（assets/index.html），
 * 并补上网页在 WebView 里缺少的原生能力：保存文件、选择文件、提示与确认框。
 */
public class MainActivity extends Activity {

    // 用固定的 https 地址提供本地网页，localStorage 才有稳定的来源
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + ASSET_HOST + "/index.html";
    private static final int REQUEST_OPEN_FILE = 1;
    private static final int REQUEST_SAVE_FILE = 2;
    private static final int PAGE_BACKGROUND = Color.rgb(0xf6, 0xf5, 0xef);

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingSaveText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(PAGE_BACKGROUND);
        webView = new WebView(this);
        webView.setBackgroundColor(PAGE_BACKGROUND);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        applySystemBarInsets(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);

        webView.addJavascriptInterface(new NativeBridge(), "IdearNative");
        webView.setWebViewClient(new AssetClient());
        webView.setWebChromeClient(new ChromeClient());
        webView.loadUrl(START_URL);
    }

    // Android 15 起应用默认全面屏绘制，这里给状态栏、导航栏和键盘留出位置
    private void applySystemBarInsets(FrameLayout root) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return;
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(light, light);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
        if (requestCode == REQUEST_OPEN_FILE) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(uri != null ? new Uri[] { uri } : null);
                fileCallback = null;
            }
        } else if (requestCode == REQUEST_SAVE_FILE) {
            String text = pendingSaveText;
            pendingSaveText = null;
            if (uri != null && text != null) writeText(uri, text);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void writeText(Uri uri, String text) {
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IOException("无法写入所选位置");
            out.write(text.getBytes(StandardCharsets.UTF_8));
            toast(R.string.backup_saved);
        } catch (IOException e) {
            toast(R.string.backup_failed);
        }
    }

    private void toast(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }

    private static WebResourceResponse notFound() {
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
    }

    /** 只提供 assets/index.html，不访问网络、不跳转外部网页。 */
    private final class AssetClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();
            if (!"https".equals(url.getScheme()) || !ASSET_HOST.equals(url.getHost())) return null;
            if (!"/index.html".equals(url.getPath())) return notFound();
            try {
                InputStream in = getAssets().open("index.html");
                return new WebResourceResponse("text/html", "utf-8", in);
            } catch (IOException e) {
                return notFound();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return !ASSET_HOST.equals(request.getUrl().getHost());
        }
    }

    /** 网页里的 alert / confirm 和「从备份恢复」的文件选择。 */
    private final class ChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setOnCancelListener(dialog -> result.confirm())
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                    .setOnCancelListener(dialog -> result.cancel())
                    .show();
            return true;
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                         FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // 各家文件管理器给 .json 标的类型不一样，选中后由网页校验内容
            try {
                startActivityForResult(intent, REQUEST_OPEN_FILE);
            } catch (ActivityNotFoundException e) {
                fileCallback = null;
                callback.onReceiveValue(null);
                toast(R.string.no_file_app);
            }
            return true;
        }
    }

    /** 网页通过 window.IdearNative 调用：导出备份时弹出系统的「保存到」界面。 */
    private final class NativeBridge {
        @JavascriptInterface
        public void saveTextFile(String name, String text) {
            runOnUiThread(() -> {
                pendingSaveText = text;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, name);
                try {
                    startActivityForResult(intent, REQUEST_SAVE_FILE);
                } catch (ActivityNotFoundException e) {
                    pendingSaveText = null;
                    toast(R.string.no_file_app);
                }
            });
        }
    }
}
