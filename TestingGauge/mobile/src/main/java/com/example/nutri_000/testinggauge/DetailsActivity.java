package com.example.nutri_000.testinggauge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

public class DetailsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    BleService bleService;
    android.os.Handler timerHandler = new android.os.Handler();
    boolean isBound = false;
    Button scanButton;
    TextView approvedDevice1, approvedDevice2, approvedDevice3;
    String newApprovedDevice;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.BleBinder binder = (BleService.BleBinder) service;
            bleService = binder.getService();
            isBound = true;
            //bleService.initializeBle();
            /*bleService.searchingFromDetails = true;
            bleService.scanner.startScan(bleService.mScanCallback);
            bleService.scanning = true;
            timerHandler.postDelayed(scanStop, 5000);*/

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
            isBound = false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        scanButton = (Button) findViewById(R.id.scanButton);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        Intent bleIntent = new Intent(this, BleService.class);
        bindService(bleIntent, mServiceConnection, this.BIND_AUTO_CREATE);
        approvedDevice1 = (TextView) findViewById(R.id.device1);
        approvedDevice2 = (TextView) findViewById(R.id.device2);
        approvedDevice3 = (TextView) findViewById(R.id.device3);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        //recyclerView.setHasFixedSize(false);
        // use a linear layout manager


        Intent intent = getIntent();
        Bundle deviceAddresses = intent.getExtras();
        String hipDevice = deviceAddresses.getString("hipDeviceAddress");
        TextView hipAddress = (TextView) findViewById(R.id.hipAddress);
        String kneeDevice = deviceAddresses.getString("kneeDeviceAddress");
        TextView kneeAddress = (TextView) findViewById(R.id.kneeAddress);
        String ankleDevice = deviceAddresses.getString("ankleDeviceAddress");
        TextView ankleAddress = (TextView) findViewById(R.id.ankleAddress);
        hipDevice = "hip: " + hipDevice;
        kneeDevice = "knee: " + kneeDevice;
        ankleDevice = "ankle: " + ankleDevice;
        hipAddress.setText(hipDevice);
        kneeAddress.setText(kneeDevice);
        ankleAddress.setText(ankleDevice);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("newDevice"));
        String defaultValue = "000000";

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()== android.R.id.home) {
            Log.v("recycler click", String.valueOf(item));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    Runnable scanStop = new Runnable() {
        @Override
        public void run() {
            if(bleService.scanning){
                bleService.scanner.stopScan(bleService.mScanCallback);
                bleService.scanning = false;
            }
            bleService.searchingFromDetails = false;
            List<String> input = new ArrayList<>();
            for (int i = 0; i < bleService.shockclockCount; i++) {
                input.add(bleService.deviceIDs[i]);
            }// define an adapter
            mAdapter = new RecyclerAdapter(input);
            recyclerView.setAdapter(mAdapter);
        }
    };
    public void scanClicked(View v){
        if(isBound) {
            Log.v("details", "service bound");
            if(bleService.scanning){
                bleService.scanner.stopScan(bleService.mScanCallback);
                bleService.scanning = false;
                bleService.searchingFromDetails = false;
            }
            if (bleService.scanning != true) {
                Log.v("details", "starting scan");
                bleService.searchingFromDetails = true;
                bleService.scanner.startScan(bleService.mScanCallback);
                bleService.scanning = true;
                timerHandler.postDelayed(scanStop, 5000);
                for(int i = 0; i > bleService.shockclockCount; i++){
                    bleService.deviceIDs[i] = null;
                }
                bleService.shockclockCount = 0;
            }
        }
    }
    private BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            newApprovedDevice = extras.getString("deviceAddress");
            Log.v("new device", "Address: " + newApprovedDevice);
            setNewApprovedDevice(newApprovedDevice);
            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                approvedDevice1.setText(newApprovedDevice);
                }
            });*/
        }
    };
    public void setNewApprovedDevice(final String newDevice){
        if(approvedDevice1.getText().toString().equals("1")){
            approvedDevice1.setText(newDevice);
        }
        else if(approvedDevice2.getText().toString().equals("2")){
            approvedDevice2.setText(newDevice);
        }
        else if(approvedDevice3.getText().toString().equals("3")){
            approvedDevice3.setText(newDevice);
        }
        else{
            String dev2 = approvedDevice1.getText().toString();
            String dev3 = approvedDevice2.getText().toString();
            approvedDevice2.setText(dev2);
            approvedDevice3.setText(dev3);
            approvedDevice1.setText(newDevice);
        }
        bleService.approvedDevices[0] = approvedDevice1.getText().toString();
        bleService.approvedDevices[1] = approvedDevice2.getText().toString();
        bleService.approvedDevices[2] = approvedDevice3.getText().toString();
    }

}
