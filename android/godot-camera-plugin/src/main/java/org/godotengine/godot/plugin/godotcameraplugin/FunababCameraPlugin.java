package org.godotengine.godot.plugin.godotcameraplugin;

import android.app.Activity;
import android.content.Context;

import org.godotengine.godot.Godot;
import org.godotengine.godot.GodotLib;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class FunababCameraPlugin extends GodotPlugin {

    private Godot mGodot;
    private Context mContext;
    private Integer mInstanceId = null;
    private FrameLayout mLayout = null;

    public static final String TAG = FunababCameraPlugin.class.getSimpleName();
    private static final int ERROR_CAMERA_NONE = 0;
    private static final int ERROR_CAMERA_FATAL = 1;
    private static final int ERROR_CAMERA_OUT_OF_MEMORY = 2;
    private static final int ERROR_CAMERA_MINIMUM_NUMBER_OF_FACE_NOT_DETECTED = 3;

    private GodotCameraView mGodotCameraView;

	public boolean initializeView(final int instanceId, final boolean cameraFacingBack, final String parameters, final int x, final int y, final int w, final int h, final boolean visibility) {
	    if (mInstanceId != null) {
            Log.d(TAG, "initializeView: can only instantiate one view at a time!");
            return false;
        }

	    mInstanceId = instanceId;
	    mGodot.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                GodotCameraView godotCameraView;
                try {
                    godotCameraView = GodotCameraView.initializeView(mContext, ParameterSerializer.unSerialize(parameters),
                            cameraFacingBack ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT,
                                new Rect(x, y, x + w, y + h), visibility);
                } catch (Exception e) {
                    Log.e(TAG, "failed to create camera preview: " + e.getMessage());
                    return;
                }

                mGodotCameraView = godotCameraView;
                mLayout.addView(mGodotCameraView);

                GodotLib.calldeferred(instanceId, "_set_camera_features_", new Object[]{
                        ParameterSerializer.serialize(mGodotCameraView.getCameraFeatures())
                });
            }
        });
	    return true;
    }

    @Override
    public View onMainCreate(Activity activity) {
	    mLayout = new FrameLayout(activity);
	    return mLayout;
    }

    public void resizeView(int instanceId, final int x, final int y, final int w, final int h) {
	    if (!sanityCheck(instanceId))
	        return;

	    if (mGodotCameraView != null) {
            mGodot.runOnUiThread(new Runnable() {
                @Override
                    public void run() {
                    mGodotCameraView.setViewRect(new Rect(x, y, x + w, y + h));
                    }
                });
        }
    }
    
    public boolean sanityCheck(int instanceId) {
	    if (mInstanceId == null) {
            Log.d(TAG, "sanityCheck: view not instantiated");
            return false;
        } else if (mInstanceId != instanceId) {
            Log.d(TAG, "sanityCheck: methods should only be called by instance script");
            return false;
        } else {
	        return true;
        }
    }

    public void destroyView(int instanceId) {
	    if (!sanityCheck(instanceId))
	        return;
	    mGodot.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _destroyView();
            }
        });
    }

    private void _destroyView() {
	    if (mGodotCameraView != null) {
	        mLayout.removeView(mGodotCameraView);
	        mInstanceId = null;
        }
    }

    private void setViewVisibility(int instanceId, final boolean is_visible) {
	    if (!sanityCheck(instanceId))
	        return;

	    if (mGodotCameraView != null)
	        mGodot.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGodotCameraView.setVisibility(is_visible ? View.VISIBLE : View.INVISIBLE);
                }
            });
    }

    private void setViewParameterInt(int instanceId, String parameterKey, int parameterValue) {
	    _setViewParameter(instanceId, parameterKey, parameterValue);
    }

    private void setViewParameterBool(int instanceId, String parameterKey, boolean parameterValue) {
        _setViewParameter(instanceId, parameterKey, parameterValue);
    }

    private void setViewParameterString(int instanceId, String parameterKey, String parameterValue) {
        _setViewParameter(instanceId, parameterKey, parameterValue);
    }

    private void _setViewParameter(int instanceId, final String parameterKey, final Object parameterValue) {
        if (!sanityCheck(instanceId))
            return;

	    if (mGodotCameraView != null)
	        mGodot.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGodotCameraView.setViewCameraParameterValue(parameterKey, parameterValue);
                }
            });
    }

    private void setPreviewCameraFacing(final int instanceId, final boolean isBackFacing) {
	    if (!sanityCheck(instanceId))
	        return;

	    if (mGodotCameraView != null)
	        mGodot.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final  HashMap<String, Object> cameraFeatures = mGodotCameraView.setViewCameraFacing(isBackFacing);
                    if (cameraFeatures != null)
                        GodotLib.calldeferred(instanceId, "_set_camera_features_", new Object[] {
                                ParameterSerializer.serialize(cameraFeatures)
                        });
                }
            });
    }

    private void takePicture(final int instanceId, final int minimumNumberOfFace) {
	    if (!sanityCheck(instanceId))
	        return;

	    if (mGodotCameraView != null) {
	        mGodot.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGodotCameraView.takePicture(new GodotCameraView.TakePictureCallback() {

                        @Override
                        public boolean onPicturePreTake(List<Rect> detectedFaces) {
                            if (detectedFaces.size() < minimumNumberOfFace) {
                                GodotLib.calldeferred(instanceId, "_on_picture_taken_", new Object[] {
                                        ERROR_CAMERA_MINIMUM_NUMBER_OF_FACE_NOT_DETECTED, "", ""
                                });
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public void onPictureTaken(Bitmap bitmap, List<Rect> detectedFaces) {

                            final HashMap<String, Object> detectedFacesExtra = new HashMap<>();
                            int faceCount = 0;
                            for (Rect rect : detectedFaces) {
                                faceCount++;
                                detectedFacesExtra.put("face" + faceCount, rect);
                            }

                            final ByteArrayOutputStream out = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                            try {
                                GodotLib.calldeferred(instanceId, "_on_picture_taken_", new Object[]{
                                        ERROR_CAMERA_NONE, out.toByteArray(), ParameterSerializer.serialize(detectedFacesExtra)
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "onPictureTaken: Error -> " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            GodotLib.calldeferred(instanceId, "_on_picture_taken_", new Object[] {
                                    e instanceof OutOfMemoryError ? ERROR_CAMERA_OUT_OF_MEMORY : ERROR_CAMERA_FATAL, "", ""
                            });
                        }
                    });
                }
            });
        }
    }

    private void refreshCameraPreview(final int instanceId) {
	    if (!sanityCheck(instanceId))
	        return;

	    if (mGodotCameraView != null) {
	        mGodot.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGodotCameraView.refreshPreview();
                }
            });
        }
    }
	
	public FunababCameraPlugin(Godot godot) {
		super(godot);
		
        mGodot = godot;
        mContext = godot.getContext();
    }
	
	@NonNull
	@Override
	public String getPluginName() {
		return "FunababCameraPlugin";
	}

	@NonNull
	@Override
	public List<String> getPluginMethods() {
		return Arrays.asList("initializeView",
                    "resizeView",
                    "destroyView",
                    "setViewVisibility",
                    "setViewParameterInt",
                    "setViewParameterBool",
                    "setViewParameterString",
                    "setPreviewCameraFacing",
                    "takePicture",
                    "refreshCameraPreview");
	}

	@NonNull
	@Override
	public Set<SignalInfo> getPluginSignals() {
		Set<SignalInfo> signals = new ArraySet<>();

		return signals;
	}

    @Override
    public void onMainPause() {
	    if (mGodotCameraView != null)
	        mGodotCameraView.onActivityPause();
    }

    @Override
    public void onMainResume() {
        if (mGodotCameraView != null)
            mGodotCameraView.onActivityResume();
    }

    @Override
    public void onMainDestroy() {
        _destroyView();
	}
}
