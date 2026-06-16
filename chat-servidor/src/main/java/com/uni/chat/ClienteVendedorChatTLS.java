package com.uni.chat;

import com.uni.dto.Mensaje;
import com.uni.dto.ProductoDTO;
import com.uni.repository.VentasRepository;
import com.uni.util.DatabaseConnection;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class ClienteVendedorChatTLS {

    private static final String QR_VENDEDOR_EXISTENTE =
        "QR_VENDEDOR_OFFICIAL_XYZ"; // Su identificador único
    private static final String QR_SALA_VENTAS = "SALA_VENTAS_PRINCIPAL"; // La sala donde atiende

    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 8888;

        String rutaCertificadoPublico = Paths.get("trust_container_root.p12")
            .toAbsolutePath()
            .toString();
        char[] passwordCert = "container-password".toCharArray();

        try {
            // 1. Configuración de Seguridad SSL/TLS
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = new FileInputStream(rutaCertificadoPublico)) {
                trustStore.load(is, passwordCert);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, tmf.getTrustManagers(), null);

            SSLSocketFactory ssf = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) ssf.createSocket(host, puerto);
            socket.startHandshake();

            System.out.println(
                "[SISTEMA VENDEDOR] Conexión segura TLS Establecida de forma correcta."
            );

            ObjectOutputStream out = new ObjectOutputStream(
                socket.getOutputStream()
            );
            ObjectInputStream in = new ObjectInputStream(
                socket.getInputStream()
            );

            // 2. Login Automático del Vendedor al iniciar el programa
            // Enviamos un mensaje de tipo LOGIN apuntando a la sala de ventas
            Mensaje loginVendedor = new Mensaje(
                "LOGIN",
                "Vendedor Automático",
                null
            );
            loginVendedor.setQrToken(QR_VENDEDOR_EXISTENTE);
            loginVendedor.setQrSalaToken(QR_SALA_VENTAS);

            out.writeObject(loginVendedor);
            out.flush();
            System.out.println(
                "[SISTEMA VENDEDOR] Sesión iniciada en la sala: " +
                    QR_SALA_VENTAS
            );

            // Instanciamos el repositorio que interactúa con las 4 tablas de ventas
            VentasRepository repository = new VentasRepository();

            // 3. HILO DE ESCUCHA Y AUTOMATIZACIÓN (ADAPTADO AL SERVIDOR)
            new Thread(() -> {
                try {
                    while (true) {
                        Mensaje deServidor = (Mensaje) in.readObject();

                        // Validamos que sea un mensaje de texto y que no sea eco de nosotros mismos
                        if (
                            "TEXTO".equals(deServidor.getTipo()) &&
                            !"Vendedor Automático".equals(
                                deServidor.getRemitente()
                            )
                        ) {
                            // LEER: El servidor mete el texto plano en el atributo QrToken
                            if (deServidor.getQrToken() == null) continue;
                            String textoRecibido = deServidor
                                .getQrToken()
                                .trim()
                                .toUpperCase();
                            String compradorNombre = deServidor.getRemitente();

                            System.out.println(
                                "[LOG] Recibido de " +
                                    compradorNombre +
                                    ": " +
                                    textoRecibido
                            );

                            // CASO 1: Catálogo (Usamos CONTAINS para que sea más flexible si Leyla escribe una frase larga)
                            if (
                                textoRecibido.contains("MENU") ||
                                textoRecibido.contains("CATALOGO") ||
                                textoRecibido.contains("PRODUCTO")
                            ) {
                                List<ProductoDTO> productos =
                                    repository.obtenerProductos();
                                StringBuilder sb = new StringBuilder(
                                    "=== LISTA DE PRODUCTOS ===\n"
                                );
                                for (ProductoDTO p : productos) {
                                    sb.append(p.getId())
                                        .append(". ")
                                        .append(p.getNombre())
                                        .append(" - $")
                                        .append(p.getPrecio())
                                        .append("\n");
                                }
                                sb.append(
                                    "--------------------------------------------------\n"
                                );
                                sb.append("Para comprar, escribe: PAGAR [ID]");

                                // ESCRIBIR: Forzamos el texto de respuesta dentro de setQrToken para que el servidor lo lea sin problemas
                                Mensaje respuestaMenu = new Mensaje(
                                    "TEXTO",
                                    "Vendedor Automático",
                                    null
                                );
                                respuestaMenu.setQrToken(sb.toString());

                                out.writeObject(respuestaMenu);
                                out.flush();
                            }
                            // CASO 2: Compra Directa
                            else if (textoRecibido.contains("PAGAR")) {
                                try {
                                    // Extraemos el ID aislando el número del texto limpio
                                    String limpio = textoRecibido.replaceAll(
                                        "[^0-8]",
                                        ""
                                    );
                                    int productoId = Integer.parseInt(limpio);

                                    int clienteId =
                                        repository.registrarClienteOObtener(
                                            compradorNombre
                                        );
                                    boolean exito = repository.procesarVenta(
                                        clienteId,
                                        productoId
                                    );

                                    Mensaje respuestaVenta = new Mensaje(
                                        "TEXTO",
                                        "Vendedor Automático",
                                        null
                                    );

                                    if (exito) {
                                        String nombreProd = repository
                                            .obtenerProductos()
                                            .stream()
                                            .filter(
                                                p -> p.getId() == productoId
                                            )
                                            .map(ProductoDTO::getNombre)
                                            .findFirst()
                                            .orElse("Producto #" + productoId);

                                        String comprobanteTexto =
                                            "=== COMPROBANTE DE PAGO ===\n" +
                                            "Cliente: " +
                                            compradorNombre +
                                            "\n" +
                                            "Detalle: " +
                                            nombreProd +
                                            "\n" +
                                            "Estado: Guardado en Base de Datos\n" +
                                            "¡Gracias por tu compra!";

                                        respuestaVenta.setQrToken(
                                            comprobanteTexto
                                        );
                                    } else {
                                        respuestaVenta.setQrToken(
                                            "[ERROR] ID de producto no encontrado. Escribe MENU."
                                        );
                                    }

                                    out.writeObject(respuestaVenta);
                                    out.flush();
                                } catch (Exception e) {
                                    Mensaje errorMsg = new Mensaje(
                                        "TEXTO",
                                        "Vendedor Automático",
                                        null
                                    );
                                    errorMsg.setQrToken(
                                        "[ERROR] Usa el formato correcto, ejemplo: PAGAR 1"
                                    );
                                    out.writeObject(errorMsg);
                                    out.flush();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(
                        "[SISTEMA VENDEDOR] Conexión perdida con el servidor o error de casteo."
                    );
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
