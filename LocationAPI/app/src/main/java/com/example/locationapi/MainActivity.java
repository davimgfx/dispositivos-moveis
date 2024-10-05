package com.example.locationapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button_gnss=findViewById(R.id.button_gnss);
        button_gnss.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i=new Intent(getApplicationContext(), GNSSActivity.class);
                startActivity(i);
            }
        });
    }
}