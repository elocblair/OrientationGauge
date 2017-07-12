package com.example.nutri_000.testinggauge;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class BleService extends Service {
    private BluetoothAdapter adapter;
    public BluetoothLeScanner scanner;
    String TAG = "bleService";
    private IBinder bleBinder = new BleBinder();

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

    }

    public int onStartCommand(Intent intent, int flags, int startId){
        //scanner.startScan(mScanCallback);
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
        }
    };
    public void initializeBle(){
        adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();

    }
}
