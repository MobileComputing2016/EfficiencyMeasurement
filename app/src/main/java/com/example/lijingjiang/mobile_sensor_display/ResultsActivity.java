package com.example.lijingjiang.mobile_sensor_display;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.apps.simplepedometer.R;

public class ResultsActivity extends AppCompatActivity {

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private TextView stepsTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        int numSteps = getIntent().getIntExtra("num_steps", 0);
        stepsTextView = (TextView) findViewById(R.id.steps_result);
        stepsTextView.setText(TEXT_NUM_STEPS + numSteps);

    }
}
