package com.robotsimulator.eq3.manual;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ManualRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "ManualRenderer";
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private Room room;
    private CarObject cube;

    private ROSWebSocketClient webSocketClientt;

//    private ManualCube cube;
    private Trail trail;

    private Context context;
    private MovementDirection currentDirection = MovementDirection.NONE;
    private float moveSpeed = 0.02f;
    private boolean isInitialized = false;

    // Parámetros de la cámara
    private float cameraRotationX = 30.0f; // Rotación vertical (inclinación)
    private float cameraRotationY = 0.0f;  // Rotación horizontal (giro)
    private float cameraDistance = 15.0f;  // Distancia de la cámara
    private static final float ROOM_BOUND = 2.9f; // Límite de la habitación

    public ManualRenderer(Context context) {
        this.context = context;
        cube = new CarObject(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glEnable(GLES30.GL_CULL_FACE);
            GLES30.glDepthFunc(GLES30.GL_LESS);
            cube = new CarObject(context);
            room = new Room();
//            cube = new ManualCube();
            trail = new Trail();

            isInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error en inicialización", e);
            isInitialized = false;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 0.1f, 100.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!isInitialized) return;

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // Calcular posición de la cámara usando coordenadas esféricas
        float camX = (float) (cameraDistance * Math.cos(Math.toRadians(cameraRotationX))
                * Math.sin(Math.toRadians(cameraRotationY)));
        float camY = (float) (cameraDistance * Math.sin(Math.toRadians(cameraRotationX)));
        float camZ = (float) (cameraDistance * Math.cos(Math.toRadians(cameraRotationX))
                * Math.cos(Math.toRadians(cameraRotationY)));

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,  // Posición de la cámara
                0f, 0f, 0f,        // Punto al que mira
                0f, 1.0f, 0f);     // Vector "arriba"

        updateCubePosition();

        room.draw(projectionMatrix, viewMatrix);
        trail.draw(projectionMatrix, viewMatrix);
        cube.draw(projectionMatrix, viewMatrix);
    }

    private void updateCubePosition() {
        if (currentDirection != MovementDirection.NONE) {
            float newX = cube.getPosX();
            float newZ = cube.getPosZ();

            // Calcular movimiento relativo a la rotación de la cámara
            float moveAngle = cameraRotationY;
            switch (currentDirection) {
                case FORWARD:
                    newX += 0.1 * Math.sin(Math.toRadians(moveAngle));
                    newZ += 0.1 * Math.cos(Math.toRadians(moveAngle));
                    break;
                case BACKWARD:
                    newX -= 0.1 * Math.sin(Math.toRadians(moveAngle));
                    newZ -= 0.1 * Math.cos(Math.toRadians(moveAngle));
                    break;
                case LEFT:
                    newX -= moveSpeed * Math.cos(Math.toRadians(moveAngle));
                    newZ += moveSpeed * Math.sin(Math.toRadians(moveAngle));
                    break;
                case RIGHT:
                    newX += moveSpeed * Math.cos(Math.toRadians(moveAngle));
                    newZ -= moveSpeed * Math.sin(Math.toRadians(moveAngle));
                    break;
            }

            // Verificar colisiones
            if (isValidPosition(newX, newZ)) {
                cube.setPosition(newX, 0.5f, newZ);
                trail.addPoint(newX, 0, newZ);
            }else {
                stopMovement();
                sendROSMessage("Detenerse");
            }
        }
    }
    private void sendROSMessage(String command) {
        if (webSocketClientt != null && webSocketClientt.isConnected()) {
            String message = "{ \"op\": \"publish\", \"topic\": \"/decisiones_pub\", \"msg\": { \"data\": \"" + command + "\" } }";
            webSocketClientt.send(message);
        }
    }

    private boolean isValidPosition(float x, float z) {
        return Math.abs(x) < ROOM_BOUND && Math.abs(z) < ROOM_BOUND;
    }

    public void startMovement(MovementDirection direction) {
        currentDirection = direction;
    }

    public void stopMovement() {
        currentDirection = MovementDirection.NONE;
    }

    // Métodos para el control de cámara 360
    public void updateCamera(float deltaX, float deltaY) {
        cameraRotationY += deltaX;
        cameraRotationX += deltaY;

        // Limitar la rotación vertical para evitar giros excesivos
        cameraRotationX = Math.max(-80, Math.min(80, cameraRotationX));

        // Mantener la rotación horizontal entre 0 y 360 grados
        cameraRotationY = cameraRotationY % 360;
    }

}