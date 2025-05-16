package com.robotsimulator.eq3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DrawingView extends View {
    private Path drawPath;
    private Paint drawPaint, canvasPaint, borderPaint;
    private Canvas drawCanvas;
    private android.graphics.Bitmap canvasBitmap;
    private ArrayList<Float> pathPoints;
    private RectF drawingArea;
    private float drawingSize;
    private PointF lastPoint;
    private static final float MIN_DISTANCE = 5f; // Distancia mínima entre puntos

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        pathPoints = new ArrayList<>();
        lastPoint = new PointF();

        drawPaint = new Paint();
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        borderPaint = new Paint();
        borderPaint.setColor(Color.GREEN);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    private void addNormalizedPoint(float touchX, float touchY) {
        if (!drawingArea.contains(touchX, touchY)) return;

        float normalizedX = (touchX - drawingArea.left) / drawingSize;
        float normalizedY = (touchY - drawingArea.top) / drawingSize;

        // Si es el primer punto o si está lo suficientemente lejos del último punto
        if (pathPoints.isEmpty() || getDistance(touchX, touchY, lastPoint.x, lastPoint.y) >= MIN_DISTANCE) {
            pathPoints.add(normalizedX);
            pathPoints.add(normalizedY);
            lastPoint.set(touchX, touchY);

            // Agregar puntos intermedios si hay un punto anterior
            if (pathPoints.size() >= 4) {
                float prevX = pathPoints.get(pathPoints.size() - 4);
                float prevY = pathPoints.get(pathPoints.size() - 3);
                interpolatePoints(prevX, prevY, normalizedX, normalizedY);
            }
        }
    }

    private void interpolatePoints(float startX, float startY, float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(2, (int)(distance * 20)); // Ajusta el 20 para más o menos puntos

        for (int i = 1; i < steps; i++) {
            float t = (float) i / steps;
            float x = startX + dx * t;
            float y = startY + dy * t;

            if (x >= 0 && x <= 1 && y >= 0 && y <= 1) {
                pathPoints.add(x);
                pathPoints.add(y);
            }
        }
    }

    private float getDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);

        drawingSize = Math.min(w, h) * 0.8f;
        float left = (w - drawingSize) / 2;
        float top = (h - drawingSize) / 2;
        drawingArea = new RectF(left, top, left + drawingSize, top + drawingSize);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                lastPoint.set(touchX, touchY);
                addNormalizedPoint(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                addNormalizedPoint(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawRect(drawingArea, borderPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    public void clearCanvas() {
        drawCanvas.drawColor(Color.WHITE);
        pathPoints.clear();
        drawPath.reset();
        invalidate();
    }

    public ArrayList<Float> getPathPoints() {
        return pathPoints;
    }
}