package com.example.bahon;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView userGreetingTextView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Dialog loadingDialog;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadingDialog = new Dialog(requireContext());
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Initialize the greeting TextView
        userGreetingTextView = view.findViewById(R.id.greeting_text);
        checkUserOnboardStatus(view);
        // Set status bar and navigation bar colors
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
            getActivity().getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));
        }

        // Fetch user data from Firestore and update the greeting text
        ExecutorService executorService;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            fetchUserName();
            fetchUserBalance();
            fetchLastJourney();

            // Update the UI on the main thread after the background tasks are done
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Perform UI updates here (for example, show the loading dialog)
                    loadingDialog.show();
                });
            }
        });


        // Initialize buttons and set OnClickListeners as before (no changes here)
        Button qrcodebtn = view.findViewById(R.id.generate_qr_button);
        qrcodebtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), QrCode.class));
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        Button calculateFare = view.findViewById(R.id.calculate_fare_button);
        calculateFare.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CalculateFare2.class));
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        LinearLayout currentJourneyCard = view.findViewById(R.id.currentJourneyCard);
        currentJourneyCard.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CurrentJourney.class));
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        LinearLayout lastJourneyCard = view.findViewById(R.id.lastJourneyCard);
        lastJourneyCard.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LstJourney.class));
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        ImageView notification = view.findViewById(R.id.notification_icon);
        notification.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LocationActivity.class));
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Fetch total spend and total journeys in last 24 hours
        getTotalSpendAndJourneys();

        return view;
    }
    private void showLoading() {
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    // Fetch the user's name from Firestore
    private void fetchUserName() {
        String userId = auth.getCurrentUser().getUid(); // Get the current user ID

        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    // Fetch the username from Firestore
                    String username = document.getString("name");
                    if (username != null && userGreetingTextView != null) {
                        // Update the greeting TextView with the fetched username
                        userGreetingTextView.setText("Hello, " + username + "!\nWelcome to Bahon");
                    }
                }
            }
        });

    }

    // Function to get total spend and total journeys in the last 24 hours
    private void getTotalSpendAndJourneys() {
        String userId = auth.getCurrentUser().getUid();

        // Calculate the timestamp for 24 hours ago
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000); // 24 hours in milliseconds

        // Reference to the 'journeys' subcollection for the logged-in user
        CollectionReference journeysRef = db.collection("users").document(userId).collection("journeys");

        // Query to get all journeys from the last 24 hours
        Query recentJourneysQuery = journeysRef.whereGreaterThan("exit_time", new Date(twentyFourHoursAgo));

        // Execute the query
        recentJourneysQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                double totalSpend = 0;
                int journeyCount = 0;

                // Loop through the results to calculate total spend and journey count
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Double fare = document.getDouble("fare");
                    if (fare != null) {
                        totalSpend += fare;
                    }
                    journeyCount++;
                }

                // Update the UI with the total spend and number of journeys
                updateUI(totalSpend, journeyCount);
            } else {
                // Handle error
                updateUI(0, 0);
            }
        });
    }

    // Function to update the UI with the total spend and total number of journeys
    private void updateUI(double totalSpend, int journeyCount) {
        View view = getView();
        if (view != null) {
            TextView totalSpendTextView = view.findViewById(R.id.totalspend);
            TextView totalJourneysTextView = view.findViewById(R.id.totalJourneyin24h);

            if (totalSpendTextView != null && totalJourneysTextView != null) {
                // Format the total spend and number of journeys
                totalSpendTextView.setText("৳ " + String.format("%.2f", totalSpend));
                totalJourneysTextView.setText(journeyCount + " Journeys in Last 24 Hours");
            }
        }
    }

    // Fetch the user's bonus and balance from Firestore
    private void fetchUserBalance() {

        String userId = auth.getCurrentUser().getUid(); // Get the current user ID

        // Reference to the user's document
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    // Fetch the bonus and balance from Firestore
                    Double bonus = document.getDouble("bonus");
                    Double balance = document.getDouble("balance");
                    if (balance == null) {
                        balance = 0.00;
                    }
                    if (bonus != null && bonus != 0) {
                        // If bonus is not zero, update the UI to show bonus balance
                        updateBalanceUI("Bonus", bonus);
                    } else {
                        // Otherwise, display wallet balance
                        updateBalanceUI("Wallet Balance", balance);
                    }
                }
            }
        });

    }

    // Update the UI with the balance or bonus
    private void updateBalanceUI(String label, Double amount) {
        TextView balanceLabel = getView().findViewById(R.id.wbbalance);
        TextView balanceTextView = getView().findViewById(R.id.balance);

        // Format the amount to two decimal places
        String formattedAmount = String.format("%.2f", amount);

        // Update the label and balance text
        balanceLabel.setText(label);
        balanceTextView.setText("৳ " + formattedAmount);
    }

    private void fetchLastJourney() {

        String userId = auth.getCurrentUser().getUid(); // Get the current user ID

        // Reference to the 'journeys' subcollection for the logged-in user
        CollectionReference journeysRef = db.collection("users").document(userId).collection("journeys");

        // Query to get the most recent journey
        journeysRef.orderBy("entry_time._seconds", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        // Check if the result contains any documents
                        if (!task.getResult().isEmpty()) {
                            // Get the most recent journey document
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);

                            // Extract the journey details
                            String entryPoint = document.getString("entry_point");
                            String exitPoint = document.getString("exit_point");
                            Double fare = document.getDouble("fare");
                            Long timestamp = document.getLong("entry_time._seconds");

                            // If data is available, proceed with formatting and updating the UI
                            if (entryPoint != null && exitPoint != null && fare != null && timestamp != null) {
                                // Format entry and exit points to show 12 characters max with "..."
                                String formattedEntryPoint = formatPoint(entryPoint);
                                String formattedExitPoint = formatPoint(exitPoint);

                                // Calculate the time passed since the journey
                                String timePassed = calculateTimePassed(timestamp);

                                // Update the UI with the details
                                updateJourneyUI(formattedEntryPoint, formattedExitPoint, timePassed, fare);
                            } else {
                                // Handle case where one or more values are null
                                updateJourneyUI("Data incomplete", "Incomplete Data", "N/A", 0.0);
                            }
                        } else {
                            // Handle case where no journey data is found
                            updateJourneyUI("No journey found", "No Data", "N/A", 0.0);
                        }
                    } else {
                        // Handle task failure
                        Exception e = task.getException();
                        if (e != null) {
                            Log.e("FetchJourneyError", "Error fetching data: " + e.getMessage());
                        }
                        // Update UI with an error message
                        updateJourneyUI("Error fetching data", "Error", "N/A", 0.0);
                    }
                });

    }

    private String formatPoint(String point) {
        if (point != null && point.length() > 12) {
            return point.substring(0, 12) + "...";
        }
        return point != null ? point : "Unknown";
    }

    private String calculateTimePassed(long timestamp) {
        long currentTime = System.currentTimeMillis()/1000;
        long timeDifference = currentTime - timestamp;
        long seconds = timeDifference;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day(s) ago";
        } else if (hours > 0) {
            return hours + " hour(s) ago";
        } else if (minutes > 0) {
            return minutes + " minute(s) ago";
        } else {
            return seconds + " second(s) ago";
        }
    }
    // Function to update the UI with the fetched journey data
    private void updateJourneyUI(String entryPoint, String exitPoint, String timePassed, double fare) {
        View view = getView();
        if (view != null) {
            TextView startPointTextView = view.findViewById(R.id.start_point_last);
            TextView endPointTextView = view.findViewById(R.id.end_point_last);
            TextView fareTextView = view.findViewById(R.id.time_fare_text); // Assuming you have a fare TextView


            // Update the text views with the formatted data
            if (startPointTextView != null) {
                startPointTextView.setText("Start: " + entryPoint);
            }
            if (endPointTextView != null) {
                endPointTextView.setText("End: " + exitPoint);
            }
            if (fareTextView != null) {
                fareTextView.setText("Fare: ৳ " + String.format("%.2f", fare)+" "+timePassed);
            }

        }
    }

    private void checkUserOnboardStatus(View view) {
        // Get the logged-in user's UID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Reference to the user's document in Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection("users").document(userId);

            // Fetch the onboard value and other journey-related fields
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Boolean isOnboard = document.getBoolean("onjourney");
                        String startPoint = document.getString("startPoint");
                        String currentLocation = document.getString("currentLocation");
                        String lastUpdatedTime = document.getString("lastUpdatedTime");

                        if (isOnboard != null && isOnboard) {
                            // User is onboard, update the UI with retrieved data
                            updateJourneyUI(view, true,
                                    startPoint != null ? startPoint : "Johuru..",
                                    currentLocation != null ? currentLocation : "Johuru..",
                                    lastUpdatedTime != null ? lastUpdatedTime : "11:31:32 AM");
                        } else {
                            // User is not onboard, handle accordingly
                            updateJourneyUI(view, false, "", "", "");
                        }
                    } else {
                        Log.e("Firestore", "No such document");
                    }
                } else {
                    Log.e("FirestoreError", "Failed to read onboard status", task.getException());
                }
            });
        } else {
            Log.e("FirebaseAuth", "User is not logged in");
        }
    }
    private void updateJourneyUI(View view, boolean onJourney, String startPoint, String currentLocation, String lastUpdatedTime) {
        // Access the TextView elements from the provided 'view' object
        TextView startPointText = view.findViewById(R.id.start_point);
        TextView currentLocationText = view.findViewById(R.id.end_point);
        TextView lastUpdatedText = view.findViewById(R.id.last_updated);

        // Update the UI based on the 'onJourney' status
        if (onJourney) {
            startPointText.setText("Start Point: " + startPoint);
            currentLocationText.setText("Current Loc.: " + currentLocation);
            String updatedTime = lastUpdatedTime != null ? lastUpdatedTime : new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(new Date());
            lastUpdatedText.setText("Last Updated: " + updatedTime);

        } else {
            startPointText.setText("Start Point: N/A");
            currentLocationText.setText("Current Loc.: N/A");
            lastUpdatedText.setText("You Are Not on a Jounrney");
        }
    }


}
