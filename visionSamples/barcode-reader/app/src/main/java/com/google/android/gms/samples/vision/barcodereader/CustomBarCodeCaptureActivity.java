package com.google.android.gms.samples.vision.barcodereader;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.images.Size;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSource;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.zxd.zxdtestmode.Util;

import java.io.IOException;

import static com.google.android.gms.samples.vision.barcodereader.BarcodeCaptureActivity.AutoFocus;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public class CustomBarcodeCaptureActivity extends AppCompatActivity implements BarcodeRecognizer {
    private static final String TAG = "BarcodeCaptureActivity";
    public static final String BARCODE_VALUE = "BARCODE_VALUE";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private static final int INITIAL_CAMERA_FACING = CameraSource.CAMERA_FACING_FRONT;
    private static final int REQUESTED_PREVIEW_WIDTH = 1920;
    private static final int REQUESTED_PREVIEW_HEIGHT = 1440;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_bar_code_capture);

        mPreview = (CameraSourcePreview) findViewById(R.id.barcode_capture_camera_source_preview);
        //noinspection unchecked
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.barcode_capture_camera_graphic_overlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
//        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
//        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
//        if (rc == PackageManager.PERMISSION_GRANTED) {

        createCameraSource(autoFocus, false);

        // TODO: 추후 동적으로 뷰 너비 높이를 구할 필요가 없으면 삭제하기
        /*
        mPreview.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mPreview.getViewTreeObserver().removeOnPreDrawListener(this);

                startCameraSource();
                return true;
            }
        });
        */

//        } else {
//            requestCameraPermission();
//        }

//        gestureDetector = new GestureDetector(this, new BarcodeCaptureActivity.CaptureGestureListener());
//        scaleGestureDetector = new ScaleGestureDetector(this, new BarcodeCaptureActivity.ScaleListener());

//        Snackbar.make(mGraphicOverlay, "Tap to capture. Pinch/Stretch to zoom",
//                Snackbar.LENGTH_LONG)
//                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();

        // TODO: 중앙 Crop 영역만 바코드를 인식하게 제한
        BarcodeTracker tracker = new BarcodeTracker(this, this, false);
        final BarcodeCropFocusingProcessor focusingProcessor = new BarcodeCropFocusingProcessor(barcodeDetector, tracker);
        mPreview.setCallback(new CameraSourcePreviewCallback() {
            @Override
            public void onCameraPreviewSizeDetermined(Size previewSize) {
                focusingProcessor.setCameraSourceSize(previewSize);
            }
        });
        barcodeDetector.setProcessor(focusingProcessor);

        mPreview.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mPreview.getViewTreeObserver().removeOnPreDrawListener(this);

                Rect visibleRect = new Rect();
                mPreview.getLocalVisibleRect(visibleRect);

                focusingProcessor.setPreviewRect(visibleRect, mPreview.getPaddingLeft(), mPreview.getPaddingTop());
                return true;
            }
        });

        // TODO: 기존 소스
//        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, this);
//        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(INITIAL_CAMERA_FACING)
                .setRequestedPreviewSize(REQUESTED_PREVIEW_WIDTH, REQUESTED_PREVIEW_HEIGHT) // 실제 디바이스 width, height를 사용하는 것이 최적의 프리뷰 사이즈 선정에 유리하다 판단
                .setRequestedFps(30.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            //noinspection deprecation
            // TODO: 추후 매크로 모드 관련 조사해보기
//            builder = builder.setFocusMode(
//                    autoFocus ? Camera.Parameters.FOCUS_MODE_MACRO : null);
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        //noinspection deprecation
        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }



    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        setFrontCameraAsTopCamera(true);
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }

        // 앱에서 나올 때 원래대로 전면 카메라를 사용하기 위함
        setFrontCameraAsTopCamera(false);
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setFrontCameraAsTopCamera(boolean topCameraEnabled) {
        int cameraId = topCameraEnabled ? 1 : 0;

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 전면 카메라 대신 Top 카메라 사용하도록 하는 로직, 0을 사용하면 원래대로 전면 카메라를 사용
                Util.camera_switch(cameraId);
                break;
            }
        }
    }

    @Override
    public void onRecognized(final Barcode item) {
        Intent data = new Intent();
        data.putExtra(BARCODE_VALUE, item.displayValue);
        setResult(CommonStatusCodes.SUCCESS, data);

        Log.i(TAG, "barcode: " + item.displayValue);
        Log.i(TAG, "boundingBox: " + item.getBoundingBox());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String description = "barcode: " + item.displayValue + "\nboundingBox: " + item.getBoundingBox();
                Toast.makeText(CustomBarcodeCaptureActivity.this, description, Toast.LENGTH_SHORT).show();
            }
        });
//        finish();
    }
}
