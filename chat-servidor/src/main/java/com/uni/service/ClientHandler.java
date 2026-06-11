package com.uni.service;

import com.uni.dto.Mensaje;
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
            while (socket.isConnected()) {
                // Se queda esperando (bloqueado) hasta que el cliente envíe un objeto Mensaje
                Mensaje mensajeRecibido = (Mensaje) input.readObject();

                if (this.nombreUsuario == null) {
                    this.nombreUsuario = mensajeRecibido.getRemitente();
                    System.out.println(
                        "[SERVIDOR] " +
                            nombreUsuario +
                            " se ha conectado de forma segura."
                    );
                }

                if ("TEXTO".equals(mensajeRecibido.getTipo())) {
                    System.out.println(
                        "[" +
                            mensajeRecibido.getRemitente() +
                            "]: " +
                            mensajeRecibido.getContenidoTexto()
                    );
                    retransmitirMensaje(mensajeRecibido);
                } else if ("ARCHIVO".equals(mensajeRecibido.getTipo())) {
                    System.out.println(
                        "[SERVIDOR] Recibiendo archivo de " +
                            mensajeRecibido.getRemitente() +
                            ": " +
                            mensajeRecibido.getNombreArchivo()
                    );

                    // Opción: Guardar el archivo en el disco del servidor
                    guardarArchivoEnServidor(mensajeRecibido);

                    // Opcional: Retransmitir el archivo a los demás usuarios de la sala
                    retransmitirMensaje(mensajeRecibido);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Ocurre cuando el cliente se desconecta
            System.out.println(
                "[SERVIDOR] " +
                    (nombreUsuario != null ? nombreUsuario : "Un cliente") +
                    " se ha desconectado."
            );
        } finally {
            cerrarTodo();
        }
    }

    // Envía el mensaje a absolutamente todos los clientes conectados, excepto a quien lo envió
    private void retransmitirMensaje(Mensaje mensaje) {
        for (ClientHandler cliente : clientesConectados) {
            try {
                if (cliente != this) {
                    cliente.output.writeObject(mensaje);
                    cliente.output.flush();
                }
            } catch (IOException e) {
                cliente.cerrarTodo();
            }
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
