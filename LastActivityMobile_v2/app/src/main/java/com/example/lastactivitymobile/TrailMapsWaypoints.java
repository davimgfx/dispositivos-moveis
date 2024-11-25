package com.example.lastactivitymobile;

import androidx.fragment.app.FragmentActivity;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.lastactivitymobile.databinding.ActivityTrailMapsWaypointsBinding;

public class TrailMapsWaypoints extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private MyDatabaseHelper myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trail_maps_waypoints);

        // Inicializar banco de dados
        myDB = new MyDatabaseHelper(this);

        // Recuperar o ID da trilha do SharedPreferences
        int trilhaId = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                .getInt("SELECTED_TRILHA_ID", -1); // Valor padrão é -1 caso não encontre

        if (trilhaId == -1) {
            Toast.makeText(this, "Nenhuma trilha selecionada.", Toast.LENGTH_SHORT).show();
            finish(); // Fecha a atividade se não houver trilha selecionada
            return;
        }
        updateTrailInfo(trilhaId);

        // Configurar o mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;

            // Aplica as preferências de mapa
            applyMapPreferences();
            addWaypoints(trilhaId); // Use o ID recuperado
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // A configuração do mapa será feita na callback acima
        // Configurar preferências do mapa
        SharedPreferences preferences = getSharedPreferences("MapPreferences", MODE_PRIVATE);
        int mapType = preferences.getInt("mapType", R.id.vetorial_button);
        int navigationMode = preferences.getInt("navigationMode", R.id.northup_button);

        // Configurar modo de satelite
        if (mapType == R.id.vetorial_button) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mapType == R.id.satelite_button) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }

        // Configurar modo de navegação
        if (navigationMode == R.id.courseup_button) {
            // Course Up: a câmera segue o movimento do usuário
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.setOnMyLocationChangeListener(location -> updateCameraForCourseUp(location));
        } else {
            // North Up: o mapa está sempre orientado ao norte
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.setOnMyLocationChangeListener(location -> updateCameraForNorthUp(location));
        }
    }

    private void applyMapPreferences() {
        // Recuperar preferências do SharedPreferences
        SharedPreferences preferences = getSharedPreferences("MapPreferences", MODE_PRIVATE);
        int mapType = preferences.getInt("mapType", R.id.vetorial_button); // Default: Vetorial
        int navigationMode = preferences.getInt("navigationMode", R.id.northup_button); // Default: North Up

        if (mapType == R.id.vetorial_button) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mapType == R.id.satelite_button) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }

        // Aplicar o modo de navegação
        if (navigationMode == R.id.courseup_button) {
            mMap.getUiSettings().setRotateGesturesEnabled(true); // Permite rotação conforme o curso
        } else {
            mMap.getUiSettings().setRotateGesturesEnabled(false); // Padrão: North Up
        }
    }

    private void addWaypoints(int trilhaId) {
        Cursor cursor = myDB.readAllDataPontosFromTrilha(trilhaId);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_PONTOS_LAT));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_PONTOS_LONG));
                String tempo = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_PONTOS_TEMPO));

                // Adicionar marcador no mapa
                LatLng ponto = new LatLng(lat, lon);
                mMap.addMarker(new MarkerOptions().position(ponto).title("Tempo: " + tempo));
            }

            // Ajustar o zoom para englobar todos os pontos
            if (cursor.moveToFirst()) {
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_PONTOS_LAT));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_PONTOS_LONG));
                LatLng primeiroPonto = new LatLng(lat, lon);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(primeiroPonto, 15));
            }
            cursor.close();
        } else {
            Toast.makeText(this, "Nenhum ponto encontrado para a trilha.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTrailInfo(int trilhaId) {
        Cursor trailCursor = myDB.readTrilhaById(trilhaId); // Método para buscar a trilha pelo ID
        Cursor pointsCursor = myDB.readAllDataPontosFromTrilha(trilhaId);

        if (trailCursor != null && trailCursor.moveToFirst()) {
            String startDate = trailCursor.getString(trailCursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_TRILHA_DATA));
            String startTime = trailCursor.getString(trailCursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_TRILHA_HORA));
            double totalDistance = trailCursor.getDouble(trailCursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_TRILHA_DISTANCIA));
            String totalTime = trailCursor.getString(trailCursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_TRILHA_TEMPO_TOTAL));

            // Velocidade media
            double totalSpeed = 0;
            int pointCount = 0;

            if (pointsCursor != null) {
                while (pointsCursor.moveToNext()) {
                    totalSpeed += pointsCursor.getDouble(pointsCursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_PONTOS_VELOCIDADE));
                    pointCount++;
                }
                pointsCursor.close();
            }

            double averageSpeed = pointCount > 0 ? totalSpeed / pointCount : 0;

            // Atualizar os TextViews no painel
            ((TextView) findViewById(R.id.trail_id_text)).setText("Trilha: ID " + trilhaId);
            ((TextView) findViewById(R.id.start_date_text)).setText("Data de Início: " + startDate);
            ((TextView) findViewById(R.id.start_time_text)).setText("Horário de Início: " + startTime);
            ((TextView) findViewById(R.id.average_velocity_text)).setText(String.format("Velocidade Média: %.2f km/h", averageSpeed));
            ((TextView) findViewById(R.id.total_distance_text)).setText(String.format("Distância Total: %.2f m", totalDistance));
            ((TextView) findViewById(R.id.trail_duration_text)).setText("Duração da Trilha: " + totalTime);
        }

        if (trailCursor != null) trailCursor.close();
    }

    private void updateCameraForNorthUp(Location location) {
        if (location != null) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16)); // Sempre aponta para o norte
        }
    }

    private void updateCameraForCourseUp(Location location) {
        if (location != null) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            float bearing = location.getBearing(); // Direção atual do movimento
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLocation)
                    .zoom(16)
                    .bearing(bearing) // Gira o mapa para seguir o movimento
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }


}