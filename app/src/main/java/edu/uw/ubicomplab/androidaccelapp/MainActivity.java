package edu.uw.ubicomplab.androidaccelapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;
import static java.util.Collections.sort;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    int numDataCycles = 3;
    int median = 0;
    List<Integer> medianVal = new ArrayList<>();

    Double[] prevFeatures;
    Double[] features;
    List<Double[]> allFeatures = new ArrayList<Double[]>();
    private String countFilepath = "count.txt";
    private String medianFilepath = "median.txt";
    int prevAverage = -1;
    // GLOBALS
    // Accelerometer
    private LineGraphSeries<DataPoint> timeAccelX = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> timeAccelY = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> timeAccelZ = new LineGraphSeries<>();

    // Gyroscope
    private LineGraphSeries<DataPoint> timeGyroX = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> timeGyroY = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> timeGyroZ = new LineGraphSeries<>();

    // Graph
    private GraphView graph;
    private int graphXBounds = 30;
    private int graphYBounds = 30;
    private int graphColor[] = {Color.argb(255,244,170,50),
            Color.argb(255, 60, 175, 240),
            Color.argb(225, 50, 220, 100)};
    private static final int MAX_DATA_POINTS_UI_IMU = 100; // Adjust to show more points on graph
    public int accelGraphXTime = 0;
    public int gyroGraphXTime = 0;
    public boolean isPlotting = false;

    // UI elements
    private TextView resultText;
    private TextView gesture1CountText, gesture2CountText, gesture3CountText, gesture4CountText, gesture5CountText, gesture6CountText, gesture7CountText, gesture8CountText;
    private Button gesture1Button,gesture2Button,gesture3Button,gesture4Button, gesture5Button, gesture6Button, gesture7Button, gesture8Button;

    // Machine learning
    private Model model;
    private boolean isRecording;
    private DescriptiveStatistics accelTime, accelX, accelY, accelZ;
    private DescriptiveStatistics gyroTime, gyroX, gyroY, gyroZ;
    private DescriptiveStatistics orientTime, orientX, orientY, orientZ;

    private static final int GESTURE_DURATION_SECS = 4;

    boolean isAnyNewGestureRecorded = false;
