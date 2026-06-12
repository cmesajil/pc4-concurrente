package com.uni.repository;

import com.uni.dto.UsuarioDTO;

public interface UsuarioRepository {
    Integer crearUsuario(String nombre, String qrToken);

    UsuarioDTO buscarPorQr(String qrToken);
}
