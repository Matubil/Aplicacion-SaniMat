package com.sanimat.controller;

import com.sanimat.dao.UsuarioDao;
import com.sanimat.model.Usuario;
import com.sanimat.service.AuthService;

/**
 * Controlador minimo para autenticar usuarios desde la pantalla de login.
 */
public class LoginController {
    private final AuthService authService;

    public LoginController(UsuarioDao usuarioDao) {
        this.authService = new AuthService(usuarioDao);
    }

    public Usuario login(String username, String password) {
        return authService.login(username, password == null ? new char[0] : password.toCharArray());
    }
}
