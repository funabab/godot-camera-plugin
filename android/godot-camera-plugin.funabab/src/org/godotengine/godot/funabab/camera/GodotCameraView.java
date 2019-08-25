package org.godotengine.godot.funabab.camera;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

public class GodotCameraView extends CameraPreview {

    private static final String FEATURE_FACE_RECOGNITION = "feature_face_recognition";
    private static final String FEATURE_FLASH_MODE = "feature_face_mode";
    private static final String FEATURE_CAMERA_ZOOM = "feature_camera_zoom";
    private static final String FEATURE_COLOR_EFFECT = "feature_color_effect";
    private static final String FEATURE_SCENE_MODE = "feature_scene_mode";
    private static final String FEATURE_WHITE_BALANCE = "feature_white_balance";


//    private static final String PARAMETER_IMAGE_QUALITY = "param_image_quality";
    private static final String PARAMETER_IMAGE_RESOLUTION = "param_image_resolution";
    private static final String PARAMETER_FLASH_MODE = "param_flash_mode";
//    private static final String PARAMETER_ENABLE_SHUTTER_SOUND = "param_enable_shutter_sound";
    private static final String PARAMETER_PITCH_TO_ZOOM = "param_pitch_to_zoom";
    private static final String PARAMETER_FACE_RECOGNITION = "param_face_recognition";
    private static final String PARAMETER_FACE_RECOGNITION_BOUND_COLOR = "param_face_recognition_bound_color";
    private static final String PARAMETER_FACE_RECOGNITION_BOUND_SHAPE = "param_face_recognition_bound_shape";
    private static final String PARAMETER_FACE_RECOGNITION_BOUND_LINE_SIZE = "param_face_recognition_bound_line_size";
    private static final String PARAMETER_SCENE_MODE = "param_scene_mode";
    private static final String PARAMETER_WHITE_BALANCE = "param_white_balance";
    private static final String PARAMETER_COLOR_EFFECT = "param_color_effect";

    private final HashMap<String, Object> mCameraViewParameters = new HashMap<>();

    public GodotCameraView(Context context, Camera camera, Camera.CameraInfo cameraInfo, Rect rect) {
        super(context, camera, cameraInfo);
        setX((float)rect.left);
        setY((float)rect.top);
        setLayoutParams(new ViewGroup.LayoutParams(rect.width(), rect.height()));
    }


    public void setViewRect(Rect rect) {
        setX((float)rect.left);
        setY((float)rect.top);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = rect.width();
        layoutParams.height = rect.height();
        setLayoutParams(layoutParams);
        onPreviewRectChanged();
    }

    public static GodotCameraView initializeView(Context context, HashMap<String, Object> parameters, int cameraFacing, Rect rect, boolean visible) throws Exception {
        final int cameraFacingId = getFacingCameraId(cameraFacing);
        final Camera camera = Camera.open(cameraFacingId);
        final GodotCameraView godotCameraView = new GodotCameraView(context, camera, getCameraInfo(cameraFacingId), rect);
        godotCameraView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        godotCameraView.setViewCameraParameters(parameters);
        return godotCameraView;
    }

    public HashMap<String, Object> setViewCameraFacing(boolean backFacing) {
        final boolean shouldRestartCameraPreview = isActive() && isCameraPreviewing();
        destroyPreview();
        final int cameraId = getFacingCameraId(backFacing ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
        Camera camera;
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            Log.e(FunababCameraPlugin.TAG, "setViewCameraFacing: " + e);
            return null;
        }

        setCamera(camera, getCameraInfo(cameraId));
        setViewCameraParameters();

        if (shouldRestartCameraPreview) {
            startCameraPreview();
        }
        return getCameraFeatures();
    }

    public void setViewCameraParameterValue(String key, Object value) {
        mCameraViewParameters.put(key, value);
        setViewCameraParameter(key, value);
    }

    private void setViewCameraParameters(HashMap<String, Object> parameters) {
        mCameraViewParameters.putAll(parameters);
        setViewCameraParameters();
    }

    private void setViewCameraParameters() {
        for (String key : mCameraViewParameters.keySet()) {
            setViewCameraParameter(key, mCameraViewParameters.get(key));
        }
    }

