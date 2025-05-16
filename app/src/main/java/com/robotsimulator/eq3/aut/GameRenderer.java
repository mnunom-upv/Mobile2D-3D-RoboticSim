package com.robotsimulator.eq3.aut;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "GameRenderer";

    private Terrain terrain;
    private Trail trail;

    private Player player;
    private ArrayList<Float> customTrackPoints;
    private float cameraRotationX = 45.0f; // Ángulo de inclinación inicial
    private float cameraRotationY = 0.0f;  // Ángulo de rotación horizontal
    private float cameraDistance = 20.0f;   // Distancia de la cámara
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private boolean isAutoMoving = false;
    private int currentPathIndex = 0;

    // Campos para la cámara
    private float cameraX = 10f;
    private float cameraY = 15f;
    private float cameraZ = 10f;
    private float cameraRotX = 45f; // Rotación vertical (pitch)
    private float cameraRotY = 0f;  // Rotación horizontal (yaw)
    private float cameraDist = 20f;

    // Variables para el punto al que mira la cámara (target)
    private float targetX = 10f;
    private float targetY = 0f;
    private float targetZ = 10f;

    private String currentDecision = "";
    private final Object decisionLock = new Object();
    private float moveSpeed = 0.1f; // Ajusta este valor según necesites

    private CarObject car;
    private CarObject carObject;
    private Context context;
    private boolean carPlacementMode = false;
    private float[] rayStart = new float[4];
    private float[] rayEnd = new float[4];
    private float[] tempMatrix = new float[16];
    private float[] invertedMVP = new float[16];
    private ArrayList<Float> trackPoints;
    private int currentTrackPointIndex = 0;



    private void updateCamera() {
        float rotXRad = (float) Math.toRadians(cameraRotX);
        float rotYRad = (float) Math.toRadians(cameraRotY);

        float horizontalDist = (float) (cameraDist * Math.cos(rotXRad));
        float camX = targetX + (float) (horizontalDist * Math.sin(rotYRad));
        float camY = targetY + (float) (cameraDist * Math.sin(rotXRad));
        float camZ = targetZ + (float) (horizontalDist * Math.cos(rotYRad));

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,
                targetX, targetY, targetZ,
                0f, 1.0f, 0f);
    }
    public void updateCameraRotation(float deltaX, float deltaY) {
        // Actualizar los ángulos de rotación
        cameraRotY += deltaX * 0.5f;
        cameraRotX += deltaY * 0.5f;

        // Limitar la rotación vertical para evitar giros excesivos
        cameraRotX = Math.max(-89f, Math.min(89f, cameraRotX));

        // Convertir ángulos a radianes
        float rotXRad = (float) Math.toRadians(cameraRotX);
        float rotYRad = (float) Math.toRadians(cameraRotY);

        // Calcular la nueva posición de la cámara usando coordenadas esféricas
        float horizontalDistance = (float) (cameraDist * Math.cos(rotXRad));
        cameraX = targetX + (float) (horizontalDistance * Math.sin(rotYRad));
        cameraZ = targetZ + (float) (horizontalDistance * Math.cos(rotYRad));
        cameraY = targetY + (float) (cameraDist * Math.sin(rotXRad));

        Log.d("Camera", String.format("Pos: (%.2f, %.2f, %.2f), Rot: (%.2f, %.2f)",
                cameraX, cameraY, cameraZ, cameraRotX, cameraRotY));
    }


    public void adjustCameraDistance(float factor) {
        // Ajustar la distancia de la cámara (zoom)
        cameraDist = Math.max(5f, Math.min(40f, cameraDist * factor));
        updateCameraRotation(0, 0); // Actualizar posición con la nueva distancia
    }

    // Controles de movimiento
    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;

    private boolean movingStop= false;


    private List<GameObject> gameObjects = new ArrayList<>();
    private Random random = new Random();


    public GameRenderer(Context context, ArrayList<Float> pathPoints) {
        this.context = context;
        this.customTrackPoints = pathPoints;
        this.player = new Player();
        car = new CarObject(context);
        this.trackPoints = pathPoints;
        Matrix.setIdentityM(modelMatrix, 0);
    }

    public void toggleCarPlacementMode() {
        carPlacementMode = !carPlacementMode;
    }

    private void placeCarAtNextTrackPoint() {
        if (trackPoints != null && trackPoints.size() >= 2) {
            if (currentTrackPointIndex >= trackPoints.size() - 1) {
                currentTrackPointIndex = 0;
            }

            // Ajustar la escala para coincidir con el tamaño del terreno
            float width = 60; // Mismo valor usado en Terrain
            float blockSize = 0.333f; // Mismo valor usado en Terrain

            // Convertir las coordenadas normalizadas a coordenadas del mundo
            float trackX = trackPoints.get(currentTrackPointIndex) * width * blockSize;
            float trackZ = trackPoints.get(currentTrackPointIndex + 1) * width * blockSize;

            car.setPosition(trackX, 0.5f, trackZ);
            currentTrackPointIndex += 2;
        }
    }
    public boolean isCarPlacementMode() {
        return carPlacementMode;
    }

    public void placeCarAt(float x, float y, float z) {
        if (isPointOnTrack(x, z)) {
            car.setPosition(x, 0, z); // Cambia y por 0
            Log.d("GameRenderer", "Car placed in: " + x + ", " + y + ", " + z);
        } else {
            Log.d("GameRenderer", "Point off the track: " + x + ", " + z);
        }
    }
    // Método para convertir coordenadas de pantalla a coordenadas del mundo
    public float[] screenToWorldCoordinates(float screenX, float screenY) {
        int width = context instanceof Activity ?
                ((Activity)context).getWindowManager().getDefaultDisplay().getWidth() : 800;
        int height = context instanceof Activity ?
                ((Activity)context).getWindowManager().getDefaultDisplay().getHeight() : 600;

        float normalizedX = (2.0f * screenX) / width - 1.0f;
        float normalizedY = 1.0f - (2.0f * screenY) / height;

        // Crear el rayo
        float[] nearPoint = {normalizedX, normalizedY, -1.0f, 1.0f};
        float[] farPoint = {normalizedX, normalizedY, 1.0f, 1.0f};

        // Calcular MVP inversa
        float[] mvpMatrix = new float[16];
        float[] mvpInverse = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.invertM(mvpInverse, 0, mvpMatrix, 0);

        // Transformar puntos
        float[] worldNear = new float[4];
        float[] worldFar = new float[4];
        Matrix.multiplyMV(worldNear, 0, mvpInverse, 0, nearPoint, 0);
        Matrix.multiplyMV(worldFar, 0, mvpInverse, 0, farPoint, 0);

        // Normalizar coordenadas homogéneas
        for (int i = 0; i < 3; i++) {
            worldNear[i] /= worldNear[3];
            worldFar[i] /= worldFar[3];
        }

        // Calcular intersección con el plano y = 0
        float t = -worldNear[1] / (worldFar[1] - worldNear[1]);
        float x = worldNear[0] + t * (worldFar[0] - worldNear[0]);
        float z = worldNear[2] + t * (worldFar[2] - worldNear[2]);

        Log.d("GameRenderer", "Coordenadas calculadas: " + x + ", " + z);
        return new float[] {x, 0, z};
    }
    public boolean isPointOnTrack(float x, float z) {
        if (customTrackPoints == null || customTrackPoints.isEmpty()) {
            Log.d("GameRenderer", "No hay puntos de pista");
            return false;
        }

        int width = 60;
        int depth = 60;
        float blockSize = 0.333f;
        float tolerance = 5.0f; // Aumentado para mayor área de detección

        // Convertir coordenadas del mundo a coordenadas normalizadas
        float normalizedX = x / (width * blockSize);
        float normalizedZ = z / (depth * blockSize);

        // Verificar cada segmento de la pista
        for (int i = 0; i < customTrackPoints.size() - 1; i += 2) {
            float trackX = customTrackPoints.get(i);
            float trackZ = customTrackPoints.get(i + 1);

            float dx = trackX - normalizedX;
            float dz = trackZ - normalizedZ;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            if (distance < tolerance) {
                Log.d("GameRenderer", "Punto válido encontrado en: " + x + ", " + z);
                return true;
            }
        }

        Log.d("GameRenderer", "Punto fuera de la pista: " + x + ", " + z);
        return false;
    }


    private float distanceToLineSegment(float px, float pz, float x1, float z1, float x2, float z2) {
        float A = px - x1;
        float B = pz - z1;
        float C = x2 - x1;
        float D = z2 - z1;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = dot / len_sq;

        float xx, zz;

        if (param < 0) {
            xx = x1;
            zz = z1;
        }
        else if (param > 1) {
            xx = x2;
            zz = z2;
        }
        else {
            xx = x1 + param * C;
            zz = z1 + param * D;
        }

        float dx = px - xx;
        float dz = pz - zz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            trail = new Trail();
            // Verificaciones de inicialización
            Log.d(TAG, "OpenGL Version: " + GLES30.glGetString(GLES30.GL_VERSION));
            Log.d(TAG, "OpenGL Vendor: " + GLES30.glGetString(GLES30.GL_VENDOR));
            Log.d(TAG, "OpenGL Renderer: " + GLES30.glGetString(GLES30.GL_RENDERER));

            // Configuraciones básicas
            GLES30.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glDepthFunc(GLES30.GL_LESS);
            carObject = new CarObject(context);


            trail = new Trail();

            // Inicialización del terreno según el modo
            if (customTrackPoints != null) {
                terrain = new Terrain(customTrackPoints);
            } else {
                terrain = new Terrain();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onSurfaceCreated", e);
        }
    }

    private boolean checkCollision(float x, float z) {
        float collisionRadius = 1.0f; // Radio de colisión

        synchronized(gameObjects) {
            for (GameObject obj : gameObjects) {
                float dx = x - obj.getPosX();
                float dz = z - obj.getPosZ();
                float distanceSquared = dx * dx + dz * dz;

                if (distanceSquared < collisionRadius * collisionRadius) {
                    return true; // Hay colisión
                }
            }
        }
        return false;
    }
    public void handleTouch(float screenX, float screenY) {
        if (!carPlacementMode) return;

        // Convertir coordenadas de pantalla a mundo
        float[] worldCoords = screenToWorldCoordinates(screenX, screenY);
        if (worldCoords == null) return;

        // Colocar el carro
        car.setPosition(worldCoords[0], 0.5f, worldCoords[2]);
        Log.d(TAG, "Car placed in: " + worldCoords[0] + ", " + worldCoords[2]);
    }    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 60, ratio, 0.1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            updatePlayerMovement();

            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
            updateCamera();

            // Actualizar la matriz de vista
            float rotXRad = (float) Math.toRadians(cameraRotX);
            float rotYRad = (float) Math.toRadians(cameraRotY);

            float horizontalDist = (float) (cameraDist * Math.cos(rotXRad));
            float camX = targetX + (float) (horizontalDist * Math.sin(rotYRad));
            float camY = targetY + (float) (cameraDist * Math.sin(rotXRad));
            float camZ = targetZ + (float) (horizontalDist * Math.cos(rotYRad));

            Matrix.setLookAtM(viewMatrix, 0,
                    camX, camY, camZ,
                    targetX, targetY, targetZ,
                    0f, 1.0f, 0f);

            if (terrain != null) {
                terrain.draw(projectionMatrix, viewMatrix);
            }

            trail.draw(projectionMatrix, viewMatrix);

            if (car != null && car.isPlaced()) {
                car.draw(projectionMatrix, viewMatrix);
            }

            if (isAutoMoving && car != null && car.isPlaced() && customTrackPoints != null
                    && currentPathIndex < customTrackPoints.size() - 1) {

                float targetX = customTrackPoints.get(currentPathIndex) * 60 * 0.333f;
                float targetZ = customTrackPoints.get(currentPathIndex + 1) * 60 * 0.333f;

                float dx = targetX - car.getPosX();
                float dz = targetZ - car.getPosZ();
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                if (distance < 0.1f) {
                    currentPathIndex += 2;
                } else {
                    float dirX = dx / distance * moveSpeed;
                    float dirZ = dz / distance * moveSpeed;
                    car.setPosition(car.getPosX() + dirX, car.getPosY(), car.getPosZ() + dirZ);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
        }
    }
    // Método existente de colocación aleatoria si aún lo quieres
    public void placeRandomObject() {
        float randomX = random.nextFloat() * 20;
        float randomZ = random.nextFloat() * 20;

        GameObject newObject = new GameObject(randomX, 1f, randomZ);
        gameObjects.add(newObject);

        Log.d("GameRenderer", "Orandom object placed in: X=" + randomX + ", Z=" + randomZ);
    }

    // Método para agregar un objeto en una posición específica
    public void addGameObject(GameObject object) {
        synchronized(gameObjects) {
            gameObjects.add(object);
            Log.d("GameRenderer", String.format(
                    "Object added in: X=%.2f, Y=%.2f, Z=%.2f - Total objects: %d",
                    object.getPosX(), object.getPosY(), object.getPosZ(), gameObjects.size()
            ));
        }
        // Forzar un renderizado inmediato
        if (context instanceof MainActivity) {
            ((MainActivity)context).runOnUiThread(() -> {
                Toast.makeText(context,
                        "Object placed - Total: " + gameObjects.size(),
                        Toast.LENGTH_SHORT).show();
            });
        }
    }
    public void updateDecision(String decision) {
        Log.d(TAG, "Recibiendo decisión: " + decision);
        synchronized (decisionLock) {
            this.currentDecision = decision;
            // Resetear todos los estados primero
            movingForward = false;
            movingBackward = false;
            movingLeft = false;
            movingRight = false;
            movingStop = false;

            // Actualizar el estado según la decisión
            if (decision.contains("Recto")) {
                movingForward = true;
            } else if (decision.contains("Reversa")) {
                movingBackward = true;
            } else if (decision.contains("Izquierda")) {
                movingLeft = true;
            } else if (decision.contains("Derecha")) {
                movingRight = true;
            } else if (decision.contains("Detenerse")) {
                movingStop = true;
            }
            Log.d(TAG, "Estado actualizado - Forward: " + movingForward +
                    ", Backward: " + movingBackward +
                    ", Left: " + movingLeft +
                    ", Right: " + movingRight +
                    ", Stop: " + movingStop);
        }
    }

    private void updatePlayerMovement() {

        if (!car.isPlaced()) {
            return;  // No hacer nada si el carro no está colocado
        }

        float deltaX = 0;
        float deltaZ = 0;
        float actualSpeed = 0.05f; // Reducir la velocidad para movimiento más suave
        float trailHeight = 0.5f; // Altura del rastro sobre el suelo

        synchronized (decisionLock) {
            if (movingForward) {
                deltaZ -= actualSpeed;
                //trail.addPoint(deltaX, 2, deltaZ);
                //trail.draw(projectionMatrix, viewMatrix);

            }
            else if (movingBackward) {
                deltaZ += actualSpeed;
                //trail.addPoint(deltaX, 0, deltaZ);

            }
            else if (movingLeft) {
                deltaX -= actualSpeed;
                //trail.addPoint(deltaX, 0, deltaZ);

            }
            else if (movingRight) {
                deltaX += actualSpeed;
                //trail.addPoint(deltaX, 0, deltaZ);

            }
        }

        if (deltaX != 0 || deltaZ != 0) {
            float newX = car.getPosX() + deltaX;
            float newZ = car.getPosZ() + deltaZ;

            // Verificar si el nuevo punto está en la pista
            if (isPointOnTrack(newX, newZ)) {
                car.setPosition(newX, car.getPosY(), newZ);
                // Añadir el punto al trail usando la posición actual del carro
                trail.addPoint(newX, trailHeight, newZ);

                // Actualizar la posición objetivo de la cámara
                targetX = newX;
                targetZ = newZ;

                Log.d(TAG, "Car moved to: (" + newX + ", " + newZ + ")");
            } else {
                Log.d(TAG, "Movimiento bloqueado: punto fuera de la pista");
            }
        }
    }





    public Terrain getTerrain() {
        return terrain;
    }
    public void clearObjects() {
        synchronized(gameObjects) {
            gameObjects.clear();
        }
    }
    public float[] screenToWorld(float screenX, float screenY) {
        float playerX = player.getPosX();
        float playerZ = player.getPosZ();
        float playerRotY = player.getRotationY();

        // Colocar objeto frente al jugador
        float distance = 3.0f;
        float objectX = playerX + (float)(Math.sin(playerRotY) * distance);
        float objectZ = playerZ - (float)(Math.cos(playerRotY) * distance);

        // Mantener dentro de los límites del terreno
        objectX = Math.max(1.0f, Math.min(objectX, 19.0f));
        objectZ = Math.max(1.0f, Math.min(objectZ, 19.0f));

        // Elevar el objeto para que sea visible sobre el terreno
        return new float[]{objectX, 1.0f, objectZ};
    }
    public Player getPlayer() {
        return player;
    }
    public void startAutoMovement() {
        isAutoMoving = true;

        // Encontrar el punto más cercano en el track
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < customTrackPoints.size() - 1; i += 2) {
            float trackX = customTrackPoints.get(i) * 60 * 0.333f;
            float trackZ = customTrackPoints.get(i + 1) * 60 * 0.333f;

            float dx = trackX - car.getPosX();
            float dz = trackZ - car.getPosZ();
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            if (distance < minDistance) {
                minDistance = distance;
                currentPathIndex = i;
            }
        }
    }

    public void stopAutoMovement() {
        isAutoMoving = false;
    }
    // Métodos para controlar el movimiento
    public void setMovingForward(boolean moving) {
        this.movingForward = moving;
    }

    public void setMovingBackward(boolean moving) {
        this.movingBackward = moving;
    }

    public void setMovingLeft(boolean moving) {
        this.movingLeft = moving;
    }

    public void setMovingRight(boolean moving) {
        this.movingRight = moving;
    }
}