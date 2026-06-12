package com.uni.dto;

import java.time.LocalDateTime;

public class MensajeHistorialDTO {

    private Long id;

    private Integer salaId;

    private Integer remitenteId;

    private String contenido;

    private LocalDateTime enviadoEn;

    public MensajeHistorialDTO(
        Long id,
        Integer salaId,
        Integer remitenteId,
        String contenido,
        LocalDateTime enviadoEn
    ) {
        this.id = id;
        this.salaId = salaId;
        this.remitenteId = remitenteId;
        this.contenido = contenido;
        this.enviadoEn = enviadoEn;
    }

    public Long getId() {
        return id;
    }

    public Integer getSalaId() {
        return salaId;
    }

    public Integer getRemitenteId() {
        return remitenteId;
    }

    public String getContenido() {
        return contenido;
    }

    public LocalDateTime getEnviadoEn() {
        return enviadoEn;
    }
}
