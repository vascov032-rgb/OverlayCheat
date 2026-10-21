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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;

public class OverlayService extends Service {
    public static boolean esp_box = false;

    private WindowManager windowManager;
    private View espView;
    private LinearLayout menuView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createESP();
        createMenu();
    }

    private void createESP() {
        espView = new View(this) {
            private Paint boxPaint = new Paint();
            {
                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(6);
                boxPaint.setColor(Color.GREEN);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (esp_box) {
                    try {
                        // Le as coordenadas que o Daemon do Termux calculou
                        BufferedReader br = new BufferedReader(new FileReader("/sdcard/esp_coords.txt"));
                        String line = br.readLine();
                        br.close();
                        if (line != null) {
                            String[] parts = line.split(" ");
                            if (parts.length == 4) {
                                float x = Float.parseFloat(parts[0]);
                                float y = Float.parseFloat(parts[1]);
                                float w = Float.parseFloat(parts[2]);
                                float h = Float.parseFloat(parts[3]);
                                canvas.drawRect(x, y, x + w, y + h, boxPaint);
                            }
                        }
                    } catch (Exception e) {}
                }
                invalidate();
            }
        };

        int layoutType = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        windowManager.addView(espView, params);
    }

    private void createMenu() {
        menuView = new LinearLayout(this);
        menuView.setOrientation(LinearLayout.VERTICAL);
        menuView.setPadding(40, 40, 40, 40);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#DD111111"));
        shape.setCornerRadius(30);
        shape.setStroke(3, Color.RED);
        menuView.setBackground(shape);

        TextView title = new TextView(this);
        title.setText("V CARRIER");
        title.setTextColor(Color.RED);
        title.setTextSize(22);
        menuView.addView(title);

        Switch swEsp = new Switch(this);
        swEsp.setText("ESP Caixa Real");
        swEsp.setTextColor(Color.WHITE);
        swEsp.setOnCheckedChangeListener((v, c) -> esp_box = c);
        menuView.addView(swEsp);

        Button btnClose = new Button(this);
        btnClose.setText("FECHAR");
        btnClose.setOnClickListener(v -> stopSelf());
        menuView.addView(btnClose);

        int layoutType = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; params.y = 200;

        menuView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (menuView != null) windowManager.removeView(menuView);
        if (espView != null) windowManager.removeView(espView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
