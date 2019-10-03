package com.example.location;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_LIGHT_STORAGE = 5000;
    private static final float RADIUS = 30;


    private Location currentLocation;
    private Geocoder geocoder;
    private Location lastLocation;
    private List<Float> lightValues;
    private Sensor lightSensor;
    private SensorEventListener lightSensorListener;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Resources res;
    private SensorManager sensorManager;

    private static String sumDesc = "";
    private static String sumLastDesc = "";
    private static String sumContent = "";
    private static String sumDistance = "";

    private TextView desc;
    private TextView light;
    private TextView lastDesc;
    private TextView lastLight;
    private TextView content;
    private TextView distanceText;


    private final String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public MainActivity() {
        super();
        this.lightValues = new LinkedList<>();
        return;
    }

    private Float computeAverage(List<Float> values) {
        Float sum = 0f;
        if (!values.isEmpty()) {
            for (Float value : values) {
                sum += value;
            }
            return sum / values.size();
        }
        return sum;
    }

    private String computeLocationName(Location loc) {
        try {
            final List<Address> addresses = this.geocoder.getFromLocation(loc.getLatitude(),
                    loc.getLongitude(), 1);
            if (addresses.size() > 0) {
                return addresses.get(0).getAddressLine(0);
            }
        }
        catch (IOException e) {
            Log.e("LOCATION", "Could not get location!");
            e.printStackTrace();
        }
        catch (Exception e) {
            Log.e("LOCATION", "Could not get location!");
            e.printStackTrace();
        }
        return "NaN";
    }

    private void resetLastLocation(Location loc) {
        this.lastLocation = null;
        if (loc != null) {
            this.lastLocation = new Location("Point A");
            this.lastLocation.setAltitude(loc.getAltitude());
            this.lastLocation.setLatitude(loc.getLatitude());
            this.lastLocation.setLongitude(loc.getLongitude());
        }
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.currentLocation = new Location("Point B");
        this.geocoder = new Geocoder(this, Locale.getDefault());
        this.locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        this.res = getResources();
        this.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
        if (sensorList.size() > 0) {
            this.lightSensor = sensorList.get(0);
        }

        this.desc = (TextView)findViewById(R.id.desc);
        this.light = (TextView)findViewById(R.id.light);
        this.lastDesc = (TextView)findViewById(R.id.lastDesc);
        this.lastLight = (TextView)findViewById(R.id.lastLight);
        this.content = (TextView)findViewById(R.id.content);
        this.distanceText = (TextView)findViewById(R.id.dist);


        if (ContextCompat.checkSelfPermission(this, this.PERMISSIONS[0])
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, this.PERMISSIONS[1])
                        != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    this.PERMISSIONS[0])
                    && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    this.PERMISSIONS[1])) {
                Log.w("PERMISSION", "Requesting permissions!");
            } else {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
                Log.w("PERMISSION", "Requesting permissions!");
            }
        }

        this.lightSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                final float lightVal = event.values[0];

                // To prevent out of memory?
                if (lightValues.size() > MAX_LIGHT_STORAGE) {
                    lightValues.clear();
                    Log.i("NOGA", "Clearing list of light values for memory!");
                }

                lightValues.add(lightVal);
                light.setText("Light: " + lightVal + " lux");

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                return;
            }
        };

        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                final double latitude = location.getLatitude();
                final double longitude = location.getLongitude();
                final double altitude = location.getAltitude();

                currentLocation.setLongitude(longitude);
                currentLocation.setLatitude(latitude);
                currentLocation.setAltitude(altitude);

                final String locationName = computeLocationName(currentLocation);
                sumDesc = "Longitude: " + longitude + "\n"
                        + "Latitude: " + latitude + "\n"
                        + "Altitude: " + altitude + "\n"
                        + "Location Name: " + locationName;
                desc.setText(sumDesc);

                if(lastLocation == null) {
                    resetLastLocation(currentLocation);
                    lastLight.setText("Last Light " + 0);
                }

                final double lastLongitude = lastLocation.getLongitude();
                final double lastLatitude = lastLocation.getLatitude();
                final double lastAltitude = lastLocation.getAltitude();
                final String lastLocationName = computeLocationName(lastLocation);

                sumLastDesc = "Last Longitude: " + lastLongitude + "\n"
                        + "Last Latitude: " + lastLatitude + "\n"
                        + "Last Altitude: " + lastAltitude + "\n"
                        + "Last Location Name: " + lastLocationName;
                lastDesc.setText(sumLastDesc);

                float distance = lastLocation.distanceTo(currentLocation);

                if(distance >= RADIUS) {
                    distance = RADIUS;
                    resetLastLocation(currentLocation);
                    sumContent += sumLastDesc + "\n"
                            + computeAverage(lightValues) + " lux";
                    content.setText(sumContent);
                    lightValues.clear();
                }

                sumDistance = "Distance: " + distance + " m";
                distanceText.setText(sumDistance);


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                return;
            }

            @Override
            public void onProviderEnabled(String provider) {
                return;
            }

            @Override
            public void onProviderDisabled(String provider) {
                return;
            }
        };
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), this.PERMISSIONS[0])
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), this.PERMISSIONS[1])
                == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    500, 1, this.locationListener);
            this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    500, 1, this.locationListener);
        }
        if (this.lightSensor != null) {
            this.sensorManager.registerListener(this.lightSensorListener, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), this.PERMISSIONS[0])
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), this.PERMISSIONS[1])
                == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(locationListener);
        }
        if (this.lightSensorListener != null) {
            this.sensorManager.unregisterListener(this.lightSensorListener);
        }
        return;
    }
}
