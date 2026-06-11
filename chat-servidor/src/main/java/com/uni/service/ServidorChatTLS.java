package com.uni.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class ServidorChatTLS {

    public static void main(String[] args) {
        int puerto = 8888;
        String rutaPkcs12 = Paths.get(
            System.getProperty("user.home"),
            "container_root.p12"
        ).toString(); // Tu contenedor PKCS12
        char[] password = "container-password".toCharArray();

        try {
            // 1. Cargar el contenedor PKCS12
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = new FileInputStream(rutaPkcs12)) {
                keyStore.load(is, password);
            }

            // 2. Inicializar el KeyManagerFactory con el KeyStore
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
            );
            kmf.init(keyStore, password);

            // 3. Crear el contexto SSL/TLS (Usando TLS v1.3 que es el estándar actual)
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // 4. Crear el ServerSocket Seguro
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket =
                (SSLServerSocket) ssf.createServerSocket(puerto);

            System.out.println(
                "Servidor TLS de mensajería corriendo de forma segura en el puerto " +
                    puerto
            );

            while (true) {
                // Espera a que un cliente se conecte por TLS
                Socket clienteSocket = serverSocket.accept();

                // Creamos el manejador pasándole el socket seguro
                ClientHandler manejador = new ClientHandler(clienteSocket);

                // Iniciamos el hilo para ese cliente
                Thread hilo = new Thread(manejador);
                hilo.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
