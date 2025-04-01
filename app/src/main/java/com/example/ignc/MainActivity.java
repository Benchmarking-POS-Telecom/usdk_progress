package com.example.ignc;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.beeper.UBeeper;
import com.usdk.apiservice.aidl.device.DeviceInfo;
import com.usdk.apiservice.aidl.device.UDeviceManager;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.printer.OnPrintListener;

public class MainActivity extends AppCompatActivity {
    private UDeviceService deviceService;
    private static final String TAG = "DeviceHelper";
    private Context context;
    private com.example.ignc.DeviceHelper DeviceHelper;

    public void init(Context context) {
        this.context = context;
    }
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

            UPrinter devicePrinter = DeviceHelper.me().getPrinter();
            devicePrinter.addText(0, "Teste");
            devicePrinter.startPrint(new OnPrintListener.Stub() {
                @Override
                public void onFinish() throws RemoteException {
                    Log.d("=> IMPRESSÃO", "=> Impressão concluída com sucesso!");
                }

                @Override
                public void onError(int error) throws RemoteException {
                    Log.e("=> IMPRESSÃO","Erro" + error);
                }
            });



            String dadosFormatados = "Modelo: " + deviceInfo.getModel() + "\n" +
                    "Fabricante: " + deviceInfo.getManufacture() + "\n" +
                    "Número de Série: " + deviceInfo.getSerialNo() + "\n";

            runOnUiThread(() -> textoResultado.setText(dadosFormatados));

        } catch (Exception e) {
            runOnUiThread(() -> textoResultado.setText("Erro: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}