package org.godotengine.godot.funabab.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class CameraPreviewFaceDetection extends View implements Camera.FaceDetectionListener {

    private final static int MINIMUM_FACE_CONFIDENCE = 50;
    private final static float FACE_BOUNDS_RECT_STROKE_WIDTH = 5f;
    private final Matrix mTransformationMatrix = new Matrix();
    private final Paint mFaceBoundPaint = new Paint();

    private final List<Rect> mDetectedFaces = new ArrayList<>();

    public static final int FACE_BOUND_SHAPE_RECT = 0;
    public static final int FACE_BOUND_SHAPE_CIRCLE = 1;

    private int mBoundShape = FACE_BOUND_SHAPE_CIRCLE;
    private CameraPreview mCameraPreview;

    public CameraPreviewFaceDetection(Context context, int faceBoundRectColor, CameraPreview cameraPreview) {
        super(context);
        setWillNotDraw(false);

        mFaceBoundPaint.setStyle(Paint.Style.STROKE);
        mFaceBoundPaint.setStrokeWidth(FACE_BOUNDS_RECT_STROKE_WIDTH);
        mFaceBoundPaint.setColor(faceBoundRectColor);
        mFaceBoundPaint.setAlpha(faceBoundRectColor & 0x000000ff);
        mCameraPreview = cameraPreview;
    }

    public List<Rect> getDetectedFacesRect(Rect localBounds, PointF scalarRect) {
        if (localBounds == null) return mDetectedFaces;

        List<Rect> result = new ArrayList<>();
        for (Rect faceBounds : mDetectedFaces) {
            Rect rect = new Rect(faceBounds);
            rect.left -= localBounds.left;
            rect.top -= localBounds.top;

            if (Rect.intersects(rect, localBounds)) {
                if (scalarRect != null) {
                    rect.left = (int)(rect.left * scalarRect.x);
                    rect.top = (int)(rect.top * scalarRect.y);
                    rect.right =(int)(((faceBounds.right - faceBounds.left) * scalarRect.x) +  rect.left);
                    rect.bottom = (int)(((faceBounds.bottom - faceBounds.top) * scalarRect.y) +  rect.top);
                }
                result.add(rect);
            }
        }
        return result;
    }

    public void reset() {
        mDetectedFaces.clear();
        invalidate();
    }

    public void setFaceBoundRectColor(int color) {
        mFaceBoundPaint.setColor(color);
        mFaceBoundPaint.setAlpha(Color.alpha(color));
        invalidate();
    }

    public void setFaceBoundShape(int shape) {
        mBoundShape = shape;
        invalidate();
    }

    public void setFaceBoundLineSize(float lineSize) {
        mFaceBoundPaint.setStrokeWidth(lineSize);
        invalidate();
    }

    private void setTransformationMatrix(Camera.CameraInfo cameraInfo, int width, int height) {
        if (cameraInfo == null)
            return;

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams == null)
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Matrix matrix = new Matrix();
        matrix.setScale(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? -1 : 1, 1);
        matrix.postRotate(cameraInfo.orientation);
        matrix.postScale(width / 2000f, height / 2000f);
        matrix.postTranslate(width / 2f, height / 2f);

        mTransformationMatrix.set(matrix);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        for (Rect faceBounds : mDetectedFaces) {
            if (mBoundShape == FACE_BOUND_SHAPE_CIRCLE) {
                canvas.drawCircle(faceBounds.left + faceBounds.width() / 2f, faceBounds.top + faceBounds.height() / 2f, faceBounds.width() / 2f, mFaceBoundPaint);
            } else {
                canvas.drawRect(faceBounds, mFaceBoundPaint);
            }
        }
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        mDetectedFaces.clear();

        for (Camera.Face face : faces) {
            if (face.score < MINIMUM_FACE_CONFIDENCE)
                continue;
            RectF transformedFaceRect = new RectF(face.rect);
            mTransformationMatrix.mapRect(transformedFaceRect);

            Rect faceBounds = new Rect((int)transformedFaceRect.left, (int)transformedFaceRect.top,
                    (int)transformedFaceRect.right, (int)transformedFaceRect.bottom);

            mDetectedFaces.add(faceBounds);
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setTransformationMatrix(mCameraPreview.getCameraInfo(), w, h);
    }
}
