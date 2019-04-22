package edu.uw.ubicomplab.androidaccelapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.LWL;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.core.Instances;

public class Model {
    // Examples of how the .arff format:
    // https://www.programcreek.com/2013/01/a-simple-machine-learning-example-in-java/
    // https://www.cs.waikato.ac.nz/~ml/weka/arff.html
    private Map<String, ArrayList<String[]>> trainingData;
    private String[] testData;
    public Map<String, String> featureNames;
    public String trainDataFilepath = "trainData.arff";
    private String testDataFilepath = "testData.arff";

    private Classifier model;
    private Context context;

    // TODO optional: give your gestures more informative names
    public String[] outputClasses = {"FULL", "EMP", "G3","G4", "G5","G6", "G7", "G8"};

    public Model(Context context) {
        this.context = context;
        resetTrainingData();

        // Specify the features
        featureNames = new TreeMap<>();
        // TODO optional: create more features with more informative names
        featureNames.put("Feature1", "numeric");
        featureNames.put("Feature2", "numeric");
        featureNames.put("Feature3", "numeric");
        featureNames.put("Feature4", "numeric");
        featureNames.put("Feature5", "numeric");
        featureNames.put("Feature6", "numeric");
        featureNames.put("Feature7", "numeric");
        featureNames.put("Feature8", "numeric");
    }

    /**
     * Add a sample to the training or testing set with the corresponding label
     * @param atime: the time for the accelerometer data
     * @param ax: the x-acceleration data
     * @param ay: the y-acceleration data
     * @param az: the z-acceleration data
     * @param gtime: the time for the accelerometer data
     * @param gx: the x-gyroscope data
     * @param gy: the y-gyroscope data
     * @param gz: the z-gyroscope data
     * @param outputLabel: the label for the data
     * @param isTraining: whether the sample should go into the train or test set
     */

    public Double[] computeFeatures(DescriptiveStatistics atime, DescriptiveStatistics ax, DescriptiveStatistics ay, DescriptiveStatistics az,
                                DescriptiveStatistics gtime, DescriptiveStatistics gx, DescriptiveStatistics gy, DescriptiveStatistics gz,
                                DescriptiveStatistics otime, DescriptiveStatistics ox, DescriptiveStatistics oy, DescriptiveStatistics oz)
    {
        Double[] data = new Double[featureNames.keySet().size()];

        // Compute features
        // TODO: replace these placeholders with real calculations involving ax, ay, etc.
//        data[0] = az.getMean();
//        data[1] = az.getMax();
//        data[2] = ay.getSum();
//        data[3] = ax.getSum();
//        data[4] = gy.getVariance();
//        data[0] = az.getMean();
//        data[1] = az.getMax(); //good
//        data[2] = ay.getSum();
//        data[3] = ax.getSum();
//        data[4] = gy.getVariance(); //good
//        data[5] = gy.getMean();
//        data[6] = ay.getMax(); // good
//        data[7] = gz.getMean();
//        data[8] = gz.getMax();
//        data[9] = gz.getVariance();
//        data[10] = gy.getMax();
//        data[11] = ax.getMax(); // good

//        data[0] = gy.getMean();; //good milk
//        data[1] = ay.getMax(); // good milk
//        data[2] = ax.getMax(); // good milk
        data[0] = az.getMean();
        data[1] = az.getMax();
        data[2] = ay.getMax(); // good
        data[3] = ax.getMax();
        data[4] = oy.getMean(); // good
        data[5] = oy.getMin(); // good
        data[6] = oy.getMax();
        data[7] = oy.getVariance(); // good
        Log.d("Pitch", "mean Pitch is: " + oy.getMean());
        Log.d("Pitch", "max Pitch is: " + oy.getMax());
        Log.d("Pitch", "min Pitch is: " + oy.getMin());
        Log.d("Pitch", "variance Pitch is: " + oy.getVariance());



        return data;
    }

    //adds a training instance to the model
    public void addTrainingSample(Double[] features, String outputLabel) {


        // Convert the feature vector to Strings
        String[] stringData = new String[featureNames.keySet().size()];
        for (int i=0; i<featureNames.keySet().size(); i++) {
            stringData[i] = Double.toString(features[i]);
        }

        // Add to the list of feature samples as strings to the training set
        ArrayList<String[]> currentSamples = trainingData.get(outputLabel);
        currentSamples.add(stringData);
        trainingData.put(outputLabel, currentSamples);

    }