    public void setViewCameraParameter(String key, Object value) {
        switch (key) {
//            case PARAMETER_IMAGE_QUALITY:
//                setImageQuality(value);
//                break;
            case PARAMETER_IMAGE_RESOLUTION:
                setImageResolution(value);
                break;
            case PARAMETER_FLASH_MODE:
                setFlashMode(value);
                break;
//            case PARAMETER_ENABLE_SHUTTER_SOUND:
//                setEnableShutterSound(value);
//                break;
            case PARAMETER_SCENE_MODE:
                setSceneMode(value);
                break;
            case PARAMETER_WHITE_BALANCE:
                setWhiteBalance(value);
                break;
            case PARAMETER_FACE_RECOGNITION:
                setFaceRecognitionMode(value);
                break;
            case PARAMETER_FACE_RECOGNITION_BOUND_COLOR:
                setFaceRecognitionBoundColor(value);
                break;
            case PARAMETER_FACE_RECOGNITION_BOUND_SHAPE:
                setFaceRecognitionBoundShape(value);
                break;
            case PARAMETER_FACE_RECOGNITION_BOUND_LINE_SIZE:
                setFaceRecognitionBoundLineSize(value);
                break;
            case PARAMETER_PITCH_TO_ZOOM:
                setPinchToZoomMode(value);
                break;
            case PARAMETER_COLOR_EFFECT:
                setColorEffect(value);
                break;
        }
    }

    public HashMap<String, Object> getCameraFeatures() {
        if (!isActive())
            return null;

        final HashMap<String, Object> result = new HashMap<>();
        final Camera.Parameters cameraParameters = getCameraParameters();
        result.put(FEATURE_FACE_RECOGNITION, cameraParameters.getMaxNumDetectedFaces() > 0);
        result.put(FEATURE_FLASH_MODE, cameraParameters.getFlashMode() != null);
        result.put(FEATURE_CAMERA_ZOOM, cameraParameters.isZoomSupported());
        result.put(FEATURE_COLOR_EFFECT, cameraParameters.getColorEffect() != null);
        result.put(FEATURE_SCENE_MODE, cameraParameters.getSceneMode() != null);
        result.put(FEATURE_WHITE_BALANCE, cameraParameters.getWhiteBalance() != null);
        return result;
    }

//    private void setImageQuality(Object imageQuality) {
//        if (imageQuality instanceof Integer)
//            setImageQuality((int)imageQuality);
//    }

    private void setImageResolution(Object imageResolution) {
        if (imageResolution instanceof Integer)
            setImageResolution((int)imageResolution);
    }

    private void setFaceRecognitionMode(Object faceRecognitionMode) {
        if (faceRecognitionMode instanceof Boolean)
            setFaceRecognitionMode((boolean)faceRecognitionMode);
    }

    private void setFaceRecognitionBoundColor(Object color) {
        if (color instanceof String) {
            final int c = Color.parseColor("#" + color);
            setFaceRecognitionBoundColor(c);
        }
    }

    private void setFaceRecognitionBoundShape(Object boundShape) {
        if (boundShape instanceof Integer)
            setFaceRecognitionBoundShape((int)boundShape);
    }

    private void setFaceRecognitionBoundLineSize(Object boundLineSize) {
        if (boundLineSize instanceof Integer)
            setFaceRecognitionBoundLineSize((int)boundLineSize);
    }

    private void setPinchToZoomMode(Object pinchToZoomMode) {
        if (pinchToZoomMode instanceof Boolean)
            setPinchToZoomMode((boolean)pinchToZoomMode);
    }

    private static final int PARAMETER_VALUE_FLASH_MODE_OFF = 0;
    private static final int PARAMETER_VALUE_FLASH_MODE_AUTO = 1;
    private static final int PARAMETER_VALUE_FLASH_MODE_ON = 2;
    private static final int PARAMETER_VALUE_FLASH_MODE_RED_EYE = 3;
    private static final int PARAMETER_VALUE_FLASH_MODE_TORCH = 4;

    private void setFlashMode(Object flashMode) {
        if (flashMode instanceof Integer) {
            final int mode = (int)flashMode;
            switch (mode) {
                case PARAMETER_VALUE_FLASH_MODE_OFF:
                    setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    break;
                case PARAMETER_VALUE_FLASH_MODE_AUTO:
                    setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
                case PARAMETER_VALUE_FLASH_MODE_ON:
                    setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    break;
                case PARAMETER_VALUE_FLASH_MODE_RED_EYE:
                    setFlashMode(Camera.Parameters.FLASH_MODE_RED_EYE);
                    break;
                case PARAMETER_VALUE_FLASH_MODE_TORCH:
                    setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    break;
            }
        }
    }

//    private void setEnableShutterSound(Object mode) {
//        if (mode instanceof Boolean)
//            setEnableShutterSound((boolean)mode);
//    }

