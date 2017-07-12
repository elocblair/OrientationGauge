package com.example.nutri_000.testinggauge;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

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


    }
}
