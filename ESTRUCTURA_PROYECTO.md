# Estructura del proyecto

Este proyecto es una aplicacion de escritorio academica desarrollada con Java base, JavaFX, JDBC y PostgreSQL. La organizacion separa la interfaz grafica, las reglas de negocio, el acceso a datos y las entidades del dominio.

## Raiz del proyecto

| Archivo o carpeta | Contenido |
| --- | --- |
| `pom.xml` | Configuracion Maven del proyecto. Define Java 21, JavaFX y el driver JDBC de PostgreSQL. |
| `README.md` | Resumen general del proyecto, requisitos y comandos basicos de ejecucion. |
| `database/schema.sql` | Script principal para crear tablas, enumeraciones, restricciones, datos iniciales y vistas de consulta. |
| `database/validacion_horarios_laborales_media_hora.sql` | Script auxiliar para validar que los horarios laborales usen bloques redondos o de media hora. |
| `src/main/java` | Codigo fuente Java organizado por paquetes. |
| `src/main/resources` | Configuracion de base de datos y estilos JavaFX. |
| `target` | Carpeta generada por compilaciones. No contiene codigo fuente. |

## Paquetes Java

```text
src/main/java/com/sanimat
|-- app
|-- config
|-- controller
|-- dao
|-- model
|-- service
|-- util
`-- view
```

## `app`

Contiene el punto de entrada principal de la aplicacion.

| Clase | Responsabilidad |
| --- | --- |
| `MainApp` | Inicializa JavaFX, carga estilos, muestra el login y luego la ventana principal. |

## `config`

Contiene clases de configuracion y estado de sesion.

| Clase | Responsabilidad |
| --- | --- |
| `DatabaseConfig` | Lee `database.properties`, permite variables de entorno y crea conexiones JDBC a PostgreSQL. |
| `Session` | Mantiene el usuario autenticado durante la ejecucion. |

## `controller`

Contiene controladores livianos de coordinacion entre vistas y servicios.

| Clase | Responsabilidad |
| --- | --- |
| `LoginController` | Coordina el login entre la pantalla `LoginView` y el servicio `AuthService`. |

## `dao`

Contiene el acceso a datos. Estas clases ejecutan SQL mediante JDBC y `PreparedStatement`.

| Clase | Responsabilidad |
| --- | --- |
| `UsuarioDao` | Busca credenciales de secretarios y medicos para iniciar sesion. |
| `EspecialidadDao` | Consulta, registra, modifica y elimina especialidades. |
| `MedicoDao` | Consulta, registra, modifica y elimina medicos junto con sus datos personales y horarios laborales. |
| `PacienteDao` | Consulta, registra, modifica y elimina pacientes junto con sus datos personales. |
| `TurnoDao` | Consulta, registra, modifica y cancela turnos. Tambien valida disponibilidad y horarios laborales. |
| `PagoDao` | Consulta y registra pagos. |
| `HistoriaClinicaDao` | Consulta, registra y modifica historias clinicas. |
| `DaoException` | Excepcion propia para errores de acceso a datos. |

## `model`

Contiene entidades y enumeraciones del dominio.

| Clase | Responsabilidad |
| --- | --- |
| `Persona` | Datos comunes: DNI, nombre, apellido, fecha de nacimiento, contacto y direccion. |
| `Paciente` | Datos del paciente, cobertura y numero de afiliado. |
| `Medico` | Datos del medico, matricula, usuario, contrasenia y especialidad. |
| `Secretario` | Datos del secretario y usuario asociado. |
| `Especialidad` | Especialidad medica con nombre y descripcion. |
| `Turno` | Turno asignado a paciente y medico, con fecha, hora, estado y pago. |
| `Pago` | Pago asociado a un turno. |
| `HistoriaClinica` | Registro clinico asociado a un paciente. |
| `HorarioMedico` | Dia y rango horario laboral de un medico. |
| `Usuario`, `Rol`, `RoleName` | Modelo de usuario autenticado y roles. |
| `EstadoTurno` | Estados posibles de un turno: confirmado, cancelado, ausente y finalizado. |
| `TipoCobertura` | Tipos de cobertura del paciente. |
| `MedioPago` | Medios de pago disponibles. |

## `service`

Contiene validaciones y reglas de negocio. La vista no deberia decidir reglas importantes directamente; debe delegarlas en esta capa.

| Clase | Responsabilidad |
| --- | --- |
| `AuthService` | Valida login y devuelve el usuario autenticado. |
| `EspecialidadService` | Valida nombre unico y operaciones de especialidades. |
| `MedicoService` | Valida datos personales, DNI, matricula, usuario unico y dependencias. |
| `PacienteService` | Valida datos personales, DNI unico, cobertura y dependencias. |
| `TurnoService` | Valida alta, modificacion, cancelacion, disponibilidad, superposicion y horarios laborales. |
| `PagoService` | Calcula monto esperado, aplica descuentos y evita pagos parciales. |
| `HistoriaClinicaService` | Valida paciente, fecha y descripcion de historia clinica. |

## `util`

Contiene utilidades reutilizables.

| Clase | Responsabilidad |
| --- | --- |
| `Validator` | Validaciones comunes: obligatorio, DNI, email, telefono, matricula, fecha futura y montos positivos. |
| `ValidationException` | Excepcion propia para errores de validacion de negocio. |
| `TextNormalizer` | Normaliza textos, por ejemplo capitalizando nombres y apellidos. |
| `PasswordUtil` | Compara credenciales ingresadas con las guardadas. |
| `Dialogs` | Centraliza alertas, errores y confirmaciones JavaFX. |

## `view`

Contiene las pantallas JavaFX y componentes visuales.

| Clase | Responsabilidad |
| --- | --- |
| `LoginView` | Pantalla de inicio de sesion. |
| `MainShell` | Ventana principal, barra lateral, navegacion por rol y contenedor de vistas. |
| `DashboardView` | Pantalla inicial con metricas y resumen de turnos. |
| `SpecialtyManagementView` | Gestion de especialidades. |
| `DoctorManagementView` | Gestion de medicos. |
| `DoctorProfileView` | Edicion del perfil del medico autenticado. |
| `PatientManagementView` | Gestion de pacientes. |
| `AppointmentManagementView` | Gestion y consulta de turnos. |
| `PaymentView` | Registro y consulta de pagos. |
| `ClinicalHistoryView` | Consulta, alta y modificacion de historia clinica. |
| `PlaceholderView` | Pantalla para funcionalidades futuras, como recetas. |
| `Ui` | Fabrica de controles, tablas, botones, tarjetas y estilos comunes. |

## Recursos

| Archivo | Contenido |
| --- | --- |
| `src/main/resources/database.properties` | URL, usuario y contrasenia de PostgreSQL. |
| `src/main/resources/styles/sanimat.css` | Estilos visuales de la aplicacion JavaFX. |

## Base de datos

El script `database/schema.sql` define, entre otras, las siguientes tablas:

| Tabla | Funcion |
| --- | --- |
| `personas` | Datos personales comunes. |
| `secretarios` | Usuarios con rol de secretaria. |
| `medicos` | Usuarios medicos, matricula y especialidad. |
| `pacientes` | Pacientes y cobertura. |
| `especialidades` | Catalogo de especialidades. |
| `horarios_laborales` | Dias y rangos horarios de atencion de medicos. |
| `turnos` | Agenda de turnos. |
| `pagos` | Pagos registrados para turnos. |
| `historias_clinicas` | Historia clinica asociada a pacientes. |
| `enum_estado_turno` | Catalogo de estados de turno. |
| `enum_tipo_cobertura` | Catalogo de coberturas. |
| `enum_medio_pago` | Catalogo de medios de pago. |
