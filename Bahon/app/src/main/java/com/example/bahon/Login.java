package com.example.bahon;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public class Login extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Switch switchRememberMe;
    private FirebaseAuth mAuth;
    private Dialog loadingDialog;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set navigation and status bar colors
        Window window = getWindow();
        window.setNavigationBarColor(getResources().getColor(R.color.blue));
        window.setStatusBarColor(getResources().getColor(R.color.blue));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        // Get references to UI elements
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        switchRememberMe = findViewById(R.id.switch_remember_me);

        // Load saved credentials
        loadSavedCredentials();

        // Set up custom loading dialog
        setupLoadingDialog();

        // Login Button
        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Register Button
        Button registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showCustomToast("Please fill in both email and password", false);
            return;
        }

        loadingDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        showCustomToast("Login successful!", true);

                        // Save credentials if switch is on
                        if (switchRememberMe.isChecked()) {
                            saveCredentials(email, password);
                        } else {
                            clearCredentials();
                        }

                        Intent intent = new Intent(Login.this, MainApp.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        showCustomToast("Authentication failed: " + task.getException().getMessage(), false);
                    }
                });
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.putBoolean("rememberMe", true);
        editor.apply();
    }

    private void loadSavedCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean("rememberMe", false);
        if (rememberMe) {
            String savedEmail = sharedPreferences.getString("email", "");
            String savedPassword = sharedPreferences.getString("password", "");
            emailEditText.setText(savedEmail);
            passwordEditText.setText(savedPassword);
            switchRememberMe.setChecked(true);
        }
    }

    private void clearCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("email");
        editor.remove("password");
        editor.remove("rememberMe");
        editor.apply();
    }

    private void showCustomToast(String message, boolean isSuccess) {
        LayoutInflater inflater = getLayoutInflater();
        int layoutId = isSuccess ? R.layout.toast_success : R.layout.toast_error;
        View layout = inflater.inflate(layoutId, findViewById(R.id.toast_text));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
