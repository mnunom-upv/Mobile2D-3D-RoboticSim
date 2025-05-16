package com.robotsimulator.eq2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.robotsimulator.R;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridLayout gridLayout;

    private static final String TAG = "MainActivity";


    private Button btnMoveForward, btnTurnLeft, btnTurnRight, btnObstacle;
    private ImageButton btnDelete, btnRefresh, btnPlay, btnHelp;
    private ImageView robot;
    private LinearLayout instructionsContainer;
    private TextView instructionsText;

    private List<String> instructions = new ArrayList<>();
    private Set<Integer> obstacles = new HashSet<>();
    private int currentPositionX = 0;
    private int currentPositionY = 0;
    private String direction = "UP";
    private boolean isExecuting = false;
    private boolean obstacleMode = false;
    private static final int CELL_SIZE = 30;
    private static final int ROBOT_SIZE = 20;

    private int currentInstructionIndex = -1;
    private boolean isAvoidingObstacle = false;
    private List<String> temporaryInstructions = new ArrayList<>();

    private Set<String> visitedPositions = new HashSet<>();

    private boolean isConnecting = false;

    private Handler handler = new Handler();

    private static final int MAX_DEPTH = 10; // Límite de profundidad para evitar bucles infinitos
    private TextView txtDecision;
    private TextView connectionStatusView;
    private AlertDialog connectionDialog;

    private ROSWebSocketClient webSocketClient;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        setupConnectionStatusView();
        initializeWebSocket();
        setupControls();
        // Inicializar vistas
        gridLayout = findViewById(R.id.gridLayout);
        btnMoveForward = findViewById(R.id.btn_move_forward);
        btnTurnLeft = findViewById(R.id.btn_turn_left);
        btnTurnRight = findViewById(R.id.btn_turn_right);
        btnDelete = findViewById(R.id.btn_delete);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnPlay = findViewById(R.id.btn_play);
        btnObstacle = findViewById(R.id.btn_obstacle);
        btnHelp = findViewById(R.id.btn_help);
        instructionsText = findViewById(R.id.instructions_text);

        initializeGrid();
        setupButtons();
        direction = "UP";
    }

    private void setupControls() {
        Button btnForward = findViewById(R.id.btn_move_forward);
        Button btnLeft = findViewById(R.id.btn_turn_left);
        Button btnRight = findViewById(R.id.btn_turn_right);

        View.OnClickListener moveListener = v -> {
            if (!isExecuting && webSocketClient != null && webSocketClient.isConnected()) {
                String command = "";
                if (v.getId() == R.id.btn_move_forward) {
                    command = "Recto";
                    instructions.add(command);
                } else if (v.getId() == R.id.btn_turn_left) {
                    command = "Izquierda";
                    instructions.add(command);
                } else if (v.getId() == R.id.btn_turn_right) {
                    command = "Derecha";
                    instructions.add(command);
                }


                if (!command.isEmpty()) {
                    webSocketClient.publishCommand(command);
                    updateInstructionsDisplay();
                }
            }
        };

        btnForward.setOnClickListener(moveListener);
        btnLeft.setOnClickListener(moveListener);
        btnRight.setOnClickListener(moveListener);
    }

    private void sendROSMessage(String command) {
        if (webSocketClient != null && webSocketClient.isConnected()) {
            String message = "{ \"op\": \"publish\", \"topic\": \"/decisiones_pub\", \"msg\": { \"data\": \"" + command + "\" } }";
            webSocketClient.send(message);
        }
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
    private void initializeWebSocket() {
        try {
            webSocketClient = new ROSWebSocketClient(
                    "ws://192.168.1.75:9090",  // Update with your ROS server address
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
                        Toast.makeText(MainActivity.this, "Connected to ROS", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionError(Exception e) {
                    runOnUiThread(() -> {
                        hideConnectionProgress();
                        showConnectionError();
                        setControlsEnabled(false);
                        Toast.makeText(MainActivity.this,
                                "Connection error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionClosed() {
                    runOnUiThread(() -> {
                        showConnectionError();
                        setControlsEnabled(false);
                        Toast.makeText(MainActivity.this,
                                "Connection closed",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });

            webSocketClient.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating WebSocket client", e);
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
    private void showReconnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reconnecting");
        builder.setMessage("Try " + reconnectAttempts + " de " + MAX_RECONNECT_ATTEMPTS);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
        handler.postDelayed(dialog::dismiss, 2000);
    }


    private void showConnectionError() {
        if (connectionStatusView != null) {
            connectionStatusView.setVisibility(View.VISIBLE);
        }
    }
    private void setControlsEnabled(boolean enabled) {
        // Habilitar siempre los controles, sin importar el estado de conexión
        findViewById(R.id.btn_play).setEnabled(true);
        findViewById(R.id.btn_delete).setEnabled(true);
        findViewById(R.id.btn_refresh).setEnabled(true);
        findViewById(R.id.btn_obstacle).setEnabled(true);
        findViewById(R.id.btn_move_forward).setEnabled(true);
        findViewById(R.id.btn_turn_left).setEnabled(true);
        findViewById(R.id.btn_turn_right).setEnabled(true);

    }



    private void hideConnectionError() {
        if (connectionStatusView != null) {
            connectionStatusView.setVisibility(View.GONE);
        }
    }



    private void initializeGrid() {
        gridLayout.setColumnCount(10);

        int cellSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CELL_SIZE,
                getResources().getDisplayMetrics()
        );

        int robotSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                ROBOT_SIZE,
                getResources().getDisplayMetrics()
        );

        // Crear el grid con celdas vacías
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                final int x = j;
                final int y = i;

                // Crear un LinearLayout como contenedor para cada celda
                LinearLayout cellContainer = new LinearLayout(this);
                cellContainer.setBackgroundResource(R.drawable.cell_border);

                cellContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (obstacleMode && !isExecuting) {
                            toggleObstacle(x, y);
                        }
                    }
                });

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSizePx;
                params.height = cellSizePx;
                params.setMargins(1, 1, 1, 1);
                cellContainer.setLayoutParams(params);
                gridLayout.addView(cellContainer);
            }
        }

        // Crear el robot separadamente
        robot = new ImageView(this);
        robot.setImageResource(R.drawable.robot_icon);
        GridLayout.LayoutParams robotParams = new GridLayout.LayoutParams();
        robotParams.width = robotSizePx;
        robotParams.height = robotSizePx;
        robot.setLayoutParams(robotParams);
        robot.setRotation(90);

        updateRobotPosition();
    }

    private void setupButtons() {
        btnMoveForward.setOnClickListener(v -> {
            if (!isExecuting) {
                instructions.add("Forward");
                updateInstructionsDisplay();
            }
        });

        btnTurnLeft.setOnClickListener(v -> {
            if (!isExecuting) {
                instructions.add("Left");
                updateInstructionsDisplay();
            }
        });

        btnTurnRight.setOnClickListener(v -> {
            if (!isExecuting) {
                instructions.add("Right");
                updateInstructionsDisplay();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (!instructions.isEmpty() && !isExecuting) {
                instructions.remove(instructions.size() - 1);
                updateInstructionsDisplay();
            }
        });

        btnRefresh.setOnClickListener(v -> resetEverything());

        btnPlay.setOnClickListener(v -> {
            if (!isExecuting && !instructions.isEmpty()) {
                currentPositionX = 0;
                currentPositionY = 0;
                direction = "UP";
                obstacleMode = false;
                btnObstacle.setEnabled(false);
                executeInstructions();
            }
        });

        btnHelp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
        });

        btnObstacle.setOnClickListener(v -> {
            if (!isExecuting) {
                obstacleMode = !obstacleMode;
                btnObstacle.setBackgroundColor(obstacleMode ?
                        ContextCompat.getColor(this, android.R.color.holo_orange_dark) :
                        ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            }
        });
    }


    private void updateInstructionsDisplay() {
        SpannableString spannableString = new SpannableString("");
        StringBuilder fullText = new StringBuilder();

        for (int i = 0; i < instructions.size(); i++) {
            String instructionText = (i + 1) + ". " + instructions.get(i) + "\n";
            fullText.append(instructionText);
        }

        spannableString = new SpannableString(fullText.toString());

        // Aplicar colores a las instrucciones
        int currentPosition = 0;
        for (int i = 0; i < instructions.size(); i++) {
            String instructionText = (i + 1) + ". " + instructions.get(i) + "\n";
            int endPosition = currentPosition + instructionText.length();

            int color;
            if (i < currentInstructionIndex) {
                // Instrucciones ya ejecutadas
                color = Color.GRAY;
            } else if (i == currentInstructionIndex) {
                // Instrucción actual
                color = Color.GREEN;
            } else {
                // Instrucciones pendientes
                color = Color.WHITE;
            }

            spannableString.setSpan(
                    new ForegroundColorSpan(color),
                    currentPosition,
                    endPosition - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            currentPosition = endPosition;
        }

        instructionsText.setText(spannableString);
    }

    private void resetEverything() {
        // Reset position to initial state (0,0)
        currentPositionX = 0;
        currentPositionY = 0;
        direction = "UP";
        instructions.clear();
        obstacles.clear();
        obstacleMode = false;
        btnObstacle.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        currentInstructionIndex = -1;
        updateInstructionsDisplay();
        // Limpiar obstáculos visualmente
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            LinearLayout container = (LinearLayout) gridLayout.getChildAt(i);
            container.removeAllViews();
        }

        updateInstructionsDisplay();
        updateRobotPosition();
        isExecuting = false;
        btnPlay.setEnabled(true);
        btnObstacle.setEnabled(true);
    }


    private void toggleObstacle(int x, int y) {
        int index = y * 10 + x;
        if (index != (currentPositionY * 10 + currentPositionX)) {
            LinearLayout cellContainer = (LinearLayout) gridLayout.getChildAt(index);

            // Verificar si ya existe un obstáculo
            if (obstacles.contains(index)) {
                obstacles.remove(index);
                cellContainer.removeAllViews(); // Eliminar la imagen del obstáculo
            } else {
                obstacles.add(index);
                ImageView obstacleView = new ImageView(this);
                obstacleView.setImageResource(R.drawable.cono);
                obstacleView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                ));
                cellContainer.addView(obstacleView);
            }
        }
    }
    private boolean isObstacleAhead() {
        int nextX = currentPositionX;
        int nextY = currentPositionY;

        switch (direction) {
            case "UP":
                nextY--;
                break;
            case "DOWN":
                nextY++;
                break;
            case "LEFT":
                nextX--;
                break;
            case "RIGHT":
                nextX++;
                break;
        }

        if (nextX < 0 || nextX > 9 || nextY < 0 || nextY > 9) {
            return true;  // Tratar los límites como obstáculos
        }

        return obstacles.contains(nextY * 10 + nextX);
    }

    // Mueve el robot a la posición correcta en la cuadrícula
    private void updateRobotPosition() {
        // Primero, remover el robot de su contenedor actual si existe
        View currentParent = (View) robot.getParent();
        if (currentParent instanceof LinearLayout) {
            ((LinearLayout) currentParent).removeView(robot);
        }

        int index = currentPositionY * 10 + currentPositionX;
        if (index >= 0 && index < gridLayout.getChildCount()) {
            // Obtener el contenedor de la nueva posición
            LinearLayout newContainer = (LinearLayout) gridLayout.getChildAt(index);

            // Configurar los parámetros del robot
            LinearLayout.LayoutParams robotParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            robotParams.gravity = android.view.Gravity.CENTER;
            robot.setLayoutParams(robotParams);

            // Actualizar la rotación del robot
            float rotation = 0;
            switch (direction) {
                case "UP":
                    rotation = 0;
                    break;
                case "RIGHT":
                    rotation = 90;
                    break;
                case "DOWN":
                    rotation = 180;
                    break;
                case "LEFT":
                    rotation = 270;
                    break;
            }
            robot.setRotation(rotation);

            // Añadir el robot al nuevo contenedor
            newContainer.addView(robot);
        }
    }


    // Función para mover el robot adelante en la dirección actual
    private void moveForward() {
        if (isObstacleAhead()) {
            isAvoidingObstacle = true;
            // Intentar encontrar una ruta alternativa
            if (canMoveAroundObstacle()) {
                // La lógica de rodeo se maneja en canMoveAroundObstacle()
                isAvoidingObstacle = false;
                return;
            } else {
                Toast.makeText(this, "It is not possible to go around the obstacle ", Toast.LENGTH_SHORT).show();
                isAvoidingObstacle = false;
                return;
            }
        }

        // Movimiento normal si no hay obstáculo
        switch (direction) {
            case "UP":
                if (currentPositionY > 0) {
                    currentPositionY--;
                    updateRobotPosition();
                } else {
                    Toast.makeText(this, "You can't go outside the grid", Toast.LENGTH_SHORT).show();
                }
                break;
            case "DOWN":
                if (currentPositionY < 9) {
                    currentPositionY++;
                    updateRobotPosition();
                } else {
                    Toast.makeText(this, "You can't go outside the grid", Toast.LENGTH_SHORT).show();
                }
                break;
            case "LEFT":
                if (currentPositionX > 0) {
                    currentPositionX--;
                    updateRobotPosition();
                } else {
                    Toast.makeText(this, "You can't go outside the grid", Toast.LENGTH_SHORT).show();
                }
                break;
            case "RIGHT":
                if (currentPositionX < 9) {
                    currentPositionX++;
                    updateRobotPosition();
                } else {
                    Toast.makeText(this, "You can't go outside the grid", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private boolean canMoveAroundObstacle() {
        visitedPositions.clear();
        return findPath(0);
    }

    private boolean findPath(int depth) {
        if (depth >= MAX_DEPTH) {
            return false;
        }

        int originalX = currentPositionX;
        int originalY = currentPositionY;
        String originalDirection = direction;
        String currentPosition = currentPositionX + "," + currentPositionY + "," + direction;

        if (visitedPositions.contains(currentPosition)) {
            return false;
        }
        visitedPositions.add(currentPosition);

        // Guarda cada movimiento en la lista temporal
        if (!isObstacleAhead() && canMoveForward()) {
            temporaryInstructions.add("FORWARD");
            moveForward();
            if (isPathClear()) {
                return true;
            }

            if (findPath(depth + 1)) {
                return true;
            }

            temporaryInstructions.remove(temporaryInstructions.size() - 1);
            currentPositionX = originalX;
            currentPositionY = originalY;
            direction = originalDirection;
            updateRobotPosition();
        }

        temporaryInstructions.add("RIGHT");
        turnRight();
        if (findPath(depth + 1)) {
            return true;
        }
        temporaryInstructions.remove(temporaryInstructions.size() - 1);

        temporaryInstructions.add("LEFT");
        temporaryInstructions.add("LEFT");
        turnLeft();
        turnLeft();
        if (findPath(depth + 1)) {
            return true;
        }
        temporaryInstructions.remove(temporaryInstructions.size() - 1);
        temporaryInstructions.remove(temporaryInstructions.size() - 1);

        turnRight();
        currentPositionX = originalX;
        currentPositionY = originalY;
        direction = originalDirection;
        updateRobotPosition();
        return false;
    }

    private boolean isPathClear() {
        // Verifica si hay un camino libre hacia el objetivo
        switch (direction) {
            case "UP":
                return !hasObstaclesInColumn(currentPositionX, 0, currentPositionY);
            case "DOWN":
                return !hasObstaclesInColumn(currentPositionX, currentPositionY, 9);
            case "LEFT":
                return !hasObstaclesInRow(0, currentPositionX, currentPositionY);
            case "RIGHT":
                return !hasObstaclesInRow(currentPositionX, 9, currentPositionY);
            default:
                return false;
        }
    }

    private boolean hasObstaclesInColumn(int x, int startY, int endY) {
        int start = Math.min(startY, endY);
        int end = Math.max(startY, endY);

        for (int y = start; y <= end; y++) {
            if (obstacles.contains(y * 10 + x)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasObstaclesInRow(int startX, int endX, int y) {
        int start = Math.min(startX, endX);
        int end = Math.max(startX, endX);

        for (int x = start; x <= end; x++) {
            if (obstacles.contains(y * 10 + x)) {
                return true;
            }
        }
        return false;
    }


    private boolean canMoveForward() {
        switch (direction) {
            case "UP":
                return currentPositionY > 0;
            case "DOWN":
                return currentPositionY < 9;
            case "LEFT":
                return currentPositionX > 0;
            case "RIGHT":
                return currentPositionX < 9;
        }
        return false;
    }


    private void turnLeft() {
        switch (direction) {
            case "UP":
                direction = "LEFT";
                break;
            case "LEFT":
                direction = "DOWN";
                break;
            case "DOWN":
                direction = "RIGHT";
                break;
            case "RIGHT":
                direction = "UP";
                break;
        }
        // Actualiza la posición para aplicar la rotación
        updateRobotPosition();
    }

    private void turnRight() {
        switch (direction) {
            case "UP":
                direction = "RIGHT";
                break;
            case "RIGHT":
                direction = "DOWN";
                break;
            case "DOWN":
                direction = "LEFT";
                break;
            case "LEFT":
                direction = "UP";
                break;
        }
        // Actualiza la posición para aplicar la rotación
        updateRobotPosition();
    }



    // Modificar el método executeInstructions
    private void executeInstructions() {
        if (!webSocketClient.isConnected()) {
            Toast.makeText(this, "No connection with ROS", Toast.LENGTH_SHORT).show();
            //return;
        }

        isExecuting = true;
        btnPlay.setEnabled(false);
        btnObstacle.setEnabled(false);
        currentInstructionIndex = -1;
        instructions.add("Stop");
        new Thread(() -> {
            for (int i = 0; i < instructions.size(); i++) {
                final String instruction = instructions.get(i);
                final String pub = instructions.get(i);
                final int index = i;
                if(pub.equals("Stop")){
                    webSocketClient.publishCommand("Detenerse");
                } else if (pub.equals("Forward")) {
                    webSocketClient.publishCommand("Recto");
                } else if (pub.equals("Right")) {
                    webSocketClient.publishCommand("Derecha");
                } else if (pub.equals("Left")) {
                    webSocketClient.publishCommand("Izquierda");

                }


                try {
                    runOnUiThread(() -> {
                        currentInstructionIndex = index;
                        updateInstructionsDisplay();

                        // Publish command to ROS
//                        webSocketClient.publishCommand(pub);

                        // Update local visualization
                        switch (instruction) {
                            case "Forward":
                                moveForward();
                                break;
                            case "Left":
                                turnLeft();
                                updateRobotPosition();
                                break;
                            case "Right":
                                turnRight();
                                updateRobotPosition();
                                break;
                            case "Stop":
                                break;
                        }
                    });

                    Thread.sleep(1000); // Wait for execution
                } catch (InterruptedException e) {
                    Log.e(TAG, "Instruction execution interrupted", e);
                    break;
                }
            }

            runOnUiThread(() -> {
                isExecuting = false;
                btnPlay.setEnabled(true);
                btnObstacle.setEnabled(true);
                currentInstructionIndex = -1;
                updateInstructionsDisplay();
            });
        }).start();
    }
}