    private static final int PARAMETER_VALUE_COLOR_EFFECT_NONE = 0;
    private static final int PARAMETER_VALUE_COLOR_EFFECT_MONO = 1;
    private static final int PARAMETER_VALUE_COLOR_EFFECT_NEGATIVE = 2;
    private static final int PARAMETER_VALUE_COLOR_EFFECT_SOLARIZE = 3;
    private static final int PARAMETER_VALUE_COLOR_EFFECT_SEPIA = 4;

    private void setColorEffect(Object colorEffect) {
        if (colorEffect instanceof Integer) {
            final int mode = (int)colorEffect;
            switch (mode) {
                case PARAMETER_VALUE_COLOR_EFFECT_NONE:
                    setColorEffect(Camera.Parameters.EFFECT_NONE);
                    break;
                case PARAMETER_VALUE_COLOR_EFFECT_MONO:
                    setColorEffect(Camera.Parameters.EFFECT_MONO);
                    break;
                case PARAMETER_VALUE_COLOR_EFFECT_NEGATIVE:
                    setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                    break;
                case PARAMETER_VALUE_COLOR_EFFECT_SOLARIZE:
                    setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
                    break;
                case PARAMETER_VALUE_COLOR_EFFECT_SEPIA:
                    setColorEffect(Camera.Parameters.EFFECT_SEPIA);
                    break;
            }
        }
    }

    private static final int PARAMETER_VALUE_SCENE_MODE_AUTO = 0;
    private static final int PARAMETER_VALUE_SCENE_MODE_HDR = 1;
    private static final int PARAMETER_VALUE_SCENE_MODE_PORTRAIT = 2;
    private static final int PARAMETER_VALUE_SCENE_MODE_LANDSCAPE = 3;
    private static final int PARAMETER_VALUE_SCENE_MODE_NIGHT = 4;
    private static final int PARAMETER_VALUE_SCENE_MODE_SUNSET = 5;

    private void setSceneMode(Object sceneMode) {
        if (sceneMode instanceof Integer) {
            final int mode = (int)sceneMode;
            switch (mode) {
                case PARAMETER_VALUE_SCENE_MODE_AUTO:
                    setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                    break;
                case PARAMETER_VALUE_SCENE_MODE_HDR:
                    setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
                    break;
                case PARAMETER_VALUE_SCENE_MODE_PORTRAIT:
                    setSceneMode(Camera.Parameters.SCENE_MODE_PORTRAIT);
                    break;
                case PARAMETER_VALUE_SCENE_MODE_LANDSCAPE:
                    setSceneMode(Camera.Parameters.SCENE_MODE_LANDSCAPE);
                    break;
                case PARAMETER_VALUE_SCENE_MODE_NIGHT:
                    setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
                    break;
                case PARAMETER_VALUE_SCENE_MODE_SUNSET:
                    setSceneMode(Camera.Parameters.SCENE_MODE_SUNSET);
                    break;
            }
        }
    }

    private static final int PARAMETER_VALUE_WHITE_BALANCE_AUTO = 0;
    private static final int PARAMETER_VALUE_WHITE_BALANCE_INCANDESCENT = 1;
    private static final int PARAMETER_VALUE_WHITE_BALANCE_FLUORESCENT = 2;
    private static final int PARAMETER_VALUE_WHITE_BALANCE_DAYLIGHT = 3;
    private static final int PARAMETER_VALUE_WHITE_BALANCE_TWILIGHT = 4;
    private static final int PARAMETER_VALUE_WHITE_BALANCE_SHADE = 5;

    private void setWhiteBalance(Object whiteBalance) {
        if (whiteBalance instanceof Integer) {
            final int mode = (int)whiteBalance;
            switch (mode) {
                case PARAMETER_VALUE_WHITE_BALANCE_AUTO:
                    setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                    break;
                case PARAMETER_VALUE_WHITE_BALANCE_INCANDESCENT:
                    setWhiteBalance(Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
                    break;
                case PARAMETER_VALUE_WHITE_BALANCE_FLUORESCENT:
                    setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
                    break;
                case PARAMETER_VALUE_WHITE_BALANCE_DAYLIGHT:
                    setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
                    break;
                case PARAMETER_VALUE_WHITE_BALANCE_TWILIGHT:
                    setWhiteBalance(Camera.Parameters.WHITE_BALANCE_TWILIGHT);
                    break;
                case PARAMETER_VALUE_WHITE_BALANCE_SHADE:
                    setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
                    break;
            }
        }
    }
}
