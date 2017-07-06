package com.example.nutri_000.testinggauge;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.support.design.widget.CoordinatorLayout;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
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

    private final static int REQUEST_ENABLE_BT = 1;

    Handler timerHandler = new Handler();
    Status statusVariables = new Status();
    FireflyCommands fireflyCommands = new FireflyCommands();
    SensorUI hipUI;
    SensorUI kneeUI;
    SensorUI ankleUI;

    private BluetoothAdapter adapter;
    private static Context context;

    private BluetoothLeScanner scanner ;

    //BLE connections for the firefly
    private BluetoothGatt fireflyGatt;
    private BluetoothGattCharacteristic FIREFLY_CHARACTERISTIC2;
    private BluetoothDevice firefly = null;
    private FloatingActionButton stimButton;


    //ble connections for the sensor

    private BluetoothGatt upperLegGatt, lowerLegGatt, footGatt;
    private BluetoothGattCharacteristic NRF_CHARACTERISTIC;
    private TextView sensorStatus;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    boolean searchHip, searchKnee, searchAnkle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // all UI components for main activity
        MainActivity.context = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        context = this;

        hipUI = new SensorUI(R.id.upperLegButton, R.id.progressBarTopRight, R.id.progressBarTopLeft, R.id.seekBarTopRight, R.id.seekBarTopLeft,
                R.id.topAngle, R.id.topAngleL,R.id.relativeHip, this );
        hipUI.leftPB.setRotation(180);

        kneeUI = new SensorUI(R.id.lowerLegButton, R.id.progressBarMidRight, R.id.progressBarMidLeft, R.id.seekBarMidRight,R.id.seekBarMidLeft,
                R.id.midAngle, R.id.midAngleL, R.id.relativeKnee, this);
        kneeUI.leftPB.setRotation(180);
        ankleUI = new SensorUI(R.id.footButton,R.id.progressBarBottomRight,R.id.progressBarBottomLeft,R.id.seekBarBottomRight,R.id.seekBarBottomLeft,
                R.id.bottomAngle,R.id.bottomAngleL, R.id.relativeAnkle, this);
        ankleUI.leftPB.setRotation(180);

        stimButton = (FloatingActionButton) findViewById(R.id.stim_buton);
        sensorStatus = (TextView) findViewById(R.id.SensorStatus);
        stimButton.bringToFront();

        adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();

        if(!adapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.v("BLUETOOTH ENABLED", adapter.enable() + "");
        }
        if(adapter.isEnabled())
        {
            Log.v("BLUETOOTH ENABLED", "TRUE");
            //setSensorStatus("Searching...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        } else {
            Log.v("BLUETOOTH ENABLED", "FALSE");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(statusVariables.scanning){
            scanner.stopScan(mScanCallback);
            statusVariables.scanning = false;
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
        if(statusVariables.scanning){
            scanner.stopScan(mScanCallback);
            statusVariables.scanning = false;
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
        if(statusVariables.scanning){
            scanner.stopScan(mScanCallback);
            statusVariables.scanning = false;
        }
        if(fireflyGatt != null) {
            fireflyGatt.disconnect();
            fireflyGatt.close();
        }
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
                    statusVariables.scanning = true;
                    scanner.startScan(mScanCallback);
                    timerHandler.postDelayed(scanTimeout, 2000);
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

    //what
    //private BluetoothDevice peripheral;
    //hwat
    public static Context getAppContext(){
        return MainActivity.context;
    }
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
            Log.i(TAG, "New LE Device: " + device.getDevice().getName() + " @ " + device.getRssi() + " Address " + device.getDevice().getAddress());
            String deviceName;
            deviceName = device.getDevice().getName();
            if (deviceName != null){
                if (deviceName.equals("JohnCougarMellenc"))
                {
                    if(device.getRssi() >= -70){
                        final BluetoothDevice peripheral;
                        peripheral = device.getDevice();
                        if(!awaitingResponse) {
                            awaitingResponse = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(searchHip){
                                        upperLegGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
                                        hipUI.connect.setBackgroundResource(R.drawable.hipgreen);
                                    }
                                    else if(searchKnee){
                                        lowerLegGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
                                        kneeUI.connect.setBackgroundResource(R.drawable.kneegreen);
                                    }
                                    else if(searchAnkle){
                                        footGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
                                        ankleUI.connect.setBackgroundResource(R.drawable.anklegreen);
                                    }
                                }
                            });
                        }
                    }
                }
                if(deviceName.equals("FireflyPCM")){
                    firefly = device.getDevice();
                    if(device.getRssi() >= -55) {
                        fireflyGatt = firefly.connectGatt(getAppContext(), false, btleGattCallback);
                    }
                }
            }
        }
    };


    boolean bgFlag = false;

    //GAUGE
    public void setGaugeValue(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(value < 0) {
                    hipUI.leftPB.setProgress(-1*value);
                    hipUI.rightPB.setProgress(0);

                }
                else if(value > 0){
                    hipUI.leftPB.setProgress(0);
                    hipUI.rightPB.setProgress(value);
                }
                if (value > hipUI.rightSB.getProgress() | (value*-1) > hipUI.leftSB.getProgress()){
                    if(!statusVariables.stimming) {
                        statusVariables.stimming = true;
                        Log.v(TAG, "Start command");
                        triggerFirefly(fireflyCommands.startStim);
                        timerHandler.postDelayed(fireflyStop, 1000);
                        timerHandler.postDelayed(fireflyDebounce,5000);

                    }
                    hipUI.relativeLayout.setBackgroundColor(Color.parseColor("#008542"));

                }

                if (value < hipUI.rightSB.getProgress() & (value*-1) < hipUI.leftSB.getProgress()){

                    hipUI.relativeLayout.setBackgroundColor(Color.parseColor("#404040"));

                }
                if(value >= 0 ){
                    hipUI.rightTV.setText(Integer.toString(value));
                    hipUI.leftTV.setText("0");
                }
                if(value <= 0) {
                    hipUI.leftTV.setText(Integer.toString(-1*value));
                    hipUI.rightTV.setText("0");
                }
            }
        });

    }
    public void setGaugeValueLL(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                }
                if(value < 0) {
                    kneeUI.leftPB.setProgress(-1*value);
                    kneeUI.rightPB.setProgress(0);

                }
                else if(value > 0){
                    kneeUI.leftPB.setProgress(0);
                    kneeUI.rightPB.setProgress(value);
                }
                if (value > kneeUI.rightSB.getProgress() | (value*-1) > kneeUI.leftSB.getProgress()){
                    if(!statusVariables.stimming) {
                        statusVariables.stimming = true;
                        Log.v(TAG, "Start command");
                        triggerFirefly(fireflyCommands.startStim);
                        timerHandler.postDelayed(fireflyStop, 1000);
                        timerHandler.postDelayed(fireflyDebounce,5000);

                    }
                    kneeUI.relativeLayout.setBackgroundColor(Color.parseColor("#008542"));

                }
                if (value < kneeUI.rightSB.getProgress() & (value*-1) < kneeUI.leftSB.getProgress()){
                    kneeUI.relativeLayout.setBackgroundColor(Color.parseColor("#333333"));

                }
                if(value >= 0 ){
                    kneeUI.rightTV.setText(Integer.toString(value));
                    kneeUI.leftTV.setText("0");
                }
                if(value <= 0) {
                    kneeUI.leftTV.setText(Integer.toString(-1*value));
                    kneeUI.rightTV.setText("0");
                }
            }
        });
    }
    public void setGaugeValueFoot(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                }
                if(value < 0) {
                    ankleUI.leftPB.setProgress(-1*value);
                    ankleUI.rightPB.setProgress(0);

                }
                else if(value > 0){
                    ankleUI.leftPB.setProgress(0);
                    ankleUI.rightPB.setProgress(value);
                }
                if (value > ankleUI.rightSB.getProgress() | (value*-1) > ankleUI.leftSB.getProgress()){
                    if(!statusVariables.stimming) {
                        statusVariables.stimming = true;
                        Log.v(TAG, "Start command");
                        triggerFirefly(fireflyCommands.startStim);
                        timerHandler.postDelayed(fireflyStop, 1000);
                        timerHandler.postDelayed(fireflyDebounce,5000);
                    }
                    ankleUI.relativeLayout.setBackgroundColor(Color.parseColor("#008542"));
                }
                if (value < ankleUI.rightSB.getProgress() & (value*-1) < ankleUI.leftSB.getProgress()){
                    ankleUI.relativeLayout.setBackgroundColor(Color.parseColor("#404040"));

                }
                if(value >= 0 ){
                    ankleUI.rightTV.setText(Integer.toString(value));
                    ankleUI.leftTV.setText("0");
                }
                if(value <= 0) {
                    ankleUI.leftTV.setText(Integer.toString(-1*value));
                    ankleUI.rightTV.setText("0");
                }
            }
        });
    }


    float gravX, gravY, gravZ,linX,linY, linZ;
    float averageHip, averageKnee, averageAnkle = 0;
    int hipCalibrateCounter, kneeCalibrateCounter, ankleCalibrateCounter = 0;

    //GATT CALLBACK
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(gatt == fireflyGatt){
                Log.v(TAG, "ff descriptor status "+ status);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if(gatt == upperLegGatt | gatt == lowerLegGatt | gatt == footGatt) {
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

                final String output = gyroX + "\n";
                Log.v(gatt.toString() , output);

                if(gatt == upperLegGatt) {
                    if(hipCalibrate & hipCalibrateCounter < 10){
                        hipCalibrateCounter++;
                        averageHip = averageHip + gyroX;
                    }
                    else if (hipCalibrate & hipCalibrateCounter == 10){
                        averageHip = averageHip/10;
                        hipCalibrateCounter++;
                    }
                    else if (hipCalibrate & hipCalibrateCounter > 10){
                        Log.v(TAG, "average found " + (int)averageHip + "\n");
                        if((averageHip+90.0) < 180 & (averageHip - 90) > -180){
                            setGaugeValue((int)(gyroX + (-1*averageHip)));
                        }
                        else if((averageHip + 90) > 180){
                            if (gyroX < 0 ){
                                setGaugeValue((int)((180 - averageHip) + (gyroX + 180)));
                            }
                            else if(gyroX > 0){
                                setGaugeValue((int)(gyroX + (-1*averageHip)));
                            }
                        }
                        else if((averageHip - 90) < -180){
                            if(gyroX < 0 ){
                                setGaugeValue((int)(gyroX + (-1*averageHip)));
                            }
                            if(gyroX > 0){
                                setGaugeValue((int)((-180 - averageHip) + (gyroX - 180)));
                            }
                        }
                    }

                }
                if(gatt == lowerLegGatt){
                    if(kneeCalibrate & kneeCalibrateCounter < 10){
                        kneeCalibrateCounter++;
                        averageKnee = averageKnee + gyroX;
                    }
                    else if (kneeCalibrate & kneeCalibrateCounter == 10){
                        averageKnee = averageKnee/10;
                        kneeCalibrateCounter++;
                    }
                    else if (kneeCalibrate & kneeCalibrateCounter > 10){
                        Log.v(TAG, "average found " + (int)averageKnee + "\n");
                        if((averageKnee+90.0) < 180 & (averageKnee - 90) > -180){
                            setGaugeValueLL((int)(gyroX + (-1*averageKnee)));
                        }
                        else if((averageKnee+90) > 180){
                            if (gyroX < 0 ){
                                setGaugeValueLL((int)((180 - averageKnee) + (gyroX + 180)));
                            }
                            else if(gyroX > 0){
                                setGaugeValueLL((int)(gyroX + (-1*averageKnee)));
                            }
                        }
                        else if((averageKnee-90) < -180){
                            if(gyroX < 0 ){
                                setGaugeValueLL((int)(gyroX + (-1*averageKnee)));
                            }
                            if(gyroX > 0){
                                setGaugeValueLL((int)((-180 - averageKnee) + (gyroX - 180)));
                            }
                        }
                    }
                }
                if(gatt == footGatt){
                    if(ankleCalibrate & ankleCalibrateCounter < 10){
                        ankleCalibrateCounter++;
                        averageAnkle = averageAnkle + gyroX;
                    }
                    else if (ankleCalibrate & ankleCalibrateCounter == 10){
                        averageAnkle = averageAnkle/10;
                        ankleCalibrateCounter++;
                    }
                    else if (ankleCalibrate & ankleCalibrateCounter > 10){
                        Log.v(TAG, "average found " + (int)averageAnkle + "\n");
                        if((averageAnkle+90.0) < 180 & (averageAnkle - 90) > -180){
                            setGaugeValueFoot((int)(gyroX + (-1*averageAnkle)));
                        }
                        else if((averageAnkle+90) > 180) {
                            if (gyroX < 0) {
                                setGaugeValueFoot((int) ((180 - averageAnkle) + (gyroX + 180)));
                            } else if (gyroX > 0) {
                                setGaugeValueFoot((int) (gyroX + (-1 * averageAnkle)));
                            }
                        }
                        else if((averageAnkle-90) < -180){
                            if(gyroX < 0 ){
                                setGaugeValueFoot((int)(gyroX + (-1*averageAnkle)));
                            }
                            if(gyroX > 0){
                                setGaugeValueFoot((int)((-180 - averageAnkle) + (gyroX - 180)));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if(newState == 0)
            {
                if(gatt == upperLegGatt) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hipUI.leftPB.setProgress(0);
                            hipUI.rightPB.setProgress(0);
                            hipUI.rightTV.setVisibility(View.INVISIBLE);
                            hipUI.leftTV.setVisibility(View.INVISIBLE);
                            hipUI.connect.setBackgroundResource(R.drawable.hipwhite);
                            hipUI.relativeLayout.setBackgroundColor(Color.parseColor("#404040"));
                        }
                    });
                    setSensorStatus("Disconnected");
                    upperLegGatt.close();
                    upperLegGatt = null;
                    Log.v("BLUETOOTH", "DISCONNECTED");
                }
                if(gatt == lowerLegGatt){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            kneeUI.leftPB.setProgress(0);
                            kneeUI.rightPB.setProgress(0);
                            kneeUI.rightTV.setVisibility(View.INVISIBLE);
                            kneeUI.leftTV.setVisibility(View.INVISIBLE);
                            kneeUI.connect.setBackgroundResource(R.drawable.kneewhite);
                            kneeUI.relativeLayout.setBackgroundColor(Color.parseColor("#333333"));

                        }
                    });
                    setSensorStatus("Disconnected");
                    //
                    lowerLegGatt.close();
                    lowerLegGatt = null;
                    Log.v("BLUETOOTH", "DISCONNECTED");
                }
                if(gatt == footGatt){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ankleUI.leftPB.setProgress(0);
                            ankleUI.rightPB.setProgress(0);
                            ankleUI.rightTV.setVisibility(View.INVISIBLE);
                            ankleUI.leftTV.setVisibility(View.INVISIBLE);
                            ankleUI.connect.setBackgroundResource(R.drawable.anklewhite);
                            ankleUI.relativeLayout.setBackgroundColor(Color.parseColor("#404040"));

                        }
                    });
                    setSensorStatus("Disconnected");
                    footGatt.close();
                    footGatt = null;
                    Log.v("BLUETOOTH", "DISCONNECTED");
                }
                if(gatt == fireflyGatt){
                    if(!statusVariables.scanning){
                        statusVariables.scanning = true;
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
                    upperLegGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    bgFlag = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hipUI.rightTV.setVisibility(VISIBLE);
                            hipUI.leftTV.setVisibility(VISIBLE);
                        }
                    });
                }
                else if(gatt == lowerLegGatt) {
                    lowerLegGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    bgFlag = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            kneeUI.rightTV.setVisibility(VISIBLE);
                            kneeUI.leftTV.setVisibility(VISIBLE);
                        }
                    });
                }
                else if(gatt == footGatt) {
                    footGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    bgFlag = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ankleUI.rightTV.setVisibility(VISIBLE);
                            ankleUI.leftTV.setVisibility(VISIBLE);
                        }
                    });
                }
                else if(gatt == fireflyGatt)
                {
                    fireflyGatt.discoverServices();
                    fireflyGatt.requestMtu(76);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stimButton.setVisibility(VISIBLE);
                        }
                    });
                }
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
                    //mtu_flag = 1;
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
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(int i = 0; i < characteristics.size(); i++)
                {
                    Log.v("CHARACTERISTIC", characteristics.get(i).getUuid().toString());

                    //check for the MPU9250_SENSOR
                    if(characteristics.get(i).getUuid().toString().equals("0000beef-1212-efde-1523-785fef13d123"))
                    {
                        setSensorStatus("Connected");
                        if(statusVariables.scanning){
                            scanner.stopScan(mScanCallback);
                            statusVariables.scanning = false;

                        }
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
                    }
                    if(characteristics.get(i).getUuid().toString().equals("0000fff2-0000-1000-8000-00805f9b34fb"))
                    {
                        if(statusVariables.scanning){
                            scanner.stopScan(mScanCallback);
                            statusVariables.scanning = false;
                        }
                        Log.v("FIREFLY", "FOUND CHARACTERISTIC");
                        FIREFLY_CHARACTERISTIC2 = characteristics.get(i);
                        FIREFLY_CHARACTERISTIC2.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        statusVariables.fireflyCharFound = true;
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
    public void triggerFirefly(byte[] onOff)
    {
        if(statusVariables.fireflyCharFound){
            FIREFLY_CHARACTERISTIC2.setValue(onOff);
            boolean b = fireflyGatt.writeCharacteristic(FIREFLY_CHARACTERISTIC2);
            Log.i(TAG, "firefly write status = " + b);
        }

    }
    Runnable fireflyStop = new Runnable() {
        @Override
        public void run() {
        if(statusVariables.fireflyCharFound){
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
    Runnable scanTimeout = new Runnable() {
        @Override
        public void run() {
            if(statusVariables.scanning){
                scanner.stopScan(mScanCallback);
                setSensorStatus("Select");
                statusVariables.scanning = false;
                if(searchHip & upperLegGatt == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hipUI.connect.setBackgroundResource(R.drawable.hipwhite);
                        }
                    });
                }
                if(searchKnee & lowerLegGatt == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            kneeUI.connect.setBackgroundResource(R.drawable.kneewhite);
                        }
                    });
                }
                if(searchAnkle & footGatt == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hipUI.connect.setBackgroundResource(R.drawable.anklewhite);
                        }
                    });
                }
            }

        }
    };

    boolean hipCalibrate, kneeCalibrate, ankleCalibrate = false;

    public void connectThigh(View v){
        if(upperLegGatt == null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSensorStatus("Searching");
                    hipUI.connect.setBackgroundResource(R.drawable.hipyellow);
                }
            });
            searchHip = true;
            searchAnkle = false;
            searchKnee = false;
            statusVariables.scanning = true;
            scanner.startScan(mScanCallback);
            timerHandler.postDelayed(scanTimeout,10000);
            awaitingResponse = false;
        }
        else{
            //zero the sensor
            hipCalibrate = true;
            hipCalibrateCounter = 0;
            averageHip = 0;
            hipUI.leftPB.setProgress(0);
            hipUI.rightPB.setProgress(0);

        }
    }
    public void connectLowerLeg(View v){
        if(lowerLegGatt ==  null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSensorStatus("Searching");
                    kneeUI.connect.setBackgroundResource(R.drawable.kneeyellow);
                }
            });
            searchHip = false;
            searchAnkle = false;
            searchKnee = true;
            statusVariables.scanning = true;
            scanner.startScan(mScanCallback);
            timerHandler.postDelayed(scanTimeout,10000);
            awaitingResponse = false;
        }
        else{
            kneeCalibrate = true;
            kneeCalibrateCounter = 0;
            averageKnee = 0;
            kneeUI.leftPB.setProgress(0);
            kneeUI.rightPB.setProgress(0);
        }
    }
    public void connectFoot(View v){
        if(footGatt == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSensorStatus("Searching");
                    ankleUI.connect.setBackgroundResource(R.drawable.ankleyellow);
                }
            });
            searchHip = false;
            searchAnkle = true;
            searchKnee = false;
            statusVariables.scanning = true;
            scanner.startScan(mScanCallback);
            timerHandler.postDelayed(scanTimeout,10000);
            awaitingResponse = false;
        }
        else{
            ankleCalibrate = true;
            ankleCalibrateCounter = 0;
            averageAnkle = 0;
            ankleUI.leftPB.setProgress(0);
            ankleUI.rightPB.setProgress(0);
        }
    }
}