package com.robotsimulator.eq3.aut;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GameObject {
    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private int mProgram = -1;
    private int[] vbo = new int[1];
    private int[] ibo = new int[1];
    private static final int COORDS_PER_VERTEX = 3;
    private final float[] vertices;
    private final short[] indices;
    private float posX, posY, posZ;
    private boolean initialized = false;

    public GameObject(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        // Preparar los datos de vértices
        vertices = new float[] {
                // Frente
                -0.5f, -0.5f,  0.5f,  // 0
                0.5f, -0.5f,  0.5f,  // 1
                0.5f,  0.5f,  0.5f,  // 2
                -0.5f,  0.5f,  0.5f,  // 3
                // Atrás
                -0.5f, -0.5f, -0.5f,  // 4
                0.5f, -0.5f, -0.5f,  // 5
                0.5f,  0.5f, -0.5f,  // 6
                -0.5f,  0.5f, -0.5f   // 7
        };

        indices = new short[] {
                0, 1, 2, 2, 3, 0,  // Frente
                1, 5, 6, 6, 2, 1,  // Derecha
                5, 4, 7, 7, 6, 5,  // Atrás
                4, 0, 3, 3, 7, 4,  // Izquierda
                3, 2, 6, 6, 7, 3,  // Arriba
                4, 5, 1, 1, 0, 4   // Abajo
        };

        // Preparar buffers
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        indexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(indices).position(0);
    }

    private void initialize() {
        if (initialized) return;

        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);" + // Rojo brillante
                        "}";

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(mProgram, vertexShader);
        GLES30.glAttachShader(mProgram, fragmentShader);
        GLES30.glLinkProgram(mProgram);

        // Generar y configurar VBO
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                vertexBuffer.capacity() * 4,
                vertexBuffer,
                GLES30.GL_STATIC_DRAW);

        // Generar y configurar IBO
        GLES30.glGenBuffers(1, ibo, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
                indexBuffer.capacity() * 2,
                indexBuffer,
                GLES30.GL_STATIC_DRAW);

        initialized = true;
    }

    public void draw(float[] projectionMatrix, float[] viewMatrix) {
        try {
            if (!initialized) {
                initialize();
            }


            GLES30.glUseProgram(mProgram);

            int positionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
            int mvpMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");

            // Matriz modelo para posición y escala
            float[] modelMatrix = new float[16];
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, posX, posY + 0.5f, posZ);
            // Calcular MVP
            float[] mvpMatrix = new float[16];
            float[] tempMatrix = new float[16];
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
            GLES30.glEnableVertexAttribArray(positionHandle);
            GLES30.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false, 0, 0);

            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, indices.length,
                    GLES30.GL_UNSIGNED_SHORT, 0);

            GLES30.glDisableVertexAttribArray(positionHandle);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
            GLES30.glUseProgram(0);
        } catch (Exception e) {
            Log.e("GameObject", "Error en draw: " + e.getMessage());
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        // Verificar compilación
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("GameObject", "Error compilando shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // Getters
    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
}