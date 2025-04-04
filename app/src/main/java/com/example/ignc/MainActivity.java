package com.example.ignc;

import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.beeper.UBeeper;
import com.usdk.apiservice.aidl.device.DeviceInfo;
import com.usdk.apiservice.aidl.device.UDeviceManager;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.printer.OnPrintListener;
import com.usdk.apiservice.aidl.system.USystem;
import com.usdk.apiservice.aidl.system.location.LocationInfo;
import com.usdk.apiservice.aidl.system.location.ULocation;

import java.lang.reflect.Field;


public class MainActivity extends AppCompatActivity {
    private com.example.ignc.DeviceHelper DeviceHelper;
    private TextView textoResultado;
    private Button botao;

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

        textoResultado = findViewById(R.id.textoResultado);
        botao = findViewById(R.id.botao);
        DeviceHelper.me().init(this);
        DeviceHelper.me().bindService();

        botao.setOnClickListener(v -> obterDadosDispositivo());
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
                    deviceInfoDetails.toString();

            runOnUiThread(() -> textoResultado.setText(dadosFormatados));

        } catch (Exception e) {
            runOnUiThread(() -> textoResultado.setText("Erro: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}