package com.uni.chat;

import com.uni.dto.Mensaje;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Scanner;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class ClienteChatTLS {

    public static void main(String[] args) {
        String host = "localhost"; // O la IP de tu servidor
        int puerto = 8888;

        // El cliente necesita el certificado público del servidor para confiar en él
        String rutaCertificadoPublico = Paths.get("trust_container_root.p12")
            .toAbsolutePath()
            .toString();
        char[] passwordCert = "container-password".toCharArray();

        try {
            // 1. Cargar el certificado en el TrustStore del cliente
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = new FileInputStream(rutaCertificadoPublico)) {
                trustStore.load(is, passwordCert);
                System.out.println("Entradas truststore: " + trustStore.size());

                java.util.Enumeration<String> aliases = trustStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    System.out.println(
                        "Alias: " +
                            alias +
                            " CertEntry=" +
                            trustStore.isCertificateEntry(alias) +
                            " KeyEntry=" +
                            trustStore.isKeyEntry(alias)
                    );
                }
            }

            // 2. Inicializar el TrustManagerFactory con el TrustStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init(trustStore);

            // 3. Crear el contexto SSL/TLS e inicializarlo con el TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // 4. Crear el Socket Seguro y conectarse
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) ssf.createSocket(host, puerto);

            socket.startHandshake();
            System.out.println("TLS OK");

            // Streams de comunicación serializada
            ObjectOutputStream out = new ObjectOutputStream(
                socket.getOutputStream()
            );
            ObjectInputStream in = new ObjectInputStream(
                socket.getInputStream()
            );

            // 1. HILO PARA ESCUCHAR AL SERVIDOR
            new Thread(() -> {
                try {
                    while (true) {
                        Mensaje deServidor = (Mensaje) in.readObject();

                        if ("ENTREGA_QR".equals(deServidor.getTipo())) {
                            System.out.println(
                                "\n[SISTEMA] ¡Usuario creado en la base de datos!"
                            );
                            System.out.println(
                                "[SISTEMA] Tu token QR de USUARIO es: " +
                                    deServidor.getQrToken()
                            );
                            System.out.println(
                                "--------------------------------------------------"
                            );
                        } else if ("TEXTO".equals(deServidor.getTipo())) {
                            System.out.println(
                                "\n[" +
                                    deServidor.getRemitente() +
                                    "]: " +
                                    deServidor.getContenidoTexto()
                            );
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Conexión con el servidor cerrada.");
                }
            }).start();

            // 2. MENÚ INICIAL DE SELECCIÓN
            Scanner sc = new Scanner(System.in);

            System.out.println("=== BIENVENIDO AL SISTEMA DE MENSAJERÍA ===");
            System.out.println("1. Registrarse como usuario nuevo");
            System.out.println("2. Iniciar sesión con QR de usuario existente");
            System.out.print("Seleccione una opción: ");
            int opcion = Integer.parseInt(sc.nextLine());

            Mensaje solicitudInicial = null;

            if (opcion == 1) {
                System.out.print(
                    "Ingrese su nombre de usuario para registrarse: "
                );
                String nombreIngresado = sc.nextLine();

                System.out.print(
                    "Escanee/Ingrese el token QR de la SALA a la que desea unirse: "
                );
                String tokenSala = sc.nextLine();

                // Usamos el constructor de 3 argumentos que tu DTO Mensaje sí posee:
                // public Mensaje(String tipo, String remitente, String contenido)
                solicitudInicial = new Mensaje(
                    "REGISTRO",
                    nombreIngresado,
                    null
                );
                solicitudInicial.setQrSalaToken(tokenSala);
            } else {
                // Flujo de Login existente
                System.out.print("Ingrese su token QR de usuario: ");
                String qrUsuario = sc.nextLine();

                System.out.print(
                    "Escanee/Ingrese el token QR de la SALA a la que desea unirse: "
                );
                String qrSala = sc.nextLine();

                solicitudInicial = new Mensaje("LOGIN", "Anonimo", null);
                solicitudInicial.setQrToken(qrUsuario);
                solicitudInicial.setQrSalaToken(qrSala);
            }

            // Enviamos la solicitud de autenticación inicial (Sea REGISTRO o LOGIN)
            out.writeObject(solicitudInicial);
            out.flush();

            System.out.println(
                "\n[SISTEMA] Procesando solicitud... Conectando a la sala."
            );

            // 3. BUCLE PARA CHATEAR EN VIVO
            while (true) {
                String texto = sc.nextLine();
                // Usamos el constructor de 2 argumentos para enviar los mensajes ordinarios de texto
                Mensaje msg = new Mensaje("Anonimo", texto);
                out.writeObject(msg);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
