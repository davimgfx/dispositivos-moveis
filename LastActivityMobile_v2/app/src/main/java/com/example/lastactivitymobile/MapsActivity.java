package com.example.lastactivitymobile;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private TextView velocityText, chronometerText, distanceText;
    private Button startButton, stopButton;

    private long startTime;
    private double totalDistance = 0;
    private Location lastLocation;

    private ArrayList<LatLng> pathPoints = new ArrayList<>();
    private Polyline polyline;

    private boolean isTracking = false;

    private long currentTrilhaId; // ID da trilha atual
    private MyDatabaseHelper databaseHelper; // Helper do banco

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Configuração inicial
        velocityText = findViewById(R.id.velocity_text);
        chronometerText = findViewById(R.id.chronometer_text);
        distanceText = findViewById(R.id.distance_text);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);

        startButton.setOnClickListener(v -> startTracking());
        stopButton.setOnClickListener(v -> stopTracking());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        databaseHelper = new MyDatabaseHelper(this);

        // Configurar callback de localização
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    // Atualizar localização no mapa (independente de rastreamento)
                    updateLocation(location);
                }
            }
        };
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configurar preferências do mapa
        SharedPreferences preferences = getSharedPreferences("MapPreferences", MODE_PRIVATE);
        int mapType = preferences.getInt("mapType", R.id.vetorial_button);
        int navigationMode = preferences.getInt("navigationMode", R.id.northup_button);

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

        // Habilitar a localização do usuário
        enableUserLocation();

        // Iniciar atualizações de localização assim que o mapa estiver pronto
        startLocationUpdates();
    }


    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000) // Atualizar a cada 2 segundos
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void startTracking() {
        if (!isTracking) {
            isTracking = true;
            startTime = SystemClock.elapsedRealtime(); // Resetar o cronômetro
            totalDistance = 0; // Resetar a distância
            lastLocation = null; // Resetar a última localização
            pathPoints.clear(); // Limpar pontos anteriores

            currentTrilhaId = databaseHelper.createTrilha(); // Criar nova trilha e armazenar ID
            Toast.makeText(this, "Trilha iniciada com ID: " + currentTrilhaId, Toast.LENGTH_SHORT).show();

            if (polyline != null) {
                polyline.remove(); // Remover linha do mapa
                polyline = null;
            }
        }
    }


    private void stopTracking() {
        if (isTracking) {
            isTracking = false;

            // Formatar o tempo final
            long elapsedMillis = SystemClock.elapsedRealtime() - startTime;
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    (elapsedMillis / 3600000), (elapsedMillis % 3600000) / 60000, (elapsedMillis % 60000) / 1000);

            databaseHelper.updateTrilha(currentTrilhaId, formattedTime, totalDistance);
            Toast.makeText(this, "Trilha finalizada. Tempo: " + formattedTime, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocation(Location location) {
        // Atualizar apenas visualização no mapa se não estiver rastreando
        if (!isTracking) {
            // Centralizar câmera no usuário
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));

            // Atualizar velocidade e posição
            float speed = location.getSpeed(); // m/s
            velocityText.setText(String.format(Locale.getDefault(), "Velocidade: %.2f m/s", speed));
            lastLocation = location;
            return;
        }

        // A lógica abaixo é executada apenas se `isTracking` for verdadeiro
        if (lastLocation != null) {
            // Calcular distância
            float distance = lastLocation.distanceTo(location);
            totalDistance += distance;

            // Atualizar Polyline no mapa
            LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
            pathPoints.add(newPoint);

            if (polyline == null) {
                polyline = mMap.addPolyline(new PolylineOptions().addAll(pathPoints).width(5).color(0xFF0000FF));
            } else {
                polyline.setPoints(pathPoints);
            }

            // Adicionar ponto no banco
            long elapsedMillis = SystemClock.elapsedRealtime() - startTime;
            String tempo = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    (elapsedMillis / 3600000), (elapsedMillis % 3600000) / 60000, (elapsedMillis % 60000) / 1000);
            databaseHelper.addPonto(currentTrilhaId, tempo, location.getLatitude(),
                    location.getLongitude(), location.getAltitude(), location.getSpeed());

            distanceText.setText(String.format(Locale.getDefault(), "Distância: %.2f m", totalDistance));
        }

        // Atualizar cronômetro
        long elapsedMillis = SystemClock.elapsedRealtime() - startTime;
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) ((elapsedMillis % 3600000) / 60000);
        int seconds = (int) ((elapsedMillis % 60000) / 1000);
        chronometerText.setText(String.format(Locale.getDefault(), "Tempo: %02d:%02d:%02d", hours, minutes, seconds));

        lastLocation = location;
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

            // Atualizar velocidade no modo "Course Up"
            float speed = location.getSpeed(); // Velocidade em m/s
            velocityText.setText(String.format(Locale.getDefault(), "Velocidade: %.2f m/s", speed));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        }
    }
}
