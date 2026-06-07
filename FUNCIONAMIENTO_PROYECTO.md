# Funcionamiento del proyecto

La aplicacion SaniMat es un prototipo de escritorio para una clinica medica. Permite iniciar sesion, navegar segun el rol del usuario y gestionar especialidades, medicos, pacientes, turnos, pagos e historias clinicas usando PostgreSQL mediante JDBC.

## Flujo general

1. `MainApp` inicia JavaFX y muestra `LoginView`.
2. `LoginController` envia usuario y contrasenia a `AuthService`.
3. `AuthService` consulta credenciales mediante `UsuarioDao`.
4. Si las credenciales son validas, se crea una sesion y se muestra `MainShell`.
5. `MainShell` construye el menu lateral segun el rol del usuario.
6. Cada pantalla JavaFX usa servicios para ejecutar reglas de negocio.
7. Los servicios usan DAOs para leer y escribir datos en PostgreSQL.

## Roles y navegacion

| Rol | Pantallas disponibles |
| --- | --- |
| Secretaria | Home, Turnos, Pacientes, Pagos, Medicos y Especialidades. |
| Medico | Home, Turnos, Historial Clinico, Generar Receta como funcionalidad futura y Editar Perfil. |
| Paciente | El rol existe en el modelo, pero no tiene credenciales ni menu propio en la base actual. |

## Conexion a PostgreSQL

La conexion se configura en:

```text
src/main/resources/database.properties
```

Valores actuales:

```text
db.url=jdbc:postgresql://localhost:5432/clinica_medica
db.user=postgres
db.password=Maty
```

`DatabaseConfig` tambien permite usar variables de entorno:

```text
SANIMAT_DB_URL
SANIMAT_DB_USER
SANIMAT_DB_PASSWORD
```

## Login

El login valida usuarios de secretarios y medicos. La base actual no trae credenciales operativas para pacientes, por eso el acceso de paciente no queda disponible desde la interfaz.

La pantalla de login envia usuario y contrasenia al controlador. Si son correctos, se abre la ventana principal con el menu correspondiente al rol.

## Gestion de especialidades

La secretaria puede:

- Consultar especialidades desde una tabla.
- Buscar por texto.
- Crear una nueva especialidad.
- Modificar una especialidad seleccionada.
- Eliminar una especialidad.

La regla principal es que no se puede registrar otra especialidad con el mismo nombre.

## Gestion de medicos

La secretaria puede:

- Consultar medicos.
- Buscar por nombre, DNI o matricula.
- Registrar un nuevo medico.
- Modificar datos de un medico seleccionado.
- Cargar dias y rangos de horarios laborales.
- Eliminar un medico si no tiene turnos registrados.

Validaciones principales:

- DNI obligatorio y numerico.
- Fecha de nacimiento obligatoria.
- Matricula numerica y unica.
- Usuario obligatorio y unico.
- Especialidad obligatoria.
- Al menos un horario laboral si se carga desde la gestion de medicos.
- Los horarios laborales deben usar bloques de 30 minutos y no superponerse en el mismo dia.
- Email y telefono con formato valido.
- Nombre, apellido y direccion se normalizan con formato capitalizado.

## Perfil del medico

El medico autenticado puede ingresar a `Editar Perfil` y modificar sus datos personales. Esta pantalla usa la misma capa de servicio de medicos, por lo que conserva validaciones de DNI, email, telefono, matricula y usuario.

## Gestion de pacientes

La secretaria puede:

- Consultar pacientes.
- Buscar por nombre o DNI.
- Registrar un paciente.
- Modificar datos de un paciente seleccionado.
- Eliminar un paciente si no tiene turnos confirmados ni historia clinica.

Validaciones principales:

- DNI obligatorio y numerico.
- Fecha de nacimiento obligatoria.
- Tipo de cobertura obligatorio.
- Email y telefono con formato valido.
- Nombre, apellido y direccion se normalizan con formato capitalizado.

## Gestion de turnos

La secretaria puede registrar, consultar, modificar y cancelar turnos.

Reglas principales:

- Un turno nuevo siempre nace como `Confirmado`.
- El calendario habilita solo los dias donde el medico trabaja.
- Los horarios se generan cada 30 minutos segun `horarios_laborales`.
- Los horarios ocupados aparecen como no disponibles.
- No se permite superponer turnos confirmados de un mismo medico.
- Para cancelar se usa el boton `Cancelar turno`.
- Un turno cancelado o finalizado ya no se puede modificar.
- `Ausente` y `Finalizado` se pueden asignar desde modificar cuando la fecha y hora del turno ya pasaron.

La disponibilidad se valida en dos lugares:

- En la interfaz, para guiar al usuario.
- En `TurnoService`, para proteger la regla aunque se intente guardar desde otro flujo.

