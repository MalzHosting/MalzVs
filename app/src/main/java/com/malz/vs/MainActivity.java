package com.malz.vs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView web;
    private Uri currentFile;

    private static final int OPEN_FILE = 100;
    private static final int SAVE_FILE = 101;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(24,24,24));
        w.setNavigationBarColor(Color.rgb(24,24,24));

        web = new WebView(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setBackgroundColor(Color.rgb(30,30,30));
        web.setWebViewClient(new WebViewClient());

        web.addJavascriptInterface(new AndroidBridge(), "Android");

        setContentView(web);

        web.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidBridge {

        @JavascriptInterface
        public void openFile() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, OPEN_FILE);
            });
        }

        @JavascriptInterface
        public void saveFile(String name, String content) {
            runOnUiThread(() -> {

                if (currentFile == null) {
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_TITLE,
                            name == null || name.isEmpty()
                                    ? "untitled.txt"
                                    : name);
                    pendingContent = content == null ? "" : content;
                    pendingName = name;
                    startActivityForResult(i, SAVE_FILE);
                } else {
                    writeFile(currentFile, content);
                }
            });
        }

        @JavascriptInterface
        public void saveAs(String name, String content) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_TITLE,
                        name == null || name.isEmpty()
                                ? "untitled.txt"
                                : name);

                pendingContent = content == null ? "" : content;
                pendingName = name;

                startActivityForResult(i, SAVE_FILE);
            });
        }

        @JavascriptInterface
        public void showHtml(String html) {
            runOnUiThread(() -> {

                WebView preview = new WebView(MainActivity.this);

                WebSettings s = preview.getSettings();
                s.setJavaScriptEnabled(true);
                s.setDomStorageEnabled(true);
                s.setAllowFileAccess(true);
                s.setAllowContentAccess(true);

                preview.setBackgroundColor(Color.WHITE);

                preview.loadDataWithBaseURL(
                        "file:///android_asset/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                );

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("MalzCode HTML Preview")
                        .setView(preview)
                        .setPositiveButton("CLOSE", null)
                        .show();
            });
        }

        @JavascriptInterface
        public void toast(String message) {
            runOnUiThread(() ->
                    Toast.makeText(
                            MainActivity.this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    private String pendingContent = "";
    private String pendingName = "";

    private void readFile(Uri uri) {
        try {

            BufferedReader r = new BufferedReader(
                    new InputStreamReader(
                            getContentResolver().openInputStream(uri),
                            StandardCharsets.UTF_8
                    )
            );

            StringBuilder out = new StringBuilder();
            String line;

            while ((line = r.readLine()) != null) {
                out.append(line).append("\n");
            }

            r.close();

            currentFile = uri;

            String name = uri.getLastPathSegment();
            if (name == null) name = "untitled";

            String safeName = name
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            String safeContent = out.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            web.evaluateJavascript(
                    "window.openFromAndroid(\"" +
                            safeName +
                            "\",\"" +
                            safeContent +
                            "\");",
                    null
            );

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Gagal membuka file",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void writeFile(Uri uri, String content) {
        try {

            OutputStream out =
                    getContentResolver().openOutputStream(uri);

            if (out == null) {
                throw new Exception("output null");
            }

            out.write(
                    (content == null ? "" : content)
                            .getBytes(StandardCharsets.UTF_8)
            );

            out.close();

            web.evaluateJavascript(
                    "window.savedFromAndroid();",
                    null
            );

            Toast.makeText(
                    this,
                    "File tersimpan",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Gagal menyimpan file",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();

        if (uri == null) {
            return;
        }

        if (requestCode == OPEN_FILE) {
            readFile(uri);
        }

        if (requestCode == SAVE_FILE) {

            currentFile = uri;

            writeFile(
                    uri,
                    pendingContent
            );

            pendingContent = "";
            pendingName = "";
        }
    }
}
