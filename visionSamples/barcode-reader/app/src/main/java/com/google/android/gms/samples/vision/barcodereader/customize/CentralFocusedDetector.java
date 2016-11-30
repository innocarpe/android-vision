package com.google.android.gms.samples.vision.barcodereader.customize;

import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;


/**
 * Created by Wooseong Kim in barcode-reader on 2016. 11. 30.
 *
 * CentralFocusedDetector
 */

public class CentralFocusedDetector extends Detector<Barcode> {
    private Detector<Barcode> mDelegate;
    private Rect mCropFrameRect;

    public CentralFocusedDetector(Detector<Barcode> delegate) {
        mDelegate = delegate;
    }

    public void setCropFrameRect(Rect cropFrameRect) {
        mCropFrameRect = cropFrameRect;
    }

    public SparseArray<Barcode> detect(Frame frame) {
        /*
        // *** crop the frame here
        Bitmap croppedBitmap;
        if (frame.getBitmap() != null) {
            croppedBitmap = frame.getBitmap();
        } else {
            Frame.Metadata metadata = frame.getMetadata();

            ByteBuffer byteBuffer = frame.getGrayscaleImageData();

            YuvImage yuvimage = new YuvImage(byteBuffer.array(), ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 100, outputStream);
            byte[] jpegArray = outputStream.toByteArray();
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

            // elemark 2 에서 들어오는 백 카메라 화상이 90도, 추후 Top 카메라도 확인 필
            Bitmap rotatedBitmap;
            if (metadata.getRotation() != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90 * metadata.getRotation());
                rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                        originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            } else {
                rotatedBitmap = originalBitmap;
            }

            // 너비는 같고 센터에서 위 아래 200px 씩 잘라봄
            int cropFrameHeight = 400;
            croppedBitmap = Bitmap.createBitmap(
                    rotatedBitmap,
                    0,
                    (rotatedBitmap.getHeight() / 2) - (cropFrameHeight / 2),
                    rotatedBitmap.getWidth(),
                    cropFrameHeight);
        }

        Frame croppedFrame = new Frame.Builder().setBitmap(croppedBitmap).build();
        */

        return mDelegate.detect(frame);
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}
