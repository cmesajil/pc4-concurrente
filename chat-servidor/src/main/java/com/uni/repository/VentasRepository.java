package com.uni.repository;

import com.uni.dto.ProductoDTO;
import com.uni.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentasRepository {

    // Obtener la lista pequeña de productos
    public List<ProductoDTO> obtenerProductos() {
        List<ProductoDTO> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, precio FROM productos";
        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                lista.add(
                    new ProductoDTO(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("precio")
                    )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Registrar cliente (Ventas) si no existe y retornar su ID
    public int registrarClienteOObtener(String nombre) {
        String buscarSql = "SELECT id FROM clientes WHERE nombre = ?";
        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(buscarSql)
        ) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String insertarSql =
            "INSERT INTO clientes (nombre) VALUES (?) RETURNING id";
        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(insertarSql)
        ) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Registrar Pedido y Comprobante en una sola transacción
    public boolean procesarVenta(int clienteId, int productoId) {
        String sqlPedido =
            "INSERT INTO pedidos (cliente_id, producto_id, estado) VALUES (?, ?, 'pagado') RETURNING id";
        String sqlComprobante =
            "INSERT INTO comprobantes (pedido_id, saldo_pagado) VALUES (?, (SELECT precio FROM productos WHERE id = ?))";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Transacción para asegurar consistencia

            int pedidoId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sqlPedido)) {
                ps.setInt(1, clienteId);
                ps.setInt(2, productoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) pedidoId = rs.getInt("id");
                }
            }

            if (pedidoId != -1) {
                try (
                    PreparedStatement ps = conn.prepareStatement(sqlComprobante)
                ) {
                    ps.setInt(1, pedidoId);
                    ps.setInt(2, productoId);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
