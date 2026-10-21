package com.cheat.overlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.SurfaceView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.DataOutputStream;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private SurfaceView surfaceView;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Extrai o daemon da pasta lib do Android e roda via SU
        new Thread(() -> {
            try {
                // O AGP empacota o executável como libff_daemon.so
                File daemonSrc = new File(getApplicationInfo().nativeLibraryDir, "libff_daemon.so");
                File daemonDst = new File("/data/local/tmp/ff_daemon");
                
                if (daemonSrc.exists()) {
                    InputStream is = new java.io.FileInputStream(daemonSrc);
                    FileOutputStream fos = new FileOutputStream(daemonDst);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    fos.close();
                    is.close();
                }
                
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                os.writeBytes("chmod 755 /data/local/tmp/ff_daemon\n");
                os.writeBytes("/data/local/tmp/ff_daemon > /dev/null 2>&1 &\n");
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (surfaceView != null) windowManager.removeView(surfaceView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
