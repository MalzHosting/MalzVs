package com.malz.vs;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private EditText editor;
    private TextView fileName;
    private Uri currentFile;

    private static final int OPEN_FILE = 100;
    private static final int SAVE_FILE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(24, 24, 24));
        window.setNavigationBarColor(Color.rgb(24, 24, 24));

        buildUI();
    }

    private void buildUI() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(30, 30, 30));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(12, 8, 12, 8);
        top.setBackgroundColor(Color.rgb(37, 37, 38));

        fileName = new TextView(this);
        fileName.setText("Malz VS");
        fileName.setTextColor(Color.WHITE);
        fileName.setTextSize(16);
        fileName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(fileName, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        Button open = button("OPEN");
        Button save = button("SAVE");
        Button newFile = button("NEW");

        top.addView(newFile);
        top.addView(open);
        top.addView(save);

        root.addView(top);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        TextView lineNumbers = new TextView(this);
        lineNumbers.setText("1");
        lineNumbers.setTextColor(Color.rgb(110, 110, 110));
        lineNumbers.setTextSize(14);
        lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT);
        lineNumbers.setPadding(8, 12, 8, 0);
        lineNumbers.setTypeface(Typeface.MONOSPACE);

        body.addView(lineNumbers, new LinearLayout.LayoutParams(
                45,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        editor = new EditText(this);
        editor.setTextColor(Color.rgb(220, 220, 220));
        editor.setHintTextColor(Color.rgb(100, 100, 100));
        editor.setHint("Start coding...");
        editor.setTextSize(14);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setBackgroundColor(Color.rgb(30, 30, 30));
        editor.setPadding(8, 12, 8, 12);
        editor.setSingleLine(false);
        editor.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        body.addView(editor, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));

        root.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(root);

        newFile.setOnClickListener(v -> newFile());
        open.setOnClickListener(v -> openFile());
        save.setOnClickListener(v -> saveFile());
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    private void newFile() {
        currentFile = null;
        fileName.setText("Untitled");
        editor.setText("");
        editor.requestFocus();
        Toast.makeText(this, "File baru", Toast.LENGTH_SHORT).show();
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_FILE);
    }

    private void saveFile() {
        if (currentFile == null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "untitled.txt");
            startActivityForResult(intent, SAVE_FILE);
            return;
        }

        writeFile(currentFile);
    }

    private void readFile(Uri uri) {

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            getContentResolver().openInputStream(uri)
                    )
            );

            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();

            editor.setText(content.toString());
            currentFile = uri;

            String name = uri.getLastPathSegment();
            if (name == null) name = "File";

            fileName.setText(name);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Gagal membuka file",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void writeFile(Uri uri) {

        try {
            OutputStream output =
                    getContentResolver().openOutputStream(uri);

            output.write(
                    editor.getText().toString().getBytes()
            );

            output.close();

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
            writeFile(uri);

            String name = uri.getLastPathSegment();
            if (name == null) name = "File";

            fileName.setText(name);
        }
    }
}
