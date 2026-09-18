package com.malz.py;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CodeEditor extends EditText {

    private boolean internal = false;
    private String beforeChange = "";

    private final ArrayDeque<String> undo =
            new ArrayDeque<>();

    private final ArrayDeque<String> redo =
            new ArrayDeque<>();

    private static final Set<String> KEYWORDS =
            new HashSet<>(Arrays.asList(
                    "and","as","assert","async","await",
                    "break","case","class","continue",
                    "def","del","elif","else","except",
                    "False","finally","for","from","global",
                    "if","import","in","is","lambda",
                    "match","None","nonlocal","not","or",
                    "pass","raise","return","True","try",
                    "while","with","yield","print","input",
                    "len","range","str","int","float",
                    "list","dict","set","tuple","open"
            ));

    public CodeEditor(Context c) {
        super(c);
        setup();
    }

    public CodeEditor(Context c, AttributeSet a) {
        super(c, a);
        setup();
    }

    private void setup() {

        setTextColor(Color.rgb(235,235,235));
        setTextSize(15);
        setTypeface(Typeface.MONOSPACE);

        setGravity(Gravity.TOP | Gravity.START);

        setBackgroundColor(
                Color.rgb(48,48,48)
        );

        setPadding(
                10,
                10,
                10,
                20
        );

        setSingleLine(false);
        setHorizontallyScrolling(true);

        setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        undo.push("");

        addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {

                if (!internal)
                    beforeChange = s.toString();
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
            }

            @Override
            public void afterTextChanged(
                    Editable e) {

                if (internal) return;

                String current = e.toString();

                if (!current.equals(beforeChange)) {

                    undo.push(current);

                    if (undo.size() > 100)
                        undo.removeLast();

                    redo.clear();
                }

                highlight(e);
                autoPairs(e);
                autoIndent(e);
            }
        });
    }

    private void autoPairs(Editable e) {

        int p = getSelectionStart();

        if (p <= 0 || p > e.length())
            return;

        char c = e.charAt(p - 1);

        String pair = null;

        if (c == '(') pair = ")";
        if (c == '[') pair = "]";
        if (c == '{') pair = "}";
        if (c == '"') pair = "\"";
        if (c == '\'') pair = "'";

        if (pair == null) return;

        if (p < e.length() &&
                e.charAt(p) == pair.charAt(0))
            return;

        internal = true;

        e.insert(p, pair);

        setSelection(p);

        internal = false;
    }

    private void autoIndent(Editable e) {

        int p = getSelectionStart();

        if (p <= 0 || p > e.length())
            return;

        if (e.charAt(p - 1) != '\n')
            return;

        int start =
                e.toString()
                        .lastIndexOf('\n', p - 2);

        start = start < 0 ? 0 : start + 1;

        String previous =
                e.subSequence(
                        start,
                        p - 1
                ).toString();

        int spaces = 0;

        while (spaces < previous.length() &&
                previous.charAt(spaces) == ' ') {
            spaces++;
        }

        String indent =
                previous.substring(0, spaces);

        if (previous.trim().endsWith(":"))
            indent += "    ";

        if (indent.isEmpty())
            return;

        internal = true;

        e.insert(p, indent);

        setSelection(
                p + indent.length()
        );

        internal = false;
    }

    private void highlight(Editable e) {

        ForegroundColorSpan[] spans =
                e.getSpans(
                        0,
                        e.length(),
                        ForegroundColorSpan.class
                );

        for (ForegroundColorSpan span : spans)
            e.removeSpan(span);

        String s = e.toString();

        int i = 0;

        while (i < s.length()) {

            char c = s.charAt(i);

            if (c == '#') {

                int end =
                        s.indexOf('\n', i);

                if (end < 0)
                    end = s.length();

                e.setSpan(
                        new ForegroundColorSpan(
                                Color.rgb(106,153,85)
                        ),
                        i,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                i = end;
                continue;
            }

            if (c == '"' || c == '\'') {

                char q = c;
                int end = i + 1;

                while (end < s.length()) {

                    if (s.charAt(end) == '\\') {
                        end += 2;
                        continue;
                    }

                    if (s.charAt(end) == q) {
                        end++;
                        break;
                    }

                    end++;
                }

                e.setSpan(
                        new ForegroundColorSpan(
                                Color.rgb(206,145,120)
                        ),
                        i,
                        Math.min(end, s.length()),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                i = end;
                continue;
            }

            if (Character.isDigit(c)) {

                int end = i + 1;

                while (end < s.length() &&
                        (Character.isDigit(s.charAt(end)) ||
                         s.charAt(end) == '.')) {
                    end++;
                }

                e.setSpan(
                        new ForegroundColorSpan(
                                Color.rgb(181,206,168)
                        ),
                        i,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                i = end;
                continue;
            }

            if (Character.isLetter(c) || c == '_') {

                int end = i + 1;

                while (end < s.length() &&
                        (Character.isLetterOrDigit(s.charAt(end)) ||
                         s.charAt(end) == '_')) {
                    end++;
                }

                String word =
                        s.substring(i,end);

                if (KEYWORDS.contains(word)) {

                    e.setSpan(
                            new ForegroundColorSpan(
                                    Color.rgb(220,170,80)
                            ),
                            i,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                i = end;
                continue;
            }

            i++;
        }
    }

    public void undoText() {

        if (undo.size() <= 1)
            return;

        String current = undo.pop();

        redo.push(current);

        String previous = undo.peek();

        internal = true;

        setText(previous);

        setSelection(
                getText().length()
        );

        internal = false;

        highlight(getText());
    }

    public void redoText() {

        if (redo.isEmpty())
            return;

        String next = redo.pop();

        undo.push(next);

        internal = true;

        setText(next);

        setSelection(
                getText().length()
        );

        internal = false;

        highlight(getText());
    }

    public void setCode(String code) {

        internal = true;

        setText(
                code == null ? "" : code
        );

        setSelection(
                getText().length()
        );

        undo.clear();
        redo.clear();

        undo.push(
                getText().toString()
        );

        internal = false;

        highlight(getText());
    }
}
