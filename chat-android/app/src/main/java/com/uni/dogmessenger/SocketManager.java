package com.uni.dogmessenger;

import com.uni.dto.Mensaje;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SocketManager {
    private static SocketManager instance;
    private SSLSocket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // IMPORTANTE: Si pruebas en el emulador de Android hacia tu PC local, usa 10.0.2.2
    // Si pruebas en teléfonos físicos en la misma red WIFI, usa la IP de tu PC (ej. 192.168.1.X)
    private static final String SERVER_IP = "192.168.42.53";
    private static final int SERVER_PORT = 8888;

    public interface MensajeListener {
        void onMensajeRecibido(Mensaje mensaje);
    }

    private MensajeListener listener;

    private SocketManager() {}

    public static SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void setListener(MensajeListener listener) {
        this.listener = listener;
    }

    public void conectar() {
        new Thread(() -> {
            try {
                // Configuración TLS ignorando validación estricta para pruebas de desarrollo local
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };

                SSLContext sc = SSLContext.getInstance("TLSv1.3");
                sc.init(null, trustAllCerts, new SecureRandom());

                socket = (SSLSocket) sc.getSocketFactory().createSocket(SERVER_IP, SERVER_PORT);
                socket.startHandshake();

                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Hilo continuo para escuchar mensajes
                while (socket.isConnected()) {
                    Mensaje mensaje = (Mensaje) in.readObject();
                    if (listener != null) {
                        listener.onMensajeRecibido(mensaje);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void enviarMensaje(Mensaje mensaje) {
        new Thread(() -> {
            try {
                if (out != null) {
                    out.writeObject(mensaje);
                    out.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
