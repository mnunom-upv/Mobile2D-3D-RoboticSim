package com.robotsimulator.eq3.aut;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class ModelLoader {
    public static class ModelData {
        public FloatBuffer vertices;
        public FloatBuffer normals;
        public FloatBuffer texCoords;
        public IntBuffer indices;
        public int numVertices;
        public int numIndices;
    }

    public static ModelData loadObjModel(Context context, String fileName) {
        try {
            // Abrir el archivo desde assets
            InputStream inputStream = context.getAssets().open(fileName);

            // Leer el archivo OBJ
            Obj obj = ObjReader.read(inputStream);

            // Asegurarse de que el modelo tenga normales y coordenadas de textura
            obj = ObjUtils.convertToRenderable(obj);

            // Crear el objeto ModelData
            ModelData modelData = new ModelData();

            // Obtener los datos del modelo
            modelData.vertices = ObjData.getVertices(obj);
            modelData.normals = ObjData.getNormals(obj);
            modelData.texCoords = ObjData.getTexCoords(obj, 2);
            modelData.indices = ObjData.getFaceVertexIndices(obj);

            modelData.numVertices = obj.getNumVertices();
            modelData.numIndices = modelData.indices.capacity();

            // Cerrar el InputStream
            inputStream.close();

            return modelData;
        } catch (IOException e) {
            Log.e("ModelLoader", "Error loading model: " + e.getMessage());
            return null;
        }
    }
}