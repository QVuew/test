package com.upivoice;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UPINotificationListener extends NotificationListenerService {

    private static final String TAG = "UPIVoiceAlert";
    private static UPINotificationListener instance;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // UPI App package names
    private static final Set<String> UPI_PACKAGES = new HashSet<>(Arrays.asList(
        "com.google.android.apps.nbu.paisa.user",  // Google Pay
        "net.one97.paytm",                          // Paytm
        "com.phonepe.app",                          // PhonePe
        "in.org.npci.upiapp",                       // BHIM UPI
        "com.amazon.mShop.android.shopping",        // Amazon Pay
        "com.freecharge.android",                   // Freecharge
        "com.mobikwik_new",                         // MobiKwik
        "com.whatsapp",                             // WhatsApp Pay
        "com.axis.mobile",                          // Axis Bank
        "com.csam.icici.bank.imobile",              // ICICI iMobile
        "com.sbi.lotusintouch",                     // SBI YONO
        "com.snapwork.hdfc"                         // HDFC PayZapp
    ));

    // Keywords indicating RECEIVED money (not sent)
    private static final String[] RECEIVED_KEYWORDS = {
        "received", "credited", "credit", "added", "deposited",
        "पाया", "प्राप्त", "जमा", "मिला"  // Hindi keywords
    };

    // Keywords to EXCLUDE (sent/debited transactions)
    private static final String[] SENT_KEYWORDS = {
        "sent", "paid", "debited", "debit", "payment of", "you paid",
        "transferred to", "withdrawn"
    };

    // Regex patterns to extract amount
    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:Rs\\.?|INR|₹)"),
        Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:received|credited|credit)"),
        Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:received|credited)")
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initTTS();
        Log.d(TAG, "UPI Voice Alert Service Started");
    }

    public static UPINotificationListener getInstance() {
        return instance;
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("en", "IN")); // Indian English
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                }
                tts.setSpeechRate(0.85f); // Slightly slower for clarity
                ttsReady = true;
                Log.d(TAG, "TTS initialized successfully");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        String packageName = sbn.getPackageName();

        // Check if it's a UPI app
        if (!UPI_PACKAGES.contains(packageName)) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        // Extract notification text
        String title = "";
        String text = "";
        String bigText = "";

        if (notification.extras != null) {
            CharSequence titleSeq = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textSeq = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence bigTextSeq = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

            if (titleSeq != null) title = titleSeq.toString();
            if (textSeq != null) text = textSeq.toString();
            if (bigTextSeq != null) bigText = bigTextSeq.toString();
        }

        String fullText = (title + " " + text + " " + bigText).toLowerCase();
        Log.d(TAG, "Notification from " + packageName + ": " + fullText);

        // Check if money was RECEIVED (not sent)
        if (!isMoneyReceived(fullText)) return;

        // Extract amount
        String amount = extractAmount(title + " " + text + " " + bigText);
        if (amount == null) return;

        // Get app name
        String appName = getAppName(packageName);

        Log.d(TAG, "Money received! Amount: " + amount + " via " + appName);
        speakAmount(amount, appName);
    }

    private boolean isMoneyReceived(String text) {
        // First check if it's a sent transaction - exclude those
        for (String sentKeyword : SENT_KEYWORDS) {
            if (text.contains(sentKeyword)) return false;
        }

        // Then check for received keywords
        for (String receivedKeyword : RECEIVED_KEYWORDS) {
            if (text.contains(receivedKeyword)) return true;
        }

        return false;
    }

    private String extractAmount(String text) {
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String amount = matcher.group(1);
                if (amount != null) {
                    return amount.replace(",", ""); // Remove commas for TTS
                }
            }
        }
        return null;
    }

    public void speakAmount(String amount, String appName) {
        if (!ttsReady || tts == null) {
            initTTS();
            return;
        }

        // Format amount for natural speech
        String spokenText = formatAmountForSpeech(amount, appName);
        Log.d(TAG, "Speaking: " + spokenText);

        tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "upi_alert");
    }

    private String formatAmountForSpeech(String amount, String appName) {
        try {
            double value = Double.parseDouble(amount);

            String amountText;
            if (value == (long) value) {
                amountText = String.valueOf((long) value) + " rupees";
            } else {
                // Handle paise
                long rupees = (long) value;
                long paise = Math.round((value - rupees) * 100);
                if (paise > 0) {
                    amountText = rupees + " rupees and " + paise + " paise";
                } else {
                    amountText = rupees + " rupees";
                }
            }

            return amountText + " received via " + appName;
        } catch (NumberFormatException e) {
            return amount + " rupees received via " + appName;
        }
    }

    private String getAppName(String packageName) {
        switch (packageName) {
            case "com.google.android.apps.nbu.paisa.user": return "Google Pay";
            case "net.one97.paytm": return "Paytm";
            case "com.phonepe.app": return "PhonePe";
            case "in.org.npci.upiapp": return "BHIM";
            case "com.amazon.mShop.android.shopping": return "Amazon Pay";
            case "com.whatsapp": return "WhatsApp Pay";
            default: return "UPI";
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        instance = null;
        super.onDestroy();
    }
}
