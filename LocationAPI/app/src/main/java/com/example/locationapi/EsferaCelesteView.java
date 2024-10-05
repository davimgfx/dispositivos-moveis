package com.example.locationapi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.GnssStatus;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class EsferaCelesteView extends View {
    private GnssStatus newStatus;
    private Paint paint;
    private int r;
    private int height, width;
    private GNSSActivity gnssActivity;

    public EsferaCelesteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();

        if (context instanceof GNSSActivity) {
            gnssActivity = (GNSSActivity) context; // Inicialize a atividade aqui
        }

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSatelliteSelectionDialog();
            }
        });
    }

    private void showSatelliteSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Localização do usuário");

        // Cria o layout para o diálogo
        View dialogView = inflate(getContext(), R.layout.dialog_layout, null);
        builder.setView(dialogView);

        // CheckBoxes para constelações
        CheckBox gpsCheckBox = dialogView.findViewById(R.id.gpsCheckBox);
        CheckBox galileoCheckBox = dialogView.findViewById(R.id.galileoCheckBox);
        CheckBox glonassCheckBox = dialogView.findViewById(R.id.glonassCheckBox);
        CheckBox unknownCheckBox = dialogView.findViewById(R.id.unknownCheckBox);
        CheckBox usedInFixCheckBox = dialogView.findViewById(R.id.usedInFixCheckBox);
        CheckBox notUsedInFixCheckBox = dialogView.findViewById(R.id.notUsedInFixCheckBox);

        // Marque todos os checkboxes como selecionados por padrão
        gpsCheckBox.setChecked(true);
        galileoCheckBox.setChecked(true);
        glonassCheckBox.setChecked(true);
        unknownCheckBox.setChecked(true);
        usedInFixCheckBox.setChecked(true);
        notUsedInFixCheckBox.setChecked(true);

        // Recuperar as preferências salvas
        gpsCheckBox.setChecked(gnssActivity.getSatellitePreference("gpsChecked", true));
        galileoCheckBox.setChecked(gnssActivity.getSatellitePreference("galileoChecked", true));
        glonassCheckBox.setChecked(gnssActivity.getSatellitePreference("glonassChecked", true));
        unknownCheckBox.setChecked(gnssActivity.getSatellitePreference("unknownChecked", true));
        usedInFixCheckBox.setChecked(gnssActivity.getSatellitePreference("usedInFix", true));
        notUsedInFixCheckBox.setChecked(gnssActivity.getSatellitePreference("notUsedInFix", true));

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            // Salvar as informações no SharedPreferences usando GNSSActivity
            gnssActivity.saveSatellitePreferences(
                    gpsCheckBox.isChecked(),
                    galileoCheckBox.isChecked(),
                    glonassCheckBox.isChecked(),
                    unknownCheckBox.isChecked(),
                    usedInFixCheckBox.isChecked(),
                    notUsedInFixCheckBox.isChecked()
            );
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // coletando informações do tamanho tela de desenho
        width = getMeasuredWidth();
        height = getMeasuredHeight();

        // definindo o raio da esfera celeste
        if (width < height)
            r = (int) (width / 2 * 0.9);
        else
            r = (int) (height / 2 * 0.9);

        // configurando o pincel para desenhar a projeção da esfera celeste
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);

        // desenha a projeção da esfera celeste
        // desenhando círculos concêntricos
        int radius = r;
        canvas.drawCircle(computeXc(0), computeYc(0), radius, paint);
        radius = (int) (radius * Math.cos(Math.toRadians(45)));
        canvas.drawCircle(computeXc(0), computeYc(0), radius, paint);
        radius = (int) (radius * Math.cos(Math.toRadians(60)));
        canvas.drawCircle(computeXc(0), computeYc(0), radius, paint);

        // desenhando os eixos
        canvas.drawLine(computeXc(0), computeYc(-r), computeXc(0), computeYc(r), paint);
        canvas.drawLine(computeXc(-r), computeYc(0), computeXc(r), computeYc(0), paint);

        // configurando o pincel para desenhar os satélites
        paint.setStyle(Paint.Style.FILL);

        // Verificar quais constelações estão selecionadas
        boolean gpsSelected = gnssActivity.getSatellitePreference("gpsChecked", true);
        boolean galileoSelected = gnssActivity.getSatellitePreference("galileoChecked", true);
        boolean glonassSelected = gnssActivity.getSatellitePreference("glonassChecked", true);
        boolean unknownSelected = gnssActivity.getSatellitePreference("unknownChecked", true);
        boolean usedInFixSelected = gnssActivity.getSatellitePreference("usedInFix", true);
        boolean notUsedInFixSelected = gnssActivity.getSatellitePreference("notUsedInFix", true);


        // desenhando os satélites (caso exista um GnssStatus disponível)
        if (newStatus != null) {
            for (int i = 0; i < newStatus.getSatelliteCount(); i++) {
                float az = newStatus.getAzimuthDegrees(i);
                float el = newStatus.getElevationDegrees(i);
                float x = (float) (r * Math.cos(Math.toRadians(el)) * Math.sin(Math.toRadians(az)));
                float y = (float) (r * Math.cos(Math.toRadians(el)) * Math.cos(Math.toRadians(az)));

                // Determinando a cor do satélite com base na constelação
                int constellationType = newStatus.getConstellationType(i);
                boolean shouldDraw = false; // Flag para determinar se deve desenhar o satélite

                // Verificar se a constelação do satélite está selecionada
                switch (constellationType) {
                    case GnssStatus.CONSTELLATION_GPS:
                        paint.setColor(Color.RED); // GPS
                        shouldDraw = gpsSelected; // Verificar se o GPS está selecionado
                        break;
                    case GnssStatus.CONSTELLATION_GLONASS:
                        paint.setColor(Color.YELLOW); // Glonass
                        shouldDraw = glonassSelected; // Verificar se o Glonass está selecionado
                        break;
                    case GnssStatus.CONSTELLATION_GALILEO:
                        paint.setColor(Color.GREEN); // Galileo
                        shouldDraw = galileoSelected; // Verificar se o Galileo está selecionado
                        break;
                    default:
                        paint.setColor(Color.GRAY); // Desconhecido
                        shouldDraw = unknownSelected; // Verificar se desconhecido está selecionado
                        break;
                }

                // Verificar se deve desenhar baseado no uso para captura de posição
                boolean usedInFix = newStatus.usedInFix(i);
                if (usedInFix && !usedInFixSelected) {
                    continue; // Se não for para desenhar usados para captura, pula
                }
                if (!usedInFix && !notUsedInFixSelected) {
                    continue; // Se não for para desenhar não usados para captura, pula
                }

                // Desenhar o satélite se todas as condições forem atendidas
                if (shouldDraw) {
                    canvas.drawCircle(computeXc(x), computeYc(y), 10, paint);
                    paint.setTextAlign(Paint.Align.LEFT);
                    paint.setTextSize(30);
                    String satID = newStatus.getSvid(i) + "";
                    String statusText = usedInFix ? " - Sim" : " - Não";
                    canvas.drawText(satID + statusText, computeXc(x) + 10, computeYc(y) + 10, paint);
                }
            }
        }
    }

    private int computeXc(double x) {
        return (int) (x + width / 2);
    }

    private int computeYc(double y) {
        return (int) (-y + height / 2);
    }

    public void setNewStatus(GnssStatus newStatus) {
        this.newStatus = newStatus;
        invalidate();
    }

}
