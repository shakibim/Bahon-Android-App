package com.example.bahon;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainApp extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    HomeFragment homeFragment = new HomeFragment();
    ProfileFragment profileFragment = new ProfileFragment();
    HistoryFragment historyFragment = new HistoryFragment();
    WalletFragment walletFragment = new WalletFragment();
    MonitorFragment monitorFragment = new MonitorFragment(); // New fragment for Monitor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main_app);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, homeFragment).commit();
        bottomNavigationView.setItemRippleColor(null);  // Completely removes ripple
        Intent serviceIntent = new Intent(this, JourneyTrackingService.class);
        startService(serviceIntent);
        // Set up navigation item selected listener
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.home) {
                    selectedFragment = homeFragment;
                } else if (item.getItemId() == R.id.history) {
                    selectedFragment = historyFragment;
                } else if (item.getItemId() == R.id.wallet) {
                    selectedFragment = walletFragment;
                } else if (item.getItemId() == R.id.profile) {
                    selectedFragment = profileFragment;
                } else if (item.getItemId() == R.id.monitor) {
                    selectedFragment = monitorFragment;  // Open the monitor fragment
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, selectedFragment).commit();
                    return true;
                }
                return false;
            }
        });

        // Check user type (supervisor or customer) and update the menu
        checkUserTypeAndUpdateMenu();
    }

    private void checkUserTypeAndUpdateMenu() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        // Fetch user data to check user type
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userType = documentSnapshot.getString("userType");
                if ("supervisor".equals(userType)) {
                    // If the user is a supervisor, make the Monitor item visible
                    bottomNavigationView.getMenu().findItem(R.id.monitor).setVisible(true);
                }
            }
        });
    }
}
