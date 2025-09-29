package com.example.bahon;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Random;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference currentLocationRef;
    private LocationCallback locationCallback;
    private Location firstLocation = null;
    private final Random random = new Random();
    private Handler handler = new Handler();
    private Runnable randomLocationRunnable;
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        currentLocationRef = FirebaseDatabase.getInstance().getReference("current_location_table");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            Log.e("LocationService", "Permissions not granted.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start foreground service
        startForeground(1, getNotification());

        LocationRequest locationRequest = new LocationRequest()
                .setInterval(5000)  // Get location every 5 seconds
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    for (Location location : locationResult.getLocations()) {
                        if (firstLocation == null) {
                            // Store the first location and start random updates
                            firstLocation = new Location(location);
                            sendLocationToFirebase(firstLocation);
                            isServiceRunning = true;
                            startRandomLocationUpdates(); // Start sending random locations
                            fusedLocationClient.removeLocationUpdates(locationCallback); // Stop real updates
                        }
                    }
                }
            }
        };

        // Start requesting location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        // Stop service if no initial location is received within 10 seconds
        handler.postDelayed(() -> {
            if (firstLocation == null) {
                Log.e("LocationService", "No initial location received, stopping service.");
                stopSelf();
            }
        }, 10000); // 10-second timeout

        return START_STICKY;
    }

    private void startRandomLocationUpdates() {
        randomLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning && firstLocation != null) {
                    Location randomLocation = generateRandomLocation(firstLocation);
                    sendLocationToFirebase(randomLocation);
                    handler.postDelayed(this, 5000); // Send every 5 seconds
                }
            }
        };
        handler.post(randomLocationRunnable); // Start the random location loop
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking Active")
                .setContentText("Your location is being sent to the server.")
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (handler != null && randomLocationRunnable != null) {
            handler.removeCallbacks(randomLocationRunnable);
        }
    }

    private Location generateRandomLocation(Location baseLocation) {
        double latitude = baseLocation.getLatitude();
        double longitude = baseLocation.getLongitude();

        // Convert meters to degrees
        double meterToDegree = 0.000009;
        double randomDistance = 0.2 + (random.nextDouble() * 0.2);
        double randomAngle = random.nextDouble() * 2 * Math.PI; // Random direction

        // Calculate new latitude and longitude
        double deltaLat = randomDistance * meterToDegree * Math.cos(randomAngle);
        double deltaLon = (randomDistance * meterToDegree / Math.cos(Math.toRadians(latitude))) * Math.sin(randomAngle);

        Location newLocation = new Location(baseLocation);
        newLocation.setLatitude(latitude + deltaLat);
        newLocation.setLongitude(longitude + deltaLon);

        return newLocation;
    }

    private void sendLocationToFirebase(Location location) {
        DatabaseReference newEntry = currentLocationRef.push();

        // Round latitude and longitude to 6 decimal places
        double roundedLat = Math.round(location.getLatitude() * 1_000_000.0) / 1_000_000.0;
        double roundedLon = Math.round(location.getLongitude() * 1_000_000.0) / 1_000_000.0;

        // Push the rounded latitude and longitude to Firebase
        newEntry.child("lat").setValue(roundedLat);
        newEntry.child("lon").setValue(roundedLon);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}