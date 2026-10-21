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
    public native String GetCheatStatus();
    
    // Variáveis que o C++ vai ler
    public static boolean aimbot = false;
    public static boolean esp = false;
    public static boolean hs = false; // Mira na cabeça

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
        shape.setColor(Color.parseColor("#DD111111")); // Fundo escuro
        shape.setCornerRadius(20);
        shape.setStroke(3, Color.RED);
        menuView.setBackground(shape);

        TextView title = new TextView(this);
        title.setText("V CARRIER - EXTERNAL");
        title.setTextColor(Color.RED);
        title.setTextSize(20);
        menuView.addView(title);

        Switch swAimbot = new Switch(this);
        swAimbot.setText("Aimbot");
        swAimbot.setTextColor(Color.WHITE);
        swAimbot.setOnCheckedChangeListener((v, c) -> aimbot = c);
        menuView.addView(swAimbot);

        Switch swHS = new Switch(this);
        swHS.setText("Mira na Cabeça");
        swHS.setTextColor(Color.WHITE);
        swHS.setOnCheckedChangeListener((v, c) -> hs = c);
        menuView.addView(swHS);

        Switch swEsp = new Switch(this);
        swEsp.setText("ESP Caixa");
        swEsp.setTextColor(Color.WHITE);
        swEsp.setOnCheckedChangeListener((v, c) -> esp = c);
        menuView.addView(swEsp);

        Button btnClose = new Button(this);
        btnClose.setText("FECHAR");
        btnClose.setOnClickListener(v -> stopSelf());
        menuView.addView(btnClose);

        int layoutType;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        // Torna o menu arrastável
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

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | // Deixa os toques passarem pro jogo
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
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
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);

            while (running) {
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
                    if (esp) {
                        // Aqui a gente vai desenhar os inimigos no futuro
                        // Exemplo: Desenhar uma caixa no centro pra testar
                        canvas.drawRect(canvas.getWidth() / 2f - 100, canvas.getHeight() / 2f - 200, canvas.getWidth() / 2f + 100, canvas.getHeight() / 2f + 200, paint);
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
