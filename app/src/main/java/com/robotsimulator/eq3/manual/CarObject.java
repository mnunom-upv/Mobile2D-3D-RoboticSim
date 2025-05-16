package com.robotsimulator.eq3.manual;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CarObject {
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer texCoordBuffer;
    private IntBuffer indexBuffer;
    private int mProgram = -1;
    private float posX, posY, posZ;
    private int numIndices;
    private boolean isPlaced = false;
    private static final int COORDS_PER_VERTEX = 3;

    public CarObject(Context context) {
        // Cargar el modelo
        ModelLoader.ModelData modelData = ModelLoader.loadObjModel(context, "carroapp.obj");
        if (modelData != null) {
            this.vertexBuffer = modelData.vertices;
            this.normalBuffer = modelData.normals;
            this.texCoordBuffer = modelData.texCoords;
            this.indexBuffer = modelData.indices;
            this.numIndices = modelData.numIndices;
        }
    }

    private void initializeShaders() {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uModelMatrix;\n" +
                        "attribute vec4 vPosition;\n" +
                        "attribute vec3 vNormal;\n" +
                        "varying vec3 fNormal;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * vPosition;\n" +
                        "  fNormal = (uModelMatrix * vec4(vNormal, 0.0)).xyz;\n" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;\n" +
                        "varying vec3 fNormal;\n" +
                        "void main() {\n" +
                        "  vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));\n" +
                        "  float diff = max(dot(normalize(fNormal), lightDir), 0.0);\n" +
                        "  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0) * (0.3 + 0.7 * diff);\n" +
                        "}";

        // Crear y compilar shaders
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Crear el programa
        mProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(mProgram, vertexShader);
        GLES30.glAttachShader(mProgram, fragmentShader);
        GLES30.glLinkProgram(mProgram);
    }

    public void setPosition(float x, float y, float z) {
        posX = x;
        posY = y;
        posZ = z;
        isPlaced = true;
    }

    public void draw(float[] projectionMatrix, float[] viewMatrix) {
        if (!isPlaced || mProgram == -1) {
            if (mProgram == -1) {
                initializeShaders();
            }
            return;
        }

        GLES30.glUseProgram(mProgram);

        // Obtener handles
        int positionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        int normalHandle = GLES30.glGetAttribLocation(mProgram, "vNormal");
        int mvpMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        int modelMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uModelMatrix");

        // Preparar matrices
        float[] modelMatrix = new float[16];
        float[] mvpMatrix = new float[16];
        float[] tempMatrix = new float[16];

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);

        // Ajusta la escala si es necesario
       // Matrix.scaleM(modelMatrix, 0, 0.1f, 0.1f, 0.1f);

        Matrix.scaleM(modelMatrix, 0, 0.2f, 0.2f, 0.1f); // Ajusta escala
        Matrix.rotateM(modelMatrix, 0, 180, 0, 1, 0);

        // Calcular MVP
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        // Pasar matrices a los shaders
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);

        // Habilitar atributos
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glEnableVertexAttribArray(normalHandle);

        // Preparar los datos
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 0, normalBuffer);

        // Dibujar
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, numIndices, GLES30.GL_UNSIGNED_SHORT, indexBuffer);

        // Deshabilitar atributos
        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(normalHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    // Getters
    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
    public boolean isPlaced() { return isPlaced; }
}