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
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.samples.vision.barcodereader.customize.BarcodeCropFocusingProcessor;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSource;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSourcePreview;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

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

    private CameraSource mCameraSource;
    // TODO: 나중에 변수명 mCameraSourcePreview로 수정할 것
    private CameraSourcePreview mPreview;
//    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_bar_code_capture);

        mPreview = (CameraSourcePreview) findViewById(R.id.barcode_capture_camera_source_preview);
        View captureLineView = findViewById(R.id.barcode_capture_line_view);

        startCaptureLineAnimation(captureLineView);

        //noinspection unchecked
//        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.barcode_capture_camera_graphic_overlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
//        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
//        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
//        if (rc == PackageManager.PERMISSION_GRANTED) {

        createCameraSource(autoFocus, false);
    }

    private void startCaptureLineAnimation(View captureLineView) {
        Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink_animation);
        captureLineView.startAnimation(blinkAnimation);
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

        // 필수적인 코드만 읽도록 수정 (1D + QR Code) -> 추후 어떻게 변경될 지 모르므로 모두 다 지원하도록 풀기
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
//                .setBarcodeFormats(
//                        Barcode.EAN_8 | Barcode.EAN_13 | Barcode.UPC_A | Barcode.UPC_E | Barcode.ITF |
//                        Barcode.CODE_39 | Barcode.CODE_93 | Barcode.CODE_128 | Barcode.CODABAR | Barcode.QR_CODE)

//        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(null, this);
//        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

        // 중앙 Crop 영역 부근만 측정하도록 하는 코드
        BarcodeGraphicTracker tracker = new BarcodeGraphicTracker(null, null, this);
        final BarcodeCropFocusingProcessor focusingProcessor = new BarcodeCropFocusingProcessor(barcodeDetector, tracker);
        barcodeDetector.setProcessor(focusingProcessor);

//        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(null, this);
//        final CentralFocusedDetector centralFocusedDetector = new CentralFocusedDetector(barcodeDetector);
//        centralFocusedDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

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
                .setFacing(CameraSource.CAMERA_FACING_BACK)
//                .setRequestedPreviewSize(300 * 2, 150 * 2)
                .setRequestedPreviewSize(1600, 1024)
//                .setRequestedPreviewSize(1600, 800)
//                .setRequestedFps(15.0f);
                .setRequestedFps(30.0f); // 좀 더 부드럽게 보이도록 수정
        // https://developers.google.com/vision/multi-tracker-tutorial
        // 위 링크에 관련 내용 있음 1600/1024, 15fps로 설정할 것

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            builder = builder.setFocusMode(
//                    autoFocus ? Camera.Parameters.FOCUS_MODE_MACRO : null);
            // 매크로 모드로는 조금 멀리서 바코드를 측정해야 하는 경우 대응을 못해서 오토 포커싱을 해야만 한다고 생각
            //noinspection deprecation
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
                // 오버레이를 아예 사용하지 않도록 수정
//                mPreview.start(mCameraSource, mGraphicOverlay);
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onBarcodeRecognized(Barcode item) {
        Intent data = new Intent();
        data.putExtra(BARCODE_VALUE, item.displayValue);
        setResult(CommonStatusCodes.SUCCESS, data);
        finish();
    }
}
