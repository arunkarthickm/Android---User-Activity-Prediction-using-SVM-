package com.example.marun.mc_group22_ass3_arun;

import android.app.Service;
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
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Activity_type;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Accel_X;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Accel_Y;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Accel_Z;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.ActivityLabel;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.sensorCount;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.valueHolder;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.valueHolderClassify;


public class SensorDataService extends Service implements SensorEventListener
{
    private static String TAG = SensorDataService.class.getName().toString();
    private SensorManager sensorManager;
    private Sensor accl_Sensor;
    static SQLiteDatabase sqLiteDatabase;
    static ContentValues values;
    String DBdirectory = "/Android/Data/CSE535_ASSIGNMENT3";
    String TABLE_NAME = "Training_Set";
    String DATABASE_NAME = "GROUP22.db";
    SQLiteDatabase db;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accl_Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,accl_Sensor, SensorManager.SENSOR_DELAY_NORMAL);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        values = new ContentValues();
        CreateNewTableAndAddColumns();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        try
        {
            Sensor sensor = event.sensor;
            if(sensor.getType() == Sensor.TYPE_ACCELEROMETER && !Activity_type.equalsIgnoreCase("Training")) {
                if(sensorCount < 50)
                {
                    DataValues accelrometerData = new DataValues(event.values[0],event.values[1],event.values[2]);
                    if(Activity_type.equals("classify"))
                    {
                        valueHolderClassify.add(accelrometerData);
                    }
                    else
                    {
                        valueHolder.add(accelrometerData);
                    }
                    sensorCount++;
                }
                else if (sensorCount == 50 && db != null)
                {
                    sensorCount++;
                    if(sensorManager!=null)
                    {
                        sensorManager.unregisterListener(this);
                    }
                    if(Activity_type.equals("classify"))
                    {
                        writePredictFile();
                    }
                    else
                    {
                        InsertValues();
                        getAllDataToFile();
                    }
                }
            }
        }
        catch(Exception e)
        {

        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
                String Create_table = "CREATE TABLE " + TABLE_NAME + "( ID INTEGER PRIMARY KEY AUTOINCREMENT)";
                db.execSQL(Create_table);
                for (int i = 1; i <= 50; i++) {
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AccelX" + Integer.toString(i) + " INTEGER");
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AccelY" + Integer.toString(i) + " INTEGER");
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AccelZ" + Integer.toString(i) + " INTEGER");
                }
                String addlabel = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN  LABEL TEXT";
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
    public void CreateFolder(){
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

            values.put(ActivityLabel, Activity_type);
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

            FileWriter writer = new FileWriter(gpxfile);
            BufferedWriter bw = new BufferedWriter(writer);
            if (cursor.moveToFirst()) {
                int rowCount=0;
                while(!cursor.isAfterLast()){
                    String tempActivity = cursor.getString(cursor.getColumnIndex(ActivityLabel)).trim();
                    StringBuilder mActivityRow = new StringBuilder();
                    if(tempActivity.equals("Walking")) {
                        mActivityRow.append("+1");
                    }else if(tempActivity.equals("Running")) {
                        mActivityRow.append("+2");
                    }else if(tempActivity.equals("Jumping")) {
                        mActivityRow.append("+3");
                    }

                    rowCount++;
                    int j = 0;

                    for (int i = 1; i <= 50; i++) {
                        String tempx = (++j) + ":" + cursor.getDouble(cursor.getColumnIndex(Accel_X + i));
                        String tempy = (++j) + ":" + cursor.getDouble(cursor.getColumnIndex(Accel_Y + i));
                        String tempz = (++j) + ":" + cursor.getDouble(cursor.getColumnIndex(Accel_Z + i));
                        mActivityRow.append(" " + tempx + " " + tempy + " " + tempz);
                    }
                    bw.write(mActivityRow.toString());
                    bw.write("\n");
                    cursor.moveToNext();
                }
                bw.flush();
                writer.flush();
                Toast.makeText(this.getApplicationContext(), rowCount, Toast.LENGTH_SHORT).show();
            }
            bw.close();
            writer.close();
            cursor.close();
        } catch (IOException e) {
            Log.d(TAG,"Error getAllDataToFile"+e.toString());
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
            Log.d(TAG,"Error getAllDataToFile"+e.toString());
            e.printStackTrace();
        }

    }



}
