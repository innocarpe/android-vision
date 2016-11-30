package com.google.android.gms.samples.vision.barcodereader.customize;

import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;


/**
 * Created by Wooseong Kim in barcode-reader on 2016. 11. 30.
 *
 * BarcodeCropFocusingProcessor
 *  중앙 특정 영역 부근만 측정되도록 하는 로직
 */

public class BarcodeCropFocusingProcessor extends FocusingProcessor<Barcode> {
    private Rect cropFrameRect; // 카메라 소스 크기에 맞게 변환된 Crop 영역 Rect

    public BarcodeCropFocusingProcessor(Detector<Barcode> detector, Tracker<Barcode> tracker) {
        super(detector, tracker);
    }

    public void setPreviewRect(Rect previewRect, int paddingHorizontal, int paddingVertical) {
        int cropFrameWidth = previewRect.right - (paddingHorizontal * 2);
        int cropFrameHeight = previewRect.bottom - (paddingVertical * 2);

        Rect originalCropFrameRect = new Rect(
                paddingHorizontal, paddingVertical,
                paddingHorizontal + cropFrameWidth, paddingVertical + cropFrameHeight);

        // elemark 2 전용
        // 뷰와 소스의 비율 차이만큼 좌표 변환
        Rect previewSourceRect = new Rect(0, 0, 1080, 1440);
        float widthRatio = (float) previewSourceRect.width() / previewRect.width();
        float heightRatio = (float) previewSourceRect.height() / previewRect.height();

        this.cropFrameRect = new Rect(
                (int) (originalCropFrameRect.left * widthRatio), (int) (originalCropFrameRect.top * heightRatio),
                (int) (originalCropFrameRect.right * widthRatio), (int) (originalCropFrameRect.bottom * heightRatio));
    }

    @Override
    public int selectFocus(Detector.Detections<Barcode> detections) {
        SparseArray<Barcode> barcodes = detections.getDetectedItems();
        int detectedId = -1;

        for (int i = 0; i < barcodes.size(); ++i) {
            int id = barcodes.keyAt(i);

            Barcode barcode = barcodes.get(id);

            // 정상적인 상황에서 바코드가 측정 되었을 때, Crop 영역 외의 바코드는 무시하는 코드
            if (barcode != null) {
                if (cropFrameRect != null) {
                    Rect barcodeRect = barcode.getBoundingBox();

//                    Log.i("FocusingProcessor", barcodeRect.toString());

                    // 추후 라인 부근만 측정하게 할 때 사용, 다만 QR 코드 대응하려면 공간이 측정되게 해야 할 듯
//                    if (cropFrameRect.top > barcodeRect.top && cropFrameRect.bottom < barcodeRect.bottom) {
//                        detectedId = id;
//                    }

                    if (cropFrameRect.contains(barcodeRect)) {
                        detectedId = id;
                    }
                } else {
                    detectedId = id;
                }
            }
        }
        return detectedId;
    }
}
