package com.google.android.gms.samples.vision.barcodereader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public class CustomBarcodeCaptureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_bar_code_capture);
    }
}
