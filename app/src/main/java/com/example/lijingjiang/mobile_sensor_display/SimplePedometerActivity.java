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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
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
import android.os.IBinder;
import android.support.annotation.FloatRange;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import com.example.lijingjiang.mobile_sensor_display.R;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import android.os.Handler;
import android.os.Message;

import java.util.List;
import java.util.UUID;

public class SimplePedometerActivity extends Activity
        implements SensorEventListener, StepListener, ConnectionCallbacks,
        OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {
    private Activity a = this;
    /**
     * To calculate the elapsed time during calculation
     */
    Date date;

    private ArrayList<Integer> hrList = new ArrayList<>();

    /**
     * Two buttons for user interaction
     */
    private Button startButtonRunning;
    private Button stopButtonRunning;

    private Button startButtonExercising;
    private Button stopButtonExercising;

    private Button startButtonSleeping;
    private Button stopButtonSleeping;

    private EditText ageField;
    private EditText exerciseTypeField;

    /**
     * The following fields are Used for pedometer
     */
    private SimpleStepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private TextView distanceResultView;

    private static final String TEXT_HR = "Heart Rate (bpm): ";
    private int hr = 42;
    private TextView hrResultView;

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps = 0;
    private TextView stepsResultView;

    /**
     * The following field is used by google location service
     */
    private Location previousLocation;
    private Location currentLocation;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private double accumulatedLocationCalculatedFromGoogle = 0.0;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;

    private boolean startRecording = false;

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

    /** The following fields will be used with bluetooth hr collection **/
    String mDeviceAddress = "00:22:D0:BD:25:8E";
    UUID mServiceUUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    UUID mCharacteristicUUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private BluetoothLeService mBluetoothLeService;
    private final static String TAG = "PolarHRListener";
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    Handler mHandler;
    int mUpdatePeriodMillis = 1000;
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
        hrResultView = (TextView) findViewById(R.id.hr);
        hrResultView.setText(TEXT_HR + hr);

        distanceResultView = (TextView) findViewById(R.id.result_field);
        distanceResultView.setText(
                String.valueOf(accumulatedLocationCalculatedFromGoogle) + " Meters");

        stepsResultView = (TextView) findViewById(R.id.steps);
        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);

        startButtonRunning = (Button) findViewById(R.id.start_running_button);
        stopButtonRunning = (Button) findViewById(R.id.stop_running_button);
        startButtonRunning.setEnabled(true);
        stopButtonRunning.setEnabled(false);


        startButtonExercising= (Button) findViewById(R.id.start_exercise_button);
        stopButtonExercising = (Button) findViewById(R.id.stop_exercise_button);
        startButtonExercising.setEnabled(true);
        stopButtonExercising.setEnabled(false);

        startButtonSleeping= (Button) findViewById(R.id.start_sleep_button);
        stopButtonSleeping= (Button) findViewById(R.id.stop_sleep_button);
        startButtonSleeping.setEnabled(true);
        stopButtonSleeping.setEnabled(false);


        ageField = (EditText)findViewById(R.id.age_field);
        exerciseTypeField = (EditText)findViewById(R.id.exercise_type);

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

        mHandler =  new Handler() {
            public void handleMessage(Message msg) {
                //distanceResultView.setText(R.string.zero);
                hrResultView.setText(TEXT_HR + hr);
                stepsResultView.setText(TEXT_NUM_STEPS + numSteps);
                //Start runnable here, maybe?
                mHandler.sendMessageDelayed(Message.obtain(), mUpdatePeriodMillis);
            }
        };

        mHandler.sendMessageDelayed(Message.obtain(), mUpdatePeriodMillis);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    @Override
    public void onResume() {
        super.onResume();

        /**
         * When switch back to the previous page, reset the data
         */
        numSteps = 0;
        accumulatedLocationCalculatedFromGoogle = 0;
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);
        distanceResultView.setText(
                String.valueOf(accumulatedLocationCalculatedFromGoogle) + " Meters");

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
    public void startCollection_Running(View view) {

        /**
         * Get current time stamp to calculate the elapsed time
         */
        date = new Date();

        startButtonRunning.setEnabled(false);
        stopButtonRunning.setEnabled(true);

        startButtonExercising.setEnabled(false);
        stopButtonExercising.setEnabled(false);

        startButtonSleeping.setEnabled(false);
        stopButtonSleeping.setEnabled(false);

        /**
         * Reset all data
         */
        hr = 0;
        numSteps = 0;
        accumulatedLocationCalculatedFromGoogle = 0;
        startRecording = true;

        distanceResultView.setText(R.string.zero + " Meters");
        hrResultView.setText(TEXT_HR + hr);
        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);

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
        //Toast
        //       .makeText(this, currentLatitude + " WORKS " + currentLongitude + "",
        //                Toast.LENGTH_LONG)
         //       .show();

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }


    public void startCollection_Exercising(View view) {

        /**
         * Get current time stamp to calculate the elapsed time
         */
        date = new Date();

        startButtonExercising.setEnabled(false);
        stopButtonExercising.setEnabled(true);

        startButtonRunning.setEnabled(false);
        stopButtonRunning.setEnabled(false);

        startButtonSleeping.setEnabled(false);
        stopButtonSleeping.setEnabled(false);

        /**
         * Reset all data
         */
        hr = 0;
        numSteps = 0;
        accumulatedLocationCalculatedFromGoogle = 0;
        startRecording = true;

        distanceResultView.setText(R.string.zero + " Meters");
        hrResultView.setText(TEXT_HR + hr);
        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);

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
        //Toast
        //       .makeText(this, currentLatitude + " WORKS " + currentLongitude + "",
        //                Toast.LENGTH_LONG)
        //       .show();

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }


    public void startCollection_Sleeping(View view) {

        /**
         * Get current time stamp to calculate the elapsed time
         */
        date = new Date();

        startButtonSleeping.setEnabled(false);
        stopButtonSleeping.setEnabled(true);


        startButtonRunning.setEnabled(false);
        stopButtonRunning.setEnabled(false);

        startButtonExercising.setEnabled(false);
        startButtonExercising.setEnabled(false);

        /**
         * Reset all data
         */
        hr = 0;
        numSteps = 0;
        accumulatedLocationCalculatedFromGoogle = 0;
        startRecording = true;

        distanceResultView.setText(R.string.zero + " Meters");
        hrResultView.setText(TEXT_HR + hr);
        stepsResultView.setText(TEXT_NUM_STEPS + numSteps);

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
        //Toast
        //       .makeText(this, currentLatitude + " WORKS " + currentLongitude + "",
        //                Toast.LENGTH_LONG)
        //       .show();

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * When the end button is pressed, execute this function
     */
    public void stopCollection_Running(View view) {
        Date mostRecentTime = new Date();
        long timeElapsed = mostRecentTime.getTime() - date.getTime();

        startButtonRunning.setEnabled(true);
        stopButtonRunning.setEnabled(false);

        startButtonExercising.setEnabled(true);
        stopButtonExercising.setEnabled(false);

        startButtonSleeping.setEnabled(true);
        stopButtonSleeping.setEnabled(false);

        startRecording = false;
        double sum = 0;
        for (int i = 0 ; i < hrList.size(); i++) {
            sum += hrList.get(i);
        }
        double average = sum / hrList.size();


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
        intent.putExtra("average_hr", average);

        intent.putExtra("age_field", Integer.valueOf(ageField.getText().toString()));

        intent.putExtra("efficiency_type", 1);

        startActivity(intent);
    }


    public void stopCollection_Exercising(View view) {
        Date mostRecentTime = new Date();
        long timeElapsed = mostRecentTime.getTime() - date.getTime();

        startButtonExercising.setEnabled(true);
        stopButtonExercising.setEnabled(false);

        startButtonSleeping.setEnabled(true);
        stopButtonSleeping.setEnabled(false);


        startButtonRunning.setEnabled(true);
        stopButtonRunning.setEnabled(false);


        startRecording = false;
        double sum = 0;
        for (int i = 0 ; i < hrList.size(); i++) {
            sum += hrList.get(i);
        }
        double average = sum / hrList.size();
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
        intent.putExtra("average_hr", average);



        intent.putExtra("age_field", Integer.valueOf(ageField.getText().toString()));
        intent.putExtra("type_field", Integer.valueOf(exerciseTypeField.getText().toString()));


        intent.putExtra("efficiency_type", 2);

        startActivity(intent);
    }


    public void stopCollection_Sleeping(View view) {
        Date mostRecentTime = new Date();
        long timeElapsed = mostRecentTime.getTime() - date.getTime();

        startButtonSleeping.setEnabled(true);
        stopButtonSleeping.setEnabled(false);

        startButtonExercising.setEnabled(true);
        stopButtonExercising.setEnabled(false);

        startButtonRunning.setEnabled(true);
        stopButtonRunning.setEnabled(false);

        startRecording = false;
        double sum = 0;
        for (int i = 0 ; i < hrList.size(); i++) {
            sum += hrList.get(i);
        }
        double average = sum / hrList.size();


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
        intent.putExtra("average_hr", average);




        intent.putExtra("efficiency_type", 3);

        double sleepEfficiency = SleepEfficiency(hrList, 2.25)[1];

        intent.putExtra("sleep_efficiency", sleepEfficiency);

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
                four.format(accumulatedLocationCalculatedFromGoogle) + " Meters");

        //Toast
        //        .makeText(this, "moved: " + accumulatedLocationCalculatedFromGoogle,
        //                Toast.LENGTH_SHORT)
        //        .show();


        previousLocation = location;
    }
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                for (BluetoothGattService gattService :mBluetoothLeService.getSupportedGattServices()){
                    Log.i("polar", gattService.getUuid().toString());
                    for (BluetoothGattCharacteristic gC:gattService.getCharacteristics()){
                        if (gC.getUuid().equals(mCharacteristicUUID)){
                            mBluetoothLeService.setCharacteristicNotification(gC, true);
                        }
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                hr  = Integer.valueOf(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                if (startRecording) {
                    System.out.println("updating hrlist: " + hrList);
                    hrList.add(hr);
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



    // Rating is stored in data[0], efficiency is stored in data[1]
    // We use rating to calculate average rating over several nights of sleep, enables us to find targetRating
    // Save target rating for future implentation, right now, just set default targetRating to 2.25
    public double[] SleepEfficiency (ArrayList<Integer> sleep, double targetRating) {
        int LF = 0;
        int HF = 0;
        int window = 60;
        int index = 0;
        while (index+window < sleep.size()) {
            List<Integer> subSleep = sleep.subList(index, index+window);
            if (Collections.max(subSleep)-Collections.min(subSleep) < 9) {
                LF++;
            }
            else {
                HF++;
            }
            index = index+window;
        }
        List<Integer> subSleep = sleep.subList(index, sleep.size()-1);
        if (Collections.max(subSleep)-Collections.min(subSleep) < 9) {
            LF++;
        }
        else {
            HF++;
        }
        double data[] = new double[2];
        data[0] = LF/HF; //rating
        data[1] = 1-(Math.abs(targetRating-data[0])/data[0]); //efficiency
        return data;
    }

    public void visualizeRunningEfficiency(View view) {

        Intent intent = new Intent(this, VisualizationActivity.class);
        intent.putExtra("type", 1);
        startActivity(intent);
    }

    public void visualizeExerciseEfficiency (View view) {


        Intent intent = new Intent(this, VisualizationActivity.class);
        intent.putExtra("type", 2);
        startActivity(intent);
    }

    public void visualizeSleepEfficiency (View view) {


        Intent intent = new Intent(this, VisualizationActivity.class);
        intent.putExtra("type", 3);
        startActivity(intent);
    }

}
