package com.sanimat.util;

/**
  Excepcion de validacion de negocio, pensada para mostrar mensajes claros al usuario.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
