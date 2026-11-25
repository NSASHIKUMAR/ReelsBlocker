package dev.local.reelsblocker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private TextView status;
    private Switch toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        status = findViewById(R.id.status);
        toggle = findViewById(R.id.toggle);

        toggle.setChecked(prefs.getBoolean("enabled", true));
        toggle.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("enabled", checked).apply();
            if (checked) prefs.edit().putLong("pausedUntil", 0).apply();
            refresh();
        });

        ((Button) findViewById(R.id.pause15)).setOnClickListener(v -> pauseFor(15 * 60 * 1000L));
        ((Button) findViewById(R.id.pause60)).setOnClickListener(v -> pauseFor(60 * 60 * 1000L));

        ((Button) findViewById(R.id.openAccessibility)).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        ((Button) findViewById(R.id.openAppInfo)).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
    }

    private void pauseFor(long ms) {
        prefs.edit().putLong("pausedUntil", System.currentTimeMillis() + ms).apply();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean enabled = prefs.getBoolean("enabled", true);
        long pausedUntil = prefs.getLong("pausedUntil", 0);
        int count = prefs.getInt("blockedCount", 0);

        String state;
        if (!enabled) {
            state = "Blocking is OFF.";
        } else if (System.currentTimeMillis() < pausedUntil) {
            long mins = (pausedUntil - System.currentTimeMillis()) / 60000 + 1;
            state = "Paused for about " + mins + " more minutes.";
        } else {
            state = "Blocking is ON.";
        }
        status.setText(state + "\nReels and Shorts closed so far: " + count);
    }
}
