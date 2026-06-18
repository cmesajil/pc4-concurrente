package com.uni.dto;

import java.io.Serializable;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tipo; // "TEXTO" o "ARCHIVO"
    private String remitente;
    private String contenidoTexto;
    private byte[] archivoBytes;
    private String nombreArchivo;
    private String qrToken;
    private String qrSalaToken;

    public Mensaje(String tipo, String remitente, String qrToken) {
        this.tipo = tipo;
        this.remitente = remitente;
        this.qrToken = qrToken;
    }

    public Mensaje(String remitente, String contenidoTexto) {
        this.tipo = "TEXTO";
        this.remitente = remitente;
        this.contenidoTexto = contenidoTexto;
    }

    public Mensaje(String remitente, byte[] archivoBytes, String nombreArchivo) {
        this.tipo = "ARCHIVO";
        this.remitente = remitente;
        this.archivoBytes = archivoBytes;
        this.nombreArchivo = nombreArchivo;
    }

    // Getters y setters exactamente iguales que en el servidor
    public String getTipo() { return tipo; }
    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }
    public String getContenidoTexto() { return contenidoTexto; }
    public byte[] getArchivoBytes() { return archivoBytes; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getQrSalaToken() { return qrSalaToken; }
    public void setQrSalaToken(String qrSalaToken) { this.qrSalaToken = qrSalaToken; }
}
