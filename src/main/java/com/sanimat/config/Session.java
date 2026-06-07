package com.sanimat.config;

import com.sanimat.model.Usuario;

/**
 * Guarda el usuario actual en memoria mientras la aplicacion esta abierta.
 */
public final class Session {
    private static Usuario currentUser;

    private Session() {
    }

    public static Usuario getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Usuario currentUser) {
        Session.currentUser = currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
