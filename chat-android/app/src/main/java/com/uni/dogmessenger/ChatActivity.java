package com.uni.dogmessenger; // Ajusta esto a tu paquete real

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.uni.dto.Mensaje;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ChatActivity extends AppCompatActivity {
    private EditText etMensaje;
    private Button btnEnviarTexto, btnEnviarArchivo;

    // NUEVO: Agregamos las referencias para la pantalla
    private TextView tvMensajes;
    private ScrollView scrollViewChat;

    private static final int PICK_FILE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Enlazamos las variables Java con los IDs del XML
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviarTexto = findViewById(R.id.btnEnviarTexto);
        btnEnviarArchivo = findViewById(R.id.btnEnviarArchivo);
        tvMensajes = findViewById(R.id.tvMensajes);
        scrollViewChat = findViewById(R.id.scrollViewChat);

        // Escuchamos los mensajes que llegan del servidor
        SocketManager.getInstance().setListener(mensaje -> {
            runOnUiThread(() -> {
                if ("TEXTO".equals(mensaje.getTipo())) {
                    String texto = mensaje.getQrToken();

                    // NUEVO: Imprimimos el mensaje en la pantalla de la app
                    tvMensajes.append("[" + mensaje.getRemitente() + "]: " + texto + "\n\n");

                    // Hacer scroll automático hacia abajo
                    scrollViewChat.post(() -> scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN));

                } else if ("ARCHIVO".equals(mensaje.getTipo())) {
                    tvMensajes.append("[" + mensaje.getRemitente() + "]: envió un archivo (" + mensaje.getNombreArchivo() + ")\n\n");
                    scrollViewChat.post(() -> scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN));
                }
            });
        });

        // Evento para enviar TEXTO
        btnEnviarTexto.setOnClickListener(v -> {
            String texto = etMensaje.getText().toString();
            if (!texto.isEmpty()) {
                Mensaje msg = new Mensaje("TEXTO", "Anonimo", null);
                msg.setQrToken(texto);
                SocketManager.getInstance().enviarMensaje(msg);

                // Limpiamos la caja de texto
                etMensaje.setText("");
            }
        });

        // Evento para enviar ARCHIVO / IMAGEN
        btnEnviarArchivo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                enviarArchivo(fileUri);
            }
        }
    }

    private void enviarArchivo(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] archivoBytes = byteBuffer.toByteArray();

                Mensaje msgArchivo = new Mensaje("AppMovil", archivoBytes, "archivo_adjunto");
                SocketManager.getInstance().enviarMensaje(msgArchivo);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}