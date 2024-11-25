package com.example.lastactivitymobile;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class TrailListActivity extends AppCompatActivity {

    MyDatabaseHelper myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trail_list);

        myDB = new MyDatabaseHelper(TrailListActivity.this);

        storeDataInView();
    }

    public void storeDataInView() {
        Cursor trilhaCursor = myDB.getAllTrilhas();

        if (trilhaCursor == null || trilhaCursor.getCount() == 0) {
            Toast.makeText(this, "Sem dados disponíveis", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = findViewById(R.id.trail_list_layout); // Pai onde os itens serão adicionados

        while (trilhaCursor.moveToNext()) {
            int trilhaId = trilhaCursor.getInt(trilhaCursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_TRILHA_ID));

            // Criar um contêiner para cada trilha
            LinearLayout trailContainer = new LinearLayout(this);
            trailContainer.setOrientation(LinearLayout.VERTICAL);
            trailContainer.setPadding(16, 16, 16, 16);

            // Criar o título da trilha
            TextView trailTitle = new TextView(this);
            trailTitle.setText("Trilha " + trilhaId);
            trailTitle.setTextSize(18);
            trailTitle.setPadding(8, 8, 8, 8);
            trailTitle.setOnClickListener(view -> {
                saveSelectedTrailId(trilhaId);
                openTrailMap();
            });

            // Criar o botão "Ver todos os pontos"
            Button toggleButton = new Button(this);
            toggleButton.setText("Ver todos os pontos");
            toggleButton.setPadding(8, 8, 8, 8);

            // Layout para os pontos
            LinearLayout pointsLayout = new LinearLayout(this);
            pointsLayout.setOrientation(LinearLayout.VERTICAL);
            pointsLayout.setVisibility(View.GONE); // Inicialmente escondido

            // Adicionar os pontos ao layout
            String[] pontos = myDB.getPontosFormatted(trilhaId).split(", ");
            for (String ponto : pontos) {
                TextView pointText = new TextView(this);
                pointText.setText(ponto);
                pointText.setPadding(8, 4, 8, 4);
                pointsLayout.addView(pointText);
            }

            // Alternar visibilidade dos pontos ao clicar no botão
            toggleButton.setOnClickListener(view -> {
                if (pointsLayout.getVisibility() == View.GONE) {
                    pointsLayout.setVisibility(View.VISIBLE);
                    toggleButton.setText("Não mostrar todos os pontos");
                } else {
                    pointsLayout.setVisibility(View.GONE);
                    toggleButton.setText("Ver todos os pontos");
                }
            });

            // Adicionar os elementos ao contêiner da trilha
            trailContainer.addView(trailTitle);
            trailContainer.addView(toggleButton);
            trailContainer.addView(pointsLayout);

            // Adicionar o contêiner ao layout principal
            layout.addView(trailContainer);
        }

        trilhaCursor.close();
    }


    private void saveSelectedTrailId(int trilhaId) {
        getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                .edit()
                .putInt("SELECTED_TRILHA_ID", trilhaId)
                .apply();

        Toast.makeText(this, "Trilha " + trilhaId + " selecionada!", Toast.LENGTH_SHORT).show();
    }

    private void openTrailMap() {
        Intent intent = new Intent(this, TrailMapsWaypoints.class);
        startActivity(intent);
    }


}