package com.example.nutri_000.testinggauge;

import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;


public class SensorUI extends AppCompatActivity {
    public ImageButton connect;
    public ProgressBar rightPB, leftPB;
    public SeekBar rightSB, leftSB;
    public TextView rightTV, leftTV;
    public RelativeLayout relativeLayout;
    SensorUI(int button, int rPB, int lPB, int rSB, int lSB, int rTV, int lTV, int relativeLO){
        connect = (ImageButton) findViewById(button);
        rightPB = (ProgressBar) findViewById(rPB);
        leftPB = (ProgressBar) findViewById(lPB);
        rightSB = (SeekBar) findViewById(rSB);
        leftSB = (SeekBar) findViewById(lSB);
        rightTV = (TextView) findViewById(rTV);
        leftTV = (TextView) findViewById(lTV);
        relativeLayout = (RelativeLayout) findViewById(relativeLO);
    }


}
