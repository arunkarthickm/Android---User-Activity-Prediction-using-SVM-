package com.example.marun.mc_group22_ass3_arun;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;
import java.util.*;
import libsvm.*;
import java.text.DecimalFormat;

import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.Activity_type;
import static com.example.marun.mc_group22_ass3_arun.GlobalConstants.curModel;

public class SVMHelper
{
    private svm_parameter svmParams;
    private svm_problem svmProblem;
    private int crossValidation;
    private int nr_fold;
    private double accuracy = 0;
    private double crossVad = 0;
    private boolean checkFile = false;
    private BufferedReader buffer;
    private InputStream is = null;
    private static DecimalFormat df2 = new DecimalFormat(".##");


    public SVMHelper()
    {
        set_parameters();
    }

    //String to Double Converter
    private static double convertToDouble(String s)
    {
        double d = Double.valueOf(s);
        if (!Double.isNaN(d) && !Double.isInfinite(d))
        {
            return (d);
        }

        System.err.print("Input Invalid\n");
        System.exit(1);
        return d;
    }

    //String to Integer Converter
    private static int convertToInt(String s)
    {
        return Integer.parseInt(s);
    }

    //SVM default values - Initialization
    public void set_parameters()
    {
        svmParams = new svm_parameter();
        svmParams.svm_type = svm_parameter.C_SVC;//
        svmParams.kernel_type = svm_parameter.LINEAR;//
        svmParams.degree = 2;
        svmParams.gamma = 0.5;//
        svmParams.coef0 = 0;
        svmParams.nu = 0.5;//
        svmParams.cache_size = 20000;//
        svmParams.C = 1;//
        svmParams.eps = 0.001;//
        svmParams.p = 0.1;
        svmParams.shrinking = 1;
        svmParams.probability = 1;//
        svmParams.nr_weight = 0;
        svmParams.weight_label = new int[0];
        svmParams.weight = new double[0];
        crossValidation = 1;
        nr_fold = 4;//
    }

