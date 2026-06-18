package com.uni.chat;

import javafx.application.Application;

public class MainDesktop {
    public static void main(String[] args) {
        // Esto inicializa el toolkit de JavaFX sin conflictos de herencia directa en el punto de entrada
        Application.launch(ClienteDesktopFX.class, args);
    }
}