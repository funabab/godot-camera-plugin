package org.godotengine.godot.funabab.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.graphics.Point;

import java.util.List;

public abstract class CameraPreview extends FrameLayout implements TextureView.SurfaceTextureListener {

    private Camera mCamera;
    private TextureView mTextureView;
    private Camera.CameraInfo mCameraInfo;
    private Camera.Parameters mCameraParameters;
    private int mDeviceRotation = -1;
    private Display mDeviceDisplay;
    private boolean mIsCameraPreviewing = false;
    private CameraPreviewFaceDetection mCameraPreviewFaceDetection;
    private CameraPreviewZoomGesture mCameraZoomGesture;

    private FrameLayout mRoot;

    private boolean mPreviewFaceDetection;
    private int mOriginalCameraOrientation = 0;
    private float mOutputImageResolution = 1f;

    @SuppressLint("ClickableViewAccessibility")
    public CameraPreview(Context context, Camera camera, Camera.CameraInfo info) {
        super(context);
        setClipChildren(true);
        mDeviceDisplay = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        mRoot = new FrameLayout(context);
        mRoot.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        mRoot.setX(0);
        mRoot.setY(0);

        mTextureView = new TextureView(context);
        mTextureView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRoot.addView(mTextureView);

        mCameraPreviewFaceDetection = new CameraPreviewFaceDetection(context, 0xffffffff, this);
        mCameraPreviewFaceDetection.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRoot.addView(mCameraPreviewFaceDetection);

        mCameraZoomGesture = new CameraPreviewZoomGesture(context, this);

        setCamera(camera, info);

        mTextureView.setSurfaceTextureListener(this);
        addView(mRoot);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isActive())
                    return mCameraZoomGesture.onTouchEvent(motionEvent);
                else
                    return true;
            }
        });
    }

    public Camera.CameraInfo getCameraInfo() {
        return mCameraInfo;
    }

    public void setCameraZoom(int zoom) {
        if (!isActive() || !mCameraParameters.isZoomSupported()) return;
        mCameraParameters.setZoom(zoom);
        setCameraParameters();
    }

    public void takePicture(final TakePictureCallback callback) {
        if (callback == null || !isActive()) return;

        final Rect visibleRect = new Rect();
        getLocalVisibleRect(visibleRect);

        final List<Rect> detectedFaces = mCameraPreviewFaceDetection.getDetectedFacesRect(visibleRect, null);
        if (!callback.onPicturePreTake(detectedFaces)) {
            return;
        }

        try {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes, Camera camera) {
                    mIsCameraPreviewing = false;
                    final ViewGroup.LayoutParams layoutParams = mRoot.getLayoutParams();
                    final Camera.Size pictureSize = mCameraParameters.getPictureSize();

                    final Point pSize = new Point(pictureSize.width, pictureSize.height);
                    if ((mCameraInfo.orientation / 90) % 2 == 1)
                        pSize.set(pictureSize.height, pictureSize.width);

                    // decode image
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    // rotate the image
                    final Matrix matrix = new Matrix();
                    matrix.setRotate(mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 360 - mCameraInfo.orientation : mCameraInfo.orientation);
                    if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                        matrix.postScale(-mOutputImageResolution, mOutputImageResolution); // fix front facing camera flipping
                    else
                        matrix.postScale(mOutputImageResolution, mOutputImageResolution);

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

                    // crop the image
                    final Bitmap outputBitmap = Bitmap.createBitmap(bitmap,
                            (int) (((float) visibleRect.left / layoutParams.width) * pSize.x * mOutputImageResolution),
                            (int) (((float) visibleRect.top / layoutParams.height) * pSize.y * mOutputImageResolution),
                            (int) (((float) visibleRect.right / layoutParams.width) * pSize.x * mOutputImageResolution),
                            (int) (((float) visibleRect.bottom / layoutParams.height) * pSize.y * mOutputImageResolution)
                    );

                    callback.onPictureTaken(outputBitmap, mCameraPreviewFaceDetection.getDetectedFacesRect(visibleRect,
                            new PointF(((float)pSize.x / layoutParams.width) * mOutputImageResolution, ((float)pSize.y / layoutParams.height) * mOutputImageResolution)));
                }
            });
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void refreshPreview() {
        if (!mIsCameraPreviewing)
            startCameraPreview();
    }

    protected final boolean isCameraPreviewing() {
        return mIsCameraPreviewing;
    }

    protected static Camera.CameraInfo getCameraInfo(int cameraId) {
        final Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info;
    }

    protected static int getFacingCameraId(int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == facing) return i;
        }
        return 0; //fallback
    }

    private void setCamera() {
        if (isActive()) return;
        mCamera = Camera.open(getFacingCameraId(mCameraInfo.facing));
        mCamera.setFaceDetectionListener(mCameraPreviewFaceDetection);
        setPreviewOrientation();
    }

    protected final void setCamera(Camera camera, Camera.CameraInfo info) {
        if (isActive()) {
            destroyPreview();
        }

        mCamera = camera;
        mCameraInfo = info;
        mOriginalCameraOrientation = info.orientation;

        mCamera.setFaceDetectionListener(mCameraPreviewFaceDetection);
        mCameraParameters = mCamera.getParameters();

        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCameraParameters.setJpegQuality(100); // best quality

        mCameraZoomGesture.setMaxZoom(!mCameraParameters.isZoomSupported() ? 0 : mCameraParameters.getMaxZoom());
        setPreviewOrientation();
    }

    private void setCameraParameters() {
        try {
            mCamera.setParameters(mCameraParameters);
        } catch (Exception e) {
            Log.e(FunababCameraPlugin.TAG, "setCameraParameters: " + e);
        }
    }

