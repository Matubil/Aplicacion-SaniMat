-- =====================================================
-- BASE DE DATOS: Sistema de Gestión Clínica Médica
-- PostgreSQL
-- =====================================================

-- =====================================================
-- CREACIÓN DE BASE DE DATOS
-- Sistema de Gestión Clínica Médica
-- PostgreSQL
-- =====================================================

SELECT 'CREATE DATABASE clinica_medica'
WHERE NOT EXISTS (
    SELECT FROM pg_database
    WHERE datname = 'clinica_medica'
)\gexec

-- =====================================================
-- LIMPIEZA DE TABLAS EXISTENTES
-- Se eliminan primero las tablas dependientes
-- =====================================================

DROP TABLE IF EXISTS pagos;
DROP TABLE IF EXISTS turnos;
DROP TABLE IF EXISTS historias_clinicas;
DROP TABLE IF EXISTS horarios_laborales;
DROP TABLE IF EXISTS pacientes;
DROP TABLE IF EXISTS secretarios;
DROP TABLE IF EXISTS medicos;
DROP TABLE IF EXISTS especialidades;
DROP TABLE IF EXISTS personas;

DROP TABLE IF EXISTS enum_medio_pago;
DROP TABLE IF EXISTS enum_estado_turno;
DROP TABLE IF EXISTS enum_tipo_cobertura;

-- =====================================================
-- TABLAS TIPO ENUM
-- =====================================================

