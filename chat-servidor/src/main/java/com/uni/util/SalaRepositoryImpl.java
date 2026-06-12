package com.uni.repository;

import com.uni.dto.SalaDTO;
import com.uni.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SalaRepositoryImpl implements SalaRepository {

    @Override
    public Integer crearSala(String nombre, String qrToken) {
        // Asume que tu tabla 'salas' tiene columnas: id (serial), nombre, qr_token
        String sql = "INSERT INTO salas (nombre, qr_token) VALUES (?, ?)";

        // Usamos DatabaseConnection.getConnection() asumiendo que tu clase utilitaria provee la conexión
        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, qrToken);
            pstmt.executeUpdate();

            // Recuperamos el ID autogenerado por Postgres
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println(
                "[ERROR DB] Error al crear la sala: " + e.getMessage()
            );
        }
        return null;
    }

    @Override
    public SalaDTO buscarPorQr(String qrToken) {
        String sql = "SELECT id, nombre FROM salas WHERE qr_token = ?";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, qrToken);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    return new SalaDTO(id, nombre);
                }
            }
        } catch (SQLException e) {
            System.err.println(
                "[ERROR DB] Error al buscar sala por QR: " + e.getMessage()
            );
        }
        return null; // Retorna null si no la encuentra, activando la creación automática en el ClientHandler
    }

    @Override
    public void agregarParticipante(Integer salaId, Integer usuarioId) {
        // Asume una tabla intermedia para la relación de muchos a muchos: sala_participantes
        String sql =
            "INSERT INTO sala_participantes (sala_id, usuario_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, salaId);
            pstmt.setInt(2, usuarioId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(
                "[ERROR DB] Error al asociar participante a la sala: " +
                    e.getMessage()
            );
        }
    }
}
