-- Valida que los horarios laborales se carguen en bloques de media hora.
-- Ejemplos validos: 08:00, 08:30, 19:00.
-- Ejecutar una sola vez sobre una base existente.

ALTER TABLE horarios_laborales
DROP CONSTRAINT IF EXISTS chk_horario_laboral_media_hora;

ALTER TABLE horarios_laborales
ADD CONSTRAINT chk_horario_laboral_media_hora
CHECK (
    EXTRACT(MINUTE FROM hora_inicio) IN (0, 30)
    AND EXTRACT(MINUTE FROM hora_fin) IN (0, 30)
    AND EXTRACT(SECOND FROM hora_inicio) = 0
    AND EXTRACT(SECOND FROM hora_fin) = 0
);
