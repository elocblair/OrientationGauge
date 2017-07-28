package com.example.nutri_000.testinggauge;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.util.Log;



public class MainActivity extends AppCompatActivity {

    BleService bleService;
    boolean isBound = false;
    boolean stimNow = false;


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.BleBinder binder = (BleService.BleBinder) service;
            bleService = binder.getService();
            isBound = true;
            bleService.initializeBle();
            bleService.scanner.startScan(bleService.mScanCallback);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
            isBound = false;
        }
    };

    private static final String TAG = "Cole";

    private final static int REQUEST_ENABLE_BT = 1;

    Handler timerHandler = new Handler();
    Status statusVariables = new Status();
    FireflyCommands fireflyCommands = new FireflyCommands();
    SensorUI hipUI;
    SensorUI kneeUI;
    SensorUI ankleUI;

    private static Context context;

    //BLE connections for the firefly
    private FloatingActionButton stimButton;

    //ble connections for the sensor
    private BluetoothGattCharacteristic NRF_CHARACTERISTIC;
    private TextView sensorStatus;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState!= null){

        }
        else{
            // all UI components for main activity
            setContentView(R.layout.activity_main);

            hipUI = new SensorUI(R.id.upperLegButton, R.id.progressBarTopRight, R.id.progressBarTopLeft, R.id.seekBarTopRight, R.id.seekBarTopLeft,
                    R.id.topAngle, R.id.topAngleL,R.id.relativeHip, this );
            hipUI.leftPB.setRotation(180);

            hipUI.green = R.drawable.hipgreen;
            hipUI.yellow = R.drawable.hipyellow;
            hipUI.white = R.drawable.hipwhite;


            kneeUI = new SensorUI(R.id.lowerLegButton, R.id.progressBarMidRight, R.id.progressBarMidLeft, R.id.seekBarMidRight,R.id.seekBarMidLeft,
                    R.id.midAngle, R.id.midAngleL, R.id.relativeKnee, this);
            kneeUI.leftPB.setRotation(180);

            kneeUI.green = R.drawable.kneegreen;
            kneeUI.yellow = R.drawable.kneeyellow;
            kneeUI.white = R.drawable.kneewhite;

            ankleUI = new SensorUI(R.id.footButton,R.id.progressBarBottomRight,R.id.progressBarBottomLeft,R.id.seekBarBottomRight,R.id.seekBarBottomLeft,
                    R.id.bottomAngle,R.id.bottomAngleL, R.id.relativeAnkle, this);
            ankleUI.leftPB.setRotation(180);

            ankleUI.green = R.drawable.anklegreen;
            ankleUI.yellow = R.drawable.ankleyellow;
            ankleUI.white = R.drawable.anklewhite;

            stimButton = (FloatingActionButton) findViewById(R.id.stim_buton);
            sensorStatus = (TextView) findViewById(R.id.SensorStatus);

            stimButton.bringToFront();

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            registerReceiver(broadcastReceiver, new IntentFilter("bleService"));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }*/
        Log.v("onDestroy", "DESTROYED");
    }
    @Override
    protected void onStop() {

        super.onStop();

        /*if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }*/
        //adapter.disable();
        Log.v("onStop", "STOPPED");
    }
    @Override
    protected void onPause(){
        super.onPause();

        /*if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }*/
    }

    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

    }

    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        int restore = 42;
        savedInstanceState.putInt("SOMETHING", restore);
    }

    //stim button clicked
    public void stimClicked(View v)
    {
        if(!statusVariables.stimming) {
            statusVariables.stimming = true;
            triggerFirefly(fireflyCommands.startStim);
            timerHandler.postDelayed(fireflyStop, 1000);
            timerHandler.postDelayed(fireflyDebounce,5000);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSION_REQUEST_COARSE_LOCATION:
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "coarse location permission granted");
                    Intent bleIntent = new Intent(this, BleService.class);
                    startService(bleIntent);
                    bindService(bleIntent, mServiceConnection, this.BIND_AUTO_CREATE);
                }
                else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                    {

                        @Override
                        public void onDismiss(DialogInterface dialog)
                        {
                            //add code to handle dismiss
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public static Context getAppContext(){
        return MainActivity.context;
    }
    public void checkValue(final int value, final SensorUI sensor){
        if (value > sensor.rightSB.getProgress() | (value*-1) > sensor.leftSB.getProgress()){
            if(!statusVariables.stimming) {
                statusVariables.stimming = true;
                Log.v(TAG, "Start command");
                triggerFirefly(fireflyCommands.startStim);
                timerHandler.postDelayed(fireflyStop, 1000);
                timerHandler.postDelayed(fireflyDebounce,5000);
            }
        }
    }
    //GAUGE
    public void setGaugeValue(final int value, final SensorUI sensor) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(value < 0) {
                    sensor.leftPB.setProgress(-1*value);
                    sensor.rightPB.setProgress(0);

                }
                else if(value > 0){
                    sensor.leftPB.setProgress(0);
                    sensor.rightPB.setProgress(value);
                }
                if (value > sensor.rightSB.getProgress() | (value*-1) > sensor.leftSB.getProgress()){
                    sensor.relativeLayout.setBackgroundColor(Color.parseColor("#008542"));
                }

                if (value < sensor.rightSB.getProgress() & (value*-1) < sensor.leftSB.getProgress()){

                    sensor.relativeLayout.setBackgroundColor(Color.parseColor("#404040"));
                    if(sensor == kneeUI){
                        sensor.relativeLayout.setBackgroundColor(Color.parseColor("#333333"));
                    }

                }
                if(value >= 0 ){
                    sensor.rightTV.setText(Integer.toString(value) + "/" + Integer.toString(sensor.rightSB.getProgress()));
                    sensor.leftTV.setText("0/"+ Integer.toString(sensor.leftSB.getProgress()));
                }
                if(value <= 0) {
                    sensor.leftTV.setText(Integer.toString(-1*value) + "/" + Integer.toString(sensor.leftSB.getProgress()));
                    sensor.rightTV.setText("0/" + Integer.toString(sensor.rightSB.getProgress()));
                }
            }
        });
    }


    //SENSOR STATUS TEXT
    public void setSensorStatus(String message)
    {
        final String msg = "Sensor " + message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                sensorStatus.setText(msg);

            }
        });
    }
    public void triggerFirefly(byte[] onOff)
    {
        if(bleService.fireflyFound){
            bleService.FIREFLY_CHARACTERISTIC2.setValue(onOff);
            boolean b = bleService.fireflyGatt.writeCharacteristic(bleService.FIREFLY_CHARACTERISTIC2);
            Log.i(TAG, "firefly write status = " + b);
        }

    }
    Runnable fireflyStop = new Runnable() {
        @Override
        public void run() {
            if(bleService.fireflyFound){
                Log.v(TAG, "Stop command");
                triggerFirefly(fireflyCommands.stopStim);
            }
        }
    };

    Runnable fireflyDebounce = new Runnable(){
        @Override
        public void run(){
            statusVariables.stimming = false;
        }
    };


    public void connectThigh(View v){
        if(bleService.hipGatt == null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSensorStatus("Searching");
                    hipUI.connect.setBackgroundResource(R.drawable.hipyellow);
                    if(bleService.kneeGatt ==  null){kneeUI.connect.setBackgroundResource(R.drawable.kneewhite);}
                    if(bleService.ankleGatt ==  null){ankleUI.connect.setBackgroundResource(R.drawable.anklewhite);}
                }
            });
            bleService.searchingHip = true;
            bleService.searchingKnee = false;
            bleService.searchingAnkle = false;
            bleService.scanner.startScan(bleService.mScanCallback);
        }
        else{
            hipUI.calibrateSensor(hipUI);
        }
    }
    public void connectLowerLeg(View v){
        if(bleService.kneeGatt ==  null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSensorStatus("Searching");
                    kneeUI.connect.setBackgroundResource(R.drawable.kneeyellow);
                    if(bleService.ankleGatt ==  null){ankleUI.connect.setBackgroundResource(R.drawable.anklewhite);}
                    if(bleService.hipGatt ==  null){hipUI.connect.setBackgroundResource(R.drawable.hipwhite);}
                }
            });
            bleService.searchingKnee = true;
            bleService.searchingHip = false;
            bleService.searchingAnkle = false;
            bleService.scanner.startScan(bleService.mScanCallback);
        }
        else{
            kneeUI.calibrateSensor(kneeUI);
        }
    }
    public void connectFoot(View v){
        if(bleService.ankleGatt == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSensorStatus("Searching");
                    ankleUI.connect.setBackgroundResource(R.drawable.ankleyellow);
                    if(bleService.hipGatt ==  null){hipUI.connect.setBackgroundResource(R.drawable.hipwhite);}
                    if(bleService.kneeGatt ==  null){kneeUI.connect.setBackgroundResource(R.drawable.kneewhite);}
                }
            });
            bleService.searchingHip = false;
            bleService.searchingKnee = false;
            bleService.searchingAnkle = true;
            bleService.scanner.startScan(bleService.mScanCallback);
        }
        else{

            ankleUI.calibrateSensor(ankleUI);
        }
    }

    private BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            String eventType = extras.getString("bleEvent");
            if(eventType.equals("sensorConnected")){
                if(extras.getString("gatt").equals("hip")){
                    connectSensor(hipUI);
                }
                if(extras.getString("gatt").equals("knee")){
                    connectSensor(kneeUI);
                }
                if(extras.getString("gatt").equals("ankle")){
                    connectSensor(ankleUI);
                }
                if(extras.getString("gatt").equals("firefly")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stimButton.setVisibility(View.VISIBLE);
                        }
                    });
                }
                Log.v("bleService", "connected message sent");
            }
            if(eventType.equals("sensorDisconnected")){
                if(extras.getString("gatt").equals("hip")){
                    onSensorDisconnected(hipUI);
                    bleService.hipGatt.close();
                    bleService.hipGatt = null;
                }
                if(extras.getString("gatt").equals("knee")){
                    onSensorDisconnected(kneeUI);
                    bleService.kneeGatt.close();
                    bleService.kneeGatt = null;
                }
                if(extras.getString("gatt").equals("ankle")){
                    onSensorDisconnected(ankleUI);
                    bleService.ankleGatt.close();
                    bleService.ankleGatt = null;

                }
            }
            if(eventType.equals("notification")){
                if(extras.getString("gatt").equals("hip")){
                    Float value = extras.getFloat("value");
                    findGaugeValue(hipUI,value);
                }
                if(extras.getString("gatt").equals("knee")){
                    Float value = extras.getFloat("value");
                    findGaugeValue(kneeUI,value);
                }
                if(extras.getString("gatt").equals("ankle")){
                    Float value = extras.getFloat("value");
                    findGaugeValue(ankleUI,value);
                }
            }
        }
    };
    private void connectSensor(final SensorUI sensor){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setSensorStatus("connected");
                sensor.connect.setBackgroundResource(sensor.green);
                sensor.rightTV.setVisibility(View.VISIBLE);
                sensor.leftTV.setVisibility(View.VISIBLE);
            }
        });
    }
    private void onSensorDisconnected(final SensorUI sensor){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sensor.leftPB.setProgress(0);
                sensor.rightPB.setProgress(0);
                sensor.rightTV.setVisibility(View.INVISIBLE);
                sensor.leftTV.setVisibility(View.INVISIBLE);
                sensor.connect.setBackgroundResource(sensor.white);
                sensor.relativeLayout.setBackgroundColor(Color.parseColor("#404040"));
            }
        });
        setSensorStatus("Disconnected");
        Log.v("BLUETOOTH", "DISCONNECTED");
    }

    public void startDetails(View v){
        Intent intent = new Intent(this, DetailsActivity.class);
        if (bleService.ankleGatt != null){
            String ankleDeviceAddress = bleService.ankleGatt.getDevice().getAddress().toString();
            intent.putExtra("ankleDeviceAddress", ankleDeviceAddress);
        }
        else{
            String string = null;
            intent.putExtra("ankleDeviceAddress", string);
        }
        if(bleService.kneeGatt != null){
            String kneeDeviceAddress = bleService.kneeGatt.getDevice().getAddress().toString();
            intent.putExtra("kneeDeviceAddress", kneeDeviceAddress);
        }
        else{
            String string = null;
            intent.putExtra("kneeDeviceAddress", string);
        }
        if(bleService.hipGatt != null){
            String hipDeviceAddress = bleService.hipGatt.getDevice().getAddress().toString();
            intent.putExtra("hipDeviceAddress", hipDeviceAddress);
        }
        else{
            String string = null;
            intent.putExtra("hipDeviceAddress", string);
        }
        startActivity(intent);
    }
    public void findGaugeValue(final SensorUI sensor, float gyroX){
        if(sensor.calibrate & sensor.calibrateCounter < 10){
            sensor.calibrateCounter++;
            sensor.average = sensor.average + gyroX;
        }
        else if (sensor.calibrate & sensor.calibrateCounter == 10){
            sensor.average = sensor.average/10;
            sensor.calibrateCounter++;
        }
        else if (sensor.calibrate & sensor.calibrateCounter > 10){
            if((sensor.average+90.0) < 180 & (sensor.average - 90) > -180){
                checkValue((int)(gyroX + (-1*sensor.average)), sensor);
                setGaugeValue((int)(gyroX + (-1*sensor.average)), sensor);
            }
            else if((sensor.average+90) > 180){
                if (gyroX < 0 ){
                    checkValue((int)((180 - sensor.average) + (gyroX + 180)),sensor);
                    setGaugeValue((int)((180 - sensor.average) + (gyroX + 180)),sensor);
                }
                else if(gyroX > 0){
                    checkValue((int)(gyroX + (-1*sensor.average)), sensor);
                    setGaugeValue((int)(gyroX + (-1*sensor.average)), sensor);
                }
            }
            else if((sensor.average-90) < -180){
                if(gyroX < 0 ){
                    checkValue((int)(gyroX + (-1*sensor.average)), sensor);
                    setGaugeValue((int)(gyroX + (-1*sensor.average)), sensor);
                }
                if(gyroX > 0){
                    checkValue((int)((-180 - sensor.average) + (gyroX - 180)), sensor);
                    setGaugeValue((int)((-180 - sensor.average) + (gyroX - 180)), sensor);
                }
            }
        }
    }


}