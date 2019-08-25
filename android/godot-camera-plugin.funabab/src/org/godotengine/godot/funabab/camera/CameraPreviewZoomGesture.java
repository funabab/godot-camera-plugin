package org.godotengine.godot.funabab.camera;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class CameraPreviewZoomGesture implements ScaleGestureDetector.OnScaleGestureListener {

    private final float ZOOM_SENSITIVITY = 0.05f;

    private CameraPreview mCameraPreview;
    private float mZoomScale = 0f;
    private int mMaxZoom;
    private ScaleGestureDetector mScaleGestureDetector;

    private boolean mActive = true;

    public CameraPreviewZoomGesture(Context context, CameraPreview cameraPreview) {
        mCameraPreview = cameraPreview;
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    public void setActive(boolean active) {
        mActive = active;
        if (!active) reset();
    }

    public void setMaxZoom(int maxZoom) {
        if (maxZoom == 0)
            mActive = false;
        mMaxZoom = maxZoom;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return mScaleGestureDetector.onTouchEvent(motionEvent);
    }

    public void reset() {
        mZoomScale = 0f;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        if (!mActive) return true;

        float scaleFactor = scaleGestureDetector.getScaleFactor() * ZOOM_SENSITIVITY;
        if (scaleGestureDetector.getCurrentSpan() > scaleGestureDetector.getPreviousSpan()) scaleFactor *= -1f;
        mZoomScale += scaleFactor;

        mZoomScale = Math.max(0f, Math.min(1f, mZoomScale));
        mCameraPreview.setCameraZoom((int)(mZoomScale * mMaxZoom));
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return mActive;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }
}
