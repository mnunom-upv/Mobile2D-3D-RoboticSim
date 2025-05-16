package com.robotsimulator.eq3.manual;

import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Room {
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private static final float WALL_HEIGHT = 0.2f; // Altura de las paredes

    private static final float ROOM_SIZE = 3.0f; // Reducido para mantener el cubo más contenido

    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec4 aColor;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  vColor = aColor;" +
                    "}";

    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    public Room() {
        // Vértices para las paredes y el suelo
        float[] vertices = {
                // Suelo
                -ROOM_SIZE, .5F, -ROOM_SIZE,
                ROOM_SIZE, .5F, -ROOM_SIZE,
                ROOM_SIZE, .5F, ROOM_SIZE,
                -ROOM_SIZE, .5F, ROOM_SIZE,

                // Pared izquierda
                -ROOM_SIZE, 0, -ROOM_SIZE,
                -ROOM_SIZE, WALL_HEIGHT, -ROOM_SIZE,  // Usa WALL_HEIGHT
                -ROOM_SIZE, WALL_HEIGHT, ROOM_SIZE,  // Usa WALL_HEIGHT
                -ROOM_SIZE, 0, ROOM_SIZE,

                // Pared derecha
                ROOM_SIZE, 0, -ROOM_SIZE,
                ROOM_SIZE, WALL_HEIGHT, -ROOM_SIZE,  // Usa WALL_HEIGHT
                ROOM_SIZE, WALL_HEIGHT, ROOM_SIZE,  // Usa WALL_HEIGHT
                ROOM_SIZE, 0, ROOM_SIZE,

                // Pared trasera
                -ROOM_SIZE, 0, -ROOM_SIZE,
                ROOM_SIZE, 0, -ROOM_SIZE,
                ROOM_SIZE, WALL_HEIGHT, -ROOM_SIZE,  // Usa WALL_HEIGHT
                -ROOM_SIZE, WALL_HEIGHT, -ROOM_SIZE,

                // Pared frontal
                -ROOM_SIZE, 0, ROOM_SIZE,
                ROOM_SIZE, 0, ROOM_SIZE,
                ROOM_SIZE, WALL_HEIGHT, ROOM_SIZE,  // Usa WALL_HEIGHT
                -ROOM_SIZE, WALL_HEIGHT, ROOM_SIZE
        };


        // Colores para cada vértice (azul claro para las paredes)
        float[] colors = new float[vertices.length * 4 / 3]; // 4 componentes de color por vértice
        for(int i = 0; i < colors.length; i += 4) {
            if (i < 16) { // Suelo
                colors[i] = 0.3f;     // R - gris oscuro
                colors[i + 1] = 0.3f; // G
                colors[i + 2] = 0.3f; // B
                colors[i + 3] = 1.0f; // A
            } else { // Paredes
                colors[i] = 1.0f;     // R - rojo alto
                colors[i + 1] = 1.0f; // G - verde alto
                colors[i + 2] = 0.0f; // B - azul bajo
                colors[i + 3] = 1.0f; // A - opacidad total

            }
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        colorBuffer = ByteBuffer.allocateDirect(colors.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorBuffer.put(colors).position(0);

        initShaders();
    }

    private void initShaders() {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        mProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(mProgram, vertexShader);
        GLES30.glAttachShader(mProgram, fragmentShader);
        GLES30.glLinkProgram(mProgram);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    public void draw(float[] projectionMatrix, float[] viewMatrix) {
        GLES30.glUseProgram(mProgram);

        int positionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        int colorHandle = GLES30.glGetAttribLocation(mProgram, "aColor");

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glEnableVertexAttribArray(colorHandle);

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

        int mvpMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Dibujar las superficies
        for(int i = 0; i < 5; i++) { // 5 superficies: suelo + 4 paredes
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, i * 4, 4);
        }

        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
    }
}