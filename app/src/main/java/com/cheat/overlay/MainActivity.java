package com.cheat.overlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        } else {
            pedirRootEIniciar();
        }
    }

    private void pedirRootEIniciar() {
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                os.writeBytes("id\n");
                os.flush();
                // Lê a resposta do KernelSU
                byte[] buffer = new byte[1024];
                int read = su.getInputStream().read(buffer);
                if (read > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Root Concedido!", Toast.LENGTH_SHORT).show();
                        startService(new Intent(MainActivity.this, OverlayService.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "ROOT NEGADO!", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro no SU: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && Settings.canDrawOverlays(this)) {
            pedirRootEIniciar();
        }
    }
}
