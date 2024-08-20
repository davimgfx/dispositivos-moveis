package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity  implements  View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button botao1 = (Button) findViewById(R.id.button_1);
        Button botaoSobre = (Button) findViewById(R.id.button_sobre);
        Button botaoSair = (Button) findViewById(R.id.button_sair);

        botao1.setOnClickListener(this);
        botaoSobre.setOnClickListener(this);
        botaoSair.setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        int id = view.getId();

        if(id == R.id.button_1){

        }
        if (id == R.id.button_sobre){

        }

        if(id == R.id.button_sair){
            finish();
        }
    }
}