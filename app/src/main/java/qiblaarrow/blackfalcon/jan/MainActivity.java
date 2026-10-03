package qiblaarrow.blackfalcon.jan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private RelativeLayout compassContainer;
    private TextView degreeTextView;
    private TextView distanceTextView;
    private TextView dmsCoordinatesTextView;

    private SensorManager sensorManager;
    private Sensor compassSensor;
    private LocationManager locationManager;

    private Location currentLocation;

    private static final double KAABA_LATITUDE = 21.422487;
    private static final double KAABA_LONGITUDE = 39.826206;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compassContainer = findViewById(R.id.compassContainer);
        degreeTextView = findViewById(R.id.degreeTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        dmsCoordinatesTextView = findViewById(R.id.dmsCoordinatesTextView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1001);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown != null) {
                    updateLocationData(lastKnown);
                }
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2, this);
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnown != null) {
                    updateLocationData(lastKnown);
                }
            }
        }
    }

    private void updateLocationData(Location location) {
        this.currentLocation = location;

        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), KAABA_LATITUDE, KAABA_LONGITUDE, results);
        float distanceInKm = results[0] / 1000f;
        distanceTextView.setText(String.format(Locale.US, "Distance: %,.0f km", distanceInKm));

        String latDMS = decimalToDMS(location.getLatitude(), true);
        String lonDMS = decimalToDMS(location.getLongitude(), false);
        dmsCoordinatesTextView.setText(String.format("GPS: %s  %s", latDMS, lonDMS));
    }

    private String decimalToDMS(double val, boolean isLatitude) {
        String direction;
        if (isLatitude) {
            direction = val >= 0 ? "N" : "S";
        } else {
            direction = val >= 0 ? "E" : "W";
        }

        val = Math.abs(val);
        int degrees = (int) val;
        val = (val - degrees) * 60;
        int minutes = (int) val;
        double seconds = (val - minutes) * 60;

        return String.format(Locale.US, "%d°%d'%.1f"%s", degrees, minutes, seconds, direction);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (compassSensor != null) {
            sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuth = Math.round(event.values[0]);

        if (currentLocation != null) {
            float qiblaBearing = calculateQiblaBearing(currentLocation.getLatitude(), currentLocation.getLongitude());
            float direction = azimuth - qiblaBearing;

            compassContainer.setRotation(-direction);
            degreeTextView.setText(Math.round(qiblaBearing) + "° Qibla");
        } else {
            compassContainer.setRotation(-azimuth);
            degreeTextView.setText(Math.round(azimuth) + "°");
        }
    }

    private float calculateQiblaBearing(double lat, double lon) {
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double lon2 = Math.toRadians(KAABA_LONGITUDE);

        double deltaLon = lon2 - lon1;

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        double bearing = Math.atan2(y, x);
        bearing = Math.toDegrees(bearing);
        return (float) ((bearing + 360) % 360);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onLocationChanged(@NonNull Location location) {
        updateLocationData(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Location permission required for Qibla direction", Toast.LENGTH_SHORT).show();
        }
    }
}
