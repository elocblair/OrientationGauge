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

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static android.view.View.VISIBLE;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Cole";
    protected UUID[] serviceUUIDs;
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean currentlyStimming = false;
    private ArcProgress gauge;
    private SeekArc seekArc;
    Handler timerHandler = new Handler();
    Handler connectionCheckHandler = new Handler();
    private CoordinatorLayout mContainerView;
    private Vibrator v;
    private BluetoothAdapter adapter;
    private static Context context;
    private BluetoothLeScanner scanner;
    private int displayDataCounter = 0;
    //BLE connections for the firefly
    boolean connectedToFirefly = false;
    private BluetoothGatt fireflyGatt;
    private BluetoothGattCharacteristic FIREFLY_CHARACTERISTIC2;
    private BluetoothDevice firefly = null;
    private TextView fireflyStatus;
    private FloatingActionButton stimButton;
    private Button scanForPCM;
    private String fireflyColor = "";

    //ble connections for the sensor
    boolean connectedToSensor = false;
    private BluetoothGatt sensorGatt;
    private BluetoothGattCharacteristic NRF_CHARACTERISTIC;
    private BluetoothDevice MPU9250 = null;
    private TextView sensorStatus;
    private int stimmingThreshold = 30;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private boolean isScanning = false;
    private boolean rescan = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivity.context = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        stimButton = (FloatingActionButton) findViewById(R.id.stim_buton);
        scanForPCM = (Button) findViewById(R.id.scanButton);
        scanForPCM.setBackgroundColor(Color.BLACK);
        fireflyStatus = (TextView) findViewById(R.id.FireflyStatus);
        sensorStatus = (TextView) findViewById(R.id.SensorStatus);
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
            }

        });
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
    //public static byte [] startStim = {2, 0, 1};
    int mtu_flag = 0;
    int charfound = 0;
    int char4found = 0;

    public static byte [] stopStim = {2, 0, 0};

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
                    peripheral = device.getDevice();
                    if(device.getRssi() > -35){
                        sensorGatt = peripheral.connectGatt(getAppContext(),false,btleGattCallback);
                        //Log.d(TAG, "found device");
                    }
                }
                if(deviceName.equals("FireflyPCM")){
                //if(device.getDevice().getAddress().equals("24:71:89:19:F0:84")){
                    firefly = device.getDevice();
                    if(rescan){
                        fireflyGatt = firefly.connectGatt(getAppContext(),false,btleGattCallback);
                    }
                }
            }

        }
    };
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
            // this will get called anytime you perform a read or write characteristic operation
            if(gatt == fireflyGatt){
                Log.v(TAG, "notify from PCM");
            }
            if(gatt == sensorGatt) {
                displayDataCounter++;
                byte[] temp = characteristic.getValue();
                int MSB = temp[1] << 8;
                int LSB = temp[0] & 0x000000FF;
                int val = MSB | LSB;
                float gyroX = val * 0.0625f;
                MSB = temp[3] << 8;
                LSB = temp[2] & 0x000000FF;
                val = MSB | LSB;
                float gyroY = val * 0.0625f;
                MSB = temp[5] << 8;
                LSB = temp[4] & 0x000000FF;
                val = MSB | LSB;
                float gyroZ = val * 0.0625f;

                final String output = (int)gyroX + "\n" + (int)gyroY + "\n" + (int)gyroZ;
                int gaugeValue = 0;

                if(((gyroZ - 90.0f) > 0.0f) & gyroZ < 190.0f)
                {
                    gaugeValue = (int)(gyroZ - 90.0f);
                }
                else if(gyroZ >= 190.0f & gyroZ < 270.0f)
                {
                    gaugeValue = 100;
                }
                else if(gyroZ >= 270.0f || gyroZ <= 90.0f)
                {
                    gaugeValue = 0;
                }
                if(displayDataCounter == 1) {
                    displayDataCounter = 0;
                    if (gaugeValue > stimmingThreshold & gaugeValue < 225) {
                        setGaugeProperties(true);
                        if(currentlyStimming == false) {
                            currentlyStimming = true;
                            Log.v(TAG, "Start command");
                            triggerFirefly(startStim);
                            timerHandler.postDelayed(timerRunnable, 1000);
                        }

                    } else {
                        setGaugeProperties(false);

                    }
                    setGaugeValue(gaugeValue);
                }
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if(newState == 0)
            {
                if(gatt == sensorGatt){
                    setSensorStatus("Searching");
                    if(!isScanning){
                        isScanning = true;
                        scanner.startScan(mScanCallback);
                        timerHandler.postDelayed(scanTimeout, 10000);
                    }

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
                if(gatt == sensorGatt) {
                    connectedToSensor = true;
                    sensorGatt.discoverServices();
                    setSensorStatus("Connecting...");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mContainerView.setBackgroundColor(Color.parseColor("#0000FF"));
                        }
                    });
                    timerHandler.postDelayed(connectedBackgroundColorReset,3000);
                } else if(gatt == fireflyGatt)
                {
                    fireflyGatt.discoverServices();
                    setFireflyStatus("Connecting...");
                    fireflyGatt.requestMtu(76);
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
                setFireflyStatus("Connecting...");
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
                        connectedToSensor = true;
                        Log.v("NRF Sensor", "FOUND CHARACTERISTIC");
                        NRF_CHARACTERISTIC = service.getCharacteristic(UUID.fromString("0000beef-1212-efde-1523-785fef13d123"));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sensorStatus.setTextColor(Color.parseColor("#ffffff"));
                            }
                        });
                        sensorGatt.setCharacteristicNotification(NRF_CHARACTERISTIC,true);
                        UUID dUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor notifyDescriptor = NRF_CHARACTERISTIC.getDescriptor(dUUID);
                        notifyDescriptor.setValue(ENABLE_NOTIFICATION_VALUE);
                        boolean b = sensorGatt.writeDescriptor(notifyDescriptor);
                        Log.v("descriptor write ", String.valueOf(b) );

                        if(!connectedToFirefly) {
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
                        }
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
    public void triggerFirefly(byte[] onOff)
    {
        if(charfound == 1 & char4found == 0) {

            FIREFLY_CHARACTERISTIC2.setValue(onOff);
            boolean b = fireflyGatt.writeCharacteristic(FIREFLY_CHARACTERISTIC2);
            Log.v(TAG, "write status = " + b);
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContainerView.setBackgroundColor(Color.parseColor("#333333"));
                }
            });
        }
    };

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
    }

}

