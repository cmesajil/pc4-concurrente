package com.uni.dto;

public class SalaDTO {

    private Integer id;
    private String nombre;

    public SalaDTO(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
