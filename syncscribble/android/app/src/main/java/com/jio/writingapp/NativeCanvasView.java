package com.jio.writingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Custom view for rendering the drawing canvas
 * This replaces the SDL surface with a native Android view
 */
public class NativeCanvasView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "NativeCanvasView";
    
    private SurfaceHolder mSurfaceHolder;
    private boolean mSurfaceReady = false;
    private Paint mPaint;
    
    // Native methods for surface integration
    private static native void jniSurfaceCreated(Object surface, int width, int height);
    private static native void jniSurfaceChanged(Object surface, int width, int height);
    private static native void jniSurfaceDestroyed();
    
    public NativeCanvasView(Context context) {
        super(context);
        init();
    }
    
    public NativeCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public NativeCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(2.0f);
        mPaint.setStyle(Paint.Style.STROKE);
        
        setFocusable(true);
        setFocusableInTouchMode(true);
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
        mSurfaceReady = true;
        
        // Get surface dimensions
        int width = getWidth();
        int height = getHeight();
        
        // Notify native code that surface is ready
        jniSurfaceCreated(holder.getSurface(), width, height);
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed: " + width + "x" + height);
        
        // Notify native code of surface changes
        jniSurfaceChanged(holder.getSurface(), width, height);
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
        mSurfaceReady = false;
        
        // Notify native code that surface is destroyed
        jniSurfaceDestroyed();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!mSurfaceReady) {
            // Draw placeholder content when surface is not ready
            canvas.drawColor(Color.WHITE);
            mPaint.setTextSize(48);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawText("Loading...", 50, 100, mPaint);
        }
    }
    
    /**
     * Request a redraw of the canvas
     * This can be called from native code via JNI
     */
    public void requestRedraw() {
        if (mSurfaceReady) {
            post(() -> invalidate());
        }
    }
    
    /**
     * Get the native surface for direct drawing
     * This can be called from native code via JNI
     */
    public Object getNativeSurface() {
        return mSurfaceHolder != null ? mSurfaceHolder.getSurface() : null;
    }
    
    public boolean isSurfaceReady() {
        return mSurfaceReady;
    }
    
    // Static method for JNI to trigger redraw
    public static void triggerRedraw() {
        // This would be called from native code to request a redraw
        // Implementation would need to maintain a reference to the active canvas view
    }
}