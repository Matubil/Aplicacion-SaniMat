package com.sanimat.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
   Reglas de validacion reutilizables por los servicios y vistas.
 */
public final class Validator {
    private static final LocalDate MIN_PATIENT_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MIN_DOCTOR_BIRTH_DATE = LocalDate.of(1940, 1, 1);
    private static final Pattern DNI_PATTERN = Pattern.compile("\\d{7,8}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\s-]{6,30}$");

    private Validator() {
    }

    public static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El campo " + field + " es obligatorio.");
        }
    }

    public static void dni(String value) {
        required(value, "DNI");
        if (!DNI_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException("El DNI debe tener entre 7 y 8 digitos.");
        }
    }

    public static void email(String value) {
        if (value != null && !value.isBlank() && !EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException("El email no tiene un formato valido.");
        }
    }

    public static void phone(String value) {
        if (value != null && !value.isBlank() && !PHONE_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException("El telefono no tiene un formato valido.");
        }
    }

    public static void matricula(int value) {
        if (value < 100 || value > 999999999) {
            throw new ValidationException("La matricula debe ser un numero positivo de al menos 3 digitos.");
        }
    }

    public static void birthDate(LocalDate date, String field) {
        notNull(date, field);
        if (date.getYear() < 1 || date.getYear() > 9999) {
            throw new ValidationException("El campo " + field + " debe tener un anio entre 0001 y 9999.");
        }
        if (date.isBefore(MIN_PATIENT_BIRTH_DATE)) {
            throw new ValidationException("El campo " + field + " debe ser igual o posterior al 01/01/1900.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("El campo " + field + " no puede ser una fecha futura.");
        }
    }

    public static void adultBirthDate(LocalDate date, String field, int minimumAge) {
        birthDate(date, field);
        if (date.isBefore(MIN_DOCTOR_BIRTH_DATE)) {
            throw new ValidationException("El campo " + field + " del medico debe ser igual o posterior al 01/01/1940.");
        }
        if (date.isAfter(LocalDate.now().minusYears(minimumAge))) {
            throw new ValidationException("El medico debe tener al menos " + minimumAge + " anios.");
        }
    }

    public static void notNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException("El campo " + field + " es obligatorio.");
        }
    }

    public static void positive(BigDecimal amount, String field) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("El campo " + field + " debe ser mayor a cero.");
        }
    }

    public static void future(LocalDateTime dateTime, String field) {
        if (dateTime == null || !dateTime.isAfter(LocalDateTime.now())) {
            throw new ValidationException("El campo " + field + " debe ser una fecha futura.");
        }
    }
}
