package com.cheat.overlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.SurfaceView;

public class OverlayService extends Service {
    static {
        System.loadLibrary("cheat");
    }

    public native void StartCheat();
    public native void StopCheat();

    private WindowManager windowManager;
    private SurfaceView surfaceView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        surfaceView = new SurfaceView(this);
        surfaceView.setZOrderOnTop(true);
        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(surfaceView, params);

        StartCheat();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (surfaceView != null) windowManager.removeView(surfaceView);
        StopCheat();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
