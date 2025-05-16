package com.robotsimulator.eq3.manual;

import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ManualCube {
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram = -1;
    private float posX = 0, posY = 0, posZ = -1;
    private static final float CUBE_SIZE = 0.1f; // Reducido para que sea más pequeño

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

    public ManualCube() {
        // Vértices para todas las caras del cubo
        float[] vertices = {
                // Frente
                -CUBE_SIZE, -CUBE_SIZE, CUBE_SIZE,   // 0
                CUBE_SIZE, -CUBE_SIZE, CUBE_SIZE,    // 1
                CUBE_SIZE, CUBE_SIZE, CUBE_SIZE,     // 2
                -CUBE_SIZE, CUBE_SIZE, CUBE_SIZE,    // 3

                // Atrás
                -CUBE_SIZE, -CUBE_SIZE, -CUBE_SIZE,  // 4
                CUBE_SIZE, -CUBE_SIZE, -CUBE_SIZE,   // 5
                CUBE_SIZE, CUBE_SIZE, -CUBE_SIZE,    // 6
                -CUBE_SIZE, CUBE_SIZE, -CUBE_SIZE,   // 7

                // Arriba
                -CUBE_SIZE, CUBE_SIZE, -CUBE_SIZE,   // 8
                CUBE_SIZE, CUBE_SIZE, -CUBE_SIZE,    // 9
                CUBE_SIZE, CUBE_SIZE, CUBE_SIZE,     // 10
                -CUBE_SIZE, CUBE_SIZE, CUBE_SIZE,    // 11

                // Abajo
                -CUBE_SIZE, -CUBE_SIZE, -CUBE_SIZE,  // 12
                CUBE_SIZE, -CUBE_SIZE, -CUBE_SIZE,   // 13
                CUBE_SIZE, -CUBE_SIZE, CUBE_SIZE,    // 14
                -CUBE_SIZE, -CUBE_SIZE, CUBE_SIZE,   // 15

                // Derecha
                CUBE_SIZE, -CUBE_SIZE, -CUBE_SIZE,   // 16
                CUBE_SIZE, CUBE_SIZE, -CUBE_SIZE,    // 17
                CUBE_SIZE, CUBE_SIZE, CUBE_SIZE,     // 18
                CUBE_SIZE, -CUBE_SIZE, CUBE_SIZE,    // 19

                // Izquierda
                -CUBE_SIZE, -CUBE_SIZE, -CUBE_SIZE,  // 20
                -CUBE_SIZE, CUBE_SIZE, -CUBE_SIZE,   // 21
                -CUBE_SIZE, CUBE_SIZE, CUBE_SIZE,    // 22
                -CUBE_SIZE, -CUBE_SIZE, CUBE_SIZE    // 23
        };

        // Color verde brillante para el cubo
        float[] colors = new float[vertices.length * 4 / 3];
        for(int i = 0; i < colors.length; i += 4) {
            colors[i] = 0.0f;     // R
            colors[i + 1] = 0.8f; // G - verde brillante
            colors[i + 2] = 0.2f; // B
            colors[i + 3] = 1.0f; // A
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

    public void setPosition(float x, float y, float z) {
        posX = x;
        posY = y;
        posZ = z;
    }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }

    public void draw(float[] projectionMatrix, float[] viewMatrix) {
        GLES30.glUseProgram(mProgram);

        int positionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        int colorHandle = GLES30.glGetAttribLocation(mProgram, "aColor");

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glEnableVertexAttribArray(colorHandle);

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

        int mvpMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);

        float[] mvpMatrix = new float[16];
        float[] tempMatrix = new float[16];
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Dibujar todas las caras del cubo
        for(int i = 0; i < 6; i++) {
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, i * 4, 4);
        }

        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
    }
}