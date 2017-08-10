package com.example.nutri_000.testinggauge;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static android.view.View.VISIBLE;
import static com.example.nutri_000.testinggauge.MainActivity.getAppContext;

public class BleService extends Service {
    private BluetoothAdapter adapter;
    public BluetoothLeScanner scanner;
    public boolean searchingHip, searchingKnee, searchingAnkle = false;
    public boolean searchingPCM = true;
    BluetoothGatt hipGatt, kneeGatt, ankleGatt, fireflyGatt;
    private int connected = 2;
    private int connecting = 1;
    private int disconnected = 0;
    public boolean scanning = true;
    String TAG = "bleService";
    private IBinder bleBinder = new BleBinder();
    Intent intent;
    //BluetoothDevice sensor;
    private BluetoothGattCharacteristic NRF_CHARACTERISTIC;
    public BluetoothGattCharacteristic FIREFLY_CHARACTERISTIC2;
    boolean fireflyFound = false;
    public class BleBinder extends Binder {
        BleService getService(){
            return BleService.this;
        }
    }
    public IBinder onBind(Intent intent){
        return bleBinder;
    }
    @Override
    public void onCreate(){
        intent = new Intent(TAG);
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        return Service.START_NOT_STICKY;
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
            Log.i(TAG, "New LE Device: " + device.getDevice().getName() + " @ " + device.getRssi() + " Address " + device.getDevice().getAddress());
            String deviceName;
            deviceName = device.getDevice().getName();
            if(deviceName != null){
                if(deviceName.equals("JohnCougarMellenc")){

                    String bleEvent = "scan";
                    intent.putExtra("bleEvent", bleEvent);
                    sendBroadcast(intent);
                    if(searchingHip){
                        BluetoothDevice sensor = device.getDevice();
                        scanner.stopScan(mScanCallback);
                        scanning = false;
                        hipGatt = sensor.connectGatt(getAppContext(),false,bleGattCallback);
                    }
                    else if(searchingKnee){
                        BluetoothDevice sensor = device.getDevice();
                        scanner.stopScan(mScanCallback);
                        scanning = false;
                        kneeGatt = sensor.connectGatt(getAppContext(),false,bleGattCallback);
                    }
                    else if(searchingAnkle){
                        BluetoothDevice sensor = device.getDevice();
                        scanner.stopScan(mScanCallback);
                        scanning = false;
                        ankleGatt = sensor.connectGatt(getAppContext(),false,bleGattCallback);
                    }
                }
                //if(device.getDevice().getAddress().equals("A0:E6:F8:BF:E6:04")){
                if(deviceName.equals("FireflyPCM")){
                    if(searchingPCM){
                        BluetoothDevice sensor = device.getDevice();
                        scanner.stopScan(mScanCallback);
                        scanning = false;
                        fireflyGatt = sensor.connectGatt(getAppContext(),false,bleGattCallback);
                    }

                }
            }
        }
    };

    public void initializeBle(){
        adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();

    }

    public final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if(gatt == hipGatt | gatt == kneeGatt | gatt == ankleGatt) {
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
                String bleEvent = "notification";
                intent.putExtra("bleEvent", bleEvent);
                intent.putExtra("value", gyroX);
                if(gatt == hipGatt){
                    intent.putExtra("gatt","hip");
                }
                if(gatt == kneeGatt){
                    intent.putExtra("gatt","knee");
                }
                if(gatt == ankleGatt){
                    intent.putExtra("gatt","ankle");
                }
                sendBroadcast(intent);

            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if(newState == disconnected) {
                String bleEvent = "sensorDisconnected";
                intent.putExtra("bleEvent", bleEvent);
                if(gatt.equals(hipGatt)){
                    intent.putExtra("gatt","hip");
                }
                else if(gatt.equals(kneeGatt)){
                    intent.putExtra("gatt","knee");
                }
                else if(gatt.equals(ankleGatt)){
                    intent.putExtra("gatt","ankle");
                }
                else if(gatt.equals(fireflyGatt)){
                    intent.putExtra("gatt","firefly");
                    fireflyFound = false;
                }
                else{
                    intent.putExtra("gatt", "unknown");
                }
                sendBroadcast(intent);
            }
            else if( newState == connecting) {
            }
            else if( newState == connected) {
                Log.v(TAG, "device connected");
                gatt.discoverServices();
            }
        }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status) {
            Log.v(TAG, "charRead");
        }
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.v(TAG, "services discovered");
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(int i = 0; i < characteristics.size(); i++){
                    if(characteristics.get(i).getUuid().toString().equals("0000beef-1212-efde-1523-785fef13d123")){
                        NRF_CHARACTERISTIC = service.getCharacteristic(UUID.fromString("0000beef-1212-efde-1523-785fef13d123"));
                        gatt.setCharacteristicNotification(NRF_CHARACTERISTIC,true);
                        UUID dUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor notifyDescriptor = NRF_CHARACTERISTIC.getDescriptor(dUUID);
                        notifyDescriptor.setValue(ENABLE_NOTIFICATION_VALUE);
                        boolean b = gatt.writeDescriptor(notifyDescriptor);
                        scanner.stopScan(mScanCallback);
                        scanning = false;
                        String bleEvent = "sensorConnected";
                        intent.putExtra("bleEvent", bleEvent);
                        intent.putExtra("gatt", "undetermined");
                        if(gatt == hipGatt){
                            intent.putExtra("gatt", "hip");
                        }
                        if(gatt == kneeGatt){
                            intent.putExtra("gatt", "knee");
                        }
                        if(gatt == ankleGatt){
                            intent.putExtra("gatt","ankle");
                        }
                        sendBroadcast(intent);
                        Log.v(TAG, String.valueOf(b));
                    }
                    if(characteristics.get(i).getUuid().toString().equals("0000fff2-0000-1000-8000-00805f9b34fb")) {
                        FIREFLY_CHARACTERISTIC2 = characteristics.get(i);
                        FIREFLY_CHARACTERISTIC2.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        fireflyFound = true;
                        Log.v(TAG, "pcm connected");
                        String bleEvent = "sensorConnected";
                        intent.putExtra("bleEvent", bleEvent);
                        intent.putExtra("gatt","firefly");
                        sendBroadcast(intent);
                    }
                }
            }
        }
    };
}