## Registro de pagos

La secretaria puede consultar turnos pendientes de pago y registrar pagos.

Reglas principales:

- Precio base de consulta: `$25.000`.
- Obra social aplica 50% de descuento.
- Particular no aplica descuento por cobertura.
- Pago en efectivo aplica 15% de descuento adicional.
- Tarjeta y transferencia no aplican descuento adicional.
- Los turnos cancelados no requieren pago.
- No se permiten pagos parciales: el monto debe coincidir exactamente con el monto calculado.

Los valores principales se encuentran como constantes en `PagoService`.

## Historia clinica

El medico puede:

- Buscar y seleccionar pacientes.
- Consultar la historia clinica del paciente seleccionado.
- Crear una nueva entrada clinica.
- Modificar el contenido del historial existente.

La tabla `historias_clinicas` contiene una historia clinica asociada al paciente. Para nuevas entradas, la aplicacion agrega el nuevo bloque fechado dentro de la descripcion existente.

La fecha de una nueva entrada se toma automaticamente desde la fecha actual del sistema y no se edita desde la pantalla. Al modificar el historial, se valida que las fechas escritas tengan formato completo `dd/mm/aaaa`.

La eliminacion de historia clinica no esta implementada actualmente.

## Funciones futuras

`Generar Receta` y algunos reportes aparecen como funcionalidades futuras. No modifican datos de la base y muestran un mensaje indicando que quedan fuera del alcance implementado del prototipo.

## Cumplimiento de requerimientos funcionales

| RF | Caso de uso | Estado | Observacion |
| --- | --- | --- | --- |
| RF-01 | Registrar especialidad | Cumple | `SpecialtyManagementView`, `EspecialidadService`, `EspecialidadDao.save`. |
| RF-02 | Consultar especialidad | Cumple | Tabla y busqueda de especialidades. |
| RF-03 | Modificar especialidad | Cumple | Seleccion, boton modificar y guardado. |
| RF-04 | Eliminar especialidad | Cumple | Boton eliminar y DAO delete. Puede fallar si la base impide eliminar por dependencias. |
| RF-05 | Registrar medico | Cumple | Alta desde gestion de medicos. |
| RF-06 | Consultar medico | Cumple | Tabla y busqueda de medicos. |
| RF-07 | Modificar medico | Cumple | Edicion de datos del medico seleccionado. |
| RF-08 | Eliminar medico | Cumple | Baja permitida si no tiene turnos registrados. |
| RF-09 | Registrar paciente | Cumple | Alta desde gestion de pacientes. |
| RF-10 | Consultar paciente | Cumple | Tabla y busqueda de pacientes. |
| RF-11 | Modificar paciente | Cumple | Edicion de paciente seleccionado. |
| RF-12 | Eliminar paciente | Cumple | Baja permitida si no tiene turnos confirmados ni historia clinica. |
| RF-13 | Registrar turno | Cumple para secretaria, parcial para paciente | Secretaria puede agendar. El rol paciente existe, pero no tiene menu/login operativo en la base actual. |
| RF-14 | Validar disponibilidad y evitar superposicion | Cumple | Usa horarios laborales, bloques de 30 minutos y valida superposicion de turnos confirmados. |
| RF-15 | Consultar turno | Cumple para secretaria y medico, parcial para paciente | Secretaria consulta todos, medico consulta sus turnos. Paciente esta previsto en servicio, pero sin menu/login operativo. |
| RF-16 | Modificar turno | Cumple | Permite cambiar datos de turnos existentes, con restricciones de estado. |
| RF-17 | Eliminar turno | Cumple como cancelacion | No borra fisicamente el registro; cambia estado a `Cancelado`, que es mejor para trazabilidad. |
| RF-18 | Registrar pago de turno | Cumple | Registra pagos de turnos pendientes y valida monto exacto. |
| RF-19 | Registrar historia clinica | Cumple | Permite crear una entrada clinica para un paciente. |
| RF-20 | Consultar historia clinica | Cumple | Permite seleccionar paciente y ver su historial. |
| RF-21 | Modificar historia clinica | Cumple | Permite editar el contenido del historial existente. |
| RF-22 | Eliminar historia clinica | No implementado | No hay boton, metodo de servicio ni metodo DAO para eliminar historias clinicas. |

## Conclusion de cumplimiento

El prototipo cumple la mayor parte de los requerimientos funcionales planteados. Los puntos a declarar como parciales o fuera de alcance son:

- El rol paciente esta modelado, pero no tiene acceso completo desde la interfaz porque la base actual no trae credenciales de pacientes.
- La eliminacion de historia clinica no esta implementada.
- Recetas y reportes quedan como ampliaciones futuras, no como funcionalidades activas.
