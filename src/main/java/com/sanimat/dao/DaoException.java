package com.sanimat.dao;

/**
 * Excepcion propia de la capa DAO para envolver errores SQL.
 */
public class DaoException extends RuntimeException {
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
