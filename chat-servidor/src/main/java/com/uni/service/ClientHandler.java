package com.uni.service;

import com.uni.dto.Mensaje;
import com.uni.dto.SalaDTO;
import com.uni.repository.SalaRepository;
import com.uni.util.DatabaseConnection;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    // ESTRUCTURA PRINCIPAL: Mapea el ID de la sala con la lista de clientes agrupados en ella
    public static ConcurrentHashMap<
        Integer,
        CopyOnWriteArrayList<ClientHandler>
    > salasActivas = new ConcurrentHashMap<>();

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private int usuarioId;
    private String qrToken;
    private String nombreUsuario;

    // Propiedad fundamental para saber a dónde retransmitir los mensajes
    private Integer salaId;

    // Línea nueva corregida con la Opción A:
    private static final SalaRepository salaRepository =
        new com.uni.repository.SalaRepositoryImpl();

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
            // Eliminamos la inserción automática a la lista global estática vieja
        } catch (IOException e) {
            cerrarTodoSilencioso();
        }
    }

    @Override
    public void run() {
        try {
            // 1. LEER EL PRIMER MENSAJE (Petición de Conexión)
            Mensaje solicitudInical = (Mensaje) input.readObject();

            // Flujo de autenticación/registro base del usuario
            if ("REGISTRO".equals(solicitudInical.getTipo())) {
                this.nombreUsuario = solicitudInical.getRemitente();
                this.qrToken = DatabaseConnection.crearNuevoUsuario(
                    this.nombreUsuario
                );
                this.usuarioId = DatabaseConnection.autenticarPorToken(
                    this.qrToken
                );

                Mensaje respuestaQR = new Mensaje(
                    "ENTREGA_QR",
                    "SERVIDOR",
                    this.qrToken
                );
                output.writeObject(respuestaQR);
                output.flush();
            } else if ("LOGIN".equals(solicitudInical.getTipo())) {
                this.qrToken = solicitudInical.getQrToken();
                this.usuarioId = DatabaseConnection.autenticarPorToken(
                    this.qrToken
                );

                if (this.usuarioId == -1) {
                    output.writeObject(
                        new Mensaje(
                            "TEXTO",
                            "SERVIDOR",
                            "ERROR: Token QR inválido."
                        )
                    );
                    cerrarTodoSilencioso();
                    return;
                }
                this.nombreUsuario = solicitudInical.getRemitente();
            } else {
                cerrarTodoSilencioso();
                return;
            }

            // 2. LOGICA DE ASIGNACIÓN A LA SALA POR QR
            // Extraemos el QR de la sala que el cliente debió mandar en la propiedad correspondiente del mensaje
            String qrSalaToken = solicitudInical.getQrSalaToken();

            // Buscamos la sala asociada a ese QR en la base de datos
            SalaDTO sala = salaRepository.buscarPorQr(qrSalaToken);

            if (sala == null) {
                System.out.println(
                    "[SALA] El QR no existe en la BD. Creando nueva sala de forma dinámica..."
                );

                // Controlamos el tamaño del texto para evitar errores de substring
                String sufijo =
                    qrSalaToken.length() > 5
                        ? qrSalaToken.substring(0, 5)
                        : qrSalaToken;
                String nombreNuevaSala = "Grupo_" + sufijo;

                Integer nuevaSalaId = salaRepository.crearSala(
                    nombreNuevaSala,
                    qrSalaToken
                );
                this.salaId = nuevaSalaId;
            } else {
                this.salaId = sala.getId();
            }

            // Registrar la relación permanente en la tabla intermedia de la DB
            salaRepository.agregarParticipante(this.salaId, this.usuarioId);

            // Añadir el hilo del cliente al grupo en memoria ram de forma segura
            vincularClienteASalaMemoria(this.salaId, this);

            System.out.println(
                "[SALA] " +
                    nombreUsuario +
                    " ingresó a la sala ID: " +
                    this.salaId
            );

            // 3. BUCLE DEL CHAT EN VIVO (Filtrado por Sala)
            while (socket.isConnected()) {
                Mensaje mensajeRecibido = (Mensaje) input.readObject();
                if ("TEXTO".equals(mensajeRecibido.getTipo())) {
                    System.out.println(
                        "[Sala " +
                            this.salaId +
                            "][" +
                            nombreUsuario +
                            "]: " +
                            mensajeRecibido.getContenidoTexto()
                    );
                    retransmitirMensajeAlGrupo(mensajeRecibido);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(
                "[SERVIDOR] " +
                    (nombreUsuario != null ? nombreUsuario : "Un cliente") +
                    " se ha desconectado."
            );
        } finally {
            cerrarTodoSilencioso();
        }
    }

    // Método auxiliar encargado de inicializar listas dentro del mapa concurrente si no existen
    private static synchronized void vincularClienteASalaMemoria(
        Integer salaId,
        ClientHandler cliente
    ) {
        salasActivas
            .computeIfAbsent(salaId, k -> new CopyOnWriteArrayList<>())
            .add(cliente);
    }

    // El Broadcast ahora solo afecta a los miembros del mismo ID de grupo
    private void retransmitirMensajeAlGrupo(Mensaje mensaje) {
        CopyOnWriteArrayList<ClientHandler> miembros = salasActivas.get(
            this.salaId
        );

        if (miembros != null) {
            for (ClientHandler cliente : miembros) {
                if (cliente != this) {
                    // No nos lo enviamos a nosotros mismos
                    try {
                        if (
                            cliente.socket != null &&
                            !cliente.socket.isClosed() &&
                            cliente.socket.isConnected()
                        ) {
                            cliente.output.writeObject(mensaje);
                            cliente.output.flush();
                        } else {
                            miembros.remove(cliente);
                        }
                    } catch (IOException e) {
                        cliente.cerrarTodoSilencioso();
                    }
                }
            }
        }
    }

    public void cerrarTodoSilencioso() {
        // Al desconectarse, lo removemos específicamente de su sala asignada
        if (this.salaId != null && salasActivas.containsKey(this.salaId)) {
            salasActivas.get(this.salaId).remove(this);
            // Opcional: si la sala se queda vacía, puedes borrarla del mapa para liberar memoria
            if (salasActivas.get(this.salaId).isEmpty()) {
                salasActivas.remove(this.salaId);
            }
        }

        try {
            if (input != null) input.close();
        } catch (IOException e) {}
        try {
            if (output != null) output.close();
        } catch (IOException e) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {}
    }
}
