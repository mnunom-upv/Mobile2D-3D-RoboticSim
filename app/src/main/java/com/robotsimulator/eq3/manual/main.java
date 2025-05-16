package com.robotsimulator.eq3.manual;/*package com.example.pista.manual;

import android.app.AlertDialog;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pista.R;
import com.example.pista.aut.GameObject;
import com.example.pista.aut.GameRenderer;
import com.example.pista.aut.Player;

import java.util.ArrayList;

public class main extends AppCompatActivity {
    private GLSurfaceView glSurfaceView2;
    private GameRenderer renderer;
    private boolean isRotating = false;
    private float previousX, previousY;

    private TextView coordsText;
    private boolean constructionMode = false;
    private Button btnConstructionMode;

    private Button btnCar;
    private boolean carPlacementMode = false;
    private static final float TOUCH_SCALE_FACTOR = 0.8f;
    private ScaleGestureDetector scaleGestureDetector;

    // Definir un tamaño fijo para el piso
    private static final float FLOOR_SIZE = 20; // Tamaño del piso en unidades
    private static final int NUM_FLOOR_OBJECTS = 50; // Número de objetos para formar el piso

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        ArrayList<Float> pathPoints = new ArrayList<>();

        glSurfaceView2 = findViewById(R.id.glSurfaceView);
        glSurfaceView2.setEGLContextClientVersion(3);

        // Crear el renderer sin verificar modo
        renderer = new GameRenderer(this, pathPoints);
        glSurfaceView2.setRenderer(renderer);

        // Configurar renderizado continuo para actualizaciones de movimiento
        glSurfaceView2.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Configurar todos los botones y controles
        setupMovementButtons();
        setupPlaceObjectButton();
        setupConstructionMode();
        setupChangeTrackButton();
        setupCamera();
        setupCarButton();
        createFloor();


    }
    private void createFloor() {
        float halfSize = FLOOR_SIZE / 2;

        // Crear cuadrícula de objetos para formar el piso
        for (int x = 0; x < NUM_FLOOR_OBJECTS; x++) {
            for (int z = 0; z < NUM_FLOOR_OBJECTS; z++) {
                // Calcular posición para cada objeto del piso
                float posX = -halfSize + (x * (FLOOR_SIZE / NUM_FLOOR_OBJECTS));
                float posZ = -halfSize + (z * (FLOOR_SIZE / NUM_FLOOR_OBJECTS));

                // Crear objeto en la posición calculada, con Y=0 para que esté en el suelo
                GameObject floorObject = new GameObject(posX, 0f, posZ);
                renderer.addGameObject(floorObject);
            }
        }

        // Forzar renderizado
        glSurfaceView2.requestRender();
    }

    private void setupCarButton() {
        Button btnCar = findViewById(R.id.btnCar);
        btnCar.setOnClickListener(v -> {
            renderer.toggleCarPlacementMode();
            btnCar.setBackgroundColor(renderer.isCarPlacementMode() ?
                    0xFF888888 : 0xFFFF0000);
            Toast.makeText(this,
                    renderer.isCarPlacementMode() ?
                            "Modo colocación de carro activado" :
                            "Modo colocación de carro desactivado",
                    Toast.LENGTH_SHORT).show();
        });
    }
    private void setupCamera() {
        // Configurar detector de gestos de escala (pinch to zoom)
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                renderer.adjustCameraDistance(1f / scaleFactor);
                return true;
            }
        });

        // Configurar detector de gestos para rotación
        glSurfaceView2.setOnTouchListener((v, event) -> {
            // Primero manejar gestos de escala
            scaleGestureDetector.onTouchEvent(event);

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isRotating = true;
                    previousX = x;
                    previousY = y;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!scaleGestureDetector.isInProgress() && isRotating) {
                        float dx = x - previousX;
                        float dy = y - previousY;
                        renderer.updateCameraRotation(dx * TOUCH_SCALE_FACTOR, dy * TOUCH_SCALE_FACTOR);
                        previousX = x;
                        previousY = y;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isRotating = false;
                    break;
            }
            return true;
        });

    }
    private void setupChangeTrackButton() {
        Button btnChangeTrack = findViewById(R.id.btnChangeTrack);
        btnChangeTrack.setOnClickListener(v -> {
            if (renderer != null && renderer.getTerrain() != null) {
                // Cambiar la pista
                // renderer.getTerrain().changeTrackType();  // Comentamos esta línea temporalmente
                // Limpiar objetos existentes
                renderer.clearObjects();
                // Forzar un nuevo renderizado
                glSurfaceView2.requestRender();
                // Notificar al usuario
                Toast.makeText(this, "Cambiando pista...", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Método para crear un listener de movimiento reutilizable
    private View.OnTouchListener createMovementTouchListener(Runnable onPress, Runnable onRelease) {
        return (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onPress.run();
                    v.performClick();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    onRelease.run();
                    return true;
            }
            return false;
        };
    }

    private void setupPlaceObjectButton() {
        Button btnPlaceObject = findViewById(R.id.btnPlaceObject);
        btnPlaceObject.setOnClickListener(v -> showObjectPlacementDialog());
    }

    private void showObjectPlacementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Colocar Objeto");

        // Crear layout para los campos de entrada
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Campos de entrada para coordenadas X, Y, Z
        EditText inputX = new EditText(this);
        inputX.setHint("Coordenada X");
        inputX.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText inputY = new EditText(this);
        inputY.setHint("Coordenada Y");
        inputY.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText inputZ = new EditText(this);
        inputZ.setHint("Coordenada Z");
        inputZ.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        layout.addView(inputX);
        layout.addView(inputY);
        layout.addView(inputZ);

        builder.setView(layout);

        builder.setPositiveButton("Colocar", (dialog, which) -> {
            try {
                float x = Float.parseFloat(inputX.getText().toString());
                float y = Float.parseFloat(inputY.getText().toString());
                float z = Float.parseFloat(inputZ.getText().toString());

                // Crear objeto en las coordenadas especificadas
                GameObject newObject = new GameObject(x, y, z);
                renderer.addGameObject(newObject);

                // Forzar múltiples renderizados
                for (int i = 0; i < 3; i++) {
                    glSurfaceView2.requestRender();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Por favor, ingrese coordenadas válidas", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    private void setupConstructionMode() {
        btnConstructionMode = findViewById(R.id.btnConstructionMode);
        coordsText = findViewById(R.id.coordsText);

        btnConstructionMode.setOnClickListener(v -> {
            constructionMode = !constructionMode;
            btnConstructionMode.setBackgroundColor(
                    constructionMode ? 0xFFFF0000 : 0xFF888888
            );
            Toast.makeText(this,
                    constructionMode ? "Modo construcción activado" : "Modo construcción desactivado",
                    Toast.LENGTH_SHORT).show();
        });

        glSurfaceView2.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();

                // Obtener las coordenadas del mundo
                float[] worldPos = renderer.screenToWorld(x, y);
                if (worldPos != null) {
                    // Actualizar el texto de coordenadas
                    String coords = String.format("Toque en: X=%.1f, Z=%.1f", worldPos[0], worldPos[2]);
                    coordsText.setText(coords);

                    // Si estamos en modo construcción, colocar objeto
                    if (constructionMode) {
                        GameObject newObject = new GameObject(worldPos[0], worldPos[1], worldPos[2]);
                        renderer.addGameObject(newObject);

                        // Asegurar que se renderice
                        glSurfaceView2.requestRender();
                    }
                }
            }
            return true;
        });

        // Actualizar coordenadas del jugador
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (renderer != null && renderer.getPlayer() != null) {
                    Player player = renderer.getPlayer();
                    String playerCoords = String.format("Jugador en: X=%.2f, Z=%.1f",
                            player.getPosX(), player.getPosZ());
                    coordsText.setText(playerCoords);
                    handler.postDelayed(this, 100);
                }
            }
        });
    }
    private void setupMovementButtons() {
        Button btnMoveForward = findViewById(R.id.btnMoveForward);
        Button btnMoveBackward = findViewById(R.id.btnMoveBackward);
        Button btnMoveLeft = findViewById(R.id.btnMoveLeft);
        Button btnMoveRight = findViewById(R.id.btnMoveRight);

        btnMoveForward.setOnTouchListener(createMovementTouchListener(
                () -> renderer.setMovingForward(true),
                () -> renderer.setMovingForward(false)
        ));

        btnMoveBackward.setOnTouchListener(createMovementTouchListener(
                () -> renderer.setMovingBackward(true),
                () -> renderer.setMovingBackward(false)
        ));

        btnMoveLeft.setOnTouchListener(createMovementTouchListener(
                () -> renderer.setMovingLeft(true),
                () -> renderer.setMovingLeft(false)
        ));

        btnMoveRight.setOnTouchListener(createMovementTouchListener(
                () -> renderer.setMovingRight(true),
                () -> renderer.setMovingRight(false)
        ));
    }
    private void setupGLSurfaceView() {
        glSurfaceView2.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isRotating = true;
                    previousX = x;
                    previousY = y;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isRotating) {
                        float deltaX = x - previousX;
                        float deltaY = y - previousY;
                        renderer.updateCamera(deltaX, deltaY);
                        previousX = x;
                        previousY = y;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isRotating = false;
                    break;
            }
            return true;
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView2 != null) {
            glSurfaceView2.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView2 != null) {
            glSurfaceView2.onResume();
        }
    }
}*/