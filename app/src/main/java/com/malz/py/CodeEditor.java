package com.malz.py;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
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
    private final ArrayDeque<String> undo = new ArrayDeque<>();
    private final ArrayDeque<String> redo = new ArrayDeque<>();
    private String beforeChange = "";

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "and","as","assert","async","await","break","case","class",
            "continue","def","del","elif","else","except","False","finally",
            "for","from","global","if","import","in","is","lambda","match",
            "None","nonlocal","not","or","pass","raise","return","True",
            "try","while","with","yield","print","input","len","range",
            "str","int","float","list","dict","set","tuple","open"
    ));

    public CodeEditor(Context c, AttributeSet a) {
        super(c, a);
        setup();
    }

    public CodeEditor(Context c) {
        super(c);
        setup();
    }

    private void setup() {
        setTextColor(Color.WHITE);
        setTextSize(15);
        setTypeface(Typeface.MONOSPACE);
        setGravity(Gravity.TOP | Gravity.START);
        setBackgroundColor(Color.rgb(18, 21, 27));
        setPadding(14, 10, 14, 20);
        setSingleLine(false);
        setHorizontallyScrolling(true);
        setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        undo.push("");

        addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!internal) beforeChange = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (internal) return;

                String current = e.toString();

                if (!beforeChange.equals(current)) {
                    undo.push(current);
                    if (undo.size() > 100) undo.removeLast();
                    redo.clear();
                }

                highlight(e);
                autoCompletePairs(e);
                autoIndent(e);
            }
        });
    }

    private void autoCompletePairs(Editable e) {
        int pos = getSelectionStart();
        if (pos <= 0 || pos > e.length()) return;

        char c = e.charAt(pos - 1);
        String pair = null;

        if (c == '(') pair = ")";
        else if (c == '[') pair = "]";
        else if (c == '{') pair = "}";
        else if (c == '"') pair = "\"";
        else if (c == '\'') pair = "'";

        if (pair == null) return;

        if (pos < e.length() && e.charAt(pos) == pair.charAt(0)) return;

        internal = true;
        e.insert(pos, pair);
        setSelection(pos);
        internal = false;
    }

    private void autoIndent(Editable e) {
        int pos = getSelectionStart();
        if (pos <= 0 || pos > e.length()) return;

        if (pos < 1 || e.charAt(pos - 1) != '\n') return;

        int lineStart = e.toString().lastIndexOf('\n', pos - 2);
        lineStart = lineStart < 0 ? 0 : lineStart + 1;

        String previous = e.subSequence(lineStart, pos - 1).toString();

        int spaces = 0;
        while (spaces < previous.length() &&
                Character.isWhitespace(previous.charAt(spaces)) &&
                previous.charAt(spaces) != '\n') {
            spaces++;
        }

        String indent = previous.substring(0, spaces);

        if (previous.trim().endsWith(":")) {
            indent += "    ";
        }

        if (indent.length() == 0) return;

        internal = true;
        e.insert(pos, indent);
        setSelection(pos + indent.length());
        internal = false;
    }

    private void clearSpans(Editable e) {
        ForegroundColorSpan[] spans =
                e.getSpans(0, e.length(), ForegroundColorSpan.class);

        for (ForegroundColorSpan span : spans) {
            e.removeSpan(span);
        }
    }

    private void highlight(Editable e) {
        clearSpans(e);

        String text = e.toString();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '#') {
                int end = text.indexOf('\n', i);
                if (end < 0) end = text.length();

                e.setSpan(
                        new ForegroundColorSpan(Color.rgb(106, 153, 85)),
                        i, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                i = end;
                continue;
            }

            if (c == '"' || c == '\'') {
                char quote = c;
                int end = i + 1;

                while (end < text.length()) {
                    if (text.charAt(end) == '\\') {
                        end += 2;
                        continue;
                    }
                    if (text.charAt(end) == quote) {
                        end++;
                        break;
                    }
                    end++;
                }

                e.setSpan(
                        new ForegroundColorSpan(Color.rgb(206, 145, 120)),
                        i, Math.min(end, text.length()),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                i = end;
                continue;
            }

            if (Character.isDigit(c)) {
                int end = i + 1;

                while (end < text.length() &&
                        (Character.isDigit(text.charAt(end)) ||
                         text.charAt(end) == '.')) {
                    end++;
                }

                e.setSpan(
                        new ForegroundColorSpan(Color.rgb(181, 206, 168)),
                        i, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                i = end;
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                int end = i + 1;

                while (end < text.length() &&
                        (Character.isLetterOrDigit(text.charAt(end)) ||
                         text.charAt(end) == '_')) {
                    end++;
                }

                String word = text.substring(i, end);

                if (KEYWORDS.contains(word)) {
                    e.setSpan(
                            new ForegroundColorSpan(Color.rgb(86, 156, 214)),
                            i, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                i = end;
                continue;
            }

            i++;
        }
    }

    public void undoText() {
        if (undo.size() <= 1) return;

        String current = undo.pop();
        redo.push(current);

        String previous = undo.peek();
        if (previous == null) previous = "";

        internal = true;
        setText(previous);
        setSelection(getText().length());
        internal = false;
        highlight(getText());
    }

    public void redoText() {
        if (redo.isEmpty()) return;

        String next = redo.pop();
        undo.push(next);

        internal = true;
        setText(next);
        setSelection(getText().length());
        internal = false;
        highlight(getText());
    }

    public void setCode(String code) {
        internal = true;
        setText(code == null ? "" : code);
        setSelection(getText().length());
        undo.clear();
        redo.clear();
        undo.push(getText().toString());
        internal = false;
        highlight(getText());
    }
}
