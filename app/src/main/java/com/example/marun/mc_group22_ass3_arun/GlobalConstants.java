package com.example.marun.mc_group22_ass3_arun;

import java.util.ArrayList;

import libsvm.svm_model;

public class GlobalConstants {
    static String Accel_X  = "AccelX_";
    static String Accel_Y  = "AccelY_";
    static String Accel_Z  = "AccelZ_";
    static String ActivityLabel  = "Label";
    static int sensorCount = 0;
    static String Activity_type;
    static ArrayList<DataValues> valueHolder = new ArrayList<DataValues>();
    static ArrayList<DataValues> valueHolderClassify = new ArrayList<DataValues>();
    static svm_model curModel;
}
