/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.lijingjiang.mobile_sensor_display;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.simplepedometer.R;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;
import java.util.Date;

public class SimplePedometerActivity extends Activity
        implements SensorEventListener, StepListener, ConnectionCallbacks,
        OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {
    /**
     * To calculate the elapsed time during calculation
     */
    Date date;

    /**
     * Two buttons for user interaction
     */
    private Button startButton;
    private Button stopButton;

    /**
     * The following fields are Used for pedometer
     */
    private SimpleStepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps = 0;

    private TextView stepsResultView;

    /**
     * The following field is used by google location service
     */
    private Location previousLocation;
    private Location currentLocation;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private TextView distanceResultView;
    private double accumulatedLocationCalculatedFromGoogle = 0.0;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;

    /**
     * The following field will be used when we enabled the gps sensor
     */
    private LocationManager locationManager;
    private Location startLocation = null;
    private Location lastKnown = null;
    private LocationListener locationListener;
    private String locationProvider;
    private long minTimeMs = 100;
    private float minDistanceMeters = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * Initialization
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);

        }

        /**
         * Attach UI objects
         */
        distanceResultView = (TextView) findViewById(R.id.result_field);
        stepsResultView = (TextView) findViewById(R.id.steps);

        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);
        distanceResultView.setText(
                String.valueOf(accumulatedLocationCalculatedFromGoogle));

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        /**
         * Sensor initialization
         */
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(this);


        /**
         * Google Service Initialization
         */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)         // 2000 milliseconds
                .setFastestInterval(2000); // 2000 milliseconds

        /**
         * The following code will enable the GPS sensor to get location data
         */
        //        locationManager = (LocationManager)
        //        this.getSystemService(Context.LOCATION_SERVICE);
        //        locationListener = new GPSLocationListener();
        //        locationProvider = LocationManager.GPS_PROVIDER;
        //        lastKnown =
        //        locationManager.getLastKnownLocation(locationProvider);
        //        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
        //        minTimeMs, minDistanceMeters, locationListener);
    }


    @Override
    public void onResume() {
        super.onResume();

        /**
         * When switch back to the previous page, reset the data
         */
        numSteps = 0;
        accumulatedLocationCalculatedFromGoogle = 0;

        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);
        distanceResultView.setText(
                String.valueOf(accumulatedLocationCalculatedFromGoogle));

        mGoogleApiClient.connect();
    }


    @Override
    public void onPause() {
        super.onPause();

        /**
         * Disconnect the Google location service
         */
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                    this);
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * When the start button is pressed, execute this function
     */
    public void startCollection(View view) {

        /**
         * Get current time stamp to calculate the elapsed time
         */
        date = Calendar.getInstance().getTime();

        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        /**
         * Reset all data
         */
        numSteps = 0;
        accumulatedLocationCalculatedFromGoogle = 0;

        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);
        distanceResultView.setText(R.string.zero);

        /**
         * When click the start button, register the sensor and start google
         * location service
         */
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);


        /**
         * The following code blocks is required by google api
         */
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {

            // TODO: Add function logic when user don't give the permission
            return;
        }
        previousLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        currentLatitude = previousLocation.getLatitude();
        currentLongitude = previousLocation.getLongitude();

        /**
         * The following toast will indicate that the user has provided the permission,
         * and the app can get the location information now
         */
        Toast
                .makeText(this, currentLatitude + " WORKS " + currentLongitude + "",
                        Toast.LENGTH_LONG)
                .show();

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * When the end button is pressed, execute this function
     */
    public void stopCollection(View view) {
        Date mostRecentTime = Calendar.getInstance().getTime();
        long timeElapsed = mostRecentTime.getTime() - date.getTime();

        startButton.setEnabled(true);
        stopButton.setEnabled(false);


        /**
         * When clicking the stop button, unregister the sensor and disconnect the google service
         */
        sensorManager.unregisterListener(this);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                    this);
            mGoogleApiClient.disconnect();
        }

        /**
         * Passing the result to the result activity
         */
        Intent intent = new Intent(this, ResultsActivity.class);
        intent.putExtra("num_steps", numSteps);
        intent.putExtra("google_distance_traveled",
                accumulatedLocationCalculatedFromGoogle);
        intent.putExtra("time_elapsed", timeElapsed);

        startActivity(intent);
    }




    /**
     * The following three functions are required by accelerometer sensor
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(event.timestamp, event.values[0],
                    event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);
    }


    /**
     * The following three functions are required by google location service interface
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    /*
     * Google Play services can resolve some errors it detects.
     * If the error has a resolution, try sending an Intent to
     * start a Google Play services activity that can resolve
     * error.
     */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
        /*
         * Thrown if Google Play services canceled the original
         * PendingIntent
         */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
      /*
       * If no resolution is available, display a dialog to the
       * user with the error.
       */
            Log.e("Error", "Location services connection failed with code " +
                    connectionResult.getErrorCode());
        }
    }

    /**
     * Call back function when the location changed
     */

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("location changed");
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        currentLocation = location;

        float[] results = new float[3];
        Location.distanceBetween(
                previousLocation.getLatitude(), previousLocation.getLongitude(),
                currentLocation.getLatitude(), currentLocation.getLongitude(), results);

        /**
         * The filter to ignore device jitter
         */
        if (results[0] > 1) {
            accumulatedLocationCalculatedFromGoogle += results[0];
        }

        DecimalFormat four = new DecimalFormat("#0.0000");

        distanceResultView.setText(
                four.format(accumulatedLocationCalculatedFromGoogle));

        Toast
                .makeText(this, "moved: " + accumulatedLocationCalculatedFromGoogle,
                        Toast.LENGTH_SHORT)
                .show();


        previousLocation = location;
    }
}
