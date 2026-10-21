package com.cheat.overlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class OverlayService extends Service implements SurfaceHolder.Callback {
    static { System.loadLibrary("cheat"); }

    public native void StartCheat();
    
    // Tudo começa DESLIGADO
    public static boolean aimbot = false;
    public static boolean esp_box = false;

    private WindowManager windowManager;
    private SurfaceView espView;
    private LinearLayout menuView;
    private RenderThread renderThread;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        createMenu();
        createESP();
        
        StartCheat();
    }

    private void createMenu() {
        menuView = new LinearLayout(this);
        menuView.setOrientation(LinearLayout.VERTICAL);
        menuView.setPadding(30, 30, 30, 30);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#DD111111")); // Fundo semi-transparente
        shape.setCornerRadius(20);
        shape.setStroke(3, Color.RED);
        menuView.setBackground(shape);

        TextView title = new TextView(this);
        title.setText("V CARRIER - EXTERNAL");
        title.setTextColor(Color.RED);
        title.setTextSize(20);
        menuView.addView(title);

        Switch swAimbot = new Switch(this);
        swAimbot.setText("Aimbot (Mira na Cabeça)");
        swAimbot.setTextColor(Color.WHITE);
        swAimbot.setOnCheckedChangeListener((v, c) -> aimbot = c);
        menuView.addView(swAimbot);

        Switch swEsp = new Switch(this);
        swEsp.setText("ESP Caixa");
        swEsp.setTextColor(Color.WHITE);
        swEsp.setOnCheckedChangeListener((v, c) -> esp_box = c);
        menuView.addView(swEsp);

        Button btnClose = new Button(this);
        btnClose.setText("FECHAR PAINEL");
        btnClose.setOnClickListener(v -> stopSelf());
        menuView.addView(btnClose);

        int layoutType;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // AQUI ESTÁ A CORREÇÃO: FLAG_NOT_TOUCH_MODAL deixa os toques passarem pro jogo fora do menu
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        // Arrastar o menu
        menuView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(menuView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(menuView, params);
    }

    private void createESP() {
        espView = new SurfaceView(this);
        espView.setZOrderOnTop(true);
        espView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        espView.getHolder().addCallback(this);

        int layoutType;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // AQUI ESTÁ A CORREÇÃO 2: FLAG_NOT_TOUCHABLE deixa o jogo receber os toques na tela toda
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        windowManager.addView(espView, params);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderThread = new RenderThread(holder);
        renderThread.running = true;
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
            Paint boxPaint = new Paint();
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(4);
            boxPaint.setColor(Color.GREEN);

            while (running) {
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
                    if (esp_box) {
                        // Desenha uma caixa no centro pra voce ver que ta funcionando
                        canvas.drawRect(canvas.getWidth() / 2f - 150, canvas.getHeight() / 2f - 250, canvas.getWidth() / 2f + 150, canvas.getHeight() / 2f + 250, boxPaint);
                    }
                    holder.unlockCanvasAndPost(canvas);
                }
                try { Thread.sleep(16); } catch (Exception e) {}
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (menuView != null) windowManager.removeView(menuView);
        if (espView != null) windowManager.removeView(espView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
