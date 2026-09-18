package com.malz.py;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;

public class CodeEditor extends EditText {

    public CodeEditor(Context context) {
        super(context);
        init();
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CodeEditor(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        init();
    }

    private void init() {
        setTextColor(Color.rgb(225, 225, 225));
        setHintTextColor(Color.rgb(110, 110, 110));
        setTextSize(13);
        setTypeface(Typeface.MONOSPACE);

        setGravity(Gravity.TOP | Gravity.START);

        setBackgroundColor(Color.TRANSPARENT);

        setPadding(dp(8), dp(5), dp(8), dp(60));

        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setLongClickable(true);
        setEnabled(true);
        setCursorVisible(true);

        setTextIsSelectable(false);

        setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        setSingleLine(false);
        setMaxLines(Integer.MAX_VALUE);

        setHorizontallyScrolling(false);

        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        setSelectAllOnFocus(false);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        requestFocus();
        return true;
    }

    private int dp(int value) {
        return (int)(value *
                getResources().getDisplayMetrics().density + 0.5f);
    }
}
