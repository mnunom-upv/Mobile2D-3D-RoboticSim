package com.robotsimulator.eq3.aut;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Trail {
    private static final String TAG = "Trail";
    private FloatBuffer vertexBuffer;
    private int mProgram = -1;
    public List<Vector3fA> points;
    private static final int COORDS_PER_VERTEX = 3;

    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    "void main() {" +
                    "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);" +
                    "}";

    public Trail() {
        points = new ArrayList<>();
        // Inicializar con un punto inicial en el origen
        //points.add(new Vector3fA(0f, 0f, 0f));
        initShaders();
        updateVertexBuffer();
    }




    private void initShaders() {
        try {
            int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
            int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

            mProgram = GLES30.glCreateProgram();
            GLES30.glAttachShader(mProgram, vertexShader);
            GLES30.glAttachShader(mProgram, fragmentShader);
            GLES30.glLinkProgram(mProgram);

            int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(mProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES30.GL_TRUE) {
                Log.e(TAG, "Error al enlazar programa: " + GLES30.glGetProgramInfoLog(mProgram));
                GLES30.glDeleteProgram(mProgram);
                throw new RuntimeException("Error al enlazar programa");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar shaders", e);
            throw new RuntimeException("Error al inicializar shaders", e);
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error al compilar shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Error al compilar shader");
        }
        return shader;
    }

    public void addPoint(float x, float y, float z) {
        points.add(new Vector3fA(x, y, z));
        updateVertexBuffer();
    }

    private void updateVertexBuffer() {
        if (points.isEmpty()) {
            Log.d(TAG, "No hay puntos para actualizar el buffer");
            return;
        }

        float[] vertices = new float[points.size() * COORDS_PER_VERTEX];
        for (int i = 0; i < points.size(); i++) {
            vertices[i * COORDS_PER_VERTEX] = points.get(i).x;
            vertices[i * COORDS_PER_VERTEX + 1] = points.get(i).y;
            vertices[i * COORDS_PER_VERTEX + 2] = points.get(i).z;
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);
    }

    public void draw(float[] projectionMatrix, float[] viewMatrix) {
        if (vertexBuffer == null || points.size() < 2) {
            return;
        }

        try {
            GLES30.glUseProgram(mProgram);

            int positionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
            GLES30.glEnableVertexAttribArray(positionHandle);
            GLES30.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false, 0, vertexBuffer);

            int mvpMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
            float[] mvpMatrix = new float[16];
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            // Establecer el ancho de línea (si está disponible en el dispositivo)
            GLES30.glLineWidth(10.0f);

            // Dibujar la línea
            GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, points.size());

            GLES30.glDisableVertexAttribArray(positionHandle);
        } catch (Exception e) {
            Log.e(TAG, "Error al dibujar el rastro", e);
        }
    }
}