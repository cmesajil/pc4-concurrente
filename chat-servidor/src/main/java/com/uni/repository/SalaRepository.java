package com.uni.repository;

import com.uni.dto.SalaDTO; // O como se llame tu clase DTO de salas

public interface SalaRepository {
    Integer crearSala(String nombre, String qrToken);

    SalaDTO buscarPorQr(String qrToken);

    void agregarParticipante(Integer salaId, Integer usuarioId);

    // NUEVO MÉTODO PARA EL HISTORIAL
    void guardarMensaje(Integer salaId, Integer remitenteId, String contenido);
}
