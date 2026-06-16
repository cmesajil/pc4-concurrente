package com.uni.dto;

import java.math.BigDecimal;

public class ProductoDTO {

    private int id;
    private String nombre;
    private BigDecimal precio;

    public ProductoDTO(int id, String nombre, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }
}
