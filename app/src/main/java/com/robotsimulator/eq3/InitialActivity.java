package com.robotsimulator.eq3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.robotsimulator.R;
import com.robotsimulator.eq3.manual.ManualActivity;


public class InitialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        Button btnAutomatic = findViewById(R.id.btnAutomatic);
        Button btnManual = findViewById(R.id.btnManual);

        btnAutomatic.setOnClickListener(v -> {
            Intent intent = new Intent(this, DrawingActivity.class);
            startActivity(intent);


        });

        btnManual.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualActivity.class);
            startActivity(intent);

        });
    }
}