package com.example.bahon;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnBoardPassenger extends AppCompatActivity {
    private long lastToastTime = 0; // To track last toast time
    private static final long TOAST_DELAY = 500; // 2 seconds delay to prevent multiple toasts
    private ListenerRegistration passengerListener;

    private ListView passengerListView;
    private PassengerAdapter adapter;
    private List<Passenger> passengerList;
    private Map<String, Boolean> previousPassengerStates = new HashMap<>();
    private Dialog loadingDialog;
    private ImageButton btnScanQr;
    private boolean isFirstLoad = true; // Flag to track first load

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_board_passenger);
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        isFirstLoad = true; // Reset the flag
        setupBackNavigation();
        adjustInsets();
        initializeUI();
        showLoading();
        fetchPassengers();
    }

    private void initializeUI() {
        btnScanQr = findViewById(R.id.qr_code_button);
        btnScanQr.setOnClickListener(v -> startActivity(new Intent(OnBoardPassenger.this, ScanQrCode.class)));

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        passengerListView = findViewById(R.id.passengerListView);
        passengerList = new ArrayList<>();
        adapter = new PassengerAdapter(this, passengerList);
        passengerListView.setAdapter(adapter);
    }

    private void fetchPassengers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove any existing listener before adding a new one
        if (passengerListener != null) {
            passengerListener.remove();
        }

        passengerListener = db.collection("users")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("Firebase", "Listen failed.", e);
                        hideLoading();
                        return;
                    }

                    Map<String, Boolean> newPassengerStates = new HashMap<>();
                    List<Passenger> newPassengerList = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String uid = document.getId();
                        String name = document.getString("name");
                        String cardNo = document.getString("card_no");
                        Boolean onJourney = document.getBoolean("onjourney");

                        if (onJourney == null) continue;

                        newPassengerStates.put(uid, onJourney);
                        if (onJourney) {
                            Passenger passenger = new Passenger(name, cardNo, onJourney);
                            newPassengerList.add(passenger);
                        }
                    }

                    checkForStatusChanges(newPassengerStates);

                    passengerList.clear();
                    passengerList.addAll(newPassengerList);
                    adapter.notifyDataSetChanged();

                    previousPassengerStates = newPassengerStates;
                    hideLoading();
                });
    }
    private void checkForStatusChanges(Map<String, Boolean> newPassengerStates) {
        if (isFirstLoad) {
            // Skip showing toasts on the first load
            isFirstLoad = false;
            return;
        }

        for (Map.Entry<String, Boolean> entry : newPassengerStates.entrySet()) {
            String uid = entry.getKey();
            Boolean newStatus = entry.getValue();

            if (previousPassengerStates.containsKey(uid)) {
                Boolean oldStatus = previousPassengerStates.get(uid);
                if (!oldStatus.equals(newStatus)) {
                    Log.d("Status Change", "UID: " + uid + " Status changed from " + oldStatus + " to " + newStatus);
                    showCustomToast(uid, newStatus);
                }
            } else if (newStatus) {
                // New passenger on journey
                Log.d("Status Change", "New passenger UID: " + uid + " Status: " + newStatus);
                showCustomToast(uid, newStatus);
            }
        }
    }

    private void showCustomToast(String uid, boolean isOnJourney) {
        Log.d("ShowToast", "Showing toast for UID: " + uid + ", isOnJourney: " + isOnJourney);

        // Check if enough time has passed before showing a new toast
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToastTime < TOAST_DELAY) {
            Log.d("Toast", "Skipping toast due to rapid successive calls.");
            return;
        }

        lastToastTime = currentTime; // Update the last toast time

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    if (name == null) return;

                    String message = isOnJourney ? name + " Started Journey" : name + " Exited Journey";
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        Log.d("Handler", "Posting toast to UI thread");

                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        if (inflater == null) return;

                        int layoutId = isOnJourney ? R.layout.toast_success : R.layout.toast_exit;
                        View layout = inflater.inflate(layoutId, null);
                        TextView text = layout.findViewById(R.id.toast_text);
                        text.setText(message);

                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.BOTTOM, 0, 300);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setView(layout);
                        toast.show();

                        Log.d("Toast", "Toast shown: " + message);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching user data", e);
                });
    }


    private void showLoading() {
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void setupBackNavigation() {
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void adjustInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    protected void onDestroy() {
        super.onDestroy();
        if (passengerListener != null) {
            passengerListener.remove();
            passengerListener = null;
        }
    }
}