    //Reads the Data Files which has the Recorded Values
    public void readFile(String fileName) throws IOException
    {
        try
        {
            buffer = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT3" +
                    File.separator + fileName));
        }
        catch(IOException e)
        {
            checkFile = true;
        }

        Vector<Double> yVector = new Vector<Double>();
        Vector<svm_node[]> xVector = new Vector<svm_node[]>();
        int mxmIndex = 0;

        String line = null;
        while((line = buffer.readLine())!= null)
        {
            StringTokenizer strTkn = new StringTokenizer(line," \t\n\r\f:");
            yVector.addElement(convertToDouble(strTkn.nextToken()));
            int temp = strTkn.countTokens()/2;
            svm_node[] x = new svm_node[temp];
            for(int j=0;j<temp;j++)
            {
                x[j] = new svm_node();
                x[j].index = convertToInt(strTkn.nextToken());
                x[j].value = convertToDouble(strTkn.nextToken());
            }
            if(temp>0) mxmIndex = Math.max(mxmIndex, x[temp-1].index);
            xVector.addElement(x);
        }

        //recognizes SVM instances for each attribute from read file
        svmProblem = new svm_problem();
        svmProblem.l = yVector.size();

        svmProblem.x = new svm_node[svmProblem.l][];
        for(int i=0;i<svmProblem.l;i++)
            svmProblem.x[i] = xVector.elementAt(i);

        svmProblem.y = new double[svmProblem.l];
        for(int i=0;i<svmProblem.l;i++)
            svmProblem.y[i] = yVector.elementAt(i);
        //NO GAMMA
        if(svmParams.gamma == 0 && mxmIndex > 0)
            svmParams.gamma = 1.0/mxmIndex;

        //Y = LINAER ARRY - TRAIN LABEL LENGTH
        //L = DATACOUT
        //X = 2D ARRAY

        /*
        if(svmParams.kernel_type == svm_parameter.PRECOMPUTED)
        {
            for(int i=0;i<svmProblem.l;i++)
            {
                if (svmProblem.x[i][0].index != 0)
                {
                    System.err.print("kernel matrix is incorrect\n");
                    System.exit(1);
                }
                if ((int)svmProblem.x[i][0].value <= 0 || (int)svmProblem.x[i][0].value > mxmIndex)
                {
                    System.err.print("input format is incorrect\n");
                    System.exit(1);
                }
            }
        }
        */

        buffer.close();
    }

    //performs cross validation on trained SVM
    private double crossValidation()
    {
        int i;
        double[] t;
        int totalC = 0;
        double totalE = 0;
        double vSum = 0, ySum = 0, vvSum = 0, yySum = 0, vySum = 0;

        t = new double[svmProblem.l];

        svm.svm_cross_validation(svmProblem,svmParams,nr_fold,t);

        svm_parameter svmTemp = new svm_parameter();

        /*
        if(svmParams.svm_type == svmTemp.EPSILON_SVR ||

                svmParams.svm_type == svmTemp.NU_SVR)

        {
            for(i=0;i<svmProblem.l;i++)
            {
                double y = svmProblem.y[i];
                double v = t[i];
                totalE += (v-y)*(v-y);
                vSum += v;
                ySum += y;
                vvSum += v*v;
                yySum += y*y;
                vySum += v*y;
            }
            System.out.print("Cross Validation Mean squared error = "+totalE/svmProblem.l+"\n");
            System.out.print("Cross Validation Squared correlation coefficient = "+
                    ((svmProblem.l*vySum-vSum*ySum)*(svmProblem.l*vySum-vSum*ySum))/
                            ((svmProblem.l*vvSum-vSum*vSum)*(svmProblem.l*yySum-ySum*ySum))+"\n"
            );
        }
        else
        {
        */
        for(i=0;i<svmProblem.l;i++)
            if(t[i] == svmProblem.y[i])
                ++totalC;
        crossVad = ((100.0 * totalC) / svmProblem.l)+1;
        return crossVad;

    }

    public double TrainTest(svm_model r)
    {
        try{

            try
            {
                buffer = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT3" +
                        File.separator + "activitydb1.txt"));
            }
            catch(IOException e)
            {
                checkFile = true;
            }


            String line = null;
            int correct = 0, total = 0;

            /*File root = new File(Environment.getExternalStorageDirectory(), "/Android/Data/CSE535_ASSIGNMENT3/");
            FileInputStream fis = new FileInputStream(root + "svmmodel.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            svm_model r = (svm_model) ois.readObject();
            ois.close();
            */
            while((line = buffer.readLine())!= null)
            {
                total++;
                StringTokenizer strTkn = new StringTokenizer(line," \t\n\r\f:");
                double res = convertToDouble(strTkn.nextToken());
                int temp = strTkn.countTokens()/2;
                svm_node[] x = new svm_node[temp];
                for(int j=0;j<temp;j++)
                {
                    x[j] = new svm_node();
                    x[j].index = convertToInt(strTkn.nextToken());
                    x[j].value = convertToDouble(strTkn.nextToken());
                }
                double sol = svm.svm_predict(r, x);
                if(sol == res)
                {
                    correct++;
                }
            }
            accuracy = (double)100 * (double)correct/(double)total;
        }
        catch(Exception ex)
        {

        }
        return accuracy;

    }

    public String Train()
    {

        try
        {
            readFile("activitydb.txt");
            String error_msg = svm.svm_check_parameter(svmProblem,svmParams);
            if(error_msg != null)
            {
                return error_msg;
            }

            curModel = svm.svm_train(svmProblem,svmParams);
            double cv = crossValidation();


            File root = new File(Environment.getExternalStorageDirectory(), "/Android/Data/CSE535_ASSIGNMENT3/svmmodel.ser");
            if (!root.exists())
            {
                root.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(root);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(curModel);

            oos.close();

            double a = TrainTest(curModel);

            return  "Accuracy - " + a + " (Cross Validation - " + df2.format(cv) +")";

        }
        catch(Exception ex)
        {
            return "error";
        }
    }

    public String Classify()
    {
        String result = "default";
        try
        {
            try
            {
                buffer = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT3" +
                        File.separator + "predictdb.txt"));
            }
            catch(IOException e)
            {
                checkFile = true;
            }

            String line = null;
            svm_node[] x = new svm_node[150];
            while((line = buffer.readLine())!= null)
            {
                StringTokenizer strTkn = new StringTokenizer(line," \t\n\r\f:");
                int temp = strTkn.countTokens()/2;
                x = new svm_node[temp];
                for(int j=0;j<temp;j++)
                {
                    x[j] = new svm_node();
                    x[j].index = convertToInt(strTkn.nextToken());
                    x[j].value = convertToDouble(strTkn.nextToken());
                }
            }

            String Res = "";

            File root = new File(Environment.getExternalStorageDirectory(), "/Android/Data/CSE535_ASSIGNMENT3/svmmodel.ser");
            FileInputStream fis = new FileInputStream(root);
            ObjectInputStream ois = new ObjectInputStream(fis);
            svm_model r = (svm_model) ois.readObject();
            ois.close();

            double val = svm.svm_predict(r,x);
            int i = (int) Math.round(val);

            switch (i)
            {
                case 1:
                    Res = "Walking";
                    break;
                case 2:
                    Res = "Jumping";
                    break;
                case 3:
                    Res = "Running";
                    break;
            }
            return Res;
        }
        catch(IOException e)
        {
            return "error";
        }
        catch(Exception ex)
        {
            return "error";
        }
    }
}
