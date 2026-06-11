package com.uni.dto;

import java.io.Serializable;

public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipo; // "TEXTO" o "ARCHIVO"
    private String remitente;
    private String contenidoTexto; // Para los mensajes normales
    private byte[] archivoBytes; // Para los bytes de la imagen/archivo
    private String nombreArchivo; // Ej: "foto.png"

    // Constructor para Texto
    public Mensaje(String remitente, String contenidoTexto) {
        this.tipo = "TEXTO";
        this.remitente = remitente;
        this.contenidoTexto = contenidoTexto;
    }

    // Constructor para Archivos
    public Mensaje(
        String remitente,
        byte[] archivoBytes,
        String nombreArchivo
    ) {
        this.tipo = "ARCHIVO";
        this.remitente = remitente;
        this.archivoBytes = archivoBytes;
        this.nombreArchivo = nombreArchivo;
    }

    // Getters
    public String getTipo() {
        return tipo;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getContenidoTexto() {
        return contenidoTexto;
    }

    public byte[] getArchivoBytes() {
        return archivoBytes;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }
}
