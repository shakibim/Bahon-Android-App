package com.example.bahon;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class JourneyTrackingService extends Service {
    private static final String CHANNEL_ID = "JourneyTrackingServiceChannel";
    private Boolean lastJourneyState = null; // Tracks previous onjourney state
    private boolean isFirstUpdate = true;
    private FirebaseFirestore db;
    private ListenerRegistration journeyListener;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseAuth.AuthStateListener authStateListener;
    private boolean isJourneyStarted = false; // Keeps track of journey state

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Listen for authentication state changes (login/logout)
        authStateListener = firebaseAuth -> {
            FirebaseUser newUser = firebaseAuth.getCurrentUser();

            if (newUser == null) {
                Log.e("JourneyTrackingService", "User logged out, stopping service...");
                stopSelf();
            } else if (currentUser == null || !newUser.getUid().equals(currentUser.getUid())) {
                Log.i("JourneyTrackingService", "User changed, restarting tracking...");
                currentUser = newUser;
                setupJourneyTracking();
            }
        };

        auth.addAuthStateListener(authStateListener);
        setupJourneyTracking();
    }

    private void setupJourneyTracking() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        if (journeyListener != null) {
            journeyListener.remove();
        }

        journeyListener = db.collection("users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("JourneyTrackingService", "Firestore Error: " + error.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Boolean onJourney = snapshot.getBoolean("onjourney");

                        // Ignore first update on login to prevent toast
                        if (isFirstUpdate) {
                            lastJourneyState = onJourney;
                            isFirstUpdate = false;
                            return;
                        }

                        // Show toast only if onjourney state changes
                        if (onJourney != null && !onJourney.equals(lastJourneyState)) {
                            if (onJourney) {
                                showCustomToast("Your journey has started!", true);
                            } else {
                                showCustomToast("Your journey has ended!", false);
                                fetchLastJourney(userId);
                            }
                            lastJourneyState = onJourney; // Update the last known state
                        }
                    }
                });
    }

    private void fetchLastJourney(String userId) {
        db.collection("users").document(userId)
                .collection("journeys")
                .orderBy("entry_time._seconds", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            showJourneyEndPopup(document);
                            return;
                        }
                    }
                });
    }

    private void showJourneyEndPopup(DocumentSnapshot journeyData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(this)) {
            Log.e("JourneyTrackingService", "Overlay permission not granted!");
            return;
        }

        String entryPoint = journeyData.getString("entry_point");
        String exitPoint = journeyData.getString("exit_point");
        double distance = journeyData.getDouble("distance_travelled_km");
        double fare = journeyData.getDouble("fare");
        double farePerKm = journeyData.getDouble("fpkm");
        Long entrySeconds = journeyData.getLong("entry_time._seconds");
        Date exitDate = journeyData.getDate("exit_time");

        new Handler(Looper.getMainLooper()).post(() -> {
            Dialog dialog = new Dialog(getApplicationContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_journey_end);
            dialog.setCancelable(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            } else {
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }

            Typeface robotoBold = ResourcesCompat.getFont(this, R.font.roboto_bold);
            TextView entryValue = dialog.findViewById(R.id.entry_value);
            TextView exitValue = dialog.findViewById(R.id.exit_value);
            TextView distanceLabel = dialog.findViewById(R.id.distance_label);
            TextView distanceValue = dialog.findViewById(R.id.distance_value);
            TextView fareLabel = dialog.findViewById(R.id.fare_label);
            TextView fareValue = dialog.findViewById(R.id.fare_value);
            TextView durationValue = dialog.findViewById(R.id.duration_value);
            TextView farePerKmLabel = dialog.findViewById(R.id.fare_per_km_label);
            TextView farePerKmValue = dialog.findViewById(R.id.fare_per_km_value);
            Button okButton = dialog.findViewById(R.id.ok_button);

// Setting values dynamically
            SpannableString entryText = new SpannableString("Entry: " + entryPoint+" at "+formatTimestamp(entrySeconds));
            entryText.setSpan(new ForegroundColorSpan(Color.parseColor("#1976D2")), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            entryText.setSpan(new CustomTypefaceSpan(robotoBold), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            SpannableString exitText = new SpannableString("Exit: " + exitPoint+ " at "+formatExitTime(exitDate));
            exitText.setSpan(new ForegroundColorSpan(Color.parseColor("#1976D2")), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            exitText.setSpan(new CustomTypefaceSpan(robotoBold), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Bold style
            entryValue.setText(entryText);
            exitValue.setText(exitText);
            durationValue.setText(calculateDuration(entrySeconds, exitDate));
            distanceValue.setText(String.format("%.6f km", distance));
            fareValue.setText(String.format("%.2f BDT", fare));
            farePerKmValue.setText(String.format("%.2f BDT/km", farePerKm));

            okButton.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        });
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }
        if (journeyListener != null) {
            journeyListener.remove();
        }
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Journey Tracking Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Journey Tracking")
                .setContentText("Tracking your journey...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    private void showCustomToast(String message, boolean isSuccess) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater == null) return;
            int layoutId = isSuccess ? R.layout.toast_success : R.layout.toast_exit;
            View layout = inflater.inflate(layoutId, null);
            TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);
            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        });
    }

    private void setSpanColor(SpannableString spannable, String target, int color) {
        int start = spannable.toString().indexOf(target);
        if (start != -1) {
            spannable.setSpan(new ForegroundColorSpan(color), start, start + target.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private String formatTimestamp(Long seconds) {
        Date date = new Date(seconds * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatExitTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());
        return sdf.format(date);
    }

    public static String calculateDuration(Long entrySeconds, Date exitDate) {
        if (entrySeconds == null || exitDate == null) {
            return "Invalid time data";
        }

        // Convert entry time to milliseconds
        long entryMillis = entrySeconds * 1000;
        long exitMillis = exitDate.getTime();

        // Calculate duration in milliseconds
        long durationMillis = exitMillis - entryMillis;

        if (durationMillis < 0) {
            return "Invalid time range"; // Entry time cannot be after exit time
        }

        // Convert duration to hours, minutes, and seconds
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;

        // Build the duration string
        StringBuilder durationString = new StringBuilder();
        if (hours > 0) {
            durationString.append(hours).append(" hour");
            if (hours > 1) durationString.append("s");
            durationString.append(" ");
        }
        if (minutes > 0) {
            durationString.append(minutes).append(" min ");
        }
        durationString.append(seconds).append(" sec");

        return durationString.toString().trim();
    }
}
