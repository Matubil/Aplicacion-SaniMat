package com.sanimat.service;

import com.sanimat.dao.PagoDao;
import com.sanimat.dao.TurnoDao;
import com.sanimat.model.EstadoTurno;
import com.sanimat.model.Pago;
import com.sanimat.model.Turno;
import com.sanimat.model.MedioPago;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reglas de negocio para pagos.
 * Calcula descuentos y evita pagos parciales o pagos sobre turnos cancelados.
 */
public class PagoService {
    private static final BigDecimal PRECIO_CONSULTA = new BigDecimal("25000");
    private static final BigDecimal DESCUENTO_OBRA_SOCIAL = new BigDecimal("0.50");
    private static final BigDecimal DESCUENTO_EFECTIVO = new BigDecimal("0.15");

    private final PagoDao pagoDao;
    private final TurnoDao turnoDao;

    public PagoService(PagoDao pagoDao, TurnoDao turnoDao) {
        this.pagoDao = pagoDao;
        this.turnoDao = turnoDao;
    }

    public List<Pago> findAll() {
        return pagoDao.findAll();
    }

    public List<Turno> findUnpaidAppointments() {
        return turnoDao.findUnpaidAppointments();
    }

    public BigDecimal calcularMontoEsperado(Turno turno, MedioPago medioPago) {
        if (turno == null) {
            return null;
        }
        // Regla del prototipo: obra social descuenta 50% y efectivo descuenta 15%.
        BigDecimal monto = PRECIO_CONSULTA;
        if ("OBRA_SOCIAL".equals(turno.getTipoCobertura())) {
            monto = monto.multiply(BigDecimal.ONE.subtract(DESCUENTO_OBRA_SOCIAL));
        }
        if (medioPago == MedioPago.EFECTIVO) {
            monto = monto.multiply(BigDecimal.ONE.subtract(DESCUENTO_EFECTIVO));
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    public void save(Pago pago) {
        Validator.notNull(pago.getTurnoId() == 0 ? null : pago.getTurnoId(), "turno");
        Validator.notNull(pago.getMedioPago(), "medio de pago");
        Validator.notNull(pago.getFechaPago(), "fecha de pago");

        Turno turno = turnoDao.findById(pago.getTurnoId())
                .orElseThrow(() -> new ValidationException("El turno seleccionado no existe."));
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ValidationException("Los turnos cancelados no requieren registrar pago.");
        }
        BigDecimal montoEsperado = calcularMontoEsperado(turno, pago.getMedioPago());

        if (pago.getMedioPago() == MedioPago.NO_REQUIERE_PAGO) {
            throw new ValidationException("Este turno requiere registrar un pago por $" + montoEsperado.toPlainString() + ".");
        }

        Validator.positive(pago.getMonto(), "monto");
        // Se exige pago exacto para no dejar turnos parcialmente abonados.
        if (pago.getMonto().compareTo(montoEsperado) != 0) {
            throw new ValidationException("No se permiten pagos parciales. El monto debe ser exactamente $" + montoEsperado.toPlainString() + ".");
        }
        pagoDao.save(pago);
    }
}
