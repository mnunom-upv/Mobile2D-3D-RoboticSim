package com.robotsimulator.eq3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.robotsimulator.R;
import com.robotsimulator.eq3.aut.MainActivity;

import java.util.ArrayList;

public class DrawingActivity extends AppCompatActivity {
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_drawing);

        drawingView = findViewById(R.id.drawingView);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnGenerate3D = findViewById(R.id.btnGenerate3D);

        btnClear.setOnClickListener(v -> drawingView.clearCanvas());

        btnGenerate3D.setOnClickListener(v -> {
            ArrayList<Float> pathPoints = drawingView.getPathPoints();
            if (!pathPoints.isEmpty()) {
                Intent intent = new Intent(this, MainActivity.class); // Cambio aquí: InitialActivity -> MainActivity
                intent.putExtra("mode", "manual");
                intent.putExtra("pathPoints", pathPoints);
                startActivity(intent);
            }
        });
    }
}