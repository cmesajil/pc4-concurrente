package com.uni.repository;

import com.uni.dto.Mensaje;
import com.uni.dto.SalaDTO; // O como se llame tu clase DTO de salas
import com.uni.dto.SalaDTO;
import java.util.List;

public interface SalaRepository {
    Integer crearSala(String nombre, String qrToken);

    SalaDTO buscarPorQr(String qrToken);

    void agregarParticipante(Integer salaId, Integer usuarioId);

    // NUEVO MÉTODO PARA EL HISTORIAL
    void guardarMensaje(Integer salaId, Integer remitenteId, String contenido);

    // =========================================================================
    // LÍNEA FALTA: Agrega esto para que el compilador reconozca el método
    // =========================================================================
    List<Mensaje> obtenerHistorial(Integer salaId);
}
