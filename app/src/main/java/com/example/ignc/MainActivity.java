package com.example.ignc;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ignc.helpers.AndroidHelper;
import com.example.ignc.helpers.BatteryReceiver;
import com.usdk.apiservice.aidl.beeper.UBeeper;
import com.usdk.apiservice.aidl.device.DeviceInfo;
import com.usdk.apiservice.aidl.device.UDeviceManager;
import com.usdk.apiservice.aidl.system.USystem;
import com.usdk.apiservice.aidl.system.location.LocationInfo;
import com.usdk.apiservice.aidl.system.location.ULocation;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
    private com.example.ignc.helpers.DeviceHelper DeviceHelper;
    private AndroidHelper androidHelper;
    private TextView textoResultado;
    private Button botao;
    private BatteryReceiver batteryReceiver;
    private float batteryLevel = -1;
    private int brightnessLevel = -1;
    private float previousBattery = -1;
    private long timeStart = -1;
    private boolean firstDropIgnored = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //PEGANDO A O CUSTO DE BATERIA DO BRILHO DA TELA ( VALOR RELATIVO )
        batteryReceiver = new BatteryReceiver(percentage -> {
            if (batteryLevel != percentage) {
                if (timeStart == -1) {
                    timeStart = System.currentTimeMillis();
                    previousBattery = percentage;
                } else if (!firstDropIgnored) {
                    firstDropIgnored = true;
                    previousBattery = percentage;
                    timeStart = System.currentTimeMillis();
                    Log.d("BAT_MONITOR", "Ignorando primeira queda.");
                } else if (percentage < previousBattery) {
                    long timeNow = System.currentTimeMillis();
                    long duration = timeNow - timeStart;
                    Log.d("BAT_MONITOR", "Brilho: " + brightnessLevel +
                            " | Queda de " + previousBattery + "% → " + percentage + "%" +
                            " | Tempo: " + (duration / 1000) + " segundos");

                    previousBattery = percentage;
                    timeStart = timeNow;
                }
            }
            batteryLevel = percentage;
            updateTextView();
        });


        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        textoResultado = findViewById(R.id.textoResultado);
        botao = findViewById(R.id.botao);

        DeviceHelper.me().init(this);
        DeviceHelper.me().bindService();
        androidHelper = new AndroidHelper(this);

        botao.setOnClickListener(v -> obterDadosDispositivo());


        androidHelper.startMonitoringBrightness(new AndroidHelper.OnBrightnessChangeListener() {
            @Override
            public void onBrightnessChanged(int brightness) {
                brightnessLevel = brightness;
                updateTextView();
            }
        });

    }


    private void updateTextView() {
        runOnUiThread(() -> {
            String batteryText = batteryLevel >= 0 ? "Bateria: " + batteryLevel + "%" : "Bateria: N/A";
            String brightnessText = brightnessLevel >= 0 ? "Brilho: " + brightnessLevel : "Brilho: N/A";
            textoResultado.setText(batteryText + "\n" + brightnessText);
        });
    }
    private void obterDadosDispositivo() {
        try {
            UDeviceManager deviceManager = DeviceHelper.me().getDeviceManager();
            DeviceInfo deviceInfo = deviceManager.getDeviceInfo();

            UBeeper deviceBeeper = DeviceHelper.me().getBeeper();
            deviceBeeper.startBeep(500);


            int value = 0;
            ULocation deviceLocation = DeviceHelper.me().getLocation();
            LocationInfo info = new LocationInfo();
            //Passe 1- ativar a localização e 0 - desativar a localização
            var inf = deviceLocation.setLocationMode(0);
            USystem deviceSystemLocation = DeviceHelper.me().getSystem();
            Object test = deviceSystemLocation.getLocation();
            if (test != null) {
                Log.d("LOC", "Foi: " + test );
            } else {
                Log.e("LOC", "NÃO Foi");
            }

            StringBuilder deviceInfoDetails = new StringBuilder();

            for (Field field : deviceInfo.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object values = field.get(deviceInfo);
                    deviceInfoDetails.append(field.getName()).append(": ").append("\n").append(values).append("\n\n");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }


            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////A IMPRESSÃO DE TESTE ESTÁ DESATIVADA PARA NÃO GASTAR PAPEL DA MÁQUINA SEMPRE QUE CLICAR NO BOTÃO////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
//            UPrinter devicePrinter = DeviceHelper.me().getPrinter();
//            devicePrinter.addText(0, "Teste");
//            devicePrinter.feedLine(5);
//            devicePrinter.startPrint(new OnPrintListener.Stub() {
//                @Override
//                public void onFinish() throws RemoteException {
//                    Log.d("=> IMPRESSÃO", "=> Impressão concluída com sucesso!");
//                }
//
//                @Override
//                public void onError(int error) throws RemoteException {
//                    Log.e("=> IMPRESSÃO","Erro" + error);
//                }
//            });
////////////////////////////////////////////////////////////////////////////////////////////////


            String dadosFormatados = "Modelo: " + deviceInfo.getModel() + "\n\n" +
                    "Location: " + "\n"  + inf + "\n\n" +
//                    "Bateria " + "\n" + batteryLevel + "%" +  "\n\n" +
                    deviceInfoDetails.toString();

            Log.d("DEVICE_INFO", dadosFormatados);

            runOnUiThread(() -> textoResultado.setText(dadosFormatados));

        } catch (Exception e) {
            runOnUiThread(() -> textoResultado.setText("Erro: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DeviceHelper.me().unregister();
        unregisterReceiver(batteryReceiver);
        androidHelper.stopMonitoringBrightness();
    }
}