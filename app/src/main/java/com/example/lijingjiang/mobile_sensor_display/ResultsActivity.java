package com.example.lijingjiang.mobile_sensor_display;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.lijingjiang.mobile_sensor_display.R;
import com.google.android.gms.vision.text.Text;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ResultsActivity extends AppCompatActivity {

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private static final String DISTANCES = "Distance traveled: ";
    private static final String TIME = "Time Elapsed: ";
    private TextView stepsTextView;
    private TextView distancesTextView;
    private TextView timeTextView;


    private TextView average_hr;

    private TextView efficiency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        /**
         * Collecting data from previous activity
         */
        int numSteps = getIntent().getIntExtra("num_steps", 0);

        long timeElapsed = getIntent().getLongExtra("time_elapsed", 0);

        double defaultValue = 0.0;
        double distances = getIntent().getDoubleExtra("google_distance_traveled", defaultValue);

        double[] measurements = new double[]{0.55, 0.65, 0.75, 0.85,0.95};

        /**
         * Data format to visualize the float number
         */
        DecimalFormat four = new DecimalFormat("#0.0000");

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(timeElapsed);
        long minutes = diffInSeconds / 60;

        stepsTextView = (TextView) findViewById(R.id.steps_result);
        distancesTextView = (TextView) findViewById(R.id.distances_result);
        timeTextView = (TextView) findViewById(R.id.time_result);

        average_hr = (TextView)findViewById(R.id.average_hr);
        efficiency = (TextView)findViewById(R.id.efficiency);


        int efficiency_type = getIntent().getIntExtra("efficiency_type",0);
        int age = getIntent().getIntExtra("age_field",0);
        int type = getIntent().getIntExtra("type_field",0);
        double average = getIntent().getDoubleExtra("average_hr", 0.0);

        double sleep_efficiency = getIntent().getDoubleExtra("sleep_efficiency", 0.0);

        timeTextView.setText(TIME + minutes + " minutes " + diffInSeconds % 60 + " seconds");
        stepsTextView.setText(TEXT_NUM_STEPS + numSteps);
        distancesTextView.setText(DISTANCES + four.format(distances) + " meters");

        average_hr.setText("Average heart rate: " + average);


        double efficiency_value = 0.0;
        double r_max = 0.0;
        r_max = 208 - 0.7 * age;
        if (efficiency_type == 2) {
            efficiency_value = Math.abs((average/r_max) - measurements[type - 1])/measurements[type - 1];
            double ratio = r_max / (r_max - getAgeRest(age) * 1.0);
            efficiency_value = 1 - efficiency_value * ratio;
        }


        double run_efficiency = 0.0;
        if (efficiency_type == 1) {

            double timeInMin = diffInSeconds/60.0;
            double b=(average - getAgeRest(age)) * timeInMin;

            double w = b / (distances * 0.000621371);
            System.out.println("the b is: " + b);
            System.out.println("the distance is: " + distances);
            System.out.println("the w is: " + w);
            run_efficiency = 1.0 * 100000.0 / (1.0 * w);
        }

        switch (efficiency_type) {
            case 1:

                efficiency.setText("Running Efficiency: " + run_efficiency);
                storeInRunningEfficiencyFile(run_efficiency);
                break;
            case 2:

                efficiency.setText("Exercise Efficiency: " + efficiency_value);
                storeInExerciseEfficiencyFile(efficiency_value);
                break;
            case 3:

                efficiency.setText("Sleeping Efficiency: " + sleep_efficiency);
                storeInSleepingEfficiencyFile(sleep_efficiency);
                break;
        }
    }

    private int getAgeRest(int age) {
        if (age <= 35) {
            return 65;
        }
        if (36 <= age && age <= 45) {
            return 66;
        }
        if (46 <= age && age <= 65) {
            return 67;
        }
        if (age >= 65) {
            return 65;
        }
        return 0;
    }

       public void storeInRunningEfficiencyFile(double val){
         String filename = "runningEfficiency";
         String valString = String.valueOf(val) + "\n";
         FileOutputStream outputStream;

         try {
             outputStream = openFileOutput(filename, Context.MODE_APPEND);
             outputStream.write(valString.getBytes());
             outputStream.close();
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

     public void storeInSleepingEfficiencyFile(double val){
         String filename = "sleepingEfficiency";

         String valString = String.valueOf(val) + "\n";
         FileOutputStream outputStream;

         try {
             outputStream = openFileOutput(filename, Context.MODE_APPEND);
             outputStream.write(valString.getBytes());
            outputStream.close();
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

        public void storeInExerciseEfficiencyFile(double val){
         String filename = "exerciseEfficiency";

         String valString = String.valueOf(val) + "\n";
         FileOutputStream outputStream;

         try {
             outputStream = openFileOutput(filename, Context.MODE_APPEND);
             outputStream.write(valString.getBytes());
             outputStream.close();
             System.out.println("storing!!!");
         } catch (Exception e) {
             System.out.println("exception!!!!");
             e.printStackTrace();
         }

 //        ArrayList<Float> results = readLastTenData(filename);
 //        System.out.println("read file size " + results.size() + "!!!");
 //
 //        System.out.println("read data " + results + "!!!");
         //System.out.println(Float.valueOf(results.get(0)));
     }
}