//    protected final void setImageQuality(int quality) {
//        mCameraParameters.setJpegQuality(quality);
//        setCameraParameters();
//    }

    private void setCameraOutputSizes(int cameraOrientation) {

        final Point deviceScreenSize = new Point();
        mDeviceDisplay.getRealSize(deviceScreenSize);

        final Camera.Size previewSize = getOptimalSize(mCameraParameters.getSupportedPreviewSizes(), deviceScreenSize);
        final Camera.Size pictureSize = getOptimalSize(mCameraParameters.getSupportedPictureSizes(), previewSize);

        float aspectRatio;

        if ((cameraOrientation / 90) % 2 == 0) {
            aspectRatio = (float)previewSize.height/previewSize.width;
        } else {
            aspectRatio = (float)previewSize.width/previewSize.height;
        }

        mCameraParameters.setPreviewSize(previewSize.width, previewSize.height);
        mCameraParameters.setPictureSize(pictureSize.width, pictureSize.height);

        final ViewGroup.LayoutParams layoutParams = mRoot.getLayoutParams();
        layoutParams.width = deviceScreenSize.x;
        layoutParams.height = (int)(deviceScreenSize.x * aspectRatio);

        // This should fix non matching situation if returned screen height (e.g when using a modded rom) is not actual screen height or if it doesn't match preview height
        // E.G: flip comparison operator to see an example on affected devices.
        if (layoutParams.height < deviceScreenSize.y) {
            layoutParams.width = (int)(deviceScreenSize.y * ((float)layoutParams.width/layoutParams.height));
            layoutParams.height = deviceScreenSize.y;
        }

        mRoot.setLayoutParams(layoutParams);
        setCameraParameters();
    }

    protected final void setFlashMode(String flashMode) {
        if (mCameraParameters.getFlashMode() != null) {
            final String flashModeValues = mCameraParameters.get("flash-mode-values");
            if (flashModeValues != null && !flashModeValues.isEmpty())
                if (!flashModeValues.contains(flashMode))
                    return;
            mCameraParameters.setFlashMode(flashMode);
            setCameraParameters();
        }
    }

