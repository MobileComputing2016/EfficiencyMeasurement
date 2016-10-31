package com.example.lijingjiang.mobile_sensor_display;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.apps.simplepedometer.R;
import com.google.android.gms.vision.text.Text;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class ResultsActivity extends AppCompatActivity {

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private static final String DISTANCES = "Distances: ";
    private static final String TIME = "Time Elapsed: ";
    private TextView stepsTextView;
    private TextView distancesTextView;
    private TextView timeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        int numSteps = getIntent().getIntExtra("num_steps", 0);
        long timeElapsed = getIntent().getLongExtra("time_elapsed", 0);
        double defaultValue = 0.0;
        double distances = getIntent().getDoubleExtra("distance_travels", defaultValue);
        System.out.println(distances);
        DecimalFormat four = new DecimalFormat("#0.0000");

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(timeElapsed);
        long minutes = diffInSeconds / 60;

        stepsTextView = (TextView) findViewById(R.id.steps_result);
        distancesTextView = (TextView) findViewById(R.id.distances_result);
        timeTextView = (TextView) findViewById(R.id.time_result);
        timeTextView.setText(TIME + minutes + " minutes " + diffInSeconds % 60 + " seconds");
        stepsTextView.setText(TEXT_NUM_STEPS + numSteps);
        distancesTextView.setText(DISTANCES + four.format(distances));
    }
}
