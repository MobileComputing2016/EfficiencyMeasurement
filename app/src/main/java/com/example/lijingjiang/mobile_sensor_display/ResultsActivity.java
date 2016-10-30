package com.example.lijingjiang.mobile_sensor_display;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.apps.simplepedometer.R;

import java.text.DecimalFormat;

public class ResultsActivity extends AppCompatActivity {

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private static final String DISTANCES = "Distances: ";
    private TextView stepsTextView;
    private TextView distancesTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        int numSteps = getIntent().getIntExtra("num_steps", 0);
        double defaultValue = 0.0;
        double distances = getIntent().getDoubleExtra("distance_travels", defaultValue);
        System.out.println(distances);
        DecimalFormat four = new DecimalFormat("#0.0000");

        stepsTextView = (TextView) findViewById(R.id.steps_result);
        distancesTextView = (TextView) findViewById(R.id.distances_result);
        stepsTextView.setText(TEXT_NUM_STEPS + numSteps);
        distancesTextView.setText(DISTANCES + four.format(distances));
    }
}
