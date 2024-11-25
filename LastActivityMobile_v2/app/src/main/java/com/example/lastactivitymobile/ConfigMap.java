package com.example.lastactivitymobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class ConfigMap extends AppCompatActivity {

    private RadioGroup typeMapRadioGroup;
    private RadioGroup navigationMapRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_map);

        typeMapRadioGroup = findViewById(R.id.type_map_radio);
        navigationMapRadioGroup = findViewById(R.id.nagivation_map_radio);

        // Recuperar preferências existentes
        SharedPreferences preferences = getSharedPreferences("MapPreferences", MODE_PRIVATE);
        int savedMapType = preferences.getInt("mapType", R.id.vetorial_button); // Default: Vetorial
        int savedNavigationMode = preferences.getInt("navigationMode", R.id.northup_button); // Default: North Up

        // Atualizar seleção com base nas preferências
        typeMapRadioGroup.check(savedMapType);
        navigationMapRadioGroup.check(savedNavigationMode);

        // Salvar preferências quando o usuário mudar a seleção
        typeMapRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("mapType", checkedId);
            editor.apply();
        });

        navigationMapRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("navigationMode", checkedId);
            editor.apply();
        });
    }
}
