package com.malz.py;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import androidx.webkit.WebViewAssetLoader;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int OPEN_FILE = 1001;
    private static final int SAVE_FILE = 1002;

    private WebView webView;
    private Uri currentUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .addPathHandler(
                                "/assets/",
                                new WebViewAssetLoader.AssetsPathHandler(this)
                        )
                        .build();

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    String url
            ) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(true);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        setContentView(webView);

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private String getName(Uri uri) {
        Cursor c = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        if (c != null) {
            try {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (c.moveToFirst() && i >= 0) {
                    return c.getString(i);
                }
            } finally {
                c.close();
            }
        }

        return "untitled.py";
    }

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/x-python");
        startActivityForResult(i, OPEN_FILE);
    }

    private void savePicker(String name) {
        if (!name.toLowerCase().endsWith(".py")) {
            name += ".py";
        }

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/x-python");
        i.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(i, SAVE_FILE);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();

        if (uri == null) {
            return;
        }

        if (requestCode == OPEN_FILE) {

            String name = getName(uri);

            if (!name.toLowerCase().endsWith(".py")) {
                webView.evaluateJavascript(
                        "window.showAndroidError && window.showAndroidError('Hanya file .py yang bisa dibuka.');",
                        null
                );
                return;
            }

            try {
                InputStream in = getContentResolver().openInputStream(uri);
                byte[] bytes = in.readAllBytes();
                in.close();

                currentUri = uri;

                String content = new String(
                        bytes,
                        StandardCharsets.UTF_8
                );

                String js =
                        "window.openFromAndroid(" +
                        JSONObjectEscape.quote(name) +
                        "," +
                        JSONObjectEscape.quote(content) +
                        ");";

                webView.evaluateJavascript(js, null);

            } catch (Exception e) {
                webView.evaluateJavascript(
                        "window.showAndroidError && window.showAndroidError(" +
                        JSONObjectEscape.quote(e.toString()) +
                        ");",
                        null
                );
            }

        } else if (requestCode == SAVE_FILE) {

            currentUri = uri;

            webView.evaluateJavascript(
                    "window.saveCurrentContent && window.saveCurrentContent();",
                    null
            );
        }
    }

    private void saveContent(Uri uri, String content) {
        try {
            OutputStream out =
                    getContentResolver().openOutputStream(uri, "wt");

            if (out == null) {
                throw new Exception("Tidak bisa membuka file.");
            }

            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();

            webView.evaluateJavascript(
                    "window.savedFromAndroid && window.savedFromAndroid();",
                    null
            );

        } catch (Exception e) {
            webView.evaluateJavascript(
                    "window.showAndroidError && window.showAndroidError(" +
                    JSONObjectEscape.quote(e.toString()) +
                    ");",
                    null
            );
        }
    }

    public class AndroidBridge {

        @JavascriptInterface
        public void openFile() {
            runOnUiThread(() -> openPicker());
        }

        @JavascriptInterface
        public void saveFile(
                String name,
                String content
        ) {
            runOnUiThread(() -> {

                if (currentUri == null) {
                    savePicker(name);
                } else {
                    saveContent(currentUri, content);
                }
            });
        }

        @JavascriptInterface
        public void saveAs(
                String name,
                String content
        ) {
            runOnUiThread(() -> savePicker(name));
        }
    }

    static class JSONObjectEscape {

        static String quote(String s) {
            if (s == null) return "\"\"";

            StringBuilder b = new StringBuilder("\"");

            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"': b.append("\\\""); break;
                    case '\\': b.append("\\\\"); break;
                    case '\n': b.append("\\n"); break;
                    case '\r': b.append("\\r"); break;
                    case '\t': b.append("\\t"); break;
                    case '\b': b.append("\\b"); break;
                    case '\f': b.append("\\f"); break;
                    default:
                        if (c < 32) {
                            b.append(String.format("\\u%04x", (int)c));
                        } else {
                            b.append(c);
                        }
                }
            }

            b.append("\"");
            return b.toString();
        }
    }
}
