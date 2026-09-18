package com.malz.py;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CodeEditor extends EditText {

    private boolean coloring = false;
    private boolean internalChange = false;

    private final ArrayDeque<String> undoStack = new ArrayDeque<>();
    private final ArrayDeque<String> redoStack = new ArrayDeque<>();

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "and","as","assert","async","await","break","case","class",
            "continue","def","del","elif","else","except","False",
            "finally","for","from","global","if","import","in","is",
            "lambda","match","None","nonlocal","not","or","pass",
            "raise","return","True","try","while","with","yield",
            "print","input","range","len","str","int","float","list",
            "dict","set","tuple","bool","open","enumerate","zip",
            "sum","min","max","abs","round","type","super"
    ));

    public CodeEditor(Context context) {
        super(context);
        setup();
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        setBackgroundColor(Color.TRANSPARENT);
        setTextColor(Color.rgb(225, 225, 225));
        setHintTextColor(Color.rgb(110, 110, 110));

        setTextSize(15);
        setTypeface(Typeface.MONOSPACE);
        setGravity(Gravity.TOP | Gravity.START);

        setPadding(dp(10), dp(8), dp(12), dp(80));

        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setLongClickable(true);
        setEnabled(true);
        setCursorVisible(true);

        // PENTING: jangan pakai setTextIsSelectable(true)
        // karena editor harus menjadi EditText normal.
        setTextIsSelectable(false);

        setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        setSingleLine(false);
        setMaxLines(Integer.MAX_VALUE);

        setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        setHorizontallyScrolling(true);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        addTextChangedListener(new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
                if (!internalChange) {
                    beforeText = s.toString();
                }
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (internalChange) return;

                String current = e.toString();

                if (!current.equals(beforeText)) {
                    if (undoStack.isEmpty() ||
                            !undoStack.peek().equals(beforeText)) {
                        undoStack.push(beforeText);
                    }

                    redoStack.clear();
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // Pastikan sentuhan selalu diteruskan ke EditText Android.
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            requestFocus();
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            int start = getSelectionStart();

            if (start >= 0) {
                getText().insert(start, "    ");
                setSelection(start + 4);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    public void setCode(String code) {
        internalChange = true;

        setText(code == null ? "" : code);

        setSelection(getText().length());

        internalChange = false;

        undoStack.clear();
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;

        String current = getText().toString();
        String previous = undoStack.pop();

        redoStack.push(current);

        internalChange = true;
        setText(previous);
        setSelection(getText().length());
        internalChange = false;
    }

    public void redo() {
        if (redoStack.isEmpty()) return;

        String current = getText().toString();
        String next = redoStack.pop();

        undoStack.push(current);

        internalChange = true;
        setText(next);
        setSelection(getText().length());
        internalChange = false;
    }

    public void insertText(String value) {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        if (start < 0) start = getText().length();
        if (end < 0) end = start;

        getText().replace(
                Math.min(start, end),
                Math.max(start, end),
                value
        );

        int pos = Math.min(start, end) + value.length();
        setSelection(pos);
        requestFocus();
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }
}
