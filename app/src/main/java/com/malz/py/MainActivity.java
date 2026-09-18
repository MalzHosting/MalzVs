package com.malz.py;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private CodeEditor editor;
    private TextView lineNumbers;
    private TextView filename;

    private LinearLayout editorPage;
    private LinearLayout terminalPage;

    private TextView terminalOutput;
    private EditText terminalInput;
    private TextView inputPrompt;
    private TextView terminalStatus;

    private Button runButton;

    private Uri currentUri;
    private String currentFile = "untitled.py";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private volatile boolean pythonReady = false;

    private InputProvider inputProvider;

    private PopupWindow suggestionPopup;
    private ListView suggestionList;

    private final String[] suggestions = {
            "print",
            "input",
            "import",
            "from",
            "def",
            "class",
            "if",
            "elif",
            "else",
            "for",
            "while",
            "try",
            "except",
            "finally",
            "with",
            "return",
            "break",
            "continue",
            "pass",
            "raise",
            "True",
            "False",
            "None",
            "and",
            "or",
            "not",
            "in",
            "is",
            "len",
            "range",
            "str",
            "int",
            "float",
            "list",
            "dict",
            "set",
            "tuple",
            "open"
    };

    private int dp(float v) {
        return (int)(v *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(17, 17, 17));
        getWindow().setNavigationBarColor(Color.BLACK);

        inputProvider = new InputProvider(
                prompt -> runOnUiThread(() -> showTerminalInput(prompt))
        );

        buildEditorPage();
        buildTerminalPage();

        showEditor();

        initPython();
    }

    private void buildEditorPage() {

        editorPage = new LinearLayout(this);
        editorPage.setOrientation(LinearLayout.VERTICAL);
        editorPage.setBackgroundColor(Color.rgb(48, 48, 48));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), 0, dp(8), 0);
        top.setBackgroundColor(Color.rgb(43, 43, 43));

        TextView back = tv("‹", 30, Color.WHITE);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(36), dp(52)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = tv("Coding Python", 17, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        filename = tv(currentFile, 11, Color.LTGRAY);

        titleBox.addView(title);
        titleBox.addView(filename);

        top.addView(titleBox,
                new LinearLayout.LayoutParams(0, dp(70), 1));

        runButton = btn("RUN");
        runButton.setTextSize(13);
        runButton.setTypeface(Typeface.DEFAULT_BOLD);

        Button menu = btn("MENU");
        menu.setTextSize(13);

        top.addView(runButton,
                new LinearLayout.LayoutParams(dp(62), dp(52)));

        top.addView(menu,
                new LinearLayout.LayoutParams(dp(62), dp(52)));

        editorPage.addView(top);

        LinearLayout codeArea = new LinearLayout(this);
        codeArea.setOrientation(LinearLayout.HORIZONTAL);
        codeArea.setBackgroundColor(Color.rgb(48, 48, 48));

        lineNumbers = tv("1", 14, Color.rgb(130, 130, 130));
        lineNumbers.setTypeface(Typeface.MONOSPACE);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setPadding(0, dp(11), dp(8), dp(20));
        lineNumbers.setBackgroundColor(Color.rgb(45, 45, 45));

        codeArea.addView(lineNumbers,
                new LinearLayout.LayoutParams(dp(40), -1));

        editor = new CodeEditor(this);
        editor.setFocusable(true);
        editor.setFocusableInTouchMode(true);
                editor.setCursorVisible(true);
        editor.setEnabled(true);
        editor.setClickable(true);

        editor.setClickable(true);
        editor.setLongClickable(true);
        editor.setEnabled(true);
        editor.setCursorVisible(true);
        editor.setTextIsSelectable(false);
        editor.setSingleLine(false);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setShowSoftInputOnFocus(true);


        codeArea.addView(editor,
                new LinearLayout.LayoutParams(0, -1, 1));

        editorPage.addView(codeArea,
                new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(Color.rgb(43, 43, 43));

        addTool(toolbar, "Tab", "    ");
        addTool(toolbar, "{}", "{}");
        addTool(toolbar, "\"\"", "\"\"");
        addTool(toolbar, ";", ";");
        addTool(toolbar, "↶", "undo");
        addTool(toolbar, "⇧", "up");
        addTool(toolbar, "⇩", "down");
        addTool(toolbar, "⇨", "right");

        editorPage.addView(toolbar,
                new LinearLayout.LayoutParams(-1, dp(42)));

        runButton.setOnClickListener(v -> runPython());

        back.setOnClickListener(v -> finish());

        menu.setOnClickListener(v -> showMenu());
    }

    private void addTool(LinearLayout bar, String label, String value) {

        Button b = btn(label);
        b.setTextSize(13);

        bar.addView(b,
                new LinearLayout.LayoutParams(
                        0, dp(55), 1));

        b.setOnClickListener(v -> {

            if (value.equals("undo")) {
                editor.undo();
                return;
            }

            if (value.equals("up")) {
                moveCursor(-1);
                return;
            }

            if (value.equals("down")) {
                moveCursor(1);
                return;
            }

            if (value.equals("right")) {
                moveCursorRight();
                return;
            }

            int pos = editor.getSelectionStart();
            editor.getText().insert(pos, value);
        });
    }

    private void moveCursor(int direction) {
        int pos = editor.getSelectionStart();

        try {
            int target = editor.getLayout().getOffsetForHorizontal(
                    editor.getLayout().getLineForOffset(pos) + direction,
                    0
            );

            editor.setSelection(
                    Math.max(0, Math.min(target, editor.length()))
            );
        } catch (Exception ignored) {
        }
    }

    private void moveCursorRight() {
        int p = editor.getSelectionStart();
        if (p < editor.length()) editor.setSelection(p + 1);
    }

    private void buildTerminalPage() {

        terminalPage = new LinearLayout(this);
        terminalPage.setOrientation(LinearLayout.VERTICAL);
        terminalPage.setBackgroundColor(Color.rgb(8, 10, 13));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), 0, dp(5), 0);
        header.setBackgroundColor(Color.rgb(43, 43, 43));

        Button back = btn("‹");
        back.setTextSize(32);

        TextView title = tv("Terminal", 20, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        header.addView(back,
                new LinearLayout.LayoutParams(dp(55), dp(62)));

        header.addView(title,
                new LinearLayout.LayoutParams(0, dp(62), 1));

        terminalStatus = tv("RUNNING", 13, Color.rgb(255, 212, 59));
        header.addView(terminalStatus,
                new LinearLayout.LayoutParams(dp(85), dp(62)));

        terminalPage.addView(header);

        terminalOutput = tv("", 14, Color.rgb(225, 225, 225));
        terminalOutput.setTypeface(Typeface.MONOSPACE);
        terminalOutput.setGravity(Gravity.TOP);
        terminalOutput.setPadding(dp(12), dp(12), dp(12), dp(12));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(terminalOutput);

        terminalPage.addView(scroll,
                new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout inputBox = new LinearLayout(this);
        inputBox.setOrientation(LinearLayout.VERTICAL);
        inputBox.setPadding(dp(10), dp(6), dp(10), dp(8));
        inputBox.setBackgroundColor(Color.rgb(25, 27, 31));
        inputBox.setVisibility(View.GONE);

        inputPrompt = tv("", 14, Color.rgb(255, 212, 59));

        terminalInput = new EditText(this);
        terminalInput.setSingleLine(true);
        terminalInput.setTextColor(Color.WHITE);
        terminalInput.setHintTextColor(Color.GRAY);
        terminalInput.setTextSize(13);
        terminalInput.setHint("ketik input lalu Enter");
        terminalInput.setBackgroundColor(Color.rgb(40, 43, 48));

        inputBox.addView(inputPrompt);
        inputBox.addView(terminalInput,
                new LinearLayout.LayoutParams(-1, dp(50)));

        terminalPage.addView(inputBox,
                new LinearLayout.LayoutParams(-1, dp(85)));

        terminalInput.setOnEditorActionListener((v, action, event) -> {
            submitTerminalInput();
            return true;
        });

        terminalInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                submitTerminalInput();
                return true;
            }
            return false;
        });

        back.setOnClickListener(v -> {
            inputProvider.cancel();
            showEditor();
        });

        inputBox.setTag("inputBox");
    }

    private void showTerminalInput(String prompt) {

        inputPrompt.setText(prompt);

        LinearLayout inputBox =
                (LinearLayout)terminalInput.getParent();

        inputBox.setVisibility(View.VISIBLE);

        terminalStatus.setText("WAITING INPUT");

        terminalInput.setText("");
        terminalInput.requestFocus();

        InputMethodManager imm =
                (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);

        imm.showSoftInput(
                terminalInput,
                InputMethodManager.SHOW_IMPLICIT
        );
    }

    private void submitTerminalInput() {

        String value = terminalInput.getText().toString();

        appendTerminal(value + "\n");

        LinearLayout inputBox =
                (LinearLayout)terminalInput.getParent();

        inputBox.setVisibility(View.GONE);

        terminalStatus.setText("RUNNING");

        InputMethodManager imm =
                (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(
                terminalInput.getWindowToken(), 0);

        inputProvider.submit(value);
    }

    
    private void modernizeButtons(android.view.View root) {
        if (root == null) return;

        styleViewButtons(root);
    }

    private void styleViewButtons(android.view.View view) {
        if (view instanceof android.widget.Button) {
            android.widget.Button b = (android.widget.Button) view;
            String t = b.getText() == null ? "" : b.getText().toString().trim();

            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();

            if (t.equalsIgnoreCase("run") ||
                t.equalsIgnoreCase("menu")) {
                bg.setColor(android.graphics.Color.rgb(68, 68, 71));
                b.setTextColor(android.graphics.Color.WHITE);
            } else {
                bg.setColor(android.graphics.Color.rgb(57, 57, 60));
                b.setTextColor(android.graphics.Color.rgb(220, 220, 220));
            }

            bg.setCornerRadius(dp(7));
            b.setBackground(bg);
            b.setMinHeight(dp(38));
            b.setMinWidth(dp(48));
            b.setPadding(dp(6), 0, dp(6), 0);
            b.setAllCaps(false);
            b.setGravity(android.view.Gravity.CENTER);
            b.setTextSize(13);
            b.setStateListAnimator(null);
            b.setElevation(dp(2));

            android.view.ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof android.widget.LinearLayout.LayoutParams) {
                android.widget.LinearLayout.LayoutParams x =
                        (android.widget.LinearLayout.LayoutParams) lp;
                x.setMargins(dp(3), dp(3), dp(3), dp(3));
                b.setLayoutParams(x);
            }
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleViewButtons(group.getChildAt(i));
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

private void showEditor() {
        setContentView(editorPage);
        modernizeButtons(editorPage);

        editor.setFocusable(true);
        editor.setFocusableInTouchMode(true);
        editor.setClickable(true);
        editor.setEnabled(true);
        editor.setCursorVisible(true);
    }

    private void showTerminal() {
        setContentView(terminalPage);
    }

    private void appendTerminal(String text) {
        terminalOutput.append(text);
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
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Python gagal dimuat: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        });
    }

    private void runPython() {

        if (!pythonReady) {
            Toast.makeText(
                    this,
                    "Python masih dimuat...",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String code = editor.getText().toString();

        terminalOutput.setText("");
        appendTerminal("MalzPy Terminal\n");
        appendTerminal("--------------------\n");
        appendTerminal("Running " + currentFile + "\n\n");

        showTerminal();

        executor.execute(() -> {

            try {

                Python py = Python.getInstance();

                PyObject runner =
                        py.getModule("runner");

                PyObject result =
                        runner.callAttr(
                                "run",
                                code,
                                inputProvider
                        );

                String output =
                        result.toJava(String.class);

                runOnUiThread(() -> {

                    appendTerminal(output);

                    if (!output.endsWith("\n")) {
                        appendTerminal("\n");
                    }

                    appendTerminal("\n[program selesai]\n");
                    terminalStatus.setText("DONE");
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    appendTerminal(
                            "\nERROR:\n" +
                            e.toString() +
                            "\n"
                    );

                    terminalStatus.setText("ERROR");
                });
            }
        });
    }

    private void newFile() {

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("contoh: main.py");

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("New Python File")
                        .setMessage("Nama file baru")
                        .setView(input)
                        .setNegativeButton("BATAL", null)
                        .setPositiveButton("BUAT", null)
                        .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(x -> {

                String name =
                        input.getText().toString().trim();

                if (name.isEmpty()) {
                    input.setError("Masukkan nama file");
                    return;
                }

                if (!name.toLowerCase().endsWith(".py")) {
                    name += ".py";
                }

                currentFile = name;
                currentUri = null;

                filename.setText(name);
                editor.setCode("");

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void openFile() {

        Intent i =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/x-python");

        startActivityForResult(i, 100);
    }

    private void saveFile() {

        if (currentUri == null) {

            Intent i =
                    new Intent(Intent.ACTION_CREATE_DOCUMENT);

            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/x-python");

            i.putExtra(
                    Intent.EXTRA_TITLE,
                    currentFile
            );

            startActivityForResult(i, 101);

        } else {
            writeFile(currentUri);
        }
    }

    private void writeFile(Uri uri) {

        executor.execute(() -> {

            try {

                OutputStream out =
                        getContentResolver()
                                .openOutputStream(uri);

                if (out == null)
                    throw new Exception("Tidak bisa membuka file");

                out.write(
                        editor.getText()
                                .toString()
                                .getBytes(StandardCharsets.UTF_8)
                );

                out.close();

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Saved " + currentFile,
                                Toast.LENGTH_SHORT
                        ).show()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "SAVE ERROR: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        });
    }

    private void loadFile(Uri uri) {

        executor.execute(() -> {

            try {

                BufferedReader r =
                        new BufferedReader(
                                new InputStreamReader(
                                        getContentResolver()
                                                .openInputStream(uri),
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder sb =
                        new StringBuilder();

                String line;

                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }

                r.close();

                String name =
                        uri.getLastPathSegment();

                if (name == null ||
                        !name.toLowerCase().endsWith(".py")) {
                    name = "opened.py";
                }

                final String finalName = name;

                runOnUiThread(() -> {

                    currentUri = uri;
                    currentFile = finalName;

                    filename.setText(finalName);
                    editor.setCode(sb.toString());
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "OPEN ERROR: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        });
    }

    private void showMenu() {

        final String[] items = {
                "NEW",
                "OPEN",
                "SAVE",
                "UNDO",
                "REDO"
        };

        new AlertDialog.Builder(this)
                .setTitle("MalzPy")
                .setItems(items, (d, which) -> {

                    if (which == 0) newFile();
                    if (which == 1) openFile();
                    if (which == 2) saveFile();
                    if (which == 3) editor.undo();
                    if (which == 4) editor.redo();

                })
                .show();
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

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == 100) {

            String name =
                    uri.getLastPathSegment();

            if (name == null ||
                    !name.toLowerCase().endsWith(".py")) {

                Toast.makeText(
                        this,
                        "Hanya file .py",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            loadFile(uri);

        } else if (requestCode == 101) {

            currentUri = uri;
            writeFile(uri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        inputProvider.cancel();
        executor.shutdownNow();
    }
}
