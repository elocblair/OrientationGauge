package com.example.nutri_000.testinggauge;

import android.app.Activity;
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
    public SensorUI(int button, int rPB, int lPB, int rSB, int lSB, int rTV, int lTV, int relativeLO, Activity MainActivity){
        connect = (ImageButton) MainActivity.findViewById(button);
        rightPB = (ProgressBar) MainActivity.findViewById(rPB);
        leftPB = (ProgressBar) MainActivity.findViewById(lPB);
        rightSB = (SeekBar) MainActivity.findViewById(rSB);
        leftSB = (SeekBar) MainActivity.findViewById(lSB);
        rightTV = (TextView) MainActivity.findViewById(rTV);
        leftTV = (TextView) MainActivity.findViewById(lTV);
        relativeLayout = (RelativeLayout) MainActivity.findViewById(relativeLO);
    }


}
