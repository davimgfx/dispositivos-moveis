package com.example.lastactivitymobile;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button configButton = (Button) findViewById(R.id.config_map_button);
        Button mapButton = (Button) findViewById(R.id.map_button);
        Button seeTrailListButton = (Button) findViewById(R.id.see_trail_button);

        configButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);
        seeTrailListButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        if (view.getId() == R.id.config_map_button) {
            Intent intent = new Intent(this, ConfigMap.class);
            startActivity(intent);
        } else if(view.getId()==R.id.map_button){
           Intent intent = new Intent(this, MapsActivity.class);
           startActivity(intent);
       } else if (view.getId()==R.id.see_trail_button){
            Intent intent = new Intent(this, TrailListActivity.class);
            startActivity(intent);
        }
    }

}