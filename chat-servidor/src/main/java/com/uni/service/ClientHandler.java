package com.uni.service;

import com.uni.dto.Mensaje;
import com.uni.util.DatabaseConnection;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    // Lista global y segura para hilos que almacena a todos los clientes conectados
    public static CopyOnWriteArrayList<ClientHandler> clientesConectados =
        new CopyOnWriteArrayList<>();

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private int usuarioId;
    private String qrToken;
    private String nombreUsuario;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            // IMPORTANTE: En Java, siempre debes crear el ObjectOutputStream ANTES del ObjectInputStream
            // o el socket se quedará bloqueado esperando un handshake de streams.
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());

            clientesConectados.add(this);
        } catch (IOException e) {
            cerrarTodo();
        }
    }

    @Override
    public void run() {
        try {
            // 1. LEER EL PRIMER MENSAJE (Petición de Conexión)
            Mensaje solicitudInical = (Mensaje) input.readObject();

            if ("REGISTRO".equals(solicitudInical.getTipo())) {
                // El cliente se conectó a ciegas: le generamos su QR en la BD
                this.nombreUsuario = solicitudInical.getRemitente();

                // Forzamos la creación en Postgres
                this.qrToken = DatabaseConnection.crearNuevoUsuario(
                    this.nombreUsuario
                );
                this.usuarioId = DatabaseConnection.autenticarPorToken(
                    this.qrToken
                );

                System.out.println(
                    "[REGISTRO] Nuevo usuario creado en Postgres. ID: " +
                        usuarioId +
                        " | QR: " +
                        qrToken
                );

                // Le entregamos su QR de vuelta inmediatamente por el canal de comunicación
                Mensaje respuestaQR = new Mensaje(
                    "ENTREGA_QR",
                    "SERVIDOR",
                    this.qrToken
                );
                output.writeObject(respuestaQR);
                output.flush();

                // Ahora que su estado en base de datos es correcto, lo sumamos al chat general
                clientesConectados.add(this);
            } else if ("LOGIN".equals(solicitudInical.getTipo())) {
                // Lógica para cuando el cliente ya tenga un QR guardado y quiera recuperarse
                this.qrToken = solicitudInical.getQrToken();
                this.usuarioId = DatabaseConnection.autenticarPorToken(
                    this.qrToken
                );

                if (this.usuarioId == -1) {
                    System.out.println(
                        "[LOGIN RECHAZADO] El token QR enviado no existe."
                    );
                    output.writeObject(
                        new Mensaje(
                            "TEXTO",
                            "SERVIDOR",
                            "ERROR: Token QR inválido."
                        )
                    );
                    cerrarTodo();
                    return;
                }

                this.nombreUsuario = solicitudInical.getRemitente();
                System.out.println(
                    "[LOGIN OK] Usuario recuperado con éxito. ID: " + usuarioId
                );

                clientesConectados.add(this);
            } else {
                // Si manda un mensaje común sin presentarse, lo pateamos por seguridad
                System.out.println("[ERROR] Intento de conexión malformado.");
                cerrarTodo();
                return;
            }

            // 2. BUCLE DEL CHAT EN VIVO
            while (socket.isConnected()) {
                Mensaje mensajeRecibido = (Mensaje) input.readObject();
                if ("TEXTO".equals(mensajeRecibido.getTipo())) {
                    System.out.println(
                        "[" +
                            nombreUsuario +
                            "]: " +
                            mensajeRecibido.getContenidoTexto()
                    );
                    retransmitirMensaje(mensajeRecibido);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[SERVIDOR] Un cliente se ha desconectado.");
        } finally {
            cerrarTodoSilencioso();
        }
    }

    // Envía el mensaje a todos los clientes conectados de forma segura
    private void retransmitirMensaje(Mensaje mensaje) {
        for (ClientHandler cliente : clientesConectados) {
            if (cliente != this) {
                try {
                    // Verificamos que el socket esté realmente activo antes de escribirle
                    if (
                        cliente.socket != null &&
                        !cliente.socket.isClosed() &&
                        cliente.socket.isConnected()
                    ) {
                        cliente.output.writeObject(mensaje);
                        cliente.output.flush();
                    } else {
                        // Si el socket ya estaba muerto de antemano, lo removemos
                        clientesConectados.remove(cliente);
                    }
                } catch (IOException e) {
                    // Si falla al escribir en vivo, lo limpiamos de inmediato
                    System.out.println(
                        "[SISTEMA] Limpiando conexión remota perdida de un usuario."
                    );
                    cliente.cerrarTodoSilencioso();
                }
            }
        }
    }

    // Reemplaza tu antiguo cerrarTodo por esta versión segura para entornos TLS
    public void cerrarTodoSilencioso() {
        clientesConectados.remove(this);

        // Cerramos los streams de forma independiente y capturando errores por separado
        try {
            if (input != null) input.close();
        } catch (IOException e) {
            /* Ignorar si ya está cerrado */
        }

        try {
            if (output != null) output.close();
        } catch (IOException e) {
            /* Ignorar si ya está cerrado */
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            /* Ignorar si ya está cerrado */
        }
    }

    // Método auxiliar por si decides almacenar las imágenes directamente en una carpeta del backend
    private void guardarArchivoEnServidor(Mensaje mensaje) {
        String carpetaDestino = "archivos_recibidos/";
        // Crea la carpeta si no existe
        java.io.File directorio = new java.io.File(carpetaDestino);
        if (!directorio.exists()) directorio.mkdirs();

        try (
            FileOutputStream fos = new FileOutputStream(
                carpetaDestino +
                    System.currentTimeMillis() +
                    "_" +
                    mensaje.getNombreArchivo()
            )
        ) {
            fos.write(mensaje.getArchivoBytes());
            System.out.println("[SERVIDOR] Archivo guardado con éxito.");
        } catch (IOException e) {
            System.err.println(
                "[SERVIDOR] Error al guardar el archivo: " + e.getMessage()
            );
        }
    }

    // Método para limpiar recursos y remover al cliente de la lista activa
    public void cerrarTodo() {
        clientesConectados.remove(this);
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
