package com.robotsimulator;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.robotsimulator.eq2.WelcomeActivity;
import com.robotsimulator.eq3.InitialActivity;
import com.robotsimulator.ind.SplashActivity;



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btnind1 = findViewById(R.id.ind1);
        Button btneq2 = findViewById(R.id.eq2);
        Button btneq3 = findViewById(R.id.eq3);


        btnind1.setOnClickListener(v -> {
            Intent intent = new Intent(this, SplashActivity.class);
            startActivity(intent);


        });
        btneq2.setOnClickListener(v -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);


        });

        btneq3.setOnClickListener(v -> {
            Intent intent = new Intent(this, InitialActivity.class);
            startActivity(intent);

        });
    }
}