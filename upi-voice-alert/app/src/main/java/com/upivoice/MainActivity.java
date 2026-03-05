package com.upivoice;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private ImageView statusIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        statusIcon = findViewById(R.id.statusIcon);
        Button grantButton = findViewById(R.id.grantButton);
        Button testButton = findViewById(R.id.testButton);

        grantButton.setOnClickListener(v -> {
            if (!isNotificationServiceEnabled()) {
                showPermissionDialog();
            } else {
                updateStatus(true);
            }
        });

        testButton.setOnClickListener(v -> {
            // Test TTS
            UPINotificationListener listener = UPINotificationListener.getInstance();
            if (listener != null) {
                listener.speakAmount("500", "GPay");
            } else {
                // Start service if not running
                new android.speech.tts.TextToSpeech(this, status -> {
                    if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                        android.speech.tts.TextToSpeech tts = new android.speech.tts.TextToSpeech(this, null);
                        tts.speak("Test: 500 rupees received via GPay", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus(isNotificationServiceEnabled());
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && pkgName.equals(cn.getPackageName())) return true;
            }
        }
        return false;
    }

    private void updateStatus(boolean enabled) {
        if (enabled) {
            statusText.setText("✅ Active - Listening for UPI payments");
            statusText.setTextColor(getColor(android.R.color.holo_green_dark));
            statusIcon.setImageResource(R.drawable.ic_active);
        } else {
            statusText.setText("❌ Permission needed - Tap 'Grant Permission'");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
            statusIcon.setImageResource(R.drawable.ic_inactive);
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs Notification Access to listen for UPI payment alerts.\n\nIn the next screen:\n1. Find 'UPI Voice Alert'\n2. Turn it ON\n3. Tap Allow")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
