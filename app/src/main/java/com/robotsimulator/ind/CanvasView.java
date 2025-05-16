package com.robotsimulator.ind;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.robotsimulator.R;

import java.util.ArrayList;
import java.util.List;

public class CanvasView extends View {
    private Paint paint;  // Pintura para dibujar la trayectoria
    private float currentX, currentY;  // Coordenadas actuales del punto de dibujo
    private String currentDirection = "";  // Dirección actual de movimiento
    private Handler handler = new Handler();  // Manejador para programar acciones
    private Runnable drawRunnable;  // Runnable para la animación de dibujo
    private List<float[]> pathPoints;  // Lista para almacenar los puntos de la trayectoria
    private float scaleFactor = 4.0f; // Factor para escalar el lienzo
    private static final float THRESHOLD = 20f; // Umbral para activar el escalado
    private Bitmap iconCar, iconCarLight; // Bitmaps para el ícono del carro y el ícono de parada
    private Bitmap currentIcon; // Icono actual basado en la dirección
    private boolean isFirstUpdate = true; // Variable para la primera actualización

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);  // Color de la línea de trayectoria
        paint.setStrokeWidth(5f);  // Ancho de la línea
        pathPoints = new ArrayList<>();

        // Cargar y redimensionar los íconos de carro desde recursos
        iconCar = BitmapFactory.decodeResource(getResources(), R.drawable.icon_car);
        iconCarLight = BitmapFactory.decodeResource(getResources(), R.drawable.icon_car_light);

        // Redimensionar los íconos a 24x24 píxeles
        iconCar = resizeBitmap(iconCar, 24, 24);
        iconCarLight = resizeBitmap(iconCarLight, 24, 24);

        currentIcon = iconCar;  // Iniciar con el icono de movimiento

        // Inicializar las coordenadas en el centro de la vista
        post(() -> {
            currentX = getWidth() / 2;
            currentY = getHeight() / 2;
            pathPoints.add(new float[]{currentX, currentY});
        });

        // Runnable para animar el movimiento y el ajuste de escala
        drawRunnable = new Runnable() {
            @Override
            public void run() {
                if (!currentDirection.isEmpty()) {
                    move();
                    adjustScaling();
                    invalidate();
                    handler.postDelayed(this, 200);
                }
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);

        // Dibujar la trayectoria
        for (int i = 1; i < pathPoints.size(); i++) {
            float[] startPoint = pathPoints.get(i - 1);
            float[] endPoint = pathPoints.get(i);
            canvas.drawLine(startPoint[0], startPoint[1], endPoint[0], endPoint[1], paint);
        }

        // Dibujar el ícono del carro en la posición actual
        canvas.drawBitmap(currentIcon, currentX - currentIcon.getWidth() / 2, currentY - currentIcon.getHeight() / 2, null);
        canvas.restore();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    private String previousDirection = ""; // Variable para almacenar la dirección anterior

    private void move() {
        float nextX = currentX;
        float nextY = currentY;

        // Actualizar la posición según la dirección actual
        switch (currentDirection) {
            case "up":
                nextY -= 2;
                currentIcon = rotateBitmap(iconCar, 0);  // Sin rotación para arriba
                break;
            case "down":
                nextY += 2;
                currentIcon = rotateBitmap(iconCar, 180);  // Rotar 180° para abajo
                break;
            case "left":
                nextX -= 2;
                currentIcon = rotateBitmap(iconCar, -90);  // Rotar -90° para izquierda
                break;
            case "right":
                nextX += 2;
                currentIcon = rotateBitmap(iconCar, 90);  // Rotar 90° para derecha
                break;
            case "stop":
                // Rotar el ícono de detención según la dirección anterior
                currentIcon = rotateBitmap(iconCarLight, getRotationAngle(previousDirection));
                // Mantener la posición actual
                return;  // Salir de la función para evitar añadir un nuevo punto
        }

        // Actualizar coordenadas y añadir nuevo punto a la trayectoria
        currentX = nextX;
        currentY = nextY;
        pathPoints.add(new float[]{currentX, currentY});

        // Guardar la dirección actual como la dirección anterior
        previousDirection = currentDirection;
    }

    // Método para obtener el ángulo de rotación basado en la dirección
    private float getRotationAngle(String direction) {
        switch (direction) {
            case "up":
                return 0;  // Sin rotación
            case "down":
                return 180;  // 180 grados
            case "left":
                return -90;  // -90 grados
            case "right":
                return 90;  // 90 grados
            default:
                return 0;  // Sin rotación
        }
    }

    // Rotar un bitmap a un ángulo específico
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void adjustScaling() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        // Calcular los límites de la trayectoria para el ajuste de escala
        for (float[] point : pathPoints) {
            if (point[0] < minX) minX = point[0];
            if (point[1] < minY) minY = point[1];
            if (point[0] > maxX) maxX = point[0];
            if (point[1] > maxY) maxY = point[1];
        }

        float width = maxX - minX;
        float height = maxY - minY;

        float maxWidth = getWidth() - (getWidth() * 0.65f);
        float maxHeight = getHeight() - (getHeight() * 0.65f);

        float scaleX = maxWidth / width;
        float scaleY = maxHeight / height;

        // Calcular nuevo factor de escala y ajustar gradualmente
        float newScaleFactor = Math.min(scaleX, scaleY);
        if (isFirstUpdate) {
            newScaleFactor *= 0.1f; // Escalar solo en la primera actualización
            isFirstUpdate = false;   // Cambiar a falso después de la primera vez
        }

        scaleFactor += (newScaleFactor - scaleFactor) * 0.8f;

        if (scaleFactor < 0.03f) scaleFactor = 0.03f;
    }

    // Establecer la dirección del movimiento
    public void setDirection(String direction) {
        currentDirection = direction.equals("stop") ? "" : direction;
        handler.removeCallbacks(drawRunnable);
        handler.post(drawRunnable);
    }

    // Método para detener el dibujo
    public void stopDrawing() {
        currentDirection = "stop";  // Establecer dirección en "stop" para mostrar el ícono de parada
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        currentX = w / 2;
        currentY = h / 2;
        pathPoints.clear();
        pathPoints.add(new float[]{currentX, currentY});
    }

    // Método para reiniciar el lienzo
    public void resetCanvas() {
        // Restaurar posición inicial al centro de la vista
        currentX = getWidth() / 2;
        currentY = getHeight() / 2;

        // Limpiar la lista de puntos de la trayectoria
        pathPoints.clear();
        pathPoints.add(new float[]{currentX, currentY});

        // Restaurar la escala inicial y los íconos
        scaleFactor = 4.0f;
        isFirstUpdate = true; // Restablecer para el ajuste de escala inicial
        currentDirection = "";
        previousDirection = "";
        currentIcon = iconCar;  // Restaurar al icono de movimiento inicial

        // Detener cualquier acción en curso y redibujar el lienzo
        handler.removeCallbacks(drawRunnable);
        invalidate();
    }
}