//    int count = 0;
//    int numSamples = -1;

    int totalCount = 0;
    int totalSamples = -1;
    int currCount = 0;
    int average = 0;
    int lastEmpty = 0;
    int sawEmpty = 0;

    SensorManager sensorManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the UI elements
        resultText = findViewById(R.id.resultText);
        gesture1CountText = findViewById(R.id.gesture1TextView);
        gesture2CountText = findViewById(R.id.gesture2TextView);
        gesture3CountText = findViewById(R.id.gesture3TextView);
        gesture4CountText = findViewById(R.id.gesture4TextView);
        gesture5CountText = findViewById(R.id.gesture5TextView);
        gesture6CountText = findViewById(R.id.gesture6TextView);
        gesture7CountText = findViewById(R.id.gesture7TextView);
        gesture8CountText = findViewById(R.id.gesture8TextView);
        gesture1Button = findViewById(R.id.gesture1Button);
        gesture2Button = findViewById(R.id.gesture2Button);
        gesture3Button = findViewById(R.id.gesture3Button);
        gesture4Button = findViewById(R.id.gesture4Button);
        gesture5Button = findViewById(R.id.gesture5Button);
        gesture6Button = findViewById(R.id.gesture6Button);
        gesture7Button = findViewById(R.id.gesture7Button);
        gesture8Button = findViewById(R.id.gesture8Button);



        // Initialize data structures for gesture recording
        accelTime = new DescriptiveStatistics();
        accelX = new DescriptiveStatistics();
        accelY = new DescriptiveStatistics();
        accelZ = new DescriptiveStatistics();
        gyroTime = new DescriptiveStatistics();
        gyroX = new DescriptiveStatistics();
        gyroY = new DescriptiveStatistics();
        gyroZ = new DescriptiveStatistics();
        orientTime = new DescriptiveStatistics();
        orientX = new DescriptiveStatistics();
        orientY = new DescriptiveStatistics();
        orientZ = new DescriptiveStatistics();

        // Initialize the model
        model = new Model(this);


        //add text to the buttons
        gesture1Button.setText(model.outputClasses[0]);
        gesture2Button.setText(model.outputClasses[1]);
        gesture3Button.setText(model.outputClasses[2]);
        gesture4Button.setText(model.outputClasses[3]);
        gesture5Button.setText(model.outputClasses[4]);
        gesture6Button.setText(model.outputClasses[5]);
        gesture7Button.setText(model.outputClasses[6]);
        gesture8Button.setText(model.outputClasses[7]);

        // Get the sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor accelerometer2 = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometer2, SensorManager.SENSOR_DELAY_GAME);

        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);



        // Check permissions
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    protected void onResume(){
        super.onResume();
        restoreData();
        updateMedianRestore();
        train();
    }

    @Override
    protected void onStop(){
        super.onStop();
        saveData();
        updateMedianSave();
    }

    private void updateMedianRestore(){
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + medianFilepath;
        File medianFile = new File(fullFileName);
        if (medianFile.exists()) {
            BufferedReader dataReader;
            try {
                FileReader fileReader = new FileReader(fullFileName);
                String text = null;
                dataReader = new BufferedReader(fileReader);
                try {
                    String line = dataReader.readLine();
                    while (line != null) {
                        medianVal.add(Integer.valueOf(line));
                        line = dataReader.readLine();
                    }
                    dataReader.close();
                } catch (Exception e) {
                    Log.e("Exception", "File read failed: " + e.toString());
                }
            }catch(Exception e){
                Log.e("Exception", "File read failed: " + e.toString());
            }
        }
    }

    private int findMedian(){
        List<Integer> lastVals = new ArrayList<Integer>();
        int medianValSize = medianVal.size();
        Log.d("ARRAY BEFORE", "OG MED VAL SIZE" + medianValSize);
        if (medianValSize > numDataCycles) {
            medianValSize = numDataCycles;
        }
        Log.d("ARRAY BEFORE", medianVal.toString() + "MED VAL SIZE" + medianValSize);
        for (int i = medianValSize; i >0; i--) {
            lastVals.add(medianVal.get(medianVal.size() - i));
        }
        sort(lastVals);
        Log.d("ARRAY After", lastVals.toString() + "INDEX:" + lastVals.size()/2 + "MED: " + lastVals.get(((lastVals.size())/2)));
        return lastVals.get(((lastVals.size())/2));
    }

    private void updateMedianSave(){
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + medianFilepath;
        File countFile = new File(fullFileName);
        if (countFile.exists()) {
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(fullFileName, false));
                int medianValSize = medianVal.size();
                if (medianValSize > numDataCycles) {
                    medianValSize = numDataCycles;
                }
                for (int i = medianValSize; i >0; i--) {
                    writer.write(Integer.toString(medianVal.get(medianVal.size() - i)));
                    writer.newLine();
                }
                writer.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }else{
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }

    private void restoreData(){
//        Log.d("data", "in restore data");
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + countFilepath;
        File countFile = new File(fullFileName);
        if (countFile.exists()) {
            BufferedReader dataReader;
            try {
                FileReader fileReader = new FileReader(fullFileName);
                String text = null;
                dataReader = new BufferedReader(fileReader);
                try {
                    totalCount = Integer.valueOf(dataReader.readLine());
                    totalSamples = Integer.valueOf(dataReader.readLine());
                    currCount = Integer.valueOf(dataReader.readLine());
                    lastEmpty = Integer.valueOf(dataReader.readLine());
                    if (totalSamples != 0) {
                        average = totalCount / totalSamples;
                    }
//                    Log.d("data reading totalCount", String.valueOf(totalCount));
//                    Log.d("data reading totalSamp", String.valueOf(totalSamples));
//                    Log.d("data reading currCount", String.valueOf(currCount));
                    dataReader.close();
                }
                catch(Exception e){
                    Log.e("Exception", "File read failed: " + e.toString());
                }
            }
            catch(Exception e){
                Log.e("Exception", "File read failed: " + e.toString());

            }
        }
        else{
            Log.e("Exception", "File does not exist ");
        }
    }

    private void saveData(){
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + countFilepath;
        File countFile = new File(fullFileName);
        if (countFile.exists()) {
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(fullFileName, false));
//                Log.d("data saving totalCount", String.valueOf(totalCount));
//                Log.d("data saving totalSamp", String.valueOf(totalSamples));
//                Log.d("data saving currCount", String.valueOf(currCount));

                writer.write(Integer.toString(totalCount));
                writer.newLine();
                writer.write(Integer.toString(totalSamples));
                writer.newLine();
                writer.write(Integer.toString(currCount));
                writer.newLine();
                writer.write(Integer.toString(lastEmpty));
                writer.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }else{
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
                writer.write(Integer.toString(totalCount));
                writer.newLine();
                writer.write(Integer.toString(totalSamples));
                writer.newLine();
                writer.write(Integer.toString(currCount));
                writer.newLine();
                writer.write(Integer.toString(lastEmpty));
                writer.close();
//                Log.d("data saving totalCount", String.valueOf(totalCount));
//                Log.d("data saving totalSamp", String.valueOf(totalSamples));
//                Log.d("data saving currCount", String.valueOf(currCount));
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }

    float[] mGravity;
    float[] mGeomagnetic;
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            accelGraphXTime += 1;

            // Get the data from the event
            long timestamp = event.timestamp;
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];

