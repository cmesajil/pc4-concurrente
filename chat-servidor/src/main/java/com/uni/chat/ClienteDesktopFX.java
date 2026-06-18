package com.uni.chat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.uni.dto.Mensaje;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class ClienteDesktopFX extends Application {

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private SSLSocket socket;
    
    // Elementos de la UI
    private TextArea areaMensajes;
    private TextField campoEntrada;
    private ImageView vistaQR;
    private String miTokenSala = "SALA_VENTAS_PRINCIPAL"; // Sala por defecto para pruebas

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage escenarioPrincipal) {
        escenarioPrincipal.setTitle("Dog Messenger - Desktop");

        // --- CONSTRUCCIÓN DE LA UI ---
        VBox contenedorPrincipal = new VBox(10);
        contenedorPrincipal.setPadding(new Insets(15));
        contenedorPrincipal.setStyle("-fx-background-color: #F0F4F8;");

        // Área de chat
        areaMensajes = new TextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setWrapText(true);
        VBox.setVgrow(areaMensajes, Priority.ALWAYS);

        // Controles inferiores (Input + Enviar)
        HBox cajaInput = new HBox(10);
        campoEntrada = new TextField();
        campoEntrada.setPromptText("Escribe un mensaje...");
        HBox.setHgrow(campoEntrada, Priority.ALWAYS);
        
        Button btnEnviar = new Button("Enviar");
        btnEnviar.setStyle("-fx-background-color: #1D71B8; -fx-text-fill: white; -fx-font-weight: bold;");
        
        cajaInput.getChildren().addAll(campoEntrada, btnEnviar);

        // Controles de QR (Clonación)
        HBox cajaQR = new HBox(10);
        cajaQR.setAlignment(Pos.CENTER_LEFT);
        Button btnMostrarQR = new Button("Generar QR para Clonar");
        vistaQR = new ImageView();
        vistaQR.setFitWidth(100);
        vistaQR.setFitHeight(100);
        
        cajaQR.getChildren().addAll(btnMostrarQR, vistaQR);

        contenedorPrincipal.getChildren().addAll(areaMensajes, cajaInput, cajaQR);

        // --- EVENTOS ---
        btnEnviar.setOnAction(e -> enviarMensaje());
        campoEntrada.setOnAction(e -> enviarMensaje()); // Permite enviar con la tecla Enter

        btnMostrarQR.setOnAction(e -> generarYMostrarQR());

        // --- CONFIGURACIÓN DE RED (Ejecutada en Hilo Secundario) ---
        conectarAlServidor();

        Scene escena = new Scene(contenedorPrincipal, 500, 600);
        escenarioPrincipal.setScene(escena);
        escenarioPrincipal.setOnCloseRequest(e -> cerrarConexion());
        escenarioPrincipal.show();
    }

    private void conectarAlServidor() {
        new Thread(() -> {
            try {
                // Configuramos el TrustManager para desarrollo local (ignora validación de certificado autofirmado)
                TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
                };

                SSLContext sc = SSLContext.getInstance("TLSv1.3");
                sc.init(null, trustAllCerts, new SecureRandom());

                // Conexión a localhost en el puerto 8888
                socket = (SSLSocket) sc.getSocketFactory().createSocket("127.0.0.1", 8888);
                socket.startHandshake();

                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Autenticación inicial (Flujo de la práctica)
                Mensaje login = new Mensaje("REGISTRO", "UsuarioDesktop", null);
                login.setQrSalaToken(miTokenSala);
                out.writeObject(login);
                out.flush();

                Platform.runLater(() -> areaMensajes.appendText("[SISTEMA] Conectado exitosamente por TLS.\n"));

                // Bucle de escucha
                while (socket.isConnected()) {
                    Mensaje msjRecibido = (Mensaje) in.readObject();
                    
                    if ("TEXTO".equals(msjRecibido.getTipo())) {
                        String texto = msjRecibido.getQrToken();
                        String remitente = msjRecibido.getRemitente();
                        
                        // JavaFX requiere que las actualizaciones de UI se hagan en el hilo principal
                        Platform.runLater(() -> areaMensajes.appendText("[" + remitente + "]: " + texto + "\n"));
                    } else if ("ENTREGA_QR_SALA".equals(msjRecibido.getTipo())) {
                        miTokenSala = msjRecibido.getQrSalaToken();
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> areaMensajes.appendText("[ERROR] Conexión perdida.\n"));
            }
        }).start();
    }

    private void enviarMensaje() {
        String texto = campoEntrada.getText().trim();
        if (!texto.isEmpty() && out != null) {
            try {
                Mensaje msg = new Mensaje("TEXTO", "Anonimo", null);
                msg.setQrToken(texto);
                out.writeObject(msg);
                out.flush();
                
                campoEntrada.clear();
            } catch (IOException e) {
                areaMensajes.appendText("[ERROR] No se pudo enviar el mensaje.\n");
            }
        }
    }

    private void generarYMostrarQR() {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // Generamos una matriz de bits con el texto del Token de la Sala
            BitMatrix bitMatrix = qrCodeWriter.encode(miTokenSala, BarcodeFormat.QR_CODE, 200, 200);
            
            // Convertimos la matriz a un flujo de bytes (PNG) en memoria
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            
            // Cargamos la imagen en la vista de JavaFX
            ByteArrayInputStream inputStream = new ByteArrayInputStream(pngOutputStream.toByteArray());
            Image qrImage = new Image(inputStream);
            vistaQR.setImage(qrImage);
            
            areaMensajes.appendText("[SISTEMA] QR Generado. Escanéalo con el móvil para clonar el chat.\n");
        } catch (Exception e) {
            areaMensajes.appendText("[ERROR] Fallo al generar el código QR.\n");
        }
    }

    private void cerrarConexion() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }
}