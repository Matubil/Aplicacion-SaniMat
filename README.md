# SaniMat - Prototipo JavaFX

Aplicacion de escritorio academica para una clinica medica. Usa JavaFX para la interfaz y PostgreSQL mediante JDBC para persistencia.

## Arquitectura

```text
src/main/java/com/sanimat
├── app          Inicio de la aplicacion y navegacion principal
├── config       Conexion JDBC y sesion de usuario
├── controller   Controladores livianos de UI
├── dao          Acceso a datos con PreparedStatement
├── model        Entidades y enums del dominio
├── service      Validaciones y reglas de negocio
├── util         Utilidades de validacion, password y alertas
└── view         Pantallas JavaFX puras
```

## Requisitos

- Java 21 o superior.
- Maven.
- PostgreSQL.

## Base de datos

1. Crear la base:

```sql
CREATE DATABASE clinica_medica;
```

2. Ejecutar el script conectado a `clinica_medica`:

```bash
psql -U postgres -d clinica_medica -f database/schema.sql
```

3. Revisar credenciales en `src/main/resources/database.properties`.

Tambien se pueden usar variables de entorno:

```text
SANIMAT_DB_URL
SANIMAT_DB_USER
SANIMAT_DB_PASSWORD
```

## Ejecutar

```bash
mvn javafx:run
```

## Usuarios de prueba

| Rol | Usuario | Clave |
| --- | --- | --- |
| Secretaria | secretaria1 | hash_de_prueba_5 |
| Secretaria | secretaria2 | hash_de_prueba_6 |
| Medico | medico1 | hash_de_prueba_1 |
| Medico | medico2 | hash_de_prueba_2 |

## Alcance del prototipo

El prototipo incluye login para secretarios y medicos, gestion de pacientes, medicos, especialidades, turnos, pagos e historias clinicas. Las reglas principales se validan en la capa de servicios y las consultas usan `PreparedStatement`.

El login respeta la base actual: las credenciales se leen desde `secretarios.usuario/contrasenia` y `medicos.usuario/contrasenia`.

La base compartida no trae credenciales para pacientes. Por eso el rol paciente queda preparado en el modelo, pero no aparece como login hasta agregar usuario/contrasenia o una tabla de usuarios para pacientes.