//            double Roll = atan2(ay, az) * 180/Math.PI;
//            double Pitch = atan2(-ax, sqrt(ay*ay + az*az)) * 180/Math.PI;
//            Log.d("ROLL PITCH", "ROLL: " + Roll + " Pitch: " + Pitch);

            // Add the original data to the graph
            DataPoint dataPointAccX = new DataPoint(accelGraphXTime, ax);
            DataPoint dataPointAccY = new DataPoint(accelGraphXTime, ay);
            DataPoint dataPointAccZ = new DataPoint(accelGraphXTime, az);
            timeAccelX.appendData(dataPointAccX, true, MAX_DATA_POINTS_UI_IMU);
            timeAccelY.appendData(dataPointAccY, true, MAX_DATA_POINTS_UI_IMU);
            timeAccelZ.appendData(dataPointAccZ, true, MAX_DATA_POINTS_UI_IMU);

            // Advance the graph
            if (isPlotting) {
                graph.getViewport().setMinX(accelGraphXTime - graphXBounds);
                graph.getViewport().setMaxX(accelGraphXTime);
            }

            // Add to gesture recorder, if applicable
            if (isRecording) {
                accelTime.addValue(timestamp);
                accelX.addValue(ax);
                accelY.addValue(ay);
                accelZ.addValue(az);
            }
        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroGraphXTime += 1;

            // Get the data from the event
            long timestamp = event.timestamp;
            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];

            // Add the original data to the graph
            DataPoint dataPointGyroX = new DataPoint(gyroGraphXTime, gx);
            DataPoint dataPointGyroY = new DataPoint(gyroGraphXTime, gy);
            DataPoint dataPointGyroZ = new DataPoint(gyroGraphXTime, gz);
            timeGyroX.appendData(dataPointGyroX, true, MAX_DATA_POINTS_UI_IMU);
            timeGyroY.appendData(dataPointGyroY, true, MAX_DATA_POINTS_UI_IMU);
            timeGyroZ.appendData(dataPointGyroZ, true, MAX_DATA_POINTS_UI_IMU);

            // Save to file, if applicable
            if (isRecording) {
                gyroTime.addValue(timestamp);
                gyroX.addValue(event.values[0]);
                gyroY.addValue(event.values[1]);
                gyroZ.addValue(event.values[2]);
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }


        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = orientation[0];
                float pitch = orientation[1];

                float roll = orientation[2];
                if (isRecording) {
                    orientTime.addValue(event.timestamp);
                    orientX.addValue(azimuth);
                    orientY.addValue(pitch);
                    orientZ.addValue(roll);
                }
        }
        }






    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void computeFeaturesAndAddSamples(boolean isTraining, String label, View v2)
    {
        isAnyNewGestureRecorded = true;
        // Add the recent gesture to the train or test set
        isRecording = false;

        if (features != null){
            prevFeatures = features;
        }
        //TODO: Replace this function to receive features from Particle
        features = model.computeFeatures(accelTime, accelX, accelY, accelZ,
                gyroTime, gyroX, gyroY, gyroZ, orientTime, orientX, orientY, orientZ);
        if ((isTraining) && (label.compareTo("FULL") == 0) ) {
            model.addTrainingSample(features, label);
            if (prevFeatures != null) {
                model.addTrainingSample(prevFeatures, "EMP");
                Log.d("INSIDE", String.valueOf(allFeatures.size()));
                for (int i = 0; i < allFeatures.size(); i++){
                    Double[] curFeature = allFeatures.get(i);
                    double percent = (double)i/(double)allFeatures.size();
                    Log.d("INSIDE", String.valueOf(percent));
                    if (percent < 0.125){
                        model.addTrainingSample(curFeature, "EMP");
                        Log.d("INSIDE", "EMP");
                    } else if (percent < 0.375){
                        model.addTrainingSample(curFeature, "25");
                        Log.d("INSIDE", "25");
                    } else if (percent < 0.625){
                        model.addTrainingSample(curFeature, "50");
                        Log.d("INSIDE", "50");
                    } else if (percent < 0.875){
                        model.addTrainingSample(curFeature, "75");
                        Log.d("INSIDE", "75");
                    } else{
                        model.addTrainingSample(curFeature, "FULL");
                        Log.d("INSIDE", "FULL");
                    }
                }
                allFeatures = new ArrayList<Double[]>();
            }
            train();
        }
        else
            model.assignTestSample(features);

        if (totalSamples < numDataCycles) {
            allFeatures.add(features);
            Log.d("INSIDE", " added" + String.valueOf(allFeatures.size()));
        }

        // Predict if the recent sample is for testing
        if (!isTraining) {
            String result = model.test();

            if (result != null) {
                Log.d("ARRAY UGHH Bef sawEmpty", String.valueOf(sawEmpty));
                if (result.compareTo("EMP") != 0){ //NOT EMPTY
                    lastEmpty = 0;
                    if(sawEmpty == 1){
                        updateCounts();
                        //updateCountsFile(true);
                        currCount = 0;
                        sawEmpty = 0;
                    }
                    if((currCount < (0.125*median))){
                        resultText.setText("RES: " + "100%" + "ML"  + result +"C:" + String.valueOf(currCount) + "Avg:" + String.valueOf(average) + "MED:" + String.valueOf(median));
                    }
                    else if((currCount < (0.375*median) && (currCount >= (0.125*median)))){
                        resultText.setText("RES: " + "75%" + "ML" + result + "C:" + String.valueOf(currCount) + "Avg:" + String.valueOf(average) + "MED:" + String.valueOf(median));
                    }
                    else if(currCount < (0.625*median) && (currCount >= (0.375*median))){
                        resultText.setText("RES: " + "50%" + "ML " + result + "C:" + String.valueOf(currCount) + "Avg:" + String.valueOf(average) + "MED:" + String.valueOf(median));
                    }
                    else if((currCount >= (0.635*median))&&(currCount < (0.875*median))){
                        resultText.setText("RES: " + "25%" +  "ML " + result + "C:" + String.valueOf(currCount) + "Avg:" + String.valueOf(average) + "MED:" + String.valueOf(median));
                    }
                    else{
                        resultText.setText("REALLY UNSURE:" + "ML " + result + "C:" + String.valueOf(currCount) + "Avg:" + String.valueOf(average) + "MED:" + String.valueOf(median));
                    }
                }
                 else{ //EMPTY
                     if (lastEmpty == 1){
                         lastEmpty = 0;
                         sawEmpty = 1;
                         resultText.setText("Result: " + "0%" + "Count:" + String.valueOf(currCount) + "AVG:" + String.valueOf(average) + "MED:" + String.valueOf(median) + "SE" + sawEmpty);
                     }
                     else if (currCount > (0.75*average)) {
                         sawEmpty = 1;
                         resultText.setText("Result: " + "0%" + "Count:" + String.valueOf(currCount) + "Normal:" + String.valueOf(average) + "MED:" + String.valueOf(median)+ "SE" + sawEmpty);
                     } else{
                         resultText.setText("Unsure redo");
                         lastEmpty = 1;
                     }
                 }

                 if(totalSamples <= numDataCycles){
                     Log.d("ARRAY morph", "ts " + totalSamples + "NDC " + numDataCycles);
                     lastEmpty = 0;
                     sawEmpty = 0;
                 }
//                Log.d("UGHH  Af lastEmpty", String.valueOf(lastEmpty));
//                Log.d("UGHH  Af sawEmpty", String.valueOf(sawEmpty));
            }
        }

        // Update number of samples shown
        updateTrainDataCount();
        v2.setEnabled(true);
    }

    public void updateCounts(){
        totalSamples++;
        totalCount = totalCount + currCount;
        if(totalSamples > 0) {
            average = totalCount / totalSamples;
            Log.d("data", "upated average to" + average + "count: " + totalCount + "samples" + totalSamples);
            medianVal.add(currCount);
            median = findMedian();
        }
    }

    /*
    public void updateCountsFile(boolean modify){
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + countFilepath;
        File countFile = new File(fullFileName);
        if (countFile.exists()){
            Log.d("writer", "exists");
            BufferedReader dataReader;
            try {
                FileReader fileReader = new FileReader(fullFileName);
                String text = null;
                dataReader = new BufferedReader(fileReader);
                try {
                    int prevTotal = Integer.valueOf(dataReader.readLine());
                    int prevNumSamples = Integer.valueOf(dataReader.readLine());
                    count = Integer.valueOf(dataReader.readLine());
                    Log.d("prev total", String.valueOf(prevTotal));
                    Log.d("prev num samples", String.valueOf(prevNumSamples));
                    dataReader.close();
                    int newCount = prevTotal;
                    int newNumSamples = prevNumSamples;
                    if (modify) {
                        newCount = (prevTotal + count);
                        newNumSamples = prevNumSamples + 1;
                        Log.d("prev newNumSamples", String.valueOf(newNumSamples));
                        prevAverage = 0;
                        if (newNumSamples != 0) {
                            prevAverage = Math.round(newCount / newNumSamples);
                        }
                    } else{
                        if (prevNumSamples == 0){
                            prevAverage = 0;
                        } else {
                            prevAverage = Math.round(prevTotal / prevNumSamples);
                        }
                    }
                    BufferedWriter writer;
                    try {
                        if (modify) {
                            writer = new BufferedWriter(new FileWriter(fullFileName, false));
                            Log.d("prev WRITING NEW COUNT", String.valueOf(newCount));
                            Log.d("prev WRITING NEW COUNT", String.valueOf(newNumSamples));
                            writer.write(Integer.toString(newCount));
                            writer.newLine();
                            writer.write(Integer.toString(newNumSamples));
                            writer.newLine();
                            writer.write(Integer.toString(count));
                            writer.close();
                        }
                    } catch(FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else{ //FILE DOES NOT EXIST
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
                writer.write(Integer.toString(count));
                writer.newLine();
                writer.write(Integer.toString(numSamples));
                writer.newLine();
                writer.write(Integer.toString(count));
                writer.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }
    */
    /**
     * Records a gesture that is GESTURE_DURATION_SECS long
     */
    public void recordGesture(View v) {
        final View v2 = v;

        // Figure out which button got pressed to determine label
        final String label;
        final boolean isTraining;
        switch (v.getId()) {
            case R.id.gesture1Button: //FULL
                label = model.outputClasses[0];
                isTraining = true;
                if (totalSamples <= numDataCycles) {
                    updateCounts();
                    //updateCountsFile(true);
                    currCount = 0;
                }
                break;
            case R.id.gesture2Button: //EMPTY
                label = model.outputClasses[1];
                isTraining = true;
                //save count to file
                break;
            case R.id.gesture3Button:
                label = model.outputClasses[2];
                isTraining = true;
                break;
            case R.id.gesture4Button:
                label = model.outputClasses[3];
                isTraining = true;
                break;
            case R.id.gesture5Button:
                label = model.outputClasses[4];
                isTraining = true;
                break;
            case R.id.gesture6Button:
                label = model.outputClasses[5];
                isTraining = true;
                break;
            case R.id.gesture7Button:
                label = model.outputClasses[6];
                isTraining = true;
                break;
            case R.id.gesture8Button:
                label = model.outputClasses[7];
                isTraining = true;
                break;
            default:
                label = "?";
                isTraining = false;
                currCount++;
                break;
        }



        //TODO: When you stop using Android sensors, you might want to remove the timers and directly add features from Particle to the set
        // Create the timer to start data collection from the Android sensors
        Timer startTimer = new Timer();
        TimerTask startTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accelTime.clear(); accelX.clear(); accelY.clear(); accelZ.clear();
                        gyroTime.clear(); gyroX.clear(); gyroY.clear(); gyroZ.clear();
                        orientTime.clear(); orientX.clear(); orientY.clear(); orientZ.clear();
                        isRecording = true;
                        v2.setEnabled(false);
                    }
                });
            }
        };

        // Create the timer to stop data collection
        Timer endTimer = new Timer();
        TimerTask endTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        computeFeaturesAndAddSamples(isTraining,label, v2);
                    }
                });
            }
        };

        // Start the timers
        startTimer.schedule(startTask, 0);
        endTimer.schedule(endTask, GESTURE_DURATION_SECS*1000);
    }

    public void train() {
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + model.trainDataFilepath;
        File trainingFile = new File(fullFileName);
        if (trainingFile.exists() && !isAnyNewGestureRecorded)
//        if (trainingFile.exists())
        {
            Log.d("TAG", "Training file exists: " + fullFileName);
            model.train(false);

        } else {
            Log.d("TAG", "Need to create training file: " + fullFileName);
            model.train(true);
        }
    }

    /**
     * Trains the model as long as there is at least one sample per class
     */
    public void trainModel(View v) {
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + model.trainDataFilepath;
        File trainingFile = new File(fullFileName);
        if (trainingFile.exists() && !isAnyNewGestureRecorded)
//        if (trainingFile.exists())
        {
            Log.d("TAG","Training file exists: "+fullFileName);
            model.train(false);

        }
        else
        {
            Log.d("TAG","Need to create training file: "+fullFileName);
            model.train(true);
        }

        // Make sure there is training data for each gesture
//        for (int i=0; i<model.outputClasses.length; i++) {
//            int gestureCount = model.getNumTrainSamples(i);
//            if (gestureCount == 0) {
//                Toast.makeText(getApplicationContext(), "Need examples for gesture" + (i+1),
//                        Toast.LENGTH_SHORT).show();
//                return;
//            }
//        }

        // Train
//        model.train();
    }

    /**
     * Resets the training data of the model
     */
    public void clearModel(View v) {
        model.resetTrainingData();
        updateTrainDataCount();
        resultText.setText("Result: ");
        isAnyNewGestureRecorded = false;
    }

    //Deletes the training file
    public void deleteTrainingFile (View v)
    {
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + model.trainDataFilepath;
        File trainingFile = new File(fullFileName);
        trainingFile.delete();
    }

    /**
     * Initializes the graph that will show filtered data
     */
    public void initializeFilteredGraph() {
        graph = findViewById(R.id.graph);
        if (isPlotting) {
            graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
            graph.setBackgroundColor(Color.TRANSPARENT);
            graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
            graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(graphXBounds);
            graph.getViewport().setMinY(-graphYBounds);
            graph.getViewport().setMaxY(graphYBounds);
            timeAccelX.setColor(graphColor[0]);
            timeAccelX.setThickness(5);
            graph.addSeries(timeAccelX);
            timeAccelY.setColor(graphColor[1]);
            timeAccelY.setThickness(5);
            graph.addSeries(timeAccelY);
            timeAccelZ.setColor(graphColor[2]);
            timeAccelZ.setThickness(5);
            graph.addSeries(timeAccelZ);
        }
        else
        {
            graph.setVisibility(View.INVISIBLE);
        }
    }

    public void updateTrainDataCount() {
        gesture1CountText.setText("Num samples: "+model.getNumTrainSamples(0));
        gesture2CountText.setText("Num samples: "+model.getNumTrainSamples(1));
        gesture3CountText.setText("Num samples: "+model.getNumTrainSamples(2));
        gesture4CountText.setText("Num samples: "+model.getNumTrainSamples(3));
        gesture5CountText.setText("Num samples: "+model.getNumTrainSamples(4));
        gesture6CountText.setText("Num samples: "+model.getNumTrainSamples(5));
        gesture7CountText.setText("Num samples: "+model.getNumTrainSamples(6));
        gesture8CountText.setText("Num samples: "+model.getNumTrainSamples(7));
    }
}
