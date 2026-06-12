package com.uni.dto;

public class UsuarioDTO {

    private Integer id;
    private String nombre;
    private String qrToken;

    public UsuarioDTO(Integer id, String nombre, String qrToken) {
        this.id = id;
        this.nombre = nombre;
        this.qrToken = qrToken;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getQrToken() {
        return qrToken;
    }
}
