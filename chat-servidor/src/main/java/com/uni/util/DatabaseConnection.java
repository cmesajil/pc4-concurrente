package com.uni.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseConnection {

    private static final String URL =
        "jdbc:postgresql://localhost:5433/postgres";

    private static final String USER = "postgres";

    private static final String PASSWORD = "root";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Crea un usuario nuevo en la DB generando un QR Token único (UUID).
     * Devuelve el token generado para enviárselo al cliente.
     */
    public static String crearNuevoUsuario(String nombre) {
        String uniqueToken = UUID.randomUUID().toString(); // Genera un token único y seguro
        String sql =
            "INSERT INTO usuarios (nombre, qr_token) VALUES (?, ?) RETURNING qr_token";

        try (
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, nombre);
            ps.setString(2, uniqueToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("qr_token");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca si el token del QR ya existe en la base de datos.
     * Devuelve el ID del usuario si lo encuentra, o -1 si no existe.
     */
    public static int autenticarPorToken(String qrToken) {
        String sql = "SELECT id FROM usuarios WHERE qr_token = ?";
        try (
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, qrToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String obtenerNombrePorId(int usuarioId) {
        String sql = "SELECT nombre FROM usuarios WHERE id = ?";
        try (
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }
        } catch (SQLException e) {
            System.err.println(
                "[ERROR DB] No se pudo obtener el nombre del usuario: " +
                    e.getMessage()
            );
        }
        return null;
    }
}
