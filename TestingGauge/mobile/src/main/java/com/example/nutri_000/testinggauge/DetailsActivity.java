package com.example.nutri_000.testinggauge;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.TextView;

public class DetailsActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()== android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
