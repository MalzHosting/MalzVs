package com.malz.py;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private CodeEditor editor;
    private TextView lineNumbers;
    private TextView terminal;
    private TextView filename;
    private Button runButton;

    private Uri currentUri = null;
    private String currentFile = "untitled.py";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean pythonReady = false;

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        buildUi();
        initPython();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9, 11, 15));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), dp(4), dp(6), dp(4));
        header.setBackgroundColor(Color.rgb(13, 16, 21));

        TextView logo = text("M", 25, Color.rgb(255, 212, 59));
        logo.setTypeface(Typeface.DEFAULT_BOLD);

        filename = text(currentFile, 15, Color.WHITE);
        filename.setTypeface(Typeface.MONOSPACE);

        header.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(52)));
        header.addView(filename, new LinearLayout.LayoutParams(0, dp(52), 1));

        root.addView(header);

        HorizontalScrollView actionsScroll = new HorizontalScrollView(this);
        actionsScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(5), 0, dp(5), 0);

        Button newButton = button("NEW");
        Button openButton = button("OPEN");
        Button saveButton = button("SAVE");
        runButton = button("RUN");
        Button undoButton = button("↶");
        Button redoButton = button("↷");

        actions.addView(newButton);
        actions.addView(openButton);
        actions.addView(saveButton);
        actions.addView(runButton);
        actions.addView(undoButton);
        actions.addView(redoButton);

        actionsScroll.addView(actions);
        root.addView(actionsScroll, new LinearLayout.LayoutParams(-1, dp(50)));

        newButton.setOnClickListener(v -> newFile());
        openButton.setOnClickListener(v -> openFile());
        saveButton.setOnClickListener(v -> saveFile());
        runButton.setOnClickListener(v -> runPython());
        undoButton.setOnClickListener(v -> editor.undoText());
        redoButton.setOnClickListener(v -> editor.redoText());

        LinearLayout editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.HORIZONTAL);
        editorArea.setBackgroundColor(Color.rgb(18, 21, 27));

        lineNumbers = text("1", 14, Color.rgb(100, 108, 120));
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setPadding(dp(5), dp(10), dp(8), dp(20));
        lineNumbers.setBackgroundColor(Color.rgb(13, 16, 21));

        editor = new CodeEditor(this);

        editorArea.addView(lineNumbers,
                new LinearLayout.LayoutParams(dp(45), -1));

        editorArea.addView(editor,
                new LinearLayout.LayoutParams(0, -1, 1));

        root.addView(editorArea,
                new LinearLayout.LayoutParams(-1, 0, 1));

        editor.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                updateLineNumbers();
            }
            public void afterTextChanged(android.text.Editable e) {}
        });

        LinearLayout terminalHeader = new LinearLayout(this);
        terminalHeader.setGravity(Gravity.CENTER_VERTICAL);
        terminalHeader.setPadding(dp(10), 0, dp(10), 0);
        terminalHeader.setBackgroundColor(Color.rgb(13, 16, 21));

        TextView terminalTitle = text("TERMINAL", 12, Color.rgb(255, 212, 59));
        terminalHeader.addView(terminalTitle,
                new LinearLayout.LayoutParams(0, dp(34), 1));

        Button clear = button("CLEAR");
        terminalHeader.addView(clear);

        root.addView(terminalHeader);

        terminal = text("MalzPy Python ready.\n", 13, Color.rgb(210, 214, 220));
        terminal.setTypeface(Typeface.MONOSPACE);
        terminal.setPadding(dp(10), dp(8), dp(10), dp(8));
        terminal.setBackgroundColor(Color.rgb(7, 9, 12));

        ScrollView terminalScroll = new ScrollView(this);
        terminalScroll.addView(terminal);
        root.addView(terminalScroll,
                new LinearLayout.LayoutParams(-1, dp(145)));

        clear.setOnClickListener(v -> terminal.setText(""));

        setContentView(root);
        updateLineNumbers();
    }

    private void updateLineNumbers() {
        if (lineNumbers == null || editor == null) return;

        String value = editor.getText().toString();
        int count = 1;

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\n') count++;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= count; i++) {
            sb.append(i);
            if (i < count) sb.append('\n');
        }

        lineNumbers.setText(sb.toString());
    }

    private void initPython() {
        runButton.setEnabled(false);

        executor.execute(() -> {
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }

                Python.getInstance();

                pythonReady = true;

                runOnUiThread(() -> {
                    runButton.setEnabled(true);
                    terminal.append("Python runtime siap (offline).\n");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminal.append("Python gagal dimuat:\n" +
                            e.getMessage() + "\n");
                });
            }
        });
    }

    private void newFile() {
        final EditText input = new EditText(this);
        input.setHint("nama file, contoh: main.py");
        input.setSingleLine(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New Python File")
                .setMessage("Masukkan nama file baru")
                .setView(input)
                .setNegativeButton("BATAL", null)
                .setPositiveButton("BUAT", null)
                .create();

        dialog.setOnShowListener(v -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
                String name = input.getText().toString().trim();

                if (name.isEmpty()) {
                    input.setError("Nama file wajib diisi");
                    return;
                }

                if (!name.toLowerCase().endsWith(".py")) {
                    name += ".py";
                }

                currentUri = null;
                currentFile = name;
                filename.setText(name);
                editor.setCode("");
                terminal.setText("File baru: " + name + "\n");

                dialog.dismiss();

                editor.requestFocus();
                InputMethodManager imm =
                        (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);
            });
        });

        dialog.show();
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/x-python");
        startActivityForResult(intent, 100);
    }

    private void saveFile() {
        if (currentUri == null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/x-python");
            intent.putExtra(Intent.EXTRA_TITLE, currentFile);
            startActivityForResult(intent, 101);
            return;
        }

        writeFile(currentUri);
    }

    private void writeFile(Uri uri) {
        executor.execute(() -> {
            try {
                OutputStream out = getContentResolver().openOutputStream(uri);

                if (out == null) throw new Exception("Tidak bisa membuka file");

                out.write(editor.getText().toString().getBytes(StandardCharsets.UTF_8));
                out.close();

                runOnUiThread(() ->
                        terminal.append("Saved: " + currentFile + "\n"));

            } catch (Exception e) {
                runOnUiThread(() ->
                        terminal.append("SAVE ERROR: " + e.getMessage() + "\n"));
            }
        });
    }

    private void loadFile(Uri uri) {
        executor.execute(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                getContentResolver().openInputStream(uri),
                                StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }

                reader.close();

                String name = uri.getLastPathSegment();
                if (name == null || !name.toLowerCase().endsWith(".py")) {
                    name = "opened.py";
                }

                final String finalName = name;

                runOnUiThread(() -> {
                    currentUri = uri;
                    currentFile = finalName;
                    filename.setText(finalName);
                    editor.setCode(sb.toString());
                    terminal.setText("Opened: " + finalName + "\n");
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        terminal.append("OPEN ERROR: " + e.getMessage() + "\n"));
            }
        });
    }

    private void runPython() {
        if (!pythonReady) {
            Toast.makeText(this, "Python masih dimuat...", Toast.LENGTH_SHORT).show();
            return;
        }

        final String code = editor.getText().toString();

        terminal.append("\n>>> RUN " + currentFile + "\n");

        runButton.setEnabled(false);

        executor.execute(() -> {
            try {
                Python py = Python.getInstance();
                PyObject runner = py.getModule("runner");
                PyObject result = runner.callAttr("run", code);
                String output = result.toJava(String.class);

                runOnUiThread(() -> {
                    terminal.append(output);

                    if (!output.endsWith("\n")) {
                        terminal.append("\n");
                    }

                    terminal.append(">>> selesai\n");
                    runButton.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminal.append("RUN ERROR:\n");
                    terminal.append(e.toString());
                    terminal.append("\n");
                    runButton.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == 100) {
            String path = uri.getLastPathSegment();

            if (path == null || !path.toLowerCase().endsWith(".py")) {
                Toast.makeText(this,
                        "MalzPy hanya bisa membuka file .py",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            currentUri = uri;
            loadFile(uri);

        } else if (requestCode == 101) {
            currentUri = uri;
            writeFile(uri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
