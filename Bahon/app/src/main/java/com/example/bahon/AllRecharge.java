package com.example.bahon;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllRecharge extends AppCompatActivity {

    private ListView allRechargeListView;
    private FirebaseFirestore firestore;
    private RechargeAdapter adapter;
    private List<RechargeData> rechargeDataList;

    // Declare ProgressDialog
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_recharge);

        firestore = FirebaseFirestore.getInstance();
        allRechargeListView = findViewById(R.id.rechargeListView);
        rechargeDataList = new ArrayList<>();
        adapter = new RechargeAdapter(this, rechargeDataList);
        allRechargeListView.setAdapter(adapter);

        // Set up system bar colors
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        // Set up window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up back arrow functionality
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Initialize the custom loading dialog
        loadingDialog = new Dialog(AllRecharge.this);
        loadingDialog.setContentView(R.layout.dialog_loading); // Ensure you have created this layout
        loadingDialog.setCancelable(false); // Prevent dismissing it by tapping outside
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background

        // Load actual recharge history data
        loadRechargeHistory();
    }

    private void loadRechargeHistory() {
        // Show the loading dialog
        loadingDialog.show();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users").document(userId).collection("recharge_history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    rechargeDataList.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Long timestamp = doc.getLong("timestamp");
                        String date = timestamp != null ? sdf.format(new Date(timestamp)) : "N/A";

                        Double amount = doc.getDouble("amount");
                        String amountStr = amount != null ? String.format(Locale.getDefault(), "৳%.2f", amount) : "৳0.00";

                        rechargeDataList.add(new RechargeData(date, amountStr));
                    }

                    // Notify the adapter of data change to refresh the ListView
                    adapter.notifyDataSetChanged();

                    // Dismiss the loading dialog
                    loadingDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("AllRecharge", "Failed to load recharge history", e);
                    Toast.makeText(this, "Failed to load recharge history", Toast.LENGTH_SHORT).show();

                    // Dismiss the loading dialog in case of failure
                    loadingDialog.dismiss();
                });
    }
}
