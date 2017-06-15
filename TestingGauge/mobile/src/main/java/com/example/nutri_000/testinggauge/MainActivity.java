package com.example.nutri_000.testinggauge;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListFragment;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.os.Vibrator;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import java.util.List;
import java.util.UUID;
import android.util.Log;


import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static android.view.View.VISIBLE;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = "Cole";
    protected UUID[] serviceUUIDs;
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean currentlyStimming = false;



    //TIMERS
    Handler timerHandler = new Handler();
    Handler connectionCheckHandler = new Handler();


    private CoordinatorLayout mContainerView;
    private Vibrator v;
    private BluetoothAdapter adapter;
    private static Context context;

    private BluetoothLeScanner scanner ;

    private int displayDataCounter = 0;

    //BLE connections for the firefly
    boolean connectedToFirefly = false;
    private BluetoothGatt fireflyGatt;
    private BluetoothGattCharacteristic FIREFLY_CHARACTERISTIC2;
    private BluetoothDevice firefly = null;
    private TextView fireflyStatus;
    private FloatingActionButton stimButton;
    private Button scanForPCM,ULConnect, LLConnect,footConnect;
    private String fireflyColor = "";


    //ble connections for the sensor
    boolean connectedToSensor = false;
    private BluetoothGatt sensorGatt, upperLegGatt, lowerLegGatt, footGatt;
    private BluetoothGattCharacteristic NRF_CHARACTERISTIC;
    private BluetoothDevice MPU9250 = null;
    private TextView sensorStatus;
    private int stimmingThreshold = 30;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private boolean isScanning = false;
    private boolean rescan = false;
    private ProgressBar topLeftPB, topRightPB, midLeftPB,midRightPB,bottomLeftPB,bottomRightPB;
    private SeekBar topLeftSB, topRightSB, midLeftSB, midRightSB, bottomLeftSB, bottomRightSB;
    private TextView topAngle;
    private TextView midAngle;
    private TextView bottomAngle;
    private TextView bottomLabel;
    private TextView topLabel;
    private TextView midLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivity.context = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        context = (Context) this;
        bottomLabel = (TextView) findViewById(R.id.bottomLabel);
        topLabel = (TextView)findViewById(R.id.topLabel);
        midLabel = (TextView)findViewById(R.id.midLabel);

        stimButton = (FloatingActionButton) findViewById(R.id.stim_buton);
        scanForPCM = (Button) findViewById(R.id.scanButton);
        ULConnect = (Button) findViewById(R.id.upperLegButton);
        LLConnect = (Button) findViewById(R.id.lowerLegButton);
        footConnect = (Button) findViewById(R.id.footButton);
        //fireflyStatus = (TextView) findViewById(R.id.FireflyStatus);
        sensorStatus = (TextView) findViewById(R.id.SensorStatus);
        topLeftPB = (ProgressBar)findViewById(R.id.progressBarTopLeft);
        topLeftPB.setRotation(180);
        topRightPB = (ProgressBar)findViewById(R.id.progressBarTopRight);
        midLeftPB = (ProgressBar)findViewById(R.id.progressBarMidLeft);
        midLeftPB.setRotation(180);
        midRightPB = (ProgressBar)findViewById(R.id.progressBarMidRight);
        bottomLeftPB = (ProgressBar)findViewById(R.id.progressBarBottomLeft);
        bottomLeftPB.setRotation(180);
        bottomRightPB = (ProgressBar)findViewById(R.id.progressBarBottomRight);
        topAngle = (TextView) findViewById(R.id.topAngle);
        midAngle = (TextView) findViewById(R.id.midAngle);
        bottomAngle = (TextView) findViewById(R.id.bottomAngle);
        topLeftSB = (SeekBar) findViewById(R.id.seekBarTopLeft);
        //topLeftSB.setRotation(180);
        topRightSB = (SeekBar)findViewById(R.id.seekBarTopRight);
        midLeftSB = (SeekBar)findViewById(R.id.seekBarMidLeft);
        //midLeftSB.setRotation(180);
        midRightSB = (SeekBar)findViewById(R.id.seekBarMidRight);
        bottomLeftSB = (SeekBar) findViewById(R.id.seekBarBottomLeft);
        //bottomLeftSB.setRotation(180);
        bottomRightSB = (SeekBar) findViewById(R.id.seekBarBottomRight);


        //bluetooth section
        serviceUUIDs = new UUID[1];
        serviceUUIDs[0] = UUID.fromString("0000AA80-0000-1000-8000-00805f9b34fb");
        mContainerView = (CoordinatorLayout) findViewById(R.id.container);
        mContainerView.setBackgroundColor(Color.parseColor("#333333"));
        adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();

        stimButton.bringToFront();
        if(!adapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.v("BLUETOOTH ENABLED", adapter.enable() + "");
        }
        if(adapter.isEnabled())
        {
            Log.v("BLUETOOTH ENABLED", "TRUE");
            setSensorStatus("Searching...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
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
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isScanning){
            scanner.stopScan(mScanCallback);
            isScanning = false;
        }
        if(sensorGatt != null) {
            sensorGatt.close();
        }
        if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }
        Log.v("onDestroy", "DESTROYED");
    }
    @Override
    protected void onStop() {

        super.onStop();
        if(isScanning){
            scanner.stopScan(mScanCallback);
            isScanning = false;
        }
        if(sensorGatt != null) {
            sensorGatt.close();
        }
        if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }
        //adapter.disable();
        Log.v("onStop", "STOPPED");
    }
    @Override
    protected void onPause(){
        super.onPause();
        if(isScanning){
            scanner.stopScan(mScanCallback);
            isScanning = false;
        }
        if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }
    }





    //stim button clicked
    public void stimClicked(View v)
    {
        if(currentlyStimming == false) {
            currentlyStimming = true;
            triggerFirefly(startStim);
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
                    if(!isScanning){
                        isScanning = true;
                        scanner.startScan(mScanCallback);
                        timerHandler.postDelayed(scanTimeout, 10000);
                    }
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
    public static byte [] startStim = {12, 1, 2, 3, 4, 60, 0, 0, 24, 83, 12, 13, (byte)0xc1};
    int mtu_flag = 0;
    int charfound = 0;
    int char4found = 0;
    public static byte [] stopStim = {2, 0, 0};
    boolean awaitingResponse = false;


    public ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            Log.v(TAG, "onScanResult");

            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            Log.i(TAG, "onBatchScanResults: " + results.size() + " results");
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
            Log.i(TAG, "Address " + device.getDevice().getAddress());

            String deviceName;
            deviceName = device.getDevice().getName();
            if (deviceName != null){
                if (deviceName.equals("JohnCougarMellenc"))
                {
                    if(device.getRssi() >= -42){
                        if(isScanning){
                            scanner.stopScan(mScanCallback);
                        }
                        peripheral = device.getDevice();
                        //sensorGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
                        if(!awaitingResponse) {
                            awaitingResponse = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setSensorStatus("Found...");
                                    mContainerView.setBackgroundColor(Color.parseColor("#0000FF"));
                                    timerHandler.postDelayed(connectedBackgroundColorReset, 500);
                                    if (upperLegGatt == null) {
                                        ULConnect.setVisibility(VISIBLE);
                                    }
                                    if (lowerLegGatt == null) {
                                        LLConnect.setVisibility(VISIBLE);
                                    }
                                    if (footGatt == null) {
                                        footConnect.setVisibility(VISIBLE);
                                    }
                                }
                            });
                        }
                        //Log.d(TAG, "found device");
                    }
                }
                /*if(deviceName.equals("FireflyPCM")){
                    firefly = device.getDevice();
                    if(rescan){
                        fireflyGatt = firefly.connectGatt(getAppContext(),false,btleGattCallback);
                    }
                }*/
            }
        }
    };


    boolean bgFlag = false;

    //GAUGE
    public void setGaugeValue(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                    //gauge.setFinishedStrokeColor(Color.parseColor("#00000000"));
                }
                if(value < 0) {
                    topLeftPB.setProgress(-1*value);
                    topRightPB.setProgress(0);

                }
                else if(value > 0){
                    topLeftPB.setProgress(0);
                    topRightPB.setProgress(value);
                }
                if (value > topRightSB.getProgress() | (value*-1) > topLeftSB.getProgress()){

                    mContainerView.setBackgroundColor(Color.parseColor("#0000ff"));
                    //timerHandler.postDelayed(connectedBackgroundColorReset,250);

                }
                if (value < topRightSB.getProgress() & (value*-1) < topLeftSB.getProgress()){

                    mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                    //timerHandler.postDelayed(connectedBackgroundColorReset,250);

                }
                topAngle.setText(Integer.toString(value));
            }
        });

    }
    public void setGaugeValueLL(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                    //gauge.setFinishedStrokeColor(Color.parseColor("#00000000"));
                }
                if(value < 0) {
                    midLeftPB.setProgress(-1*value);
                    midRightPB.setProgress(0);

                }
                else if(value > 0){
                    midLeftPB.setProgress(0);
                    midRightPB.setProgress(value);
                }
                if (value > midRightSB.getProgress() | (value*-1) > midLeftSB.getProgress()){

                    mContainerView.setBackgroundColor(Color.parseColor("#0000ff"));
                    //timerHandler.postDelayed(connectedBackgroundColorReset,250);

                }
                if (value < midRightSB.getProgress() & (value*-1) < midLeftSB.getProgress()){

                    mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                    //timerHandler.postDelayed(connectedBackgroundColorReset,250);

                }
                midAngle.setText(Integer.toString(value));
            }
        });
    }
    public void setGaugeValueFoot(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                    //gauge.setFinishedStrokeColor(Color.parseColor("#00000000"));
                }
                if(value < 0) {
                    bottomLeftPB.setProgress(-1*value);
                    bottomRightPB.setProgress(0);

                }
                else if(value > 0){
                    bottomLeftPB.setProgress(0);
                    bottomRightPB.setProgress(value);
                }
                if (value > bottomRightSB.getProgress() | (value*-1) > bottomLeftSB.getProgress()){

                    mContainerView.setBackgroundColor(Color.parseColor("#0000ff"));
                    //timerHandler.postDelayed(connectedBackgroundColorReset,250);

                }
                if (value < bottomRightSB.getProgress() & (value*-1) < bottomLeftSB.getProgress()){

                    mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                    //timerHandler.postDelayed(connectedBackgroundColorReset,250);

                }
                bottomAngle.setText(Integer.toString(value));
            }
        });
    }
    public void setGaugeProperties(boolean stimming)
    {
        if(stimming == true) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mContainerView.setBackgroundColor(Color.parseColor("#009900"));


                    //gauge.setFinishedStrokeColor(Color.parseColor("#ffffff"));
                    //gauge.setUnfinishedStrokeColor(Color.parseColor("#007700"));
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!bgFlag){
                        mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                       // gauge.setFinishedStrokeColor(Color.parseColor("#00ff00"));
                       // gauge.setUnfinishedStrokeColor(Color.parseColor("#222222"));
                    }
                }
            });
        }
    }


    float gravX, gravY, gravZ,linX,linY, linZ;

    //GATT CALLBACK
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(gatt == fireflyGatt){
                Log.v(TAG, "ff descriptor status "+ status);
            }
            if(gatt == sensorGatt){
                Log.v(TAG, "sensor descriptor status "+ status);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if(gatt == upperLegGatt | gatt == lowerLegGatt | gatt == footGatt) {
                displayDataCounter++;
                byte[] temp = characteristic.getValue();
                int MSB = temp[1] << 8;
                int LSB = temp[0] & 0x000000FF;
                int val = MSB | LSB;
                float gyroZ = val * 0.0625f;
                MSB = temp[3] << 8;
                LSB = temp[2] & 0x000000FF;
                val = MSB | LSB;
                float gyroY = val * 0.0625f;
                MSB = temp[5] << 8;
                LSB = temp[4] & 0x000000FF;
                val = MSB | LSB;
                float gyroX = val * 0.0625f;

                MSB = temp[7] << 8;
                LSB = temp[6] & 0x000000FF;
                val = MSB|LSB;
                 linX = -1.0f*val*0.001f;

                MSB = temp[9] <<8;
                LSB = temp[8] & 0x000000FF;
                val = MSB|LSB;
                 linY = -1.0f*val*0.001f;

                MSB = temp[11] << 8;
                LSB = temp[10] & 0x000000FF;
                val = MSB|LSB;
                 linZ = -1.0f*val*0.001f;

               /* MSB = temp[13] << 8;
                LSB = temp[12] & 0x000000FF;
                val = MSB|LSB;
                gravX = -1.0f*val*0.001f;
                MSB = temp[15] <<8;
                LSB = temp[14] & 0x000000FF;
                val = MSB|LSB;
                gravY = -1.0f*val*0.001f;
                MSB = temp[17] << 8;
                LSB = temp[16] & 0x000000FF;
                val = MSB|LSB;
                gravZ = -1.0f*val*0.001f;*/

                final String output = linX + "\t" + linY + "\t" + linZ;
                //final String output = (int)gyroX + "\t" + (int)gyroY + "\t" + (int)gyroZ + "\t" + gravX + "\t" +gravY + "\t" + gravZ;
                Log.v(gatt.toString() , output);
                int gaugeValue = 0;


                //LOWER LEG
                if(flag ==1){
                    if(gravY > gravZ){
                        gaugeValue = (int)(-1.0f*(gyroY + 90));
                    }
                    else if(gravY < gravZ){
                        gaugeValue = (int)((gyroY + 90));
                    }
                    //gaugeValue = (int)(gyroY +90);
                }
                if(flag == 3){
                    if(gravY > gravZ){
                        gaugeValue = (int)((90 - gyroY));
                    }
                    else if(gravY < gravZ){
                        gaugeValue = (int)(-1.0f*(90 - gyroY));
                    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                   }
                if(flag == 2){
                    if(gyroX > 90){
                        gaugeValue = (int)(-1.0f*(gyroX - 90));
                    }
                    if(gyroX <= 90){
                        gaugeValue = (int)(-1.0f*(gyroX - 90));
                    }
                }
                if(flag == 4){
                    if (gyroX > -90){
                        gaugeValue = (int)(-1.0f*(gyroX+90));
                    }
                    if(gyroX <= -90){
                        gaugeValue = (int)(-1.0f*(gyroX+90));
                    }
                }



                // Q1
                /*if((gyroX > 0.0f) & (gyroX < 90.0f))
                {
                    gaugeValue = (int)gyroX;
                }
                //Q2
                else if(gyroX >= 90.0f & gyroX < 180.0f)
                {
                    gaugeValue = (int)(gyroX - 90.0f);
                }
                //Q3 and Q4
                else if(gyroX >= -180.0f & gyroX <= 0.0f)
                {
                    gaugeValue = 0;
                }*/
                // ONE Quadrant
                if(((gyroX - 90.0f) > 0.0f) & gyroX < 190.0f) {
                    gaugeValue = (int) (gyroX - 90.0f);
                }
                if(((gyroX) > 0.0f) & gyroX < 90){
                    gaugeValue = (int) (gyroX-90.0f);
                }
                else if(gyroX >= 190.0f & gyroX < 270.0f)
                {
                    gaugeValue = 90;
                }
                else if(gyroX >= 270.0f || gyroX <= 90.0f)
                {
                    gaugeValue = 0;
                }
                if((gyroX > -90.0f) & (gyroX <= 0.0f)){
                    gaugeValue = -90;
                }
                if((gyroX > -180.0f) & (gyroX <= -90.0f)){
                    gaugeValue = 90;
                }

                //if(displayDataCounter == 1) {
                    //displayDataCounter = 0;
                    /*if (gaugeValue > stimmingThreshold & gaugeValue < 225) {
                        setGaugeProperties(true);
                        if(currentlyStimming == false) {
                            currentlyStimming = true;
                            Log.v(TAG, "Start command");
                            triggerFirefly(startStim);
                            timerHandler.postDelayed(timerRunnable, 1000);
                        }

                    } else {
                        setGaugeProperties(false);

                    }*/
                    if(gatt == upperLegGatt) {
                        setGaugeValue(gaugeValue);
                    }
                    if(gatt == lowerLegGatt){
                        setGaugeValueLL(gaugeValue);
                    }
                    if(gatt == footGatt){
                        setGaugeValueFoot(gaugeValue);
                    }

                //}
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if(newState == 0)
            {
                if(gatt == upperLegGatt) {




                    //connectedToSensor = false;
                    //rescan = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            topLeftPB.setProgress(0);
                            topRightPB.setProgress(0);
                            topAngle.setVisibility(View.INVISIBLE);
                            topLabel.setTextColor(Color.parseColor("#ffffff"));

                        }
                    });

                    /*if(!isScanning){
                        isScanning = true;
                        scanner.startScan(mScanCallback);
                        timerHandler.postDelayed(scanTimeout, 10000);
                    }*/
                    setSensorStatus("Disconnected");
                    upperLegGatt = null;
                    upperLegGatt.close();
                    Log.v("BLUETOOTH", "DISCONNECTED");
                }
                if(gatt == lowerLegGatt){

                    //connectedToSensor = false;
                    //rescan = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            midLeftPB.setProgress(0);
                            midRightPB.setProgress(0);
                            midAngle.setVisibility(View.INVISIBLE);
                            midLabel.setTextColor(Color.parseColor("#ffffff"));
                        }
                    });

                    /*if(!isScanning){
                        isScanning = true;
                        scanner.startScan(mScanCallback);
                        timerHandler.postDelayed(scanTimeout, 10000);
                    }*/

                    setSensorStatus("Disconnected");
                    lowerLegGatt = null;
                    lowerLegGatt.close();
                    Log.v("BLUETOOTH", "DISCONNECTED");
                }
                if(gatt == footGatt){


                    //connectedToSensor = false;
                    //rescan = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bottomLeftPB.setProgress(0);
                            bottomRightPB.setProgress(0);
                            bottomAngle.setVisibility(View.INVISIBLE);
                            bottomLabel.setTextColor(Color.parseColor("#ffffff"));
                        }
                    });

                    /*if(!isScanning){
                        isScanning = true;
                        scanner.startScan(mScanCallback);
                        timerHandler.postDelayed(scanTimeout, 10000);
                    }*/


                    setSensorStatus("Disconnected");
                    footGatt = null;
                    footGatt.close();
                    Log.v("BLUETOOTH", "DISCONNECTED");
                }
                if(gatt == fireflyGatt){
                    setFireflyStatus("Disconnected");
                    if(!isScanning){
                        isScanning = true;
                        scanner.startScan(mScanCallback);
                        timerHandler.postDelayed(scanTimeout, 10000);
                    }
                }
            }
            else if( newState == 1)
            {
                Log.v("BLUETOOTH", "CONNECTING");
            }
            else if( newState == 2)
            {
                Log.v("BLUETOOTH", "CONNECTED");
                if(gatt == upperLegGatt) {
                    connectedToSensor = true;
                    upperLegGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    bgFlag = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            topAngle.setVisibility(VISIBLE);
                            topLabel.setTextColor(Color.parseColor("#00ff00"));                        }


                    });
                    //timerHandler.postDelayed(connectedBackgroundColorReset,3000);
                }
                else if(gatt == lowerLegGatt) {
                    connectedToSensor = true;
                    lowerLegGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    bgFlag = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            midAngle.setVisibility(VISIBLE);
                            midLabel.setTextColor(Color.parseColor("#00ff00"));

                        }
                    });

                }
                else if(gatt == footGatt) {
                    connectedToSensor = true;
                    footGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    bgFlag = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bottomAngle.setVisibility(VISIBLE);
                            bottomLabel.setTextColor(Color.parseColor("#00ff00"));                        }


                    });

                }
                /*else if(gatt == fireflyGatt)
                {
                    fireflyGatt.discoverServices();
                    setFireflyStatus("Connecting...");
                    fireflyGatt.requestMtu(76);
                }*/
            }
            else if(newState == 3)
            {
                Log.v("BLUETOOTH", "DISCONNECTING");
            }
        }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if(gatt == fireflyGatt){
                if(mtu == 76){
                    Log.v(TAG, "mtu changed " + status);
                    mtu_flag = 1;
                }
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
                if(gatt == fireflyGatt){
                    if(characteristic == FIREFLY_CHARACTERISTIC2){
                        Log.v(TAG, "write status " + status);
                    }
                }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status) {
            Log.v(TAG, "charRead");
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
                //setFireflyStatus("Connecting...");
            }
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(int i = 0; i < characteristics.size(); i++)
                {
                    Log.v("CHARACTERISTIC", characteristics.get(i).getUuid().toString());

                    //check for the MPU9250_SENSOR
                    if(characteristics.get(i).getUuid().toString().equals("0000beef-1212-efde-1523-785fef13d123"))
                    {
                        setSensorStatus("Connected");
                        if(isScanning){
                            scanner.stopScan(mScanCallback);
                        }
                        connectedToSensor = true;
                        Log.v("NRF Sensor", "FOUND CHARACTERISTIC");
                        NRF_CHARACTERISTIC = service.getCharacteristic(UUID.fromString("0000beef-1212-efde-1523-785fef13d123"));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sensorStatus.setTextColor(Color.parseColor("#ffffff"));
                            }
                        });
                        gatt.setCharacteristicNotification(NRF_CHARACTERISTIC,true);
                        UUID dUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor notifyDescriptor = NRF_CHARACTERISTIC.getDescriptor(dUUID);
                        notifyDescriptor.setValue(ENABLE_NOTIFICATION_VALUE);
                        boolean b = gatt.writeDescriptor(notifyDescriptor);
                        Log.v("descriptor write ", String.valueOf(b) );

                        /*if(!connectedToFirefly) {
                            if(fireflyGatt != null){
                                if(isScanning){
                                    scanner.stopScan(mScanCallback);
                                    isScanning = false;
                                }
                                fireflyGatt = firefly.connectGatt(context, false, btleGattCallback);
                            }
                            if(fireflyGatt == null){
                                if(isScanning){
                                    scanner.stopScan(mScanCallback);
                                    isScanning = false;
                                }
                                rescan = true;
                                if(!isScanning) {
                                    scanner.startScan(mScanCallback);
                                    timerHandler.postDelayed(scanTimeout, 5000);
                                }
                            }
                        }*/
                    }
                    if(characteristics.get(i).getUuid().toString().equals("0000fff2-0000-1000-8000-00805f9b34fb"))
                    {
                        if(isScanning){
                            scanner.stopScan(mScanCallback);
                            isScanning = false;
                        }
                        setFireflyStatus("Connected");
                        Log.v("FIREFLY", "FOUND CHARACTERISTIC");
                        FIREFLY_CHARACTERISTIC2 = characteristics.get(i);
                        FIREFLY_CHARACTERISTIC2.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        connectedToFirefly = true;
                        charfound = 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fireflyStatus.setTextColor(Color.parseColor("#ffffff"));

                            }
                        });
                        if(!connectedToSensor) {

                        }
                    }
                }
            }
        }
    };

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

    //fIREfLY STATUS TEXT
    public void setFireflyStatus(String message)
    {
        final String msg = fireflyColor +" Firefly " + message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                fireflyStatus.setText(msg);
            }
        });
    }

    //START AND STOP COMMANDS 4 FIREFLY
    public void triggerFirefly(byte[] onOff)
    {
        if(charfound == 1 & char4found == 0) {

            FIREFLY_CHARACTERISTIC2.setValue(onOff);
            boolean b = fireflyGatt.writeCharacteristic(FIREFLY_CHARACTERISTIC2);
            Log.i(TAG, "firefly write status = " + b);
        }
    }
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "Stop command");
            currentlyStimming = false;
            triggerFirefly(stopStim);

        }
    };


    Runnable connectedBackgroundColorReset = new Runnable() {
        @Override
        public void run() {
            bgFlag = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                }
            });
        }
    };

    //
    Runnable checkConnectedStatus = new Runnable() {
        @Override
        public void run() {
            if(MPU9250 == null || firefly == null) {

            }
            Log.v(TAG, "check");

            connectionCheckHandler.postDelayed(checkConnectedStatus, 1000);
        }
    };
    Runnable scanTimeout = new Runnable() {
        @Override
        public void run() {
            if(isScanning){
                scanner.stopScan(mScanCallback);
                //setSensorStatus("Connected");
                isScanning = false;
            }
            if(!connectedToSensor){
                if(!isScanning){
                    isScanning = true;
                    scanner.startScan(mScanCallback);
                    timerHandler.postDelayed(scanTimeout, 10000);
                }
            }
            else if(connectedToSensor & !connectedToFirefly){
                scanner.stopScan(mScanCallback);
                isScanning = false;
                Log.v(TAG, "scan stopped for good");
                scanForPCM.setVisibility(VISIBLE);

            }
        }
    };
    public void scanAgain(View v){
        if(!isScanning){
            isScanning = true;
            scanner.startScan(mScanCallback);
            timerHandler.postDelayed(scanTimeout, 10000);
        }
        setSensorStatus("Searching");
    }
    int calibrationCounter = 0;
    int flag = 0;
    float averageX, averageY = 0;
    public void gravityOrientation(View v){
        //calibrationCounter++;
        averageX = 0;
        averageY = 0;
        flag = 0;
        timerHandler.postDelayed(calibrationRepeat, 20);
        calibrationCounter = 0;
        //check for gravity vector values
    }
    public void gravity(){
        calibrationCounter++;
        if(calibrationCounter < 20){
            averageX = averageX + gravX;
            averageY = averageY + gravY;
            timerHandler.postDelayed(calibrationRepeat, 20);
        }
        if(calibrationCounter == 20){
            averageX = averageX/20.0f;
            averageY = averageY/20.0f;
            if(averageX > .80f){
                flag = 1;
            }
            else if( averageY >.80f){
                flag = 2;
            }
            else if(averageX < -0.8f){
                flag = 3;
            }
            else if(averageY < -0.8f){
                flag = 4;
            }
            else{
                flag = 0;
            }
        }
    }
    public void connectThigh(View v){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ULConnect.setVisibility(View.INVISIBLE);
                LLConnect.setVisibility(View.INVISIBLE);
                footConnect.setVisibility(View.INVISIBLE);
                scanForPCM.setVisibility(View.VISIBLE);
            }
        });
        upperLegGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
        awaitingResponse = false;
    }
    public void connectLowerLeg(View v){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ULConnect.setVisibility(View.INVISIBLE);
                LLConnect.setVisibility(View.INVISIBLE);
                footConnect.setVisibility(View.INVISIBLE);
                scanForPCM.setVisibility(View.VISIBLE);
            }
        });
        lowerLegGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
        awaitingResponse = false;
    }
    public void connectFoot(View v){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ULConnect.setVisibility(View.INVISIBLE);
                LLConnect.setVisibility(View.INVISIBLE);
                footConnect.setVisibility(View.INVISIBLE);
                scanForPCM.setVisibility(View.VISIBLE);
            }
        });
        footGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
        awaitingResponse = false;
    }
    Runnable calibrationRepeat = new Runnable() {
        @Override
        public void run() {
            gravity();
        }
    };

}