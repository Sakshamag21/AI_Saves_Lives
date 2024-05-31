package com.example.myapplication;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;



public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float SUDDEN_DECREASE_THRESHOLD = 10.0f; // Threshold in km/h
    private static final String CHANNEL_ID = "location_alerts";
    private LocationManager locationManager;
    private TextView locationTextView;
    private TextView speedTextView;
    private LocationListener locationListener;
    private Location previousLocation;
    private long previousTime;
    private float previousSpeedKmH;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        speedTextView = findViewById(R.id.speedTextView);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();

        // Define the LocationListener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                float speed = location.getSpeed(); // Speed in meters/second

                // Manually calculate speed if not available from location object
                if (speed == 0.0f && previousLocation != null) {
                    long currentTime = System.currentTimeMillis();
                    float distance = location.distanceTo(previousLocation);
                    float timeDelta = (currentTime - previousTime) / 1000.0f; // Convert milliseconds to seconds
                    speed = distance / timeDelta; // Speed in meters/second
                }

                // Update previous location and time
                previousLocation = location;
                previousTime = System.currentTimeMillis();

                // Convert speed to km/h
                float currentSpeedKmH = speed * 3.6f;

                // Check for sudden decrease in speed
                if (previousSpeedKmH > 0 && previousSpeedKmH - currentSpeedKmH >= SUDDEN_DECREASE_THRESHOLD) {
                    Log.d("Speed", "Sudden decrease detected! Previous speed: " + previousSpeedKmH + " km/h, Current speed: " + currentSpeedKmH + " km/h");
                    sendNotification("Sudden Decrease Detected", "Previous speed: " + previousSpeedKmH + " km/h, Current speed: " + currentSpeedKmH + " km/h");
                }

                // Update previous speed
                previousSpeedKmH = currentSpeedKmH;

                // Update TextViews with location and speed
                String locationString = "Latitude: " + latitude + "\nLongitude: " + longitude;
                String speedString = "Speed: " + currentSpeedKmH + " km/h";

                runOnUiThread(() -> {
                    locationTextView.setText(locationString);
                    speedTextView.setText(speedString);
                });

                Log.d("Location", locationString);
                Log.d("Speed", speedString);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Check and request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, request location updates
            requestLocationUpdates();
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted
            requestLocationUpdates();
        } else {
            Log.d("Location", "Location permission denied");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Alerts";
            String description = "Notifications for sudden decrease in speed";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Make sure to have a notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
