package com.uni.repository;

import com.uni.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class MensajeRepository {

    public void guardarMensaje(
        Integer salaId,
        Integer remitenteId,
        String contenido
    ) {
        String sql = """
            INSERT INTO mensajes(
                sala_id,
                remitente_id,
                contenido
            )
            VALUES (?, ?, ?)
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, salaId);
            stmt.setInt(2, remitenteId);
            stmt.setString(3, contenido);

            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
