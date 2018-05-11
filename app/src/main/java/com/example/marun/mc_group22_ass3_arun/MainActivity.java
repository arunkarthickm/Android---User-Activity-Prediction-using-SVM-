package com.example.marun.mc_group22_ass3_arun;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Accel_X;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Accel_Y;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Accel_Z;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.ActivityLabel;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Activity_type;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.valueHolder;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.valueHolderClassify;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{

    Intent serviceIntent;

    TextView ParameterHeading;
    TextView Parameters;
    //TextView ResultsHeading;
    TextView Accuracy;

    //Accelerometer sensors
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    boolean senseFlag = false;
    int dataCount = 0;
    long lastUpdate = System.currentTimeMillis();

    String DBdirectory = "/Android/Data/CSE535_ASSIGNMENT3";
    String TABLE_NAME = "SVM_input";
    String DATABASE_NAME = "GROUP22.db";
    SQLiteDatabase db;
    ProgressDialog progressDialog;
    SensorEventListener parent;
    volatile boolean collect = false;
    SVMHelper svmArun;

    double currX, currY, currZ;

    Runnable ar = new Runnable()
    {
        @Override
        public void run() {

            while(!collect);

            while(senseFlag && !Activity_type.equalsIgnoreCase("Training"))
            {

                if(dataCount < 50)
                {
                    dataCount++;
                    DataValues acceData = new DataValues(currX, currY, currZ);
                    if(Activity_type.equals("classify"))
                    {
                        valueHolderClassify.add(acceData);
                    }
                    else {
                        valueHolder.add(acceData);
                    }
                }
                else if(dataCount >= 50 && db != null)
                {
                    sensorManager.unregisterListener(parent);
                    collect = false;
                    progressDialog.dismiss();
                    senseFlag = false;
                    dataCount++;
                    if(Activity_type.equals("classify"))
                    {
                        writePredictFile();
                        SVMClassifyTask ss = new SVMClassifyTask();
                        ss.execute();
                    }
                    else
                    {
                        InsertValues();
                        //getAllDataToFile();
                    }
                }

                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {

                }
            }
        }
    };


    void setUIParameters()
    {
        ParameterHeading.setVisibility(View.VISIBLE);
        Parameters.setVisibility(View.VISIBLE);
        //ResultsHeading.setVisibility(View.VISIBLE);
        Accuracy.setVisibility(View.VISIBLE);

        Parameters.setText("The SVM type determines the type of penality for misclassification\n svm_type = svm_parameter.C_SVC\n" +
                "SVM Kernel Type - linear\nkernel_type = svm_parameter.LINEAR;\n" +
                "This parameter specifies coef0 for 'poly' and 'precomputed' kernel functions.\ncoef0 = 0;\n" +
                "Cache size for SVM Computation cache_size = 20000;\n" +
                "C is the penalty parameter of the error term.\n"+
                "C = 1;\n" +
                "Epsilon parameter specifies the tolerance of the termination criterion.\n"+
                "epsilon = 1e-2\n" +
                "nr_fold parameter specifies the no. of folds for cross validation\n"+
                "nr_fold = 4;\n");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sensor Declaration section
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        parent = this;
        CreateNewTableAndAddColumns();

        svmArun = new SVMHelper();


        ParameterHeading = (TextView) (TextView) findViewById(R.id.parametersHeading);
        Parameters = (TextView) (TextView) findViewById(R.id.parameters);
        //ResultsHeading = (TextView) (TextView) findViewById(R.id.Results);
        Accuracy = (TextView) (TextView) findViewById(R.id.accuracyValue);

        ParameterHeading.setVisibility(View.INVISIBLE);
        Parameters.setVisibility(View.INVISIBLE);
        //ResultsHeading.setVisibility(View.INVISIBLE);
        Accuracy.setVisibility(View.INVISIBLE);


    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        Sensor sen = event.sensor;
        if(sen.getType() == Sensor.TYPE_ACCELEROMETER && senseFlag && !Activity_type.equalsIgnoreCase("Training"))
        {
            currX = event.values[0];
            currY = event.values[1];
            currZ = event.values[2];

            /*
            if(!Activity_type.equalsIgnoreCase("Classify"))
            {
                collect = true;
                Thread collData = new Thread(ar);
                if(!collData.isAlive())
                    collData.start();
            }
            */

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    public void onWalkingClick(View view)
    {
        reset();
        Activity_type = "Walking";
        //LoadData task = new LoadData();
        //task.execute();
        valueHolder = new ArrayList<DataValues>();
        sensorManager.registerListener(this, accelerometerSensor , SensorManager.SENSOR_DELAY_FASTEST);
        //call
        senseFlag = true;
        dataCount = 0;
        //collect = true;

        RecordTask ss =  new RecordTask();
        ss.execute();

        //progressDialog = ProgressDialog.show(MainActivity.this, Activity_type, "Collecting Sensor Data", true);


    }

    public void onRunningClick(View view)
    {
        reset();
        Activity_type = "Running";
        //LoadData task = new LoadData();
        //task.execute();
        valueHolder = new ArrayList<DataValues>();
        //call
        sensorManager.registerListener(this, accelerometerSensor , SensorManager.SENSOR_DELAY_FASTEST);
        senseFlag = true;
        dataCount = 0;
        //collect = true;

        RecordTask ss =  new RecordTask();
        ss.execute();

        //progressDialog = ProgressDialog.show(MainActivity.this, Activity_type,"Collecting Sensor Data", true);

    }


    void reset()
    {
        Activity_type = "";
        valueHolder = new ArrayList<DataValues>();
        //call
        sensorManager.unregisterListener(this, accelerometerSensor);
        senseFlag = false;
        dataCount = 0;
        //collect = false;

        ParameterHeading.setVisibility(View.INVISIBLE);
        Parameters.setVisibility(View.INVISIBLE);
        //ResultsHeading.setVisibility(View.INVISIBLE);
        Accuracy.setVisibility(View.INVISIBLE);
    }

    public void onClearClick(View view)
    {
        File root = new File(Environment.getExternalStorageDirectory(), "/Android/Data/CSE535_ASSIGNMENT3/");
        if (root.exists()) {
            File gpxfile = new File(root, "GROUP22.db");
            if(gpxfile.exists()) {
                gpxfile.delete();
                CreateNewTableAndAddColumns();
            }
        }


    }


    public void onjumpingClick(View view)
    {
        reset();
        Activity_type = "Jumping";
        //LoadData task = new LoadData();
        //task.execute();
        valueHolder = new ArrayList<DataValues>();
        //call
        sensorManager.registerListener(this, accelerometerSensor , SensorManager.SENSOR_DELAY_FASTEST);
        senseFlag = true;
        dataCount = 0;
        //collect = true;

        RecordTask ss =  new RecordTask();
        ss.execute();


        //progressDialog = ProgressDialog.show(MainActivity.this, Activity_type, "Collecting Sensor Data", true);
    }

    public void onTrainingClicked(View v)
    {
        reset();
        getAllDataToFile();
        Activity_type = "Training";

        SVMTrainTask ss =  new SVMTrainTask();
        ss.execute();

    }

    public void onClassifyClick(View v)
    {
        reset();
        Activity_type = "classify";
        valueHolderClassify = new ArrayList<DataValues>();
        //LoadData task = new LoadData();
        //task.execute();
        //call

        sensorManager.registerListener(this, accelerometerSensor , SensorManager.SENSOR_DELAY_FASTEST);
        senseFlag = true;
        dataCount = 0;

        SVMClassifyTask ss =  new SVMClassifyTask();
        ss.execute();

        //collect = true;
    }

    public void onViewClick(View v) {
        Intent intent= new Intent(MainActivity.this, com.example.marun.mc_group22_ass3_arun.Viewer.class);
        startActivity(intent);
    }


    public void CreateNewTableAndAddColumns()
    {
        CreateFolder();
        try
        {
            db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + DBdirectory + "/" + DATABASE_NAME, null);
        }
        catch(Exception ex)
        {
            Log.d("EXCEPTION", "createtable: "+ex.getMessage());
        }
        db.beginTransaction();
        try {
            if(!checkTableExistence(TABLE_NAME))
            {
                String Create_table = "CREATE TABLE " + TABLE_NAME + "( Id INTEGER PRIMARY KEY AUTOINCREMENT)";
                db.execSQL(Create_table);
                for (int i = 1; i <= 50; i++) {
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AccelX_" + Integer.toString(i) + " INTEGER");
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AccelY_" + Integer.toString(i) + " INTEGER");
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AccelZ_" + Integer.toString(i) + " INTEGER");
                }
                String addlabel = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN  Label INTEGER";
                db.execSQL(addlabel);
                db.setTransactionSuccessful();
            }
        }
        catch (SQLiteException e)
        {

        }
        finally
        {
            db.endTransaction();
        }
    }

    public boolean checkTableExistence(String tableName)
    {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+tableName+"'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    //Create the required folder in the internal storage
    public void CreateFolder()
    {
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + DBdirectory);
            boolean success = true;
            if (!folder.exists())
            {
                success = folder.mkdirs();
            }
            if (success)
            {
            }
            else
            {
            }
        }
        catch(Exception e){
            Toast.makeText(this,"Folder creation failed... Please try again!", Toast.LENGTH_SHORT).show();
        }
    }

    //Database Insert Operation
    public void InsertValues()
    {
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            for (int i = 1; i < 2; i++) {
                values.put(Accel_X + Integer.toString(i), valueHolder.get(i).getX());
                values.put(Accel_Y + Integer.toString(i), valueHolder.get(i).getY());
                values.put(Accel_Z + Integer.toString(i), valueHolder.get(i).getZ());
            }


            switch (Activity_type)
            {
                case "Walking":
                    values.put("Label", 1);
                    break;

                case "Jumping":
                    values.put("Label", 2);
                    break;

                case "Running":
                    values.put("Label", 3);
                    break;
            }

            long newRowId = db.insert(TABLE_NAME, null, values);

            for(int i =2;i<=50;i++) {
                String Update = "UPDATE " + TABLE_NAME
                        + " SET "
                        + Accel_X + Integer.toString(i) + " = " + valueHolder.get(i).getX() + ", "
                        + Accel_Y + Integer.toString(i) + " = " + valueHolder.get(i).getY() + ", "
                        + Accel_Z + Integer.toString(i) + " = " + valueHolder.get(i).getZ()
                        + " WHERE ID = " + newRowId;
                db.execSQL(Update);
            }

            String op = "";
        }
        catch(Exception e)
        {

        }
        finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }




    public void getAllDataToFile()
    {
        String mQuery = "SELECT  * FROM " + TABLE_NAME;
        db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + DBdirectory + "/" + DATABASE_NAME, null);
        Cursor cursor = db.rawQuery(mQuery, null);

        try {

            File root = new File(Environment.getExternalStorageDirectory(), "/Android/Data/CSE535_ASSIGNMENT3/");
            if (!root.exists()) {
                root.mkdirs();
            }

            File gpxfile = new File(root, "activitydb.txt");
            if(gpxfile.exists()) {
                gpxfile.delete();
            }
            gpxfile = new File(root, "activitydb.txt");
            gpxfile.createNewFile();


            File gpxfile1 = new File(root, "activitydb1.txt");
            if(gpxfile1.exists()) {
                gpxfile1.delete();
            }
            gpxfile1 = new File(root, "activitydb1.txt");
            gpxfile1.createNewFile();


            FileWriter writer = new FileWriter(gpxfile);
            BufferedWriter bw = new BufferedWriter(writer);

            FileWriter writer1 = new FileWriter(gpxfile1);
            BufferedWriter bw1 = new BufferedWriter(writer1);

            if (cursor.moveToFirst()) {
                int rowCount = 0;
                while(!cursor.isAfterLast())
                {
                    int activity = cursor.getInt((int)cursor.getColumnIndex("Label"));
                    StringBuilder mActivityRow = new StringBuilder();
                    mActivityRow.append(activity);
                    rowCount++;
                    int j = 0;

                    for (int i = 1; i <= 50; i++)
                    {
                        String tempx = (++j) + ":" + cursor.getDouble(cursor.getColumnIndex(Accel_X + i));
                        String tempy = (++j) + ":" + cursor.getDouble(cursor.getColumnIndex(Accel_Y + i));
                        String tempz = (++j) + ":" + cursor.getDouble(cursor.getColumnIndex(Accel_Z + i));
                        mActivityRow.append(" " + tempx + " " + tempy + " " + tempz);
                    }

                    if(rowCount == 20 || rowCount == 17 || rowCount == 18 || rowCount == 19)
                    {
                        bw1.write(mActivityRow.toString());
                        bw1.write("\n");
                        if(rowCount == 20)
                            rowCount = 0;
                    }
                    else
                    {
                        bw.write(mActivityRow.toString());
                        bw.write("\n");
                    }

                    cursor.moveToNext();
                }
                bw.flush();
                writer.flush();

                bw1.flush();
                writer1.flush();
            }
            bw.close();
            writer.close();
            bw1.close();
            writer1.close();

            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    void writePredictFile()
    {
        try {

            File root = new File(Environment.getExternalStorageDirectory(), "/Android/Data/CSE535_ASSIGNMENT3/");
            if (!root.exists()) {
                root.mkdirs();
            }

            File gpxfile = new File(root, "predictdb.txt");
            if(gpxfile.exists()) {
                gpxfile.delete();
            }
            gpxfile = new File(root, "predictdb.txt");
            gpxfile.createNewFile();



            FileWriter writer = new FileWriter(gpxfile);
            BufferedWriter bw = new BufferedWriter(writer);



            StringBuilder mActivityRow = new StringBuilder();
            int j = 0;
            for (int i = 0; i < 50; i++) {
                mActivityRow.append((++j) + ":" + valueHolderClassify.get(i).getX() + " ");
                mActivityRow.append((++j) + ":" + valueHolderClassify.get(i).getY() + " ");
                mActivityRow.append((++j) + ":" + valueHolderClassify.get(i).getZ() + " ");
            }
            bw.write(mActivityRow.toString());
            bw.write("\n");
            bw.flush();
            writer.flush();

            bw.close();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private class SVMTrainTask extends AsyncTask<Void, String, Void>
    {
        //Status Flag
        int flag = 0;
        ProgressDialog waitDialog;
        //string formatter

        String trainRes;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            waitDialog = ProgressDialog.show(MainActivity.this,"","SVM Training... Please Wait!",true);
        }

        protected Void doInBackground(Void... params) {
            try {
                //fetching the local db file
                trainRes = svmArun.Train();
            }
            catch(Exception e)
            {

            }
            return null;
        }

        protected void onProgressUpdate(String... value) {
            super.onProgressUpdate(value);


        }

        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            waitDialog.dismiss();
            setUIParameters();
            Accuracy.setText("RESULTS\n"+trainRes);
        }
    }


    private class SVMClassifyTask extends AsyncTask<Void, String, Void>
    {
        //Status Flag
        int flag = 0;
        ProgressDialog waitDialog;
        //string formatter

        String trainRes;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            waitDialog = ProgressDialog.show(MainActivity.this,"","Collecting data and SVM Classifying... Please Wait!",true);
        }

        protected Void doInBackground(Void... params) {
            try {
                //fetching the local db file

                while(dataCount<50 && senseFlag && !Activity_type.equalsIgnoreCase("Training"))
                {
                    dataCount++;
                    DataValues acceData = new DataValues(currX, currY, currZ);
                    if(Activity_type.equals("classify"))
                    {
                        valueHolderClassify.add(acceData);
                    }

                    try
                    {
                        Thread.sleep(150);
                    }
                    catch (InterruptedException e)
                    {

                    }
                }

                if(dataCount >= 50 && db != null)
                {
                    sensorManager.unregisterListener(parent);
                    collect = false;
                    senseFlag = false;
                    dataCount++;
                    if(Activity_type.equalsIgnoreCase("classify"))
                    {
                        writePredictFile();
                    }
                }

                trainRes = svmArun.Classify();
            }
            catch(Exception e)
            {

            }
            return null;
        }

        protected void onProgressUpdate(String... value) {
            super.onProgressUpdate(value);
        }

        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            waitDialog.dismiss();
            setUIParameters();
            Accuracy.setText("RESULTS\n"+trainRes);
        }
    }


    private class RecordTask extends AsyncTask<Void, String, Void>
    {
        //Status Flag
        int flag = 0;
        ProgressDialog waitDialog;
        //string formatter

        String trainRes;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            waitDialog = ProgressDialog.show(MainActivity.this,"","Collecting Data... Please Wait!",true);
        }

        protected Void doInBackground(Void... params)
        {
            while(senseFlag && !Activity_type.equalsIgnoreCase("Training") && dataCount < 50 && !(Activity_type.equalsIgnoreCase("classify")))
            {
                dataCount++;
                DataValues acceData = new DataValues(currX, currY, currZ);
                valueHolder.add(acceData);

                try
                {
                    Thread.sleep(150);
                }
                catch (InterruptedException e)
                {

                }
            }
            return null;
        }

        protected void onProgressUpdate(String... value) {
            super.onProgressUpdate(value);


        }

        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            waitDialog.dismiss();
            if(dataCount >= 50 && db != null && !Activity_type.equalsIgnoreCase("classify") &&
                    !Activity_type.equalsIgnoreCase("train"))
            {
                sensorManager.unregisterListener(parent);
                collect = false;
                senseFlag = false;
                dataCount++;
                InsertValues();
                Parameters.setText(Activity_type + " - Accelerometer Data Recorded - Successfully");
                Parameters.setVisibility(View.VISIBLE);
                //getAllDataToFile();
            }

        }
    }







}
