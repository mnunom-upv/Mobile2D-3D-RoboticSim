package com.robotsimulator.eq3.aut;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class Terrain {
    private int mProgram = -1;
    private int currentTrackType = 0;
    private FloatBuffer vertexBuffer;
    private int[] vbo = new int[1];
    private static final int COORDS_PER_VERTEX = 3;
    private ArrayList<Float> customTrackPoints;
    private boolean isCustomTrack = false;

    private Trail trail;


    public Terrain() {
        // Inicializar con la primera pista
        float[] terrainVertices = generateTerrainVertices();
        initializeBuffers(terrainVertices);
    }

    public Terrain(ArrayList<Float> pathPoints) {
        this.customTrackPoints = pathPoints;
        this.isCustomTrack = true;
        float[] terrainVertices = generateTerrainVertices();
        initializeBuffers(terrainVertices);
    }

    private void initializeBuffers(float[] vertices) {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                vertexBuffer.capacity() * 4,
                vertexBuffer,
                GLES30.GL_STATIC_DRAW);
    }

    public void draw(float[] projectionMatrix, float[] viewMatrix) {
        try {
            if (mProgram == -1) {
                initializeShaders();
            }

            GLES30.glUseProgram(mProgram);

            // Obtener las ubicaciones de los atributos y uniformes
            int positionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
            int mvpMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");

            // Calcular la matriz MVP
            float[] mvpMatrix = new float[16];
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            // Vincular el VBO y configurar el atributo de posición
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
            GLES30.glEnableVertexAttribArray(positionHandle);
            GLES30.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false, 0, 0);

            // Dibujar el terreno
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexBuffer.capacity() / COORDS_PER_VERTEX);

            // Limpiar
            GLES30.glDisableVertexAttribArray(positionHandle);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
            GLES30.glUseProgram(0);
        } catch (Exception e) {
            Log.e("Terrain", "Error en draw: " + e.getMessage());
        }
    }

    private void initializeShaders() {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(1.0, 0.4, 0.7, 1.0);" +  // Color rosa
                        "}";

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES30.glCreateProgram();
        if (mProgram != 0) {
            GLES30.glAttachShader(mProgram, vertexShader);
            GLES30.glAttachShader(mProgram, fragmentShader);
            GLES30.glLinkProgram(mProgram);
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        if (shader != 0) {
            GLES30.glShaderSource(shader, shaderCode);
            GLES30.glCompileShader(shader);

            int[] compiled = new int[1];
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Terrain", "Error al compilar shader: " +
                        GLES30.glGetShaderInfoLog(shader));
                GLES30.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private float[] generateTerrainVertices() {
        if (isCustomTrack) {
            return generateCustomTrackVertices();
        }

        // El código original de generación de terreno para el modo automático
        int width = 20;
        int depth = 20;
        float[] vertices = new float[width * depth * 6 * 3];
        int index = 0;

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                boolean isTrack = false;

                switch(currentTrackType) {
                    case 0: // Pista recta
                        isTrack = (x >= 8 && x <= 12);
                        break;
                    case 1: // Zigzag
                        int offset = (z / 4) % 2 == 0 ? 5 : 10;
                        isTrack = (x >= offset && x <= offset + 4);
                        break;
                    case 2: // Circular
                        float dx = x - width/2.0f;
                        float dz = z - depth/2.0f;
                        float dist = (float)Math.sqrt(dx*dx + dz*dz);
                        isTrack = (dist >= 6 && dist <= 8);
                        break;
                    case 3: // Curvas
                        float centerX = (float)(width/2 + Math.sin(z * 0.3) * 5);
                        isTrack = Math.abs(x - centerX) <= 2;
                        break;
                }

                float height = isTrack ? 0.0f : 0.2f;

                // Agregar vértices para el cuadrado actual
                addSquareVertices(vertices, index, x, height, z);
                index += 18; // 6 vértices * 3 coordenadas
            }
        }
        return vertices;
    }
    private float[] generateCustomTrackVertices() {
        int width = 60;
        int depth = 60;
        float wallHeight = 0.5f;
        float[][] heightMap = new float[width][depth];
        boolean[][] isTrack = new boolean[width][depth];
        ArrayList<Float> verticesList = new ArrayList<>();

        // Inicializar mapas
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                heightMap[x][z] = 0.0f;
                isTrack[x][z] = false;
            }
        }

        // Procesar puntos del dibujo
        for (int i = 0; i < customTrackPoints.size() - 3; i += 2) {
            float x1 = customTrackPoints.get(i) * width;
            float z1 = customTrackPoints.get(i + 1) * depth;
            float x2 = customTrackPoints.get(i + 2) * width;
            float z2 = customTrackPoints.get(i + 3) * depth;

            // Interpolar entre puntos para rellenar espacios
            float steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1)) * 2;
            for (float t = 0; t <= 1; t += 1/steps) {
                float x = x1 + (x2 - x1) * t;
                float z = z1 + (z2 - z1) * t;

                int baseX = (int)x;
                int baseZ = (int)z;

                // Crear un área más ancha y suave alrededor del punto
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int newX = baseX + dx;
                        int newZ = baseZ + dz;

                        if (newX >= 0 && newX < width && newZ >= 0 && newZ < depth) {
                            // Calcular distancia al centro del punto
                            float distX = dx / 2.0f;
                            float distZ = dz / 2.0f;
                            float dist = (float)Math.sqrt(distX * distX + distZ * distZ);

                            // Crear un efecto más suave y redondeado
                            if (dist <= 1.0f) {
                                float influence = 1.0f - (dist * dist);
                                heightMap[newX][newZ] = Math.max(heightMap[newX][newZ], influence * wallHeight);
                                isTrack[newX][newZ] = true;
                            }
                        }
                    }
                }
            }
        }

        // Suavizar el heightMap
        float[][] smoothedMap = new float[width][depth];
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < depth - 1; z++) {
                float sum = 0;
                int count = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        sum += heightMap[x + dx][z + dz];
                        count++;
                    }
                }
                smoothedMap[x][z] = sum / count;
            }
        }

        // Generar vértices con el heightMap suavizado
        float blockSize = 0.333f;
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                if (isTrack[x][z] && smoothedMap[x][z] > 0.01f) {
                    float xPos = x * blockSize;
                    float zPos = z * blockSize;
                    addCubeVertices(verticesList, xPos, smoothedMap[x][z], zPos, blockSize);
                }
            }
        }

        float[] vertices = new float[verticesList.size()];
        for (int i = 0; i < verticesList.size(); i++) {
            vertices[i] = verticesList.get(i);
        }

        return vertices;
    }

    private void addCubeVertices(ArrayList<Float> vertices, float x, float height, float z, float size) {
        float halfSize = size * 0.5f;

        // Cara superior (con altura variable)
        addQuadVertices(vertices,
                x, height, z,
                x + size, height, z,
                x + size, height, z + size,
                x, height, z + size);

        // Caras laterales
        if (height > 0) {
            // Frente
            addQuadVertices(vertices,
                    x, 0, z,
                    x + size, 0, z,
                    x + size, height, z,
                    x, height, z);

            // Atrás
            addQuadVertices(vertices,
                    x, 0, z + size,
                    x + size, 0, z + size,
                    x + size, height, z + size,
                    x, height, z + size);

            // Lados
            addQuadVertices(vertices,
                    x, 0, z,
                    x, 0, z + size,
                    x, height, z + size,
                    x, height, z);

            addQuadVertices(vertices,
                    x + size, 0, z,
                    x + size, 0, z + size,
                    x + size, height, z + size,
                    x + size, height, z);
        }
    }
    private void addQuadVertices(ArrayList<Float> vertices,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4) {
        // Primer triángulo
        vertices.add(x1); vertices.add(y1); vertices.add(z1);
        vertices.add(x2); vertices.add(y2); vertices.add(z2);
        vertices.add(x3); vertices.add(y3); vertices.add(z3);

        // Segundo triángulo
        vertices.add(x1); vertices.add(y1); vertices.add(z1);
        vertices.add(x3); vertices.add(y3); vertices.add(z3);
        vertices.add(x4); vertices.add(y4); vertices.add(z4);
    }



    private void addSquareVertices(float[] vertices, int index, float x, float height, float z) {
        // Primer triángulo
        vertices[index] = x;
        vertices[index + 1] = height;
        vertices[index + 2] = z;

        vertices[index + 3] = x + 1;
        vertices[index + 4] = height;
        vertices[index + 5] = z;

        vertices[index + 6] = x;
        vertices[index + 7] = height;
        vertices[index + 8] = z + 1;

        // Segundo triángulo
        vertices[index + 9] = x + 1;
        vertices[index + 10] = height;
        vertices[index + 11] = z;

        vertices[index + 12] = x + 1;
        vertices[index + 13] = height;
        vertices[index + 14] = z + 1;

        vertices[index + 15] = x;
        vertices[index + 16] = height;
        vertices[index + 17] = z + 1;
    }


}