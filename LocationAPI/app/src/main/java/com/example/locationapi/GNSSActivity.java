package com.example.locationapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.Context;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.Manifest;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;


import java.util.ArrayList;

public class GNSSActivity extends AppCompatActivity implements SensorEventListener {
    private LocationManager locationManager; // O Gerente de localização
    private LocationProvider locationProvider; // O provedor de localizações
    private static final int REQUEST_LOCATION = 1;
    private static final String PREFS_NAME = "LocationSettings";
    private static final String COORDINATE_FORMAT_KEY = "coordinate_format";
    private String[] formats = {"[+/-DDD.DDDDD]", "[+/-DDD:MM.MMMMM]", "[+/-DDD:MM:SS.SSSSS]"};
    private int selectedFormatIndex = 0;
    private ImageView compassArrow;

    // Adicionando sensores
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    // sharedPreferences
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.esfera_celeste_layout);

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("SatellitePreferences", Context.MODE_PRIVATE);

        // Inicializando o sensor de rotação
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Inicializando o ImageView da seta
        compassArrow = findViewById(R.id.compassArrow);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        obtemLocationProvider_Permission();

        // Carregar a preferência de formato salva
        loadSelectedFormat();

        // Dialog
        TextView textViewLocation = findViewById(R.id.textviewLocation_id);
        textViewLocation.setOnClickListener(v -> showFormatDialog());

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar o listener do sensor de rotação
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Desregistrar o listener do sensor ao pausar a atividade
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Obter a matriz de rotação a partir do vetor de rotação
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            // Converter a matriz de rotação em ângulos de orientação (azimute, inclinação e rotação)
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // O azimute (rumo) está em radians, precisamos converter para graus
            float azimuthInRadians = orientationAngles[0];
            float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360; // Corrigir valores negativos
            }

            // Exibir o rumo (azimute) na interface
            rotateCompassArrow(azimuthInDegrees);
            mostraRumo(azimuthInDegrees);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não utilizado, mas é necessário implementar esse método
    }

    private void rotateCompassArrow(float azimuthInDegrees) {
        // Rotacionar a imagem da seta com base no valor do azimute
        compassArrow.setRotation(azimuthInDegrees);
    }

    public void obtemLocationProvider_Permission() {
        // Verifica se a aplicação tem acesso a sistema de localização
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // caso tenha permissão para acessar sistema de localização, obtenha o LocationProvider
            locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            // Começa processo de aquisiçãoi de localizações e Satelites
            startLocationAndGNSSUpdates();
        } else {
            // Solicite a permissão
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                // O usuário acabou de dar a permissão
                obtemLocationProvider_Permission();
            } else {
                // O usuário não deu a permissão solicitada
                Toast.makeText(this, getString(R.string.textNoPermition),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void startLocationAndGNSSUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(locationProvider.getName(), 1000, 0.1f, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                mostraLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                LocationListener.super.onStatusChanged(provider, status, extras);
            }
        });
        locationManager.registerGnssStatusCallback(new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                mostraGNSSGrafico(status);
                mostraGNSSScatterPlot(status);
            }
        });
    }

    public void saveSatellitePreferences(boolean gpsChecked, boolean galileoChecked, boolean glonassChecked,
                                         boolean unknownChecked, boolean usedInFixChecked, boolean notUsedInFixChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("gpsChecked", gpsChecked);
        editor.putBoolean("galileoChecked", galileoChecked);
        editor.putBoolean("glonassChecked", glonassChecked);
        editor.putBoolean("unknownChecked", unknownChecked);
        editor.putBoolean("usedInFix", usedInFixChecked);
        editor.putBoolean("notUsedInFix", notUsedInFixChecked);
        editor.apply();
    }

    public boolean getSatellitePreference(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void mostraGNSSGrafico(GnssStatus status) {
        EsferaCelesteView esferaCelesteView = findViewById(R.id.esferacelesteview_id);
        esferaCelesteView.setNewStatus(status);
    }

    public void mostraLocation(Location location) {
        TextView textView = findViewById(R.id.textviewLocation_id);
        String mens = getString(R.string.textLastPosition) + "\n";

        if (location != null) {
            String latitudeFormatted;
            String longitudeFormatted;

            // Formatar a latitude e longitude com base na opção selecionada
            switch (selectedFormatIndex) {
                case 0:
                    // Graus [+/-DDD.DDDDD]
                    latitudeFormatted = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES);
                    longitudeFormatted = Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                    break;
                case 1:
                    // Graus-Minutos [+/-DDD:MM.MMMMM]
                    latitudeFormatted = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
                    longitudeFormatted = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
                    break;
                case 2:
                    // Graus-Minutos-Segundos [+/-DDD:MM:SS.SSSSS]
                    latitudeFormatted = Location.convert(location.getLatitude(), Location.FORMAT_SECONDS);
                    longitudeFormatted = Location.convert(location.getLongitude(), Location.FORMAT_SECONDS);
                    break;
                default:
                    latitudeFormatted = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES);
                    longitudeFormatted = Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                    break;
            }

            mens += "Lat = " + latitudeFormatted + "\n"
                    + "Long = " + longitudeFormatted + "\n"
                    + "Alt (m) = " + location.getAltitude();
        } else {
            mens += getString(R.string.textLocationNotAvailable);
        }

        textView.setText(mens);
    }

    private void mostraRumo(float azimuth) {
        TextView textView = findViewById(R.id.textDisplacement_id);
        String mens = getString(R.string.textHeadingDegrees) + " = " +azimuth;
        textView.setText(mens);
    }

    private void showFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.textChooseLatLon));

        // Recuperar a preferência atual salva
        int selectedFormat = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(COORDINATE_FORMAT_KEY, 0);  // Padrão é 0

        builder.setSingleChoiceItems(formats, selectedFormat, (dialog, which) -> {
            // Salvar o índice selecionado no SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(COORDINATE_FORMAT_KEY, which);
            editor.apply();

            // Atualizar o formato selecionado
            selectedFormatIndex = which;

            // Fechar o diálogo
            dialog.dismiss();

        });

        // Mostrar o diálogo
        builder.create().show();
    }

    private void loadSelectedFormat() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedFormatIndex = sharedPreferences.getInt(COORDINATE_FORMAT_KEY, 0);  // Padrão é 0
    }

    public void mostraGNSSScatterPlot(GnssStatus status) {
        // Recupera as preferências do usuário
        SharedPreferences sharedPreferences = getSharedPreferences("SatellitePreferences", Context.MODE_PRIVATE);
        boolean gpsSelected = sharedPreferences.getBoolean("gpsChecked", true);
        boolean galileoSelected = sharedPreferences.getBoolean("galileoChecked", true);
        boolean glonassSelected = sharedPreferences.getBoolean("glonassChecked", true);
        boolean unknownSelected = sharedPreferences.getBoolean("unknownChecked", true);
        boolean usedInFixSelected = sharedPreferences.getBoolean("usedInFix", true);
        boolean notUsedInFixSelected = sharedPreferences.getBoolean("notUsedInFix", true);

        ScatterChart scatterChart = findViewById(R.id.scatterChart);
        ArrayList<Entry> scatterEntriesGPS = new ArrayList<>();
        ArrayList<Entry> scatterEntriesGlonass = new ArrayList<>();
        ArrayList<Entry> scatterEntriesGalileo = new ArrayList<>();
        ArrayList<Entry> scatterEntriesUnknown = new ArrayList<>();

        if (!gpsSelected && !galileoSelected && !glonassSelected && !unknownSelected) {
            scatterChart.clear();
            scatterChart.invalidate();
            return; // Não continua o processamento
        }


        if (status != null) {
            for (int i = 0; i < status.getSatelliteCount(); i++) {
                int svid = status.getSvid(i); // Obtém o SVID (Satellite Vehicle ID)
                float snr = status.getCn0DbHz(i); // Obtém o (SNR)
                int constellationType = status.getConstellationType(i); // Obtém o tipo de constelação
                boolean usedInFix = status.usedInFix(i);

                if (( usedInFix && !usedInFixSelected) || (!usedInFix && !notUsedInFixSelected)) {
                    continue; // Pula este satélite se não atender os filtros
                }

                // Aplica os filtros das constelações com base nas preferências
                boolean shouldAdd;
                switch (constellationType) {
                    case GnssStatus.CONSTELLATION_GPS:
                        shouldAdd = gpsSelected;
                        if (shouldAdd) {
                            scatterEntriesGPS.add(new Entry(svid, snr)); // GPS
                        }
                        break;
                    case GnssStatus.CONSTELLATION_GLONASS:
                        shouldAdd = glonassSelected;
                        if (shouldAdd) {
                            scatterEntriesGlonass.add(new Entry(svid, snr)); // Glonass
                        }
                        break;
                    case GnssStatus.CONSTELLATION_GALILEO:
                        shouldAdd = galileoSelected;
                        if (shouldAdd) {
                            scatterEntriesGalileo.add(new Entry(svid, snr)); // Galileo
                        }
                        break;
                    default:
                        shouldAdd = unknownSelected;
                        if (shouldAdd) {
                            scatterEntriesUnknown.add(new Entry(svid, snr)); // Desconhecido
                        }
                        break;
                }
            }

            // Criação dos conjuntos de dados para o gráfico
            ScatterData scatterData = new ScatterData();

                if (!scatterEntriesGPS.isEmpty()) {
                    ScatterDataSet scatterDataSetGPS = new ScatterDataSet(scatterEntriesGPS, "GPS");
                    scatterDataSetGPS.setColor(Color.RED);
                    scatterDataSetGPS.setValueTextColor(Color.WHITE);
                    scatterDataSetGPS.setScatterShapeSize(10f);
                    scatterData.addDataSet(scatterDataSetGPS);
                }

                if (!scatterEntriesGlonass.isEmpty()) {
                    ScatterDataSet scatterDataSetGlonass = new ScatterDataSet(scatterEntriesGlonass, "Glonass");
                    scatterDataSetGlonass.setColor(Color.YELLOW);
                    scatterDataSetGlonass.setValueTextColor(Color.WHITE);
                    scatterDataSetGlonass.setScatterShapeSize(10f);
                    scatterData.addDataSet(scatterDataSetGlonass);
                }


                if (!scatterEntriesGalileo.isEmpty()) {
                    ScatterDataSet scatterDataSetGalileo = new ScatterDataSet(scatterEntriesGalileo, "Galileo");
                    scatterDataSetGalileo.setColor(Color.GREEN);
                    scatterDataSetGalileo.setValueTextColor(Color.WHITE);
                    scatterDataSetGalileo.setScatterShapeSize(10f);
                    scatterData.addDataSet(scatterDataSetGalileo);
                }

                if (!scatterEntriesUnknown.isEmpty()) {
                    ScatterDataSet scatterDataSetUnknown = new ScatterDataSet(scatterEntriesUnknown, getString(R.string.unknownCheckBox));
                    scatterDataSetUnknown.setColor(Color.GRAY);
                    scatterDataSetUnknown.setValueTextColor(Color.WHITE);
                    scatterDataSetUnknown.setScatterShapeSize(10f);
                    scatterData.addDataSet(scatterDataSetUnknown);
                }

                // Configura os dados no gráfico
                scatterChart.setData(scatterData);


                // Configurações dos eixos
                XAxis xAxis = scatterChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setTextColor(Color.WHITE);

                YAxis yAxisLeft = scatterChart.getAxisLeft();
                yAxisLeft.setTextColor(Color.WHITE);
                YAxis yAxisRight = scatterChart.getAxisRight();
                yAxisRight.setTextColor(Color.WHITE);

                scatterChart.getLegend().setEnabled(false); // Desabilita a legenda

                // Descrição
                scatterChart.getDescription().setText("SVID x SNR(dBHz)");
                scatterChart.getDescription().setTextColor(Color.WHITE);

                scatterChart.invalidate(); // Atualiza o gráfico
            }
        }
    }
