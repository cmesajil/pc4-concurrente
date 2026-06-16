package com.uni.service;

import com.uni.dto.ProductoDTO;
import com.uni.repository.VentasRepository;
import java.util.List;

public class AutomatizacionVentaService {

    private final VentasRepository repository = new VentasRepository();

    public String procesarMensajeTexto(
        String nombreClienteChat,
        String textoMensaje
    ) {
        String comando = textoMensaje.trim().toUpperCase();

        // Flujo 1: Mostrar Catálogo
        if ("MENU".equals(comando) || "PRODUCTOS".equals(comando)) {
            List<ProductoDTO> productos = repository.obtenerProductos();
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
            sb.append("--------------------------------------------------\n");
            sb.append("Para comprar, escribe: PAGAR [Numero de ID]");
            return sb.toString();
        }

        // Flujo 2: Ejecutar proceso automático de compra y comprobante
        if (comando.startsWith("PAGAR ")) {
            try {
                String idStr = comando.replace("PAGAR ", "").trim();
                int productoId = Integer.parseInt(idStr);

                // 1. Asegurar registro en la tabla de clientes (usando el nombre de pantalla de la App)
                int clienteId = repository.registrarClienteOObtener(
                    nombreClienteChat
                );

                // 2. Procesar inserciones en DB
                boolean exito = repository.procesarVenta(clienteId, productoId);

                if (exito) {
                    List<ProductoDTO> productos = repository.obtenerProductos();
                    String nombreProducto = productos
                        .stream()
                        .filter(p -> p.getId() == productoId)
                        .map(ProductoDTO::getNombre)
                        .findFirst()
                        .orElse("Producto");

                    // Retornar la estructura exacta que pediste para el comprobante
                    return (
                        "=== COMPROBANTE DE PAGO ===\n" +
                        "Cliente: " +
                        nombreClienteChat +
                        "\n" +
                        "Detalle: " +
                        nombreProducto +
                        "\n" +
                        "Estado del Pedido: PAGADO (Guardado automáticamente)\n" +
                        "¡Gracias por su compra!"
                    );
                } else {
                    return "[SISTEMA VENDEDOR] Error al procesar el pago. ID de producto inválido.";
                }
            } catch (NumberFormatException e) {
                return "[SISTEMA VENDEDOR] Formato incorrecto. Ejemplo: PAGAR 1";
            }
        }

        // Mensaje por defecto si están interactuando en la sala del vendedor
        return (
            "[SISTEMA VENDEDOR] Hola " +
            nombreClienteChat +
            ". Escribe 'MENU' para ver los productos disponibles."
        );
    }
}
