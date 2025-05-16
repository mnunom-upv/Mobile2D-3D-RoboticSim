package com.robotsimulator.eq3.manual;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.robotsimulator.R;

import java.net.URISyntaxException;

public class ManualActivity extends AppCompatActivity {
    private static final String TAG = "ManualActivity";
    private GLSurfaceView glSurfaceView;
    private ManualRenderer renderer;
    private ROSWebSocketClient webSocketClient;

    // Variables para ROS
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 0;
    private AlertDialog connectionDialog;
    private TextView connectionStatusView;
    private Handler handler = new Handler();

    // Variables para el control táctil
    private float previousX;
    private float previousY;
    private Trail clear;
    private static final float TOUCH_SCALE_FACTOR = 0.5f;
    private boolean isCameraMoving = false;
    private TextView txtDecision;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        setupConnectionStatusView();
        initializeWebSocket();
        setupGLView();
        setupControls();
    }

    private void setupConnectionStatusView() {
        txtDecision = findViewById(R.id.txtDecision);
        connectionStatusView = findViewById(R.id.connectionStatusView);

        if (connectionStatusView == null) {
            TextView statusView = new TextView(this);
            statusView.setId(View.generateViewId());
            statusView.setTextColor(getResources().getColor(android.R.color.white));
            statusView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            statusView.setPadding(16, 8, 16, 8);
            statusView.setText("No connection to server");
            statusView.setVisibility(View.GONE);
            ((android.view.ViewGroup) findViewById(android.R.id.content)).addView(statusView);
            connectionStatusView = statusView;
        }
    }

    private void initializeWebSocket() {
        try {
            webSocketClient = new ROSWebSocketClient(
                    "ws://192.168.1.75:9090",
                    this,
                    findViewById(R.id.txtDecision)
                    );
            webSocketClient.setConnectionCallback(new ROSWebSocketClient.ConnectionCallback() {
                @Override
                public void onConnectionSuccess() {
                    runOnUiThread(() -> {
                        hideConnectionProgress();
                        hideConnectionError();
                        setControlsEnabled(true);
                        isConnecting = false;
                        Toast.makeText(ManualActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionError(Exception e) {
                    runOnUiThread(() -> {
                        hideConnectionProgress();
                        isConnecting = false;
                        showConnectionError();
                        setControlsEnabled(false);
                        if (!isFinishing()) {
                            attemptReconnection();
                        }
                    });
                }

                @Override
                public void onConnectionClosed() {
                    runOnUiThread(() -> {
                        showConnectionError();
                        setControlsEnabled(false);
                        if (!isFinishing()) {
                            attemptReconnection();
                        }
                    });
                }
            });

            connectToWebSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            showConnectionError();
        }
    }

    private void connectToWebSocket() {
        if (!isConnecting && (webSocketClient == null || !webSocketClient.isConnected())) {
            isConnecting = true;
            showConnectionProgress();
            webSocketClient.connect();

            handler.postDelayed(() -> {
                if (isConnecting) {
                    hideConnectionProgress();
                    isConnecting = false;
                    showConnectionError();
                    if (!isFinishing()) {
                        attemptReconnection();
                    }
                }
            }, 10000);
        }
    }

    private void attemptReconnection() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            handler.postDelayed(() -> {
                if (!isFinishing()) {
                    showReconnectionDialog();
                    connectToWebSocket();
                }
            }, 5000);
        }
    }

    private void showConnectionProgress() {
        if (connectionDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Connecting");
            builder.setMessage("Trying to connect to server...");
            builder.setCancelable(false);
            connectionDialog = builder.create();
        }
        connectionDialog.show();
    }

    private void hideConnectionProgress() {
        if (connectionDialog != null && connectionDialog.isShowing()) {
            try {
                connectionDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog: " + e.getMessage());
            }
        }
        isConnecting = false;
    }

    private void showConnectionError() {
        if (connectionStatusView != null) {
            connectionStatusView.setVisibility(View.VISIBLE);
        }
    }

    private void hideConnectionError() {
        if (connectionStatusView != null) {
            connectionStatusView.setVisibility(View.GONE);
        }
    }

    private void showReconnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reconnecting");
        builder.setMessage("Tried " + reconnectAttempts + " de " + MAX_RECONNECT_ATTEMPTS);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
        handler.postDelayed(dialog::dismiss, 2000);
    }


    private void setControlsEnabled(boolean enabled) {
        // Habilitar siempre los controles, sin importar el estado de conexión
        findViewById(R.id.btnForward).setEnabled(true);
        findViewById(R.id.bntBackward).setEnabled(true);
        findViewById(R.id.btnLeft).setEnabled(true);
        findViewById(R.id.btnRight).setEnabled(true);
        findViewById(R.id.btnReset).setEnabled(true);
    }

    private void setupGLView() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(3);
        renderer = new ManualRenderer(this);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        glSurfaceView.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    previousX = x;
                    previousY = y;
                    isCameraMoving = true;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isCameraMoving) {
                        float dx = (x - previousX) * TOUCH_SCALE_FACTOR;
                        float dy = (y - previousY) * TOUCH_SCALE_FACTOR;
                        renderer.updateCamera(dx, -dy);
                        previousX = x;
                        previousY = y;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    isCameraMoving = false;
                    break;
            }
            return true;
        });
    }

    private void setupControls() {
        ImageButton btnForward = findViewById(R.id.btnForward);
        ImageButton btnBackward = findViewById(R.id.bntBackward);
        ImageButton btnLeft = findViewById(R.id.btnLeft);
        ImageButton btnRight = findViewById(R.id.btnRight);
        Button btnReset = findViewById(R.id.btnReset);

        View.OnTouchListener moveListener = (v, event) -> {
            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (v.getId() == R.id.btnForward) {
                            renderer.startMovement( MovementDirection.BACKWARD);
                            sendROSMessage("Recto");
                        } else if (v.getId() == R.id.bntBackward) {
                            renderer.startMovement(MovementDirection.FORWARD);
                            sendROSMessage("Reversa");
                        } else if (v.getId() == R.id.btnLeft) {
                            renderer.startMovement(MovementDirection.LEFT);
                            sendROSMessage("Izquierda");
                        } else if (v.getId() == R.id.btnRight) {
                            renderer.startMovement(MovementDirection.RIGHT);
                            sendROSMessage("Derecha");
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        renderer.stopMovement();
                        sendROSMessage("Detenerse");
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en control de movimiento", e);
            }
            return true;
        };

        btnForward.setOnTouchListener(moveListener);
        btnBackward.setOnTouchListener(moveListener);
        btnLeft.setOnTouchListener(moveListener);
        btnRight.setOnTouchListener(moveListener);
        btnReset.setOnClickListener(v -> {
            clear.points.clear();
        });
    }

    private void sendROSMessage(String command) {
        if (webSocketClient != null && webSocketClient.isConnected()) {
            String message = "{ \"op\": \"publish\", \"topic\": \"/decisiones_pub\", \"msg\": { \"data\": \"" + command + "\" } }";
            webSocketClient.send(message);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        hideConnectionProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        if (webSocketClient != null && !webSocketClient.isConnected()) {
            reconnectAttempts = 0;
            connectToWebSocket();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}