CREATE TABLE enum_tipo_cobertura (
                                     tipo_cobertura_id SERIAL PRIMARY KEY,
                                     nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE enum_estado_turno (
                                   estado_turno_id SERIAL PRIMARY KEY,
                                   nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE enum_medio_pago (
                                 medio_pago_id SERIAL PRIMARY KEY,
                                 nombre VARCHAR(50) NOT NULL UNIQUE
);

-- =====================================================
-- TABLA PERSONAS
-- Representa la clase abstracta Persona del UML
-- =====================================================

CREATE TABLE personas (
                          persona_id SERIAL PRIMARY KEY,
                          dni VARCHAR(15) NOT NULL UNIQUE,
                          nombre VARCHAR(100) NOT NULL,
                          apellido VARCHAR(100) NOT NULL,
                          genero VARCHAR(30),
                          fecha_nacimiento DATE NOT NULL,
                          direccion VARCHAR(150),
                          telefono VARCHAR(30),
                          mail VARCHAR(120),

                          CONSTRAINT chk_persona_dni_formato
                              CHECK (dni ~ '^[0-9]+$')
);

-- =====================================================
-- TABLA ESPECIALIDADES
-- =====================================================

CREATE TABLE especialidades (
                                especialidad_id SERIAL PRIMARY KEY,
                                nombre_especialidad VARCHAR(100) NOT NULL UNIQUE,
                                descripcion VARCHAR(255)
);

-- =====================================================
-- TABLA MÉDICOS
-- Relación 1 a 1 con personas
-- =====================================================

CREATE TABLE medicos (
                         medico_id SERIAL PRIMARY KEY,
                         persona_id INT NOT NULL UNIQUE,
                         matricula INT NOT NULL UNIQUE,
                         usuario VARCHAR(50) NOT NULL UNIQUE,
                         contrasenia VARCHAR(255) NOT NULL,
                         especialidad_id INT NOT NULL,

                         CONSTRAINT fk_medico_persona
                             FOREIGN KEY (persona_id)
                                 REFERENCES personas(persona_id),

                         CONSTRAINT fk_medico_especialidad
                             FOREIGN KEY (especialidad_id)
                                 REFERENCES especialidades(especialidad_id)
);

-- =====================================================
-- TABLA HORARIOS LABORALES
-- Un médico puede tener uno o varios horarios laborales
-- =====================================================

CREATE TABLE horarios_laborales (
                                    horario_laboral_id SERIAL PRIMARY KEY,
                                    medico_id INT NOT NULL,
                                    dia_semana VARCHAR(20) NOT NULL,
                                    hora_inicio TIME NOT NULL,
                                    hora_fin TIME NOT NULL,
                                    activo BOOLEAN NOT NULL DEFAULT TRUE,

                                    CONSTRAINT fk_horario_laboral_medico
                                        FOREIGN KEY (medico_id)
                                            REFERENCES medicos(medico_id),

                                    CONSTRAINT chk_horario_laboral_valido
                                        CHECK (hora_inicio < hora_fin),

                                    CONSTRAINT chk_horario_laboral_media_hora
                                        CHECK (
                                            EXTRACT(MINUTE FROM hora_inicio) IN (0, 30)
                                            AND EXTRACT(MINUTE FROM hora_fin) IN (0, 30)
                                            AND EXTRACT(SECOND FROM hora_inicio) = 0
                                            AND EXTRACT(SECOND FROM hora_fin) = 0
                                        )
);

-- =====================================================
-- TABLA SECRETARIOS
-- Relación 1 a 1 con personas
-- =====================================================

CREATE TABLE secretarios (
                             secretario_id SERIAL PRIMARY KEY,
                             persona_id INT NOT NULL UNIQUE,
                             usuario VARCHAR(50) NOT NULL UNIQUE,
                             contrasenia VARCHAR(255) NOT NULL,

                             CONSTRAINT fk_secretario_persona
                                 FOREIGN KEY (persona_id)
                                     REFERENCES personas(persona_id)
);

-- =====================================================
-- TABLA PACIENTES
-- Relación 1 a 1 con personas
-- =====================================================

CREATE TABLE pacientes (
                           paciente_id SERIAL PRIMARY KEY,
                           persona_id INT NOT NULL UNIQUE,
                           numero_afiliado VARCHAR(50),
                           tipo_cobertura_id INT NOT NULL,

                           CONSTRAINT fk_paciente_persona
                               FOREIGN KEY (persona_id)
                                   REFERENCES personas(persona_id),

                           CONSTRAINT fk_paciente_tipo_cobertura
                               FOREIGN KEY (tipo_cobertura_id)
                                   REFERENCES enum_tipo_cobertura(tipo_cobertura_id)
);

-- =====================================================
-- TABLA HISTORIAS CLÍNICAS
-- Relación con paciente
-- =====================================================

CREATE TABLE historias_clinicas (
                                    historia_clinica_id SERIAL PRIMARY KEY,
                                    paciente_id INT NOT NULL UNIQUE,
                                    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
                                    descripcion TEXT,

                                    CONSTRAINT fk_historia_paciente
                                        FOREIGN KEY (paciente_id)
                                            REFERENCES pacientes(paciente_id)
);

-- =====================================================
-- TABLA TURNOS
-- Relaciona paciente, médico y secretario
-- =====================================================

CREATE TABLE turnos (
                        turno_id SERIAL PRIMARY KEY,
                        paciente_id INT NOT NULL,
                        medico_id INT NOT NULL,
                        secretario_id INT,
                        fecha DATE NOT NULL,
                        hora TIME NOT NULL,
                        estado_turno_id INT NOT NULL,

                        CONSTRAINT fk_turno_paciente
                            FOREIGN KEY (paciente_id)
                                REFERENCES pacientes(paciente_id),

                        CONSTRAINT fk_turno_medico
                            FOREIGN KEY (medico_id)
                                REFERENCES medicos(medico_id),

                        CONSTRAINT fk_turno_secretario
                            FOREIGN KEY (secretario_id)
                                REFERENCES secretarios(secretario_id),

                        CONSTRAINT fk_turno_estado
                            FOREIGN KEY (estado_turno_id)
                                REFERENCES enum_estado_turno(estado_turno_id),

                        CONSTRAINT uq_turno_medico_fecha_hora
                            UNIQUE (medico_id, fecha, hora)
);

-- =====================================================
-- TABLA PAGOS
-- Relación 1 a 1 con turno
-- =====================================================

CREATE TABLE pagos (
                       pago_id SERIAL PRIMARY KEY,
                       turno_id INT NOT NULL UNIQUE,
                       monto NUMERIC(10,2) NOT NULL,
                       fecha_pago DATE NOT NULL DEFAULT CURRENT_DATE,
                       medio_pago_id INT NOT NULL,

                       CONSTRAINT fk_pago_turno
                           FOREIGN KEY (turno_id)
                               REFERENCES turnos(turno_id),

                       CONSTRAINT fk_pago_medio_pago
                           FOREIGN KEY (medio_pago_id)
                               REFERENCES enum_medio_pago(medio_pago_id),

                       CONSTRAINT chk_pago_monto
                           CHECK (monto >= 0)
);

-- =====================================================
-- CARGA INICIAL DE TABLAS ENUM
-- =====================================================

INSERT INTO enum_tipo_cobertura (nombre)
VALUES
    ('OBRA_SOCIAL'),
    ('PARTICULAR');

INSERT INTO enum_estado_turno (nombre)
VALUES
    ('CONFIRMADO'),
    ('CANCELADO'),
    ('AUSENTE'),
    ('FINALIZADO');

INSERT INTO enum_medio_pago (nombre)
VALUES
    ('EFECTIVO'),
    ('TARJETA'),
    ('TRANSFERENCIA'),
    ('NO_REQUIERE_PAGO');

-- =====================================================
-- CARGA INICIAL DE ESPECIALIDADES
-- =====================================================

INSERT INTO especialidades (nombre_especialidad, descripcion)
VALUES
    ('Clínica Médica', 'Atención general para pacientes adultos.'),
    ('Cardiología', 'Atención de enfermedades del corazón y sistema cardiovascular.'),
    ('Traumatología', 'Atención de lesiones óseas, musculares y articulares.'),
    ('Dermatología', 'Diagnóstico y tratamiento de enfermedades de la piel.'),
    ('Pediatría', 'Atención médica de niños y adolescentes.');

-- =====================================================
-- CARGA INICIAL DE PERSONAS
-- Personas 1 a 4: médicos
-- Personas 5 a 6: secretarios
-- Personas 7 a 12: pacientes
-- =====================================================

INSERT INTO personas (
    dni, nombre, apellido, genero, fecha_nacimiento, direccion, telefono, mail
)
VALUES
    ('20111222', 'Juan', 'Martínez', 'Masculino', '1978-03-10', 'Belgrano 100', '3514443333', 'juan.martinez@clinica.com'),
    ('22123456', 'Ana', 'López', 'Femenino', '1982-07-25', 'Colón 200', '3514442222', 'ana.lopez@clinica.com'),
    ('24345678', 'Roberto', 'Fernández', 'Masculino', '1975-11-02', 'San Martín 850', '3515554444', 'roberto.fernandez@clinica.com'),
    ('27888999', 'Luciana', 'Pereyra', 'Femenino', '1988-09-18', 'Av. Córdoba 1200', '3516667777', 'luciana.pereyra@clinica.com'),

    ('33444555', 'Pablo', 'Virgolini', 'Masculino', '1992-01-15', 'Rivadavia 500', '3513332222', 'pablo.virgolini@clinica.com'),
    ('34555666', 'Paula', 'Giménez', 'Femenino', '1995-06-21', 'Mitre 430', '3513331111', 'paula.gimenez@clinica.com'),

    ('25678345', 'Carlos', 'Gómez', 'Masculino', '1985-04-12', 'Av. Siempre Viva 123', '3514567890', 'carlos.gomez@mail.com'),
    ('30111222', 'Laura', 'Pérez', 'Femenino', '1990-08-20', 'San Martín 456', '3515551111', 'laura.perez@mail.com'),
    ('28777444', 'Miguel', 'Torres', 'Masculino', '1987-02-03', 'Ituzaingó 750', '3512223333', 'miguel.torres@mail.com'),
    ('39888111', 'Sofía', 'Ramírez', 'Femenino', '2001-12-10', 'Buenos Aires 980', '3517778888', 'sofia.ramirez@mail.com'),
    ('41222333', 'Valentina', 'Acosta', 'Femenino', '2004-05-28', 'Independencia 345', '3519990000', 'valentina.acosta@mail.com'),
    ('18222111', 'Jorge', 'Molina', 'Masculino', '1969-10-14', 'Maipú 610', '3511212121', 'jorge.molina@mail.com');

-- =====================================================
-- CARGA INICIAL DE MÉDICOS
-- =====================================================

INSERT INTO medicos (
    persona_id, matricula, usuario, contrasenia, especialidad_id
)
VALUES
    (1, 1234, 'medico1', 'hash_de_prueba_1', 1),
    (2, 5678, 'medico2', 'hash_de_prueba_2', 2),
    (3, 9012, 'medico3', 'hash_de_prueba_3', 3),
    (4, 3456, 'medico4', 'hash_de_prueba_4', 4);

-- =====================================================
-- CARGA INICIAL DE HORARIOS LABORALES
-- =====================================================

INSERT INTO horarios_laborales (
    medico_id, dia_semana, hora_inicio, hora_fin
)
VALUES
    -- Dr. Juan Martínez - Clínica Médica
    (1, 'LUNES', '08:00', '12:00'),
    (1, 'MIERCOLES', '08:00', '12:00'),
    (1, 'VIERNES', '10:00', '14:00'),

    -- Dra. Ana López - Cardiología
    (2, 'LUNES', '14:00', '18:00'),
    (2, 'MARTES', '09:00', '13:00'),
    (2, 'JUEVES', '09:00', '13:00'),

    -- Dr. Roberto Fernández - Traumatología
    (3, 'MARTES', '15:00', '19:00'),
    (3, 'MIERCOLES', '15:00', '19:00'),
    (3, 'VIERNES', '08:00', '12:00'),

    -- Dra. Luciana Pereyra - Dermatología
    (4, 'LUNES', '09:00', '13:00'),
    (4, 'JUEVES', '14:00', '18:00');

-- =====================================================
-- CARGA INICIAL DE SECRETARIOS
-- =====================================================

INSERT INTO secretarios (
    persona_id, usuario, contrasenia
)
VALUES
    (5, 'PabloSecre', '12345'),
    (6, 'secretaria2', 'hash_de_prueba_6');

-- =====================================================
-- CARGA INICIAL DE PACIENTES
-- tipo_cobertura_id:
-- 1 = OBRA_SOCIAL
-- 2 = PARTICULAR
-- =====================================================

INSERT INTO pacientes (
    persona_id, numero_afiliado, tipo_cobertura_id
)
VALUES
    (7, 'OS12345', 1),
    (8, NULL, 2),
    (9, 'OS67890', 1),
    (10, NULL, 2),
    (11, 'OS55577', 1),
    (12, NULL, 2);

-- =====================================================
-- CARGA INICIAL DE HISTORIAS CLÍNICAS
-- =====================================================

INSERT INTO historias_clinicas (
    paciente_id, fecha, descripcion
)
VALUES
    (1, CURRENT_DATE, 'Paciente sin antecedentes relevantes registrados al momento.'),
    (2, CURRENT_DATE, 'Paciente refiere antecedentes de alergia estacional.'),
    (3, CURRENT_DATE, 'Paciente con antecedente de lesión muscular previa.'),
    (4, CURRENT_DATE, 'Paciente sin antecedentes clínicos relevantes.'),
    (5, CURRENT_DATE, 'Paciente con controles pediátricos previos.'),
    (6, CURRENT_DATE, 'Paciente con antecedentes de hipertensión controlada.');

-- =====================================================
-- CARGA INICIAL DE TURNOS
-- estado_turno_id:
-- 1 = CONFIRMADO
-- 2 = CANCELADO
-- 3 = AUSENTE
-- 4 = FINALIZADO
-- =====================================================

INSERT INTO turnos (
    paciente_id, medico_id, secretario_id, fecha, hora, estado_turno_id
)
VALUES
    (1, 1, 1, '2026-05-20', '09:00', 1),
    (2, 2, 1, '2026-05-20', '14:30', 1),
    (3, 3, 2, '2026-05-21', '15:30', 1),
    (4, 4, 2, '2026-05-21', '09:30', 1),
    (5, 1, 1, '2026-05-22', '10:30', 4),
    (6, 2, 1, '2026-05-22', '10:00', 4),
    (1, 3, 2, '2026-05-23', '08:30', 2),
    (2, 4, 2, '2026-05-23', '14:30', 3);

-- =====================================================
-- CARGA INICIAL DE PAGOS
-- medio_pago_id:
-- 1 = EFECTIVO
-- 2 = TARJETA
-- 3 = TRANSFERENCIA
-- 4 = NO_REQUIERE_PAGO
-- =====================================================

INSERT INTO pagos (
    turno_id, monto, fecha_pago, medio_pago_id
)
VALUES
    (1, 5000.00, '2026-05-20', 1),
    (2, 7500.00, '2026-05-20', 2),
    (5, 5000.00, '2026-05-22', 3),
    (6, 3000.00, '2026-05-22', 1),
    (7, 0.00, '2026-05-23', 4);

-- =====================================================
-- SELECTS SIMPLES DE TABLAS PRINCIPALES
-- =====================================================

-- Personas registradas
SELECT *
FROM personas;

-- Pacientes registrados
SELECT *
FROM pacientes;

-- Médicos registrados
SELECT *
FROM medicos;

-- Secretarios registrados
SELECT *
FROM secretarios;

-- Especialidades médicas disponibles
SELECT *
FROM especialidades;

-- Horarios laborales cargados para los médicos
SELECT *
FROM horarios_laborales;

-- Turnos registrados
SELECT *
FROM turnos;

-- Pagos registrados
SELECT *
FROM pagos;

-- Historias clínicas registradas
SELECT *
FROM historias_clinicas;

-- Tipos de cobertura disponibles
SELECT *
FROM enum_tipo_cobertura;

-- Estados posibles de un turno
SELECT *
FROM enum_estado_turno;

-- Medios de pago disponibles
SELECT *
FROM enum_medio_pago;

-- CONSULTA 1: MÉDICOS CARDIOLOGOS DISPONIBLE EN LA SEMANA
SELECT
    e.nombre_especialidad AS especialidad,
    p.nombre AS nombre_medico,
    p.apellido AS apellido_medico,
    p.dni,
    m.matricula,
    hl.dia_semana,
    hl.hora_inicio,
    hl.hora_fin
FROM especialidades e
         JOIN medicos m
              ON m.especialidad_id = e.especialidad_id
         JOIN personas p
              ON p.persona_id = m.persona_id
         LEFT JOIN horarios_laborales hl
                   ON hl.medico_id = m.medico_id
WHERE e.nombre_especialidad = 'Cardiología'
ORDER BY p.apellido, hl.dia_semana, hl.hora_inicio;

-- CONSULTA 2: PACIENTES CON OBRA SOCIAL
SELECT
    pa.paciente_id,
    pe.dni,
    pe.nombre,
    pe.apellido,
    etc.nombre AS tipo_cobertura,
    pa.numero_afiliado
FROM pacientes pa
         JOIN personas pe
              ON pe.persona_id = pa.persona_id
         JOIN enum_tipo_cobertura etc
              ON etc.tipo_cobertura_id = pa.tipo_cobertura_id
WHERE etc.nombre = 'OBRA_SOCIAL'
ORDER BY pe.apellido, pe.nombre;

-- CONSULTA 3: MÉDICOS DISPONIBLES EN EL DIA LUNES
SELECT
    p.nombre AS nombre_medico,
    p.apellido AS apellido_medico,
    e.nombre_especialidad,
    hl.dia_semana,
    hl.hora_inicio,
    hl.hora_fin
FROM horarios_laborales hl
         JOIN medicos m
              ON m.medico_id = hl.medico_id
         JOIN personas p
              ON p.persona_id = m.persona_id
         JOIN especialidades e
              ON e.especialidad_id = m.especialidad_id
WHERE hl.dia_semana = 'LUNES'
  AND hl.activo = TRUE
ORDER BY hl.hora_inicio;
