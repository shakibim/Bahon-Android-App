package com.example.bahon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class LocationActivity extends AppCompatActivity {

    private Switch switchLocation;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LocationPrefs";
    private static final String KEY_SWITCH_STATE = "switchState";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));
        // Initialize the switch and shared preferences
        switchLocation = findViewById(R.id.switch_remember_me);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load the previous switch state from shared preferences
        boolean switchState = sharedPreferences.getBoolean(KEY_SWITCH_STATE, false);
        switchLocation.setChecked(switchState);

        // Set listener for switch state changes
        switchLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save the switch state in shared preferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_SWITCH_STATE, isChecked);
                editor.apply();

                // Start or stop the location service based on switch state
                Intent serviceIntent = new Intent(LocationActivity.this, LocationService.class);
                if (isChecked) {
                    startService(serviceIntent);
                } else {
                    stopService(serviceIntent);
                }
            }
        });
    }
}
