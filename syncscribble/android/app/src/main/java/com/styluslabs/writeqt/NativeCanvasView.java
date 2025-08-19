package com.styluslabs.writeqt;

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
    
    private SurfaceHolder mHolder;
    private Paint mPaint;
    private boolean mSurfaceReady = false;
    
    // Native methods for drawing operations
    private static native void jniSurfaceCreated(Object surface, int width, int height);
    private static native void jniSurfaceChanged(Object surface, int width, int height);
    private static native void jniSurfaceDestroyed();
    private static native void jniDrawFrame();
    
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
        mHolder = getHolder();
        mHolder.addCallback(this);
        
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        
        // Enable drawing
        setWillNotDraw(false);
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
        
        // Notify native code
        jniSurfaceDestroyed();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!mSurfaceReady) {
            return;
        }
        
        // Clear the canvas with white background
        canvas.drawColor(Color.WHITE);
        
        // Trigger native drawing
        jniDrawFrame();
    }
    
    /**
     * Request a redraw of the canvas
     * This can be called from native code via JNI
     */
    public void requestRedraw() {
        if (mSurfaceReady) {
            post(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                }
            });
        }
    }
    
    /**
     * Get the native surface for direct drawing
     * This can be called from native code via JNI
     */
    public Object getNativeSurface() {
        return mHolder.getSurface();
    }
    
    public boolean isSurfaceReady() {
        return mSurfaceReady;
    }
}