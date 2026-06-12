package com.uni.dto;

import java.io.Serializable;

public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipo; // "TEXTO" o "ARCHIVO"
    private String remitente;
    private String contenidoTexto; // Para los mensajes normales
    private byte[] archivoBytes; // Para los bytes de la imagen/archivo
    private String nombreArchivo; // Ej: "foto.png"
    private String qrToken;
    private String qrSalaToken; // Token QR que escaneó/seleccionó el usuario para entrar al grupo

    // Constructor para solicitar Registro o hacer Login
    public Mensaje(String tipo, String remitente, String qrToken) {
        this.tipo = tipo;
        this.remitente = remitente;
        this.qrToken = qrToken;
    }

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

    public void setRemitente(String remitente) {
        this.remitente = remitente; // Usa el nombre exacto de tu variable global aquí (ej. emisor, usuario, etc.)
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

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    // Getter y Setter correspondientes...
    public String getQrSalaToken() {
        return qrSalaToken;
    }

    public void setQrSalaToken(String qrSalaToken) {
        this.qrSalaToken = qrSalaToken;
    }
}
