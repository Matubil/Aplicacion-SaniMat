package com.sanimat.service;

import com.sanimat.dao.UsuarioDao;
import com.sanimat.model.Usuario;
import com.sanimat.util.PasswordUtil;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

/**
 * Servicio de autenticacion.
 * Valida credenciales y devuelve el usuario con sus datos de rol.
 */
public class AuthService {
    private final UsuarioDao usuarioDao;

    public AuthService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public Usuario login(String username, char[] password) {
        Validator.required(username, "usuario");
        Validator.notNull(password, "contraseña");

        Usuario usuario = usuarioDao.findByUsername(username)
                .orElseThrow(() -> new ValidationException("Usuario o contraseña incorrectos."));

        if (!PasswordUtil.verify(password, usuario.getPasswordHash())) {
            throw new ValidationException("Usuario o contraseña incorrectos.");
        }
        return usuario;
    }
}