    public void assignTestSample(Double[] features)
    {
        //Convert the feature vector to Strings
        String[] stringData = new String[featureNames.keySet().size()];
        for (int i=0; i<featureNames.keySet().size(); i++) {
            stringData[i] = Double.toString(features[i]);
        }
        //Assign the data to the test condition (We are testing only one instance at a time)
        testData = stringData;
    }

    /**
     * Clears all of the data for the model
     */
    public void resetTrainingData() {
        // Create a blank list for each gesture
        trainingData = new LinkedHashMap<>();
        for (String s: outputClasses) {
            trainingData.put(s, new ArrayList<String[]>());
        }
    }

    /**
     * Returns the number of training samples for the given class index
     * @param index: the class index
     * @return the number of samples for the given class index
     */
    public int getNumTrainSamples(int index) {
        String className = outputClasses[index];
        return trainingData.get(className).size();
    }

    /**
     * Create an .arff file for the dataset
     * @param isTraining: whether the data is training or testing data
     */
    private void createDataFile(boolean isTraining) {
        PrintWriter writer;
        // Setup the file writer depending on whether it is training or testing data
        if (isTraining)
            writer = createPrintWriter(trainDataFilepath);
        else
            writer = createPrintWriter(testDataFilepath);

        // Name the dataset
        writer.println("@relation gestures");
        writer.println("");

        // Define the features
        for (String s: featureNames.keySet()) {
            writer.println("@attribute "+s+" "+featureNames.get(s));
        }

        // Define the possible output classes
        String outputOptions = "@attribute gestureName {";
        for (String s: outputClasses) {
            outputOptions += s+", ";
        }
        outputOptions = outputOptions.substring(0, outputOptions.length()-2);
        outputOptions += "}";
        writer.println(outputOptions);
        writer.println("");

        // Write the data
        writer.println("@data");
        if (isTraining) {
            // Go through each category of possible outputs and save their samples
            for (String s: outputClasses) {
                ArrayList<String[]> gestureSamples = trainingData.get(s);
                for (String[] sampleData: gestureSamples) {
                    String sample = "";
                    for (String x: sampleData) {
                        sample += x+",";
                    }
                    sample += s;
                    writer.println(sample);
                }
            }
        }
        else {
            // Write the new sample with a blank label
            String sample = "";
            for (String x: testData) {
                sample += x+",";
            }
            sample += "?";
            writer.println(sample);
        }
        writer.close();
    }

    /**
     * Trains a model for the training data
     */
    public void train(boolean isAddTrainingSamples) {

        // Create the file for training
        if (isAddTrainingSamples)
            createDataFile(true);

        // Read the file and specify the last index as the class
        Instances trainInstances = createInstances(trainDataFilepath);
        if (trainInstances == null) {
            return;
        }
        trainInstances.setClassIndex(trainInstances.numAttributes()-1);
        Toast.makeText(context,
                "Number of total training samples: "+trainInstances.size(),
                Toast.LENGTH_SHORT).show();

        // Define the classifier
        // TODO optional: try out different classifiers provided by Weka
        model = new IBk(1);
        try {
            model.buildClassifier(trainInstances);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Returns the string label for the recently tested gesture
     * @return the string label
     */
    public String test() {
        // Create the file for testing
        createDataFile(false);

        // Read the file and specify the last index as the class
        Instances testInstances = createInstances(testDataFilepath);
        testInstances.setClassIndex(testInstances.numAttributes()-1);

        // Predict
        String classLabel = null;
        try {
            double classIndex = model.classifyInstance(testInstances.instance(0));
            classLabel = testInstances.classAttribute().value((int) classIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classLabel;
    }

    /**
     * Reads the .arff file and converts it into an Instances object
     * @param filename the filepath for the .arff file
     * @return a newly created Instances object
     */
    private Instances createInstances(String filename) {
        // Read the file
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + filename;
        BufferedReader dataReader;
        try {
            FileReader fileReader = new FileReader(fullFileName);
            dataReader = new BufferedReader(fileReader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // Create the training instance
        Instances instances;
        try {
            instances = new Instances(dataReader);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    "Something is wrong with your .arff file!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
        return instances;
    }

    /**
     * Creates the file at the location
     * @param filename: the filename that appears at the root of external storage
     * @return writer: the PrintWriter object to be used
     */
    public PrintWriter createPrintWriter(String filename) {
        // Create the file
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + filename;
        PrintWriter writer;
        try {
            writer = new PrintWriter(fullFileName);
        } catch(FileNotFoundException e) {
            return null;
        }
        return writer;
    }
}
