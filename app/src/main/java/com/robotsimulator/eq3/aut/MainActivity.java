package com.robotsimulator.eq3.aut;

import android.app.AlertDialog;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.robotsimulator.R;

import java.net.URISyntaxException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Variables OpenGL
    private GLSurfaceView glSurfaceView;
    private GameRenderer renderer;
    private TextView coordsText;
    private boolean isRotating = false;
    private float previousX, previousY;
    private static final float TOUCH_SCALE_FACTOR = 0.5f;

    // Variables ROS
    private ROSWebSocketClient webSocketClient;
    private TextView txtDecision, tvSignalStatus;
    private Switch switchMode;
    private ImageView imgSignal;
    private boolean flag;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private AlertDialog connectionDialog;
    private TextView connectionStatusView;

    // Lista de estados posibles
    private final String[] states = {
            "Recto",
            "Derecha",
            "Detenerse"
    };

    // Colores para cada estado
    private final int[] stateColors = {
            Color.GREEN,     // Recto
            Color.RED,       // Reversa
            Color.BLUE,      // Izquierda
            Color.YELLOW,    // Derecha
            Color.GRAY       // Detenerse
    };

    private ImageButton btnStart;
    private int currentStateIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        // Inicialización OpenGL
        initializeOpenGL();
        setupGLSurfaceViewTouchListener();

        // Inicialización ROS
        initializeROSViews();
        setupConnectionStatusView();
        initializeWebSocket();
        setupListeners();
    }

    private void initializeOpenGL() {
        String mode = getIntent().getStringExtra("mode");
        ArrayList<Float> pathPoints = null;
        if ("manual".equals(mode)) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.containsKey("pathPoints")) {
                pathPoints = (ArrayList<Float>) extras.getSerializable("pathPoints");
            }
        }

        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(3);
        renderer = new GameRenderer(this, pathPoints);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void initializeROSViews() {
        txtDecision = findViewById(R.id.txtDecision);
        switchMode = findViewById(R.id.switchMode);  // Añadir esta línea

        // Iniciar en modo automático
        if (switchMode != null) {
            switchMode.setChecked(true);
            flag = true;
        }
    }

    private void setupConnectionStatusView() {
        connectionStatusView = findViewById(R.id.connectionStatusView);
        if (connectionStatusView == null) {
            Log.e("MainActivity", "ConnectionStatusView not found in layout");
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
                    findViewById(R.id.txtDecision),
                    renderer
            );
            webSocketClient.setConnectionCallback(new ROSWebSocketClient.ConnectionCallback() {
                @Override
                public void onConnectionSuccess() {
                    runOnUiThread(() -> {
                        hideConnectionProgress();
                        hideConnectionError();
                        setControlsEnabled(true);
                        isConnecting = false;
                        Toast.makeText(MainActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionError(Exception e) {
                    runOnUiThread(() -> {
                        hideConnectionProgress();
                        isConnecting = false;
                        showConnectionError();
                        setControlsEnabled(false);
                    });
                }

                @Override
                public void onConnectionClosed() {
                    runOnUiThread(() -> {
                        showConnectionError();
                        setControlsEnabled(false);
                    });
                }
            });

            connectToWebSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            showConnectionError();
        }
    }

    private void setupListeners() {
        // Inicializar el TextView para las decisiones
        txtDecision = findViewById(R.id.txtDecision);

        // Inicializar y configurar el botón de inicio
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            // Obtener el estado actual
            String currentState = states[currentStateIndex];
            int currentColor = stateColors[currentStateIndex];

            // Actualizar el renderer y la UI
            renderer.updateDecision(currentState);
            txtDecision.setText("Decision: " + currentState);
            btnStart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(currentColor));

            // Avanzar al siguiente estado
            currentStateIndex = (currentStateIndex + 1) % states.length;
        });

        // Configuración del botón del carro
        ImageButton btnCar = findViewById(R.id.btnCar);
        btnCar.setOnClickListener(v -> {
            renderer.toggleCarPlacementMode();
            btnCar.setSelected(renderer.isCarPlacementMode());
            Toast.makeText(this,
                    renderer.isCarPlacementMode() ? "Tap on the track to place the car" : "Placement mode disabled",
                    Toast.LENGTH_SHORT).show();
        });
    }



    // Métodos de conexión ROS
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
                }
            }, 10000);
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
            connectionDialog.dismiss();
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

    private void setControlsEnabled(boolean enabled) {
        findViewById(R.id.btnStart).setEnabled(enabled);
        findViewById(R.id.btnCar).setEnabled(enabled);
        switchMode.setEnabled(enabled);
    }

    // Lifecycle methods
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
        /*if (webSocketClient != null && !webSocketClient.isConnected()) {
            reconnectAttempts = 0;
            connectToWebSocket();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        /*if (webSocketClient != null) {
            webSocketClient.close();
        }*/
    }

    // Touch handling for OpenGL
    private void setupGLSurfaceViewTouchListener() {
        glSurfaceView.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (renderer.isCarPlacementMode()) {
                        float[] worldCoords = renderer.screenToWorldCoordinates(x, y);
                        if (worldCoords != null) {
                            renderer.placeCarAt(worldCoords[0], worldCoords[1], worldCoords[2]);
                            Toast.makeText(this, "Placing car in: " + worldCoords[0] + ", " + worldCoords[2], Toast.LENGTH_SHORT).show();
                        }
                    }
                    isRotating = true;
                    previousX = x;
                    previousY = y;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!renderer.isCarPlacementMode() && isRotating) {
                        float dx = x - previousX;
                        float dy = y - previousY;
                        renderer.updateCameraRotation(dx * TOUCH_SCALE_FACTOR, dy * TOUCH_SCALE_FACTOR);
                    }
                    previousX = x;
                    previousY = y;
                    break;

                case MotionEvent.ACTION_UP:
                    isRotating = false;
                    break;
            }
            return true;
        });
    }


}