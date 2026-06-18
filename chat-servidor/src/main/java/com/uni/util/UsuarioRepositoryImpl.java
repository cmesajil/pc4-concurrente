package com.uni.util;

import com.uni.dto.UsuarioDTO;
import com.uni.util.DatabaseConnection;
import java.sql.*;
import com.uni.repository.*;

public class UsuarioRepositoryImpl implements UsuarioRepository {

    @Override
    public Integer crearUsuario(String nombre, String qrToken) {
        String sql = """
            INSERT INTO usuarios(
                nombre,
                qr_token
            )
            VALUES (?, ?)
            RETURNING id
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, nombre);
            stmt.setString(2, qrToken);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public UsuarioDTO buscarPorQr(String qrToken) {
        String sql = """
            SELECT *
            FROM usuarios
            WHERE qr_token = ?
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, qrToken);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UsuarioDTO(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("qr_token")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
