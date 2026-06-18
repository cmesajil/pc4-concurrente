package com.uni.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.uni.repository.MensajeRepository;
import com.uni.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

class DatabaseConnectionTest {

    @Test
    void debeConectarseAPostgres() {
        assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
            }
        });
    }

    @Test
    void debeGuardarMensaje() throws Exception {
        MensajeRepository repo = new MensajeRepository();

        String texto = "Hola desde PostgreSQL";

        repo.guardarMensaje(1, 1, texto);

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                """
                    SELECT contenido
                    FROM mensajes
                    ORDER BY id DESC
                    LIMIT 1
                """
            )
        ) {
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next());

            assertEquals(texto, rs.getString("contenido"));
        }
    }
}
