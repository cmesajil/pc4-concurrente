package com.uni.dogmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.uni.dto.Mensaje;

public class LoginActivity extends AppCompatActivity {
    private EditText etNombre;
    private Button btnEscanearQR;
    private String qrSalaEscaneado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNombre = findViewById(R.id.etNombre);
        btnEscanearQR = findViewById(R.id.btnEscanearQR);

        // Inicializamos la conexión al servidor de fondo
        SocketManager.getInstance().conectar();

        btnEscanearQR.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Escanea el QR de la Sala o del Chat a clonar");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {
                qrSalaEscaneado = result.getContents();
                String nombre = etNombre.getText().toString();

                if (!nombre.isEmpty()) {
                    // Preparamos el mensaje inicial como lo exige el servidor
                    Mensaje registro = new Mensaje("REGISTRO", nombre, null);
                    registro.setQrSalaToken(qrSalaEscaneado);

                    // Enviamos por socket
                    SocketManager.getInstance().enviarMensaje(registro);

                    // Pasamos a la actividad de chat
                    Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Ingresa un nombre primero", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}