package com.malz.py;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(9, 11, 15));

        TextView logo = new TextView(this);
        logo.setText("M");
        logo.setTextColor(Color.rgb(255, 212, 59));
        logo.setTextSize(76);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        TextView name = new TextView(this);
        name.setText("MalzPy");
        name.setTextColor(Color.WHITE);
        name.setTextSize(25);
        name.setGravity(Gravity.CENTER);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        root.addView(logo, new LinearLayout.LayoutParams(-1, 100));
        root.addView(name, new LinearLayout.LayoutParams(-1, 60));

        setContentView(root);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 700);
    }
}