//    protected final void setEnableShutterSound(boolean mode) {
//        if (!isActive()) return;
//        if (!mode && !mCameraInfo.canDisableShutterSound) return;
//        mCamera.enableShutterSound(mode);
//    }

    protected final void setColorEffect(String colorEffect) {
        if (mCameraParameters.getColorEffect() != null) {
            final String colorEffectValues = mCameraParameters.get("effect-values");
            if (colorEffectValues != null && !colorEffectValues.isEmpty())
                if (!colorEffectValues.contains(colorEffect))
                    return;
            mCameraParameters.setColorEffect(colorEffect);
            setCameraParameters();
        }
    }

    protected final void setSceneMode(String sceneMode) {
        if (mCameraParameters.getSceneMode() != null) {
            final String sceneModeValues = mCameraParameters.get("scene-mode-values");
            if (sceneModeValues != null && !sceneModeValues.isEmpty())
                if (!sceneModeValues.contains(sceneMode))
                    return;
            mCameraParameters.setSceneMode(sceneMode);
            setCameraParameters();
        }
    }

    protected final void setWhiteBalance(String whiteBalance) {
        if (mCameraParameters.getWhiteBalance() != null) {
            final String whiteBalanceValues = mCameraParameters.get("whitebalance-values");
            if (whiteBalanceValues != null && !whiteBalanceValues.isEmpty())
                if (!whiteBalanceValues.contains(whiteBalance))
                    return;
            mCameraParameters.setWhiteBalance(whiteBalance);
            setCameraParameters();
        }
    }

    protected final void setPinchToZoomMode(boolean active) {
        mCameraZoomGesture.setActive(active);
    }

    protected final void setFaceRecognitionMode(boolean active) {
        if (!isActive())
            return;

        mCameraPreviewFaceDetection.reset();
        mPreviewFaceDetection = active && mCameraParameters.getMaxNumDetectedFaces() > 0;
        if (mIsCameraPreviewing)
            startCameraPreview();
    }

    protected final void setFaceRecognitionBoundColor(int color) {
        mCameraPreviewFaceDetection.setFaceBoundRectColor(color);
    }

    protected final void setFaceRecognitionBoundShape(int boundShape) {
        mCameraPreviewFaceDetection.setFaceBoundShape(boundShape);
    }

    protected final void setFaceRecognitionBoundLineSize(int boundLineSize) {
        mCameraPreviewFaceDetection.setFaceBoundLineSize(boundLineSize);
    }

    protected final void setImageResolution(int imageResolution) {
        mOutputImageResolution = Math.max(0, Math.min(imageResolution, 10)) / 10f;
    }

    protected final boolean isActive() {
        return mCamera != null;
    }

    private void setPreviewOrientation() {
        final int rotation = mDeviceDisplay.getRotation();
        setPreviewOrientation(rotation);
        mDeviceRotation = rotation;
    }

    protected final void setPreviewOrientation(int currentRotation) {
        if (!isActive()) return;

        int degrees = 0;
        switch (currentRotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mOriginalCameraOrientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mOriginalCameraOrientation - degrees + 360) % 360;
        }
        mCameraInfo.orientation = result;
        setCameraOutputSizes(result);
        mCamera.setDisplayOrientation(result);
    }

    protected final void startCameraPreview() {
        if (!isActive()) return;
        startCameraPreview(mTextureView.getSurfaceTexture());
    }

    protected final void startCameraPreview(SurfaceTexture surfaceTexture) {
        if (!isActive())
            return;
        mCamera.stopPreview();
        setCameraParameters();
        mCamera.setFaceDetectionListener(mCameraPreviewFaceDetection);

        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
            Log.e(FunababCameraPlugin.TAG, "startCameraPreview: " + e);
        }
        mCamera.startPreview();
        if (mPreviewFaceDetection) {
            mCamera.startFaceDetection();
        }

        mIsCameraPreviewing = true;
    }

    protected final Camera.Parameters getCameraParameters() {
        return mCameraParameters;
    }

    private Camera.Size getOptimalSize(List<Camera.Size> supportedSizes, Camera.Size cameraSize) {
        return getOptimalSize(supportedSizes, new Point(cameraSize.width, cameraSize.height));
    }

    private Camera.Size getOptimalSize(List<Camera.Size> supportedSizes, Point matchSize) {
        if (supportedSizes == null || matchSize == null)
            return null;

        final float ASPECT_RATIO_TOLERANCE = 0.1f;
//        final float targetRatio = (float)matchSize.x/matchSize.y;
        final float targetRatio = matchSize.x > matchSize.y ? (float)matchSize.x/matchSize.y : (float)matchSize.y/matchSize.x;

        Camera.Size optimalSize = null;
        float minimumDifference = Float.MAX_VALUE;
        int maximumHeight = 0;

        for (Camera.Size size : supportedSizes) {
            final float ratio = (float)size.width/size.height;
            final float difference = Math.abs(ratio - targetRatio);
            if (difference > ASPECT_RATIO_TOLERANCE || difference > minimumDifference) continue;
            if (maximumHeight > 0 && difference != minimumDifference)
                continue;
            optimalSize = size;
            minimumDifference = difference;
            if (difference == minimumDifference)
                maximumHeight = size.height;
        }

        if (optimalSize == null) {
            minimumDifference = Float.MAX_VALUE;
            for (Camera.Size size : supportedSizes) {
                final float ratio = (float)size.width/size.height;
                if (Math.abs(ratio - targetRatio) < minimumDifference) {
                    optimalSize = size;
                    minimumDifference = Math.abs(ratio - targetRatio);
                }
            }
        }

        return optimalSize;
    }

    protected final void destroyPreview() {
        if (isActive()) {
            mCamera.stopPreview();
            mCamera.release();
            mIsCameraPreviewing = false;
            mCamera = null;
            mDeviceRotation = -1;
            mCameraPreviewFaceDetection.reset();
            mCameraZoomGesture.reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        destroyPreview();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == View.VISIBLE){
            setCamera();
            startCameraPreview();
        } else {
            destroyPreview();
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    public void onActivityPause() {
        destroyPreview();
    }

    public void onActivityResume() {
        // TODO
        setCamera();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (isActive() && getVisibility() == View.VISIBLE) {
            setCamera();
            startCameraPreview(surface);
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        destroyPreview();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    protected final void onPreviewRectChanged() {
        if (!isActive()) return;
        final int rotation = mDeviceDisplay.getRotation();
        if (rotation != mDeviceRotation) {
            mCamera.stopPreview();
            setPreviewOrientation(rotation);
            mDeviceRotation = rotation;
            if (mIsCameraPreviewing)
                startCameraPreview();
        }
    }

    public interface TakePictureCallback {
        boolean onPicturePreTake(List<Rect> detectedFaces);
        void onPictureTaken(Bitmap bitmap, List<Rect> detectedFaces);
        void onError(Throwable e);
    }
}
