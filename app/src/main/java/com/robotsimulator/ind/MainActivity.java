package com.robotsimulator.ind;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.robotsimulator.R;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    private ROSWebSocketClient webSocketClient;
    private CanvasView canvasView;
    private ImageButton btnForward, btnReverse, btnLeft1, btnRight1, btnStop;
    private TextView txtDecision, tvSignalStatus;
    private Switch switchMode;
    boolean flag;
    private Handler handler = new Handler();
    private Runnable sendStateRunnable;
    private Button btnResetCanvas;
    private ImageView imgSignal;
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private AlertDialog connectionDialog;
    private TextView connectionStatusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        initializeViews();
        setupConnectionStatusView();
        initializeWebSocket();
        setupListeners();
    }

    private void initializeViews() {
        txtDecision = findViewById(R.id.txtDecision);
        canvasView = findViewById(R.id.canvasView);
        switchMode = findViewById(R.id.switchMode);
        tvSignalStatus = findViewById(R.id.tvSignalStatus);
        imgSignal = findViewById(R.id.imgSignal);

        btnForward = findViewById(R.id.btnForward);
        btnReverse = findViewById(R.id.btnReverse);
        btnLeft1 = findViewById(R.id.btnLeft);
        btnRight1 = findViewById(R.id.btnRight);
        btnStop = findViewById(R.id.btnStop);
        btnResetCanvas = findViewById(R.id.btnResetCanvas);

        // Deshabilitar controles inicialmente
        setControlsEnabled(false);
    }

    private void setupConnectionStatusView() {
        connectionStatusView = findViewById(R.id.connectionStatusView);
        setupListeners();
        if (connectionStatusView == null) {
            Log.e("MainActivity", "ConnectionStatusView not found in layout");
            return;
        }
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
            setupListeners();
        }
    }

    private void initializeWebSocket() {
        setupListeners();
        try {
            webSocketClient = new ROSWebSocketClient(
                    "ws://192.168.1.75:9090",
                    this,
                    findViewById(R.id.txtDistance),
                    findViewById(R.id.txtCenter),
                    findViewById(R.id.txtRight),
                    findViewById(R.id.txtLeft),
                    findViewById(R.id.txtDecision),
                    tvSignalStatus,
                    imgSignal
            );
            webSocketClient.setConnectionCallback(new ROSWebSocketClient.ConnectionCallback() {

                @Override
                public void onConnectionSuccess() {
                    runOnUiThread(() -> {
                        hideConnectionProgress();  // Añadir esta línea
                        hideConnectionError();
                        setControlsEnabled(true);
                        isConnecting = false;  // Añadir esta línea
                        Toast.makeText(MainActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionError(Exception e) {
                    runOnUiThread(() -> {
                        hideConnectionProgress();
                        isConnecting = false;
                        showConnectionError();
                        setControlsEnabled(true);
                    });
                }

                @Override
                public void onConnectionClosed() {
                    runOnUiThread(() -> {
                        showConnectionError();
                        setControlsEnabled(true);
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

            // Añadir un timeout para el diálogo de conexión
            handler.postDelayed(() -> {
                if (isConnecting) {
                    hideConnectionProgress();
                    isConnecting = false;
                    showConnectionError();
                }
            }, 10000); // 10 segundos de timeout
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
        runOnUiThread(() -> {
            if (connectionDialog != null && connectionDialog.isShowing()) {
                try {
                    connectionDialog.dismiss();
                } catch (Exception e) {
                    Log.e("MainActivity", "Error dismissing dialog: " + e.getMessage());
                }
            }
            isConnecting = false;
        });
    }

    private void showConnectionError() {
        runOnUiThread(() -> {
            if (connectionStatusView != null) {
                connectionStatusView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideConnectionError() {
        runOnUiThread(() -> {
            if (connectionStatusView != null) {
                connectionStatusView.setVisibility(View.GONE);
            }
        });
    }
    private void setControlsEnabled(boolean enabled) {
        btnForward.setEnabled(true);
        btnReverse.setEnabled(true);
        btnLeft1.setEnabled(true);
        btnRight1.setEnabled(true);
        btnStop.setEnabled(true);
        btnResetCanvas.setEnabled(true);
        switchMode.setEnabled(true);
    }

    private void setupListeners() {
        txtDecision.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentText = s.toString();
                if (flag) {
                    updateCanvasDirection(currentText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnResetCanvas.setOnClickListener(v -> canvasView.resetCanvas());

        btnForward.setOnClickListener(v -> {

                canvasView.setDirection("up");
                sendDecisionMessage("Recto");

        });

        btnReverse.setOnClickListener(v -> {
                canvasView.setDirection("down");
                sendDecisionMessage("Reversa");

        });

        btnLeft1.setOnClickListener(v -> {
                canvasView.setDirection("left");
                sendDecisionMessage("Izquierda");

        });

        btnRight1.setOnClickListener(v -> {
                canvasView.setDirection("right");
                sendDecisionMessage("Derecha");

        });

        btnStop.setOnClickListener(v -> {
                canvasView.stopDrawing();
                sendDecisionMessage("Detenerse");

        });

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (webSocketClient != null && webSocketClient.isConnected()) {
                setButtonsEnabled(!isChecked);
                flag = isChecked;
                canvasView.stopDrawing();
                String estado = flag ? "aut" : "man";
                String message = "{ \"op\": \"publish\", \"topic\": \"/control\", \"msg\": { \"data\": \"" + estado + "\" } }";
                webSocketClient.send(message);
            } else {
                switchMode.setChecked(!isChecked);
                Toast.makeText(this, "No connection to the server", Toast.LENGTH_SHORT).show();


            }
        });
    }

    private void sendDecisionMessage(String decision) {
        if (webSocketClient != null && webSocketClient.isConnected()) {
            String message = "{ \"op\": \"publish\", \"topic\": \"/decisiones_pub\", \"msg\": { \"data\": \"" + decision + "\" } }";
            webSocketClient.send(message);
        }else{
            Toast.makeText(this, "Connection not available. Local action: " + decision, Toast.LENGTH_SHORT).show();

        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (webSocketClient != null && webSocketClient.isConnected()) {
            btnForward.setEnabled(enabled);
            btnReverse.setEnabled(enabled);
            btnLeft1.setEnabled(enabled);
            btnRight1.setEnabled(enabled);
            btnStop.setEnabled(enabled);
        }
    }

    private void updateCanvasDirection(String text) {
        if (text.contains("Recto")) {
            canvasView.setDirection("up");
        } else if (text.contains("Derecha")) {
            canvasView.setDirection("right");
        } else if (text.contains("Izquierda")) {
            canvasView.setDirection("left");
        } else if (text.contains("Reversa")) {
            canvasView.setDirection("down");
        } else {
            canvasView.stopDrawing();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && sendStateRunnable != null) {
            handler.removeCallbacks(sendStateRunnable);
        }
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webSocketClient != null && !webSocketClient.isConnected()) {
            reconnectAttempts = 0;
            connectToWebSocket();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideConnectionProgress();
    }
}