package com.cheat.overlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class OverlayService extends Service implements SurfaceHolder.Callback {
    static { System.loadLibrary("cheat"); }

    public native void StartCheat();
    public native String GetCheatStatus();

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private RenderThread renderThread;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        surfaceView = new SurfaceView(this);
        surfaceView.setZOrderOnTop(true);
        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        surfaceView.getHolder().addCallback(this);

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
    public void surfaceCreated(SurfaceHolder holder) {
        renderThread = new RenderThread(holder);
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (renderThread != null) renderThread.running = false;
    }

    private class RenderThread extends Thread {
        private final SurfaceHolder holder;
        public boolean running = true;

        public RenderThread(SurfaceHolder holder) { this.holder = holder; }

        @Override
        public void run() {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.RED);
            paint.setTextSize(50);

            Paint boxPaint = new Paint();
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(3);
            boxPaint.setColor(Color.GREEN);

            while (running) {
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
                    
                    // Desenha o FOV (Aimbot)
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.RED);
                    canvas.drawCircle(canvas.getWidth() / 2f, canvas.getHeight() / 2f, 300, paint);

                    // Desenha o Painel
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.argb(200, 20, 20, 20)); // Fundo preto semi-transparente
                    canvas.drawRect(100, 200, 700, 800, paint);
                    
                    paint.setColor(Color.RED);
                    paint.setTextSize(60);
                    canvas.drawText("V CARRIER - PANEL", 120, 260, paint);
                    
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(40);
                    canvas.drawText("Status: " + GetCheatStatus(), 120, 350, paint);
                    canvas.drawText("Aimbot: ON (HS: 100%)", 120, 420, paint);
                    canvas.drawText("ESP Box: ON", 120, 490, paint);
                    canvas.drawText("ESP Skeleton: ON", 120, 560, paint);

                    // Exemplo de ESP Box (Desenhado no meio da tela pra teste)
                    canvas.drawRect(canvas.getWidth() / 2f - 100, canvas.getHeight() / 2f - 200, canvas.getWidth() / 2f + 100, canvas.getHeight() / 2f + 200, boxPaint);

                    holder.unlockCanvasAndPost(canvas);
                }
                try { Thread.sleep(16); } catch (Exception e) {}
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (surfaceView != null) windowManager.removeView(surfaceView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
