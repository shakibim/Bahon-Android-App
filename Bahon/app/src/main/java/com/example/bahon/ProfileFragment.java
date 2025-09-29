package com.example.bahon;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView userNameTextView, phoneNumberTextView, emailTextView, addressTextView, cardNumberTextView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private Dialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        userNameTextView = view.findViewById(R.id.userName);
        phoneNumberTextView = view.findViewById(R.id.pNumber);
        emailTextView = view.findViewById(R.id.email);
        addressTextView = view.findViewById(R.id.addr);
        cardNumberTextView = view.findViewById(R.id.cardNumber);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Set up loading dialog
        setupLoadingDialog();

        // Load user data with loading dialog
        loadUserData();

        // Button setups
        setupButtons(view);

        return view;
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(requireContext());
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false); // Prevent closing by tapping outside
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
    }

    private void loadUserData() {
        loadingDialog.show(); // Show loading dialog

        String userId = auth.getCurrentUser().getUid();

        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingDialog.dismiss(); // Dismiss loading dialog
                    if (documentSnapshot.exists()) {
                        // Display data in TextViews
                        userNameTextView.setText(documentSnapshot.getString("name"));
                        phoneNumberTextView.setText(documentSnapshot.getString("phone"));
                        emailTextView.setText(documentSnapshot.getString("email"));
                        addressTextView.setText(documentSnapshot.getString("address"));
                        cardNumberTextView.setText(documentSnapshot.getString("card_no"));
                    } else {
                        Toast.makeText(getActivity(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss(); // Dismiss loading dialog
                    Toast.makeText(getActivity(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupButtons(View view) {
        Button logOutButton = view.findViewById(R.id.log_out_button);
        logOutButton.setOnClickListener(v -> {

            auth.signOut();
            showCustomToast("Logged Out Successfully", true);

            new Handler().postDelayed(() -> requireActivity().finish(), 1000);
        });

        Button editProfileButton = view.findViewById(R.id.edit_profile_button);
        Button reportIssuesButton = view.findViewById(R.id.report_button);

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfile.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        reportIssuesButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ReportIssues.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void showCustomToast(String message, boolean isSuccess) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int layoutId = isSuccess ? R.layout.toast_success : R.layout.toast_error;
        View layout = inflater.inflate(layoutId, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}