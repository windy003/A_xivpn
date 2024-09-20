package cn.gov.xivpn2.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.BuildConfig;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("App Crashed");
        }

        String exception = getIntent().getStringExtra("EXCEPTION");
        if (exception == null) {
            exception = "NULL EXCEPTION";
        }

        StringBuilder sb = getMessage(exception);

        TextView textView = findViewById(R.id.textview);
        textView.setText(sb.toString());
        textView.setHorizontallyScrolling(true);

        Button btn = findViewById(R.id.btn_report);
        btn.setOnClickListener(v -> {
            try {
                String uriString = "https://github.com/Exclude0122/xivpn/issues/new?title=Crash&body=";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                Log.e("CrashActivity", "open browser", e);
            }
        });
    }

    private static @NonNull StringBuilder getMessage(String exception) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please report this issue to the developers.\n");
        sb.append("Android Release: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("App Version: ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n");
        sb.append("\n");

        sb.append("Exception:\n");
        sb.append(exception);

        return sb;
    }
}