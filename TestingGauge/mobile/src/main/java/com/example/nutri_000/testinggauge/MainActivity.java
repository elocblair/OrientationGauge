package com.example.nutri_000.testinggauge;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.support.design.widget.CoordinatorLayout;
import java.lang.CharSequence;
import android.view.WindowManager;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.os.Vibrator;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import java.util.List;
import java.util.UUID;
import android.util.Log;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.triggertrap.seekarc.SeekArc;
import com.triggertrap.seekarc.SeekArc.OnSeekArcChangeListener;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    protected static final UUID SIMPLE_BLE_PERIPHERAL_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    protected static final UUID FIREFLY_CHARACTERISTIC_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    protected UUID[] serviceUUIDs;
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean currentlyStimming = false;

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
    new SimpleDateFormat("HH:mm", Locale.US);
    private ArcProgress gauge;
    private SeekArc seekArc;
    Handler timerHandler = new Handler();
    Handler connectionCheckHandler = new Handler();
    long startTime = 0;

    private CoordinatorLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private Vibrator v;
    private BluetoothAdapter adapter;
    private static Context context;
    private BluetoothLeScanner scanner;
    private BluetoothGatt mGatt;
    private BluetoothDevice dev;
    private BluetoothGattCharacteristic CHAR;
    private BluetoothGattCharacteristic char4;
    private int displayDataCounter = 0;
    //BLE connections for the firefly
    boolean connectedToFirefly = false;
    private BluetoothGatt fireflyGatt;
    private BluetoothGattCharacteristic FIREFLY_CHARACTERISTIC;
    private BluetoothDevice firefly = null;
    private TextView fireflyStatus;
    private FloatingActionButton stimButton;
    private String fireflyColor = "";

    //ble connections for the sensor
    boolean connectedToSensor = false;
    private BluetoothGatt sensorGatt;
    private BluetoothGattCharacteristic MPU9520_CHARACTERISTIC;
    private BluetoothDevice MPU9250 = null;
    private TextView sensorStatus;
    private int stimmingThreshold = 30;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    float[] q = new float[]{1.0f, 0.0f, 0.0f, 0.0f};
    float GyroMeansError = (float)Math.PI * (40.f / 180.f);
    float beta = (float)Math.sqrt(3.0f/ 4.0f) * GyroMeansError * 2.0f;
    //float beta = .0f;
    //float beta = 0.1f;
    float deltat = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivity.context = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        stimButton = (FloatingActionButton) findViewById(R.id.stim_buton);
        fireflyStatus = (TextView) findViewById(R.id.FireflyStatus);
        sensorStatus = (TextView) findViewById(R.id.SensorStatus);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = (Context) this;
        gauge = (ArcProgress)findViewById(R.id.arc_progress);
        seekArc = (SeekArc)findViewById(R.id.seekArc);
        seekArc.setSweepAngle(290);
        seekArc.setArcRotation(215);
        seekArc.setProgress(30);
        seekArc.setArcColor(0x00000000);
        seekArc.setProgressColor(0x00000000);

        serviceUUIDs = new UUID[1];
        serviceUUIDs[0] = UUID.fromString("0000AA80-0000-1000-8000-00805f9b34fb");
        //serviceUUIDs[0] = SIMPLE_BLE_PERIPHERAL_UUID;
        mContainerView = (CoordinatorLayout) findViewById(R.id.container);
        mContainerView.setBackgroundColor(Color.parseColor("#333333"));
        //bluetooth section
        adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();
        seekArc.setOnSeekArcChangeListener(new OnSeekArcChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {
            }

            @Override
            public void onProgressChanged(SeekArc seekArc, int progress,
                                          boolean fromUser) {
                stimmingThreshold = progress;
                Log.v("progress", progress + "");
                //stimmingThreshold =
            }

        });
        stimButton.bringToFront();


        if(!adapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.v("BLUETOOTH ENABLED", adapter.enable() + "");
            //mTextView.setText("Searching For Sensor");
        }

        if(adapter.isEnabled())
        {
            Log.v("BLUETOOTH ENABLED", "TRUE");
            //firefly = adapter.getRemoteDevice("B0:B4:48:C3:EC:82");
            //fireflyGatt = firefly.connectGatt((Context) this, false, btleGattCallback);
            //setFireflyStatus("Searching...");
            setSensorStatus("Searching...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

            //adapter.startLeScan(leScanCallback);
        } else {
            Log.v("BLUETOOTH ENABLED", "FALSE");
        }

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(v.hasVibrator())
        {
            Log.v("CAN VIBRATE", "YES");
        } else
        {
            Log.v("CAN VIBRATE", "NO");
        }
        //mClockView = (TextView) findViewById(R.id.clock);
    }
    @Override
    protected void onDestroy() {
        //adapter.stopLeScan(leScanCallback);
        if(sensorGatt != null) {
            sensorGatt.close();
        }
        if(fireflyGatt != null) {
            fireflyGatt.close();
        }
        //adapter.disable();
        Log.v("onDestroy", "DESTROYED");
        super.onDestroy();
    }
    @Override
    protected void onStop() {
        //adapter.stopLeScan(leScanCallback);
        if(sensorGatt != null) {
            sensorGatt.close();
        }
        if(fireflyGatt != null) {
            fireflyGatt.close();
        }
        //adapter.disable();
        Log.v("onStop", "STOPPED");

        super.onStop();
    }
    //stim button clicked
    public void stimClicked(View v)
    {
        if(currentlyStimming == false) {
            currentlyStimming = true;
            triggerFirefly((int) 1);
            timerHandler.postDelayed(timerRunnable, 1000);

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
                    scanner.startScan(mScanCallback);
                    //Connect without scanning
                    //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("B0:B4:48:C3:EE:01");
                    //mBluetoothGatt = device.connectGatt(this,false,mGattCallback);

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
    //le scan callback
    private BluetoothDevice peripheral;
    public static Context getAppContext(){
        return MainActivity.context;
    }

    public ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            Log.d(TAG, "onScanResult");

            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            Log.d(TAG, "onBatchScanResults: " + results.size() + " results");
            for (ScanResult result : results)
            {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            Log.d(TAG, "LE Scan Failed: " + errorCode);
        }

        private void processResult(ScanResult device)
        {
            Log.i(TAG, "New LE Device: " + device.getDevice().getName() + " @ " + device.getRssi());
            Log.d(TAG, "Address " + device.getDevice().getAddress());

            String deviceName;
            deviceName = device.getDevice().getName();
            if (deviceName != null){
                if (deviceName.equals("JohnCougarMellenc"))
                {
                    Log.d(TAG, "found device");
                    scanner.stopScan(mScanCallback);
                    peripheral = device.getDevice();
                    sensorGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
                }
                if(deviceName.equals("FireflyPCM")){
                    firefly = device.getDevice();
                }
            }

        }
    };

    //connect to device
    public void connectToDevice(BluetoothDevice device)
    {
        if(MPU9250 != null & firefly != null) {
            //adapter.stopLeScan(leScanCallback);
        }
        if(device == MPU9250)
        {
            sensorGatt = MPU9250.connectGatt((Context)this, false, btleGattCallback);
            //mTextView.setText("Discovering Sensor Services");
        } else if(device == firefly)
        {
            fireflyGatt = firefly.connectGatt((Context)this, false, btleGattCallback);
            //mTextView.setText("Discovering Firefly Services");

        }
    }
    public void setGaugeValue(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                    gauge.setFinishedStrokeColor(Color.parseColor("#00000000"));
                }
                gauge.setProgress(value);

            }
        });
    }
    public void setGaugeProperties(boolean stimming)
    {
        if(stimming == true) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContainerView.setBackgroundColor(Color.parseColor("#009900"));
                    gauge.setFinishedStrokeColor(Color.parseColor("#ffffff"));
                    gauge.setUnfinishedStrokeColor(Color.parseColor("#007700"));

                    //custom:arc_finished_color="#009900"
                    //custom:arc_unfinished_color="#ffffff"
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                    gauge.setFinishedStrokeColor(Color.parseColor("#00ff00"));
                    gauge.setUnfinishedStrokeColor(Color.parseColor("#222222"));
                }
            });
        }
    }
    //btlegattcallback
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation

            byte[] values = characteristic.getValue();
            for(int i = 0; i < values.length; i++)
            {
                if(values[i] > 122 | values[i] < 32)
                {
                    values[i] = ' ';
                }
            }
            String decoded = characteristic.getStringValue(0);
            decoded = decoded.trim();

            Log.v("CHARACTERISTIC VALUE", decoded);


        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            if(newState == 0)
            {
                setFireflyStatus("Disconnected");

                Log.v("BLUETOOTH", "DISCONNECTED");
                //firefly = adapter.getRemoteDevice("24:71:89:19:F0:84");
                //fireflyGatt = firefly.connectGatt(context, false, btleGattCallback);

            } else if( newState == 1)
            {
                Log.v("BLUETOOTH", "CONNECTING");
            }
            else if( newState == 2)
            {
                Log.v("BLUETOOTH", "CONNECTED");
                if(gatt == sensorGatt) {
                    sensorGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    //mTextView.setText("Connected To Sensor, Discovering Services");
                    fireflyGatt = firefly.connectGatt(context, false, btleGattCallback);

                } else if(gatt == fireflyGatt)
                {
                    fireflyGatt.discoverServices();
                    setFireflyStatus("Connecting...");
                    //mTextView.setText("Connected To Firefly, Discovering Services");
                }
            }
            else if(newState == 3)
            {
                Log.v("BLUETOOTH", "DISCONNECTING");
            }

        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status) {
            if(gatt == sensorGatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    displayDataCounter++;
                    byte[] temp = characteristic.getValue();
                    //updateMadgwick(temp);
                    int gyrox = (temp[1]<<8) + temp[0];
                    int gyroy = (temp[3]<<8) + temp[2];
                    int gyroz = (temp[5]<<8) + temp[4];

                    double roll, pitch, yaw;
                    roll = (double) gyrox * 0.0625;
                    pitch = (double) gyroy * 0.0625;
                    yaw = (double) gyroz * 0.0625;
                    //Log.v("roll" + roll,  "\n");
                    Log.v("yaw" + yaw, "\n");
                    /*roll = (roll + 360) % 360;
                    pitch = (pitch + 360) % 360;
                    yaw = (yaw + 360) % 360;*/
//                    final String output = (int)roll + "\n" + (int)pitch + "\n" + (int)yaw;
                    //final String output = q[0] + "\n" + q[1] + "\n" + q[2] + "\n" + q[3] + "\n" + (int)roll + "\n" + (int)pitch + "\n" + (int)yaw;
                    int gaugeValue = 0;
                    //Log.v("ROLL", roll+"");
                    /*if(-1*(int)(90.0f-roll) > 0 )
                    {
                        gaugeValue = -1*(int)(90.0f-roll);
                    }
                    if(-1*(int)(90.0f-roll) > 100)
                    {
                        gaugeValue = 100;
                    }*/
                    if(((roll - 90.0f) > 0.0f) & roll < 190.0f)
                    {
                        gaugeValue = (int)(roll - 90.0f);
                        Log.v("VALUE", (roll - 90.0f) + "");
                    }
                    else if(roll >= 190.0f & roll < 270.0f)
                    {
                        gaugeValue = 100;
                        Log.v("VALUE", "100");
                    }
                    else if(roll >= 270.0f || roll <= 90.0f)
                    {
                        gaugeValue = 0;
                        Log.v("VALUE", "0");
                    }
//                    if(gaugeValue == 0)
//                    {
//                        gaugeValue++;
//                    }


                    if(displayDataCounter == 1) {
                        displayDataCounter = 0;
                        if (gaugeValue > stimmingThreshold & gaugeValue < 225) {
                            setGaugeProperties(true);
                            if(currentlyStimming == false) {
                                currentlyStimming = true;
                                triggerFirefly((int) 1);
                                timerHandler.postDelayed(timerRunnable, 1000);
                            }

                        } else {
                            setGaugeProperties(false);
                            //triggerFirefly((int)0);
                        }
                        setGaugeValue(gaugeValue);
                    }
                    //setEulerOutput(output);
                    sensorGatt.readCharacteristic(MPU9520_CHARACTERISTIC);
                    //Log.v("REREAD CHAR", "CHAR");
                } else {
                    Log.i("log", String.valueOf(status));
                }
            }

        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("HOORAY", "HOORAY");
            }

            List<BluetoothGattService> services = gatt.getServices();
            if(gatt == sensorGatt)
            {
                setSensorStatus("Connecting...");
            } else
            {
                setFireflyStatus("Connecting...");
            }
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(int i = 0; i < characteristics.size(); i++)
                {
                    Log.v("CHARACTERISTIC", characteristics.get(i).getUuid().toString());
                    //mTextView.setText("Discovering Chars");

                    //check for the MPU9250_SENSOR
                    if(characteristics.get(i).getUuid().toString().equals("0000beef-1212-efde-1523-785fef13d123"))
                    {

                        //sensorGatt = mGatt;
                        setSensorStatus("Connected");
                        Log.v("NRF Sensor", "FOUND CHARACTERISTIC");
                        MPU9520_CHARACTERISTIC = service.getCharacteristic(UUID.fromString("0000beef-1212-efde-1523-785fef13d123"));
                        boolean readStatus = sensorGatt.readCharacteristic(MPU9520_CHARACTERISTIC);
                        Log.v("READ STATUS", "" + readStatus);
                        sensorGatt.readCharacteristic(MPU9520_CHARACTERISTIC);
                        connectedToSensor = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sensorStatus.setTextColor(Color.parseColor("#ffffff"));

                                //custom:arc_finished_color="#009900"
                                //custom:arc_unfinished_color="#ffffff"
                            }
                        });
                        if(!connectedToFirefly) {
                            //serviceUUIDs[0] = SIMPLE_BLE_PERIPHERAL_UUID;
                            //adapter.startLeScan(leScanCallback);
                            //mTextView.setText("Finding Firefly");
                            /*firefly = adapter.getRemoteDevice("B0:B4:48:C3:EC:82");
                            fireflyGatt = firefly.connectGatt(context, false, btleGattCallback);
                            setFireflyStatus("Searching...");*/
                        }
                    }
                    if(characteristics.get(i).getUuid().toString().equals("0000fff1-0000-1000-8000-00805f9b34fb"))
                    {
                        setFireflyStatus("Connected");
                        //fireflyGatt = mGatt;
                        Log.v("FIREFLY", "FOUND CHARACTERISTIC");
                        FIREFLY_CHARACTERISTIC = service.getCharacteristic(FIREFLY_CHARACTERISTIC_UUID);
                        //boolean readStatus = sensorGatt.readCharacteristic(FIREFLY_CHARACTERISTIC);
                        //Log.v("READ STATUS", ""+readStatus);
                        connectedToFirefly = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fireflyStatus.setTextColor(Color.parseColor("#ffffff"));


                                //custom:arc_finished_color="#009900"
                                //custom:arc_unfinished_color="#ffffff"
                            }
                        });                        if(!connectedToSensor) {
                            //serviceUUIDs[0] = UUID.fromString("0000AA80-0000-1000-8000-00805f9b34fb");
                            //adapter.startLeScan(serviceUUIDs, leScanCallback);
                            //setSensorStatus("Searching...");
                            //mTextView.setText("Finding Sensor");

                        }
                        //triggerFirefly();
                        //fireflyGatt.readCharacteristic(FIREFLY_CHARACTERISTIC);
                    }
                }
            }
        }

    };
    public void setSensorStatus(String message)
    {
        final String msg = "Sensor " + message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                sensorStatus.setText(msg);


                //custom:arc_finished_color="#009900"
                //custom:arc_unfinished_color="#ffffff"
            }
        });
    }
    public void setFireflyStatus(String message)
    {
        final String msg = fireflyColor +" Firefly " + message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                fireflyStatus.setText(msg);


                //custom:arc_finished_color="#009900"
                //custom:arc_unfinished_color="#ffffff"
            }
        });
    }
    public void triggerFirefly(int onOff)
    {
        if(connectedToFirefly) {
            FIREFLY_CHARACTERISTIC.setValue(onOff, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            fireflyGatt.writeCharacteristic(FIREFLY_CHARACTERISTIC);
            currentlyStimming = false;
        }
    }

    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            triggerFirefly((int)0);
        }
    };
    Runnable checkConnectedStatus = new Runnable() {
        @Override
        public void run() {
            if(MPU9250 == null || firefly == null) {
                //connect to the device
                //adapter.startLeScan(leScanCallback);

            }
            //check if the mpu9250 device isn't null
            //check if the firefly isn't null
            connectionCheckHandler.postDelayed(checkConnectedStatus, 1000);
        }
    };
    //timerHandler.postDelayed(timerRunnable, 1000);

}

