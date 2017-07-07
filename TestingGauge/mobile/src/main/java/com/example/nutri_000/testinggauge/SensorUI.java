package com.example.nutri_000.testinggauge;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;

public class SensorUI extends AppCompatActivity {
    public ImageButton connect;
    public ProgressBar rightPB, leftPB;
    public SeekBar rightSB, leftSB;
    public TextView rightTV, leftTV;
    public RelativeLayout relativeLayout;
    public float average;
    public int calibrateCounter;
    boolean calibrate;
    BluetoothGatt gatt;
    boolean search;
    static TextView sensorStatus;
    public SensorUI(int button, int rPB, int lPB, int rSB, int lSB, int rTV, int lTV, int relativeLO, Activity MainActivity){
        connect = (ImageButton) MainActivity.findViewById(button);
        rightPB = (ProgressBar) MainActivity.findViewById(rPB);
        leftPB = (ProgressBar) MainActivity.findViewById(lPB);
        rightSB = (SeekBar) MainActivity.findViewById(rSB);
        leftSB = (SeekBar) MainActivity.findViewById(lSB);
        rightTV = (TextView) MainActivity.findViewById(rTV);
        leftTV = (TextView) MainActivity.findViewById(lTV);
        relativeLayout = (RelativeLayout) MainActivity.findViewById(relativeLO);
        average = 0;
        calibrateCounter = 0;
        calibrate = false;
        search = false;
        sensorStatus = (TextView) MainActivity.findViewById(R.id.SensorStatus);
        //BluetoothGatt gatt = null;
    }
    public void searchSensor(final SensorUI sensor, Status status, BluetoothLeScanner scanner, ScanCallback mScanCallback ){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //setSensorStatus("Searching");
                //sensor.connect.setBackgroundResource(R.drawable.hipyellow);
            }
        });
        sensor.search = true;
        status.scanning = true;
        scanner.startScan(mScanCallback);
        //timerHandler.postDelayed(scanTimeout,10000);
    }
    public void calibrateSensor(final SensorUI sensor){
        //zero the sensor
        calibrate = true;
        calibrateCounter = 0;
        average = 0;
        sensor.leftPB.setProgress(0);
        sensor.rightPB.setProgress(0);
    }
    //SENSOR STATUS TEXT
    /*public void setSensorStatus(String message)
    {
        final String msg = "Sensor " + message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sensorStatus.setText(msg);
            }
        });
    }*/

}
