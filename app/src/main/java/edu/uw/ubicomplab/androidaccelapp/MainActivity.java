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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    Double[] prevFeatures;
    Double[] features;
    private String countFilepath = "count.txt";
    int prevAverage = 12;
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

    private static final int GESTURE_DURATION_SECS = 8;

    boolean isAnyNewGestureRecorded = false;
    int count = 0;
    int numSamples = 0;

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

        // Initialize the graphs
        initializeFilteredGraph();


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
            }
        }
        else
            model.assignTestSample(features);

        // Predict if the recent sample is for testing
        if (!isTraining) {
            String result = model.test();

            if (result != null) {
//                if((result.compareTo("FULL") == 0) && (count < (0.125*prevAverage))){
//                    resultText.setText("Result: " + "100%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
//                }
//                else if((result.compareTo("FULL") == 0) && (count < (0.375*prevAverage) && (count >= (0.125*prevAverage)))){
//                    resultText.setText("Result: " + "75%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
//                }
//                else if(count < (0.625*prevAverage) && (count >= (0.375*prevAverage))){
//                    resultText.setText("Result: " + "50%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
//                }
//                else if((result.compareTo("EMP") == 0) && (count >= (0.635*prevAverage))&&(count < (0.875*prevAverage))){
//                    resultText.setText("Result: " + "25%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
//                }
//                else if((result.compareTo("EMP") == 0)&& (count >= (0.875*prevAverage)) ){
//                    resultText.setText("Result: " + "0%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
//                }
//                else{
//                    result = "Unsure:" + result + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage);
//                    resultText.setText("Result: " +result);
//                }
                if (result.compareTo("EMP") != 0){
                    if((count < (0.125*prevAverage))){
                        resultText.setText("Result: " + "100%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
                    }
                    else if((count < (0.375*prevAverage) && (count >= (0.125*prevAverage)))){
                        resultText.setText("Result: " + "75%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
                    }
                    else if(count < (0.625*prevAverage) && (count >= (0.375*prevAverage))){
                        resultText.setText("Result: " + "50%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
                    }
                    else if((count >= (0.635*prevAverage))&&(count < (0.875*prevAverage))){
                        resultText.setText("Result: " + "25%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
                    }
//                    else if( (count >= (0.875*prevAverage)) ){
//                        resultText.setText("Result: " + "0%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
//                    }
                }
                 else{
                     if (count > (0.5*prevAverage)) {
                         resultText.setText("Result: " + "0%" + "Count:" + String.valueOf(count) + "Normal:" + String.valueOf(prevAverage));
                     } else{
                         resultText.setText("Unsure redo");
                     }
                }
            }
        }

        // Update number of samples shown
        updateTrainDataCount();
        v2.setEnabled(true);
    }
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
                count = 0;
                break;
            case R.id.gesture2Button: //EMPTY
                numSamples++;
                label = model.outputClasses[1];
                isTraining = true;
                //save count to file
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
                            Log.d("prev total", String.valueOf(prevTotal));
                            Log.d("prev num samples", String.valueOf(prevNumSamples));
                            dataReader.close();
                            int newCount = (prevTotal + count);
                            int newNumSamples = prevNumSamples+1;
                            Log.d("prev newNumSamples", String.valueOf(newNumSamples));
                            prevAverage = Math.round(newCount/newNumSamples);
                            BufferedWriter writer;
                            try {
                                writer = new BufferedWriter(new FileWriter(fullFileName, false));
                                writer.write(Integer.toString(newCount));
                                writer.newLine();
                                writer.write(Integer.toString(newNumSamples));
                                writer.close();
                            } catch(FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                } else{
                    try {
                       BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
                       writer.write(Integer.toString(count));
                       writer.newLine();
                       writer.write(Integer.toString(numSamples));
                       writer.close();
                    }
                    catch (IOException e) {
                        Log.e("Exception", "File write failed: " + e.toString());
                    }
                }
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
                count++;
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
