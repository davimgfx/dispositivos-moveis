package com.example.lastactivitymobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    // public static final String DATABASE_NAME = "trails2.db";
    public static final String DATABASE_NAME = "trailstesteteste.db";
    public static final int DATABASE_VERSION = 1;

    // Tabela Trilha
    public static final String TABLE_TRILHA = "trilha";
    public static final String COLUMN_TRILHA_ID = "_id";
    public static final String COLUMN_TRILHA_TEMPO_TOTAL = "tempo_total";
    public static final String COLUMN_TRILHA_DISTANCIA = "distancia";
    public static final String COLUMN_TRILHA_DATA = "data";
    public static final String COLUMN_TRILHA_HORA = "hora";

    // Tabela Pontos
    public static final String TABLE_PONTOS = "pontos";
    public static final String COLUMN_PONTOS_ID = "_id";
    public static final String COLUMN_PONTOS_TRILHA_ID = "id_trilha";
    public static final String COLUMN_PONTOS_TEMPO = "tempo";
    public static final String COLUMN_PONTOS_LAT = "latitude";
    public static final String COLUMN_PONTOS_LONG = "longitude";
    public static final String COLUMN_PONTOS_ALT = "altitude";
    public static final String COLUMN_PONTOS_VELOCIDADE = "velocidade";

    public MyDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Criar tabela Trilha
        db.execSQL("CREATE TABLE " + TABLE_TRILHA + " (" +
                COLUMN_TRILHA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TRILHA_TEMPO_TOTAL + " TEXT, " +
                COLUMN_TRILHA_DISTANCIA + " REAL, " +
                COLUMN_TRILHA_DATA + " TEXT, " +
                COLUMN_TRILHA_HORA + " TEXT)");

        // Criar tabela Pontos
        db.execSQL("CREATE TABLE " + TABLE_PONTOS + " (" +
                COLUMN_PONTOS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PONTOS_TRILHA_ID + " INTEGER, " +
                COLUMN_PONTOS_TEMPO + " TEXT, " +
                COLUMN_PONTOS_LAT + " REAL, " +
                COLUMN_PONTOS_LONG + " REAL, " +
                COLUMN_PONTOS_ALT + " REAL, " +
                COLUMN_PONTOS_VELOCIDADE + " REAL, " +
                "FOREIGN KEY(" + COLUMN_PONTOS_TRILHA_ID + ") REFERENCES " + TABLE_TRILHA + "(" + COLUMN_TRILHA_ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRILHA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PONTOS);
        onCreate(db);
    }

    public long createTrilha() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Obter data e hora atuais
        Calendar calendar = Calendar.getInstance();
        String currentDate = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        String currentTime = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));

        // Inserir valores na tabela trilha
        cv.put(COLUMN_TRILHA_TEMPO_TOTAL, "00:00:00");
        cv.put(COLUMN_TRILHA_DISTANCIA, 0.0);
        cv.put(COLUMN_TRILHA_DATA, currentDate);
        cv.put(COLUMN_TRILHA_HORA, currentTime);

        long trilhaId = db.insert(TABLE_TRILHA, null, cv);
        if (trilhaId == -1) {
            // Caso a inserção falhe
            throw new RuntimeException("Erro ao criar a trilha no banco de dados.");
        }
        return trilhaId; // Retornar o ID da trilha
    }


    public void updateTrilha(long id, String tempoTotal, double distancia) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TRILHA_TEMPO_TOTAL, tempoTotal);
        cv.put(COLUMN_TRILHA_DISTANCIA, distancia);
        db.update(TABLE_TRILHA, cv, COLUMN_TRILHA_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void addPonto(long idTrilha, String tempo, double lat, double lon, double alt, double velocidade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PONTOS_TRILHA_ID, idTrilha);
        cv.put(COLUMN_PONTOS_TEMPO, tempo);
        cv.put(COLUMN_PONTOS_LAT, lat);
        cv.put(COLUMN_PONTOS_LONG, lon);
        cv.put(COLUMN_PONTOS_ALT, alt);
        cv.put(COLUMN_PONTOS_VELOCIDADE, velocidade);
        db.insert(TABLE_PONTOS, null, cv);
    }


    public Cursor readAllDataPontosFromTrilha(Integer id) {
        String query = "SELECT * FROM " + TABLE_PONTOS + " WHERE " + COLUMN_PONTOS_TRILHA_ID + " = " + id;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }

    public Cursor getAllTrilhas() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRILHA, null);
    }

    public String getPontosFormatted(int trilhaId) {
        StringBuilder pontosData = new StringBuilder();

        Cursor pontosCursor = readAllDataPontosFromTrilha(trilhaId);
        if (pontosCursor != null && pontosCursor.getCount() > 0) {
            int count = 1;
            while (pontosCursor.moveToNext()) {
                String tempo = pontosCursor.getString(pontosCursor.getColumnIndexOrThrow(COLUMN_PONTOS_TEMPO));
                double lat = pontosCursor.getDouble(pontosCursor.getColumnIndexOrThrow(COLUMN_PONTOS_LAT));
                double lon = pontosCursor.getDouble(pontosCursor.getColumnIndexOrThrow(COLUMN_PONTOS_LONG));
                double alt = pontosCursor.getDouble(pontosCursor.getColumnIndexOrThrow(COLUMN_PONTOS_ALT));
                double velocidade = pontosCursor.getDouble(pontosCursor.getColumnIndexOrThrow(COLUMN_PONTOS_VELOCIDADE));

                pontosData.append("  Ponto ").append(count).append(":\n")
                        .append("    Tempo: ").append(tempo).append("\n")
                        .append("    Latitude: ").append(lat).append("\n")
                        .append("    Longitude: ").append(lon).append("\n")
                        .append("    Altitude: ").append(alt).append("\n")
                        .append("    Velocidade: ").append(String.format(Locale.getDefault(), "%.2f m/s", velocidade)).append("\n");
                count++;
            }
            pontosCursor.close();
        } else {
            pontosData.append("  Nenhum ponto registrado para esta trilha.\n");
        }

        return pontosData.toString();
    }

    public Cursor readTrilhaById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRILHA + " WHERE " + COLUMN_TRILHA_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});
        return cursor;
    }

}

