package com.example.lijingjiang.mobile_sensor_display;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisualizationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);

        int type = getIntent().getIntExtra("type",0);

        ArrayList<Float> result = new ArrayList<>();
        switch (type){
            case 1:
                result = readLastTenData("runningEfficiency");
                break;
            case 2:
                result = readLastTenData("exerciseEfficiency");
                break;
            case 3:
                result = readLastTenData("sleepingEfficiency");
                result.clear();
                result.add(Float.valueOf("0.1"));
                result.add(Float.valueOf("0.20"));
                result.add(Float.valueOf("0.30"));
                result.add(Float.valueOf("0.20"));
                result.add(Float.valueOf("0.10"));
                result.add(Float.valueOf("0.60"));
                result.add(Float.valueOf("0.20"));
                result.add(Float.valueOf("0.30"));
                result.add(Float.valueOf("0.60"));
                result.add(Float.valueOf("0.40"));
                break;
        }


        BarChart mBarChart = (BarChart) findViewById(R.id.barchart);
        for (int i = 0; i < result.size(); i++){
            if (result.get(i) == null) {
                continue;
            }
            mBarChart.addBar(new BarModel((result.get(i)*100), 0xFF56B7F1));
        }

        mBarChart.startAnimation();
    }


    public boolean fileExistence(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    public ArrayList<Float> readLastTenData(String fname){
        ArrayList<Float> results = new ArrayList<>();
        File file = getBaseContext().getFileStreamPath(fname);
        if (!file.exists()){
            System.out.println("FILE DOESNT EXIST!!!");
            return results;
        } else {
            System.out.println("FILE EXISTS!!!");
            try {
                FileInputStream is = new FileInputStream(file);

                int counter = 0;
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    System.out.println("LINE READ " + line);
                    counter++;
                    results.add(Float.valueOf(line));
                    line = reader.readLine();
                }
                Collections.reverse(results);
                if (results.size() <= 10){
                    return results;
                } else {
                    ArrayList<Float> tenResults = new ArrayList<>();
                    for (int i = 0; i < 10; i++){
                        tenResults.add(results.get(i));
                    }
                    return tenResults;
                }
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
        return results;
    }


}
