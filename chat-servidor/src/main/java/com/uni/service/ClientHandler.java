package com.uni.service;

import com.uni.dto.Mensaje;
import com.uni.dto.SalaDTO;
import com.uni.repository.SalaRepository;
import com.uni.util.DatabaseConnection;
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

    // Repositorio de persistencia de salas e historial
    private static final SalaRepository salaRepository =
        new com.uni.util.SalaRepositoryImpl();

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
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
                    output.flush();
                    cerrarTodoSilencioso();
                    return;
                }

                // =========================================================================
                // SOLUCIÓN DEL NOMBRE: Recuperamos el nombre legítimo desde PostgreSQL
                // =========================================================================
                this.nombreUsuario = DatabaseConnection.obtenerNombrePorId(
                    this.usuarioId
                );
                if (
                    this.nombreUsuario == null || this.nombreUsuario.isEmpty()
                ) {
                    this.nombreUsuario = "Usuario_" + this.usuarioId;
                }
            } else {
                cerrarTodoSilencioso();
                return;
            }
            // =========================================================================
            // ENVIAR EL NOMBRE REAL CONFIRMADO AL CLIENTE
            // =========================================================================
            Mensaje msgConfig = new Mensaje("CONFIGURACION", "SERVIDOR", null);
            msgConfig.setQrToken(this.nombreUsuario); // Viaja el nombre real
            output.writeObject(msgConfig);
            output.flush();
            // 2. LOGICA DE ASIGNACIÓN A LA SALA POR QR
            String qrSalaToken = solicitudInical.getQrSalaToken();

            // Buscamos la sala asociada a ese QR en la base de datos
            SalaDTO sala = salaRepository.buscarPorQr(qrSalaToken);

            if (sala == null) {
                System.out.println(
                    "[SALA] El token proporcionado no existe. Generando una SALA NUEVA con un QR único..."
                );

                // GENERAMOS UN NUEVO QR ALEATORIO UNICO
                String nuevoQrSala = java.util.UUID.randomUUID().toString();
                String nombreNuevaSala = "Sala_" + nuevoQrSala.substring(0, 8);

                // Guardamos en la base de datos con el QR legítimamente nuevo
                Integer nuevaSalaId = salaRepository.crearSala(
                    nombreNuevaSala,
                    nuevoQrSala
                );
                this.salaId = nuevaSalaId;

                // NOTIFICAMOS AL CLIENTE: Le enviamos el nuevo QR de la sala que se acaba de crear
                try {
                    Mensaje respuestaQrSala = new Mensaje(
                        "ENTREGA_QR_SALA",
                        "SERVIDOR",
                        null
                    );
                    respuestaQrSala.setQrSalaToken(nuevoQrSala);
                    output.writeObject(respuestaQrSala);
                    output.flush();
                } catch (IOException e) {
                    System.err.println(
                        "[ERROR] No se pudo enviar el nuevo QR de la sala al cliente."
                    );
                }
            } else {
                System.out.println("[SALA] Sala encontrada con éxito.");
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

            // ====================================================================
            // CARGAR Y ENVIAR EL HISTORIAL DE LA SALA AL NUEVO DISPOSITIVO
            // ====================================================================
            java.util.List<Mensaje> historial = salaRepository.obtenerHistorial(
                this.salaId
            );
            if (!historial.isEmpty()) {
                // Para los mensajes informativos del sistema usamos un objeto DTO limpio
                // asegurando que el texto viaje en la propiedad esperada por tu repositorio/cliente
                Mensaje inicioH = new Mensaje("TEXTO", "SISTEMA", null);
                inicioH.setQrToken(
                    "--- Cargando historial de mensajes anteriores ---"
                );
                output.writeObject(inicioH);

                for (Mensaje msgAntiguo : historial) {
                    output.writeObject(msgAntiguo);
                }

                Mensaje finH = new Mensaje("TEXTO", "SISTEMA", null);
                finH.setQrToken(
                    "-------------------------------------------------"
                );
                output.writeObject(finH);
                output.flush();
            }

            // 3. BUCLE DEL CHAT EN VIVO (Filtrado por Sala)
            while (socket.isConnected()) {
                Mensaje mensajeRecibido = (Mensaje) input.readObject();
                if ("TEXTO".equals(mensajeRecibido.getTipo())) {
                    // Extraemos la cadena de texto que viaja en la propiedad qrToken
                    String textoMensaje = mensajeRecibido.getQrToken();

                    System.out.println(
                        "[Sala " +
                            this.salaId +
                            "][" +
                            nombreUsuario +
                            "]: " +
                            textoMensaje
                    );

                    // Forzamos que el remitente del mensaje sea el nombre real recuperado
                    mensajeRecibido.setRemitente(this.nombreUsuario);

                    // GUARDAR EN EL HISTORIAL DE POSTGRESQL
                    salaRepository.guardarMensaje(
                        this.salaId,
                        this.usuarioId,
                        textoMensaje
                    );

                    // Retransmitir en memoria RAM a los usuarios conectados
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

    private static synchronized void vincularClienteASalaMemoria(
        Integer salaId,
        ClientHandler cliente
    ) {
        salasActivas
            .computeIfAbsent(salaId, k -> new CopyOnWriteArrayList<>())
            .add(cliente);
    }

    private void retransmitirMensajeAlGrupo(Mensaje mensaje) {
        CopyOnWriteArrayList<ClientHandler> miembros = salasActivas.get(
            this.salaId
        );
        if (miembros != null) {
            for (ClientHandler cliente : miembros) {
                if (cliente != this) {
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
        if (this.salaId != null && salasActivas.containsKey(this.salaId)) {
            salasActivas.get(this.salaId).remove(this);
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
