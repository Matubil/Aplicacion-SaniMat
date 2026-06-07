# SaniMat - Prototipo JavaFX

SaniMat es una aplicación de escritorio académica para la gestión de una clínica médica. El sistema permite administrar pacientes, médicos, especialidades, turnos, pagos e historias clínicas, utilizando JavaFX para la interfaz gráfica y PostgreSQL como base de datos mediante conexión JDBC.

## Tecnologías utilizadas

* Java 21 o superior
* JavaFX
* Maven
* PostgreSQL
* JDBC

## Arquitectura del proyecto

El proyecto está organizado mediante una arquitectura por capas, separando la interfaz gráfica, la lógica de negocio, el acceso a datos y las entidades del dominio.

```text
src/main/java/com/sanimat
├── app          Inicio de la aplicación y navegación principal
├── config       Configuración de conexión JDBC y sesión de usuario
├── controller   Controladores de la interfaz
├── dao          Acceso a datos mediante JDBC y PreparedStatement
├── model        Entidades y enumeraciones del dominio
├── service      Validaciones y reglas de negocio
├── util         Utilidades de validación, contraseña y alertas
└── view         Pantallas JavaFX
```

## Requisitos

Para ejecutar el proyecto se necesita:

* Java 21 o una versión superior compatible.
* Maven instalado.
* PostgreSQL instalado y en ejecución.
* Base de datos `clinica_medica` creada.

## Base de datos

1. Crear la base de datos en PostgreSQL:

```sql
CREATE DATABASE clinica_medica;
```

2. Ejecutar el script principal del proyecto conectado a la base `clinica_medica`:

```bash
psql -U postgres -d clinica_medica -f database/schema.sql
```

El archivo `database/schema.sql` crea las tablas, restricciones, datos iniciales y vistas necesarias para el funcionamiento del prototipo.

## Configuración de conexión

La configuración inicial de conexión se encuentra en:

```text
src/main/resources/database.properties
```

También puede generarse un archivo externo:

```text
database.properties
```

Este archivo externo tiene prioridad sobre el archivo incluido en `src/main/resources`.

Además, la aplicación incluye un botón de configuración de conexión en la pantalla de login. Esta opción permite indicar host, puerto, base de datos, usuario y contraseña, además de probar la conexión antes de iniciar sesión. Fue incorporada para facilitar la ejecución y prueba del prototipo sin necesidad de modificar manualmente el archivo de configuración.

También pueden utilizarse variables de entorno:

```text
SANIMAT_DB_URL
SANIMAT_DB_USER
SANIMAT_DB_PASSWORD
```

## Ejecutar el proyecto

Desde la raíz del proyecto, ejecutar:

```bash
mvn javafx:run
```

También puede abrirse el proyecto desde un entorno compatible con Maven y ejecutar la clase principal:

```text
com.sanimat.app.MainApp
```

## Usuarios de prueba

| Rol        | Usuario    | Contraseña |
| ---------- | ---------- | ---------- |
| Secretaría | pabloSecre | 12345      |

Los usuarios de prueba se cargan desde el script `database/schema.sql`.

## Funcionalidades implementadas

El prototipo incluye las siguientes funcionalidades principales:

* Inicio de sesión con usuario y contraseña.
* Navegación según rol.
* Menú principal para secretaría.
* Menú principal para médico.
* Gestión de especialidades.
* Gestión de médicos.
* Gestión de pacientes.
* Gestión de turnos.
* Cancelación de turnos.
* Actualización de turnos como finalizados o ausentes desde el perfil médico.
* Registro de pagos.
* Validación de pagos completos, sin pagos parciales.
* Consulta y actualización de historia clínica.
* Edición del perfil del médico autenticado.
* Conexión a PostgreSQL mediante JDBC.

## Reglas principales implementadas

* No se permiten turnos superpuestos para un mismo médico.
* Los horarios se generan en bloques de 30 minutos según la disponibilidad laboral del médico.
* Los turnos cancelados o finalizados no pueden modificarse.
* Los turnos cancelados no requieren pago.
* No se permiten pagos parciales.
* El monto del pago debe coincidir con el importe calculado por el sistema.
* Los datos obligatorios se validan antes de guardar.
* Se validan formatos como DNI, email, teléfono, matrícula y fechas.
* El acceso a las pantallas depende del rol del usuario autenticado.

## Alcance del prototipo

El prototipo se centra en los módulos principales de gestión administrativa y médica de una clínica. La gestión de pacientes y turnos se centraliza desde el perfil de secretaría, mientras que el perfil médico permite consultar turnos asignados y trabajar con historias clínicas.

Algunas funcionalidades quedan contempladas como futuras ampliaciones del sistema, como el acceso directo de pacientes, la generación de recetas médicas, reportes completos y recordatorios automáticos de turnos.

No se implementa facturación electrónica ni gestión contable avanzada, ya que el módulo de pagos se limita al registro simple del cobro correspondiente al turno.

## Estructura de base de datos

El script `database/schema.sql` define, entre otras, las siguientes tablas:

* `personas`
* `secretarios`
* `medicos`
* `pacientes`
* `especialidades`
* `horarios_laborales`
* `turnos`
* `pagos`
* `historias_clinicas`
* `enum_estado_turno`
* `enum_tipo_cobertura`
* `enum_medio_pago`

## Observación

Este proyecto fue desarrollado con fines académicos como prototipo funcional para la materia Seminario de Práctica de Informática.
