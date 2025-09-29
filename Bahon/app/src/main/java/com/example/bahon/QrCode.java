package com.example.bahon;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrCode extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        // Set up Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set status bar and navigation bar colors
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        // Apply insets to the root view with ID "main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize loading dialog
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Find the ImageView for the QR code
        ImageView qrCodeImageView = findViewById(R.id.qr_code_image);

        // Show loading dialog before fetching the data
        showLoading();

        // Fetch the user card number from Firebase
        fetchCardNumberAndGenerateQRCode(qrCodeImageView);
    }

    private void fetchCardNumberAndGenerateQRCode(ImageView qrCodeImageView) {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cardNumber = documentSnapshot.getString("card_no");
                        if (cardNumber != null) {
                            // Generate and set the QR code using the fetched card number
                            Bitmap qrCodeBitmap = generateQRCode(cardNumber);
                            if (qrCodeBitmap != null) {
                                qrCodeImageView.setImageBitmap(qrCodeBitmap);
                            } else {
                                Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Card number not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                    hideLoading();  // Hide loading dialog after operation is complete
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                    hideLoading();  // Hide loading dialog on failure
                });
    }

    // Method to generate QR code
    private Bitmap generateQRCode(String text) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            int width = 300;
            int height = 300;
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Show the loading dialog
    private void showLoading() {
        loadingDialog.show();
    }

    // Hide the loading dialog
    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
